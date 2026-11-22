package com.n7x.motionstudio.export

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.SpannableString
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.ForegroundColorSpan
import com.n7x.motionstudio.data.OverlayEntity
import kotlin.math.ceil

/** Draws a text or sticker layer into a bitmap at the export resolution. */
object OverlayRenderer {
    private val FAMILIES = mapOf(
        "sans" to "sans-serif",
        "light" to "sans-serif-light",
        "medium" to "sans-serif-medium",
        "black" to "sans-serif-black",
        "condensed" to "sans-serif-condensed",
        "serif" to "serif",
        "mono" to "monospace",
        "serifmono" to "serif-monospace",
        "cursive" to "cursive",
        "casual" to "casual",
        "smallcaps" to "sans-serif-smallcaps"
    )

    private fun resolveTypeface(layer: OverlayEntity): Typeface {
        val style = when {
            layer.bold && layer.italic -> Typeface.BOLD_ITALIC
            layer.bold -> Typeface.BOLD
            layer.italic -> Typeface.ITALIC
            else -> Typeface.NORMAL
        }
        val base = try {
            if (layer.fontId.startsWith("file:")) {
                Typeface.createFromFile(layer.fontId.removePrefix("file:"))
            } else {
                Typeface.create(FAMILIES[layer.fontId] ?: "sans-serif", Typeface.NORMAL)
            }
        } catch (e: Exception) {
            Typeface.DEFAULT
        }
        return Typeface.create(base, style)
    }

    /** revealCount: how many letters are visible (typewriter). -1 shows everything. */
    fun render(layer: OverlayEntity, outW: Int, outH: Int, revealCount: Int = -1): Bitmap? {
        if (layer.text.isBlank()) return null
        val content: CharSequence = if (revealCount in 0 until layer.text.length) {
            SpannableString(layer.text).apply {
                setSpan(ForegroundColorSpan(Color.TRANSPARENT), revealCount, layer.text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        } else {
            layer.text
        }
        val size = layer.size * outH
        val face = resolveTypeface(layer)
        val fill = TextPaint(Paint.ANTI_ALIAS_FLAG)
        fill.color = layer.colorArgb
        fill.textSize = size
        fill.typeface = face
        fill.letterSpacing = layer.letterSpacing
        val shadowOffset = size * 0.05f * layer.shadowSize
        if (layer.shadowSize > 0f) {
            fill.setShadowLayer(size * 0.2f * layer.shadowSize, shadowOffset, shadowOffset, layer.shadowColor)
        }
        val strokeWidth = if (layer.outlineWidth > 0f) size * 0.18f * layer.outlineWidth else 0f

        val maxWidth = (outW * 0.92f).toInt()
        val measure = StaticLayout.Builder.obtain(content, 0, content.length, fill, maxWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .build()
        var lineWidth = 0f
        for (i in 0 until measure.lineCount) lineWidth = maxOf(lineWidth, measure.getLineWidth(i))
        val layoutWidth = minOf(maxWidth, ceil(lineWidth).toInt() + 2)

        val fillLayout = StaticLayout.Builder.obtain(content, 0, content.length, fill, layoutWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .build()
        val strokeLayout = if (strokeWidth > 0f) {
            val stroke = TextPaint(fill)
            stroke.clearShadowLayer()
            stroke.style = Paint.Style.STROKE
            stroke.strokeWidth = strokeWidth
            stroke.strokeJoin = Paint.Join.ROUND
            stroke.color = layer.outlineColor
            StaticLayout.Builder.obtain(content, 0, content.length, stroke, layoutWidth)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .build()
        } else {
            null
        }

        val shadowExtra = size * 0.25f * layer.shadowSize
        val padX = (if (layer.box) outH * 0.04f else 0f) + strokeWidth + size * 0.12f + shadowExtra
        val padY = (if (layer.box) outH * 0.012f else 0f) + strokeWidth + size * 0.08f + shadowExtra
        val width = ceil(layoutWidth + 2 * padX).toInt().coerceAtLeast(2)
        val height = ceil(fillLayout.height + 2 * padY).toInt().coerceAtLeast(2)

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        if (layer.box) {
            val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            boxPaint.color = layer.boxColor
            val radius = outH * 0.03f
            canvas.drawRoundRect(RectF(0f, 0f, width.toFloat(), height.toFloat()), radius, radius, boxPaint)
        }
        canvas.translate(padX, padY)
        strokeLayout?.draw(canvas)
        fillLayout.draw(canvas)
        return bitmap
    }
}
