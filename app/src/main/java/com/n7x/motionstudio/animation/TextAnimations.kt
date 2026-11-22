package com.n7x.motionstudio.animation

import com.n7x.motionstudio.data.OverlayEntity
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/**
 * Start ("in"), stop ("out") and overall ("loop") animations of text and sticker layers.
 * The same code drives the preview, the timeline and the small demo tiles.
 */
object TextAnimations {
    val IN_TYPES = listOf(
        "none" to "None",
        "fade" to "Fade",
        "slideUp" to "Slide up",
        "slideDown" to "Slide down",
        "slideLeft" to "From left",
        "slideRight" to "From right",
        "zoomIn" to "Zoom in",
        "zoomOut" to "Zoom out",
        "pop" to "Pop",
        "bounce" to "Bounce",
        "spin" to "Spin",
        "typewriter" to "Typewriter"
    )
    val OUT_TYPES = listOf(
        "none" to "None",
        "fade" to "Fade",
        "slideUp" to "Slide up",
        "slideDown" to "Slide down",
        "slideLeft" to "To left",
        "slideRight" to "To right",
        "zoomOut" to "Zoom out",
        "zoomIn" to "Zoom in",
        "spin" to "Spin",
        "drop" to "Drop",
        "typewriter" to "Erase"
    )
    val LOOP_TYPES = listOf(
        "none" to "None",
        "pulse" to "Pulse",
        "heartbeat" to "Heartbeat",
        "float" to "Float",
        "shake" to "Shake",
        "wiggle" to "Wiggle",
        "swing" to "Swing",
        "blink" to "Blink",
        "rainbow" to "Rainbow"
    )

    /** Types that only make sense for text (not for emoji stickers). */
    val TEXT_ONLY = setOf("typewriter", "rainbow")

    fun describe(kind: String, id: String): String = when (kind) {
        "in" -> when (id) {
            "fade" -> "The text slowly appears."
            "slideUp" -> "The text rises up from below while it appears."
            "slideDown" -> "The text drops in from above."
            "slideLeft" -> "The text slides in from the left."
            "slideRight" -> "The text slides in from the right."
            "zoomIn" -> "The text grows from nothing to full size."
            "zoomOut" -> "The text shrinks down from a big size."
            "pop" -> "The text pops in with a small bounce in size."
            "bounce" -> "The text drops from above and bounces."
            "spin" -> "The text spins while it grows in."
            "typewriter" -> "The letters appear one by one."
            else -> "No start animation. The text just appears."
        }
        "out" -> when (id) {
            "fade" -> "The text slowly fades away."
            "slideUp" -> "The text floats up and fades away."
            "slideDown" -> "The text sinks down and fades away."
            "slideLeft" -> "The text slides out to the left."
            "slideRight" -> "The text slides out to the right."
            "zoomOut" -> "The text shrinks to nothing."
            "zoomIn" -> "The text grows big and fades away."
            "spin" -> "The text spins while it shrinks away."
            "drop" -> "The text falls down and fades."
            "typewriter" -> "The letters disappear one by one."
            else -> "No stop animation. The text just disappears."
        }
        else -> when (id) {
            "pulse" -> "The text gently grows and shrinks all the time."
            "heartbeat" -> "The text beats like a heart."
            "float" -> "The text floats up and down."
            "shake" -> "The text shakes left and right."
            "wiggle" -> "The text wiggles side to side."
            "swing" -> "The text swings like a pendulum."
            "blink" -> "The text blinks on and off softly."
            "rainbow" -> "The text colour changes through the rainbow."
            else -> "No overall animation."
        }
    }

    /**
     * alpha and scale multiply, dx/dy (fractions of the preview width/height) and rotation add.
     * reveal is the share of letters shown (1 = all). hue is set by the rainbow animation.
     */
    data class State(
        val alpha: Float,
        val dx: Float,
        val dy: Float,
        val scale: Float,
        val rotation: Float,
        val reveal: Float,
        val hue: Float?
    ) {
        companion object {
            val NONE = State(1f, 0f, 0f, 1f, 0f, 1f, null)
        }
    }

    private class Part(
        val alpha: Float = 1f,
        val dx: Float = 0f,
        val dy: Float = 0f,
        val scale: Float = 1f,
        val rotation: Float = 0f,
        val reveal: Float = 1f
    )

