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
        "slide" to "Slide",
        "zoom" to "Zoom",
        "spin" to "Spin"
    )

    /** alpha and scale multiply the clip's own animation, dx (fraction of width) and rotation add to it. */
    data class Adjust(val alpha: Float, val dx: Float, val scale: Float, val rotation: Float) {
        companion object {
            val NONE = Adjust(1f, 0f, 1f, 0f)
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
                return build(type, out, inn)
            }
            start += dur
        }
        return Adjust.NONE
    }

    private fun ease(p: Float) = Easing.EASE_IN_OUT.apply(p.coerceIn(0f, 1f))

    private fun build(type: String, out: Float, inn: Float): Adjust {
        val a = max(out, inn)
        return when (type) {
            "fade" -> Adjust(1f - a, 0f, 1f, 0f)
            "slide" -> Adjust(1f, inn - out, 1f, 0f)
            "zoom" -> Adjust(1f - a, 0f, (1f + out * 0.8f) * (1f - inn * 0.3f), 0f)
            "spin" -> Adjust(1f - a, 0f, 1f - 0.3f * a, (out - inn) * 90f)
            else -> Adjust.NONE
        }
    }
}
