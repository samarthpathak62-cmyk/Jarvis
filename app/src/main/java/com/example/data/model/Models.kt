package com.example.data.model

enum class AIProvider(val displayName: String, val description: String, val defaultModel: String) {
    GEMINI(
        displayName = "Google Gemini",
        description = "High-speed reasoning via Gemini direct REST API",
        defaultModel = "gemini-2.5-flash"
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
    val speechRate: Float = 0.95f,
    val pitch: Float = 0.82f,
    val voiceType: String = "jarvis_british_male" // jarvis_british_male, python_david_male, deep_baritone_male, hinglish_indian_male, cyber_robotic_male
)

enum class OrbState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING
}
