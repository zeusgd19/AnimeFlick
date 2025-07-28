package com.zeusgd.AnimeFlick

import retrofit2.http.Body
import retrofit2.http.POST

data class TranslateRequest(
    val q: String,
    val source: String,
    val target: String,
    val format: String = "text"
)

data class TranslateResponse(
    val content: ContentTranslation
)

data class ContentTranslation(
    val source_language: String,
    val translation: String
)

interface LibreTranslateApi {
    @POST("translate")
    suspend fun translate(@Body request: TranslateRequest): TranslateResponse
}