package com.gps.camera.map.photo.gpscamera.core.translation

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.gps.camera.map.photo.gpscamera.R

/**
 * Thin wrapper around ML Kit's on-device Translate API. A screen calls [prepare] once a
 * target language is picked — this downloads the on-device model if it isn't already
 * present — then can call [translate] or [translateAllViews] afterward.
 */
object TranslationManager {

    private var activeTranslator: Translator? = null
    private var activeLanguageCode: String? = null
    private var isDownloading = false
    private val pendingTranslations = mutableListOf<() -> Unit>()

    /** Downloads (if needed) and activates the on-device model for [targetLanguageCode]. */
    fun prepare(targetLanguageCode: String, onReady: () -> Unit, onError: (Exception) -> Unit) {
        if (targetLanguageCode == TranslateLanguage.ENGLISH) {
            activeTranslator?.close()
            activeTranslator = null
            activeLanguageCode = TranslateLanguage.ENGLISH
            onReady()
            return
        }
        if (targetLanguageCode == activeLanguageCode && activeTranslator != null && !isDownloading) {
            onReady()
            return
        }

        isDownloading = true
        activeLanguageCode = targetLanguageCode
        activeTranslator?.close()

        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(targetLanguageCode)
            .build()
        val translator = Translation.getClient(options)
        activeTranslator = translator

        translator.downloadModelIfNeeded(DownloadConditions.Builder().build())
            .addOnSuccessListener {
                isDownloading = false
                onReady()
                flushPending()
            }
            .addOnFailureListener { error ->
                isDownloading = false
                activeTranslator = null
                activeLanguageCode = null
                onError(error)
                flushPending()
            }
    }

    /**
     * Translates [text] using the translator activated by the last [prepare] call. If a
     * download is still in progress, the request is queued and replayed once it finishes.
     */
    fun translate(text: String, onResult: (String) -> Unit) {
        if (text.isBlank() || activeLanguageCode == null || activeLanguageCode == TranslateLanguage.ENGLISH) {
            onResult(text)
            return
        }
        if (isDownloading) {
            pendingTranslations.add { translate(text, onResult) }
            return
        }
        val translator = activeTranslator
        if (translator == null) {
            onResult(text)
            return
        }
        translator.translate(text)
            .addOnSuccessListener { onResult(it) }
            .addOnFailureListener { onResult(text) }
    }

    /**
     * Translates a single [TextView], remembering its original English text in a view tag
     * so repeated calls (e.g. on every screen resume) always translate from the true
     * original rather than re-translating an already-translated string.
     */
    fun translateView(view: TextView) {
        val original = view.getTag(R.id.translation_original_text) as? String
            ?: view.text?.toString()?.also { view.setTag(R.id.translation_original_text, it) }
        if (original.isNullOrBlank()) return
        translate(original) { translated -> view.text = translated }
    }

    /**
     * Recursively translates every [TextView] under [root] — skipping [NativeAdView]
     * subtrees entirely, since that text is ad creative supplied by the ad network, not
     * app copy, and isn't ours to translate.
     */
    fun translateAllViews(root: View) {
        if (root is NativeAdView) return
        if (root is TextView) {
            translateView(root)
        } else if (root is ViewGroup) {
            for (i in 0 until root.childCount) translateAllViews(root.getChildAt(i))
        }
    }

    private fun flushPending() {
        val pending = ArrayList(pendingTranslations)
        pendingTranslations.clear()
        pending.forEach { it() }
    }

    fun release() {
        activeTranslator?.close()
        activeTranslator = null
        activeLanguageCode = null
    }
}
