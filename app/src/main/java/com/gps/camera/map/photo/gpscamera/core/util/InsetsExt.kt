package com.gps.camera.map.photo.gpscamera.core.util

import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

/** Adds the system bars' bottom inset to this view's existing bottom padding. */
fun View.applyBottomInsetAsPadding() {
    val initialPadding = paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        view.updatePadding(bottom = initialPadding + systemBars.bottom)
        insets
    }
}

/** Adds the system bars' bottom inset to this view's existing bottom margin. */
fun View.applyBottomInsetAsMargin() {
    val params = layoutParams as ViewGroup.MarginLayoutParams
    val initialMargin = params.bottomMargin
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        val marginParams = view.layoutParams as ViewGroup.MarginLayoutParams
        marginParams.bottomMargin = initialMargin + systemBars.bottom
        view.layoutParams = marginParams
        insets
    }
}

/** Adds the system bars' top inset to this view's existing top padding. */
fun View.applyTopInsetAsPadding() {
    val initialPadding = paddingTop
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        view.updatePadding(top = initialPadding + systemBars.top)
        insets
    }
}

/**
 * Adds the system bars' top and bottom insets to this view's existing top/bottom padding,
 * in a single listener. Note: ViewCompat.setOnApplyWindowInsetsListener only allows one
 * listener per view — calling applyTopInsetAsPadding() and applyBottomInsetAsPadding()
 * separately on the same view means the second call silently replaces the first. Use this
 * combined function whenever a view needs both.
 */
fun View.applyVerticalInsetsAsPadding() {
    val initialPaddingTop = paddingTop
    val initialPaddingBottom = paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        view.updatePadding(top = initialPaddingTop + systemBars.top, bottom = initialPaddingBottom + systemBars.bottom)
        insets
    }
}

/** Adds the system bars' top inset to this view's existing top margin. */
fun View.applyTopInsetAsMargin() {
    val params = layoutParams as ViewGroup.MarginLayoutParams
    val initialMargin = params.topMargin
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        val marginParams = view.layoutParams as ViewGroup.MarginLayoutParams
        marginParams.topMargin = initialMargin + systemBars.top
        view.layoutParams = marginParams
        insets
    }
}
