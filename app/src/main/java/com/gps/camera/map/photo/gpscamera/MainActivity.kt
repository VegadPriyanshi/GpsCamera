package com.gps.camera.map.photo.gpscamera

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.gps.camera.map.photo.gpscamera.core.translation.TranslationManager
import com.gps.camera.map.photo.gpscamera.features.home.ui.HomeFragment

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Every screen reachable from Home lives in fragmentContainer, so translating
        // each fragment's view as it's created covers the whole app in one place —
        // no screen needs to remember to call TranslationManager itself.
        supportFragmentManager.registerFragmentLifecycleCallbacks(
            object : FragmentManager.FragmentLifecycleCallbacks() {
                override fun onFragmentViewCreated(
                    fm: FragmentManager,
                    fragment: Fragment,
                    view: View,
                    savedInstanceState: Bundle?
                ) {
                    TranslationManager.translateAllViews(view)
                }
            },
            true
        )

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, HomeFragment())
                .commit()
        }
    }
}
