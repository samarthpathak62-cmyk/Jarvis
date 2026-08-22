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
    val autoSpeak: Boolean = true,
    val speechRate: Float = 0.95f,
    val pitch: Float = 0.85f,
    val voiceType: String = "expressive_british_hinglish", // expressive_british_hinglish, jarvis_british_male, python_david_male, deep_baritone_male, hinglish_indian_male, cyber_robotic_male
    val enableLaughterSimulation: Boolean = true
)

enum class OrbState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING
}

data class MacroStep(
    val title: String,
    val actionType: String,
    val target: String? = null,
    val input: String? = null,
    val delaySeconds: Long = 0L
)

data class MacroRoutine(
    val id: String,
    val title: String,
    val subtitle: String,
    val iconName: String,
    val steps: List<MacroStep>
)

data class ScheduledTaskItem(
    val id: String,
    val title: String,
    val target: String,
    val totalSeconds: Long,
    val remainingSeconds: Long,
    val status: String = "PENDING"
)

data class BatteryTelemetry(
    val percentage: Int,
    val isCharging: Boolean,
    val temperatureCelsius: Float,
    val health: String,
    val level: Int = percentage
)

data class MemoryTelemetry(
    val usedRamMb: Long,
    val totalRamMb: Long,
    val ramPercent: Int,
    val freeStorageGb: Float,
    val totalStorageGb: Float,
    val availableRamMb: Long = (totalRamMb - usedRamMb).coerceAtLeast(0)
)
