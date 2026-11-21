package com.n7x.motionstudio.effects

import com.n7x.motionstudio.data.MediaClipEntity

/** Color settings of a clip. */
data class Look(
    val filterId: String,
    val brightness: Float,
    val contrast: Float,
    val saturation: Float
)

fun MediaClipEntity.look() = Look(filterId, brightness, contrast, saturation)

/**
 * 4x5 color matrices (row major, offsets in 0..255 like android.graphics.ColorMatrix).
 * The same matrix will be reused by the export engine.
 */
object ColorOps {
    val FILTERS = listOf(
        "none" to "None",
        "mono" to "Mono",
        "sepia" to "Sepia",
        "warm" to "Warm",
        "cool" to "Cool",
        "vivid" to "Vivid",
        "noir" to "Noir",
        "fade" to "Fade",
        "invert" to "Invert"
    )

    private fun identity() = floatArrayOf(
        1f, 0f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f, 0f,
        0f, 0f, 1f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    )

    /** Result applies [first] and then [second]. */
    fun then(first: FloatArray, second: FloatArray): FloatArray {
        val out = FloatArray(20)
        for (i in 0 until 4) {
            for (j in 0 until 5) {
                var sum = 0f
                for (k in 0 until 4) sum += second[i * 5 + k] * first[k * 5 + j]
                if (j == 4) sum += second[i * 5 + 4]
                out[i * 5 + j] = sum
            }
        }
        return out
    }

    private fun brightness(b: Float): FloatArray {
        val m = identity()
        m[4] = 255f * b
        m[9] = 255f * b
        m[14] = 255f * b
        return m
    }

    private fun contrast(c: Float): FloatArray {
        val f = 1f + c
        val offset = 127.5f * (1f - f)
        val m = identity()
        m[0] = f
        m[6] = f
        m[12] = f
        m[4] = offset
        m[9] = offset
        m[14] = offset
        return m
    }

    private fun saturation(s: Float): FloatArray {
        val lr = 0.299f
        val lg = 0.587f
        val lb = 0.114f
        val inv = 1f - s
        return floatArrayOf(
            lr * inv + s, lg * inv, lb * inv, 0f, 0f,
            lr * inv, lg * inv + s, lb * inv, 0f, 0f,
            lr * inv, lg * inv, lb * inv + s, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    }

    private fun scaleRgb(r: Float, g: Float, b: Float): FloatArray {
        val m = identity()
        m[0] = r
        m[6] = g
        m[12] = b
        return m
    }

    private fun preset(id: String): FloatArray = when (id) {
        "mono" -> saturation(0f)
        "sepia" -> floatArrayOf(
            0.393f, 0.769f, 0.189f, 0f, 0f,
            0.349f, 0.686f, 0.168f, 0f, 0f,
            0.272f, 0.534f, 0.131f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
        "warm" -> scaleRgb(1.08f, 1.0f, 0.86f)
        "cool" -> scaleRgb(0.88f, 1.0f, 1.1f)
        "vivid" -> then(saturation(1.4f), contrast(0.12f))
        "noir" -> then(saturation(0f), contrast(0.35f))
        "fade" -> then(contrast(-0.2f), brightness(0.06f))
        "invert" -> floatArrayOf(
            -1f, 0f, 0f, 0f, 255f,
            0f, -1f, 0f, 0f, 255f,
            0f, 0f, -1f, 0f, 255f,
            0f, 0f, 0f, 1f, 0f
        )
        else -> identity()
    }

    /** Final matrix, or null when nothing changes. */
    fun build(filterId: String, brightness: Float, contrast: Float, saturation: Float): FloatArray? {
        if (filterId == "none" && brightness == 0f && contrast == 0f && saturation == 1f) return null
        var m = preset(filterId)
        if (saturation != 1f) m = then(m, saturation(saturation))
        if (contrast != 0f) m = then(m, contrast(contrast))
        if (brightness != 0f) m = then(m, brightness(brightness))
        return m
    }
}
