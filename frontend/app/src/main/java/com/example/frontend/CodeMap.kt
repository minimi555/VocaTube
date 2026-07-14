package com.example.frontend

/**
 * 单词分类 code → 中文名映射。
 *
 * 后端 categories 表有 (code, name) 但 API 只返回 code，所以客户端硬编码这个映射。
 * 若后端新增分类，同步更新此表。
 */
object CodeMap {
    private val map = mapOf(
        "CET4" to "大学英语四级",
        "CET6" to "大学英语六级",
        "IELTS" to "雅思",
        "SAT" to "SAT",
        "TOEFL" to "托福",
        "kaoyan" to "考研",
    )

    fun label(code: String): String = map[code] ?: code
}
