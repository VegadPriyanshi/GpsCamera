package com.example.phonecleaner.core.prefs

import android.content.Context



object AppPrefs {

    private const val PREFS_NAME =
        "phone_cleaner_settings"


    private const val KEY_CLEANING_REMINDER_ENABLED =
        "cleaning_reminder_enabled"


    // =========================================================
    // Shared Preferences
    // =========================================================

    private fun prefs(
        context: Context
    ) =
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )


    // =========================================================
    // Language
    // =========================================================

    private const val KEY_SELECTED_LANGUAGE =
        "selected_language"

    private const val KEY_NOTIFICATION_DEFAULTS_MIGRATION =
        "notification_defaults_migration_v1"


    var Context.selectedLanguageCode: String?
        get() =
            prefs(this)
                .getString(
                    KEY_SELECTED_LANGUAGE,
                    null
                )

        set(value) {

            prefs(this)
                .edit()
                .putString(
                    KEY_SELECTED_LANGUAGE,
                    value
                )
                .apply()
        }


    // =========================================================
    // Onboarding
    // =========================================================

    private const val KEY_ONBOARDING_COMPLETED =
        "onboarding_completed"


    fun isOnboardingCompleted(
        context: Context
    ): Boolean {

        return prefs(context)
            .getBoolean(
                KEY_ONBOARDING_COMPLETED,
                false
            )
    }


    fun setOnboardingCompleted(
        context: Context,
        completed: Boolean
    ) {

        prefs(context)
            .edit()
            .putBoolean(
                KEY_ONBOARDING_COMPLETED,
                completed
            )
            .apply()
    }


    // =========================================================
    // Auto-clean Schedule
    // =========================================================

    private const val KEY_AUTO_CLEAN_ENABLED =
        "auto_clean_enabled"

    // ... KEEP THE REST OF YOUR EXISTING AppPrefs.kt EXACTLY AS IT IS
}