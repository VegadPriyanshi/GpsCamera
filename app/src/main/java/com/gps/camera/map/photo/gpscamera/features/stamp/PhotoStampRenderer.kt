package com.gps.camera.map.photo.gpscamera.features.stamp

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils

object PhotoStampRenderer {

    const val LAYOUT_CLASSIC = 0
    const val LAYOUT_PILL_DATE = 1
    const val LAYOUT_RIBBON_CORNER = 2
    const val LAYOUT_LIGHT_CARD = 3
    const val LAYOUT_CENTER_PIN = 4
    const val LAYOUT_SPLIT_MAP = 5
    const val LAYOUT_MINIMAL = 6
    const val LAYOUT_TOP_STRIP = 7

    private class TextBlock(val height: Float, val draw: (Canvas, Float) -> Unit)

    fun applyStamp(
        source: Bitmap,
        addressLine: String?,
        dateTimeLine: String?,
        latLongLine: String?,
        mapThumbnail: Bitmap?,
        layoutStyle: Int = LAYOUT_CLASSIC
    ): Bitmap {
        val output = source.copy(Bitmap.Config.ARGB_8888, true)
        if (addressLine == null && dateTimeLine == null && latLongLine == null) {
            return output
        }

        val canvas = Canvas(output)
        val ctx = StampContext(canvas, output.width, output.height)

        when (layoutStyle) {
            LAYOUT_PILL_DATE -> renderPillDate(ctx, addressLine, dateTimeLine, latLongLine)
            LAYOUT_RIBBON_CORNER -> renderRibbonCorner(ctx, addressLine, dateTimeLine, latLongLine)
            LAYOUT_LIGHT_CARD -> renderLightCard(ctx, addressLine, dateTimeLine, latLongLine, mapThumbnail)
            LAYOUT_CENTER_PIN -> renderCenterPin(ctx, addressLine, dateTimeLine, latLongLine)
            LAYOUT_SPLIT_MAP -> renderSplitMap(ctx, addressLine, dateTimeLine, latLongLine, mapThumbnail)
            LAYOUT_MINIMAL -> renderMinimal(ctx, addressLine, dateTimeLine, latLongLine)
            LAYOUT_TOP_STRIP -> renderTopStrip(ctx, addressLine, dateTimeLine, latLongLine, mapThumbnail)
            else -> renderClassic(ctx, addressLine, dateTimeLine, latLongLine, mapThumbnail)
        }

        return output
    }

    private class StampContext(val canvas: Canvas, val width: Int, val height: Int) {
        val padding = width * 0.04f
        val addressTextSize = width * 0.036f
        val detailTextSize = width * 0.029f
        val blockSpacing = detailTextSize * 0.5f

        val addressPaint = TextPaint().apply {
            color = Color.WHITE
            textSize = addressTextSize
            isAntiAlias = true
            isFakeBoldText = true
        }
        val dateTimePaint = TextPaint().apply {
            color = Color.parseColor("#F4C775")
            textSize = detailTextSize
            isAntiAlias = true
            isFakeBoldText = true
        }
        val latLongPaint = TextPaint().apply {
            color = Color.parseColor("#CFCFCF")
            textSize = detailTextSize
            isAntiAlias = true
        }
    }

