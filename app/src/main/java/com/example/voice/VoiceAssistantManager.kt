package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import com.example.data.model.VoiceSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class VoiceAssistantManager(
    private val context: Context,
    private val scope: CoroutineScope
) : RecognitionListener, TextToSpeech.OnInitListener {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _audioRms = MutableStateFlow(0f)
    val audioRms: StateFlow<Float> = _audioRms.asStateFlow()

    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText.asStateFlow()

    private var onSpeechResultCallback: ((String) -> Unit)? = null
    private var currentVoiceSettings = VoiceSettings()

    init {
        mainHandler.post {
            initSpeechRecognizer()
            initTextToSpeech()
        }
    }

    private fun initSpeechRecognizer() {
        try {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                speechRecognizer?.destroy()
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(this@VoiceAssistantManager)
                }
            }
        } catch (e: Exception) {
            Log.w("VoiceAssistantManager", "SpeechRecognizer not initialized: ${e.message}")
            speechRecognizer = null
        }
    }

    private fun initTextToSpeech() {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.w("VoiceAssistantManager", "TTS initialization failed: ${e.message}")
            tts = null
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsInitialized = true
            try {
                tts?.language = Locale.US
                applyVoiceSettings(currentVoiceSettings)
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        scope.launch(Dispatchers.Main) {
                            _isSpeaking.value = true
                        }
                    }

                    override fun onDone(utteranceId: String?) {
                        scope.launch(Dispatchers.Main) {
                            _isSpeaking.value = false
                            _audioRms.value = 0f
                        }
                    }

                    override fun onError(utteranceId: String?) {
                        scope.launch(Dispatchers.Main) {
                            _isSpeaking.value = false
                            _audioRms.value = 0f
                        }
                    }
                })
            } catch (e: Exception) {
                Log.w("VoiceAssistantManager", "Error setting up TTS listeners: ${e.message}")
            }
        } else {
            Log.w("VoiceAssistantManager", "TTS init failed with status: $status")
            isTtsInitialized = false
        }
    }

    fun applyVoiceSettings(settings: VoiceSettings) {
        currentVoiceSettings = settings
        if (!isTtsInitialized || tts == null) return

        try {
            tts?.setPitch(settings.pitch)
            tts?.setSpeechRate(settings.speechRate)

            val availableVoices = tts?.voices
            if (!availableVoices.isNullOrEmpty()) {
                val targetVoice = findBestMaleVoice(settings.voiceType, availableVoices)
                if (targetVoice != null) {
                    tts?.voice = targetVoice
                } else {
                    when (settings.voiceType) {
                        "jarvis_british_male", "calm_british" -> tts?.language = Locale.UK
                        "hinglish_indian_male" -> tts?.language = Locale("en", "IN")
                        else -> tts?.language = Locale.US
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("VoiceAssistantManager", "Could not configure custom voice locale: ${e.message}")
        }
    }

    private fun findBestMaleVoice(voiceType: String, voices: Set<Voice>): Voice? {
        val maleVoiceKeywords = listOf("male", "rjs", "gbd", "sfg", "tpf", "cxx", "end", "iol", "fis", "david", "george", "guy", "mark")
        val femaleVoiceKeywords = listOf("female", "woman", "girl", "zira", "eva", "jenny", "sfg#female", "tpf#female", "cxx#female", "aria")

        val isMaleCandidate: (Voice) -> Boolean = { v ->
            val lower = v.name.lowercase(Locale.ROOT)
            val hasMaleKeyword = maleVoiceKeywords.any { lower.contains(it) }
            val hasFemaleKeyword = femaleVoiceKeywords.any { lower.contains(it) }
            (hasMaleKeyword || !hasFemaleKeyword)
        }

        return when (voiceType) {
            "jarvis_british_male", "calm_british" -> {
                // Priority: British English Male Local -> British Male Network -> Any British
                voices.filter { it.locale.country.equals("GB", ignoreCase = true) || it.locale.language.equals("en", ignoreCase = true) && it.locale.country.equals("GB", ignoreCase = true) }
                    .sortedWith(compareByDescending<Voice> { !it.isNetworkConnectionRequired }
                        .thenByDescending { isMaleCandidate(it) }
                        .thenByDescending { it.name.contains("rjs", ignoreCase = true) || it.name.contains("gbd", ignoreCase = true) || it.name.contains("male", ignoreCase = true) })
                    .firstOrNull()
            }
            "python_david_male", "calm_natural" -> {
                // Priority: US English Male Local (pyttsx3 SAPI5 David style)
                voices.filter { it.locale.country.equals("US", ignoreCase = true) || it.locale == Locale.US }
                    .sortedWith(compareByDescending<Voice> { !it.isNetworkConnectionRequired }
                        .thenByDescending { isMaleCandidate(it) }
                        .thenByDescending { it.name.contains("sfg", ignoreCase = true) || it.name.contains("tpf", ignoreCase = true) || it.name.contains("male", ignoreCase = true) })
                    .firstOrNull()
            }
            "deep_baritone_male", "deep_resonant" -> {
                // Priority: Deepest resonant male voice across US / GB
                voices.filter { it.locale.language.equals("en", ignoreCase = true) }
                    .sortedWith(compareByDescending<Voice> { !it.isNetworkConnectionRequired }
                        .thenByDescending { it.name.contains("male", ignoreCase = true) }
                        .thenByDescending { isMaleCandidate(it) })
                    .firstOrNull()
            }
            "hinglish_indian_male" -> {
                // Priority: Indian English / Hinglish Male Local (Prabhat/en-IN style)
                voices.filter { it.locale.country.equals("IN", ignoreCase = true) || it.locale.language.equals("hi", ignoreCase = true) }
                    .sortedWith(compareByDescending<Voice> { !it.isNetworkConnectionRequired }
                        .thenByDescending { isMaleCandidate(it) }
                        .thenByDescending { it.name.contains("cxx", ignoreCase = true) || it.name.contains("end", ignoreCase = true) || it.name.contains("male", ignoreCase = true) })
                    .firstOrNull() ?: voices.find { it.locale.country.equals("IN", ignoreCase = true) }
            }
            "cyber_robotic_male" -> {
                // Priority: Any clean local voice, will apply sub-bass pitch
                voices.filter { it.locale.language.equals("en", ignoreCase = true) && !it.isNetworkConnectionRequired }
                    .sortedByDescending { isMaleCandidate(it) }
                    .firstOrNull()
            }
            else -> {
                // General Male Fallback
                voices.filter { it.locale.language.equals("en", ignoreCase = true) }
                    .sortedWith(compareByDescending<Voice> { !it.isNetworkConnectionRequired }
                        .thenByDescending { isMaleCandidate(it) })
                    .firstOrNull()
            }
        }
    }

    fun testVoice(sampleText: String = "JARVIS neural audio online. Commander, all local systems calibrated.") {
        speak(sampleText)
    }

    fun startListening(onResult: (String) -> Unit) {
        stopSpeaking()
        onSpeechResultCallback = onResult
        _recognizedText.value = ""

        mainHandler.post {
            try {
                if (speechRecognizer == null) {
                    initSpeechRecognizer()
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                }

                speechRecognizer?.startListening(intent)
                _isListening.value = true
            } catch (e: Exception) {
                Log.e("VoiceAssistantManager", "Error starting speech recognition", e)
                _isListening.value = false
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
            } catch (e: Exception) {
                Log.w("VoiceAssistantManager", "Error stopping speech recognition: ${e.message}")
            }
            _isListening.value = false
            _audioRms.value = 0f
        }
    }

    fun speak(text: String) {
        if (!isTtsInitialized || tts == null || text.isBlank()) return
        stopSpeaking()

        try {
            // Clean markdown formatting for smoother vocalization
            val cleanText = text
                .replace(Regex("[#*_`\\[\\]()]"), "")
                .replace(Regex("```[\\s\\S]*?```"), "code block omitted.")
                .trim()

            val utteranceId = "jarvis_speech_${System.currentTimeMillis()}"
            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
            }

            tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        } catch (e: Exception) {
            Log.e("VoiceAssistantManager", "Error speaking text", e)
        }
    }

    fun stopSpeaking() {
        try {
            if (tts != null && isTtsInitialized) {
                tts?.stop()
            }
        } catch (e: Exception) {
            Log.w("VoiceAssistantManager", "Error stopping TTS: ${e.message}")
        }
        _isSpeaking.value = false
        _audioRms.value = 0f
    }

    // --- SpeechRecognizer Callbacks ---

    override fun onReadyForSpeech(params: Bundle?) {
        scope.launch(Dispatchers.Main) {
            _isListening.value = true
        }
    }

    override fun onBeginningOfSpeech() {}

    override fun onRmsChanged(rmsdB: Float) {
        // Normalize RMS dB (typical range -2 to 10) to 0.0 .. 1.0
        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
        _audioRms.value = normalized
    }

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        scope.launch(Dispatchers.Main) {
            _isListening.value = false
        }
    }

    override fun onError(error: Int) {
        scope.launch(Dispatchers.Main) {
            _isListening.value = false
            _audioRms.value = 0f
        }
        Log.w("VoiceAssistantManager", "Speech recognition error code: $error")
    }

    override fun onResults(results: Bundle?) {
        scope.launch(Dispatchers.Main) {
            _isListening.value = false
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull() ?: ""
            if (text.isNotBlank()) {
                _recognizedText.value = text
                onSpeechResultCallback?.invoke(text)
            }
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        scope.launch(Dispatchers.Main) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull() ?: ""
            if (text.isNotBlank()) {
                _recognizedText.value = text
            }
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}

    fun release() {
        mainHandler.post {
            try {
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (e: Exception) {
                Log.w("VoiceAssistantManager", "Error destroying speech recognizer: ${e.message}")
            }
            try {
                tts?.stop()
                tts?.shutdown()
                tts = null
                isTtsInitialized = false
            } catch (e: Exception) {
                Log.w("VoiceAssistantManager", "Error shutting down TTS: ${e.message}")
            }
        }
    }
}
