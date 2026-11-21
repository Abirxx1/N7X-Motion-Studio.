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
        "vivid" to "Vivid",
        "pop" to "Pop",
        "dramatic" to "Dramatic",
        "cinema" to "Cinema",
        "cyber" to "Cyber",
        "warm" to "Warm",
        "golden" to "Golden",
        "sunset" to "Sunset",
        "rose" to "Rose",
        "polaroid" to "Polaroid",
        "vintage" to "Vintage",
        "sepia" to "Sepia",
        "matte" to "Matte",
        "fade" to "Fade",
        "dreamy" to "Dreamy",
        "cool" to "Cool",
        "ice" to "Ice",
        "forest" to "Forest",
        "night" to "Night",
        "mono" to "Mono",
        "silver" to "Silver",
        "noir" to "Noir",
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

    private fun offsetRgb(r: Float, g: Float, b: Float): FloatArray {
        val m = identity()
        m[4] = r
        m[9] = g
        m[14] = b
        return m
    }

    private fun chain(vararg matrices: FloatArray): FloatArray {
        var result = matrices[0]
        for (i in 1 until matrices.size) result = then(result, matrices[i])
        return result
    }

    private fun preset(id: String): FloatArray = when (id) {
        "mono" -> saturation(0f)
        "silver" -> chain(saturation(0f), contrast(0.15f), brightness(0.08f))
        "noir" -> chain(saturation(0f), contrast(0.35f))
        "sepia" -> floatArrayOf(
            0.393f, 0.769f, 0.189f, 0f, 0f,
            0.349f, 0.686f, 0.168f, 0f, 0f,
            0.272f, 0.534f, 0.131f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
        "vintage" -> chain(saturation(0.7f), scaleRgb(1.1f, 1.0f, 0.82f), contrast(-0.12f), brightness(0.04f))
        "polaroid" -> chain(scaleRgb(1.06f, 1.0f, 0.92f), saturation(1.2f), contrast(0.1f), brightness(0.04f))
        "warm" -> scaleRgb(1.08f, 1.0f, 0.86f)
        "golden" -> chain(scaleRgb(1.12f, 1.04f, 0.78f), brightness(0.06f), saturation(1.1f))
        "sunset" -> chain(scaleRgb(1.18f, 0.95f, 0.8f), saturation(1.25f), contrast(0.08f))
        "rose" -> chain(scaleRgb(1.12f, 0.95f, 1.02f), brightness(0.04f))
        "cool" -> scaleRgb(0.88f, 1.0f, 1.1f)
        "ice" -> chain(scaleRgb(0.85f, 1.0f, 1.18f), saturation(0.85f), brightness(0.05f))
        "forest" -> chain(scaleRgb(0.9f, 1.1f, 0.9f), saturation(1.2f))
        "night" -> chain(scaleRgb(0.7f, 0.85f, 1.1f), brightness(-0.12f))
        "vivid" -> chain(saturation(1.4f), contrast(0.12f))
        "pop" -> chain(saturation(1.6f), contrast(0.2f))
        "dramatic" -> chain(contrast(0.4f), saturation(1.15f), brightness(-0.05f))
        "matte" -> chain(contrast(-0.25f), brightness(0.08f), saturation(0.85f))
        "fade" -> chain(contrast(-0.2f), brightness(0.06f))
        "dreamy" -> chain(brightness(0.1f), contrast(-0.15f), saturation(1.2f), scaleRgb(1.04f, 1.0f, 1.06f))
        "cinema" -> chain(scaleRgb(1.1f, 1.0f, 0.95f), offsetRgb(-6f, 2f, 8f), contrast(0.15f), saturation(1.1f))
        "cyber" -> chain(scaleRgb(1.15f, 0.9f, 1.25f), contrast(0.2f), saturation(1.3f))
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
