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

// ---- Quiz (POST /quiz/generate, POST /quiz/grade) ----

@Serializable
data class QuizGenerateRequest(
    @SerialName("subtitle_text") val subtitleText: String,
    @SerialName("category_code") val categoryCode: String,
)

@Serializable
data class QuizReadingQuestion(
    val index: Int,
    val type: String = "",
    val question: String = "",
    val options: Map<String, String> = emptyMap(),
)

@Serializable
data class QuizGenerateResponse(
    @SerialName("quiz_id") val quizId: String = "",
    @SerialName("cloze_passage") val clozePassage: String = "",
    @SerialName("cloze_count") val clozeCount: Int = 0,
    @SerialName("reading_passage") val readingPassage: String = "",
    @SerialName("reading_questions") val readingQuestions: List<QuizReadingQuestion> = emptyList(),
)

@Serializable
data class QuizGradeRequest(
    @SerialName("quiz_id") val quizId: String,
    @SerialName("cloze_answers") val clozeAnswers: Map<String, String>,
    @SerialName("reading_answers") val readingAnswers: Map<String, String>,
)

@Serializable
data class QuizScore(
    @SerialName("cloze_correct") val clozeCorrect: Int = 0,
    @SerialName("cloze_total") val clozeTotal: Int = 0,
    @SerialName("reading_correct") val readingCorrect: Int = 0,
    @SerialName("reading_total") val readingTotal: Int = 0,
    @SerialName("total_correct") val totalCorrect: Int = 0,
    @SerialName("total_questions") val totalQuestions: Int = 0,
)

@Serializable
data class QuizClozeResult(
    val index: Int,
    @SerialName("user_answer") val userAnswer: String = "",
    @SerialName("correct_answer") val correctAnswer: String = "",
    @SerialName("is_correct") val isCorrect: Boolean = false,
    val lemma: String = "",
    val explanation: String? = null,
)

@Serializable
data class QuizReadingResult(
    val index: Int,
    @SerialName("user_answer") val userAnswer: String = "",
    @SerialName("correct_answer") val correctAnswer: String = "",
    @SerialName("is_correct") val isCorrect: Boolean = false,
    val explanation: String? = null,
)

@Serializable
data class QuizGradeResponse(
    val score: QuizScore = QuizScore(),
    @SerialName("cloze_results") val clozeResults: List<QuizClozeResult> = emptyList(),
    @SerialName("reading_results") val readingResults: List<QuizReadingResult> = emptyList(),
)