    // ---- Layout 0: Classic — badge/map left, stacked text right, bottom gradient ----
    private fun renderClassic(
        ctx: StampContext,
        addressLine: String?,
        dateTimeLine: String?,
        latLongLine: String?,
        mapThumbnail: Bitmap?
    ) {
        val badgeRadius = ctx.width * 0.045f
        val iconSize = if (mapThumbnail != null) ctx.width * 0.16f else badgeRadius * 2f
        val textStartX = ctx.padding + iconSize + ctx.width * 0.03f
        val textWidth = (ctx.width - textStartX - ctx.padding).toInt()
        val verticalPadding = ctx.detailTextSize * 1.1f

        val blocks = buildTextBlocks(ctx, addressLine, dateTimeLine, latLongLine, textStartX, textWidth)
        if (blocks.isEmpty()) return

        val totalTextHeight = blocks.sumOf { it.height.toDouble() }.toFloat() + ctx.blockSpacing * (blocks.size - 1)
        val overlayHeight = maxOf(totalTextHeight, iconSize) + verticalPadding * 2f
        val overlayTop = ctx.height - overlayHeight

        drawGradientBand(ctx.canvas, ctx.width, overlayTop, ctx.height.toFloat())
        drawBlocks(ctx.canvas, blocks, overlayTop, overlayHeight, totalTextHeight, ctx.blockSpacing)

        val iconCenterY = overlayTop + overlayHeight / 2f
        if (mapThumbnail != null) {
            drawMapThumbnail(ctx.canvas, mapThumbnail, ctx.padding, iconCenterY - iconSize / 2f, iconSize)
        } else {
            drawPinBadge(ctx.canvas, ctx.padding + badgeRadius, iconCenterY, badgeRadius)
        }
    }

    // ---- Layout 1: Pill Date — solid bar, colored date/time pill top-left, text-only ----
    private fun renderPillDate(
        ctx: StampContext,
        addressLine: String?,
        dateTimeLine: String?,
        latLongLine: String?
    ) {
        val textWidth = (ctx.width - ctx.padding * 2).toInt()
        val pillHeight = ctx.detailTextSize * 1.9f
        val addressLayout = addressLine?.let { buildAddressLayout(it, ctx.addressPaint, textWidth) }

        var contentHeight = 0f
        if (dateTimeLine != null) contentHeight += pillHeight + ctx.blockSpacing * 1.5f
        addressLayout?.let { contentHeight += it.height + ctx.blockSpacing }
        if (latLongLine != null) contentHeight += ctx.detailTextSize * 1.15f

        val overlayHeight = contentHeight + ctx.padding * 2f
        val overlayTop = ctx.height - overlayHeight
        ctx.canvas.drawRect(0f, overlayTop, ctx.width.toFloat(), ctx.height.toFloat(), solidPaint("#CC0D1B2A"))

        var currentY = overlayTop + ctx.padding

        if (dateTimeLine != null) {
            val pillPaint = solidPaint("#2E7D32")
            val pillTextWidth = ctx.dateTimePaint.measureText(dateTimeLine)
            val pillRect = RectF(ctx.padding, currentY, ctx.padding + pillTextWidth + ctx.padding, currentY + pillHeight)
            ctx.canvas.drawRoundRect(pillRect, pillRect.height() / 2f, pillRect.height() / 2f, pillPaint)
            ctx.canvas.drawText(
                dateTimeLine,
                pillRect.left + ctx.padding / 2f,
                pillRect.top + pillRect.height() * 0.66f,
                TextPaint(ctx.dateTimePaint).apply { color = Color.WHITE }
            )
            currentY = pillRect.bottom + ctx.blockSpacing * 1.5f
        }

        addressLayout?.let { layout ->
            ctx.canvas.save()
            ctx.canvas.translate(ctx.padding, currentY)
            layout.draw(ctx.canvas)
            ctx.canvas.restore()
            currentY += layout.height + ctx.blockSpacing
        }

        latLongLine?.let { text ->
            ctx.canvas.drawText(text, ctx.padding, currentY + ctx.detailTextSize, ctx.latLongPaint)
        }
    }

