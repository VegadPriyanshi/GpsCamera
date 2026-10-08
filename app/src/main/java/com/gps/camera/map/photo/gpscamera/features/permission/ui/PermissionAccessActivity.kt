package com.gps.camera.map.photo.gpscamera.features.permission.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.gps.camera.map.photo.gpscamera.MainActivity
import com.gps.camera.map.photo.gpscamera.R
import com.gps.camera.map.photo.gpscamera.databinding.ActivityPermissionAccessBinding

class PermissionAccessActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPermissionAccessBinding

    private val cameraPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {
            updatePermissionUi()
        }

    private val locationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {
            updatePermissionUi()
        }

    private val photosPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {
            updatePermissionUi()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPermissionAccessBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClicks()
        updatePermissionUi()
    }

    private fun setupClicks() {

        binding.cameraPermissionRow.setOnClickListener {
            requestCameraPermission()
        }
        binding.switchCamera.setOnClickListener {

            if (binding.switchCamera.isChecked) {
                requestCameraPermission()
            } else {
                openPermissionSettings()
            }
        }

        binding.locationPermissionRow.setOnClickListener {
            requestLocationPermission()
        }

        binding.switchLocation.setOnClickListener {

            if (binding.switchLocation.isChecked) {
                requestLocationPermission()
            } else {
                openPermissionSettings()
            }
        }

        binding.photosPermissionRow.setOnClickListener {
            requestPhotosPermission()
        }

        binding.switchPhotos.setOnClickListener {

            if (binding.switchPhotos.isChecked) {
                requestPhotosPermission()
            } else {
                openPermissionSettings()
            }
        }

        binding.btnContinue.setOnClickListener {
            continueToMain()
        }
    }

    private fun requestCameraPermission() {

        if (!hasCameraPermission()) {
            cameraPermissionLauncher.launch(
                Manifest.permission.CAMERA
            )
        }
    }

    private fun requestLocationPermission() {

        if (hasFineLocationPermission()) {
            updatePermissionUi()
            return
        }

        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun requestPhotosPermission() {

        val permission = getPhotosPermission()

        if (permission == null) {
            updatePermissionUi()
            return
        }

        if (!hasPermission(permission)) {
            photosPermissionLauncher.launch(permission)
        }
    }

    private fun getPhotosPermission(): String? {

        return when {

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                Manifest.permission.READ_MEDIA_IMAGES
            }

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }

            else -> {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
        }
    }

    private fun hasCameraPermission(): Boolean {
        return hasPermission(Manifest.permission.CAMERA)
    }

    private fun hasFineLocationPermission(): Boolean {
        return hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun updatePermissionUi() {

        binding.switchCamera.isChecked =
            hasCameraPermission()

        binding.switchLocation.isChecked =
            hasFineLocationPermission()

        val photosPermission = getPhotosPermission()

        binding.switchPhotos.isChecked =
            photosPermission == null ||
                    hasPermission(photosPermission)
    }

    private fun updateAction(
        view: TextView,
        granted: Boolean
    ) {

        if (granted) {
            view.text = getString(R.string.permission_allowed)
            view.setTextColor(
                ContextCompat.getColor(
                    this,
                    android.R.color.holo_green_dark
                )
            )
        } else {
            view.text = getString(R.string.permission_allow)
            view.setTextColor(
                ContextCompat.getColor(
                    this,
                    R.color.accent_blue
                )
            )
        }
    }


    private fun openPermissionSettings() {

        val intent = Intent(
            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        )

        intent.data = android.net.Uri.parse(
            "package:$packageName"
        )

        startActivity(intent)
    }

    private fun continueToMain() {

        startActivity(
            Intent(
                this,
                MainActivity::class.java
            )
        )

        finish()
    }
}