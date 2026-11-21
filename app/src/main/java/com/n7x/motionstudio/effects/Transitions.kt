package com.n7x.motionstudio.effects

import com.n7x.motionstudio.animation.Easing
import kotlin.math.max

/**
 * Cut transitions. Clips do not overlap yet, so a transition plays as half "out"
 * at the end of a clip and half "in" at the start of the next one.
 */
object Transitions {
    val TYPES = listOf(
        "none" to "None",
        "fade" to "Fade",
        "flash" to "Flash",
        "slideL" to "Slide left",
        "slideR" to "Slide right",
        "slideU" to "Slide up",
        "slideD" to "Slide down",
        "zoomIn" to "Zoom in",
        "zoomOut" to "Zoom out",
        "spin" to "Spin",
        "flip" to "Flip"
    )

    /** Older projects used "slide" and "zoom". */
    fun canonical(type: String): String = when (type) {
        "slide" -> "slideL"
        "zoom" -> "zoomIn"
        else -> type
    }

    fun nameOf(type: String): String {
        val id = canonical(type)
        return TYPES.firstOrNull { it.first == id }?.second ?: id
    }

    /**
     * alpha and scale multiply the clip's own animation, dx/dy (fractions of width/height),
     * rotation and flip (degrees) add to it. white is a white overlay (0..1).
     */
    data class Adjust(
        val alpha: Float,
        val dx: Float,
        val dy: Float,
        val scale: Float,
        val rotation: Float,
        val flip: Float,
        val white: Float
    ) {
        companion object {
            val NONE = Adjust(1f, 0f, 0f, 1f, 0f, 0f, 0f)
        }
    }

    fun at(durations: List<Long>, types: List<String>, lengths: List<Long>, t: Long): Adjust {
        if (durations.isEmpty()) return Adjust.NONE
        var start = 0L
        for (i in durations.indices) {
            val dur = durations[i]
            if (t < start + dur || i == durations.lastIndex) {
                val local = (t - start).coerceIn(0L, dur)
                var out = 0f
                var inn = 0f
                var type = "none"
                if (i < durations.lastIndex && types[i] != "none") {
                    val half = minOf(lengths[i] / 2, dur / 2, durations[i + 1] / 2)
                    if (half > 0 && local > dur - half) {
                        out = ease((local - (dur - half)).toFloat() / half)
                        type = types[i]
                    }
                }
                if (out == 0f && i > 0 && types[i - 1] != "none") {
                    val half = minOf(lengths[i - 1] / 2, durations[i - 1] / 2, dur / 2)
                    if (half > 0 && local < half) {
                        inn = ease(1f - local.toFloat() / half)
                        type = types[i - 1]
                    }
                }
                return adjust(type, out, inn)
            }
            start += dur
        }
        return Adjust.NONE
    }

    fun ease(p: Float): Float = Easing.EASE_IN_OUT.apply(p.coerceIn(0f, 1f))

    /** out: 0..1 while the old clip leaves, inn: 1..0 while the new clip arrives. */
    fun adjust(type: String, out: Float, inn: Float): Adjust {
        val a = max(out, inn)
        return when (canonical(type)) {
            "fade" -> Adjust(1f - a, 0f, 0f, 1f, 0f, 0f, 0f)
            "flash" -> Adjust(1f, 0f, 0f, 1f, 0f, 0f, a)
            "slideL" -> Adjust(1f, inn - out, 0f, 1f, 0f, 0f, 0f)
            "slideR" -> Adjust(1f, out - inn, 0f, 1f, 0f, 0f, 0f)
            "slideU" -> Adjust(1f, 0f, inn - out, 1f, 0f, 0f, 0f)
            "slideD" -> Adjust(1f, 0f, out - inn, 1f, 0f, 0f, 0f)
            "zoomIn" -> Adjust(1f - a, 0f, 0f, (1f + out * 0.8f) * (1f - inn * 0.3f), 0f, 0f, 0f)
            "zoomOut" -> Adjust(1f - a, 0f, 0f, (1f - out * 0.5f) * (1f + inn * 0.6f), 0f, 0f, 0f)
            "spin" -> Adjust(1f - a, 0f, 0f, 1f - 0.3f * a, (out - inn) * 90f, 0f, 0f)
            "flip" -> Adjust(1f, 0f, 0f, 1f, 0f, (out - inn) * 90f, 0f)
            else -> Adjust.NONE
        }
    }
}
