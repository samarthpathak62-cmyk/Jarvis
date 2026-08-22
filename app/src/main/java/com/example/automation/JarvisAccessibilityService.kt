package com.example.automation

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.lang.ref.WeakReference

class JarvisAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instanceRef = WeakReference(this)
        isServiceRunning = true
        Log.d(TAG, "JarvisAccessibilityService connected successfully")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Track active window state
    }

    override fun onInterrupt() {
        Log.d(TAG, "JarvisAccessibilityService interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        if (instanceRef?.get() == this) {
            instanceRef = null
        }
    }

    fun getScreenHierarchySummary(): ScreenHierarchySummary {
        try {
            val root = rootInActiveWindow ?: return ScreenHierarchySummary(
                packageName = "Unknown",
                visibleTexts = emptyList(),
                clickableElements = emptyList(),
                inputElements = emptyList(),
                summaryText = "Screen content is currently not accessible or phone is on secure/locked display."
            )

            val pkgName = root.packageName?.toString() ?: "Unknown"
            val visibleTexts = mutableListOf<String>()
            val clickableElements = mutableListOf<String>()
            val inputElements = mutableListOf<String>()

            fun traverse(node: AccessibilityNodeInfo?, depth: Int) {
                if (node == null || depth > 25) return

                try {
                    val text = node.text?.toString()?.trim()
                    val desc = node.contentDescription?.toString()?.trim()

                    val displayLabel = when {
                        !text.isNullOrEmpty() -> text
                        !desc.isNullOrEmpty() -> desc
                        else -> null
                    }

                    if (!displayLabel.isNullOrEmpty()) {
                        if (node.isClickable) {
                            clickableElements.add(displayLabel)
                        } else if (node.isEditable) {
                            inputElements.add(displayLabel)
                        } else {
                            visibleTexts.add(displayLabel)
                        }
                    }

                    val count = node.childCount
                    for (i in 0 until count) {
                        traverse(node.getChild(i), depth + 1)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error traversing node: ${e.message}")
                }
            }

            traverse(root, 0)

            val sb = StringBuilder()
            sb.append("Current App Package: $pkgName\n")
            if (visibleTexts.isNotEmpty()) {
                sb.append("Visible Text Content: ${visibleTexts.distinct().take(30).joinToString(" | ")}\n")
            }
            if (clickableElements.isNotEmpty()) {
                sb.append("Clickable Buttons/Actions: ${clickableElements.distinct().take(15).joinToString(", ")}\n")
            }
            if (inputElements.isNotEmpty()) {
                sb.append("Input Fields: ${inputElements.distinct().joinToString(", ")}\n")
            }

            return ScreenHierarchySummary(
                packageName = pkgName,
                visibleTexts = visibleTexts.distinct(),
                clickableElements = clickableElements.distinct(),
                inputElements = inputElements.distinct(),
                summaryText = sb.toString()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting screen hierarchy summary", e)
            return ScreenHierarchySummary(
                packageName = "Unknown",
                visibleTexts = emptyList(),
                clickableElements = emptyList(),
                inputElements = emptyList(),
                summaryText = "Screen inspection unavailable: ${e.message}"
            )
        }
    }

    fun clickElementWithText(targetText: String): Boolean {
        return try {
            val root = rootInActiveWindow ?: return false
            val nodes = root.findAccessibilityNodeInfosByText(targetText)
            for (node in nodes) {
                if (node.isClickable && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    return true
                }
                // Check parent if parent is clickable
                var parent = node.parent
                while (parent != null) {
                    if (parent.isClickable && parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                        return true
                    }
                    parent = parent.parent
                }
            }
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error clicking element with text $targetText", e)
            false
        }
    }

    fun triggerGlobalAction(actionCode: Int): Boolean {
        return try {
            performGlobalAction(actionCode)
        } catch (e: Exception) {
            Log.e(TAG, "Error performing global action $actionCode", e)
            false
        }
    }

    companion object {
        private const val TAG = "JarvisAccessibility"
        private var instanceRef: WeakReference<JarvisAccessibilityService>? = null
        var isServiceRunning: Boolean = false
            private set

        fun getInstance(): JarvisAccessibilityService? = instanceRef?.get()

        fun isAccessibilityEnabled(context: Context): Boolean {
            if (isServiceRunning && getInstance() != null) return true
            val expectedServiceName = "${context.packageName}/${JarvisAccessibilityService::class.java.name}"
            val shortServiceName = "${context.packageName}/.automation.JarvisAccessibilityService"
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            return enabledServices.split(":").any {
                it.equals(expectedServiceName, ignoreCase = true) || it.equals(shortServiceName, ignoreCase = true)
            }
        }

        fun openAccessibilitySettings(context: Context) {
            try {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open accessibility settings", e)
            }
        }
    }
}

data class ScreenHierarchySummary(
    val packageName: String,
    val visibleTexts: List<String>,
    val clickableElements: List<String>,
    val inputElements: List<String>,
    val summaryText: String
)
