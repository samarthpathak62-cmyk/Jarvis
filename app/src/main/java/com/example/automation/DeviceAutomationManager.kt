package com.example.automation

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
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
    SAFETY_CONFIRMATION_REQUIRED,
    UNKNOWN
}

enum class GlobalNavAction {
    HOME,
    BACK,
    RECENTS,
    NOTIFICATIONS
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
            "chrome" to listOf("com.android.chrome", "Google Chrome"),
            "browser" to listOf("com.android.chrome", "Browser"),
            "google" to listOf("com.google.android.googlequicksearchbox", "Google"),
            "camera" to listOf("android.media.action.IMAGE_CAPTURE", "Camera"),
            "settings" to listOf("android.settings.SETTINGS", "Settings"),
            "calculator" to listOf("com.google.android.calculator", "Calculator"),
            "maps" to listOf("com.google.android.apps.maps", "Google Maps"),
            "play store" to listOf("com.android.vending", "Google Play Store"),
            "playstore" to listOf("com.android.vending", "Google Play Store"),
            "gmail" to listOf("com.google.android.gm", "Gmail"),
            "mail" to listOf("com.google.android.gm", "Gmail"),
            "gallery" to listOf("com.google.android.apps.photos", "Photos / Gallery"),
            "photos" to listOf("com.google.android.apps.photos", "Google Photos"),
            "spotify" to listOf("com.spotify.music", "Spotify"),
            "instagram" to listOf("com.instagram.android", "Instagram"),
            "clock" to listOf("com.google.android.deskclock", "Clock"),
            "contacts" to listOf("com.google.android.contacts", "Contacts"),
            "messages" to listOf("com.google.android.apps.messaging", "Messages"),
            "phone" to listOf("android.intent.action.DIAL", "Phone / Dialer"),
            "dialer" to listOf("android.intent.action.DIAL", "Phone / Dialer")
        )
    }

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

        // 3. Screen analysis command ("Screen analyze karo", "screen read karo", "screen par kya hai", "analyze screen")
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

        // 4. Global Navigation (Home, Back, Recents, Notifications)
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

        // 5. Open App + Search combination (e.g. "YouTube kholo aur Minecraft search karo", "Chrome me search karo weather")
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

        // 6. Direct Search Command (e.g. "search karo best movies", "google par search karo...", "search for ...")
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

        // 7. Open App command (e.g. "YouTube kholo", "open WhatsApp", "Chrome chalao", "Camera open karo")
        val openAppMatch = Regex("(open|kholo|chalao|launch|start)\\s+([a-zA-Z0-9_\\s]+)", RegexOption.IGNORE_CASE).find(cleanedText)
            ?: Regex("([a-zA-Z0-9_\\s]+)\\s+(open karo|kholo|chalao|launch karo)", RegexOption.IGNORE_CASE).find(cleanedText)

        if (openAppMatch != null) {
            val rawTarget = if (openAppMatch.groupValues[1].matches(Regex("(open|kholo|chalao|launch|start)", RegexOption.IGNORE_CASE))) {
                openAppMatch.groupValues[2].trim()
            } else {
                openAppMatch.groupValues[1].trim()
            }

            // Remove stop words
            val target = rawTarget.replace(Regex("\\b(app|application|ko|ka|karo|please)\\b", RegexOption.IGNORE_CASE), "").trim()
            if (target.isNotEmpty()) {
                return ParsedCommand(
                    actionType = AutomationActionType.OPEN_APP,
                    target = target,
                    delaySeconds = delaySec
                )
            }
        }

        // 8. Generate Text command (e.g. "title aur description likho", "caption banao", "is video ke liye title likho")
        if (cleanedText.contains("title") && (cleanedText.contains("description") || cleanedText.contains("likho") || cleanedText.contains("banao") || cleanedText.contains("generate"))) {
            return ParsedCommand(
                actionType = AutomationActionType.GENERATE_TEXT,
                input = text
            )
        }

        return null
    }

    private fun extractDelaySeconds(lowerText: String): Long {
        // e.g. "5 second", "10 seconds", "2 minute", "30 sec"
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
                    val intent = Intent(android.provider.Settings.ACTION_SETTINGS).apply {
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
            // 1. Try YouTube Search Intent
            try {
                val ytIntent = Intent(Intent.ACTION_SEARCH).apply {
                    setPackage("com.google.android.youtube")
                    putExtra("query", query)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(ytIntent)
                return AutomationResult(true, "Searched for '$query' on YouTube app.", "SEARCH: YouTube -> $query")
            } catch (e: Exception) {
                // Fallback to YouTube web search
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
            // Fallback to direct browser URL
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
     * Performs global Android navigation (Back, Home, Recents, Notifications)
     */
    fun performGlobalNav(action: GlobalNavAction): AutomationResult {
        val service = JarvisAccessibilityService.getInstance()
        if (service == null) {
            return AutomationResult(
                success = false,
                message = "Global navigation requires JARVIS Accessibility Service to be enabled in Settings."
            )
        }

        val actionCode = when (action) {
            GlobalNavAction.HOME -> android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME
            GlobalNavAction.BACK -> android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK
            GlobalNavAction.RECENTS -> android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_RECENTS
            GlobalNavAction.NOTIFICATIONS -> android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS
        }

        val success = service.triggerGlobalAction(actionCode)
        return if (success) {
            AutomationResult(true, "Action ${action.name} performed successfully.", "GLOBAL_NAV: ${action.name}")
        } else {
            AutomationResult(false, "Failed to perform ${action.name} global navigation.")
        }
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
