package com.gps.camera.map.photo.gpscamera.features.settings.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.gps.camera.map.photo.gpscamera.core.util.applyVerticalInsetsAsPadding
import com.gps.camera.map.photo.gpscamera.databinding.FragmentSettingsBinding
import com.gps.camera.map.photo.gpscamera.features.settings.data.StampPreferences

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var preferences: StampPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        preferences = StampPreferences(requireContext())
        binding.root.applyVerticalInsetsAsPadding()

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.switchAddress.isChecked = preferences.showAddress
        binding.switchDate.isChecked = preferences.showDate
        binding.switchTime.isChecked = preferences.showTime
        binding.switch24Hour.isChecked = preferences.use24HourFormat
        binding.switchLatLong.isChecked = preferences.showLatLong

        binding.switchAddress.setOnCheckedChangeListener { _, checked ->
            preferences.showAddress = checked
        }
        binding.switchDate.setOnCheckedChangeListener { _, checked ->
            preferences.showDate = checked
        }
        binding.switchTime.setOnCheckedChangeListener { _, checked ->
            preferences.showTime = checked
        }
        binding.switch24Hour.setOnCheckedChangeListener { _, checked ->
            preferences.use24HourFormat = checked
        }
        binding.switchLatLong.setOnCheckedChangeListener { _, checked ->
            preferences.showLatLong = checked
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
