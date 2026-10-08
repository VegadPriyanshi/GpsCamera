package com.gps.camera.map.photo.gpscamera.core.config

import android.util.Log
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings

object RemoteConfigManager {

    private const val TAG = "RemoteConfigManager"

    private val remoteConfig: FirebaseRemoteConfig =
        FirebaseRemoteConfig.getInstance()

    private var isReady = false

    private val pendingCallbacks = mutableListOf<() -> Unit>()

    // ---------------------------------------------------------
    // Remote Config Keys
    // ---------------------------------------------------------

    private const val KEY_ADS_ENABLED = "ads_enabled"

    private const val KEY_SPLASH_BANNER_ENABLED =
        "splash_banner_enabled"

    private const val KEY_SPLASH_INTERSTITIAL_ENABLED =
        "splash_interstitial_enabled"

    private const val KEY_LANGUAGE_AD_1_ENABLED =
        "language_ad_1_enabled"

    private const val KEY_LANGUAGE_AD_2_ENABLED =
        "language_ad_2_enabled"

    private const val KEY_ONBOARDING_FIRST_ENABLED =
        "onboarding_first_enabled"

    private const val KEY_ONBOARDING_FULL_ENABLED =
        "onboarding_full_enabled"

    private const val KEY_ONBOARDING_LAST_ENABLED =
        "onboarding_last_enabled"

    private const val KEY_ONBOARDING_INTERSTITIAL_ENABLED =
        "onboarding_interstitial_enabled"

    private const val KEY_HOME_ENABLED =
        "home_enabled"

    private const val KEY_HOME_INTERSTITIAL_ENABLED =
        "home_interstitial_enabled"

    private const val KEY_LAYOUT_ENABLED =
        "layout_enabled"


    private const val KEY_GALLERY_ENABLED = "gallery_enabled"

    private const val KEY_PHOTO_SAVED_ENABLED = "photo_saved_enabled"

    private const val KEY_UNINSTALL_INTERSTITIAL_ENABLED =
        "uninstall_interstitial_enabled"

    private const val KEY_UNINSTALL_BANNER_ENABLED =
        "uninstall_banner_enabled"

    private const val KEY_UNINSTALL_INTRO_NATIVE_ENABLED =
        "uninstall_intro_native_enabled"

    private const val KEY_UNINSTALL_REASON_NATIVE_ENABLED =
        "uninstall_reason_native_enabled"

    // ---------------------------------------------------------
    // Default Values
    // ---------------------------------------------------------

    private val defaultValues = mapOf(
        KEY_ADS_ENABLED to true,

        KEY_SPLASH_BANNER_ENABLED to true,
        KEY_SPLASH_INTERSTITIAL_ENABLED to true,

        KEY_LANGUAGE_AD_1_ENABLED to true,
        KEY_LANGUAGE_AD_2_ENABLED to true,

        KEY_ONBOARDING_FIRST_ENABLED to true,
        KEY_ONBOARDING_FULL_ENABLED to true,
        KEY_ONBOARDING_LAST_ENABLED to true,

        KEY_ONBOARDING_INTERSTITIAL_ENABLED to true,

        KEY_HOME_ENABLED to true,
        KEY_HOME_INTERSTITIAL_ENABLED to true,
        KEY_LAYOUT_ENABLED to true,
        KEY_GALLERY_ENABLED to true,
        KEY_PHOTO_SAVED_ENABLED to true,
        KEY_UNINSTALL_INTERSTITIAL_ENABLED to true,
        KEY_UNINSTALL_BANNER_ENABLED to true,
        KEY_UNINSTALL_INTRO_NATIVE_ENABLED to true,
        KEY_UNINSTALL_REASON_NATIVE_ENABLED to true,


    )

    // ---------------------------------------------------------
    // Initialize
    // ---------------------------------------------------------

    fun initialize() {

        val configSettings = remoteConfigSettings {
            // use for production app minimumFetchIntervalInSeconds = 12 * 60 * 60
            minimumFetchIntervalInSeconds = 0
        }

        remoteConfig.setConfigSettingsAsync(configSettings)

        remoteConfig.setDefaultsAsync(defaultValues)
    }

