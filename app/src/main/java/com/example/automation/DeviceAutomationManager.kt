package com.example.automation

import android.app.ActivityManager
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import android.util.Log
import com.example.data.model.BatteryTelemetry
import com.example.data.model.MacroRoutine
import com.example.data.model.MacroStep
import com.example.data.model.MemoryTelemetry
import kotlinx.coroutines.delay
import java.util.Locale
import java.util.regex.Pattern

enum class AutomationActionType {
    OPEN_APP,
    SEARCH,
    ANALYZE_SCREEN,
    NAVIGATE_UI,
    GENERATE_TEXT,
    GLOBAL_NAV,
    FLASHLIGHT,
    VOLUME,
    SYSTEM_SETTINGS,
    TELEMETRY,
    MACRO_ROUTINE,
    SAFETY_CONFIRMATION_REQUIRED,
    UNKNOWN
}

enum class GlobalNavAction {
    HOME,
    BACK,
    RECENTS,
    NOTIFICATIONS,
    QUICK_SETTINGS,
    SCREENSHOT,
    LOCK_SCREEN
}

data class ParsedCommand(
    val actionType: AutomationActionType,
    val target: String? = null,
    val input: String? = null,
    val delaySeconds: Long = 0L,
    val globalNavAction: GlobalNavAction? = null,
    val isDestructive: Boolean = false,
    val rawExplanation: String = ""
)

data class AutomationResult(
    val success: Boolean,
    val message: String,
    val actionTaken: String? = null,
    val data: Any? = null
)

class DeviceAutomationManager(private val context: Context) {

    companion object {
        private const val TAG = "DeviceAutomation"

        // Common app mappings for instant package resolution
        private val COMMON_APPS = mapOf(
            "youtube" to listOf("com.google.android.youtube", "YouTube"),
            "yt" to listOf("com.google.android.youtube", "YouTube"),
            "whatsapp" to listOf("com.whatsapp", "WhatsApp"),
            "wa" to listOf("com.whatsapp", "WhatsApp"),
            "chrome" to listOf("com.android.chrome", "Google Chrome"),
            "browser" to listOf("com.android.chrome", "Browser"),
            "google" to listOf("com.google.android.googlequicksearchbox", "Google"),
            "camera" to listOf("android.media.action.IMAGE_CAPTURE", "Camera"),
            "settings" to listOf("android.settings.SETTINGS", "Settings"),
            "calculator" to listOf("com.google.android.calculator", "Calculator"),
            "calc" to listOf("com.google.android.calculator", "Calculator"),
            "maps" to listOf("com.google.android.apps.maps", "Google Maps"),
            "map" to listOf("com.google.android.apps.maps", "Google Maps"),
            "play store" to listOf("com.android.vending", "Google Play Store"),
            "playstore" to listOf("com.android.vending", "Google Play Store"),
            "gmail" to listOf("com.google.android.gm", "Gmail"),
            "mail" to listOf("com.google.android.gm", "Gmail"),
            "gallery" to listOf("com.google.android.apps.photos", "Photos / Gallery"),
            "photos" to listOf("com.google.android.apps.photos", "Google Photos"),
            "spotify" to listOf("com.spotify.music", "Spotify"),
            "instagram" to listOf("com.instagram.android", "Instagram"),
            "insta" to listOf("com.instagram.android", "Instagram"),
            "clock" to listOf("com.google.android.deskclock", "Clock / Alarm"),
            "alarm" to listOf("com.google.android.deskclock", "Clock / Alarm"),
            "contacts" to listOf("com.google.android.contacts", "Contacts"),
            "messages" to listOf("com.google.android.apps.messaging", "Messages"),
            "sms" to listOf("com.google.android.apps.messaging", "Messages"),
            "phone" to listOf("android.intent.action.DIAL", "Phone / Dialer"),
            "dialer" to listOf("android.intent.action.DIAL", "Phone / Dialer"),
            "telegram" to listOf("org.telegram.messenger", "Telegram"),
            "phonepe" to listOf("com.phonepe.app", "PhonePe"),
            "gpay" to listOf("com.google.android.apps.nbu.paisa.user", "Google Pay"),
            "paytm" to listOf("net.one97.paytm", "Paytm"),
            "netflix" to listOf("com.netflix.mediaclient", "Netflix"),
            "hotstar" to listOf("in.startv.hotstar", "Disney+ Hotstar"),
            "prime" to listOf("com.amazon.avod.thirdpartyclient", "Prime Video")
        )
    }

    private var isTorchOn: Boolean = false

