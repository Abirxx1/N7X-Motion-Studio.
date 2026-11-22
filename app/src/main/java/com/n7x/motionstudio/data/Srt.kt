package com.n7x.motionstudio.data

/** Reads .srt subtitle files. */
object Srt {
    data class Cue(val startMs: Long, val endMs: Long, val text: String)

    private val timeLine = Regex(
        "(\\d+):(\\d+):(\\d+)[,.](\\d+)\\s*-->\\s*(\\d+):(\\d+):(\\d+)[,.](\\d+)"
    )

    fun parse(content: String): List<Cue> {
        val clean = content.removePrefix("\uFEFF").replace("\r\n", "\n").replace("\r", "\n").trim()
        val cues = mutableListOf<Cue>()
        for (block in clean.split(Regex("\n\\s*\n"))) {
            val lines = block.lines()
            val index = lines.indexOfFirst { timeLine.containsMatchIn(it) }
            if (index < 0) continue
            val m = timeLine.find(lines[index]) ?: continue
            val text = lines.drop(index + 1).joinToString("\n").trim()
            if (text.isEmpty()) continue
            val g = m.groupValues
            val start = toMs(g[1], g[2], g[3], g[4])
            val end = toMs(g[5], g[6], g[7], g[8])
            if (end > start) cues.add(Cue(start, end, text))
        }
        return cues
    }

    private fun toMs(h: String, m: String, s: String, fraction: String): Long {
        val ms = fraction.padEnd(3, '0').take(3).toLong()
        return ((h.toLong() * 60 + m.toLong()) * 60 + s.toLong()) * 1000 + ms
    }
}
