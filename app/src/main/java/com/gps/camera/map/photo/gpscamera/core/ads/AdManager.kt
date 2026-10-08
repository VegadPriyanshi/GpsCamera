package com.gps.camera.map.photo.gpscamera.core.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions

/** Placements that show a native ad — one ad unit per placement, no odd/even variants. */
enum class NativePlacement {
    LANGUAGE_AD_1,
    LANGUAGE_AD_2,
    ONBOARDING_FIRST,
    ONBOARDING_LAST,
    ONBOARDING_FULL,
    LAYOUT,
    HOME ,
    GALLERY,
    PHOTO_SAVED
}

/**
 * Single home for all AdMob request/callback wiring in the app. Screens never talk to the
 * Google Mobile Ads SDK directly — they call in here, so the loading/callback/failure-handling
 * code for a given ad format is written once and reused everywhere that format is shown.
 */
object AdManager {

    private const val TAG = "AdManager"

    private const val INTERSTITIAL_COOLDOWN_MS = 35_000L

    private var lastInterstitialShownTime = 0L

    private var interstitialAd: InterstitialAd? = null
    private val nativeAdCache = mutableMapOf<NativePlacement, NativeAd>()

    // ---------------------------------------------------------------------
    // Banner
    // ---------------------------------------------------------------------