    // ---- Layout 2: Ribbon Corner — accent time ribbon top-right, pin bottom-left ----
    private fun renderRibbonCorner(
        ctx: StampContext,
        addressLine: String?,
        dateTimeLine: String?,
        latLongLine: String?
    ) {
        val badgeRadius = ctx.width * 0.04f
        val ribbonAllowance = ctx.detailTextSize * 2.6f
        val textStartX = ctx.padding + badgeRadius * 2f + ctx.width * 0.03f
        val textWidth = (ctx.width - textStartX - ctx.padding).toInt()

        val addressLayout = addressLine?.let { buildAddressLayout(it, ctx.addressPaint, textWidth) }
        var textFlowHeight = ribbonAllowance
        addressLayout?.let { textFlowHeight += it.height + ctx.blockSpacing }
        if (latLongLine != null) textFlowHeight += ctx.detailTextSize * 1.15f

        val overlayHeight = maxOf(textFlowHeight, badgeRadius * 2f + ctx.padding * 2f) + ctx.padding
        val overlayTop = ctx.height - overlayHeight
        ctx.canvas.drawRect(0f, overlayTop, ctx.width.toFloat(), ctx.height.toFloat(), solidPaint("#CC1A1A1A"))

        dateTimeLine?.let { text ->
            val ribbonPaint = solidPaint("#C62828")
            val textPaint = TextPaint(ctx.detailTimePaintBold()).apply { color = Color.WHITE }
            val ribbonTextWidth = textPaint.measureText(text)
            val ribbonRect = RectF(
                ctx.width - ribbonTextWidth - ctx.padding * 2, overlayTop,
                ctx.width.toFloat(), overlayTop + ctx.detailTextSize * 2f
            )
            ctx.canvas.drawRect(ribbonRect, ribbonPaint)
            ctx.canvas.drawText(text, ribbonRect.left + ctx.padding / 2f, ribbonRect.top + ribbonRect.height() * 0.65f, textPaint)
        }

        val badgeCenterX = ctx.padding + badgeRadius
        val badgeCenterY = ctx.height - ctx.padding - badgeRadius
        drawPinBadge(ctx.canvas, badgeCenterX, badgeCenterY, badgeRadius)

        var currentY = overlayTop + ribbonAllowance

        addressLayout?.let { layout ->
            ctx.canvas.save()
            ctx.canvas.translate(textStartX, currentY)
            layout.draw(ctx.canvas)
            ctx.canvas.restore()
            currentY += layout.height + ctx.blockSpacing
        }
        latLongLine?.let { text ->
            ctx.canvas.drawText(text, textStartX, currentY + ctx.detailTextSize, ctx.latLongPaint)
        }
    }

    // ---- Layout 3: Light Card — light background bar, navy text, blue pin ----
    private fun renderLightCard(
        ctx: StampContext,
        addressLine: String?,
        dateTimeLine: String?,
        latLongLine: String?,
        mapThumbnail: Bitmap?
    ) {
        val badgeRadius = ctx.width * 0.045f
        val iconSize = if (mapThumbnail != null) ctx.width * 0.16f else badgeRadius * 2f
        val textStartX = ctx.padding + iconSize + ctx.width * 0.03f
        val textWidth = (ctx.width - textStartX - ctx.padding).toInt()

        val addressPaint = TextPaint(ctx.addressPaint).apply { color = Color.parseColor("#15305C") }
        val dateTimePaint = TextPaint(ctx.dateTimePaint).apply { color = Color.parseColor("#1976D2") }
        val latLongPaint = TextPaint(ctx.latLongPaint).apply { color = Color.parseColor("#6B7A99") }

        val blocks = buildTextBlocks(ctx, addressLine, dateTimeLine, latLongLine, textStartX, textWidth, addressPaint, dateTimePaint, latLongPaint)
        if (blocks.isEmpty()) return

        val verticalPadding = ctx.detailTextSize * 1.1f
        val totalTextHeight = blocks.sumOf { it.height.toDouble() }.toFloat() + ctx.blockSpacing * (blocks.size - 1)
        val overlayHeight = maxOf(totalTextHeight, iconSize) + verticalPadding * 2f
        val overlayTop = ctx.height - overlayHeight

        ctx.canvas.drawRect(0f, overlayTop, ctx.width.toFloat(), ctx.height.toFloat(), solidPaint("#F0F2F5"))
        drawBlocks(ctx.canvas, blocks, overlayTop, overlayHeight, totalTextHeight, ctx.blockSpacing)

        val iconCenterY = overlayTop + overlayHeight / 2f
        if (mapThumbnail != null) {
            drawMapThumbnail(ctx.canvas, mapThumbnail, ctx.padding, iconCenterY - iconSize / 2f, iconSize)
        } else {
            drawPinBadge(ctx.canvas, ctx.padding + badgeRadius, iconCenterY, badgeRadius, "#1976D2")
        }
    }

