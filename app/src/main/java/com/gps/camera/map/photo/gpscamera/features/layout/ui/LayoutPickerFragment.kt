package com.gps.camera.map.photo.gpscamera.features.layout.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.gms.ads.nativead.NativeAd
import com.gps.camera.map.photo.gpscamera.R
import com.gps.camera.map.photo.gpscamera.core.ads.AdManager
import com.gps.camera.map.photo.gpscamera.core.ads.NativeAdRenderer
import com.gps.camera.map.photo.gpscamera.core.ads.NativePlacement
import com.gps.camera.map.photo.gpscamera.core.util.applyVerticalInsetsAsPadding
import com.gps.camera.map.photo.gpscamera.databinding.FragmentLayoutPickerBinding
import com.gps.camera.map.photo.gpscamera.features.layout.adapter.LayoutOptionAdapter
import com.gps.camera.map.photo.gpscamera.features.settings.data.StampPreferences
import com.gps.camera.map.photo.gpscamera.features.settings.ui.SettingsFragment
import com.gps.camera.map.photo.gpscamera.features.stamp.PhotoStampRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.gps.camera.map.photo.gpscamera.core.config.RemoteConfigManager

class LayoutPickerFragment : Fragment() {

    private var _binding: FragmentLayoutPickerBinding? = null
    private val binding get() = _binding!!

    private lateinit var preferences: StampPreferences
    private lateinit var adapter: LayoutOptionAdapter
    private var nativeAd: NativeAd? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLayoutPickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        preferences = StampPreferences(requireContext())
        binding.root.applyVerticalInsetsAsPadding()

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        binding.btnDone.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        binding.btnSettingsLayout.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(com.gps.camera.map.photo.gpscamera.R.id.fragmentContainer, SettingsFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)

        viewLifecycleOwner.lifecycleScope.launch {
            val previews = withContext(Dispatchers.Default) { generatePreviews() }
            if (_binding == null) return@launch

            adapter = LayoutOptionAdapter(previews, preferences.stampLayout) { index ->
                preferences.stampLayout = index
                adapter.setSelected(index)
            }
            binding.recyclerView.adapter = adapter
        }

        loadNativeAd()
    }

    private fun loadNativeAd() {

        // ---------------------------------------------------------
        // Remote Config: Layout Native Ad
        // ---------------------------------------------------------

        if (
            !RemoteConfigManager.areAdsEnabled() ||
            !RemoteConfigManager.isLayoutEnabled()
        ) {
            binding.nativeAdContainer.visibility = View.GONE
            return
        }

        // ---------------------------------------------------------
        // Use preloaded Layout Native Ad
        // ---------------------------------------------------------

        val preloaded = AdManager.consumeNativeAd(
            NativePlacement.LAYOUT
        )

        if (preloaded != null) {
            renderNativeAd(preloaded)
            return
        }

        // ---------------------------------------------------------
        // No preloaded ad → Load normally
        // ---------------------------------------------------------

        binding.nativeAdContainer.visibility = View.VISIBLE
        binding.shimmerNativeAd.visibility = View.VISIBLE
        binding.shimmerNativeAd.startShimmer()
        binding.nativeAdView.root.visibility = View.GONE

        AdManager.loadNativeAd(
            context = requireContext(),
            adUnitId = getString(R.string.native_layout_ad_id),

            onLoaded = { ad ->
                if (_binding != null) {
                    renderNativeAd(ad)
                } else {
                    ad.destroy()
                }
            },

            onFailed = {
                if (_binding != null) {
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
        binding.nativeAdView.root.visibility = View.VISIBLE
        NativeAdRenderer.bind(binding.nativeAdView, ad)
    }

    private fun generatePreviews(): List<Bitmap> {
        val placeholder = Bitmap.createBitmap(480, 640, Bitmap.Config.ARGB_8888).apply {
            Canvas(this).drawColor(Color.parseColor("#B0B8C4"))
        }

        val addressLine = if (preferences.showAddress) {
            "308, Raiya Rd, Chandan Park, Rajkot, Gujarat 360005, India"
        } else {
            null
        }
        val dateTimeLine = buildSampleDateTimeLine()
        val latLongLine = if (preferences.showLatLong) {
            "Lat: 22.300945   Long: 70.762818"
        } else {
            null
        }

        return (0 until StampPreferences.LAYOUT_COUNT).map { layoutStyle ->
            PhotoStampRenderer.applyStamp(
                source = placeholder,
                addressLine = addressLine,
                dateTimeLine = dateTimeLine,
                latLongLine = latLongLine,
                mapThumbnail = null,
                layoutStyle = layoutStyle
            )
        }
    }

    private fun buildSampleDateTimeLine(): String? {
        val showDate = preferences.showDate
        val showTime = preferences.showTime
        if (!showDate && !showTime) return null

        val timeText = if (preferences.use24HourFormat) "17:30" else "05:30 PM"
        return when {
            showDate && showTime -> "20 Aug 2026, $timeText"
            showDate -> "20 Aug 2026"
            else -> timeText
        }
    }

    override fun onDestroyView() {
        nativeAd?.destroy()
        super.onDestroyView()
        _binding = null
    }
}