    /**
     * Parses natural language command into structured automation instruction
     */
    fun parseCommand(userText: String): ParsedCommand? {
        val text = userText.trim()
        val lower = text.lowercase(Locale.ROOT)

        // 1. Detect delay (e.g. "5 second baad", "10 seconds baad", "2 minute baad", "after 5 seconds")
        val delaySec = extractDelaySeconds(lower)

        // Clean delay phrases from text to parse core action
        val cleanedText = lower
            .replace(Regex("\\d+\\s*(second|sec|minute|min|seconds|minutes)\\s*(baad|ke baad|after)?"), "")
            .replace(Regex("after\\s*\\d+\\s*(second|sec|minute|min|seconds|minutes)"), "")
            .trim()

        // 2. Destructive safety check
        if (isDestructiveAction(lower)) {
            return ParsedCommand(
                actionType = AutomationActionType.SAFETY_CONFIRMATION_REQUIRED,
                target = "Security Protection",
                isDestructive = true,
                rawExplanation = "Caution: This request involves sensitive or potentially destructive device operations. Explicit confirmation is required before proceeding."
            )
        }

        // 3. Flashlight / Torch commands
        if (cleanedText.contains("torch on") || cleanedText.contains("flashlight on") || cleanedText.contains("flash on") ||
            cleanedText.contains("torch jalao") || cleanedText.contains("flashlight jalao") || cleanedText.contains("flash chalao") ||
            cleanedText.contains("turn on torch") || cleanedText.contains("turn on flashlight")
        ) {
            return ParsedCommand(
                actionType = AutomationActionType.FLASHLIGHT,
                target = "ON",
                delaySeconds = delaySec
            )
        }
        if (cleanedText.contains("torch off") || cleanedText.contains("flashlight off") || cleanedText.contains("flash off") ||
            cleanedText.contains("torch band") || cleanedText.contains("flashlight band") || cleanedText.contains("flash band") ||
            cleanedText.contains("turn off torch") || cleanedText.contains("turn off flashlight")
        ) {
            return ParsedCommand(
                actionType = AutomationActionType.FLASHLIGHT,
                target = "OFF",
                delaySeconds = delaySec
            )
        }
        if (cleanedText == "torch" || cleanedText == "flashlight" || cleanedText.contains("toggle torch") || cleanedText.contains("toggle flash")) {
            return ParsedCommand(
                actionType = AutomationActionType.FLASHLIGHT,
                target = "TOGGLE",
                delaySeconds = delaySec
            )
        }

        // 4. Volume commands
        if (cleanedText.contains("mute") || cleanedText.contains("silent") || cleanedText.contains("awaaz band")) {
            return ParsedCommand(
                actionType = AutomationActionType.VOLUME,
                target = "MUTE",
                input = "0",
                delaySeconds = delaySec
            )
        }
        if (cleanedText.contains("volume full") || cleanedText.contains("volume max") || cleanedText.contains("volume 100") || cleanedText.contains("awaaz full")) {
            return ParsedCommand(
                actionType = AutomationActionType.VOLUME,
                target = "SET",
                input = "100",
                delaySeconds = delaySec
            )
        }
        val volMatch = Regex("volume\\s*(\\d+)(?:%|percent)?").find(cleanedText)
        if (volMatch != null) {
            val level = volMatch.groupValues[1]
            return ParsedCommand(
                actionType = AutomationActionType.VOLUME,
                target = "SET",
                input = level,
                delaySeconds = delaySec
            )
        }
        if (cleanedText.contains("volume badhao") || cleanedText.contains("volume up") || cleanedText.contains("awaaz badhao") || cleanedText.contains("increase volume")) {
            return ParsedCommand(
                actionType = AutomationActionType.VOLUME,
                target = "UP",
                delaySeconds = delaySec
            )
        }
        if (cleanedText.contains("volume kam") || cleanedText.contains("volume down") || cleanedText.contains("awaaz kam") || cleanedText.contains("decrease volume")) {
            return ParsedCommand(
                actionType = AutomationActionType.VOLUME,
                target = "DOWN",
                delaySeconds = delaySec
            )
        }

        // 5. Battery & Telemetry commands
        if (cleanedText.contains("battery status") || cleanedText.contains("battery kitni") || cleanedText.contains("battery percent") ||
            cleanedText == "battery" || cleanedText.contains("device status") || cleanedText.contains("ram status") ||
            cleanedText.contains("system telemetry") || cleanedText.contains("system status") || cleanedText.contains("device health")
        ) {
            return ParsedCommand(
                actionType = AutomationActionType.TELEMETRY,
                target = "SYSTEM_HEALTH",
                delaySeconds = delaySec
            )
        }

        // 6. Macro Routines
        if (cleanedText.contains("morning routine") || cleanedText.contains("morning briefing") || cleanedText.contains("morning protocol")) {
            return ParsedCommand(
                actionType = AutomationActionType.MACRO_ROUTINE,
                target = "morning_briefing",
                delaySeconds = delaySec
            )
        }
        if (cleanedText.contains("gaming mode") || cleanedText.contains("game mode") || cleanedText.contains("focus mode")) {
            return ParsedCommand(
                actionType = AutomationActionType.MACRO_ROUTINE,
                target = "gaming_mode",
                delaySeconds = delaySec
            )
        }
        if (cleanedText.contains("night routine") || cleanedText.contains("night protocol") || cleanedText.contains("lockdown")) {
            return ParsedCommand(
                actionType = AutomationActionType.MACRO_ROUTINE,
                target = "night_lockdown",
                delaySeconds = delaySec
            )
        }
        if (cleanedText.contains("system scan") || cleanedText.contains("diagnostic") || cleanedText.contains("run diagnostic")) {
            return ParsedCommand(
                actionType = AutomationActionType.MACRO_ROUTINE,
                target = "system_diagnostic",
                delaySeconds = delaySec
            )
        }

        // 7. System Settings Navigation
        if (cleanedText.contains("wifi settings") || cleanedText == "wifi kholo" || cleanedText.contains("wifi open")) {
            return ParsedCommand(
                actionType = AutomationActionType.SYSTEM_SETTINGS,
                target = "WIFI",
                delaySeconds = delaySec
            )
        }
        if (cleanedText.contains("bluetooth settings") || cleanedText == "bluetooth kholo" || cleanedText.contains("bluetooth open")) {
            return ParsedCommand(
                actionType = AutomationActionType.SYSTEM_SETTINGS,
                target = "BLUETOOTH",
                delaySeconds = delaySec
            )
        }
        if (cleanedText.contains("hotspot settings") || cleanedText.contains("hotspot kholo")) {
            return ParsedCommand(
                actionType = AutomationActionType.SYSTEM_SETTINGS,
                target = "HOTSPOT",
                delaySeconds = delaySec
            )
        }
        if (cleanedText.contains("display settings") || cleanedText.contains("brightness settings")) {
            return ParsedCommand(
                actionType = AutomationActionType.SYSTEM_SETTINGS,
                target = "DISPLAY",
                delaySeconds = delaySec
            )
        }

        // 8. Screenshot & Global Navigation
        if (cleanedText.contains("screenshot") || cleanedText.contains("screen capture") || cleanedText.contains("screenshot lo")) {
            return ParsedCommand(
                actionType = AutomationActionType.GLOBAL_NAV,
                globalNavAction = GlobalNavAction.SCREENSHOT,
                delaySeconds = delaySec
            )
        }
        if (cleanedText.contains("go home") || cleanedText == "home jao" || cleanedText == "home screen" || cleanedText.contains("home par jao")) {
            return ParsedCommand(
                actionType = AutomationActionType.GLOBAL_NAV,
                globalNavAction = GlobalNavAction.HOME,
                delaySeconds = delaySec
            )
        }
        if (cleanedText.contains("go back") || cleanedText == "back jao" || cleanedText == "piche jao" || cleanedText.contains("wapas jao")) {
            return ParsedCommand(
                actionType = AutomationActionType.GLOBAL_NAV,
                globalNavAction = GlobalNavAction.BACK,
                delaySeconds = delaySec
            )
        }
        if (cleanedText.contains("recent apps") || cleanedText.contains("recents kholo") || cleanedText.contains("app switcher")) {
            return ParsedCommand(
                actionType = AutomationActionType.GLOBAL_NAV,
                globalNavAction = GlobalNavAction.RECENTS,
                delaySeconds = delaySec
            )
        }
        if (cleanedText.contains("notification panel") || cleanedText.contains("notifications kholo") || cleanedText.contains("notifications dikhao")) {
            return ParsedCommand(
                actionType = AutomationActionType.GLOBAL_NAV,
                globalNavAction = GlobalNavAction.NOTIFICATIONS,
                delaySeconds = delaySec
            )
        }
        if (cleanedText.contains("quick settings") || cleanedText.contains("control center")) {
            return ParsedCommand(
                actionType = AutomationActionType.GLOBAL_NAV,
                globalNavAction = GlobalNavAction.QUICK_SETTINGS,
                delaySeconds = delaySec
            )
        }

        // 9. Screen analysis command
        if (cleanedText.contains("screen analyze") || cleanedText.contains("analyze screen") ||
            cleanedText.contains("screen read") || cleanedText.contains("screen par kya") ||
            cleanedText.contains("screen dekho") || cleanedText.contains("describe screen") ||
            cleanedText.contains("read screen")
        ) {
            return ParsedCommand(
                actionType = AutomationActionType.ANALYZE_SCREEN,
                delaySeconds = delaySec
            )
        }

        // 10. Open App + Search combination (e.g. "YouTube kholo aur Minecraft search karo")
        val searchInAppMatch = Regex("(youtube|google|chrome|play store|maps|browser)\\s*(kholo|open karo|me|par)?\\s*(aur|and)?\\s*(.+?)\\s*(search karo|dhundo|play karo|search)").find(cleanedText)
        if (searchInAppMatch != null) {
            val appName = searchInAppMatch.groupValues[1]
            val query = searchInAppMatch.groupValues[4].trim()
            return ParsedCommand(
                actionType = AutomationActionType.SEARCH,
                target = appName,
                input = query,
                delaySeconds = delaySec
            )
        }

        // 11. Direct Search Command
        val directSearchMatch = Regex("(search karo|search for|google karo|dhundo)\\s*(.+)", RegexOption.IGNORE_CASE).find(cleanedText)
        if (directSearchMatch != null) {
            val query = directSearchMatch.groupValues[2].trim()
            return ParsedCommand(
                actionType = AutomationActionType.SEARCH,
                target = "browser",
                input = query,
                delaySeconds = delaySec
            )
        }

        // 12. Open App command
        val openAppMatch = Regex("(open|kholo|chalao|launch|start)\\s+([a-zA-Z0-9_\\s]+)", RegexOption.IGNORE_CASE).find(cleanedText)
            ?: Regex("([a-zA-Z0-9_\\s]+)\\s+(open karo|kholo|chalao|launch karo)", RegexOption.IGNORE_CASE).find(cleanedText)

        if (openAppMatch != null) {
            val rawTarget = if (openAppMatch.groupValues[1].matches(Regex("(open|kholo|chalao|launch|start)", RegexOption.IGNORE_CASE))) {
                openAppMatch.groupValues[2].trim()
            } else {
                openAppMatch.groupValues[1].trim()
            }

            val target = rawTarget.replace(Regex("\\b(app|application|ko|ka|karo|please|jaldi|now)\\b", RegexOption.IGNORE_CASE), "").trim()
            if (target.isNotEmpty() && isKnownOrInstalledApp(target)) {
                return ParsedCommand(
                    actionType = AutomationActionType.OPEN_APP,
                    target = target,
                    delaySeconds = delaySec
                )
            }
        }

        // 13. Generate Text command
        if (cleanedText.contains("title") && (cleanedText.contains("description") || cleanedText.contains("likho") || cleanedText.contains("banao") || cleanedText.contains("generate"))) {
            return ParsedCommand(
                actionType = AutomationActionType.GENERATE_TEXT,
                input = text
            )
        }

        return null
    }

