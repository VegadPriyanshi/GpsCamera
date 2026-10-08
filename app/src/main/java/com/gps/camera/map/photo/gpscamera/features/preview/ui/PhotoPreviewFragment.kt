package com.gps.camera.map.photo.gpscamera.features.preview.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.gps.camera.map.photo.gpscamera.R
import com.gps.camera.map.photo.gpscamera.core.util.BitmapUtils
import com.gps.camera.map.photo.gpscamera.core.util.applyBottomInsetAsPadding
import com.gps.camera.map.photo.gpscamera.core.util.applyTopInsetAsMargin
import com.gps.camera.map.photo.gpscamera.databinding.FragmentPhotoPreviewBinding
import com.gps.camera.map.photo.gpscamera.features.camera.ui.CameraFragment
import com.gps.camera.map.photo.gpscamera.features.stamp.repository.PhotoSaveRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import java.io.File

class PhotoPreviewFragment : Fragment() {

    private var _binding: FragmentPhotoPreviewBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: PhotoSaveRepository
    private var stampedBitmap: Bitmap? = null

    // The raw, never-stamped photo — rotation is always applied fresh from this,
    // and the stamp is redrawn on top afterward so the text/badge stay upright.
    private var originalBitmap: Bitmap? = null
    private var rotationDegrees = 0
    private var stampData: PhotoSaveRepository.StampData? = null
    private var mapThumbnail: Bitmap? = null

    private val photoPath: String by lazy { requireArguments().getString(ARG_PHOTO_PATH)!! }
    private val hasLocation: Boolean by lazy { requireArguments().getBoolean(ARG_HAS_LOCATION) }
    private val latitude: Double by lazy { requireArguments().getDouble(ARG_LATITUDE) }
    private val longitude: Double by lazy { requireArguments().getDouble(ARG_LONGITUDE) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPhotoPreviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repository = PhotoSaveRepository(requireContext())
        binding.actionBar.applyBottomInsetAsPadding()
        binding.btnRotate.applyTopInsetAsMargin()
        binding.btnRotate.isEnabled = false

        // Show the initial decoded bitmap while we generate the stamp
        viewLifecycleOwner.lifecycleScope.launch {
            val initial = withContext(Dispatchers.Default) { BitmapUtils.decodeExifBitmap(photoPath) }
            if (_binding != null) {
                binding.ivPreview.setImageBitmap(initial)
            }
        }
        generateStamp()

        binding.btnRetake.setOnClickListener {
            File(photoPath).delete()
            parentFragmentManager.popBackStack()
        }

        binding.btnNext.setOnClickListener {
            confirmAndSave()
        }

        binding.btnRotate.setOnClickListener {
            rotatePreview()
        }
    }

    private fun showLoading(@androidx.annotation.StringRes messageRes: Int) {
        binding.tvLoading.setText(messageRes)
        binding.loadingScrim.visibility = View.VISIBLE
        binding.progressBar.visibility = View.VISIBLE
        binding.tvLoading.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        binding.loadingScrim.visibility = View.GONE
        binding.progressBar.visibility = View.GONE
        binding.tvLoading.visibility = View.GONE
    }

    private fun generateStamp() {
        showLoading(R.string.processing_photo)
        binding.btnNext.isEnabled = false

        val lat = if (hasLocation) latitude else null
        val lon = if (hasLocation) longitude else null

        viewLifecycleOwner.lifecycleScope.launch {
            val thumbnail = if (lat != null && lon != null) captureMapThumbnail(lat, lon) else null
            if (_binding == null) return@launch
            mapThumbnail = thumbnail

            val data = repository.buildStampData(lat, lon)
            if (_binding == null) return@launch
            stampData = data

            // Fix rotation issue: use decodeExifBitmap instead of BitmapFactory.decodeFile
            val decoded = withContext(Dispatchers.Default) { BitmapUtils.decodeExifBitmap(photoPath) }
            if (_binding == null) return@launch
            originalBitmap = decoded

            val stamped = withContext(Dispatchers.Default) { renderCurrentStamp() }
            if (_binding == null) return@launch
            stampedBitmap = stamped
            binding.ivPreview.setImageBitmap(stamped)

            hideLoading()
            binding.btnNext.isEnabled = true
            binding.btnRotate.isEnabled = true
        }
    }

