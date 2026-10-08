package com.gps.camera.map.photo.gpscamera

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.gps.camera.map.photo.gpscamera.core.config.RemoteConfigManager

class GpsCameraApp : Application() {

    override fun onCreate() {
        super.onCreate()

        MobileAds.initialize(this)

        RemoteConfigManager.initialize()

        RemoteConfigManager.fetchAndActivate()
    }
}