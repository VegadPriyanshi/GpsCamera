package com.gps.camera.map.photo.gpscamera.features.grid

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF

/** Composes a captured photo and a map snapshot into a single grid image. */
object PhotoGridComposer {

    const val STYLE_TOP_BOTTOM = 0
    const val STYLE_BOTTOM_TOP = 1
    const val STYLE_LEFT_RIGHT = 2
    const val STYLE_RIGHT_LEFT = 3
    const val STYLE_MAP_LARGE_TOP = 4
    const val STYLE_MAP_SMALL_TOP = 5
    const val STYLE_COUNT = 6

    const val COLOR_WHITE = 0
    const val COLOR_BLACK = 1

    /** Returns (mapRect, photoRect) for [style] within a width x height canvas, gap already applied. */
    fun computeRects(style: Int, width: Int, height: Int): Pair<RectF, RectF> {
        val gap = width * 0.015f
        val w = width.toFloat()
        val h = height.toFloat()

        return when (style) {
            STYLE_BOTTOM_TOP -> {
                val half = (h - gap) / 2f
                RectF(0f, half + gap, w, h) to RectF(0f, 0f, w, half)
            }
            STYLE_LEFT_RIGHT -> {
                val half = (w - gap) / 2f
                RectF(0f, 0f, half, h) to RectF(half + gap, 0f, w, h)
            }
            STYLE_RIGHT_LEFT -> {
                val half = (w - gap) / 2f
                RectF(half + gap, 0f, w, h) to RectF(0f, 0f, half, h)
            }
            STYLE_MAP_LARGE_TOP -> {
                val mapHeight = h * 0.65f - gap / 2f
                RectF(0f, 0f, w, mapHeight) to RectF(0f, mapHeight + gap, w, h)
            }
            STYLE_MAP_SMALL_TOP -> {
                val mapHeight = h * 0.35f - gap / 2f
                RectF(0f, 0f, w, mapHeight) to RectF(0f, mapHeight + gap, w, h)
            }
            else -> {
                val half = (h - gap) / 2f
                RectF(0f, 0f, w, half) to RectF(0f, half + gap, w, h)
            }
        }
    }

    fun compose(
        mapBitmap: Bitmap?,
        photoBitmap: Bitmap,
        style: Int,
        dividerColor: Int,
        outputWidth: Int,
        outputHeight: Int
    ): Bitmap {
        val output = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawColor(if (dividerColor == COLOR_WHITE) Color.WHITE else Color.BLACK)

        val (mapRect, photoRect) = computeRects(style, outputWidth, outputHeight)
        drawCentered(canvas, mapBitmap, mapRect, Color.parseColor("#B0B8C4"))
        drawCentered(canvas, photoBitmap, photoRect, Color.parseColor("#B0B8C4"))

        return output
    }

    /** A lightweight abstract diagram (two colored blocks) used for the style-selector row. */
    fun composeStyleIcon(style: Int, dividerColor: Int, width: Int, height: Int): Bitmap {
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawColor(if (dividerColor == COLOR_WHITE) Color.WHITE else Color.BLACK)

        val (mapRect, photoRect) = computeRects(style, width, height)
        canvas.drawRect(mapRect, Paint().apply { color = Color.parseColor("#4DB6AC") })
        canvas.drawRect(photoRect, Paint().apply { color = Color.parseColor("#90A4AE") })
        return output
    }

    /** Draws [bitmap] into [rect] center-cropped, like ImageView's centerCrop scale type. */
    private fun drawCentered(canvas: Canvas, bitmap: Bitmap?, rect: RectF, fallbackColor: Int) {
        if (bitmap == null) {
            canvas.drawRect(rect, Paint().apply { color = fallbackColor })
            return
        }
        val srcRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
        val dstRatio = rect.width() / rect.height()

        val srcRect = if (srcRatio > dstRatio) {
            val cropWidth = (bitmap.height * dstRatio).toInt().coerceAtMost(bitmap.width)
            val left = (bitmap.width - cropWidth) / 2
            Rect(left, 0, left + cropWidth, bitmap.height)
        } else {
            val cropHeight = (bitmap.width / dstRatio).toInt().coerceAtMost(bitmap.height)
            val top = (bitmap.height - cropHeight) / 2
            Rect(0, top, bitmap.width, top + cropHeight)
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(bitmap, srcRect, rect, paint)
    }
}