    // ---- Layout 4: Center Pin — pin straddles the band, centered text below ----
    private fun renderCenterPin(
        ctx: StampContext,
        addressLine: String?,
        dateTimeLine: String?,
        latLongLine: String?
    ) {
        val badgeRadius = ctx.width * 0.06f
        val textWidth = (ctx.width - ctx.padding * 2).toInt()
        val centerPaint = TextPaint(ctx.addressPaint).apply { textAlign = Paint.Align.CENTER }
        val addressLayout = addressLine?.let { text ->
            StaticLayout.Builder.obtain(text, 0, text.length, centerPaint, textWidth)
                .setMaxLines(2)
                .setEllipsize(TextUtils.TruncateAt.END)
                .setAlignment(android.text.Layout.Alignment.ALIGN_CENTER)
                .build()
        }

        var textHeight = 0f
        addressLayout?.let { textHeight += it.height + ctx.blockSpacing }
        if (dateTimeLine != null) textHeight += ctx.detailTextSize * 1.4f
        if (latLongLine != null) textHeight += ctx.detailTextSize * 1.4f

        val overlayHeight = badgeRadius * 0.6f + ctx.blockSpacing * 2f + textHeight + ctx.padding
        val overlayTop = ctx.height - overlayHeight
        ctx.canvas.drawRect(0f, overlayTop, ctx.width.toFloat(), ctx.height.toFloat(), solidPaint("#CC0D1B2A"))

        val centerX = ctx.width / 2f
        drawPinBadge(ctx.canvas, centerX, overlayTop, badgeRadius)

        var currentY = overlayTop + badgeRadius * 0.6f + ctx.blockSpacing * 2f

        addressLayout?.let { layout ->
            ctx.canvas.save()
            ctx.canvas.translate(centerX, currentY)
            layout.draw(ctx.canvas)
            ctx.canvas.restore()
            currentY += layout.height + ctx.blockSpacing
        }
        dateTimeLine?.let { text ->
            val p = TextPaint(ctx.dateTimePaint).apply { textAlign = Paint.Align.CENTER }
            ctx.canvas.drawText(text, centerX, currentY + ctx.detailTextSize, p)
            currentY += ctx.detailTextSize * 1.4f
        }
        latLongLine?.let { text ->
            val p = TextPaint(ctx.latLongPaint).apply { textAlign = Paint.Align.CENTER }
            ctx.canvas.drawText(text, centerX, currentY + ctx.detailTextSize, p)
        }
    }

    // ---- Layout 5: Split Map — map/pin fills left half, text fills right half ----
    private fun renderSplitMap(
        ctx: StampContext,
        addressLine: String?,
        dateTimeLine: String?,
        latLongLine: String?,
        mapThumbnail: Bitmap?
    ) {
        val halfWidth = ctx.width / 2f
        val iconSize = ctx.width * 0.24f

        val textStartX = halfWidth + ctx.padding
        val textWidth = (ctx.width - textStartX - ctx.padding).toInt()
        val blocks = buildTextBlocks(ctx, addressLine, dateTimeLine, latLongLine, textStartX, textWidth)
        val totalTextHeight = blocks.sumOf { it.height.toDouble() }.toFloat() +
            ctx.blockSpacing * (blocks.size - 1).coerceAtLeast(0)

        val overlayHeight = maxOf(totalTextHeight, iconSize) + ctx.padding * 2f
        val overlayTop = ctx.height - overlayHeight
        ctx.canvas.drawRect(0f, overlayTop, ctx.width.toFloat(), ctx.height.toFloat(), solidPaint("#CC1A1A1A"))

        val iconTop = overlayTop + (overlayHeight - iconSize) / 2f
        val iconLeft = (halfWidth - iconSize) / 2f

        if (mapThumbnail != null) {
            drawMapThumbnail(ctx.canvas, mapThumbnail, iconLeft, iconTop, iconSize)
        } else {
            drawPinBadge(ctx.canvas, halfWidth / 2f, overlayTop + overlayHeight / 2f, iconSize / 2.6f)
        }

        ctx.canvas.drawLine(halfWidth, overlayTop + ctx.padding, halfWidth, ctx.height - ctx.padding, solidPaint("#33FFFFFF").apply { strokeWidth = 2f })

        var currentTop = overlayTop + (overlayHeight - totalTextHeight) / 2f
        for (block in blocks) {
            block.draw(ctx.canvas, currentTop)
            currentTop += block.height + ctx.blockSpacing
        }
    }

