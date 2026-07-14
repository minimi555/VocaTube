package com.example.frontend.data.subtitle

/**
 * 一条字幕：起止时间（毫秒）+ 文本。
 */
data class Cue(
    val startMs: Long,
    val endMs: Long,
    val text: String,
)

/**
 * 一路字幕（中文或英文），cues 已按开始时间升序排列。
 * 用二分查找按当前播放位置定位当前应显示的那一条。
 */
class Subtitles(val cues: List<Cue>) {

    /**
     * 返回在 positionMs 时刻应显示的字幕文本；没有命中则返回 null。
     */
    fun textAt(positionMs: Long): String? {
        if (cues.isEmpty()) return null
        var lo = 0
        var hi = cues.size - 1
        var ans = -1
        // 找最后一个 startMs <= positionMs 的 cue
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            if (cues[mid].startMs <= positionMs) {
                ans = mid
                lo = mid + 1
            } else {
                hi = mid - 1
            }
        }
        if (ans < 0) return null
        val cue = cues[ans]
        return if (positionMs <= cue.endMs) cue.text else null
    }

    companion object {
        val EMPTY = Subtitles(emptyList())
    }
}
