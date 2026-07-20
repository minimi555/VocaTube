package com.example.frontend.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit interface for the VocaTube backend.
 *
 * The {word} path segment is case-sensitive on the backend (utf8mb4_bin collation),
 * so pass the word exactly as the user typed it. Retrofit URL-encodes path values
 * by default.
 */
interface ApiService {

    @GET("words/{word}")
    suspend fun getWord(@Path("word") word: String): WordDetail

    @GET("words/{word}/category/{code}")
    suspend fun checkCategory(
        @Path("word") word: String,
        @Path("code") code: String
    ): WordCategoryCheck

    @GET("videos")
    suspend fun getVideos(): List<VideoTitle>

    @GET("videos/{id}")
    suspend fun getVideo(@Path("id") id: Int): VideoDetail

    // ---- 学习咨询 ----

    @POST("ask")
    suspend fun ask(@Body request: AskRequest): AskResponse

    @GET("schools")
    suspend fun getSchools(): List<SchoolItem>

    // The LangChain agent behind this endpoint can take 10-30s; the header is
    // stripped and turned into a per-call read timeout by Network's interceptor.
    @Headers("${Network.HEADER_READ_TIMEOUT}: 180")
    @POST("school/search")
    suspend fun schoolSearch(@Body request: SchoolSearchRequest): SchoolSearchResponse

    @GET("school/history")
    suspend fun schoolHistory(@Query("limit") limit: Int = 20): List<SchoolSearchHistoryItem>
}
