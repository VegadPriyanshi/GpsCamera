package com.gps.camera.map.photo.gpscamera.features.gallery.ui

import android.app.RecoverableSecurityException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.gps.camera.map.photo.gpscamera.R
import com.gps.camera.map.photo.gpscamera.core.util.applyTopInsetAsPadding
import com.gps.camera.map.photo.gpscamera.databinding.FragmentPhotoDetailBinding

class PhotoDetailFragment : Fragment() {

    private var _binding: FragmentPhotoDetailBinding? = null
    private val binding get() = _binding!!

    private val photoUri: Uri by lazy { requireArguments().getString(ARG_PHOTO_URI)!!.toUri() }

    private val deleteRequestLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                onDeleteSuccess()
            } else {
                showDeleteFailed()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPhotoDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Glide.with(binding.ivPhoto).load(photoUri).into(binding.ivPhoto)
        binding.appBar.applyTopInsetAsPadding()

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnDelete.setOnClickListener {
            confirmDelete()
        }

        binding.btnShare.setOnClickListener {
            sharePhoto()
        }
    }

    private fun confirmDelete() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_photo_confirm_title)
            .setMessage(R.string.delete_photo_confirm_message)
            .setPositiveButton(R.string.delete) { _, _ -> performDelete() }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun performDelete() {
        try {
            val rows = requireContext().contentResolver.delete(photoUri, null, null)
            if (rows > 0) onDeleteSuccess() else showDeleteFailed()
        } catch (e: SecurityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                handleRecoverableDelete(e)
            } else {
                showDeleteFailed()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun handleRecoverableDelete(e: SecurityException) {
        val recoverable = e as? RecoverableSecurityException
        if (recoverable == null) {
            showDeleteFailed()
            return
        }
        val intentSender = recoverable.userAction.actionIntent.intentSender
        deleteRequestLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
    }

    private fun onDeleteSuccess() {
        Toast.makeText(requireContext(), R.string.photo_deleted, Toast.LENGTH_SHORT).show()
        parentFragmentManager.popBackStack()
    }

    private fun showDeleteFailed() {
        Toast.makeText(requireContext(), R.string.delete_failed, Toast.LENGTH_SHORT).show()
    }

    private fun sharePhoto() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, photoUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.share_photo)))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_PHOTO_URI = "photo_uri"

        fun newInstance(photoUri: Uri): PhotoDetailFragment =
            PhotoDetailFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PHOTO_URI, photoUri.toString())
                }
            }
    }
}
