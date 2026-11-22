package com.n7x.motionstudio.export

import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Writes a silent WAV file. Used to delay a music layer so it starts at the right time. */
object SilenceWav {
    private const val SAMPLE_RATE = 44100
    private const val CHANNELS = 2
    private const val BYTES_PER_SAMPLE = 2

    fun write(file: File, durationMs: Long) {
        val ms = durationMs.coerceIn(1L, 15L * 60L * 1000L)
        val dataSize = (ms * SAMPLE_RATE / 1000L).toInt() * CHANNELS * BYTES_PER_SAMPLE
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray())
        header.putInt(36 + dataSize)
        header.put("WAVE".toByteArray())
        header.put("fmt ".toByteArray())
        header.putInt(16)
        header.putShort(1)
        header.putShort(CHANNELS.toShort())
        header.putInt(SAMPLE_RATE)
        header.putInt(SAMPLE_RATE * CHANNELS * BYTES_PER_SAMPLE)
        header.putShort((CHANNELS * BYTES_PER_SAMPLE).toShort())
        header.putShort((BYTES_PER_SAMPLE * 8).toShort())
        header.put("data".toByteArray())
        header.putInt(dataSize)
        FileOutputStream(file).buffered().use { out ->
            out.write(header.array())
            val chunk = ByteArray(64 * 1024)
            var left = dataSize
            while (left > 0) {
                val n = minOf(left, chunk.size)
                out.write(chunk, 0, n)
                left -= n
            }
        }
    }
}
