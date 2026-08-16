package com.example.data.model

enum class AIProvider(val displayName: String, val description: String, val defaultModel: String) {
    GEMINI(
        displayName = "Google Gemini",
        description = "High-speed reasoning via Gemini direct REST API",
        defaultModel = "gemini-3.5-flash"
    ),
    OPENROUTER(
        displayName = "OpenRouter",
        description = "Universal AI gateway with multi-model support",
        defaultModel = "openai/gpt-4o-mini"
    )
}

data class ModelOption(
    val id: String,
    val name: String,
    val provider: AIProvider,
    val description: String
)

data class UserProfile(
    val id: String,
    val displayName: String,
    val email: String,
    val photoUrl: String? = null,
    val isAuthenticated: Boolean = false
)

data class VoiceSettings(
    val autoSpeak: Boolean = false,
    val speechRate: Float = 1.0f,
    val pitch: Float = 0.95f,
    val voiceType: String = "calm_natural" // calm_natural, calm_british, deep_resonant, smooth_neutral
)

enum class OrbState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING
}
