package com.example.frontend.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTOs matching the VocaTube FastAPI responses.
 *
 * GET /words/{word} -> WordDetail
 * GET /videos       -> List<VideoTitle>
 * GET /videos/{id}  -> VideoDetail
 *
 * Note: phrases/sentences come from JSON-encoded TEXT columns on the backend and
 * are passed through without strict validation, so we keep fields defensive
 * (defaults + nullable) to tolerate missing keys.
 */

@Serializable
data class TranslationItem(
    val type: String? = null,
    val translation: String = ""
)

@Serializable
data class PhraseItem(
    val phrase: String = "",
    val translation: String = ""
)

@Serializable
data class SentenceItem(
    val sentence: String = "",
    val translation: String = ""
)

@Serializable
data class WordDetail(
    val word: String = "",
    val translations: List<TranslationItem> = emptyList(),
    val phrases: List<PhraseItem> = emptyList(),
    val sentences: List<SentenceItem> = emptyList(),
    val categories: List<String> = emptyList()
)

@Serializable
data class WordCategoryCheck(
    val word: String = "",
    @SerialName("category_code") val categoryCode: String = "",
    val belongs: Boolean = false
)

@Serializable
data class VideoTitle(
    val id: Int,
    val title: String = ""
)

@Serializable
data class VideoDetail(
    val id: Int,
    val title: String = "",
    @SerialName("video_url") val videoUrl: String = "",
    @SerialName("subtitle_zh_url") val subtitleZhUrl: String? = null,
    @SerialName("subtitle_en_url") val subtitleEnUrl: String? = null
)

// ---- 学习咨询：RAG 备考问答 (POST /ask) ----

@Serializable
data class AskRequest(val question: String, val k: Int = 4)

@Serializable
data class SourceItem(
    val source: String? = null,
    val section: String? = null
)

@Serializable
data class AskResponse(
    val answer: String = "",
    val sources: List<SourceItem> = emptyList()
)

// ---- 学习咨询：学校查询 (GET /schools, POST /school/search, GET /school/history) ----

@Serializable
data class SchoolItem(
    val id: Int,
    val name: String = "",
    val domain: String = "",
    @SerialName("qs_rank") val qsRank: Int = 0
)

@Serializable
data class SchoolSearchRequest(val question: String)

@Serializable
data class SchoolSearchResponse(val answer: String = "")

@Serializable
data class SchoolSearchHistoryItem(
    val id: Int,
    val question: String = "",
    val answer: String = "",
    @SerialName("created_at") val createdAt: String = ""
)
