package com.n7x.motionstudio.animation

import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs

/** x, y are fractions of the preview width/height (0.1 = 10% of the width). */
data class Transform(
    val x: Float,
    val y: Float,
    val scale: Float,
    val rotation: Float,
    val opacity: Float
) {
    companion object {
        val IDENTITY = Transform(0f, 0f, 1f, 0f, 1f)
    }
}

/** CSS-style cubic bezier easing curve from (0,0) to (1,1) with two control points. */
data class Easing(val x1: Float, val y1: Float, val x2: Float, val y2: Float) {

    fun apply(p: Float): Float {
        if (p <= 0f) return 0f
        if (p >= 1f) return 1f
        if (x1 == y1 && x2 == y2) return p
        var lo = 0f
        var hi = 1f
        var s = p
        repeat(24) {
            val x = bezier(s, x1, x2)
            if (x < p) lo = s else hi = s
            s = (lo + hi) / 2f
        }
        return bezier(s, y1, y2)
    }

    private fun bezier(s: Float, a: Float, b: Float): Float {
        val u = 1f - s
        return 3f * u * u * s * a + 3f * u * s * s * b + s * s * s
    }

    companion object {
        val LINEAR = Easing(0f, 0f, 1f, 1f)
        val EASE_IN = Easing(0.42f, 0f, 1f, 1f)
        val EASE_OUT = Easing(0f, 0f, 0.58f, 1f)
        val EASE_IN_OUT = Easing(0.42f, 0f, 0.58f, 1f)
        val PRESETS = listOf(
            "Linear" to LINEAR,
            "Ease in" to EASE_IN,
            "Ease out" to EASE_OUT,
            "Ease in-out" to EASE_IN_OUT
        )
    }
}

/**
 * A keyframe. timeMs is measured from the start of its clip on the timeline.
 * The easing shapes the movement from this keyframe to the next one.
 */
data class Keyframe(val timeMs: Long, val value: Transform, val easing: Easing)

object Keyframes {
    const val TOLERANCE_MS = 50L

    fun parse(json: String): List<Keyframe> {
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                val ea = o.getJSONArray("e")
                Keyframe(
                    timeMs = o.getLong("t"),
                    value = Transform(
                        x = o.getDouble("x").toFloat(),
                        y = o.getDouble("y").toFloat(),
                        scale = o.getDouble("s").toFloat(),
                        rotation = o.getDouble("r").toFloat(),
                        opacity = o.getDouble("o").toFloat()
                    ),
                    easing = Easing(
                        ea.getDouble(0).toFloat(),
                        ea.getDouble(1).toFloat(),
                        ea.getDouble(2).toFloat(),
                        ea.getDouble(3).toFloat()
                    )
                )
            }.sortedBy { it.timeMs }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun toJson(keys: List<Keyframe>): String {
        val array = JSONArray()
        keys.forEach { k ->
            array.put(
                JSONObject()
                    .put("t", k.timeMs)
                    .put("x", k.value.x.toDouble())
                    .put("y", k.value.y.toDouble())
                    .put("s", k.value.scale.toDouble())
                    .put("r", k.value.rotation.toDouble())
                    .put("o", k.value.opacity.toDouble())
                    .put(
                        "e",
                        JSONArray()
                            .put(k.easing.x1.toDouble())
                            .put(k.easing.y1.toDouble())
                            .put(k.easing.x2.toDouble())
                            .put(k.easing.y2.toDouble())
                    )
            )
        }
        return array.toString()
    }

    /** Transform at time t (ms from clip start). No keyframes means no change. */
    fun evaluate(keys: List<Keyframe>, t: Long): Transform {
        if (keys.isEmpty()) return Transform.IDENTITY
        if (t <= keys.first().timeMs) return keys.first().value
        if (t >= keys.last().timeMs) return keys.last().value
        val bIndex = keys.indexOfFirst { it.timeMs > t }
        val a = keys[bIndex - 1]
        val b = keys[bIndex]
        val span = (b.timeMs - a.timeMs).toFloat()
        val p = if (span <= 0f) 1f else (t - a.timeMs) / span
        val e = a.easing.apply(p)
        return Transform(
            x = lerp(a.value.x, b.value.x, e),
            y = lerp(a.value.y, b.value.y, e),
            scale = lerp(a.value.scale, b.value.scale, e),
            rotation = lerp(a.value.rotation, b.value.rotation, e),
            opacity = lerp(a.value.opacity, b.value.opacity, e)
        )
    }

    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    /** Update the keyframe at time t, or add one. */
    fun upsert(keys: List<Keyframe>, t: Long, value: Transform): List<Keyframe> {
        val index = keys.indexOfFirst { abs(it.timeMs - t) <= TOLERANCE_MS }
        return if (index >= 0) {
            keys.toMutableList().also { it[index] = it[index].copy(value = value) }
        } else {
            (keys + Keyframe(t, value, Easing.EASE_IN_OUT)).sortedBy { it.timeMs }
        }
    }

    fun removeNear(keys: List<Keyframe>, t: Long): List<Keyframe> =
        keys.filter { abs(it.timeMs - t) > TOLERANCE_MS }

    /** Set the easing of the keyframe that is active at time t. */
    fun setEasing(keys: List<Keyframe>, t: Long, easing: Easing): List<Keyframe> {
        if (keys.isEmpty()) return keys
        val index = keys.indexOfLast { it.timeMs <= t + TOLERANCE_MS }.let { if (it < 0) 0 else it }
        return keys.toMutableList().also { it[index] = it[index].copy(easing = easing) }
    }

    /** Split keyframes at clip time t into the left and right part (right part restarts at 0). */
    fun split(keys: List<Keyframe>, t: Long): Pair<List<Keyframe>, List<Keyframe>> {
        if (keys.isEmpty()) return Pair(emptyList(), emptyList())
        val at = evaluate(keys, t)
        val easing = keys.lastOrNull { it.timeMs <= t }?.easing ?: Easing.LINEAR
        val left = keys.filter { it.timeMs < t } + Keyframe(t, at, easing)
        val right = listOf(Keyframe(0L, at, easing)) +
            keys.filter { it.timeMs > t }.map { it.copy(timeMs = it.timeMs - t) }
        return Pair(left, right)
    }
}
