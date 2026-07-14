package com.example.frontend.data.remote

import retrofit2.http.GET
import retrofit2.http.Path

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
}