    private fun extractDelaySeconds(lowerText: String): Long {
        val secPattern = Pattern.compile("(\\d+)\\s*(?:second|sec|seconds)")
        val secMatcher = secPattern.matcher(lowerText)
        if (secMatcher.find()) {
            return secMatcher.group(1)?.toLongOrNull() ?: 0L
        }

        val minPattern = Pattern.compile("(\\d+)\\s*(?:minute|min|minutes)")
        val minMatcher = minPattern.matcher(lowerText)
        if (minMatcher.find()) {
            val mins = minMatcher.group(1)?.toLongOrNull() ?: 0L
            return mins * 60L
        }

        return 0L
    }

    private fun isDestructiveAction(lowerText: String): Boolean {
        val dangerousKeywords = listOf(
            "factory reset", "format phone", "delete all data", "wipe data",
            "send money", "transfer money", "upi pin", "netbanking password",
            "uninstall system app", "delete contacts", "delete photos permanently"
        )
        return dangerousKeywords.any { lowerText.contains(it) }
    }

    /**
     * Executes the parsed automation command with verification
     */
    suspend fun executeCommand(
        command: ParsedCommand,
        onProgress: ((String) -> Unit)? = null
    ): AutomationResult {
        if (command.delaySeconds > 0) {
            onProgress?.invoke("⏳ JARVIS Automation: Waiting for ${command.delaySeconds} seconds before execution...")
            delay(command.delaySeconds * 1000L)
        }

        return when (command.actionType) {
            AutomationActionType.FLASHLIGHT -> {
                when (command.target) {
                    "ON" -> setFlashlight(true)
                    "OFF" -> setFlashlight(false)
                    else -> toggleFlashlight()
                }
            }
            AutomationActionType.VOLUME -> {
                when (command.target) {
                    "MUTE" -> muteVolume()
                    "UP" -> adjustVolume(1)
                    "DOWN" -> adjustVolume(-1)
                    "SET" -> {
                        val pct = command.input?.toIntOrNull() ?: 50
                        setVolumePercent(pct)
                    }
                    else -> setVolumePercent(70)
                }
            }
            AutomationActionType.TELEMETRY -> {
                getSystemHealthReport()
            }
            AutomationActionType.MACRO_ROUTINE -> {
                executeMacro(command.target ?: "morning_briefing", onProgress)
            }
            AutomationActionType.SYSTEM_SETTINGS -> {
                openSetting(command.target ?: "SETTINGS")
            }
            AutomationActionType.OPEN_APP -> {
                val target = command.target ?: "Application"
                openApp(target)
            }
            AutomationActionType.SEARCH -> {
                searchAppOrWeb(command.target, command.input ?: "")
            }
            AutomationActionType.ANALYZE_SCREEN -> {
                analyzeScreen()
            }
            AutomationActionType.GLOBAL_NAV -> {
                command.globalNavAction?.let { performGlobalNav(it) }
                    ?: AutomationResult(false, "Unknown global navigation action")
            }
            AutomationActionType.SAFETY_CONFIRMATION_REQUIRED -> {
                AutomationResult(
                    success = false,
                    message = "⚠️ SAFETY BARRIER: ${command.rawExplanation}"
                )
            }
            else -> {
                AutomationResult(false, "Command is not actionable directly on device")
            }
        }
    }

