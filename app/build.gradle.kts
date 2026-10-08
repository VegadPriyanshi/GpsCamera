plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.gps.camera.map.photo.gpscamera"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.gps.camera.map.photo.gpscamera"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {

            resValue(type = "string", name = "banner_splash_ad_id", value = "ca-app-pub-3940256099942544/6300978111")
            resValue(type = "string", name = "interstitial_splash_ad_id", value = "ca-app-pub-3940256099942544/1033173712")
            resValue(type = "string", name = "interstitial_onboarding_ad_id", value = "ca-app-pub-3940256099942544/1033173712")
            resValue(type = "string", name = "native_language_ad_id", value = "ca-app-pub-3940256099942544/2247696110")
            resValue(type = "string", name = "native_language_ad_2_id", value = "ca-app-pub-3940256099942544/2247696110")
            resValue(type = "string", name = "native_onboarding_ad_id", value = "ca-app-pub-3940256099942544/2247696110")
            resValue(type = "string", name = "native_onboarding_full_ad_id", value = "ca-app-pub-3940256099942544/2247696110")
            resValue(type = "string", name = "native_layout_ad_id", value = "ca-app-pub-3940256099942544/2247696110")
            resValue(type = "string", name = "native_home_ad_id", value = "ca-app-pub-3940256099942544/2247696110")
            resValue(type = "string", name = "native_gallery_ad_id", value = "ca-app-pub-3940256099942544/2247696110")
            resValue(type = "string", name = "native_photo_saved_ad_id", value = "ca-app-pub-3940256099942544/2247696110")
            resValue(type = "string", name = "interstitial_home_ad_id", value = "ca-app-pub-3940256099942544/1033173712")
            resValue(type = "string", name = "banner_uninstall_ad_id", value = "ca-app-pub-3940256099942544/6300978111")
            resValue(type = "string", name = "interstitial_uninstall_ad_id", value = "ca-app-pub-3940256099942544/1033173712")
            resValue(type = "string", name = "native_uninstall_intro_ad_id", value = "ca-app-pub-3940256099942544/2247696110")
            resValue(type = "string", name = "native_uninstall_reason_ad_id",value = "ca-app-pub-3940256099942544/2247696110")

        }
        release {
            isMinifyEnabled = false

            resValue(type = "string", name = "banner_splash_ad_id", value = "ca-app-pub-3940256099942544/6300978111")
            resValue(type = "string", name = "interstitial_splash_ad_id", value = "ca-app-pub-3940256099942544/1033173712")
            resValue(type = "string", name = "interstitial_onboarding_ad_id", value = "ca-app-pub-3940256099942544/1033173712")
            resValue(type = "string", name = "native_language_ad_id", value = "ca-app-pub-3940256099942544/2247696110")
            resValue(type = "string", name = "native_language_ad_2_id", value = "ca-app-pub-3940256099942544/2247696110")
            resValue(type = "string", name = "native_onboarding_ad_id", value = "ca-app-pub-3940256099942544/2247696110")
            resValue(type = "string", name = "native_onboarding_full_ad_id", value = "ca-app-pub-3940256099942544/2247696110")
            resValue(type = "string", name = "native_layout_ad_id", value = "ca-app-pub-3940256099942544/2247696110")
            resValue(type = "string", name = "native_home_ad_id", value = "ca-app-pub-3940256099942544/2247696110")
            resValue(type = "string", name = "native_gallery_ad_id", value = "ca-app-pub-3940256099942544/2247696110")
            resValue(type = "string", name = "native_photo_saved_ad_id", value = "ca-app-pub-3940256099942544/2247696110")
            resValue(type = "string", name = "interstitial_home_ad_id", value = "ca-app-pub-3940256099942544/1033173712")
            resValue(type = "string", name = "banner_uninstall_ad_id", value = "ca-app-pub-3940256099942544/6300978111")
            resValue(type = "string", name = "interstitial_uninstall_ad_id", value = "ca-app-pub-3940256099942544/1033173712")
            resValue(type = "string", name = "native_uninstall_intro_ad_id", value = "ca-app-pub-3940256099942544/2247696110")
            resValue(type = "string", name = "native_uninstall_reason_ad_id",value = "ca-app-pub-3940256099942544/2247696110")

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        viewBinding = true
        resValues = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    implementation(libs.play.services.location)
    implementation(libs.play.services.ads)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.glide)
    implementation(libs.androidx.recyclerview)

    implementation(libs.osmdroid.android)
    implementation(libs.androidx.exifinterface)

    implementation(libs.shimmer)
    implementation(libs.mlkit.translate)
    implementation(libs.dotsindicator)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.viewpager2)
    implementation(platform("com.google.firebase:firebase-bom:34.17.0"))
    implementation("com.google.firebase:firebase-config")
}