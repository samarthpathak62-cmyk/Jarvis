package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.JarvisDatabase
import com.example.data.local.entity.ChatMessageEntity
import com.example.data.local.entity.ChatSessionEntity
import com.example.data.model.AIProvider
import com.example.data.model.ModelOption
import com.example.data.model.OrbState
import com.example.data.model.UserProfile
import com.example.data.model.VoiceSettings
import com.example.data.remote.AIServiceManager
import com.example.data.repository.ChatRepository
import com.example.data.security.SecureKeyStorage
import com.example.voice.VoiceAssistantManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class Screen {
    object Splash : Screen()
    object Auth : Screen()
    object AISetup : Screen()
    object Chat : Screen()
    object Settings : Screen()
}

class JarvisViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    val secureStorage = SecureKeyStorage(context)
    private val database = JarvisDatabase.getDatabase(context)
    val chatRepository = ChatRepository(database.chatDao())
    val aiServiceManager = AIServiceManager(secureStorage)
    val voiceManager = VoiceAssistantManager(context, viewModelScope)

    private val _currentScreen = MutableStateFlow<Screen>(Screen.Splash)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _userProfile = MutableStateFlow(secureStorage.getUserSession())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _currentSessionId = MutableStateFlow<Long>(1L)
    val currentSessionId: StateFlow<Long> = _currentSessionId.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessageEntity>>(emptyList())
    val messages: StateFlow<List<ChatMessageEntity>> = _messages.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _statusText = MutableStateFlow("ONLINE // STANDBY")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    private val _keyValidationState = MutableStateFlow<String?>(null)
    val keyValidationState: StateFlow<String?> = _keyValidationState.asStateFlow()

    private val _isValidatingKey = MutableStateFlow(false)
    val isValidatingKey: StateFlow<Boolean> = _isValidatingKey.asStateFlow()

    val availableModels = listOf(
        ModelOption("gemini-3.5-flash", "Gemini 3.5 Flash (Fast)", AIProvider.GEMINI, "Ultra-fast response with high reasoning"),
        ModelOption("gemini-3.1-pro-preview", "Gemini 3.1 Pro (Advanced)", AIProvider.GEMINI, "Complex tasks, coding, deep STEM"),
        ModelOption("gemini-3.1-flash-lite-preview", "Gemini 3.1 Flash Lite", AIProvider.GEMINI, "Lightweight efficient companion"),
        ModelOption("openai/gpt-4o-mini", "GPT-4o Mini (OpenRouter)", AIProvider.OPENROUTER, "Fast, lightweight multimodal model"),
        ModelOption("anthropic/claude-3.5-haiku", "Claude 3.5 Haiku (OpenRouter)", AIProvider.OPENROUTER, "High-speed intelligent reasoning"),
        ModelOption("meta-llama/llama-3.3-70b-instruct", "Llama 3.3 70B (OpenRouter)", AIProvider.OPENROUTER, "Open-weight powerhouse"),
        ModelOption("deepseek/deepseek-chat", "DeepSeek V3 (OpenRouter)", AIProvider.OPENROUTER, "High capability reasoning")
    )

    init {
        initSession()
        observeVoiceSettings()
    }

    private fun initSession() {
        viewModelScope.launch {
            val sessions = chatRepository.getAllSessions()
            sessions.collect { list ->
                if (list.isEmpty()) {
                    val newId = chatRepository.createNewSession("Initial System Boot")
                    _currentSessionId.value = newId
                    loadMessages(newId)
                } else if (_currentSessionId.value == 1L && list.isNotEmpty()) {
                    _currentSessionId.value = list.first().id
                    loadMessages(list.first().id)
                }
            }
        }
    }

    private fun loadMessages(sessionId: Long) {
        viewModelScope.launch {
            chatRepository.getMessagesForSession(sessionId).collect { msgList ->
                _messages.value = msgList
            }
        }
    }

    private fun observeVoiceSettings() {
        voiceManager.applyVoiceSettings(secureStorage.getVoiceSettings())
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun handleGoogleSignIn(name: String = "Commander", email: String = "user@gmail.com", photoUrl: String? = null) {
        val user = UserProfile(
            id = "usr_${System.currentTimeMillis()}",
            displayName = name,
            email = email,
            photoUrl = photoUrl,
            isAuthenticated = true
        )
        secureStorage.saveUserSession(user)
        _userProfile.value = user

        if (hasAnyApiKey()) {
            navigateTo(Screen.Chat)
        } else {
            navigateTo(Screen.AISetup)
        }
    }

    fun signOut() {
        secureStorage.clearUserSession()
        _userProfile.value = secureStorage.getUserSession()
        navigateTo(Screen.Auth)
    }

    fun hasAnyApiKey(): Boolean {
        return secureStorage.hasApiKey(AIProvider.GEMINI) || secureStorage.hasApiKey(AIProvider.OPENROUTER)
    }

    fun validateAndSaveApiKey(provider: AIProvider, key: String, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _isValidatingKey.value = true
            _keyValidationState.value = "Verifying cryptographic link..."
            val result = aiServiceManager.validateApiKey(provider, key)
            _isValidatingKey.value = false

            if (result.isSuccess) {
                secureStorage.saveApiKey(provider, key)
                secureStorage.activeProvider = provider
                val user = secureStorage.getUserSession()
                if (!user.isAuthenticated) {
                    val defaultUser = UserProfile(
                        id = "usr_${System.currentTimeMillis()}",
                        displayName = "Commander",
                        email = "commander@jarvis.ai",
                        photoUrl = null,
                        isAuthenticated = true
                    )
                    secureStorage.saveUserSession(defaultUser)
                    _userProfile.value = defaultUser
                }
                _keyValidationState.value = null
                onComplete(true, "AI Brain successfully synchronized.")
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Validation failed"
                _keyValidationState.value = errorMsg
                onComplete(false, errorMsg)
            }
        }
    }

    fun saveDirectApiKey(provider: AIProvider, key: String) {
        secureStorage.saveApiKey(provider, key)
        secureStorage.activeProvider = provider
    }

    fun removeApiKey(provider: AIProvider) {
        secureStorage.removeApiKey(provider)
    }

    fun sendMessage(userText: String) {
        if (userText.isBlank() || _isGenerating.value) return
        val currentSession = _currentSessionId.value

        viewModelScope.launch {
            voiceManager.stopSpeaking()
            _isGenerating.value = true
            _statusText.value = "PROCESSING QUERY..."

            // 1. Save User Message
            chatRepository.addMessage(
                sessionId = currentSession,
                sender = "user",
                content = userText
            )

            // 2. Fetch context
            val history = chatRepository.getMessagesList(currentSession)

            // 3. Add placeholder assistant message
            val assistantMsgId = chatRepository.addMessage(
                sessionId = currentSession,
                sender = "assistant",
                content = "...",
                isStreaming = true
            )

            val fullResponseBuffer = StringBuilder()
            val result = aiServiceManager.generateResponse(
                messages = history,
                onStreamChunk = { chunk ->
                    fullResponseBuffer.append(chunk)
                    viewModelScope.launch {
                        chatRepository.updateMessageContent(
                            messageId = assistantMsgId,
                            content = fullResponseBuffer.toString(),
                            isStreaming = true
                        )
                    }
                }
            )

            _isGenerating.value = false

            if (result.isSuccess) {
                val finalText = result.getOrNull() ?: fullResponseBuffer.toString()
                chatRepository.updateMessageContent(
                    messageId = assistantMsgId,
                    content = finalText,
                    isStreaming = false,
                    isError = false
                )
                _statusText.value = "ONLINE // READY"

                // Auto-speak if enabled in settings
                if (secureStorage.getVoiceSettings().autoSpeak) {
                    voiceManager.speak(finalText)
                }
            } else {
                val errorText = result.exceptionOrNull()?.message ?: AIServiceManager.ERROR_API_FAILURE
                chatRepository.updateMessageContent(
                    messageId = assistantMsgId,
                    content = errorText,
                    isStreaming = false,
                    isError = true
                )
                _statusText.value = "LINK INTERRUPTED"
            }
        }
    }

    fun startVoiceInput() {
        voiceManager.startListening { recognized ->
            if (recognized.isNotBlank()) {
                sendMessage(recognized)
            }
        }
    }

    fun stopVoiceInput() {
        voiceManager.stopListening()
    }

    fun speakMessage(text: String) {
        voiceManager.speak(text)
    }

    fun stopSpeaking() {
        voiceManager.stopSpeaking()
    }

    fun clearCurrentChat() {
        viewModelScope.launch {
            chatRepository.clearSessionMessages(_currentSessionId.value)
            _statusText.value = "MEMORY PURGED // READY"
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            chatRepository.clearAllData()
            secureStorage.clearAllData()
            _userProfile.value = secureStorage.getUserSession()
            val newId = chatRepository.createNewSession("New Transmission")
            _currentSessionId.value = newId
            loadMessages(newId)
            navigateTo(Screen.Splash)
        }
    }

    fun computeOrbState(isListening: Boolean, isSpeaking: Boolean, isGenerating: Boolean): OrbState {
        return when {
            isListening -> OrbState.LISTENING
            isGenerating -> OrbState.THINKING
            isSpeaking -> OrbState.SPEAKING
            else -> OrbState.IDLE
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.release()
    }
}