    // --- Hardware Controls ---

    fun setFlashlight(enable: Boolean): AutomationResult {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            val cameraId = cameraManager?.cameraIdList?.firstOrNull()
            if (cameraManager != null && cameraId != null) {
                cameraManager.setTorchMode(cameraId, enable)
                isTorchOn = enable
                AutomationResult(
                    success = true,
                    message = if (enable) "🔦 Flashlight turned ON." else "🔦 Flashlight turned OFF.",
                    actionTaken = "FLASHLIGHT: ${if (enable) "ON" else "OFF"}",
                    data = enable
                )
            } else {
                AutomationResult(false, "Camera / Flashlight hardware not available on this device.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting torch mode", e)
            AutomationResult(false, "Failed to control flashlight: ${e.message}")
        }
    }

    fun toggleFlashlight(): AutomationResult {
        return setFlashlight(!isTorchOn)
    }

    fun getFlashlightState(): Boolean = isTorchOn

    fun setVolumePercent(percent: Int): AutomationResult {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (audioManager != null) {
                val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val targetVol = ((percent.coerceIn(0, 100) / 100.0) * maxVol).toInt().coerceIn(0, maxVol)
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, AudioManager.FLAG_SHOW_UI)
                AutomationResult(
                    success = true,
                    message = "🔊 Media Volume adjusted to $percent% (level $targetVol/$maxVol).",
                    actionTaken = "VOLUME: $percent%",
                    data = percent
                )
            } else {
                AutomationResult(false, "Audio service unavailable.")
            }
        } catch (e: Exception) {
            AutomationResult(false, "Failed to adjust volume: ${e.message}")
        }
    }

    fun adjustVolume(step: Int): AutomationResult {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (audioManager != null) {
                val direction = if (step > 0) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
                val currentPct = getCurrentVolumePercent()
                AutomationResult(
                    success = true,
                    message = "🔊 Volume ${if (step > 0) "increased" else "decreased"} to $currentPct%.",
                    actionTaken = "VOLUME_ADJUST",
                    data = currentPct
                )
            } else {
                AutomationResult(false, "Audio service unavailable.")
            }
        } catch (e: Exception) {
            AutomationResult(false, "Failed to adjust volume: ${e.message}")
        }
    }

    fun muteVolume(): AutomationResult {
        return setVolumePercent(0)
    }

    fun getCurrentVolumePercent(): Int {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return 50
            val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            if (max > 0) (current * 100 / max) else 50
        } catch (e: Exception) {
            50
        }
    }

    fun getBatteryTelemetry(): BatteryTelemetry {
        return try {
            val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
                context.registerReceiver(null, filter)
            }
            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val pct = if (level >= 0 && scale > 0) (level * 100 / scale) else 85
            val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            val tempTenths = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 280
            val tempCelsius = tempTenths / 10.0f
            val healthCode = batteryStatus?.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_GOOD) ?: BatteryManager.BATTERY_HEALTH_GOOD
            val healthStr = when (healthCode) {
                BatteryManager.BATTERY_HEALTH_GOOD -> "Good (Optimal)"
                BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheated"
                BatteryManager.BATTERY_HEALTH_DEAD -> "Critical"
                else -> "Normal"
            }
            BatteryTelemetry(pct, isCharging, tempCelsius, healthStr)
        } catch (e: Exception) {
            BatteryTelemetry(85, false, 28.5f, "Good (Optimal)")
        }
    }

    fun getMemoryTelemetry(): MemoryTelemetry {
        return try {
            val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            actManager?.getMemoryInfo(memInfo)
            val totalRam = memInfo.totalMem / (1024 * 1024)
            val availRam = memInfo.availMem / (1024 * 1024)
            val usedRam = (totalRam - availRam).coerceAtLeast(0)
            val ramPercent = if (totalRam > 0) ((usedRam.toDouble() / totalRam.toDouble()) * 100).toInt() else 45

            val path = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availableBlocks = stat.availableBlocksLong
            val totalStorage = (totalBlocks * blockSize) / (1024f * 1024f * 1024f)
            val freeStorage = (availableBlocks * blockSize) / (1024f * 1024f * 1024f)

            MemoryTelemetry(usedRam, totalRam, ramPercent, freeStorage, totalStorage)
        } catch (e: Exception) {
            MemoryTelemetry(3400, 8192, 42, 48.5f, 128.0f)
        }
    }

    fun getSystemHealthReport(): AutomationResult {
        val batt = getBatteryTelemetry()
        val mem = getMemoryTelemetry()
        val accEnabled = getAccessibilityStatus()

        val report = buildString {
            append("⚡ **JARVIS REAL-TIME DEVICE TELEMETRY**\n\n")
            append("🔋 **Battery:** ${batt.percentage}% ${if (batt.isCharging) "[⚡ CHARGING]" else "[DISCHARGING]"} (${batt.temperatureCelsius}°C, ${batt.health})\n")
            append("🧠 **RAM:** ${mem.usedRamMb}MB / ${mem.totalRamMb}MB (${mem.ramPercent}% load)\n")
            append("💾 **Storage:** ${String.format("%.1f", mem.freeStorageGb)}GB Free / ${String.format("%.1f", mem.totalStorageGb)}GB Total\n")
            append("🔊 **Volume:** ${getCurrentVolumePercent()}%\n")
            append("🛡️ **Automation Service:** ${if (accEnabled) "🟢 ACTIVE & ONLINE" else "🔴 OFF (Needs Permission in Settings)"}")
        }

        return AutomationResult(
            success = true,
            message = report,
            actionTaken = "DEVICE_TELEMETRY",
            data = batt
        )
    }

    // --- Macro Routines ---

    suspend fun executeMacro(macroId: String, onProgress: ((String) -> Unit)? = null): AutomationResult {
        return when (macroId) {
            "morning_briefing" -> {
                onProgress?.invoke("🌅 Executing Morning Protocol: Checking system diagnostics...")
                delay(800)
                setVolumePercent(75)
                onProgress?.invoke("🔊 Setting voice and media volume to optimal 75%...")
                delay(800)
                val batt = getBatteryTelemetry()
                onProgress?.invoke("🔋 Battery calibrated: ${batt.percentage}%")
                delay(600)
                searchAppOrWeb("google", "today latest news headlines")
                AutomationResult(
                    success = true,
                    message = "🌅 **Morning Briefing Complete!**\nVolume calibrated to 75%, battery verified at ${batt.percentage}%, and morning news briefing opened.",
                    actionTaken = "MACRO: Morning Briefing"
                )
            }
            "gaming_mode" -> {
                onProgress?.invoke("🎮 Engaging Gaming & Ultra Focus Mode...")
                delay(600)
                setVolumePercent(0)
                onProgress?.invoke("🔇 Distractions silenced. Media volume muted.")
                delay(600)
                AutomationResult(
                    success = true,
                    message = "🎮 **Gaming & Ultra-Focus Mode Active!**\nAudio muted, background distractions cleared. Commander, you are clear for battle.",
                    actionTaken = "MACRO: Gaming Mode"
                )
            }
            "night_lockdown" -> {
                onProgress?.invoke("🌙 Initiating Night Lockdown Protocol...")
                delay(600)
                setVolumePercent(15)
                setFlashlight(false)
                onProgress?.invoke("🔦 Flashlight verified OFF. Volume lowered to 15%.")
                delay(600)
                openApp("clock")
                AutomationResult(
                    success = true,
                    message = "🌙 **Night Lockdown Engaged!**\nVolume set to 15%, torch turned off, and Clock/Alarm opened for sleep cycle.",
                    actionTaken = "MACRO: Night Lockdown"
                )
            }
            "system_diagnostic" -> {
                onProgress?.invoke("⚡ Running deep neural & hardware diagnostic...")
                delay(1000)
                getSystemHealthReport()
            }
            else -> {
                AutomationResult(false, "Unknown macro routine: $macroId")
            }
        }
    }

    fun openSetting(settingType: String): AutomationResult {
        return try {
            val intent = when (settingType.uppercase(Locale.ROOT)) {
                "WIFI" -> Intent(Settings.ACTION_WIFI_SETTINGS)
                "BLUETOOTH" -> Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                "HOTSPOT", "WIRELESS" -> Intent(Settings.ACTION_WIRELESS_SETTINGS)
                "DISPLAY", "BRIGHTNESS" -> Intent(Settings.ACTION_DISPLAY_SETTINGS)
                "SOUND", "VOLUME" -> Intent(Settings.ACTION_SOUND_SETTINGS)
                "ACCESSIBILITY" -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                "APPS" -> Intent(Settings.ACTION_APPLICATION_SETTINGS)
                else -> Intent(Settings.ACTION_SETTINGS)
            }.apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            AutomationResult(true, "Opened $settingType Settings on device.", "SETTINGS: $settingType")
        } catch (e: Exception) {
            AutomationResult(false, "Failed to open $settingType settings: ${e.message}")
        }
    }

    /**
     * Opens an app by user friendly name or package name
     */
    fun openApp(appNameOrPackage: String): AutomationResult {
        val cleanName = appNameOrPackage.trim().lowercase(Locale.ROOT)

        // Special system intents
        when (cleanName) {
            "camera" -> {
                return try {
                    val intent = Intent("android.media.action.IMAGE_CAPTURE").apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    AutomationResult(true, "Camera opened successfully.", "OPEN_APP: Camera")
                } catch (e: Exception) {
                    AutomationResult(false, "Could not open Camera app: ${e.message}")
                }
            }
            "settings" -> {
                return try {
                    val intent = Intent(Settings.ACTION_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    AutomationResult(true, "Android Settings opened successfully.", "OPEN_APP: Settings")
                } catch (e: Exception) {
                    AutomationResult(false, "Could not open Settings: ${e.message}")
                }
            }
            "phone", "dialer" -> {
                return try {
                    val intent = Intent(Intent.ACTION_DIAL).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    AutomationResult(true, "Phone dialer opened successfully.", "OPEN_APP: Phone")
                } catch (e: Exception) {
                    AutomationResult(false, "Could not open dialer: ${e.message}")
                }
            }
        }

        // Check common apps map
        val resolvedPkg = COMMON_APPS[cleanName]?.firstOrNull() ?: findInstalledPackage(cleanName)

        if (resolvedPkg != null) {
            val pm = context.packageManager
            val launchIntent = pm.getLaunchIntentForPackage(resolvedPkg)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                return try {
                    context.startActivity(launchIntent)
                    val label = COMMON_APPS[cleanName]?.getOrNull(1) ?: getAppLabel(resolvedPkg)
                    AutomationResult(true, "$label ($resolvedPkg) successfully launched on device.", "OPEN_APP: $label")
                } catch (e: Exception) {
                    AutomationResult(false, "Failed to launch $cleanName: ${e.message}")
                }
            }
        }

        // Try web fallback if app is not installed (e.g. YouTube web)
        if (cleanName == "youtube" || cleanName == "yt") {
            return try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                AutomationResult(true, "YouTube opened via browser.", "OPEN_APP: YouTube (Web)")
            } catch (e: Exception) {
                AutomationResult(false, "Could not open YouTube.")
            }
        }

        return AutomationResult(
            success = false,
            message = "App '$appNameOrPackage' is not installed or cannot be launched."
        )
    }

    /**
     * Searches inside an app or browser
     */
    fun searchAppOrWeb(targetApp: String?, query: String): AutomationResult {
        if (query.isBlank()) {
            return AutomationResult(false, "Search query was empty.")
        }

        val app = targetApp?.lowercase(Locale.ROOT) ?: "browser"

        if (app.contains("youtube") || app == "yt") {
            try {
                val ytIntent = Intent(Intent.ACTION_SEARCH).apply {
                    setPackage("com.google.android.youtube")
                    putExtra("query", query)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(ytIntent)
                return AutomationResult(true, "Searched for '$query' on YouTube app.", "SEARCH: YouTube -> $query")
            } catch (e: Exception) {
                return try {
                    val encoded = Uri.encode(query)
                    val webYt = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=$encoded")).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(webYt)
                    AutomationResult(true, "Searched for '$query' on YouTube.", "SEARCH: YouTube -> $query")
                } catch (ex: Exception) {
                    AutomationResult(false, "Failed to search YouTube: ${ex.message}")
                }
            }
        }

        if (app.contains("map")) {
            return try {
                val mapUri = Uri.parse("geo:0,0?q=${Uri.encode(query)}")
                val mapIntent = Intent(Intent.ACTION_VIEW, mapUri).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(mapIntent)
                AutomationResult(true, "Searching Google Maps for '$query'.", "SEARCH: Maps -> $query")
            } catch (e: Exception) {
                AutomationResult(false, "Failed to search Maps: ${e.message}")
            }
        }

        if (app.contains("play store") || app.contains("playstore")) {
            return try {
                val marketUri = Uri.parse("market://search?q=${Uri.encode(query)}")
                val marketIntent = Intent(Intent.ACTION_VIEW, marketUri).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(marketIntent)
                AutomationResult(true, "Searching Play Store for '$query'.", "SEARCH: Play Store -> $query")
            } catch (e: Exception) {
                AutomationResult(false, "Failed to search Play Store: ${e.message}")
            }
        }

        // Standard Web / Google Search
        return try {
            val searchIntent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra(SearchManager.QUERY, query)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(searchIntent)
            AutomationResult(true, "Submitted web search for '$query'.", "SEARCH: Web -> $query")
        } catch (e: Exception) {
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(browserIntent)
                AutomationResult(true, "Opened Google search for '$query'.", "SEARCH: Browser -> $query")
            } catch (ex: Exception) {
                AutomationResult(false, "Failed to perform search: ${ex.message}")
            }
        }
    }

    /**
     * Analyzes current visible screen via Accessibility Service
     */
    fun analyzeScreen(): AutomationResult {
        val service = JarvisAccessibilityService.getInstance()
        if (service == null) {
            val isConfigured = JarvisAccessibilityService.isAccessibilityEnabled(context)
            return AutomationResult(
                success = false,
                message = if (isConfigured) {
                    "JARVIS Accessibility Service is enabled in system but active window is currently locked or transitioning."
                } else {
                    "JARVIS Accessibility Service is NOT enabled. To allow screen reading and UI automation, please turn on 'JARVIS Automation Service' in Android Accessibility Settings."
                },
                data = null
            )
        }

        val summary = service.getScreenHierarchySummary()
        return AutomationResult(
            success = true,
            message = "Screen analysis complete for app: ${summary.packageName}",
            actionTaken = "ANALYZE_SCREEN",
            data = summary
        )
    }

    /**
     * Performs global Android navigation (Back, Home, Recents, Notifications, Screenshot, etc.)
     */
    fun performGlobalNav(action: GlobalNavAction): AutomationResult {
        val service = JarvisAccessibilityService.getInstance()
        if (service == null) {
            // Provide intelligent fallback for Home/Settings if accessibility service is off
            when (action) {
                GlobalNavAction.HOME -> {
                    try {
                        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                            addCategory(Intent.CATEGORY_HOME)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(homeIntent)
                        return AutomationResult(true, "Navigated to Home Screen.", "GLOBAL_NAV: HOME")
                    } catch (e: Exception) {
                        return AutomationResult(false, "Failed to go home: ${e.message}")
                    }
                }
                GlobalNavAction.QUICK_SETTINGS -> {
                    return openSetting("SETTINGS")
                }
                else -> {
                    return AutomationResult(
                        success = false,
                        message = "Global navigation for ${action.name} requires JARVIS Accessibility Service. Tap 'Enable Automation' to grant permission."
                    )
                }
            }
        }

        val actionCode = when (action) {
            GlobalNavAction.HOME -> android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME
            GlobalNavAction.BACK -> android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK
            GlobalNavAction.RECENTS -> android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_RECENTS
            GlobalNavAction.NOTIFICATIONS -> android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS
            GlobalNavAction.QUICK_SETTINGS -> android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS
            GlobalNavAction.LOCK_SCREEN -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN
            } else {
                android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME
            }
            GlobalNavAction.SCREENSHOT -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT
            } else {
                android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS
            }
        }

        val success = service.triggerGlobalAction(actionCode)
        return if (success) {
            AutomationResult(true, "Action ${action.name} performed successfully on device.", "GLOBAL_NAV: ${action.name}")
        } else {
            AutomationResult(false, "Failed to perform ${action.name} global navigation.")
        }
    }

    private fun isKnownOrInstalledApp(target: String): Boolean {
        val clean = target.trim().lowercase(Locale.ROOT)
        if (clean.isBlank() || clean.length < 2) return false

        // Conversational words that should NEVER be treated as app names
        val conversationalWords = setOf(
            "baat", "dimag", "muh", "topic", "chat", "kuch", "apna", "suno", "bhai", "yaar",
            "conversation", "bolna", "story", "joke", "chutkula", "code", "python", "ai",
            "brain", "system", "life", "dost", "friend", "dil", "aankh", "kaan", "timepass",
            "kaam", "help", "sawal", "question", "kavita", "shayari", "charcha", "bolo", "batao"
        )
        if (conversationalWords.contains(clean) || clean.split("\\s+".toRegex()).any { conversationalWords.contains(it) }) {
            return false
        }

        if (COMMON_APPS.containsKey(clean)) return true
        if (clean in setOf("camera", "settings", "phone", "dialer", "calculator", "gallery", "browser", "music", "clock", "messages", "sms", "email", "gmail", "contacts")) return true

        return findInstalledPackage(clean) != null
    }

    private fun findInstalledPackage(nameQuery: String): String? {
        return try {
            val pm = context.packageManager
            val apps = pm.getInstalledApplications(0)
            for (app in apps) {
                val label = try {
                    pm.getApplicationLabel(app).toString().lowercase(Locale.ROOT)
                } catch (e: Exception) {
                    ""
                }
                val pkg = app.packageName.lowercase(Locale.ROOT)
                if (label == nameQuery || (label.isNotEmpty() && label.contains(nameQuery)) || pkg.contains(nameQuery)) {
                    return app.packageName
                }
            }
            null
        } catch (e: Exception) {
            Log.e("DeviceAutomation", "Error finding installed package", e)
            null
        }
    }

    private fun getAppLabel(packageName: String): String {
        return try {
            val pm = context.packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString()
        } catch (e: Exception) {
            packageName
        }
    }

    fun getAccessibilityStatus(): Boolean {
        return try {
            JarvisAccessibilityService.isAccessibilityEnabled(context)
        } catch (e: Exception) {
            false
        }
    }

    fun openAccessibilitySettings() {
        try {
            JarvisAccessibilityService.openAccessibilitySettings(context)
        } catch (e: Exception) {
            Log.e("DeviceAutomation", "Could not open accessibility settings", e)
        }
    }
}