    // ---- Layout 6: Minimal — text only, no badge, soft gradient ----
    private fun renderMinimal(
        ctx: StampContext,
        addressLine: String?,
        dateTimeLine: String?,
        latLongLine: String?
    ) {
        val textWidth = (ctx.width - ctx.padding * 2).toInt()
        val blocks = buildTextBlocks(ctx, addressLine, dateTimeLine, latLongLine, ctx.padding, textWidth)
        if (blocks.isEmpty()) return

        val verticalPadding = ctx.detailTextSize * 1.1f
        val totalTextHeight = blocks.sumOf { it.height.toDouble() }.toFloat() + ctx.blockSpacing * (blocks.size - 1)
        val overlayHeight = totalTextHeight + verticalPadding * 2f
        val overlayTop = ctx.height - overlayHeight

        drawGradientBand(ctx.canvas, ctx.width, overlayTop, ctx.height.toFloat())
        drawBlocks(ctx.canvas, blocks, overlayTop, overlayHeight, totalTextHeight, ctx.blockSpacing)
    }

    // ---- Layout 7: Top Strip — same as classic but band at the top of the photo ----
    private fun renderTopStrip(
        ctx: StampContext,
        addressLine: String?,
        dateTimeLine: String?,
        latLongLine: String?,
        mapThumbnail: Bitmap?
    ) {
        val badgeRadius = ctx.width * 0.045f
        val iconSize = if (mapThumbnail != null) ctx.width * 0.16f else badgeRadius * 2f
        val textStartX = ctx.padding + iconSize + ctx.width * 0.03f
        val textWidth = (ctx.width - textStartX - ctx.padding).toInt()
        val verticalPadding = ctx.detailTextSize * 1.1f

        val blocks = buildTextBlocks(ctx, addressLine, dateTimeLine, latLongLine, textStartX, textWidth)
        if (blocks.isEmpty()) return

        val totalTextHeight = blocks.sumOf { it.height.toDouble() }.toFloat() + ctx.blockSpacing * (blocks.size - 1)
        val overlayHeight = maxOf(totalTextHeight, iconSize) + verticalPadding * 2f

        val gradientPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, overlayHeight,
                Color.argb(210, 0, 0, 0), Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
        }
        ctx.canvas.drawRect(0f, 0f, ctx.width.toFloat(), overlayHeight, gradientPaint)
        drawBlocks(ctx.canvas, blocks, 0f, overlayHeight, totalTextHeight, ctx.blockSpacing)

