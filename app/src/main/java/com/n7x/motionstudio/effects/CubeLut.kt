package com.n7x.motionstudio.effects

import java.io.File

/** Reads a 3D .cube LUT file. The result is cube[r][g][b] = packed ARGB color. */
object CubeLut {
    fun parse(file: File): Array<Array<IntArray>> {
        var size = 0
        var data = IntArray(0)
        var count = 0
        file.useLines { lines ->
            for (raw in lines) {
                val line = raw.trim()
                if (line.isEmpty() || line.startsWith("#")) continue
                if (line.startsWith("LUT_1D_SIZE")) {
                    throw IllegalArgumentException("1D LUTs are not supported")
                }
                if (line.startsWith("LUT_3D_SIZE")) {
                    size = line.split(Regex("\\s+"))[1].toInt()
                    require(size in 2..65) { "unsupported LUT size $size" }
                    data = IntArray(size * size * size)
                    continue
                }
                val first = line[0]
                if (!(first.isDigit() || first == '-' || first == '.')) continue
                require(size > 0) { "LUT_3D_SIZE is missing" }
                val parts = line.split(Regex("\\s+"))
                require(parts.size >= 3) { "bad data line" }
                require(count < data.size) { "too many data lines" }
                val r = (parts[0].toFloat().coerceIn(0f, 1f) * 255f + 0.5f).toInt()
                val g = (parts[1].toFloat().coerceIn(0f, 1f) * 255f + 0.5f).toInt()
                val b = (parts[2].toFloat().coerceIn(0f, 1f) * 255f + 0.5f).toInt()
                data[count] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
                count++
            }
        }
        require(size > 0) { "LUT_3D_SIZE is missing" }
        require(count == data.size) { "expected ${data.size} colors but found $count" }
        val cube = Array(size) { Array(size) { IntArray(size) } }
        for (n in data.indices) {
            val r = n % size
            val g = (n / size) % size
            val b = n / (size * size)
            cube[r][g][b] = data[n]
        }
        return cube
    }
}
