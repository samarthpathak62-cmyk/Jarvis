package com.example.data.remote.openrouter

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OpenRouterChatRequest(
    val model: String,
    val messages: List<OpenRouterMessage>,
    val temperature: Float? = null,
    val stream: Boolean = false
)

@JsonClass(generateAdapter = true)
data class OpenRouterMessage(
    val role: String, // "system", "user", "assistant"
    val content: String
)

@JsonClass(generateAdapter = true)
data class OpenRouterChatResponse(
    val id: String? = null,
    val choices: List<OpenRouterChoice>? = null,
    val error: OpenRouterError? = null
)

@JsonClass(generateAdapter = true)
data class OpenRouterChoice(
    val index: Int? = null,
    val message: OpenRouterMessage? = null,
    val delta: OpenRouterDelta? = null,
    val finish_reason: String? = null
)

@JsonClass(generateAdapter = true)
data class OpenRouterDelta(
    val content: String? = null
)

@JsonClass(generateAdapter = true)
data class OpenRouterError(
    val code: Any? = null,
    val message: String? = null
)