    // ---------------------------------------------------------
    // Fetch and Activate
    // ---------------------------------------------------------

    fun fetchAndActivate(
        onComplete: (Boolean) -> Unit = {}
    ) {

        remoteConfig.fetchAndActivate()
            .addOnCompleteListener { task ->

                isReady = true

                if (task.isSuccessful) {

                    Log.d(
                        TAG,
                        "Remote Config fetch and activate SUCCESS"
                    )

                    onComplete(true)

                } else {

                    Log.w(
                        TAG,
                        "Remote Config fetch failed. Using default values."
                    )

                    onComplete(false)
                }

                notifyReadyCallbacks()
            }
    }

    // ---------------------------------------------------------
    // Wait Until Remote Config Is Ready
    // ---------------------------------------------------------

    fun whenReady(callback: () -> Unit) {

        if (isReady) {
            callback()
        } else {
            pendingCallbacks.add(callback)
        }
    }

    private fun notifyReadyCallbacks() {

        val callbacks = pendingCallbacks.toList()

        pendingCallbacks.clear()

        callbacks.forEach { callback ->
            callback()
        }
    }

    // ---------------------------------------------------------
    // Ad Controls
    // ---------------------------------------------------------

    fun areAdsEnabled(): Boolean =
        remoteConfig.getBoolean(KEY_ADS_ENABLED)

    fun isSplashBannerEnabled(): Boolean =
        remoteConfig.getBoolean(KEY_SPLASH_BANNER_ENABLED)

    fun isSplashInterstitialEnabled(): Boolean =
        remoteConfig.getBoolean(KEY_SPLASH_INTERSTITIAL_ENABLED)

    fun isLanguageAd1Enabled(): Boolean =
        remoteConfig.getBoolean(KEY_LANGUAGE_AD_1_ENABLED)

    fun isLanguageAd2Enabled(): Boolean =
        remoteConfig.getBoolean(KEY_LANGUAGE_AD_2_ENABLED)

    fun isOnboardingFirstEnabled(): Boolean =
        remoteConfig.getBoolean(KEY_ONBOARDING_FIRST_ENABLED)

    fun isOnboardingFullEnabled(): Boolean =
        remoteConfig.getBoolean(KEY_ONBOARDING_FULL_ENABLED)

    fun isOnboardingLastEnabled(): Boolean =
        remoteConfig.getBoolean(KEY_ONBOARDING_LAST_ENABLED)

    fun isOnboardingInterstitialEnabled(): Boolean =
        remoteConfig.getBoolean(KEY_ONBOARDING_INTERSTITIAL_ENABLED)

    fun isHomeEnabled(): Boolean =
        remoteConfig.getBoolean(KEY_HOME_ENABLED)

    fun isHomeInterstitialEnabled(): Boolean =
        remoteConfig.getBoolean(KEY_HOME_INTERSTITIAL_ENABLED)

    fun isLayoutEnabled(): Boolean =
        remoteConfig.getBoolean(KEY_LAYOUT_ENABLED)


    fun isGalleryEnabled(): Boolean =
        remoteConfig.getBoolean(KEY_GALLERY_ENABLED)

    fun isPhotoSavedEnabled(): Boolean =
        remoteConfig.getBoolean(KEY_PHOTO_SAVED_ENABLED)

    fun isUninstallInterstitialEnabled(): Boolean =
        remoteConfig.getBoolean(KEY_UNINSTALL_INTERSTITIAL_ENABLED)

    fun isUninstallBannerEnabled(): Boolean =
        remoteConfig.getBoolean(KEY_UNINSTALL_BANNER_ENABLED)


    fun isUninstallIntroNativeEnabled(): Boolean =
        remoteConfig.getBoolean(KEY_UNINSTALL_INTRO_NATIVE_ENABLED)

    fun isUninstallReasonNativeEnabled(): Boolean =
        remoteConfig.getBoolean(KEY_UNINSTALL_REASON_NATIVE_ENABLED)
}