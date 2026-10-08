package com.gps.camera.map.photo.gpscamera.features.grid.ui

import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.RectF
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.gps.camera.map.photo.gpscamera.R
import com.gps.camera.map.photo.gpscamera.core.util.BitmapUtils
import com.gps.camera.map.photo.gpscamera.core.util.applyVerticalInsetsAsPadding
import com.gps.camera.map.photo.gpscamera.databinding.FragmentPhotoGridBinding
import com.gps.camera.map.photo.gpscamera.features.camera.ui.CameraFragment
import com.gps.camera.map.photo.gpscamera.features.grid.PhotoGridComposer
import com.gps.camera.map.photo.gpscamera.features.grid.adapter.GridStyleAdapter
import com.gps.camera.map.photo.gpscamera.features.location.repository.AddressRepository
import com.gps.camera.map.photo.gpscamera.features.stamp.repository.PhotoSaveRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.overlay.Marker
import java.io.File
import kotlin.math.roundToInt

/**
 * Lets the user pick a grid layout that pairs the captured photo with a live, pannable/zoomable
 * map cell centered on the photo's GPS fix. The map stays interactive right up to Save, at which
 * point its current on-screen frame is flattened together with the photo into one image.
 */
class PhotoGridFragment : Fragment() {

    private var _binding: FragmentPhotoGridBinding? = null
    private val binding get() = _binding!!

    private lateinit var saveRepository: PhotoSaveRepository
    private lateinit var addressRepository: AddressRepository
    private lateinit var adapter: GridStyleAdapter

    private var photoBitmap: Bitmap? = null
    private var selectedStyle = PhotoGridComposer.STYLE_TOP_BOTTOM
    private var selectedColor = PhotoGridComposer.COLOR_WHITE

    // Photo cell pan/zoom state, in the *source* photoBitmap's own pixel coordinates —
    // (photoFocusX, photoFocusY) is the source point centered in the cell view.
    private var photoFocusX = 0f
    private var photoFocusY = 0f
    private var photoZoom = 1f
    private var photoLastTouchX = 0f
    private var photoLastTouchY = 0f
    private val photoMatrix = Matrix()
    private lateinit var photoScaleDetector: ScaleGestureDetector

    private val photoPath: String by lazy { requireArguments().getString(ARG_PHOTO_PATH)!! }
    private val hasLocation: Boolean by lazy { requireArguments().getBoolean(ARG_HAS_LOCATION) }
    private val latitude: Double by lazy { requireArguments().getDouble(ARG_LATITUDE) }
    private val longitude: Double by lazy { requireArguments().getDouble(ARG_LONGITUDE) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Configuration.getInstance().apply {
            userAgentValue = requireContext().packageName
            osmdroidBasePath = requireContext().cacheDir
            osmdroidTileCache = File(requireContext().cacheDir, "osmdroid/tiles").apply { mkdirs() }
        }
        _binding = FragmentPhotoGridBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        saveRepository = PhotoSaveRepository(requireContext())
        addressRepository = AddressRepository(requireContext())

        binding.root.applyVerticalInsetsAsPadding()
        setupMapCell()
        setupPhotoCell()

        binding.btnSaveGrid.isEnabled = false

        binding.btnBackGrid.setOnClickListener {
            // Only returns to the retake/next confirm screen — the capture isn't
            // discarded, so the temp photo file must survive for a possible re-entry.
            parentFragmentManager.popBackStack(CameraFragment.BACKSTACK_GRID_CAPTURE, 0)
        }

        binding.btnGridColorWhite.setOnClickListener { setGridColor(PhotoGridComposer.COLOR_WHITE) }
        binding.btnGridColorBlack.setOnClickListener { setGridColor(PhotoGridComposer.COLOR_BLACK) }

        binding.btnSaveGrid.setOnClickListener {
            saveGrid()
        }

        binding.recyclerGridStyles.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        setGridColor(selectedColor)
        setupStyleAdapter()

        binding.gridContainer.post {
            if (_binding == null) return@post
            applyGridLayout()
        }

        loadPhoto()
    }

    /** Configures the live map cell: tiles, pinch-zoom/pan, and a pin at the photo's GPS fix. */
    private fun setupMapCell() {
        val mapView = binding.mapCellView
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.minZoomLevel = 3.0
        mapView.zoomController.setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT)

