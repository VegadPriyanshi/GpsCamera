package com.gps.camera.map.photo.gpscamera.features.uninstall.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.gps.camera.map.photo.gpscamera.MainActivity
import com.gps.camera.map.photo.gpscamera.databinding.ActivityUninstallIntroBinding
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.ads.nativead.NativeAd
import com.gps.camera.map.photo.gpscamera.R
import com.gps.camera.map.photo.gpscamera.core.ads.AdManager
import com.gps.camera.map.photo.gpscamera.core.ads.NativeAdRenderer
import com.gps.camera.map.photo.gpscamera.core.config.RemoteConfigManager

class UninstallIntroActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUninstallIntroBinding

    private var nativeAd: NativeAd? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityUninstallIntroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.nativeAdContainer.apply {
            ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->

                val bottom = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                ).bottom

                view.setPadding(
                    view.paddingLeft,
                    view.paddingTop,
                    view.paddingRight,
                    bottom
                )

                insets
            }

            ViewCompat.requestApplyInsets(this)
        }

        loadNativeAd()

        // User changed their mind.
        // Continue using GPS Camera.
        binding.btnTryAgain.setOnClickListener {
            finishAffinity()        }

        // User still wants to uninstall.
        // Open the reason screen.
        binding.tvStillWantUninstall.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    UninstallReasonActivity::class.java
                )
            )
        }
    }

    private fun loadNativeAd() {

        if (
            !RemoteConfigManager.areAdsEnabled() ||
            !RemoteConfigManager.isUninstallIntroNativeEnabled()
        ) {
            binding.nativeAdContainer.visibility = View.GONE
            return
        }

        binding.shimmerNativeAd.visibility = View.VISIBLE
        binding.shimmerNativeAd.startShimmer()

        AdManager.loadNativeAd(
            context = this,
            adUnitId = getString(R.string.native_uninstall_intro_ad_id),

            onLoaded = { ad ->

                if (isFinishing || isDestroyed) {
                    ad.destroy()
                    return@loadNativeAd
                }

                nativeAd?.destroy()
                nativeAd = ad

                binding.shimmerNativeAd.stopShimmer()
                binding.shimmerNativeAd.visibility = View.GONE

                binding.nativeAdView.root.visibility = View.VISIBLE

                NativeAdRenderer.bind(
                    binding.nativeAdView,
                    ad
                )
            },

            onFailed = {

                if (isFinishing || isDestroyed) {
                    return@loadNativeAd
                }

                binding.shimmerNativeAd.stopShimmer()
                binding.shimmerNativeAd.visibility = View.GONE

                binding.nativeAdContainer.visibility = View.GONE
            }
        )
    }

    private fun continueToApp() {

        val intent = Intent(
            this,
            MainActivity::class.java
        ).apply {
            flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        startActivity(intent)
    }

    private fun openReasonScreen() {

        startActivity(
            Intent(
                this,
                UninstallReasonActivity::class.java
            )
        )
    }


    override fun onDestroy() {

        nativeAd?.destroy()
        nativeAd = null

        super.onDestroy()
    }
}