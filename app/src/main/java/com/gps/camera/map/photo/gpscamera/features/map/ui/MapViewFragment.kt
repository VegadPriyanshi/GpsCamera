package com.gps.camera.map.photo.gpscamera.features.map.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.gps.camera.map.photo.gpscamera.R
import com.gps.camera.map.photo.gpscamera.core.util.applyBottomInsetAsMargin
import com.gps.camera.map.photo.gpscamera.core.util.applyTopInsetAsMargin
import com.gps.camera.map.photo.gpscamera.databinding.FragmentMapBinding
import com.gps.camera.map.photo.gpscamera.features.gallery.ui.PhotoDetailFragment
import com.gps.camera.map.photo.gpscamera.features.map.repository.MapRepository
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.overlay.Marker
import java.io.File

class MapViewFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: MapRepository

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
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repository = MapRepository(requireContext())

        binding.mapView.setTileSource(TileSourceFactory.MAPNIK)
        binding.mapView.setMultiTouchControls(true)
        binding.mapView.minZoomLevel = 3.0
        binding.mapView.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)

        binding.btnBack.applyTopInsetAsMargin()
        binding.btnZoomOut.applyBottomInsetAsMargin()

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnZoomIn.setOnClickListener {
            binding.mapView.controller.zoomIn()
        }

        binding.btnZoomOut.setOnClickListener {
            binding.mapView.controller.zoomOut()
        }

        loadPhotoMarkers()
    }

    private fun openPhoto(uri: android.net.Uri) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, PhotoDetailFragment.newInstance(uri))
            .addToBackStack(null)
            .commit()
    }

    private fun loadPhotoMarkers() {
        viewLifecycleOwner.lifecycleScope.launch {
            val locations = repository.getPhotoLocations()
            if (_binding == null) return@launch

            if (locations.isEmpty()) {
                binding.tvEmpty.visibility = View.VISIBLE
                binding.mapView.post {
                    if (_binding == null) return@post
                    binding.mapView.controller.setZoom(4.0)
                    binding.mapView.controller.setCenter(GeoPoint(20.5937, 78.9629))
                    nudgeTileRefresh()
                }
                return@launch
            }

            val points = mutableListOf<GeoPoint>()
            locations.forEach { location ->
                val point = GeoPoint(location.latitude, location.longitude)
                val marker = Marker(binding.mapView)
                marker.position = point
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                marker.setOnMarkerClickListener { _, _ ->
                    openPhoto(location.uri)
                    true
                }
                binding.mapView.overlays.add(marker)
                points.add(point)
            }

            binding.mapView.post {
                if (_binding == null) return@post
                if (points.size == 1) {
                    binding.mapView.controller.setZoom(15.0)
                    binding.mapView.controller.setCenter(points[0])
                } else {
                    val box = BoundingBox.fromGeoPoints(points).increaseByScale(1.5f)
                    binding.mapView.zoomToBoundingBox(box, true)
                }
                nudgeTileRefresh()
            }
        }
    }

    /**
     * OSMDroid sometimes fails to request tiles after a purely programmatic
     * zoom/center change, only fetching them once the user manually zooms.
     * A tiny zoom step forces the same tile-request path a real gesture would.
     */
    private fun nudgeTileRefresh() {
        val mapView = _binding?.mapView ?: return
        mapView.controller.zoomIn()
        mapView.postDelayed({
            _binding?.mapView?.controller?.zoomOut()
        }, 300)
    }

    override fun onResume() {
        super.onResume()
        _binding?.mapView?.onResume()
    }

    override fun onPause() {
        _binding?.mapView?.onPause()
        super.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
