package com.n7x.motionstudio.export

import android.graphics.Matrix
import androidx.media3.effect.MatrixTransformation
import androidx.media3.effect.RgbMatrix
import com.n7x.motionstudio.animation.Keyframes
import com.n7x.motionstudio.data.MediaClipEntity
import com.n7x.motionstudio.data.timelineMs
import com.n7x.motionstudio.effects.Transitions
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max

/**
 * Keyframe animation and transitions of all clips, evaluated at a point on the timeline.
 * It uses the same functions as the editor preview, so both show the same result.
 */
class TimelineMotion(clips: List<MediaClipEntity>) {
    class Frame(
        val x: Float,
        val y: Float,
        val scale: Float,
        val rotation: Float,
        val flip: Float,
        val opacity: Float,
        val white: Float
    )

    private val keys = clips.map { Keyframes.parse(it.keyframes) }
    private val durations = clips.map { it.timelineMs() }
    private val types = clips.map { it.transitionType }
    private val lengths = clips.map { it.transitionMs }
    private val starts: LongArray = LongArray(clips.size).also { s ->
        var t = 0L
        for (i in clips.indices) {
            s[i] = t
            t += durations[i]
        }
    }

    /** False when the project has no keyframes and no transitions, so nothing needs rendering. */
    val needed: Boolean = keys.any { it.isNotEmpty() } || types.dropLast(1).any { it != "none" }

    fun frameAt(timeMs: Long): Frame {
        var index = durations.lastIndex
        for (i in durations.indices) {
            if (timeMs < starts[i] + durations[i]) {
                index = i
                break
            }
        }
        val local = (timeMs - starts[index]).coerceAtLeast(0L)
        val base = Keyframes.evaluate(keys[index], local)
        val adj = Transitions.at(durations, types, lengths, timeMs)
        return Frame(
            x = base.x + adj.dx,
            y = base.y + adj.dy,
            scale = base.scale * adj.scale,
            rotation = base.rotation + adj.rotation,
            flip = adj.flip,
            opacity = base.opacity * adj.alpha,
            white = adj.white
        )
    }
}

/** Moves, scales and rotates the whole frame. Runs on the finished timeline. */
class MotionTransformation(
    private val motion: TimelineMotion,
    private val aspect: Float
) : MatrixTransformation {
    override fun getMatrix(presentationTimeUs: Long): Matrix {
        val f = motion.frameAt(presentationTimeUs / 1000L)
        val flipScale = max(abs(cos(Math.toRadians(f.flip.toDouble()))).toFloat(), 0.001f)
        val m = Matrix()
        // rotate in pixel space so the picture is not squashed
        m.postScale(aspect, 1f)
        m.postRotate(-f.rotation)
        m.postScale(1f / aspect, 1f)
        m.postScale(f.scale * flipScale, f.scale)
        m.postTranslate(2f * f.x, -2f * f.y)
        return m
    }
}

/** Fades the picture towards black (opacity) or white (flash). */
class MotionColor(private val motion: TimelineMotion) : RgbMatrix {
    override fun getMatrix(presentationTimeUs: Long, useHdr: Boolean): FloatArray {
        val f = motion.frameAt(presentationTimeUs / 1000L)
        val s = (f.opacity * (1f - f.white)).coerceIn(0f, 1f)
        val g = FloatArray(16)
        g[0] = s
        g[5] = s
        g[10] = s
        g[15] = 1f
        g[12] = f.white
        g[13] = f.white
        g[14] = f.white
        return g
    }
}
