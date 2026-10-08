package com.gps.camera.map.photo.gpscamera.features.camera.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.gps.camera.map.photo.gpscamera.R
import com.gps.camera.map.photo.gpscamera.core.util.applyBottomInsetAsPadding
import com.gps.camera.map.photo.gpscamera.core.util.applyTopInsetAsMargin
import com.gps.camera.map.photo.gpscamera.databinding.FragmentCameraBinding
import com.gps.camera.map.photo.gpscamera.features.gallery.ui.GalleryFragment
import com.gps.camera.map.photo.gpscamera.features.grid.ui.GridCapturePreviewFragment
import com.gps.camera.map.photo.gpscamera.features.location.repository.AddressRepository
import com.gps.camera.map.photo.gpscamera.features.location.repository.LocationRepository
import com.gps.camera.map.photo.gpscamera.features.preview.ui.PhotoPreviewFragment
import com.gps.camera.map.photo.gpscamera.features.settings.data.StampPreferences
import com.gps.camera.map.photo.gpscamera.features.settings.ui.SettingsFragment
import com.gps.camera.map.photo.gpscamera.features.stamp.repository.PhotoSaveRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class CameraFragment : Fragment() {

    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!

    private var imageCapture: ImageCapture? = null
    private lateinit var locationRepository: LocationRepository
    private lateinit var addressRepository: AddressRepository
    private lateinit var stampPreferences: StampPreferences
    private lateinit var saveRepository: PhotoSaveRepository

    private var isFrontCamera = false
    private var isFlashOn = false
    private var liveUpdatesStarted = false

    private var timerSecondsIndex = 0
    private val timerSeconds: Int get() = TIMER_OPTIONS[timerSecondsIndex]

    // Photo Grid capture skips the live address bar entirely and routes the captured
    // photo to the grid editor instead of the normal stamp preview.
    private val isGridMode: Boolean by lazy { arguments?.getBoolean(ARG_GRID_MODE) ?: false }

    private val requiredPermissions: Array<String>
        get() {
            val basePermissions = arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            return if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                basePermissions + Manifest.permission.WRITE_EXTERNAL_STORAGE
            } else {
                basePermissions
            }
        }

    private val requestPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            if (results.values.all { it }) {
                showCameraPreview()
            } else {
                showPermissionMessage()
            }
        }

    private val pickImageForGrid =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) handlePickedGridImage(uri)
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        locationRepository = LocationRepository(requireContext())
        addressRepository = AddressRepository(requireContext())
        stampPreferences = StampPreferences(requireContext())
        saveRepository = PhotoSaveRepository(requireContext())

        binding.controlsArea.applyBottomInsetAsPadding()
        binding.topBar.applyTopInsetAsMargin()

        binding.tvPermissionMessage.setOnClickListener {
            requestPermissions.launch(requiredPermissions)
        }

        binding.btnCapture.setOnClickListener {
            onCaptureClicked()
        }

        binding.btnTimer.setOnClickListener {
            showTimerMenu()
        }
        updateTimerBadge()

        binding.btnSettingsCamera.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, SettingsFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.btnFlash.setOnClickListener {
            toggleFlash()
        }

        binding.btnFlipCamera.setOnClickListener {
            isFrontCamera = !isFrontCamera
            startCamera()
        }

        binding.btnGallery.setOnClickListener {
            if (isGridMode) {
                pickImageForGrid.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            } else {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, GalleryFragment())
                    .addToBackStack(null)
                    .commit()
            }
        }

        if (hasAllPermissions()) {
            showCameraPreview()
        } else {
            requestPermissions.launch(requiredPermissions)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        // The Fragment instance survives navigating away (only the view is destroyed), so this
        // flag must reset too — otherwise the guard in startLiveLocationUpdates() thinks the
        // (now-cancelled) update loop is still running and never restarts it on return.
        liveUpdatesStarted = false
    }

    private fun hasAllPermissions(): Boolean = requiredPermissions.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    private fun showCameraPreview() {
        binding.tvPermissionMessage.visibility = View.GONE
        binding.previewView.visibility = View.VISIBLE
        binding.btnCapture.visibility = View.VISIBLE
        startCamera()
        if (!isGridMode) {
            startLiveLocationUpdates()
        }
    }

    private fun showPermissionMessage() {
        binding.tvPermissionMessage.visibility = View.VISIBLE
        binding.previewView.visibility = View.GONE
        binding.btnCapture.visibility = View.GONE
        binding.ivLiveStamp.visibility = View.GONE
    }

    private fun toggleFlash() {
        isFlashOn = !isFlashOn
        imageCapture?.flashMode = if (isFlashOn) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
        binding.btnFlash.alpha = if (isFlashOn) 1f else 0.5f
    }

    private fun showTimerMenu() {
        val popup = PopupMenu(requireContext(), binding.btnTimer)
        popup.menuInflater.inflate(R.menu.menu_timer, popup.menu)
        popup.menu.getItem(timerSecondsIndex).isChecked = true

        popup.setOnMenuItemClickListener { item ->
            val index = when (item.itemId) {
                R.id.timerOff -> 0
                R.id.timer3s -> 1
                R.id.timer5s -> 2
                R.id.timer10s -> 3
                else -> timerSecondsIndex
            }
            timerSecondsIndex = index
            updateTimerBadge()
            true
        }
        popup.show()
    }

    private fun updateTimerBadge() {
        val seconds = timerSeconds
        if (seconds == 0) {
            binding.ivTimerIcon.alpha = 0.5f
            binding.tvTimerBadge.visibility = View.GONE
        } else {
            binding.ivTimerIcon.alpha = 1f
            binding.tvTimerBadge.text = getString(R.string.timer_seconds_badge, seconds)
            binding.tvTimerBadge.visibility = View.VISIBLE
        }
    }

    private fun onCaptureClicked() {
        if (timerSeconds > 0) {
            startCountdownThenCapture(timerSeconds)
        } else {
            takePhoto()
        }
    }

    private fun startCountdownThenCapture(seconds: Int) {
        binding.btnCapture.isEnabled = false
        binding.btnTimer.isEnabled = false
        binding.tvCountdown.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch {
            for (remaining in seconds downTo 1) {
                if (_binding == null) return@launch
                binding.tvCountdown.text = remaining.toString()
                delay(1000)
            }
            if (_binding == null) return@launch
            binding.tvCountdown.visibility = View.GONE
            binding.btnTimer.isEnabled = true
            takePhoto()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            if (_binding == null) return@addListener
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder()
                .setFlashMode(if (isFlashOn) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF)
                .build()

            val cameraSelector = if (isFrontCamera) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(viewLifecycleOwner, cameraSelector, preview, imageCapture)
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    /** Periodically refreshes the live stamp preview shown exactly where it will be on the photo. */
    private fun startLiveLocationUpdates() {
        if (liveUpdatesStarted) return
        liveUpdatesStarted = true

        viewLifecycleOwner.lifecycleScope.launch {
            while (isActive) {
                refreshLiveInfo()
                delay(LIVE_UPDATE_INTERVAL_MS)
            }
        }
    }

    private suspend fun refreshLiveInfo() {
        // Try the cached last-known fix first so the address can appear almost
        // instantly; fall back to a fresh GPS fix only if nothing is cached yet.
        val location = locationRepository.getLastKnownLocation() ?: locationRepository.getCurrentLocation()
        if (_binding == null) return

        val stampData = saveRepository.buildStampData(location?.latitude, location?.longitude)
        if (_binding == null) return

        // Exact output includes map thumbnail for layouts that use it
        val layout = stampPreferences.stampLayout
        val needsMap = layout in listOf(0, 3, 5, 7) // LAYOUT_CLASSIC, LAYOUT_LIGHT_CARD, LAYOUT_SPLIT_MAP, LAYOUT_TOP_STRIP

        var mapThumb: Bitmap? = null
        if (needsMap && location != null) {
            mapThumb = captureMapThumbnail(location.latitude, location.longitude)
        }

        if (_binding == null) return

        // Match the saved photo's behavior exactly: if a field has nothing to show, it's
        // simply omitted — never a "fetching…" or "unavailable" placeholder.
        if (stampData.addressLine == null && stampData.dateTimeLine == null && stampData.latLongLine == null) {
            binding.ivLiveStamp.visibility = View.GONE
            return
        }

        val bitmap = withContext(Dispatchers.Default) {
            val w = binding.previewView.width
            val h = binding.previewView.height
            if (w <= 0 || h <= 0) return@withContext null

            // Render the stamp on a transparent placeholder that matches the preview area
            val placeholder = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            saveRepository.applyStampToBitmap(placeholder, stampData, mapThumb)
        }

        if (_binding == null || bitmap == null) return
        binding.ivLiveStamp.setImageBitmap(bitmap)
        binding.ivLiveStamp.visibility = View.VISIBLE
    }

    private suspend fun captureMapThumbnail(latitude: Double, longitude: Double): Bitmap? {
        val mapView = binding.mapSnapshotView
        return try {
            withContext(Dispatchers.Main) {
                Configuration.getInstance().apply {
                    userAgentValue = requireContext().packageName
                    osmdroidBasePath = requireContext().cacheDir
                    osmdroidTileCache = File(requireContext().cacheDir, "osmdroid/tiles").apply { mkdirs() }
                }
                mapView.setTileSource(TileSourceFactory.MAPNIK)
                mapView.controller.setZoom(16.0)
                mapView.controller.setCenter(GeoPoint(latitude, longitude))
                mapView.onResume()
            }

            var result: Bitmap? = null
            for (attempt in 1..5) {
                delay(500)
                if (_binding == null || mapView.width == 0 || mapView.height == 0) break
                val candidate = Bitmap.createBitmap(mapView.width, mapView.height, Bitmap.Config.ARGB_8888)
                withContext(Dispatchers.Main) {
                    mapView.draw(Canvas(candidate))
                }
                if (!isBlankThumbnail(candidate)) {
                    result = candidate
                    break
                }
            }
            withContext(Dispatchers.Main) {
                mapView.onPause()
            }
            result
        } catch (e: Exception) {
            null
        }
    }

    private fun isBlankThumbnail(bitmap: Bitmap): Boolean {
        val width = bitmap.width
        val height = bitmap.height
        val samplePoints = listOf(
            0.2f to 0.2f, 0.5f to 0.2f, 0.8f to 0.2f,
            0.2f to 0.5f, 0.5f to 0.5f, 0.8f to 0.5f,
            0.2f to 0.8f, 0.5f to 0.8f, 0.8f to 0.8f
        )
        val samples = samplePoints.map { (fx, fy) ->
            bitmap.getPixel((fx * width).toInt().coerceIn(0, width - 1), (fy * height).toInt().coerceIn(0, height - 1))
        }
        val reds = samples.map { Color.red(it) }
        val greens = samples.map { Color.green(it) }
        val blues = samples.map { Color.blue(it) }
        val variance = (reds.max() - reds.min()) + (greens.max() - greens.min()) + (blues.max() - blues.min())
        return variance < 15
    }

    private fun takePhoto() {
        val capture = imageCapture ?: return
        binding.btnCapture.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            val location = locationRepository.getCurrentLocation()
            captureToTempFile(capture, location?.latitude, location?.longitude)
        }
    }

    private fun captureToTempFile(capture: ImageCapture, latitude: Double?, longitude: Double?) {
        val tempFile = File(requireContext().cacheDir, "capture_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(tempFile).build()

        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    _binding?.btnCapture?.isEnabled = true
                    openPreview(tempFile, latitude, longitude)
                }

                override fun onError(exception: ImageCaptureException) {
                    _binding?.btnCapture?.isEnabled = true
                    Log.e("GpsCamera", "Photo capture failed", exception)
                    Toast.makeText(requireContext(), "Capture failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun openPreview(tempFile: File, latitude: Double?, longitude: Double?) {
        if (isGridMode) {
            openGridCapturePreview(tempFile, latitude, longitude, fromGallery = false)
            return
        }
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, PhotoPreviewFragment.newInstance(tempFile.absolutePath, latitude, longitude))
            .addToBackStack(BACKSTACK_PHOTO_PREVIEW)
            .commit()
    }

    private fun handlePickedGridImage(uri: Uri) {
        viewLifecycleOwner.lifecycleScope.launch {
            val tempFile = withContext(Dispatchers.IO) { copyUriToTempFile(uri) }
            if (_binding == null || tempFile == null) return@launch
            // The picked photo has no capture-time GPS fix of its own, so the map
            // context comes from the device's current location instead.
            val location = locationRepository.getCurrentLocation()
            if (_binding == null) return@launch
            openGridCapturePreview(tempFile, location?.latitude, location?.longitude, fromGallery = true)
        }
    }

    private fun copyUriToTempFile(uri: Uri): File? = try {
        val tempFile = File(requireContext().cacheDir, "gallery_${System.currentTimeMillis()}.jpg")
        requireContext().contentResolver.openInputStream(uri)?.use { input ->
            tempFile.outputStream().use { output -> input.copyTo(output) }
        }
        tempFile
    } catch (e: Exception) {
        Log.e("GpsCamera", "Failed to copy picked image", e)
        null
    }

    private fun openGridCapturePreview(tempFile: File, latitude: Double?, longitude: Double?, fromGallery: Boolean) {
        parentFragmentManager.beginTransaction()
            .replace(
                R.id.fragmentContainer,
                GridCapturePreviewFragment.newInstance(tempFile.absolutePath, latitude, longitude, fromGallery)
            )
            .addToBackStack(BACKSTACK_GRID_CAPTURE)
            .commit()
    }

    companion object {
        private const val LIVE_UPDATE_INTERVAL_MS = 30_000L
        private const val ARG_GRID_MODE = "grid_mode"
        const val BACKSTACK_GRID_CAPTURE = "grid_capture"
        const val BACKSTACK_PHOTO_PREVIEW = "photo_preview"

        // 0 = timer off.
        private val TIMER_OPTIONS = intArrayOf(0, 3, 5, 10)

        fun newInstance(gridMode: Boolean = false): CameraFragment = CameraFragment().apply {
            arguments = Bundle().apply { putBoolean(ARG_GRID_MODE, gridMode) }
        }
    }
}