    /** localMs = time since the layer's start. */
    fun evaluate(layer: OverlayEntity, localMs: Long): State {
        val local = localMs.coerceAtLeast(0L)
        var alpha = 1f
        var dx = 0f
        var dy = 0f
        var scale = 1f
        var rotation = 0f
        var reveal = 1f
        var hue: Float? = null

        if (layer.inAnim != "none" && layer.inMs > 0L && local < layer.inMs) {
            val part = enter(layer.inAnim, (local / layer.inMs.toFloat()).coerceIn(0f, 1f))
            alpha *= part.alpha
            dx += part.dx
            dy += part.dy
            scale *= part.scale
            rotation += part.rotation
            reveal = min(reveal, part.reveal)
        }
        val outStart = layer.durationMs - layer.outMs
        if (layer.outAnim != "none" && layer.outMs > 0L && local > outStart) {
            val part = leave(layer.outAnim, ((local - outStart) / layer.outMs.toFloat()).coerceIn(0f, 1f))
            alpha *= part.alpha
            dx += part.dx
            dy += part.dy
            scale *= part.scale
            rotation += part.rotation
            reveal = min(reveal, part.reveal)
        }
        if (layer.loopAnim != "none") {
            val cycle = local / 1000f * layer.loopSpeed
            val part = loop(layer.loopAnim, cycle)
            alpha *= part.alpha
            dx += part.dx
            dy += part.dy
            scale *= part.scale
            rotation += part.rotation
            if (layer.loopAnim == "rainbow") hue = (cycle * 360f) % 360f
        }
        return State(
            alpha = alpha.coerceIn(0f, 1f),
            dx = dx,
            dy = dy,
            scale = scale.coerceAtLeast(0f),
            rotation = rotation,
            reveal = reveal.coerceIn(0f, 1f),
            hue = hue
        )
    }

    // p goes from 0 (hidden) to 1 (normal)
    private fun enter(type: String, p: Float): Part {
        val e = outCubic(p)
        return when (type) {
            "fade" -> Part(alpha = e)
            "slideUp" -> Part(alpha = e, dy = (1f - e) * 0.25f)
            "slideDown" -> Part(alpha = e, dy = -(1f - e) * 0.25f)
            "slideLeft" -> Part(alpha = e, dx = -(1f - e) * 0.4f)
            "slideRight" -> Part(alpha = e, dx = (1f - e) * 0.4f)
            "zoomIn" -> Part(alpha = e, scale = e)
            "zoomOut" -> Part(alpha = e, scale = 2f - e)
            "pop" -> Part(alpha = min(1f, p * 4f), scale = backOut(p))
            "bounce" -> Part(alpha = min(1f, p * 5f), dy = -(1f - bounceOut(p)) * 0.45f)
            "spin" -> Part(alpha = e, scale = e, rotation = -(1f - e) * 360f)
            "typewriter" -> Part(reveal = p)
            else -> Part()
        }
    }

    // q goes from 0 (normal) to 1 (gone)
    private fun leave(type: String, q: Float): Part {
        val e = inCubic(q)
        return when (type) {
            "fade" -> Part(alpha = 1f - q)
            "slideUp" -> Part(alpha = 1f - e, dy = -e * 0.25f)
            "slideDown" -> Part(alpha = 1f - e, dy = e * 0.25f)
            "slideLeft" -> Part(alpha = 1f - e, dx = -e * 0.4f)
            "slideRight" -> Part(alpha = 1f - e, dx = e * 0.4f)
            "zoomOut" -> Part(alpha = 1f - e, scale = 1f - e)
            "zoomIn" -> Part(alpha = 1f - e, scale = 1f + e * 1.2f)
            "spin" -> Part(alpha = 1f - e, scale = 1f - e, rotation = e * 360f)
            "drop" -> Part(alpha = 1f - e, dy = e * 0.5f)
            "typewriter" -> Part(reveal = 1f - q)
            else -> Part()
        }
    }

    // cycle: one turn per second at speed 1
    private fun loop(type: String, cycle: Float): Part {
        val s = sin(2.0 * PI * cycle).toFloat()
        return when (type) {
            "pulse" -> Part(scale = 1f + 0.08f * s)
            "heartbeat" -> {
                val ph = cycle - kotlin.math.floor(cycle)
                Part(scale = 1f + 0.16f * (bump(ph, 0.1f, 0.1f) + 0.7f * bump(ph, 0.3f, 0.1f)))
            }
            "float" -> Part(dy = 0.025f * s)
            "shake" -> Part(dx = 0.012f * sin(2.0 * PI * cycle * 7.0).toFloat())
            "wiggle" -> Part(rotation = 6f * sin(2.0 * PI * cycle * 2.0).toFloat())
            "swing" -> Part(rotation = 12f * s)
            "blink" -> Part(alpha = 0.25f + 0.75f * (0.5f + 0.5f * cos(2.0 * PI * cycle).toFloat()))
            else -> Part()
        }
    }

    private fun bump(x: Float, center: Float, width: Float) = max(0f, 1f - abs(x - center) / width)

    private fun outCubic(x: Float) = 1f - (1f - x).pow(3)

    private fun inCubic(x: Float) = x * x * x

    private fun backOut(x: Float): Float {
        val c1 = 1.70158f
        val c3 = c1 + 1f
        return 1f + c3 * (x - 1f).pow(3) + c1 * (x - 1f).pow(2)
    }

    private fun bounceOut(x: Float): Float {
        val n1 = 7.5625f
        val d1 = 2.75f
        return when {
            x < 1f / d1 -> n1 * x * x
            x < 2f / d1 -> {
                val t = x - 1.5f / d1
                n1 * t * t + 0.75f
            }
            x < 2.5f / d1 -> {
                val t = x - 2.25f / d1
                n1 * t * t + 0.9375f
            }
            else -> {
                val t = x - 2.625f / d1
                n1 * t * t + 0.984375f
            }
        }
    }
}