        val iconCenterY = overlayHeight / 2f
        if (mapThumbnail != null) {
            drawMapThumbnail(ctx.canvas, mapThumbnail, ctx.padding, iconCenterY - iconSize / 2f, iconSize)
        } else {
            drawPinBadge(ctx.canvas, ctx.padding + badgeRadius, iconCenterY, badgeRadius)
        }
    }

    // ---- Shared helpers ----

    private fun StampContext.detailTimePaintBold(): TextPaint = TextPaint(dateTimePaint).apply { isFakeBoldText = true }

    private fun buildAddressLayout(text: String, paint: TextPaint, maxWidth: Int, maxLines: Int = 2): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, maxWidth)
            .setMaxLines(maxLines)
            .setEllipsize(TextUtils.TruncateAt.END)
            .build()

    private fun buildTextBlocks(
        ctx: StampContext,
        addressLine: String?,
        dateTimeLine: String?,
        latLongLine: String?,
        startX: Float,
        maxWidth: Int,
        addressPaint: TextPaint = ctx.addressPaint,
        dateTimePaint: TextPaint = ctx.dateTimePaint,
        latLongPaint: TextPaint = ctx.latLongPaint
    ): List<TextBlock> {
        val blocks = mutableListOf<TextBlock>()

        addressLine?.let { text ->
            val layout = buildAddressLayout(text, addressPaint, maxWidth)
            blocks.add(TextBlock(layout.height.toFloat()) { c, top ->
                c.save()
                c.translate(startX, top)
                layout.draw(c)
                c.restore()
            })
        }
        dateTimeLine?.let { text ->
            val lineHeight = ctx.detailTextSize * 1.15f
            blocks.add(TextBlock(lineHeight) { c, top ->
                c.drawText(text, startX, top + ctx.detailTextSize, dateTimePaint)
            })
        }
        latLongLine?.let { text ->
            val lineHeight = ctx.detailTextSize * 1.15f
            blocks.add(TextBlock(lineHeight) { c, top ->
                c.drawText(text, startX, top + ctx.detailTextSize, latLongPaint)
            })
        }
        return blocks
    }

    private fun drawBlocks(
        canvas: Canvas,
        blocks: List<TextBlock>,
        overlayTop: Float,
        overlayHeight: Float,
        totalTextHeight: Float,
        blockSpacing: Float
    ) {
        var currentTop = overlayTop + (overlayHeight - totalTextHeight) / 2f
        for (block in blocks) {
            block.draw(canvas, currentTop)
            currentTop += block.height + blockSpacing
        }
    }

    private fun drawGradientBand(canvas: Canvas, width: Int, top: Float, bottom: Float) {
        val gradientPaint = Paint().apply {
            shader = LinearGradient(
                0f, top, 0f, bottom,
                Color.TRANSPARENT, Color.argb(210, 0, 0, 0),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, top, width.toFloat(), bottom, gradientPaint)
    }

    private fun solidPaint(hex: String): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor(hex) }

    private fun drawMapThumbnail(canvas: Canvas, thumbnail: Bitmap, left: Float, top: Float, size: Float) {
        val rect = RectF(left, top, left + size, top + size)
        val clipPath = Path().apply {
            addRoundRect(rect, size * 0.18f, size * 0.18f, Path.Direction.CW)
        }

        canvas.save()
        canvas.clipPath(clipPath)
        val srcRect = Rect(0, 0, thumbnail.width, thumbnail.height)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(thumbnail, srcRect, rect, paint)
        canvas.restore()

        val markerRadius = size * 0.11f
        val markerCenterX = left + size / 2f
        val markerCenterY = top + size / 2f - markerRadius * 0.3f
        drawPin(canvas, markerCenterX, markerCenterY, markerRadius)
    }

    private fun drawPinBadge(canvas: Canvas, centerX: Float, centerY: Float, radius: Float, colorHex: String = "#E24B4A") {
        canvas.drawCircle(centerX, centerY, radius, solidPaint(colorHex))
        val pinRadius = radius * 0.42f
        val topCircleCenterY = centerY - radius * 0.12f
        drawPin(canvas, centerX, topCircleCenterY, pinRadius, Color.WHITE)
    }

    private fun drawPin(
        canvas: Canvas,
        centerX: Float,
        topCircleCenterY: Float,
        pinRadius: Float,
        color: Int = Color.parseColor("#E24B4A")
    ) {
        val pinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        val pinPath = Path().apply {
            addCircle(centerX, topCircleCenterY, pinRadius, Path.Direction.CW)
            moveTo(centerX - pinRadius * 0.85f, topCircleCenterY + pinRadius * 0.55f)
            lineTo(centerX + pinRadius * 0.85f, topCircleCenterY + pinRadius * 0.55f)
            lineTo(centerX, topCircleCenterY + pinRadius * 2.3f)
            close()
        }
        canvas.drawPath(pinPath, pinPaint)
    }
}
