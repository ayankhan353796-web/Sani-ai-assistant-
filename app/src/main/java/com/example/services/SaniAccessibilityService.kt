package com.example.services

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class PhoneControlAction(val label: String, val description: String) {
    LOCK_SCREEN("Lock Screen", "Locks device screen safely (requires API 28+)"),
    HOME("Go Home", "Navigates to Android Home screen"),
    BACK("Navigate Back", "Performs system Back action"),
    RECENTS("Recent Apps", "Opens Android Recents overview"),
    NOTIFICATIONS("Open Notifications", "Pulls down notification shade"),
    SCREENSHOT("Take Screenshot", "Captures screen (API 28+)")
}

class SaniAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isConnected.value = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Intentionally minimal; SANI never scrapes screen content passively.
    }

    override fun onInterrupt() {
        _isConnected.value = false
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance === this) {
            instance = null
        }
        _isConnected.value = false
    }

    fun executeAction(action: PhoneControlAction): Boolean {
        return when (action) {
            PhoneControlAction.LOCK_SCREEN -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
                } else {
                    false
                }
            }
            PhoneControlAction.HOME -> performGlobalAction(GLOBAL_ACTION_HOME)
            PhoneControlAction.BACK -> performGlobalAction(GLOBAL_ACTION_BACK)
            PhoneControlAction.RECENTS -> performGlobalAction(GLOBAL_ACTION_RECENTS)
            PhoneControlAction.NOTIFICATIONS -> performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
            PhoneControlAction.SCREENSHOT -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
                } else {
                    false
                }
            }
        }
    }

    companion object {
        private val _isConnected = MutableStateFlow(false)
        val isConnected = _isConnected.asStateFlow()

        @Volatile
        private var instance: SaniAccessibilityService? = null

        fun perform(action: PhoneControlAction): Boolean {
            return instance?.executeAction(action) ?: false
        }

        fun isServiceEnabledInSettings(context: Context): Boolean {
            val expected = ComponentName(context, SaniAccessibilityService::class.java).flattenToString()
            val enabled = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
        }
    }
}