        // The map cell sits inside a vertically-scrolling screen; without this, the outer
        // NestedScrollView steals drag/pinch gestures meant for panning and zooming the map.
        mapView.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> v.parent?.requestDisallowInterceptTouchEvent(true)
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.parent?.requestDisallowInterceptTouchEvent(false)
            }
            false
        }

        val center = if (hasLocation) GeoPoint(latitude, longitude) else GeoPoint(20.5937, 78.9629)
        val zoom = if (hasLocation) 17.0 else 4.0

        mapView.post {
            if (_binding == null) return@post
            mapView.controller.setZoom(zoom)
            mapView.controller.setCenter(center)
            nudgeTileRefresh()
        }

        if (hasLocation) {
            val marker = Marker(mapView)
            marker.position = center
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            marker.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_location_badge)
            mapView.overlays.add(marker)

            viewLifecycleOwner.lifecycleScope.launch {
                val addressLine = addressRepository.getAddressLine(latitude, longitude)
                if (_binding == null || addressLine == null) return@launch
                marker.title = addressLine
                marker.showInfoWindow()
            }
        }
    }

    /**
     * OSMDroid sometimes fails to request tiles after a purely programmatic
     * zoom/center change, only fetching them once the user manually zooms.
     * A tiny zoom step forces the same tile-request path a real gesture would.
     */
    private fun nudgeTileRefresh() {
        val mapView = _binding?.mapCellView ?: return
        mapView.controller.zoomIn()
        mapView.postDelayed({
            _binding?.mapCellView?.controller?.zoomOut()
        }, 300)
    }

    private fun loadPhoto() {
        viewLifecycleOwner.lifecycleScope.launch {
            // Fix: Use decodeExifBitmap to respect camera orientation metadata
            val decodedPhoto = withContext(Dispatchers.Default) { BitmapUtils.decodeExifBitmap(photoPath) }
            if (_binding == null || decodedPhoto == null) return@launch
            photoBitmap = decodedPhoto
            photoFocusX = decodedPhoto.width / 2f
            photoFocusY = decodedPhoto.height / 2f
            binding.ivPhotoCell.setImageBitmap(decodedPhoto)
            binding.ivPhotoCell.doOnLayout { applyPhotoMatrix() }
            applyPhotoMatrix()

            binding.progressBarGrid.visibility = View.GONE
            binding.btnSaveGrid.isEnabled = true
        }
    }

    /** Lets the user drag to reposition and pinch to zoom the photo within its cell. */
    private fun setupPhotoCell() {
        val imageView = binding.ivPhotoCell

        photoScaleDetector = ScaleGestureDetector(
            requireContext(),
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    photoZoom = (photoZoom * detector.scaleFactor).coerceIn(1f, 4f)
                    applyPhotoMatrix()
                    return true
                }
            }
        )

        imageView.setOnTouchListener { v, event ->
            photoScaleDetector.onTouchEvent(event)
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    v.parent?.requestDisallowInterceptTouchEvent(true)
                    photoLastTouchX = event.x
                    photoLastTouchY = event.y
                }
                MotionEvent.ACTION_POINTER_DOWN -> {
                    photoLastTouchX = event.x
                    photoLastTouchY = event.y
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!photoScaleDetector.isInProgress && event.pointerCount == 1) {
                        panPhoto(event.x - photoLastTouchX, event.y - photoLastTouchY)
                    }
                    photoLastTouchX = event.x
                    photoLastTouchY = event.y
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.parent?.requestDisallowInterceptTouchEvent(false)
            }
            true
        }
    }

    private fun panPhoto(dx: Float, dy: Float) {
        val photo = photoBitmap ?: return
        val view = binding.ivPhotoCell
        if (view.width == 0 || view.height == 0) return

        val scale = coverScale(photo, view) * photoZoom
        photoFocusX -= dx / scale
        photoFocusY -= dy / scale
        applyPhotoMatrix()
    }

    private fun coverScale(photo: Bitmap, view: View): Float =
        maxOf(view.width.toFloat() / photo.width, view.height.toFloat() / photo.height)

    /** Re-centers the photo's Matrix on the current focus/zoom, clamped so no edge gap shows. */
    private fun applyPhotoMatrix() {
        val photo = photoBitmap ?: return
        val view = binding.ivPhotoCell
        if (view.width == 0 || view.height == 0) return

        val scale = coverScale(photo, view) * photoZoom
        val halfVisibleW = (view.width / 2f) / scale
        val halfVisibleH = (view.height / 2f) / scale
        photoFocusX = photoFocusX.coerceIn(halfVisibleW, photo.width - halfVisibleW)
        photoFocusY = photoFocusY.coerceIn(halfVisibleH, photo.height - halfVisibleH)

        photoMatrix.reset()
        photoMatrix.setScale(scale, scale)
        photoMatrix.postTranslate(
            view.width / 2f - photoFocusX * scale,
            view.height / 2f - photoFocusY * scale
        )
        view.imageMatrix = photoMatrix
    }

    /** Extracts, at full source resolution, exactly the region the user has framed on-screen. */
    private fun extractFramedPhoto(): Bitmap {
        val photo = photoBitmap!!
        val view = binding.ivPhotoCell
        val scale = coverScale(photo, view) * photoZoom
        val visibleW = (view.width / scale).roundToInt().coerceIn(1, photo.width)
        val visibleH = (view.height / scale).roundToInt().coerceIn(1, photo.height)
        val left = (photoFocusX - visibleW / 2f).roundToInt().coerceIn(0, photo.width - visibleW)
        val top = (photoFocusY - visibleH / 2f).roundToInt().coerceIn(0, photo.height - visibleH)
        return Bitmap.createBitmap(photo, left, top, visibleW, visibleH)
    }

    /** Sizes and positions the map/photo cells within [gridContainer] per the selected style. */
    private fun applyGridLayout() {
        val container = binding.gridContainer
        val width = container.width
        val height = container.height
        if (width == 0 || height == 0) return

        val (mapRect, photoRect) = PhotoGridComposer.computeRects(selectedStyle, width, height)
        setCellBounds(binding.mapCellView, mapRect)
        setCellBounds(binding.ivPhotoCell, photoRect)
        binding.ivPhotoCell.doOnLayout { applyPhotoMatrix() }
    }

    private fun setCellBounds(view: View, rect: RectF) {
        val params = (view.layoutParams as? FrameLayout.LayoutParams) ?: FrameLayout.LayoutParams(0, 0)
        params.width = rect.width().roundToInt()
        params.height = rect.height().roundToInt()
        params.leftMargin = rect.left.roundToInt()
        params.topMargin = rect.top.roundToInt()
        params.gravity = Gravity.TOP or Gravity.START
        view.layoutParams = params
    }

    private fun setupStyleAdapter() {
        adapter = GridStyleAdapter(generateStyleIcons(), selectedStyle) { index ->
            selectedStyle = index
            adapter.setSelected(index)
            applyGridLayout()
        }
        binding.recyclerGridStyles.adapter = adapter
    }

    private fun generateStyleIcons(): List<Bitmap> =
        (0 until PhotoGridComposer.STYLE_COUNT).map { style ->
            PhotoGridComposer.composeStyleIcon(style, selectedColor, 90, 120)
        }

    private fun setGridColor(color: Int) {
        selectedColor = color
        val isWhiteSelected = color == PhotoGridComposer.COLOR_WHITE

        binding.btnGridColorWhite.backgroundTintList = tintOf(if (isWhiteSelected) R.color.accent_blue else R.color.card_stroke)
        binding.btnGridColorWhite.setTextColor(requireContext().getColor(if (isWhiteSelected) R.color.white else R.color.navy))

        binding.btnGridColorBlack.backgroundTintList = tintOf(if (!isWhiteSelected) R.color.accent_blue else R.color.card_stroke)
        binding.btnGridColorBlack.setTextColor(requireContext().getColor(if (!isWhiteSelected) R.color.white else R.color.navy))

        binding.gridContainer.setBackgroundColor(requireContext().getColor(if (isWhiteSelected) R.color.white else R.color.black))

        if (::adapter.isInitialized) {
            adapter.updatePreviews(generateStyleIcons())
        }
    }

    private fun tintOf(colorRes: Int): ColorStateList = ColorStateList.valueOf(requireContext().getColor(colorRes))

    private fun saveGrid() {
        val photo = photoBitmap ?: return
        val mapView = binding.mapCellView
        if (mapView.width == 0 || mapView.height == 0) return

        binding.btnSaveGrid.isEnabled = false

        // Flatten whatever the user has currently panned/zoomed the live map cell to.
        val mapSnapshot = Bitmap.createBitmap(mapView.width, mapView.height, Bitmap.Config.ARGB_8888)
        mapView.draw(Canvas(mapSnapshot))

        viewLifecycleOwner.lifecycleScope.launch {
            val finalBitmap = withContext(Dispatchers.Default) {
                // Crop the ORIGINAL full-resolution photo to exactly what the user framed on
                // screen, rather than re-deriving a naive center-crop of the untouched photo.
                val framedPhoto = extractFramedPhoto()
                PhotoGridComposer.compose(mapSnapshot, framedPhoto, selectedStyle, selectedColor, photo.width, photo.height)
            }
            // The saved GPS EXIF always reflects where the photo was actually taken,
            // regardless of where the user panned the map cell for framing.
            val lat = if (hasLocation) latitude else null
            val lon = if (hasLocation) longitude else null
            val saved = saveRepository.saveToGallery(finalBitmap, lat, lon)
            File(photoPath).delete()

            if (_binding == null) return@launch
            binding.btnSaveGrid.isEnabled = true

            Toast.makeText(
                requireContext(),
                if (saved != null) "Photo saved" else "Failed to save photo",
                Toast.LENGTH_SHORT
            ).show()

            if (saved != null) {
                parentFragmentManager.popBackStack(CameraFragment.BACKSTACK_GRID_CAPTURE, FragmentManager.POP_BACK_STACK_INCLUSIVE)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        _binding?.mapCellView?.onResume()
    }

    override fun onPause() {
        _binding?.mapCellView?.onPause()
        super.onPause()
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

        fun newInstance(photoPath: String, latitude: Double?, longitude: Double?): PhotoGridFragment =
            PhotoGridFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PHOTO_PATH, photoPath)
                    putBoolean(ARG_HAS_LOCATION, latitude != null && longitude != null)
                    putDouble(ARG_LATITUDE, latitude ?: 0.0)
                    putDouble(ARG_LONGITUDE, longitude ?: 0.0)
                }
            }
    }
}
