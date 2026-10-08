package com.gps.camera.map.photo.gpscamera.features.home.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.nativead.NativeAd
import com.gps.camera.map.photo.gpscamera.R
import com.gps.camera.map.photo.gpscamera.core.ads.AdManager
import com.gps.camera.map.photo.gpscamera.core.ads.NativeAdRenderer
import com.gps.camera.map.photo.gpscamera.core.ads.NativePlacement
import com.gps.camera.map.photo.gpscamera.core.util.applyTopInsetAsMargin
import com.gps.camera.map.photo.gpscamera.databinding.FragmentHomeBinding
import com.gps.camera.map.photo.gpscamera.features.camera.ui.CameraFragment
import com.gps.camera.map.photo.gpscamera.features.gallery.ui.GalleryFragment
import com.gps.camera.map.photo.gpscamera.features.layout.ui.LayoutPickerFragment
import com.gps.camera.map.photo.gpscamera.features.map.repository.MapRepository
import com.gps.camera.map.photo.gpscamera.features.map.ui.MapViewFragment
import com.gps.camera.map.photo.gpscamera.features.settings.ui.SettingsFragment
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import java.io.File
import com.gps.camera.map.photo.gpscamera.core.config.RemoteConfigManager
import com.gps.camera.map.photo.gpscamera.core.util.applyBottomInsetAsMargin

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private var nativeAd: NativeAd? = null

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
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnSettingsHome.applyTopInsetAsMargin()
        binding.nativeAdContainer.applyBottomInsetAsMargin()
        binding.mapPreview.setTileSource(TileSourceFactory.MAPNIK)
        binding.mapPreview.setMultiTouchControls(false)
        binding.mapPreview.minZoomLevel = 3.0
        binding.mapPreview.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)

        binding.btnSettingsHome.setOnClickListener {
            openScreen(SettingsFragment())
        }
        binding.cardCamera.setOnClickListener {
            openScreen(CameraFragment())
        }
        binding.cardGallery.setOnClickListener {
            openScreen(GalleryFragment())
        }
        binding.cardLayout.setOnClickListener {
            openScreen(LayoutPickerFragment())
        }
        binding.cardPhotoGrid.setOnClickListener {
            openScreen(CameraFragment.newInstance(gridMode = true))
        }
        binding.mapTouchCatcher.setOnClickListener {
            openScreen(MapViewFragment())
        }

        loadMapPreview()
        loadNativeAd()
    }

    private fun loadNativeAd() {

        // ---------------------------------------------------------
        // Remote Config: Home Native Ad
        // ---------------------------------------------------------

        if (
            !RemoteConfigManager.areAdsEnabled() ||
            !RemoteConfigManager.isHomeEnabled()
        ) {
            binding.nativeAdContainer.visibility = View.GONE
            return
        }

        // ---------------------------------------------------------
        // Use preloaded Home Native Ad
        // ---------------------------------------------------------

        val preloaded = AdManager.consumeNativeAd(
            NativePlacement.HOME
        )

        if (preloaded != null) {
            renderNativeAd(preloaded)
            return
        }

        // ---------------------------------------------------------
        // No preloaded ad → Load normally
        // ---------------------------------------------------------

        binding.nativeAdContainer.visibility = View.VISIBLE
        binding.shimmerNativeAd.visibility = View.VISIBLE
        binding.shimmerNativeAd.startShimmer()

        AdManager.loadNativeAd(
            context = requireContext(),
            adUnitId = getString(R.string.native_home_ad_id),

            onLoaded = { ad ->
                if (_binding != null) {
                    renderNativeAd(ad)
                } else {
                    ad.destroy()
                }
            },

            onFailed = {
                if (_binding != null) {
                    binding.shimmerNativeAd.stopShimmer()
                    binding.nativeAdContainer.visibility = View.GONE
                }
            }
        )
    }

    private fun renderNativeAd(ad: NativeAd) {
        nativeAd?.destroy()
        nativeAd = ad
        binding.shimmerNativeAd.stopShimmer()
        binding.shimmerNativeAd.visibility = View.GONE
        binding.nativeAdView.root.visibility = View.VISIBLE
        NativeAdRenderer.bind(binding.nativeAdView, ad)
    }

    private fun openScreen(fragment: Fragment) {

        // 1. Global ads switch
        if (!RemoteConfigManager.areAdsEnabled()) {
            openScreenWithoutAd(fragment)
            return
        }

        // 2. Home interstitial Remote Config switch
        if (!RemoteConfigManager.isHomeInterstitialEnabled()) {
            openScreenWithoutAd(fragment)
            return
        }

        // 3. 35-second cooldown
        if (!AdManager.canShowInterstitial()) {
            openScreenWithoutAd(fragment)
            return
        }

        // 4. 35 seconds passed → load and show Home interstitial
        AdManager.loadAndShowInterstitial(
            activity = requireActivity(),
            adUnitId = getString(R.string.interstitial_home_ad_id)
        ) {

            if (!isAdded) return@loadAndShowInterstitial

            openScreenWithoutAd(fragment)
        }
    }


    private fun openScreenWithoutAd(fragment: Fragment) {

        parentFragmentManager.beginTransaction()
            .replace(
                R.id.fragmentContainer,
                fragment
            )
            .addToBackStack(null)
            .commit()
    }

    private fun loadMapPreview() {
        val repository = MapRepository(requireContext())
        viewLifecycleOwner.lifecycleScope.launch {
            val locations = repository.getPhotoLocations()
            if (_binding == null) return@launch

            val center = if (locations.isNotEmpty()) {
                GeoPoint(locations.first().latitude, locations.first().longitude)
            } else {
                GeoPoint(20.5937, 78.9629)
            }
            val zoom = if (locations.isNotEmpty()) 12.0 else 4.0

            binding.mapPreview.post {
                if (_binding == null) return@post
                binding.mapPreview.controller.setZoom(zoom)
                binding.mapPreview.controller.setCenter(center)
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
        val mapView = _binding?.mapPreview ?: return
        mapView.controller.zoomIn()
        mapView.postDelayed({
            _binding?.mapPreview?.controller?.zoomOut()
        }, 300)
    }

    override fun onResume() {
        super.onResume()
        _binding?.mapPreview?.onResume()
    }

    override fun onPause() {
        _binding?.mapPreview?.onPause()
        super.onPause()
    }

    override fun onDestroyView() {
        nativeAd?.destroy()
        super.onDestroyView()
        _binding = null
    }
}
