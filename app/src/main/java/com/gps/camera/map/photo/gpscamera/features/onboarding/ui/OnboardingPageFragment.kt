package com.gps.camera.map.photo.gpscamera.features.onboarding.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.gps.camera.map.photo.gpscamera.databinding.FragmentOnboardingGenericBinding
import com.gps.camera.map.photo.gpscamera.features.onboarding.data.OnboardingItem

class OnboardingPageFragment : Fragment() {

    private var _binding: FragmentOnboardingGenericBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingGenericBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val titleRes = arguments?.getInt(ARG_TITLE) ?: 0
        val descRes = arguments?.getInt(ARG_DESC) ?: 0
        val badgeRes = arguments?.getInt(ARG_BADGE) ?: 0
        val iconRes = arguments?.getInt(ARG_ICON) ?: 0

        binding.tvTitle.setText(titleRes)
        binding.tvDescription.setText(descRes)
        binding.badgeContainer.setBackgroundResource(badgeRes)
        binding.ivOnboardingIcon.setImageResource(iconRes)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_TITLE = "title"
        private const val ARG_DESC = "desc"
        private const val ARG_BADGE = "badge"
        private const val ARG_ICON = "icon"

        fun newInstance(item: OnboardingItem): OnboardingPageFragment {
            return OnboardingPageFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_TITLE, item.title)
                    putInt(ARG_DESC, item.description)
                    putInt(ARG_BADGE, item.badgeBackground)
                    putInt(ARG_ICON, item.icon)
                }
            }
        }
    }
}