    private fun rotatePreview() {
        if (originalBitmap == null) return
        rotationDegrees = (rotationDegrees + 90) % 360
        binding.btnRotate.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            val stamped = withContext(Dispatchers.Default) { renderCurrentStamp() }
            if (_binding == null) return@launch
            stampedBitmap = stamped
            binding.ivPreview.setImageBitmap(stamped)
            binding.btnRotate.isEnabled = true
        }
    }

    /** Rotates the raw photo to the current angle, then draws the stamp fresh on top. */
    private fun renderCurrentStamp(): Bitmap {
        val base = originalBitmap!!
        val rotated = if (rotationDegrees == 0) {
            base
        } else {
            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            Bitmap.createBitmap(base, 0, 0, base.width, base.height, matrix, true)
        }
        val data = stampData ?: PhotoSaveRepository.StampData(null, null, null)
        return repository.applyStampToBitmap(rotated, data, mapThumbnail)
    }

    private suspend fun captureMapThumbnail(latitude: Double, longitude: Double): Bitmap? {
        val mapView = binding.mapSnapshotView
        return try {
            Configuration.getInstance().apply {
                userAgentValue = requireContext().packageName
                osmdroidBasePath = requireContext().cacheDir
                osmdroidTileCache = File(requireContext().cacheDir, "osmdroid/tiles").apply { mkdirs() }
            }
            mapView.setTileSource(TileSourceFactory.MAPNIK)
            mapView.controller.setZoom(16.0)
            mapView.controller.setCenter(GeoPoint(latitude, longitude))
            mapView.onResume()

            var result: Bitmap? = null
            for (attempt in 1..5) {
                delay(500)
                if (_binding == null || mapView.width == 0 || mapView.height == 0) break
                val candidate = Bitmap.createBitmap(mapView.width, mapView.height, Bitmap.Config.ARGB_8888)
                mapView.draw(Canvas(candidate))
                if (!isBlankThumbnail(candidate)) {
                    result = candidate
                    break
                }
            }
            mapView.onPause()
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

    private fun confirmAndSave() {
        val bitmap = stampedBitmap ?: return
        binding.btnNext.isEnabled = false
        binding.btnRetake.isEnabled = false
        binding.btnRotate.isEnabled = false
        showLoading(R.string.saving_photo)

        viewLifecycleOwner.lifecycleScope.launch {
            val lat = if (hasLocation) latitude else null
            val lon = if (hasLocation) longitude else null
            val savedUri = repository.saveToGallery(bitmap, lat, lon)
            File(photoPath).delete()

            if (_binding == null) return@launch
            hideLoading()
            binding.btnNext.isEnabled = true
            binding.btnRetake.isEnabled = true
            binding.btnRotate.isEnabled = true

            if (savedUri != null) {
                // Drop this now-spent preview (its temp file is gone) from the back stack
                // entirely, so navigating back from the next screen can't land on it again.
                parentFragmentManager.popBackStack(CameraFragment.BACKSTACK_PHOTO_PREVIEW, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, PhotoSavedFragment.newInstance(savedUri))
                    .addToBackStack(null)
                    .commit()
            } else {
                Toast.makeText(requireContext(), "Failed to save photo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_PHOTO_PATH = "photo_path"
        private const val ARG_HAS_LOCATION = "has_location"
        private const val ARG_LATITUDE = "latitude"
        private const val ARG_LONGITUDE = "longitude"

        fun newInstance(photoPath: String, latitude: Double?, longitude: Double?): PhotoPreviewFragment =
            PhotoPreviewFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PHOTO_PATH, photoPath)
                    putBoolean(ARG_HAS_LOCATION, latitude != null && longitude != null)
                    putDouble(ARG_LATITUDE, latitude ?: 0.0)
                    putDouble(ARG_LONGITUDE, longitude ?: 0.0)
                }
            }
    }
}
