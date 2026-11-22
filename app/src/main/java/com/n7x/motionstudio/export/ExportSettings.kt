package com.n7x.motionstudio.export

import kotlin.math.roundToInt

/** What the user chose in the export dialog. The canvas is 16:9 landscape for now. */
data class ExportSettings(
    val resolution: Int = 1080,      // height in pixels: 720, 1080, 1440, 2160
    val fps: Int = 0,                // 0 = keep the source frame rate, otherwise the highest rate to keep
    val h265: Boolean = false,
    val customMbps: Float = 0f,      // 0 = automatic
    val audioKbps: Int = 128         // only used for the size estimate, the phone's AAC encoder decides
) {
    val width: Int get() = resolution * 16 / 9
    val height: Int get() = resolution
    val mime: String get() = if (h265) "video/hevc" else "video/avc"

    fun autoMbps(): Float {
        val base = when (resolution) {
            720 -> 6f
            1080 -> 12f
            1440 -> 20f
            else -> 45f
        }
        val rate = if (fps >= 60) 1.5f else 1f
        val codec = if (h265) 0.7f else 1f
        return (base * rate * codec * 10f).roundToInt() / 10f
    }

    fun videoMbps(): Float = if (customMbps > 0f) customMbps else autoMbps()

    fun estimatedBytes(durationMs: Long): Long =
        ((videoMbps() * 1_000_000f + audioKbps * 1000f) * (durationMs / 1000f) / 8f).toLong()
}
