package com.example.frontend.data.remote

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Networking singletons for the VocaTube backend.
 *
 * BASE_URL points at 127.0.0.1:8000 because the app is tested over USB. Before
 * launching, forward the phone's localhost to the dev machine's backend with:
 *
 *     adb reverse tcp:8000 tcp:8000
 *
 * SECURITY: the backend is plain HTTP with no auth, and AndroidManifest sets
 * usesCleartextTraffic=true to allow it. This is fine for a USB/LAN debug
 * prototype only. Before shipping, move to HTTPS (or a scoped
 * network-security-config) and add access control on the backend.
 */
object Network {

    const val BASE_URL = "http://127.0.0.1:8000/"

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    val okHttp: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
        )
        .build()

    val api: ApiService = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttp)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(ApiService::class.java)

    /**
     * Turn a relative "/assets/..." path from the backend into an absolute,
     * per-segment URL-encoded URL. Segments contain spaces and full-width
     * Unicode, so each segment is encoded while the "/" separators are kept.
     * An already-absolute http(s) URL is returned unchanged.
     */
    fun assetUrl(relativeUrl: String): String {
        if (relativeUrl.startsWith("http://") || relativeUrl.startsWith("https://")) {
            return relativeUrl
        }
        val base = BASE_URL.trimEnd('/')
        val encodedPath = relativeUrl
            .split("/")
            .joinToString("/") { segment ->
                if (segment.isEmpty()) segment
                else java.net.URLEncoder.encode(segment, "UTF-8").replace("+", "%20")
            }
        return base + encodedPath
    }
}
