package com.gps.camera.map.photo.gpscamera.features.uninstall.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdView
import com.gps.camera.map.photo.gpscamera.R
import com.gps.camera.map.photo.gpscamera.core.ads.AdManager
import com.gps.camera.map.photo.gpscamera.core.config.RemoteConfigManager
import com.gps.camera.map.photo.gpscamera.databinding.ActivityUninstallSplashBinding
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class UninstallSplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUninstallSplashBinding

    private var bannerAdView: AdView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityUninstallSplashBinding.inflate(layoutInflater)

        setContentView(binding.root)
        setupBottomSystemInset()

        checkRemoteConfig()
    }

    private fun checkRemoteConfig() {

        RemoteConfigManager.whenReady {

            val adsEnabled =
                RemoteConfigManager.areAdsEnabled()

            val bannerEnabled =
                RemoteConfigManager.isUninstallBannerEnabled()

            val interstitialEnabled =
                RemoteConfigManager.isUninstallInterstitialEnabled()

            // --------------------------------
            // BANNER
            // --------------------------------

            if (adsEnabled && bannerEnabled) {

                // Banner is enabled.
                // Load banner first.
                loadBannerAd(
                    adsEnabled,
                    interstitialEnabled
                )

            } else {

                // Banner disabled.
                binding.bannerAdContainer.visibility =
                    View.GONE

                // Directly show interstitial.
                showInterstitial(
                    adsEnabled,
                    interstitialEnabled
                )
            }
        }
    }

    private fun loadBannerAd(
        adsEnabled: Boolean,
        interstitialEnabled: Boolean
    ) {

        // Show shimmer while banner is loading
        binding.shimmerBannerPlaceholder.visibility =
            View.VISIBLE

        binding.shimmerBannerPlaceholder.startShimmer()

        // Get adaptive banner size
        val adSize =
            AdManager.adaptiveBannerAdSize(this)

        // Create AdView
        val adView = AdView(this)

        adView.adUnitId =
            getString(R.string.banner_uninstall_ad_id)

        adView.setAdSize(adSize)

        // Initially hide real banner
        adView.visibility =
            View.GONE

        bannerAdView = adView

        // Add banner to container
        binding.bannerAdContainer.addView(adView)

        // Load banner
        AdManager.loadBanner(
            adView = adView,

            // --------------------------------
            // BANNER SUCCESS
            // --------------------------------

            onLoaded = {

                if (isFinishing) return@loadBanner

                // Stop shimmer
                binding.shimmerBannerPlaceholder.stopShimmer()

                // Hide placeholder
                binding.shimmerBannerPlaceholder.visibility =
                    View.GONE

                // Show real banner
                adView.visibility =
                    View.VISIBLE

                // IMPORTANT:
                // Banner is now visible.
                // NOW start interstitial.
                showInterstitial(
                    adsEnabled,
                    interstitialEnabled
                )
            },

            // --------------------------------
            // BANNER FAILED
            // --------------------------------

            onFailed = {

                if (isFinishing) return@loadBanner

                // Stop shimmer
                binding.shimmerBannerPlaceholder.stopShimmer()

                // Hide placeholder
                binding.shimmerBannerPlaceholder.visibility =
                    View.GONE

                // Hide banner container
                binding.bannerAdContainer.visibility =
                    View.GONE

                // Banner failed.
                // Don't stop the flow.
                // Continue to interstitial.
                showInterstitial(
                    adsEnabled,
                    interstitialEnabled
                )
            }
        )
    }

    private fun showInterstitial(
        adsEnabled: Boolean,
        interstitialEnabled: Boolean
    ) {

        if (
            adsEnabled &&
            interstitialEnabled
        ) {

            AdManager.loadAndShowInterstitial(
                this,
                getString(R.string.interstitial_uninstall_ad_id)
            ) {

                // Interstitial closed
                openUninstallIntro()
            }

        } else {

            // Interstitial disabled
            openUninstallIntro()
        }
    }

    private fun openUninstallIntro() {

        if (isFinishing) return

        startActivity(
            Intent(
                this,
                UninstallIntroActivity::class.java
            )
        )

        finish()
    }


    private fun setupBottomSystemInset() {

        ViewCompat.setOnApplyWindowInsetsListener(
            binding.bannerAdContainer
        ) { view, insets ->

            val systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
            )

            val gestureInsets = insets.getInsets(
                WindowInsetsCompat.Type.mandatorySystemGestures()
            )

            val bottomInset = maxOf(
                systemBars.bottom,
                gestureInsets.bottom
            )

            val layoutParams = view.layoutParams

            if (layoutParams is ViewGroup.MarginLayoutParams) {

                layoutParams.bottomMargin = bottomInset

                view.layoutParams = layoutParams
            }

            insets
        }

        ViewCompat.requestApplyInsets(binding.bannerAdContainer)
    }

    override fun onDestroy() {

        binding.shimmerBannerPlaceholder.stopShimmer()

        bannerAdView?.destroy()

        bannerAdView = null

        super.onDestroy()
    }
}