package com.gps.camera.map.photo.gpscamera.features.language.data

import com.google.mlkit.nl.translate.TranslateLanguage

data class Language(
    val displayName: String,
    val nativeName: String,
    val translateCode: String
)

object Languages {
    val ALL = listOf(
        Language("English", "English", TranslateLanguage.ENGLISH),
        Language("Hindi", "हिन्दी", TranslateLanguage.HINDI),
        Language("Spanish", "Español", TranslateLanguage.SPANISH),
        Language("French", "Français", TranslateLanguage.FRENCH),
        Language("German", "Deutsch", TranslateLanguage.GERMAN),
        Language("Portuguese", "Português", TranslateLanguage.PORTUGUESE),
        Language("Russian", "Русский", TranslateLanguage.RUSSIAN),
        Language("Arabic", "العربية", TranslateLanguage.ARABIC),
        Language("Chinese", "中文", TranslateLanguage.CHINESE),
        Language("Japanese", "日本語", TranslateLanguage.JAPANESE)
    )
}
