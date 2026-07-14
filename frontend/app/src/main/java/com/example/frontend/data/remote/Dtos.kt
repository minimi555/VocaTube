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
