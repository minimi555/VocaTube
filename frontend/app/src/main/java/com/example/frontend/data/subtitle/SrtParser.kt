package com.example.frontend.data.subtitle

/**
 * Parses SRT subtitle text into a list of [Cue].
 *
 * SRT blocks look like:
 * ```
 * 1
 * 00:00:01,000 --> 00:00:04,000
 * Line one
 * Line two
 *
 * 2
 * ...
 * ```
 * We tolerate "." or "," as the millisecond separator and blank lines between blocks.
 */
object SrtParser {

    private val TIME_LINE = Regex(
        """(\d{1,2}):(\d{2}):(\d{2})[,.](\d{1,3})\s*-->\s*(\d{1,2}):(\d{2}):(\d{2})[,.](\d{1,3})"""
    )

    fun parse(srt: String): List<Cue> {
        val cues = ArrayList<Cue>()
        // Normalize newlines, then split into blocks on blank lines.
        val blocks = srt.replace("\r\n", "\n").replace("\r", "\n").split(Regex("\n\\s*\n"))
        for (block in blocks) {
            val lines = block.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
            if (lines.isEmpty()) continue
            // Find the timing line (may be first or second line depending on index presence).
            val timeIdx = lines.indexOfFirst { TIME_LINE.containsMatchIn(it) }
            if (timeIdx < 0) continue
            val m = TIME_LINE.find(lines[timeIdx]) ?: continue
            val start = toMs(m.groupValues, 1)
            val end = toMs(m.groupValues, 5)
            val text = lines.drop(timeIdx + 1).joinToString("\n")
            if (text.isNotEmpty()) cues.add(Cue(start, end, text))
        }
        return cues
    }

    private fun toMs(g: List<String>, base: Int): Long {
        val h = g[base].toLong()
        val min = g[base + 1].toLong()
        val sec = g[base + 2].toLong()
        val millisRaw = g[base + 3]
        // pad/truncate to 3 digits
        val millis = millisRaw.padEnd(3, '0').take(3).toLong()
        return ((h * 60 + min) * 60 + sec) * 1000 + millis
    }
}