    /**
     * The full-width, device-adaptive banner size (taller and wider than the old fixed
     * 320x50 banner) for the current screen width. Computing this is a local/synchronous
     * calculation — no network round trip — so callers can size a placeholder to match
     * before the ad request even starts, with no layout jump once it loads.
     */
    fun adaptiveBannerAdSize(activity: Activity): AdSize {
        val metrics = activity.resources.displayMetrics
        val adWidthDp = (metrics.widthPixels / metrics.density).toInt()
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidthDp)
    }

    /**
     * Loads a banner into [adView], reusing its currently-set ad unit ID and size.
     * [onLoaded] fires once the ad is ready to display; [onFailed] fires if the request
     * failed (e.g. no fill, no network) — callers use these to swap a shimmer/placeholder
     * for the real ad, or to hide the ad slot entirely on failure.
     */
    fun loadBanner(adView: AdView, onLoaded: () -> Unit, onFailed: (LoadAdError) -> Unit) {
        adView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                onLoaded()
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                Log.w(TAG, "Banner ad failed to load: ${error.message}")
                onFailed(error)
            }
        }
        adView.loadAd(AdRequest.Builder().build())
    }

    // ---------------------------------------------------------------------
    // Interstitial
    // ---------------------------------------------------------------------

    /**
     * Requests an interstitial and holds onto it until [showInterstitial] is called.
     * Safe to call even if one is already loaded/loading — it's a no-op in that case,
     * since an interstitial is single-use and callers should preload once per screen.
     */
    fun loadInterstitial(context: Context, adUnitId: String) {
        if (interstitialAd != null) return
        InterstitialAd.load(
            context,
            adUnitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Interstitial failed to load: ${error.message}")
                    interstitialAd = null
                }
            }
        )
    }

    /**
     * Shows the preloaded interstitial if one is ready, otherwise calls [onComplete]
     * immediately — callers should treat [onComplete] as "safe to move on now" in both
     * cases, so the app never gets stuck waiting on an ad that may never arrive.
     */
    fun showInterstitial(
        activity: Activity,
        onComplete: () -> Unit
    ) {
        val ad = interstitialAd

        if (ad == null) {
            onComplete()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {

            override fun onAdShowedFullScreenContent() {

                // Start 35-second cooldown when the ad
                // is actually shown.
                lastInterstitialShownTime =
                    System.currentTimeMillis()

                Log.d(
                    "AdManager",
                    "INTERSTITIAL SHOWN - timer started at $lastInterstitialShownTime"
                )
            }

            override fun onAdDismissedFullScreenContent() {

                interstitialAd = null

                onComplete()
            }

            override fun onAdFailedToShowFullScreenContent(
                error: AdError
            ) {

                Log.w(
                    TAG,
                    "Interstitial failed to show: ${error.message}"
                )

                interstitialAd = null

                onComplete()
            }
        }

        ad.show(activity)
    }



    fun loadAndShowInterstitial(
        activity: Activity,
        adUnitId: String,
        onComplete: () -> Unit
    ) {

        Log.d(
            TAG,
            "HOME AD METHOD CALLED - adUnitId=$adUnitId"
        )

        InterstitialAd.load(
            activity,
            adUnitId,
            AdRequest.Builder().build(),

            object : InterstitialAdLoadCallback() {

                override fun onAdLoaded(ad: InterstitialAd) {

                    Log.d(
                        TAG,
                        "HOME INTERSTITIAL LOADED SUCCESSFULLY"
                    )

                    ad.fullScreenContentCallback =
                        object : FullScreenContentCallback() {

                            override fun onAdShowedFullScreenContent() {

                                Log.d(
                                    TAG,
                                    "HOME INTERSTITIAL SHOWN"
                                )

                                lastInterstitialShownTime =
                                    System.currentTimeMillis()
                            }

                            override fun onAdDismissedFullScreenContent() {

                                Log.d(
                                    TAG,
                                    "HOME INTERSTITIAL DISMISSED"
                                )
                                onComplete()
                            }

                            override fun onAdFailedToShowFullScreenContent(
                                error: AdError
                            ) {

                                Log.w(
                                    TAG,
                                    "Home interstitial failed: ${error.message}"
                                )

                                onComplete()
                            }
                        }

                    ad.show(activity)
                }

                override fun onAdFailedToLoad(
                    error: LoadAdError
                ) {

                    Log.w(
                        TAG,
                        "Home interstitial failed to load: ${error.message}"
                    )

                    // No ad → don't block the user.
                    onComplete()
                }
            }
        )
    }

    // ---------------------------------------------------------------------
    // Native
    // ---------------------------------------------------------------------

    /**
     * Preloads a native ad for [placement] and caches it for later use via
     * [consumeNativeAd]. Lets a screen kick off the request early (e.g. while the user is
     * still browsing a previous screen) so the ad is likely ready by the time it's shown.
     */
    fun preloadNativeAd(context: Context, placement: NativePlacement, adUnitId: String) {
        if (nativeAdCache.containsKey(placement)) return
        val loader = com.google.android.gms.ads.AdLoader.Builder(context, adUnitId)
            .forNativeAd { nativeAd ->
                Log.d(TAG, "PRELOADED: $placement")
                nativeAdCache[placement] = nativeAd }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Native ad ($placement) failed to load: ${error.message}")
                }
            })
            .withNativeAdOptions(NativeAdOptions.Builder().build())
            .build()
        loader.loadAd(AdRequest.Builder().build())
    }

    /** Returns and clears the cached native ad for [placement], if one finished loading. */
    fun consumeNativeAd(placement: NativePlacement): NativeAd? {
        val ad = nativeAdCache.remove(placement)

        Log.d(
            TAG,
            "CONSUME: $placement -> ${if (ad != null) "READY" else "NOT READY"}"
        )

        return ad
    }
    /**
     * Loads a native ad and hands it straight to [onLoaded] — for screens that need to
     * render it immediately rather than stash it in the preload cache for later.
     */
    fun loadNativeAd(
        context: Context,
        adUnitId: String,
        onLoaded: (NativeAd) -> Unit,
        onFailed: (LoadAdError) -> Unit
    ) {
        val loader = com.google.android.gms.ads.AdLoader.Builder(context, adUnitId)
            .forNativeAd { nativeAd -> onLoaded(nativeAd) }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Native ad failed to load: ${error.message}")
                    onFailed(error)
                }
            })
            .withNativeAdOptions(NativeAdOptions.Builder().build())
            .build()
        loader.loadAd(AdRequest.Builder().build())
    }


    fun canShowInterstitial(): Boolean {

        val currentTime = System.currentTimeMillis()

        val elapsedTime =
            currentTime - lastInterstitialShownTime

        val remainingTime =
            INTERSTITIAL_COOLDOWN_MS - elapsedTime

        Log.d(
            "AdManager",
            "lastShown=$lastInterstitialShownTime, " +
                    "elapsed=${elapsedTime / 1000}s, " +
                    "remaining=${maxOf(0, remainingTime) / 1000}s"
        )

        return elapsedTime >= INTERSTITIAL_COOLDOWN_MS
    }



}
