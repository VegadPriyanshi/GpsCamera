package com.gps.camera.map.photo.gpscamera.features.onboarding.data

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

data class OnboardingItem(
    @StringRes val title: Int = 0,
    @StringRes val description: Int = 0,
    @DrawableRes val badgeBackground: Int = 0,
    @DrawableRes val icon: Int = 0,
    @StringRes val buttonText: Int = com.gps.camera.map.photo.gpscamera.R.string.onboarding_next,
    val showNativeAd: Boolean = false,
    val isFullNativeAd: Boolean = false
)
