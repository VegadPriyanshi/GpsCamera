package com.gps.camera.map.photo.gpscamera.features.uninstall.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import com.gps.camera.map.photo.gpscamera.MainActivity
import com.gps.camera.map.photo.gpscamera.databinding.ActivityUninstallReasonBinding
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.ads.nativead.NativeAd
import com.gps.camera.map.photo.gpscamera.R
import com.gps.camera.map.photo.gpscamera.core.ads.AdManager
import com.gps.camera.map.photo.gpscamera.core.ads.NativeAdRenderer
import com.gps.camera.map.photo.gpscamera.core.config.RemoteConfigManager
import com.gps.camera.map.photo.gpscamera.core.util.applyBottomInsetAsMargin

class UninstallReasonActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUninstallReasonBinding
    private var nativeAd: NativeAd? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityUninstallReasonBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.nativeAdContainer.applyBottomInsetAsMargin()

        loadNativeAd()

        // Back arrow
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnExit.setOnClickListener {
            finishAffinity()
        }

        // Open Android App Info
        binding.btnUninstall.setOnClickListener {
            launchAppInfo()
        }
    }


    private fun loadNativeAd() {

        if (
            !RemoteConfigManager.areAdsEnabled() ||
            !RemoteConfigManager.isUninstallReasonNativeEnabled()
        ) {
            binding.nativeAdContainer.visibility = View.GONE
            return
        }

        binding.shimmerNativeAd.visibility = View.VISIBLE
        binding.shimmerNativeAd.startShimmer()

        AdManager.loadNativeAd(
            context = this,
            adUnitId = getString(R.string.native_uninstall_reason_ad_id),

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

    private fun launchAppInfo() {

        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        ).apply {
            data = Uri.parse("package:$packageName")
        }

        startActivity(intent)
    }


    override fun onDestroy() {

        nativeAd?.destroy()
        nativeAd = null

        super.onDestroy()
    }
}