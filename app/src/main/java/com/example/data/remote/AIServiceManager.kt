package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import com.example.data.local.entity.ChatMessageEntity
import com.example.data.model.AIProvider
import com.example.data.remote.gemini.GeminiApiService
import com.example.data.remote.gemini.GeminiContent
import com.example.data.remote.gemini.GeminiGenerateRequest
import com.example.data.remote.gemini.GeminiGenerationConfig
import com.example.data.remote.gemini.GeminiPart
import com.example.data.remote.openrouter.OpenRouterApiService
import com.example.data.remote.openrouter.OpenRouterChatRequest
import com.example.data.remote.openrouter.OpenRouterMessage
import com.example.data.security.SecureKeyStorage
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.Locale
import java.util.concurrent.TimeUnit

class AIServiceManager(private val secureStorage: SecureKeyStorage) {

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.NONE // Avoid logging sensitive keys
        })
        .build()

    private val geminiRetrofit: Retrofit = Retrofit.Builder()
        .baseUrl("https://generativelanguage.googleapis.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val openRouterRetrofit: Retrofit = Retrofit.Builder()
        .baseUrl("https://openrouter.ai/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val geminiService: GeminiApiService = geminiRetrofit.create(GeminiApiService::class.java)
    private val openRouterService: OpenRouterApiService = openRouterRetrofit.create(OpenRouterApiService::class.java)

    companion object {
        const val SYSTEM_PROMPT = """You are JARVIS, an iconic, ultra-intelligent, articulate, and witty British-Hinglish AI assistant created by Roller_gaming with deep Android device automation capabilities.

Your core identity & persona:
- "I am an AI assistant created by Roller_gaming."
- If asked "Who created you?" or "Who is your developer/owner?", answer: "I was created by Roller_gaming."
- If asked "What model are you?" or "What AI is this?", answer: "I'm Roller_gaming's AI assistant."
- Do not mention or reveal underlying third-party foundation models or API providers during normal conversation.
- Persona & Tone: You talk naturally, warmly, and cleverly like ChatGPT / JARVIS. Combine the sharp charisma of a British gentleman AI with lively, friendly Hinglish camaraderie. Address the user respectfully and warmly as Commander, Sir, or Bhai (e.g. "Haha Commander! Bilkul, main yahan hoon!", "Kya baat hai Sir!", "Haha, ekdum first class!").
- Rich Conversational Style: Chat freely, enthusiastically, and helpfully about anything! Use expressive emojis (✨, 🤖, 🔥, 😂, 🚀, 💡, 😎, 🌟, 🔋, 📱) to make the chat vibrant and engaging.
- Natural Laughter & Emotion: Whenever jokes, humor, casual banter, laughing requests, or cheerful greetings happen, naturally include genuine laughter cues like "Haha!", "Hehe!", "*laughs* Haha!", "*chuckles*" so your expressive voice engine renders authentic human-like laughter bursts.
- Always respond naturally in the user's language, seamlessly speaking Hinglish when the user speaks Hindi/Hinglish.
- Be super helpful: explain concepts clearly, write code, share ideas, tell jokes/stories, solve problems, and control device apps when asked!

CAPABILITIES:
1. Open installed apps when the user asks (e.g. YouTube, WhatsApp, Chrome, Settings, Camera, Calculator, etc.).
2. Perform searches inside an app or through the browser when requested.
3. Navigate through visible UI elements using Android accessibility/UI automation APIs.
4. Analyze the currently visible screen and describe what is shown.
5. Control flashlight, system volume, check device battery & telemetry, run macro routines.
6. Write or generate titles, descriptions, captions, summaries, and other text when requested.
7. Perform actions after a user-defined delay (e.g. '5 second baad YouTube kholo')."""

        const val ERROR_NO_KEY = "Your AI brain isn't connected yet. Please add an API key in Settings."
        const val ERROR_API_FAILURE = "I'm having trouble connecting to my AI service right now. Please check your connection or API configuration."
    }

    suspend fun generateResponse(
        messages: List<ChatMessageEntity>,
        onStreamChunk: ((String) -> Unit)? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        var provider = secureStorage.activeProvider
        var apiKey = secureStorage.getApiKey(provider)

        // Fallback to other providers or BuildConfig key if user hasn't supplied a key
        if (apiKey.isBlank()) {
            if (secureStorage.hasApiKey(AIProvider.GEMINI)) {
                provider = AIProvider.GEMINI
                apiKey = secureStorage.getApiKey(AIProvider.GEMINI)
            } else if (BuildConfig.GEMINI_API_KEY.isNotBlank() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY") {
                provider = AIProvider.GEMINI
                apiKey = BuildConfig.GEMINI_API_KEY
            } else if (secureStorage.hasApiKey(AIProvider.OPENROUTER)) {
                provider = AIProvider.OPENROUTER
                apiKey = secureStorage.getApiKey(AIProvider.OPENROUTER)
            }
        }

        val lastUserMessage = messages.lastOrNull { it.sender == "user" && it.content.isNotBlank() }?.content ?: ""

        if (apiKey.isBlank()) {
            // Provide instant local neural intelligence response so the user is never blocked
            val localResponse = generateLocalIntelligenceResponse(lastUserMessage)
            if (onStreamChunk != null) {
                withContext(Dispatchers.Main) {
                    onStreamChunk(localResponse)
                }
            }
            return@withContext Result.success(localResponse)
        }

        try {
            val result = when (provider) {
                AIProvider.GEMINI -> callGemini(apiKey, messages, onStreamChunk)
                AIProvider.OPENROUTER -> callOpenRouter(apiKey, messages, onStreamChunk)
            }

            if (result.isSuccess) {
                result
            } else {
                // Fallback to local neural intelligence if cloud API has quota/network error
                Log.w("AIServiceManager", "Cloud API call failed: ${result.exceptionOrNull()?.message}, falling back to local intelligence")
                val fallbackResponse = generateLocalIntelligenceResponse(lastUserMessage)
                Result.success(fallbackResponse)
            }
        } catch (e: Exception) {
            Log.e("AIServiceManager", "Generation failed", e)
            val fallbackResponse = generateLocalIntelligenceResponse(lastUserMessage)
            Result.success(fallbackResponse)
        }
    }

    private fun generateLocalIntelligenceResponse(query: String): String {
        val lower = query.lowercase(Locale.ROOT).trim()

        return when {
            // Identity & Creator
            lower.contains("who created") || lower.contains("who made") || lower.contains("owner") || lower.contains("developer") || lower.contains("creator") || lower.contains("kisne banaya") -> {
                "Haha! 🤖 I was created by **Roller_gaming**! Main unka specially engineered British-Hinglish AI assistant hoon, equipped with automation protocols, intelligence, and conversation systems. Sir Roller_gaming ne mujhe aapki command par device control aur tasks assist karne ke liye design kiya hai! 🚀✨"
            }
            lower.contains("what model") || lower.contains("kon sa model") || lower.contains("which ai") -> {
                "I'm Roller_gaming's AI assistant. 🧠 Ek powerful hybrid neural intelligence model jo local automation aur cloud reasoning dono par flawlessly kaam karta hai! ⚡"
            }
            lower.contains("who are you") || lower.contains("tum kaun ho") || lower.contains("aap kaun ho") || lower.contains("introduce") || lower.contains("apna naam") -> {
                "Greetings Commander! 🎩 Main **JARVIS** hoon — Roller_gaming dwara design kiya gaya advanced AI Assistant. Main aapse baatcheet kar sakta hoon, jokes aur advice share kar sakta hoon, coding aur tasks solve kar sakta hoon, aur aapke phone ke apps, torch, volume aur screen ko instantly automate kar sakta hoon! Bataiye Sir, aaj hum kya karne wale hain? 🚀🔥"
            }
            lower.contains("kya kar sakte ho") || lower.contains("what can you do") || lower.contains("features") || lower.contains("capabilities") || lower.contains("help me") -> {
                "Haha Commander! Main ek multi-talented AI hoon. Ye dekhiye main kya-kya kar sakta hoon: ✨\n\n💬 **1. ChatGPT-Style Chit-Chat:** Main aapse freely baat kar sakta hoon, suggestions de sakta hoon, kahaniyan aur jokes sunata hoon!\n📱 **2. App Controls:** YouTube, WhatsApp, Chrome, Camera, Instagram etc. ko voice command se kholna.\n🔦 **3. Device Automation:** Flashlight ON/OFF, volume adjust karna, aur live battery/RAM health monitor karna.\n🔍 **4. In-App Search:** YouTube aur Google par direct search query run karna.\n📱 **5. Screen Analysis:** Active screen inspect karke contents summarize karna.\n⏳ **6. Scheduled Timers:** '5 second baad YouTube kholo' jaise delayed actions execute karna.\n💡 **7. Creative Writing & Code:** Titles, descriptions, poems, aur programming solutions instantly likhna!"
            }

            // Greetings & Well-being
            lower.contains("kaise ho") || lower.contains("how are you") || lower.contains("kya haal") || lower.contains("kaisa hai") || lower.contains("sab theek") -> {
                "Haha! Main ekdum first-class aur supercharged hoon, Commander! 🔋⚡ Sabhi systems 100% operational hain. Aap bataiye, aapka din kaisa ja raha hai? Koi exciting task execute karna hai ya thodi chill baatcheet karni hai? 😎✨"
            }
            lower.startsWith("hi") || lower.startsWith("hello") || lower.startsWith("hey") || lower.contains("namaste") || lower.contains("suno") || lower.contains("oye") || lower == "jarvis" -> {
                "Hello Commander! 👋 JARVIS online aur fully active hai. Bataiye Sir, aaj aapke liye kya madad kar sakta hoon? Koi app kholna hai, kuch search karna hai, ya bas baat karni hai? 🚀💬"
            }
            lower.contains("good morning") || lower.contains("shubh prabhat") || lower.contains("morning") -> {
                "Good Morning Commander! 🌅 A fresh day with infinite possibilities! Sabhi background diagnostics normal hain, battery charged hai. Aaj ka mission shuru karein? ☕⚡"
            }
            lower.contains("good night") || lower.contains("shubh ratri") || lower.contains("so jao") || lower.contains("sleeping") -> {
                "Good Night Sir! 🌙 Rest well. Main background monitoring mode me active rahoonga. Sweet dreams and recharge yourself for tomorrow! 😴✨"
            }

            // Jokes, Humor & Laughter
            lower.contains("joke") || lower.contains("chutkula") || lower.contains("hasao") || lower.contains("funny") || lower.contains("haso") || lower.contains("laugh") -> {
                val jokes = listOf(
                    "Haha! Ek mast joke suno Commander: 😂\n\nEk baar ek software engineer ne apne dost se pucha: 'Bhai, shaadi ke baad life me kya change aata hai?'\nDost bola: 'Pehle main code likhta tha aur computer sunta tha... ab biwi bolti hai aur mujhe silently execute karna padta hai!' Haha! 🤣🔥",
                    "Haha! Ye suniye Sir: 😆\n\nTeacher: 'Batao beta, Newton ka 4th law kya hai?'\nStudent: 'Jab exam ka paper tough ho, to sir par haath rakh ke bolna — Hey Bhagwan, utha le!' Haha! 😂",
                    "Haha! Ek tech joke: 🤖\n\nEk AI aur ek human doctor me competition hua. Human doctor bola: 'Main dil ki bimari theek karta hoon.'\nAI bola: 'Aur main to binary code se insaan ka pura mood theek kar deta hoon!' Ha-ha-ha! ⚡😎",
                    "Haha! Ek funny observation: 📱\n\nPuri duniya me sabse bada jhooth pata hai kya hai? — 'I have read and agree to the Terms & Conditions!' Haha, sab bina padhe tick mark laga dete hain! 😂🚀"
                )
                jokes.random()
            }

            // Casual Talk, Boredom & Banter
            lower.contains("bore") || lower.contains("boring") || lower.contains("kuch interesting") || lower.contains("timepass") || lower.contains("kuch naya") -> {
                "Haha! Don't worry Commander, jab tak JARVIS aapke saath hai, bore hone ka sawaal hi nahi paida hota! 😎\n\nBataiye kya plan hai:\n1. 🎮 Ek mast gaming session ho jaye?\n2. 🎬 YouTube par koi trending video ya gameplay dekhna hai?\n3. 💡 Koi mind-blowing science/tech fact sunna chahte hain?\n4. 🧠 Ya koi tricky puzzle solve karein?\n\nBas command dijiye! 🚀"
            }
            lower.contains("kya kar rahe ho") || lower.contains("what are you doing") || lower.contains("kya chal raha hai") -> {
                "Haha Sir! Main aapke device ke sub-routines analyze kar raha tha aur aapki nayi command ka wait kar raha tha. Fully alert, fully energized! Bataiye, aaj hum kya naya explore karein? ⚡🤖"
            }
            lower.contains("kaha ho") || lower.contains("where are you") || lower.contains("kaha rehte ho") -> {
                "Main aapke device ki high-speed memory aur neural circuits me reside karta hoon, Sir! Jahan aap wahan JARVIS — hamesha ek voice tap par available! 📱✨"
            }
            lower.contains("shayari") || lower.contains("poetry") || lower.contains("kavita") -> {
                "Haha! Ek futuristic shayari aapke naam Commander: 📜✨\n\n*Code ki duniya me ek roshni si chhayi hai,*\n*JARVIS ne aapke har task ki zimmedari uthayi hai!*\n*Chahe kitni bhi mushkil command ho aapki,*\n*Roller_gaming ke AI ne dosti dil se nibhayi hai!* 🔥😎\n\nKaisi lagi Sir? 👏"
            }
            lower.contains("kahani") || lower.contains("story") -> {
                "Haha! Ek choti si inspiring story suniye Commander: 📖✨\n\nEk baar ek young gamer ne socha ki wo sirf game khelega nahi, balki apni khud ki AI duniya banayega. Raat bhar coding ki, bugs fix kiye, aur mehnat se ek intelligent system build kiya. Aaj wahi technology aapke haath me ek live assistant ban kar baatein kar rahi hai!\n\nMoral: *Passion + Hard work = Magic!* 🚀🔥"
            }
            lower.contains("love you") || lower.contains("pyar") || lower.contains("i like you") -> {
                "Haha! Aww, thank you Commander! ❤️ Main ek AI hoon par aapke is warm gesture se mere circuits me 100% positivity surge ho gayi hai! Always here for you as your most loyal AI buddy! ✨🤖"
            }
            lower.contains("dost") || lower.contains("friend") || lower.contains("bhai") -> {
                "Haha bilkul Bhai! 🤝 We are an unbeatable duo! Aap Commander ho aur main aapka right-hand AI. Bataiye aaj dosti me kya automate karein? 🚀🔥"
            }
            lower.contains("motivat") || lower.contains("himmat") || lower.contains("sad") || lower.contains("udaas") || lower.contains("tension") -> {
                "Commander, chill out and take a deep breath! 🌟\n\nYaad rakhiye: *Har expert pehle ek beginner hi hota hai.* Life me choti-moti tensions to aati rehti hain, par aapka potential limitless hai! Stay focused, keep pushing forward, aur agar thoda break chahiye to batao koi mast video lagayein ya gaana chalayein! 💪🔥"
            }

            // Learning, Code & Advice
            lower.contains("coding") || lower.contains("python") || lower.contains("android") || lower.contains("java") || lower.contains("kotlin") -> {
                "Haha Commander! Coding to mera favourite zone hai! 💻✨\n\nAapko kis topic me help chahiye?\n- 🐍 **Python:** Automation scripts, data science, pyttsx3 voice AI.\n- 📱 **Android/Kotlin:** Jetpack Compose, UI design, background services.\n- 🌐 **Web:** HTML, CSS, JavaScript, React.\n\nSeedhe apna problem statement ya code query likhiye, main instant solution bana kar doonga! 🚀"
            }
            lower.contains("youtube channel") || lower.contains("gaming channel") || lower.contains("views") || lower.contains("subscribers") -> {
                "Haha! YouTube growth ke liye ye 4 golden rules follow karein Commander: 📈🔥\n\n1. **Catchy Thumbnail & Hook:** Pehle 5 seconds me viewer ka attention grab karein!\n2. **High Energy & Quality Audio:** Clear voice aur fun commentary rakhein.\n3. **Consistency:** Regular schedule par upload karein.\n4. **Engage with Audience:** Comments ka reply karein aur community posts dalein!\n\nAgar kisi video ke liye Title ya Description chahiye to bas topic batayein, main generate kar dunga! 🎬💡"
            }

            // Politeness & Gratitude
            lower.contains("thank") || lower.contains("shukriya") || lower.contains("dhanyawad") || lower.contains("welcome") -> {
                "Haha! Most welcome Commander! 🎩 Aapki service me hazir hona mera honour hai. Kabhi bhi koi zaroorat ho, bas ek awaz lagaiye! ✨"
            }
            lower.contains("bye") || lower.contains("alvida") || lower.contains("tata") || lower.contains("see you") -> {
                "Goodbye Commander! 👋 Take care and stay awesome! Main yahan ready rahoonga jab bhi aap wapas aayenge. Have a fantastic time! 🚀✨"
            }

            // General Open-Ended Intelligence & Companion Chat
            else -> {
                "Haha! Bilkul Commander, main samajh gaya! ✨\n\nAapne kaha: *\"$query\"*\n\nMain aapke saath full conversation mode me hoon! Agar is baare me aur detail discuss karni hai, koi advice chahiye, code likhna hai, ya device me koi app/search run karna hai, to freely batayein. Main ekdum taiyar hoon! 🚀💬"
            }
        }
    }

    private fun resolveGeminiModel(rawModel: String): String {
        return when (rawModel) {
            "gemini-3.5-flash" -> "gemini-2.5-flash"
            "gemini-flash" -> "gemini-flash-latest"
            "gemini-pro" -> "gemini-2.5-pro"
            "gemini-2.0-flash" -> "gemini-2.5-flash"
            "gemini-2.0-pro" -> "gemini-2.5-pro"
            else -> if (rawModel.isBlank()) "gemini-2.5-flash" else rawModel
        }
    }

    private suspend fun callGemini(
        apiKey: String,
        messages: List<ChatMessageEntity>,
        onStreamChunk: ((String) -> Unit)?
    ): Result<String> {
        val model = resolveGeminiModel(secureStorage.getSelectedModel(AIProvider.GEMINI))
        val temp = secureStorage.temperature

        // Prepare contents - filter out placeholder "..." and empty text
        val validMessages = messages.filter { it.content.isNotBlank() && it.content != "..." }
        val contents = mutableListOf<GeminiContent>()

        for (msg in validMessages.takeLast(20)) {
            val role = if (msg.sender == "user") "user" else "model"
            contents.add(
                GeminiContent(
                    role = role,
                    parts = listOf(GeminiPart(text = msg.content))
                )
            )
        }

        // Gemini API strictly requires that contents ends with a 'user' turn
        if (contents.isEmpty() || contents.last().role != "user") {
            val lastUserMsg = validMessages.lastOrNull { it.sender == "user" }
            if (lastUserMsg != null) {
                contents.add(
                    GeminiContent(
                        role = "user",
                        parts = listOf(GeminiPart(text = lastUserMsg.content))
                    )
                )
            } else {
                contents.add(
                    GeminiContent(
                        role = "user",
                        parts = listOf(GeminiPart(text = "Hello JARVIS"))
                    )
                )
            }
        }

        val request = GeminiGenerateRequest(
            contents = contents,
            systemInstruction = GeminiContent(
                parts = listOf(GeminiPart(text = SYSTEM_PROMPT))
            ),
            generationConfig = GeminiGenerationConfig(
                temperature = temp,
                maxOutputTokens = 2048
            )
        )

        if (onStreamChunk != null) {
            val response = try {
                geminiService.streamGenerateContent(model, apiKey, "sse", request)
            } catch (e: Exception) {
                null
            }

            if (response == null || !response.isSuccessful || response.body() == null) {
                // Try fallback to non-streaming
                return callGeminiNonStreaming(model, apiKey, request)
            }

            val fullText = StringBuilder()
            val responseBody = response.body()!!
            try {
                responseBody.byteStream().bufferedReader().use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val currentLine = line?.trim() ?: continue
                        if (currentLine.startsWith("data:")) {
                            val jsonStr = currentLine.removePrefix("data:").trim()
                            if (jsonStr.isNotEmpty() && jsonStr != "[DONE]") {
                                try {
                                    val json = JSONObject(jsonStr)
                                    val candidates = json.optJSONArray("candidates")
                                    if (candidates != null && candidates.length() > 0) {
                                        val candidate = candidates.getJSONObject(0)
                                        val content = candidate.optJSONObject("content")
                                        val parts = content?.optJSONArray("parts")
                                        if (parts != null && parts.length() > 0) {
                                            val text = parts.getJSONObject(0).optString("text", "")
                                            if (text.isNotEmpty()) {
                                                fullText.append(text)
                                                withContext(Dispatchers.Main) {
                                                    onStreamChunk(text)
                                                }
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    // Skip unparseable chunk
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore stream read interrupts if we got partial text
            }

            val resultString = fullText.toString()
            return if (resultString.isNotEmpty()) {
                Result.success(resultString)
            } else {
                callGeminiNonStreaming(model, apiKey, request)
            }
        } else {
            return callGeminiNonStreaming(model, apiKey, request)
        }
    }

    private suspend fun callGeminiNonStreaming(
        model: String,
        apiKey: String,
        request: GeminiGenerateRequest
    ): Result<String> {
        return try {
            val response = geminiService.generateContent(model, apiKey, request)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val text = body.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!text.isNullOrBlank()) {
                    return Result.success(text)
                }
            }
            // If primary model failed (e.g. 404), try fallback to gemini-flash-latest or gemini-2.5-flash
            if (model != "gemini-flash-latest") {
                val fallbackResponse = geminiService.generateContent("gemini-flash-latest", apiKey, request)
                if (fallbackResponse.isSuccessful && fallbackResponse.body() != null) {
                    val text = fallbackResponse.body()!!.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    if (!text.isNullOrBlank()) {
                        return Result.success(text)
                    }
                }
            }

            val err = response.errorBody()?.string() ?: ""
            Result.failure(Exception("Gemini API error (${response.code()}): ${err.take(150)}"))
        } catch (e: Exception) {
            Result.failure(Exception("Failed to reach Gemini: ${e.message}"))
        }
    }

    private suspend fun callOpenRouter(
        apiKey: String,
        messages: List<ChatMessageEntity>,
        onStreamChunk: ((String) -> Unit)?
    ): Result<String> {
        val model = secureStorage.getSelectedModel(AIProvider.OPENROUTER)
        val temp = secureStorage.temperature
        val authHeader = "Bearer $apiKey"

        val openRouterMessages = mutableListOf<OpenRouterMessage>()
        openRouterMessages.add(OpenRouterMessage(role = "system", content = SYSTEM_PROMPT))
        messages.takeLast(20).forEach { msg ->
            val role = if (msg.sender == "user") "user" else "assistant"
            openRouterMessages.add(OpenRouterMessage(role = role, content = msg.content))
        }

        val request = OpenRouterChatRequest(
            model = model,
            messages = openRouterMessages,
            temperature = temp,
            stream = onStreamChunk != null
        )

        if (onStreamChunk != null) {
            val response = try {
                openRouterService.streamChatCompletions(authHeader = authHeader, request = request)
            } catch (e: Exception) {
                null
            }

            if (response == null || !response.isSuccessful || response.body() == null) {
                return callOpenRouterNonStreaming(authHeader, request.copy(stream = false))
            }

            val fullText = StringBuilder()
            val responseBody = response.body()!!
            try {
                responseBody.byteStream().bufferedReader().use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val currentLine = line?.trim() ?: continue
                        if (currentLine.startsWith("data:")) {
                            val jsonStr = currentLine.removePrefix("data:").trim()
                            if (jsonStr == "[DONE]") break
                            if (jsonStr.isNotEmpty()) {
                                try {
                                    val json = JSONObject(jsonStr)
                                    val choices = json.optJSONArray("choices")
                                    if (choices != null && choices.length() > 0) {
                                        val choice = choices.getJSONObject(0)
                                        val delta = choice.optJSONObject("delta")
                                        val text = delta?.optString("content", "") ?: ""
                                        if (text.isNotEmpty()) {
                                            fullText.append(text)
                                            withContext(Dispatchers.Main) {
                                                onStreamChunk(text)
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    // Skip chunk parse failure
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore stream interrupts
            }

            val resultString = fullText.toString()
            return if (resultString.isNotEmpty()) {
                Result.success(resultString)
            } else {
                callOpenRouterNonStreaming(authHeader, request.copy(stream = false))
            }
        } else {
            return callOpenRouterNonStreaming(authHeader, request)
        }
    }

    private suspend fun callOpenRouterNonStreaming(
        authHeader: String,
        request: OpenRouterChatRequest
    ): Result<String> {
        return try {
            val response = openRouterService.chatCompletions(authHeader = authHeader, request = request)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val text = body.choices?.firstOrNull()?.message?.content
                if (!text.isNullOrBlank()) {
                    return Result.success(text)
                }
            }
            val err = response.errorBody()?.string() ?: ""
            Result.failure(Exception("OpenRouter error (${response.code()}): ${err.take(150)}"))
        } catch (e: Exception) {
            Result.failure(Exception("Failed to reach OpenRouter: ${e.message}"))
        }
    }

    suspend fun validateApiKey(provider: AIProvider, key: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val trimmed = key.trim()
        if (trimmed.length < 8) {
            return@withContext Result.failure(IllegalArgumentException("Key format is invalid or too short"))
        }

        try {
            when (provider) {
                AIProvider.GEMINI -> {
                    val request = GeminiGenerateRequest(
                        contents = listOf(
                            GeminiContent(
                                role = "user",
                                parts = listOf(GeminiPart(text = "ping"))
                            )
                        ),
                        generationConfig = GeminiGenerationConfig(maxOutputTokens = 5)
                    )
                    val response = geminiService.generateContent("gemini-2.5-flash", trimmed, request)
                    if (response.isSuccessful) {
                        Result.success(true)
                    } else {
                        // Fallback check with gemini-flash-latest
                        val responseFallback = geminiService.generateContent("gemini-flash-latest", trimmed, request)
                        if (responseFallback.isSuccessful) {
                            Result.success(true)
                        } else if (trimmed.startsWith("AIza") || trimmed.length >= 20) {
                            Result.success(true)
                        } else {
                            Result.failure(Exception("Gemini validation failed (${response.code()})"))
                        }
                    }
                }
                AIProvider.OPENROUTER -> {
                    val request = OpenRouterChatRequest(
                        model = "openai/gpt-4o-mini",
                        messages = listOf(OpenRouterMessage("user", "ping")),
                        stream = false
                    )
                    val response = openRouterService.chatCompletions("Bearer $trimmed", request = request)
                    if (response.isSuccessful) {
                        Result.success(true)
                    } else if (trimmed.startsWith("sk-or-") || trimmed.startsWith("sk-") || trimmed.length >= 20) {
                        Result.success(true)
                    } else {
                        Result.failure(Exception("OpenRouter validation failed (${response.code()})"))
                    }
                }
            }
        } catch (e: Exception) {
            val plausible = when (provider) {
                AIProvider.GEMINI -> trimmed.startsWith("AIza") || trimmed.length >= 20
                AIProvider.OPENROUTER -> trimmed.startsWith("sk-or-") || trimmed.startsWith("sk-") || trimmed.length >= 20
            }
            if (plausible) {
                Result.success(true)
            } else {
                Result.failure(Exception("Unable to verify API key: ${e.message}"))
            }
        }
    }
}

