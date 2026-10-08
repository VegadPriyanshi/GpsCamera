package com.gps.camera.map.photo.gpscamera.features.language.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.ads.nativead.NativeAd
import com.gps.camera.map.photo.gpscamera.R
import com.gps.camera.map.photo.gpscamera.core.ads.AdManager
import com.gps.camera.map.photo.gpscamera.core.ads.NativeAdRenderer
import com.gps.camera.map.photo.gpscamera.core.ads.NativePlacement
import com.gps.camera.map.photo.gpscamera.core.prefs.AppPrefs.selectedLanguageCode
import com.gps.camera.map.photo.gpscamera.core.translation.TranslationManager
import com.gps.camera.map.photo.gpscamera.core.util.applyBottomInsetAsMargin
import com.gps.camera.map.photo.gpscamera.core.util.applyTopInsetAsMargin
import com.gps.camera.map.photo.gpscamera.databinding.ActivityLanguageBinding
import com.gps.camera.map.photo.gpscamera.features.language.data.Languages
import com.gps.camera.map.photo.gpscamera.features.onboarding.ui.OnboardingActivity
import com.google.mlkit.nl.translate.TranslateLanguage
import com.gps.camera.map.photo.gpscamera.core.config.RemoteConfigManager

class LanguageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLanguageBinding
    private var nativeAd: NativeAd? = null
    private var pickedLanguageCode: String? = null
    private var adReloadedAfterSelection = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityLanguageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvTitle.applyTopInsetAsMargin()
        binding.nativeAdContainer.applyBottomInsetAsMargin()

        binding.rvLanguages.layoutManager = LinearLayoutManager(this)
        binding.rvLanguages.adapter = LanguageAdapter(Languages.ALL) { language ->
            pickedLanguageCode = language.translateCode
            binding.btnDone.visibility = View.VISIBLE
            if (!adReloadedAfterSelection) {
                adReloadedAfterSelection = true

                if (
                    RemoteConfigManager.areAdsEnabled() &&
                    RemoteConfigManager.isLanguageAd2Enabled()
                ) {
                    val preloadedAd = AdManager.consumeNativeAd(
                        NativePlacement.LANGUAGE_AD_2
                    )

                    if (preloadedAd != null) {
                        renderNativeAd(preloadedAd)
                    } else {
                        loadAndShowNativeAd(getString(R.string.native_language_ad_2_id))
                    }
                }
            }
        }

        binding.btnDone.setOnClickListener { onDoneClicked() }

        showInitialNativeAd()

        // Preload Onboarding's native ads now, while the user is still browsing this screen.
        if (
            RemoteConfigManager.areAdsEnabled() &&
            RemoteConfigManager.isOnboardingFirstEnabled()
        ) {
            AdManager.preloadNativeAd(
                this,
                NativePlacement.ONBOARDING_FIRST,
                getString(R.string.native_onboarding_ad_id)
            )
        }
        if (
            RemoteConfigManager.areAdsEnabled() &&
            RemoteConfigManager.isOnboardingLastEnabled()
        ) {
            AdManager.preloadNativeAd(
                this,
                NativePlacement.ONBOARDING_LAST,
                getString(R.string.native_onboarding_ad_id)
            )
        }
        if (
            RemoteConfigManager.areAdsEnabled() &&
            RemoteConfigManager.isOnboardingFullEnabled()
        ) {
            AdManager.preloadNativeAd(
                this,
                NativePlacement.ONBOARDING_FULL,
                getString(R.string.native_onboarding_full_ad_id)
            )
        }


        if (
            RemoteConfigManager.areAdsEnabled() &&
            RemoteConfigManager.isGalleryEnabled()
        ) {
            AdManager.preloadNativeAd(
                this,
                NativePlacement.GALLERY,
                getString(R.string.native_gallery_ad_id)
            )
        }
    }

    private fun showInitialNativeAd() {

        if (
            !RemoteConfigManager.areAdsEnabled() ||
            !RemoteConfigManager.isLanguageAd1Enabled()
        ) {
            binding.nativeAdContainer.visibility = View.GONE
            return
        }

        val preloaded = AdManager.consumeNativeAd(
            NativePlacement.LANGUAGE_AD_1
        )

        if (preloaded != null) {
            renderNativeAd(preloaded)
        } else {
            loadAndShowNativeAd(getString(R.string.native_language_ad_id))
        }
    }

    private fun loadAndShowNativeAd(adUnitId: String) {
        binding.shimmerNativeAd.startShimmer()
        binding.shimmerNativeAd.visibility = View.VISIBLE
        binding.nativeAdView.root.visibility = View.GONE
        AdManager.loadNativeAd(
            context = this,
            adUnitId = adUnitId,
            onLoaded = { ad -> if (!isFinishing) renderNativeAd(ad) },
            onFailed = {
                if (!isFinishing) {
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
        binding.nativeAdContainer.visibility = View.VISIBLE
        binding.nativeAdView.root.visibility = View.VISIBLE
        NativeAdRenderer.bind(binding.nativeAdView, ad)
    }

    private fun onDoneClicked() {
        val code = pickedLanguageCode ?: return
        if (code == TranslateLanguage.ENGLISH) {
            proceed(code)
            return
        }
        binding.downloadOverlay.visibility = View.VISIBLE
        TranslationManager.prepare(
            targetLanguageCode = code,
            onReady = {
                binding.downloadOverlay.visibility = View.GONE
                proceed(code)
            },
            onError = { error ->
                binding.downloadOverlay.visibility = View.GONE
                Toast.makeText(this, getString(R.string.language_download_failed, error.message), Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun proceed(code: String) {
        selectedLanguageCode = code
        startActivity(Intent(this, OnboardingActivity::class.java))
        finish()
    }

    override fun onDestroy() {
        nativeAd?.destroy()
        super.onDestroy()
    }
}
