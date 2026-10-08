package com.gps.camera.map.photo.gpscamera.features.gallery.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.gps.camera.map.photo.gpscamera.R
import com.gps.camera.map.photo.gpscamera.core.util.applyVerticalInsetsAsPadding
import com.gps.camera.map.photo.gpscamera.databinding.FragmentGalleryBinding
import com.gps.camera.map.photo.gpscamera.features.gallery.adapter.PhotoAdapter
import com.gps.camera.map.photo.gpscamera.features.gallery.repository.GalleryRepository
import com.gps.camera.map.photo.gpscamera.features.map.ui.MapViewFragment
import kotlinx.coroutines.launch
import com.google.android.gms.ads.nativead.NativeAd
import com.gps.camera.map.photo.gpscamera.core.ads.AdManager
import com.gps.camera.map.photo.gpscamera.core.ads.NativeAdRenderer
import com.gps.camera.map.photo.gpscamera.core.ads.NativePlacement
import com.gps.camera.map.photo.gpscamera.core.config.RemoteConfigManager

class GalleryFragment : Fragment() {

    private var _binding: FragmentGalleryBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: GalleryRepository
    private lateinit var adapter: PhotoAdapter

    private var nativeAd: NativeAd? = null

    private val requiredPermission: String
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

    private val requestPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                showGallery()
            } else {
                showPermissionMessage()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGalleryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.root.applyVerticalInsetsAsPadding()

        repository = GalleryRepository(requireContext())
        adapter = PhotoAdapter { uri ->
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, PhotoDetailFragment.newInstance(uri))
                .addToBackStack(null)
                .commit()
        }
        binding.recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.recyclerView.adapter = adapter

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnMap.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, MapViewFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.tvPermissionMessage.setOnClickListener {
            requestPermission.launch(requiredPermission)
        }

        if (hasPermission()) {
            showGallery()
        } else {
            requestPermission.launch(requiredPermission)
        }
        loadNativeAd()
    }

    private fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(requireContext(), requiredPermission) ==
            PackageManager.PERMISSION_GRANTED

    private fun showGallery() {
        binding.tvPermissionMessage.visibility = View.GONE
        binding.recyclerView.visibility = View.VISIBLE
        loadPhotos()
    }

    private fun showPermissionMessage() {
        binding.tvPermissionMessage.visibility = View.VISIBLE
        binding.recyclerView.visibility = View.GONE
        binding.tvEmpty.visibility = View.GONE
    }

    private fun loadPhotos() {
        viewLifecycleOwner.lifecycleScope.launch {
            val photos = repository.getSavedPhotos()
            if (_binding == null) return@launch
            adapter.submitList(photos)
            binding.tvEmpty.visibility = if (photos.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun loadNativeAd() {

        if (
            !RemoteConfigManager.areAdsEnabled() ||
            !RemoteConfigManager.isGalleryEnabled()
        ) {
            binding.nativeAdContainer.visibility = View.GONE
            return
        }

        binding.nativeAdContainer.visibility = View.VISIBLE

        val preloaded = AdManager.consumeNativeAd(
            NativePlacement.GALLERY
        )

        if (preloaded != null) {
            renderNativeAd(preloaded)
        } else {
            // Gallery uses preload-only.
            // Do not make another AdMob request here.
            showPlaceholder()
        }
    }
    private fun renderNativeAd(ad: NativeAd) {

        nativeAd?.destroy()
        nativeAd = ad

        binding.shimmerNativeAd.stopShimmer()
        binding.shimmerNativeAd.visibility = View.GONE

        binding.nativeAdView.root.visibility = View.VISIBLE

        NativeAdRenderer.bind(
            binding.nativeAdView,
            ad
        )
    }

    private fun showPlaceholder() {

        binding.shimmerNativeAd.stopShimmer()
        binding.shimmerNativeAd.visibility = View.GONE

        binding.nativeAdView.root.visibility = View.GONE

        binding.nativeAdContainer.visibility = View.VISIBLE
    }

    override fun onDestroyView() {

        nativeAd?.destroy()
        nativeAd = null

        super.onDestroyView()
        _binding = null
    }
}
