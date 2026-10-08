package com.gps.camera.map.photo.gpscamera.core.ads

import com.google.android.gms.ads.nativead.NativeAd
import com.gps.camera.map.photo.gpscamera.core.util.applyBottomInsetAsPadding
import com.gps.camera.map.photo.gpscamera.databinding.NativeAdFullBinding
import com.gps.camera.map.photo.gpscamera.databinding.NativeAdMediumBinding

/** Binds a loaded [NativeAd] into the shared native ad templates. */
object NativeAdRenderer {

    fun bindFull(binding: NativeAdFullBinding, nativeAd: NativeAd) {
        binding.bottomContent.applyBottomInsetAsPadding()

        val view = binding.root
        view.headlineView = binding.adHeadline
        view.bodyView = binding.adBody
        view.callToActionView = binding.adCallToAction
        view.iconView = binding.adIcon
        view.mediaView = binding.adMedia
        view.advertiserView = binding.adAdvertiser

        binding.adHeadline.text = nativeAd.headline
        binding.adBody.text = nativeAd.body
        binding.adCallToAction.text = nativeAd.callToAction

        val icon = nativeAd.icon
        if (icon != null) {
            binding.adIcon.setImageDrawable(icon.drawable)
            binding.adIcon.visibility = android.view.View.VISIBLE
        } else {
            binding.adIcon.visibility = android.view.View.GONE
        }

        val advertiser = nativeAd.advertiser
        if (advertiser != null) {
            binding.adAdvertiser.text = advertiser
            binding.adAdvertiser.visibility = android.view.View.VISIBLE
        } else {
            binding.adAdvertiser.visibility = android.view.View.GONE
        }

        // Deferred: MediaView reports itself as "too small" if given its mediaContent
        // before the current layout pass has measured it (e.g. right after flipping the
        // slot from GONE to VISIBLE). Posting waits for that pass to finish first.
        binding.adMedia.post { binding.adMedia.mediaContent = nativeAd.mediaContent }

        view.setNativeAd(nativeAd)
    }

    fun bind(binding: NativeAdMediumBinding, nativeAd: NativeAd) {
        val view = binding.root
        view.headlineView = binding.adHeadline
        view.bodyView = binding.adBody
        view.callToActionView = binding.adCallToAction
        view.iconView = binding.adIcon
        view.mediaView = binding.adMedia
        view.advertiserView = binding.adAdvertiser

        binding.adHeadline.text = nativeAd.headline
        binding.adBody.text = nativeAd.body
        binding.adCallToAction.text = nativeAd.callToAction

        val icon = nativeAd.icon
        if (icon != null) {
            binding.adIcon.setImageDrawable(icon.drawable)
            binding.adIcon.visibility = android.view.View.VISIBLE
        } else {
            binding.adIcon.visibility = android.view.View.GONE
        }

        val advertiser = nativeAd.advertiser
        if (advertiser != null) {
            binding.adAdvertiser.text = advertiser
            binding.adAdvertiser.visibility = android.view.View.VISIBLE
        } else {
            binding.adAdvertiser.visibility = android.view.View.GONE
        }

        // Deferred: MediaView reports itself as "too small" if given its mediaContent
        // before the current layout pass has measured it (e.g. right after flipping the
        // slot from GONE to VISIBLE). Posting waits for that pass to finish first.
        binding.adMedia.post { binding.adMedia.mediaContent = nativeAd.mediaContent }

        view.setNativeAd(nativeAd)
    }
}
