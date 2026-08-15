package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
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
        initSpeechRecognizer()
        initTextToSpeech()
    }

    private fun initSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(this@VoiceAssistantManager)
            }
        }
    }

    private fun initTextToSpeech() {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsInitialized = true
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
        } else {
            Log.e("VoiceAssistantManager", "TTS init failed with status: $status")
        }
    }

    fun applyVoiceSettings(settings: VoiceSettings) {
        currentVoiceSettings = settings
        if (!isTtsInitialized || tts == null) return

        tts?.setPitch(settings.pitch)
        tts?.setSpeechRate(settings.speechRate)

        try {
            val availableVoices = tts?.voices
            if (!availableVoices.isNullOrEmpty()) {
                val targetVoice = when (settings.voiceType) {
                    "calm_british" -> {
                        availableVoices.find { it.locale.country == "GB" && !it.isNetworkConnectionRequired }
                            ?: availableVoices.find { it.locale.country == "GB" }
                    }
                    "deep_resonant" -> {
                        availableVoices.find { it.name.contains("male", ignoreCase = true) || it.name.contains("en-us-x-sfg") }
                    }
                    "smooth_neutral" -> {
                        availableVoices.find { it.locale.language == "en" && it.quality >= Voice.QUALITY_NORMAL }
                    }
                    else -> { // "calm_natural"
                        availableVoices.find { it.locale == Locale.US && !it.isNetworkConnectionRequired }
                    }
                }

                if (targetVoice != null) {
                    tts?.voice = targetVoice
                } else {
                    tts?.language = Locale.US
                }
            }
        } catch (e: Exception) {
            Log.w("VoiceAssistantManager", "Could not configure custom voice locale: ${e.message}")
        }
    }

    fun startListening(onResult: (String) -> Unit) {
        stopSpeaking()
        onSpeechResultCallback = onResult
        _recognizedText.value = ""

        if (speechRecognizer == null) {
            initSpeechRecognizer()
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        try {
            speechRecognizer?.startListening(intent)
            _isListening.value = true
        } catch (e: Exception) {
            Log.e("VoiceAssistantManager", "Error starting speech recognition", e)
            _isListening.value = false
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            Log.e("VoiceAssistantManager", "Error stopping speech recognition", e)
        }
        _isListening.value = false
        _audioRms.value = 0f
    }

    fun speak(text: String) {
        if (!isTtsInitialized || tts == null || text.isBlank()) return
        stopSpeaking()

        // Clean markdown formatting for smoother vocalization
        val cleanText = text
            .replace(Regex("[#*_`\\[\\]()]"), "")
            .replace(Regex("```[\\s\\S]*?```"), "code block omitted.")
            .trim()

        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "jarvis_speech_${System.currentTimeMillis()}")
        }

        tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, params, "jarvis_speech_${System.currentTimeMillis()}")
    }

    fun stopSpeaking() {
        if (tts != null && isTtsInitialized) {
            tts?.stop()
        }
        _isSpeaking.value = false
        _audioRms.value = 0f
    }

    // --- SpeechRecognizer Callbacks ---

    override fun onReadyForSpeech(params: Bundle?) {
        _isListening.value = true
    }

    override fun onBeginningOfSpeech() {}

    override fun onRmsChanged(rmsdB: Float) {
        // Normalize RMS dB (typical range -2 to 10) to 0.0 .. 1.0
        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
        _audioRms.value = normalized
    }

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        _isListening.value = false
    }

    override fun onError(error: Int) {
        _isListening.value = false
        _audioRms.value = 0f
        Log.w("VoiceAssistantManager", "Speech recognition error code: $error")
    }

    override fun onResults(results: Bundle?) {
        _isListening.value = false
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val text = matches?.firstOrNull() ?: ""
        if (text.isNotBlank()) {
            _recognizedText.value = text
            onSpeechResultCallback?.invoke(text)
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val text = matches?.firstOrNull() ?: ""
        if (text.isNotBlank()) {
            _recognizedText.value = text
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}

    fun release() {
        try {
            speechRecognizer?.destroy()
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            Log.e("VoiceAssistantManager", "Error releasing voice resources", e)
        }
    }
}
