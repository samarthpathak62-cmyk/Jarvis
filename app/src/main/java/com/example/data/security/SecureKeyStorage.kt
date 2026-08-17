package com.example.data.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import com.example.data.model.AIProvider
import com.example.data.model.UserProfile
import com.example.data.model.VoiceSettings
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecureKeyStorage(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        initKeyStore()
    }

    private fun initKeyStore() {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance("AES", ANDROID_KEYSTORE)
                val keyGenParameterSpec = android.security.keystore.KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or android.security.keystore.KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
                keyGenerator.init(keyGenParameterSpec)
                keyGenerator.generateKey()
            }
        } catch (e: Exception) {
            // Hardware keystore might not be accessible on certain ROMs or emulators
        }
    }

    private fun getSecretKey(): SecretKey? {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (keyStore.containsAlias(KEY_ALIAS)) {
                val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
                entry?.secretKey
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun encrypt(plainText: String): String {
        if (plainText.isEmpty()) return ""
        val secretKey = getSecretKey()
        if (secretKey == null) {
            return Base64.encodeToString(plainText.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)
        }
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))
            val combined = ByteArray(iv.size + encryptedBytes.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)
            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            // Fallback obfuscation if hardware keystore has transient issue
            Base64.encodeToString(plainText.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)
        }
    }

    private fun decrypt(encryptedBase64: String): String {
        if (encryptedBase64.isEmpty()) return ""
        val secretKey = getSecretKey()
        if (secretKey == null) {
            return try {
                String(Base64.decode(encryptedBase64, Base64.NO_WRAP), StandardCharsets.UTF_8)
            } catch (e: Exception) {
                ""
            }
        }
        return try {
            val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)
            if (combined.size <= GCM_IV_LENGTH) {
                return String(Base64.decode(encryptedBase64, Base64.NO_WRAP), StandardCharsets.UTF_8)
            }
            val iv = ByteArray(GCM_IV_LENGTH)
            val encryptedBytes = ByteArray(combined.size - GCM_IV_LENGTH)
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH)
            System.arraycopy(combined, GCM_IV_LENGTH, encryptedBytes, 0, encryptedBytes.size)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            val plainBytes = cipher.doFinal(encryptedBytes)
            String(plainBytes, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            // Fallback decode attempt
            try {
                String(Base64.decode(encryptedBase64, Base64.NO_WRAP), StandardCharsets.UTF_8)
            } catch (ex: Exception) {
                ""
            }
        }
    }

    // --- API Key Management ---

    fun saveApiKey(provider: AIProvider, key: String) {
        val encrypted = encrypt(key.trim())
        prefs.edit().putString(getPrefKeyForProvider(provider), encrypted).apply()
    }

    fun getApiKey(provider: AIProvider): String {
        val encrypted = prefs.getString(getPrefKeyForProvider(provider), "") ?: ""
        return decrypt(encrypted)
    }

    fun removeApiKey(provider: AIProvider) {
        prefs.edit().remove(getPrefKeyForProvider(provider)).apply()
    }

    fun hasApiKey(provider: AIProvider): Boolean {
        return getApiKey(provider).isNotBlank()
    }

    fun getMaskedApiKey(provider: AIProvider): String {
        val key = getApiKey(provider)
        if (key.isBlank()) return "Not Configured"
        if (key.length <= 8) return "••••••••"
        return "${key.take(4)}••••••••${key.takeLast(4)}"
    }

    private fun getPrefKeyForProvider(provider: AIProvider): String {
        return when (provider) {
            AIProvider.GEMINI -> KEY_GEMINI_API_KEY
            AIProvider.OPENROUTER -> KEY_OPENROUTER_API_KEY
        }
    }

    // --- AI Configuration ---

    var activeProvider: AIProvider
        get() {
            val name = prefs.getString(KEY_ACTIVE_PROVIDER, AIProvider.GEMINI.name) ?: AIProvider.GEMINI.name
            return try {
                AIProvider.valueOf(name)
            } catch (e: Exception) {
                AIProvider.GEMINI
            }
        }
        set(value) {
            prefs.edit().putString(KEY_ACTIVE_PROVIDER, value.name).apply()
        }

    fun getSelectedModel(provider: AIProvider): String {
        val defaultModel = provider.defaultModel
        val key = if (provider == AIProvider.GEMINI) KEY_GEMINI_MODEL else KEY_OPENROUTER_MODEL
        return prefs.getString(key, defaultModel) ?: defaultModel
    }

    fun setSelectedModel(provider: AIProvider, model: String) {
        val key = if (provider == AIProvider.GEMINI) KEY_GEMINI_MODEL else KEY_OPENROUTER_MODEL
        prefs.edit().putString(key, model).apply()
    }

    var temperature: Float
        get() = prefs.getFloat(KEY_TEMPERATURE, 0.7f)
        set(value) {
            prefs.edit().putFloat(KEY_TEMPERATURE, value.coerceIn(0.0f, 1.0f)).apply()
        }

    // --- Voice & TTS Settings ---

    fun getVoiceSettings(): VoiceSettings {
        return VoiceSettings(
            autoSpeak = prefs.getBoolean(KEY_AUTO_SPEAK, false),
            speechRate = prefs.getFloat(KEY_SPEECH_RATE, 0.95f),
            pitch = prefs.getFloat(KEY_PITCH, 0.82f),
            voiceType = prefs.getString(KEY_VOICE_TYPE, "jarvis_british_male") ?: "jarvis_british_male"
        )
    }

    fun saveVoiceSettings(settings: VoiceSettings) {
        prefs.edit()
            .putBoolean(KEY_AUTO_SPEAK, settings.autoSpeak)
            .putFloat(KEY_SPEECH_RATE, settings.speechRate)
            .putFloat(KEY_PITCH, settings.pitch)
            .putString(KEY_VOICE_TYPE, settings.voiceType)
            .apply()
    }

    // --- User Session ---

    fun saveUserSession(user: UserProfile) {
        prefs.edit()
            .putString(KEY_USER_ID, user.id)
            .putString(KEY_USER_NAME, user.displayName)
            .putString(KEY_USER_EMAIL, user.email)
            .putString(KEY_USER_PHOTO, user.photoUrl ?: "")
            .putBoolean(KEY_USER_AUTH, user.isAuthenticated)
            .apply()
    }

    fun getUserSession(): UserProfile {
        val isAuth = prefs.getBoolean(KEY_USER_AUTH, false)
        val id = prefs.getString(KEY_USER_ID, "") ?: ""
        val name = prefs.getString(KEY_USER_NAME, "User") ?: "User"
        val email = prefs.getString(KEY_USER_EMAIL, "") ?: ""
        val photo = prefs.getString(KEY_USER_PHOTO, "")
        return UserProfile(
            id = id,
            displayName = name,
            email = email,
            photoUrl = if (photo.isNullOrBlank()) null else photo,
            isAuthenticated = isAuth
        )
    }

    fun clearUserSession() {
        prefs.edit()
            .remove(KEY_USER_ID)
            .remove(KEY_USER_NAME)
            .remove(KEY_USER_EMAIL)
            .remove(KEY_USER_PHOTO)
            .putBoolean(KEY_USER_AUTH, false)
            .apply()
    }

    fun clearAllData() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "jarvis_secure_preferences"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "JarvisMasterKey_v1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128

        private const val KEY_GEMINI_API_KEY = "sec_gemini_key"
        private const val KEY_OPENROUTER_API_KEY = "sec_openrouter_key"
        private const val KEY_ACTIVE_PROVIDER = "cfg_active_provider"
        private const val KEY_GEMINI_MODEL = "cfg_gemini_model"
        private const val KEY_OPENROUTER_MODEL = "cfg_openrouter_model"
        private const val KEY_TEMPERATURE = "cfg_temperature"

        private const val KEY_AUTO_SPEAK = "voice_auto_speak"
        private const val KEY_SPEECH_RATE = "voice_speech_rate"
        private const val KEY_PITCH = "voice_pitch"
        private const val KEY_VOICE_TYPE = "voice_type"

        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_PHOTO = "user_photo"
        private const val KEY_USER_AUTH = "user_authenticated"
    }
}
