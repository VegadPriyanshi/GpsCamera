package com.gps.camera.map.photo.gpscamera.features.onboarding.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.gms.ads.nativead.NativeAd
import com.gps.camera.map.photo.gpscamera.R
import com.gps.camera.map.photo.gpscamera.core.ads.AdManager
import com.gps.camera.map.photo.gpscamera.core.ads.NativeAdRenderer
import com.gps.camera.map.photo.gpscamera.core.ads.NativePlacement
import com.gps.camera.map.photo.gpscamera.core.translation.TranslationManager
import com.gps.camera.map.photo.gpscamera.core.util.applyBottomInsetAsPadding
import com.gps.camera.map.photo.gpscamera.databinding.ActivityOnboardingBinding
import com.gps.camera.map.photo.gpscamera.features.onboarding.data.OnboardingItem
import com.gps.camera.map.photo.gpscamera.core.config.RemoteConfigManager
import com.gps.camera.map.photo.gpscamera.features.permission.ui.PermissionAccessActivity

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var items: List<OnboardingItem>
    private var nativeAdFirstPage: NativeAd? = null
    private var nativeAdLastPage: NativeAd? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bottomLayout.applyBottomInsetAsPadding()

        items = buildItems()

        binding.viewPager.adapter = OnboardingAdapter(this, items)
        binding.wormIndicator.attachTo(binding.viewPager)
        binding.viewPager.offscreenPageLimit = 1
        (binding.viewPager.getChildAt(0) as? RecyclerView)?.overScrollMode = RecyclerView.OVER_SCROLL_NEVER

        binding.btnNext.setOnClickListener {
            val current = binding.viewPager.currentItem
            if (current < items.size - 1) {
                binding.viewPager.currentItem = current + 1
            } else {
                finishOnboarding()
            }
        }

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                translateCurrentPage(position)
                updateBottomBarForPage(position)
            }
        })
        binding.viewPager.post {
            translateCurrentPage(0)
            updateBottomBarForPage(0)
        }

        RemoteConfigManager.whenReady {

            if (
                RemoteConfigManager.areAdsEnabled() &&
                RemoteConfigManager.isOnboardingInterstitialEnabled()
            ) {
                AdManager.loadInterstitial(
                    this,
                    getString(R.string.interstitial_onboarding_ad_id)
                )
            }
        }
    }

    private fun translateCurrentPage(position: Int) {
        val item = items[position]
        TranslationManager.translate(getString(item.buttonText)) { translated ->
            binding.btnNext.text = translated
        }
        // Runs after the page's fragment view has finished being created/attached, since
        // ViewPager2 builds it asynchronously (this is also why the title/description
        // views aren't reached by the button-text translate call above).
        binding.viewPager.post { TranslationManager.translateAllViews(binding.viewPager) }
    }

    private fun buildItems(): List<OnboardingItem> = listOf(
        OnboardingItem(
            title = R.string.onboarding_title_1,
            description = R.string.onboarding_desc_1,
            badgeBackground = R.drawable.bg_card_blue,
            icon = R.drawable.ic_camera,
            showNativeAd = true
        ),
        OnboardingItem(
            title = R.string.onboarding_title_2,
            description = R.string.onboarding_desc_2,
            badgeBackground = R.drawable.bg_card_teal,
            icon = R.drawable.ic_layout
        ),
        OnboardingItem(isFullNativeAd = true),
        OnboardingItem(
            title = R.string.onboarding_title_4,
            description = R.string.onboarding_desc_4,
            badgeBackground = R.drawable.bg_card_purple,
            icon = R.drawable.ic_map
        ),
        OnboardingItem(
            title = R.string.onboarding_title_5,
            description = R.string.onboarding_desc_5,
            badgeBackground = R.drawable.bg_card_orange,
            icon = R.drawable.ic_photo_grid,
            buttonText = R.string.onboarding_get_started,
            showNativeAd = true
        )
    )

    private fun updateBottomBarForPage(position: Int) {

        val item = items[position]

        // ---------------------------------------------------------
        // Full Native Ad Page
        // ---------------------------------------------------------

        if (item.isFullNativeAd) {

            if (
                !RemoteConfigManager.areAdsEnabled() ||
                !RemoteConfigManager.isOnboardingFullEnabled()
            ) {
                binding.bottomLayout.visibility = View.VISIBLE
                binding.nativeAdContainer.visibility = View.GONE
                return
            }

            binding.bottomLayout.visibility = View.GONE
            binding.nativeAdContainer.visibility = View.VISIBLE

            binding.shimmerNativeAd.visibility = View.VISIBLE
            binding.shimmerNativeAd.startShimmer()
            binding.nativeAdView.root.visibility = View.GONE

            val preloadedFullAd = AdManager.consumeNativeAd(
                NativePlacement.ONBOARDING_FULL
            )

            if (preloadedFullAd != null) {

                renderNativeAd(preloadedFullAd)

            } else {

                AdManager.loadNativeAd(
                    context = this,
                    adUnitId = getString(R.string.native_onboarding_full_ad_id),

                    onLoaded = { ad ->

                        if (isFinishing) return@loadNativeAd

                        if (binding.viewPager.currentItem == position) {
                            renderNativeAd(ad)
                        } else {
                            ad.destroy()
                        }
                    },

                    onFailed = {

                        if (
                            !isFinishing &&
                            binding.viewPager.currentItem == position
                        ) {
                            binding.shimmerNativeAd.stopShimmer()
                            binding.nativeAdContainer.visibility = View.GONE
                        }
                    }
                )
            }

            return
        }

        // ---------------------------------------------------------
        // Normal / First / Last Pages
        // ---------------------------------------------------------

        binding.bottomLayout.visibility = View.VISIBLE

        if (!item.showNativeAd) {
            binding.nativeAdContainer.visibility = View.GONE
            return
        }

        val isFirstPage = position == 0

// ---------------------------------------------------------
// Remote Config: Onboarding First Ad
// ---------------------------------------------------------

        if (
            isFirstPage &&
            (
                    !RemoteConfigManager.areAdsEnabled() ||
                            !RemoteConfigManager.isOnboardingFirstEnabled()
                    )
        ) {
            binding.nativeAdContainer.visibility = View.GONE
            return
        }

// ---------------------------------------------------------
// Remote Config: Onboarding Last Ad
// ---------------------------------------------------------

        val isLastPage = position == items.lastIndex

        if (
            isLastPage &&
            (
                    !RemoteConfigManager.areAdsEnabled() ||
                            !RemoteConfigManager.isOnboardingLastEnabled()
                    )
        ) {
            binding.nativeAdContainer.visibility = View.GONE
            return
        }

        // ---------------------------------------------------------
        // Native Ad Container
        // ---------------------------------------------------------

        binding.nativeAdContainer.visibility = View.VISIBLE

        val cachedAd = if (isFirstPage) {
            nativeAdFirstPage
        } else {
            nativeAdLastPage
        }

        if (cachedAd != null) {
            renderNativeAd(cachedAd)
            return
        }

        binding.shimmerNativeAd.visibility = View.VISIBLE
        binding.shimmerNativeAd.startShimmer()
        binding.nativeAdView.root.visibility = View.GONE

        val placement = if (isFirstPage) {
            NativePlacement.ONBOARDING_FIRST
        } else {
            NativePlacement.ONBOARDING_LAST
        }

        val preloaded = AdManager.consumeNativeAd(placement)

        if (preloaded != null) {

            if (isFirstPage) {
                nativeAdFirstPage = preloaded
            } else {
                nativeAdLastPage = preloaded
            }

            renderNativeAd(preloaded)

        } else {

            AdManager.loadNativeAd(
                context = this,
                adUnitId = getString(R.string.native_onboarding_ad_id),

                onLoaded = { ad ->

                    if (isFinishing) {
                        ad.destroy()
                        return@loadNativeAd
                    }

                    if (isFirstPage) {
                        nativeAdFirstPage = ad
                    } else {
                        nativeAdLastPage = ad
                    }

                    if (binding.viewPager.currentItem == position) {
                        renderNativeAd(ad)
                    }
                },

                onFailed = {

                    if (
                        !isFinishing &&
                        binding.viewPager.currentItem == position
                    ) {
                        binding.shimmerNativeAd.stopShimmer()
                        binding.nativeAdContainer.visibility = View.GONE
                    }
                }
            )
        }
    }

    private fun renderNativeAd(ad: NativeAd) {
        binding.shimmerNativeAd.stopShimmer()
        binding.shimmerNativeAd.visibility = View.GONE
        binding.nativeAdView.root.visibility = View.VISIBLE
        NativeAdRenderer.bind(binding.nativeAdView, ad)
    }

    private fun finishOnboarding() {

        if (
            RemoteConfigManager.areAdsEnabled() &&
            RemoteConfigManager.isOnboardingInterstitialEnabled()
        ) {

            AdManager.showInterstitial(this) {
                if (isFinishing) return@showInterstitial

                openPermissionAccess()
            }

        } else {

            openPermissionAccess()
        }
    }

    private fun openPermissionAccess() {

        startActivity(
            Intent(
                this,
                PermissionAccessActivity::class.java
            )
        )

        finish()
    }

    override fun onDestroy() {
        nativeAdFirstPage?.destroy()
        nativeAdLastPage?.destroy()
        super.onDestroy()
    }
}
