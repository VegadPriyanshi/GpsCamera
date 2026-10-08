package com.gps.camera.map.photo.gpscamera.features.splash.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdView
import com.gps.camera.map.photo.gpscamera.R
import com.gps.camera.map.photo.gpscamera.core.ads.AdManager
import com.gps.camera.map.photo.gpscamera.core.ads.NativePlacement
import com.gps.camera.map.photo.gpscamera.core.config.RemoteConfigManager
import com.gps.camera.map.photo.gpscamera.core.util.applyBottomInsetAsMargin
import com.gps.camera.map.photo.gpscamera.databinding.ActivitySplashBinding
import com.gps.camera.map.photo.gpscamera.features.language.ui.LanguageActivity

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    private val handler = Handler(Looper.getMainLooper())

    private var bannerAdView: AdView? = null
    private var bannerResolved = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bannerAdContainer.applyBottomInsetAsMargin()

        // ---------------------------------------------------------
        // Splash timing
        // ---------------------------------------------------------

        handler.postDelayed(
            { revealProgressAndName() },
            LOGO_ONLY_DELAY_MS
        )

        handler.postDelayed(
            { attemptGoToMain() },
            TOTAL_SPLASH_DELAY_MS
        )

        // ---------------------------------------------------------
        // Remote Config
        // ---------------------------------------------------------

        RemoteConfigManager.whenReady {

            // -----------------------------------------------------
            // 1. Splash Banner
            // -----------------------------------------------------

            if (
                RemoteConfigManager.areAdsEnabled() &&
                RemoteConfigManager.isSplashBannerEnabled()
            ) {
                loadBannerAd()
            } else {
                // Banner is disabled by Remote Config.
                // Mark it resolved so splash does not wait for it.
                bannerResolved = true

                binding.bannerAdContainer.visibility = View.GONE
            }

            // -----------------------------------------------------
            // 2. Language Native Ad #1
            // -----------------------------------------------------

            if (
                RemoteConfigManager.areAdsEnabled() &&
                RemoteConfigManager.isLanguageAd1Enabled()
            ) {
                AdManager.preloadNativeAd(
                    this,
                    NativePlacement.LANGUAGE_AD_1,
                    getString(R.string.native_language_ad_id)
                )
            }

            // -----------------------------------------------------
            // 3. Language Native Ad #2
            // -----------------------------------------------------

            if (
                RemoteConfigManager.areAdsEnabled() &&
                RemoteConfigManager.isLanguageAd2Enabled()
            ) {
                AdManager.preloadNativeAd(
                    this,
                    NativePlacement.LANGUAGE_AD_2,
                    getString(R.string.native_language_ad_2_id)
                )
            }

            // -----------------------------------------------------
            // 4. Splash Interstitial
            // -----------------------------------------------------

            if (
                RemoteConfigManager.areAdsEnabled() &&
                RemoteConfigManager.isSplashInterstitialEnabled()
            ) {
                AdManager.loadInterstitial(
                    this,
                    getString(R.string.interstitial_splash_ad_id)
                )
            }
        }
    }

    // =================================================================
    // Splash Banner
    // =================================================================

    private fun loadBannerAd() {

        val adSize = AdManager.adaptiveBannerAdSize(this)

        val placeholderHeightPx =
            (adSize.height * resources.displayMetrics.density).toInt()

        binding.shimmerBannerPlaceholder.layoutParams.height =
            placeholderHeightPx

        binding.shimmerBannerPlaceholder.requestLayout()

        // Adaptive banner is created completely in code.
        val adView = AdView(this).apply {

            adUnitId = getString(
                R.string.banner_splash_ad_id
            )

            setAdSize(adSize)

            visibility = View.GONE
        }

        bannerAdView = adView

        binding.bannerAdContainer.addView(adView)

        // Start shimmer while banner is loading.
        binding.shimmerBannerPlaceholder.visibility = View.VISIBLE
        binding.shimmerBannerPlaceholder.startShimmer()

        AdManager.loadBanner(
            adView = adView,

            onLoaded = {

                bannerResolved = true

                if (isFinishing) {
                    return@loadBanner
                }

                binding.shimmerBannerPlaceholder.stopShimmer()
                binding.shimmerBannerPlaceholder.visibility = View.GONE

                adView.visibility = View.VISIBLE
            },

            onFailed = {

                bannerResolved = true

                if (isFinishing) {
                    return@loadBanner
                }

                binding.shimmerBannerPlaceholder.stopShimmer()
                binding.shimmerBannerPlaceholder.visibility = View.GONE

                binding.bannerAdContainer.visibility = View.GONE
            }
        )
    }

    // =================================================================
    // Splash UI
    // =================================================================

    private fun revealProgressAndName() {

        if (isFinishing) {
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.tvAppName.visibility = View.VISIBLE

        binding.progressBar
            .animate()
            .alpha(1f)
            .setDuration(FADE_IN_DURATION_MS)
            .start()

        binding.tvAppName
            .animate()
            .alpha(1f)
            .setDuration(FADE_IN_DURATION_MS)
            .start()
    }

    // =================================================================
    // Navigation
    // =================================================================

    private fun attemptGoToMain() {

        if (isFinishing) {
            return
        }

        if (bannerResolved) {

            goToMain()

        } else {

            // Give the banner a small additional amount of time.
            handler.postDelayed(
                { goToMain() },
                BANNER_GRACE_MS
            )
        }
    }

    private fun goToMain() {

        if (isFinishing) {
            return
        }

        // ---------------------------------------------------------
        // Splash Interstitial Remote Config
        // ---------------------------------------------------------

        if (
            RemoteConfigManager.areAdsEnabled() &&
            RemoteConfigManager.isSplashInterstitialEnabled()
        ) {

            AdManager.showInterstitial(this) {

                if (isFinishing) {
                    return@showInterstitial
                }

                startActivity(
                    Intent(
                        this,
                        LanguageActivity::class.java
                    )
                )

                finish()
            }

        } else {

            startActivity(
                Intent(
                    this,
                    LanguageActivity::class.java
                )
            )

            finish()
        }
    }

    // =================================================================
    // Cleanup
    // =================================================================

    override fun onDestroy() {

        handler.removeCallbacksAndMessages(null)

        binding.shimmerBannerPlaceholder.stopShimmer()

        bannerAdView?.destroy()
        bannerAdView = null

        super.onDestroy()
    }

    companion object {

        private const val LOGO_ONLY_DELAY_MS = 1500L

        private const val TOTAL_SPLASH_DELAY_MS = 3500L

        private const val BANNER_GRACE_MS = 1500L

        private const val FADE_IN_DURATION_MS = 350L
    }
}