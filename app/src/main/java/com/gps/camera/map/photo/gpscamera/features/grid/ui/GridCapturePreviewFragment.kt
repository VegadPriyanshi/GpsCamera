package com.gps.camera.map.photo.gpscamera.features.grid.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.gps.camera.map.photo.gpscamera.R
import com.gps.camera.map.photo.gpscamera.core.util.BitmapUtils
import com.gps.camera.map.photo.gpscamera.core.util.applyBottomInsetAsPadding
import com.gps.camera.map.photo.gpscamera.databinding.FragmentGridCapturePreviewBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Shows the photo for a Photo Grid before the grid editor — either just captured (with a
 * retake option) or picked from the gallery (with a rotate option, since a picked photo can't
 * be "retaken").
 */
class GridCapturePreviewFragment : Fragment() {

    private var _binding: FragmentGridCapturePreviewBinding? = null
    private val binding get() = _binding!!

    private var originalBitmap: Bitmap? = null
    private var rotationDegrees = 0

    private val photoPath: String by lazy { requireArguments().getString(ARG_PHOTO_PATH)!! }
    private val hasLocation: Boolean by lazy { requireArguments().getBoolean(ARG_HAS_LOCATION) }
    private val latitude: Double by lazy { requireArguments().getDouble(ARG_LATITUDE) }
    private val longitude: Double by lazy { requireArguments().getDouble(ARG_LONGITUDE) }
    private val fromGallery: Boolean by lazy { requireArguments().getBoolean(ARG_FROM_GALLERY) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGridCapturePreviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.bottomBar.applyBottomInsetAsPadding()

        // Use decodeExifBitmap to handle automatic rotation from camera hardware.
        val decoded = BitmapUtils.decodeExifBitmap(photoPath)
        originalBitmap = decoded
        binding.ivGridCapture.setImageBitmap(decoded)

        if (fromGallery) {
            binding.btnRetakeGrid.contentDescription = getString(R.string.rotate_photo)
            binding.btnRetakeGrid.setOnClickListener {
                rotatePhoto()
            }
        } else {
            binding.btnRetakeGrid.contentDescription = getString(R.string.retake)
            binding.btnRetakeGrid.setOnClickListener {
                File(photoPath).delete()
                parentFragmentManager.popBackStack()
            }
        }

        binding.tvNextGrid.setOnClickListener {
            goNext()
        }
    }

    private fun rotatePhoto() {
        val base = originalBitmap ?: return
        rotationDegrees = (rotationDegrees + 90) % 360
        val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
        val rotated = Bitmap.createBitmap(base, 0, 0, base.width, base.height, matrix, true)
        binding.ivGridCapture.setImageBitmap(rotated)
    }

    private fun goNext() {
        val lat = if (hasLocation) latitude else null
        val lon = if (hasLocation) longitude else null

        if (fromGallery && rotationDegrees != 0) {
            binding.tvNextGrid.isEnabled = false
            binding.btnRetakeGrid.isEnabled = false
            viewLifecycleOwner.lifecycleScope.launch {
                val rotatedPath = withContext(Dispatchers.IO) { writeRotatedTempFile() }
                if (_binding == null) return@launch
                openGridEditor(rotatedPath, lat, lon)
            }
        } else {
            openGridEditor(photoPath, lat, lon)
        }
    }

    /** Persists the currently-rotated bitmap to a fresh temp file for the grid editor to load. */
    private fun writeRotatedTempFile(): String {
        val base = originalBitmap!!
        val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
        val rotated = Bitmap.createBitmap(base, 0, 0, base.width, base.height, matrix, true)
        val newFile = File(requireContext().cacheDir, "gallery_rotated_${System.currentTimeMillis()}.jpg")
        newFile.outputStream().use { out -> rotated.compress(Bitmap.CompressFormat.JPEG, 95, out) }
        File(photoPath).delete()
        return newFile.absolutePath
    }

    private fun openGridEditor(path: String, latitude: Double?, longitude: Double?) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, PhotoGridFragment.newInstance(path, latitude, longitude))
            .addToBackStack(null)
            .commit()
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
        private const val ARG_FROM_GALLERY = "from_gallery"

        fun newInstance(
            photoPath: String,
            latitude: Double?,
            longitude: Double?,
            fromGallery: Boolean = false
        ): GridCapturePreviewFragment =
            GridCapturePreviewFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PHOTO_PATH, photoPath)
                    putBoolean(ARG_HAS_LOCATION, latitude != null && longitude != null)
                    putDouble(ARG_LATITUDE, latitude ?: 0.0)
                    putDouble(ARG_LONGITUDE, longitude ?: 0.0)
                    putBoolean(ARG_FROM_GALLERY, fromGallery)
                }
            }
    }
}
