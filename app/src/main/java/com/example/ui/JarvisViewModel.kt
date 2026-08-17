package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.automation.AutomationActionType
import com.example.automation.DeviceAutomationManager
import com.example.automation.ParsedCommand
import com.example.automation.ScreenHierarchySummary
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
    val automationManager = DeviceAutomationManager(context)

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
        ModelOption("gemini-2.5-flash", "Gemini 2.5 Flash (Fast)", AIProvider.GEMINI, "Ultra-fast response with high reasoning"),
        ModelOption("gemini-2.0-flash", "Gemini 2.0 Flash (Fast)", AIProvider.GEMINI, "High-speed reasoning companion"),
        ModelOption("gemini-2.5-pro", "Gemini 2.5 Pro (Advanced)", AIProvider.GEMINI, "Complex tasks, coding, deep STEM"),
        ModelOption("gemini-3.1-pro-preview", "Gemini 3.1 Pro (Preview)", AIProvider.GEMINI, "Next-gen reasoning preview"),
        ModelOption("gemini-3.1-flash-lite-preview", "Gemini 3.1 Flash Lite", AIProvider.GEMINI, "Lightweight efficient companion"),
        ModelOption("openai/gpt-4o-mini", "GPT-4o Mini (OpenRouter)", AIProvider.OPENROUTER, "Fast, lightweight multimodal model"),
        ModelOption("anthropic/claude-3.5-haiku", "Claude 3.5 Haiku (OpenRouter)", AIProvider.OPENROUTER, "High-speed intelligent reasoning"),
        ModelOption("meta-llama/llama-3.3-70b-instruct", "Llama 3.3 70B (OpenRouter)", AIProvider.OPENROUTER, "Open-weight powerhouse"),
        ModelOption("deepseek/deepseek-chat", "DeepSeek V3 (OpenRouter)", AIProvider.OPENROUTER, "High capability reasoning")
    )

    private var messageJob: kotlinx.coroutines.Job? = null

    init {
        initSession()
        observeVoiceSettings()
    }

    private fun initSession() {
        viewModelScope.launch {
            try {
                val sessions = chatRepository.getAllSessions()
                sessions.collect { list ->
                    if (list.isEmpty()) {
                        val newId = chatRepository.createNewSession("Initial System Boot")
                        _currentSessionId.value = newId
                        loadMessages(newId)
                    } else if (_currentSessionId.value == 1L && list.isNotEmpty()) {
                        val firstId = list.first().id
                        _currentSessionId.value = firstId
                        loadMessages(firstId)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("JarvisViewModel", "Error in initSession", e)
            }
        }
    }

    fun loadMessages(sessionId: Long) {
        messageJob?.cancel()
        messageJob = viewModelScope.launch {
            try {
                chatRepository.getMessagesForSession(sessionId).collect { msgList ->
                    _messages.value = msgList
                }
            } catch (e: Exception) {
                android.util.Log.e("JarvisViewModel", "Error loading messages for session: $sessionId", e)
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
        return secureStorage.hasApiKey(AIProvider.GEMINI) || secureStorage.hasApiKey(AIProvider.OPENROUTER) ||
                (com.example.BuildConfig.GEMINI_API_KEY.isNotBlank() && com.example.BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY")
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

    fun openAccessibilitySettings() {
        automationManager.openAccessibilitySettings()
    }

    fun isAccessibilityEnabled(): Boolean {
        return automationManager.getAccessibilityStatus()
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

            // 2. Add placeholder assistant message
            val assistantMsgId = chatRepository.addMessage(
                sessionId = currentSession,
                sender = "assistant",
                content = "...",
                isStreaming = true
            )

            // 3. Check for Device Automation Command
            val parsedCmd = automationManager.parseCommand(userText)

            if (parsedCmd != null && parsedCmd.actionType != AutomationActionType.GENERATE_TEXT) {
                handleDeviceAutomation(parsedCmd, assistantMsgId, currentSession, userText)
            } else {
                handleGenerativeAIResponse(assistantMsgId, currentSession)
            }
        }
    }

    private suspend fun handleDeviceAutomation(
        command: ParsedCommand,
        assistantMsgId: Long,
        currentSession: Long,
        userText: String
    ) {
        when (command.actionType) {
            AutomationActionType.SAFETY_CONFIRMATION_REQUIRED -> {
                val warningText = "⚠️ **SAFETY BARRIER TRIGGERED**\n\nCommand: `$userText`\n\nIs request me sensitive ya irreversible action shamil hai. Safety policy ke tahat bina explicit verbal confirmation ke ye execute nahi kiya ja sakta."
                chatRepository.updateMessageContent(assistantMsgId, warningText, isStreaming = false, isError = false)
                _isGenerating.value = false
                _statusText.value = "ONLINE // READY"
                if (secureStorage.getVoiceSettings().autoSpeak) {
                    voiceManager.speak("Safety restriction: Yeh command execute karne ke liye confirmation zaroori hai.")
                }
                return
            }

            AutomationActionType.ANALYZE_SCREEN -> {
                _statusText.value = "INSPECTING SCREEN HIERARCHY..."
                val autoResult = automationManager.analyzeScreen()
                if (autoResult.success && autoResult.data is ScreenHierarchySummary) {
                    val summary = autoResult.data
                    val contextPrompt = "User Command: \"$userText\"\n\nCURRENT SCREEN UI INSPECTION:\nApp: ${summary.packageName}\n${summary.summaryText}\n\nPlease analyze this screen data and describe clearly to the user in Hinglish what is on the screen and what actions are available. Never invent fake information."

                    val response = generateAISummary(contextPrompt, assistantMsgId, currentSession)
                    if (response.isBlank()) {
                        val fallback = "📱 **SCREEN ANALYSIS [${summary.packageName}]**\n\n${summary.summaryText}"
                        chatRepository.updateMessageContent(assistantMsgId, fallback, isStreaming = false, isError = false)
                    }
                } else {
                    val msg = "⚠️ **SCREEN ANALYSIS UNAVAILABLE**\n\n${autoResult.message}\n\n💡 *Tip: Settings me jaakar 'JARVIS Automation Service' ko ON karein taaki screen read ki ja sake.*"
                    chatRepository.updateMessageContent(assistantMsgId, msg, isStreaming = false, isError = false)
                    if (secureStorage.getVoiceSettings().autoSpeak) {
                        voiceManager.speak("Screen analyze karne ke liye Accessibility permission enable karein.")
                    }
                }
                _isGenerating.value = false
                _statusText.value = "ONLINE // READY"
                return
            }

            AutomationActionType.OPEN_APP, AutomationActionType.SEARCH, AutomationActionType.GLOBAL_NAV -> {
                if (command.delaySeconds > 0) {
                    _statusText.value = "DELAY TIMER ACTIVE (${command.delaySeconds}s)..."
                    chatRepository.updateMessageContent(
                        assistantMsgId,
                        "⏳ **AUTOMATION SCHEDULED**\n`ACTION: ${command.actionType.name}`\n`DELAY: ${command.delaySeconds} seconds`\n\nExecuting in ${command.delaySeconds} seconds...",
                        isStreaming = true
                    )
                }

                val autoResult = automationManager.executeCommand(command) { progress ->
                    _statusText.value = progress
                }

                val responseText = if (autoResult.success) {
                    val actionName = autoResult.actionTaken ?: command.actionType.name
                    "⚡ **ACTION EXECUTED**\n`ACTION:` ${actionName}\n`TARGET:` ${command.target ?: "Device"}\n`STATUS:` SUCCESS\n\n${autoResult.message}"
                } else {
                    "❌ **AUTOMATION FAILED**\n`ACTION:` ${command.actionType.name}\n`STATUS:` FAILED\n\n${autoResult.message}"
                }

                chatRepository.updateMessageContent(assistantMsgId, responseText, isStreaming = false, isError = !autoResult.success)
                _isGenerating.value = false
                _statusText.value = "ONLINE // READY"

                if (secureStorage.getVoiceSettings().autoSpeak) {
                    voiceManager.speak(if (autoResult.success) "${command.target ?: "Action"} complete kar diya gaya hai." else "Action perform nahi ho paya.")
                }
                return
            }

            else -> {
                handleGenerativeAIResponse(assistantMsgId, currentSession)
            }
        }
    }

    private suspend fun generateAISummary(prompt: String, assistantMsgId: Long, currentSession: Long): String {
        val tempMessages = listOf(
            ChatMessageEntity(sessionId = currentSession, sender = "user", content = prompt)
        )
        val fullBuffer = StringBuilder()
        val result = aiServiceManager.generateResponse(
            messages = tempMessages,
            onStreamChunk = { chunk ->
                fullBuffer.append(chunk)
                viewModelScope.launch {
                    chatRepository.updateMessageContent(assistantMsgId, fullBuffer.toString(), isStreaming = true)
                }
            }
        )

        return if (result.isSuccess) {
            val finalRes = result.getOrNull() ?: fullBuffer.toString()
            chatRepository.updateMessageContent(assistantMsgId, finalRes, isStreaming = false, isError = false)
            if (secureStorage.getVoiceSettings().autoSpeak) {
                voiceManager.speak(finalRes)
            }
            finalRes
        } else {
            ""
        }
    }

    private suspend fun handleGenerativeAIResponse(assistantMsgId: Long, currentSession: Long) {
        try {
            val history = chatRepository.getMessagesList(currentSession)
            val fullResponseBuffer = StringBuilder()
            var lastDbUpdateMs = 0L

            val result = aiServiceManager.generateResponse(
                messages = history,
                onStreamChunk = { chunk ->
                    fullResponseBuffer.append(chunk)
                    val currentText = fullResponseBuffer.toString()
                    val now = System.currentTimeMillis()
                    if (now - lastDbUpdateMs > 150) {
                        lastDbUpdateMs = now
                        viewModelScope.launch {
                            try {
                                chatRepository.updateMessageContent(
                                    messageId = assistantMsgId,
                                    content = currentText,
                                    isStreaming = true
                                )
                            } catch (e: Exception) {
                                android.util.Log.w("JarvisViewModel", "Error streaming chunk update", e)
                            }
                        }
                    }
                }
            )

            _isGenerating.value = false

            if (result.isSuccess) {
                val finalText = result.getOrNull()?.ifBlank { null } ?: fullResponseBuffer.toString().ifBlank { "Transmission received." }
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
                _statusText.value = "ONLINE // READY"
            }
        } catch (e: Exception) {
            android.util.Log.e("JarvisViewModel", "Unhandled error in handleGenerativeAIResponse", e)
            _isGenerating.value = false
            _statusText.value = "ONLINE // READY"
            chatRepository.updateMessageContent(
                messageId = assistantMsgId,
                content = "System recovered: ${e.message}",
                isStreaming = false,
                isError = true
            )
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
