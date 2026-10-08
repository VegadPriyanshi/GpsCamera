package com.gps.camera.map.photo.gpscamera.features.onboarding.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.gps.camera.map.photo.gpscamera.features.onboarding.data.OnboardingItem

class OnboardingAdapter(
    activity: FragmentActivity,
    private val items: List<OnboardingItem>
) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = items.size

    override fun createFragment(position: Int): Fragment {
        val item = items[position]
        return if (item.isFullNativeAd) {
            FullNativeAdFragment()
        } else {
            OnboardingPageFragment.newInstance(item)
        }
    }
}
