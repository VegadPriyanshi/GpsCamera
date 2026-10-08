package com.gps.camera.map.photo.gpscamera.features.onboarding.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.gms.ads.nativead.NativeAd
import com.gps.camera.map.photo.gpscamera.R
import com.gps.camera.map.photo.gpscamera.core.ads.AdManager
import com.gps.camera.map.photo.gpscamera.core.ads.NativeAdRenderer
import com.gps.camera.map.photo.gpscamera.core.ads.NativePlacement
import com.gps.camera.map.photo.gpscamera.databinding.FragmentFullNativeAdBinding
import com.gps.camera.map.photo.gpscamera.databinding.FragmentOnboardingGenericBinding
import com.gps.camera.map.photo.gpscamera.databinding.NativeAdFullBinding
import com.gps.camera.map.photo.gpscamera.core.config.RemoteConfigManager

class FullNativeAdFragment : Fragment() {

    private var _binding: FragmentFullNativeAdBinding? = null
    private val binding get() = _binding!!
    private var nativeAd: NativeAd? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFullNativeAdBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val statusBarTop = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.setPadding(0, statusBarTop, 0, 0)
            insets
        }

        binding.btnCloseFullNative.setOnClickListener { advanceToNextPage() }

        if (
            !RemoteConfigManager.areAdsEnabled() ||
            !RemoteConfigManager.isOnboardingFullEnabled()
        ) {
            showPlaceholder()
            return
        }

        val preloaded = AdManager.consumeNativeAd(
            NativePlacement.ONBOARDING_FULL
        )

        if (preloaded != null) {
            renderAd(preloaded)
        } else {
            AdManager.loadNativeAd(
                context = requireContext(),
                adUnitId = getString(R.string.native_onboarding_full_ad_id),
                onLoaded = { ad ->
                    if (isAdded) {
                        renderAd(ad)
                    } else {
                        ad.destroy()
                    }
                },
                onFailed = {
                    if (isAdded) {
                        showPlaceholder()
                    }
                }
            )
        }
    }

    private fun renderAd(ad: NativeAd) {
        nativeAd?.destroy()
        nativeAd = ad
        val adBinding = NativeAdFullBinding.inflate(layoutInflater, binding.flFullNative, false)
        NativeAdRenderer.bindFull(adBinding, ad)
        binding.flFullNative.removeAllViews()
        binding.flFullNative.addView(adBinding.root)
    }

    private fun showPlaceholder() {
        val placeholder = FragmentOnboardingGenericBinding.inflate(layoutInflater, binding.flFullNative, false)
        placeholder.tvTitle.setText(R.string.onboarding_placeholder_title)
        placeholder.tvDescription.setText(R.string.onboarding_placeholder_desc)
        placeholder.badgeContainer.setBackgroundResource(R.drawable.bg_card_teal)
        placeholder.ivOnboardingIcon.setImageResource(R.drawable.ic_photo_grid)
        binding.flFullNative.removeAllViews()
        binding.flFullNative.addView(placeholder.root)
    }

    private fun advanceToNextPage() {
        val viewPager = activity?.findViewById<ViewPager2>(R.id.viewPager) ?: return
        val current = viewPager.currentItem
        if (current < (viewPager.adapter?.itemCount ?: 0) - 1) {
            viewPager.currentItem = current + 1
        }
    }

    override fun onDestroyView() {
        nativeAd?.destroy()
        _binding = null
        super.onDestroyView()
    }
}
