package com.gps.camera.map.photo.gpscamera.features.settings.data

import android.content.Context

class StampPreferences(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var showAddress: Boolean
        get() = prefs.getBoolean(KEY_SHOW_ADDRESS, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_ADDRESS, value).apply()

    var showDate: Boolean
        get() = prefs.getBoolean(KEY_SHOW_DATE, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_DATE, value).apply()

    var showTime: Boolean
        get() = prefs.getBoolean(KEY_SHOW_TIME, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_TIME, value).apply()

    var showLatLong: Boolean
        get() = prefs.getBoolean(KEY_SHOW_LAT_LONG, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_LAT_LONG, value).apply()

    var use24HourFormat: Boolean
        get() = prefs.getBoolean(KEY_USE_24H, false)
        set(value) = prefs.edit().putBoolean(KEY_USE_24H, value).apply()

    var stampLayout: Int
        get() = prefs.getInt(KEY_STAMP_LAYOUT, 0)
        set(value) = prefs.edit().putInt(KEY_STAMP_LAYOUT, value).apply()

    companion object {
        private const val PREFS_NAME = "stamp_settings"
        private const val KEY_SHOW_ADDRESS = "show_address"
        private const val KEY_SHOW_DATE = "show_date"
        private const val KEY_SHOW_TIME = "show_time"
        private const val KEY_SHOW_LAT_LONG = "show_lat_long"
        private const val KEY_USE_24H = "use_24h"
        private const val KEY_STAMP_LAYOUT = "stamp_layout"

        const val LAYOUT_COUNT = 8
    }
}
