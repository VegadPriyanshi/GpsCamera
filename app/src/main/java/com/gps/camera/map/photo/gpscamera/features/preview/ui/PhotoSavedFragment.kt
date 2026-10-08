package com.gps.camera.map.photo.gpscamera.features.preview.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.android.gms.ads.nativead.NativeAd
import com.gps.camera.map.photo.gpscamera.R
import com.gps.camera.map.photo.gpscamera.core.ads.AdManager
import com.gps.camera.map.photo.gpscamera.core.ads.NativeAdRenderer
import com.gps.camera.map.photo.gpscamera.core.ads.NativePlacement
import com.gps.camera.map.photo.gpscamera.core.config.RemoteConfigManager
import com.gps.camera.map.photo.gpscamera.core.util.applyBottomInsetAsMargin
import com.gps.camera.map.photo.gpscamera.core.util.applyTopInsetAsMargin
import com.gps.camera.map.photo.gpscamera.databinding.FragmentPhotoSavedBinding
import com.gps.camera.map.photo.gpscamera.features.gallery.ui.GalleryFragment

/**
 * Shown right after a photo is saved from the normal capture flow — lets the user jump to the
 * app's own gallery, or share the just-saved photo via the system share sheet.
 */
class PhotoSavedFragment : Fragment() {

    private var _binding: FragmentPhotoSavedBinding? = null
    private val binding get() = _binding!!
    private var nativeAd: NativeAd? = null

    private val photoUri: Uri by lazy { requireArguments().getString(ARG_PHOTO_URI)!!.toUri() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPhotoSavedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBackSaved.applyTopInsetAsMargin()
        binding.btnGallerySaved.applyTopInsetAsMargin()
        binding.nativeAdContainer.applyBottomInsetAsMargin()

        Glide.with(binding.ivSavedThumbnail).load(photoUri).into(binding.ivSavedThumbnail)

        binding.btnBackSaved.setOnClickListener {
            // The spent PhotoPreviewFragment was already removed from the back stack when
            // this screen was pushed, so CameraFragment is directly below — plain pop.
            parentFragmentManager.popBackStack()
        }
        binding.btnGallerySaved.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, GalleryFragment())
                .addToBackStack(null)
                .commit()
        }
        binding.btnShareSaved.setOnClickListener {
            launchShareChooser()
        }

        loadNativeAd()
    }

    private fun loadNativeAd() {
        if (
            !RemoteConfigManager.areAdsEnabled() ||
            !RemoteConfigManager.isPhotoSavedEnabled()
        ) {
            binding.nativeAdContainer.visibility = View.GONE
            return
        }

        val preloaded = AdManager.consumeNativeAd(NativePlacement.PHOTO_SAVED)
        if (preloaded != null) {
            renderNativeAd(preloaded)
            return
        }

        binding.shimmerNativeAd.startShimmer()
        AdManager.loadNativeAd(
            context = requireContext(),
            adUnitId = getString(R.string.native_photo_saved_ad_id),
            onLoaded = { ad -> if (_binding != null) renderNativeAd(ad) },
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

    private fun launchShareChooser() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, photoUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.share_photo)))
    }

    override fun onDestroyView() {
        nativeAd?.destroy()
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_PHOTO_URI = "photo_uri"

        fun newInstance(photoUri: Uri): PhotoSavedFragment =
            PhotoSavedFragment().apply {
                arguments = Bundle().apply { putString(ARG_PHOTO_URI, photoUri.toString()) }
            }
    }
}
