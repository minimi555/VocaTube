package com.example.frontend.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

/**
 * Downloads a raw SRT subtitle file as text.
 *
 * The backend serves subtitles as static .srt files. Pass an absolute, already
 * URL-encoded URL (use [Network.assetUrl] to build one from a "/assets/..." path).
 */
object SubtitleApi {

    /**
     * @param absoluteUrl a fully-qualified, URL-encoded http(s) URL.
     * @return the SRT file contents.
     * @throws Exception on network failure or a non-2xx response (caller handles).
     */
    suspend fun download(absoluteUrl: String): String = withContext(Dispatchers.IO) {
        Network.okHttp.newCall(Request.Builder().url(absoluteUrl).build()).execute().use { resp ->
            if (!resp.isSuccessful) error("HTTP ${resp.code} for $absoluteUrl")
            resp.body?.string() ?: ""
        }
    }
}
