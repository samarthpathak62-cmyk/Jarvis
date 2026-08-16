package com.example.data.remote.openrouter

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Streaming

interface OpenRouterApiService {

    @POST("api/v1/chat/completions")
    suspend fun chatCompletions(
        @Header("Authorization") authHeader: String,
        @Header("HTTP-Referer") referer: String = "https://jarvis.rollergaming.ai",
        @Header("X-Title") title: String = "JARVIS AI",
        @Body request: OpenRouterChatRequest
    ): Response<OpenRouterChatResponse>

    @POST("api/v1/chat/completions")
    @Streaming
    suspend fun streamChatCompletions(
        @Header("Authorization") authHeader: String,
        @Header("HTTP-Referer") referer: String = "https://jarvis.rollergaming.ai",
        @Header("X-Title") title: String = "JARVIS AI",
        @Body request: OpenRouterChatRequest
    ): Response<ResponseBody>
}
