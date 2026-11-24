package com.example.services

import android.app.Notification
import android.app.RemoteInput
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.data.model.EmotionStyle
import com.example.data.model.SaniLanguage
import com.example.data.repository.AutoReplyRuntimeCache
import com.example.data.repository.SaniAiRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "SaniNotifListener"

data class CapturedMessageNotification(
    val id: String,
    val appName: String,
    val packageName: String,
    val sender: String,
    val text: String,
    val timestampMillis: Long,
    val autoReplyStatus: String
)

class SaniNotificationListenerService : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val aiRepository = SaniAiRepository()
    private val lastReplyTimestamps = ConcurrentHashMap<String, Long>()
    private val repliedNotificationKeys = ConcurrentHashMap.newKeySet<String>()

    override fun onListenerConnected() {
        super.onListenerConnected()
        _isConnected.value = true
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        _isConnected.value = false
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        if (!AutoReplyRuntimeCache.messageReadingEnabled) return

        val notification = sbn.notification ?: return
        if ((notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0) return

        val pkg = sbn.packageName ?: return
        if (!SUPPORTED_MESSAGING_PACKAGES.containsKey(pkg)) return

        val extras = notification.extras ?: return
        val sender = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()
        if (sender.isBlank() || text.isBlank()) return

        val appLabel = SUPPORTED_MESSAGING_PACKAGES[pkg] ?: pkg
        val isGroup = extras.getBoolean(Notification.EXTRA_IS_GROUP_CONVERSATION, false) ||
            extras.containsKey(Notification.EXTRA_CONVERSATION_TITLE)

        val settings = AutoReplyRuntimeCache.settings.value
        var statusLabel = "Read only"

        if (settings.enabled && settings.allowedPackages.contains(pkg)) {
            val safetyReason = checkSafetyBlockReason(
                key = sbn.key,
                sender = sender,
                text = text,
                isGroup = isGroup,
                replyInGroups = settings.replyInGroups,
                replyToAll = settings.replyToAll,
                allowlistCsv = settings.contactAllowlistCsv
            )
            if (safetyReason != null) {
                statusLabel = "Blocked: $safetyReason"
            } else {
                val replyAction = findReplyAction(notification)
                if (replyAction == null) {
                    statusLabel = "No reply action"
                } else {
                    statusLabel = "Auto-replied via SANI"
                    repliedNotificationKeys.add(sbn.key)
                    lastReplyTimestamps["$pkg:$sender"] = System.currentTimeMillis()
                    scope.launch {
                        sendAiAutoReply(replyAction, sender, text)
                    }
                }
            }
        }

        val captured = CapturedMessageNotification(
            id = "${sbn.key}_${sbn.postTime}",
            appName = appLabel,
            packageName = pkg,
            sender = sender,
            text = text,
            timestampMillis = sbn.postTime,
            autoReplyStatus = statusLabel
        )
        _recentMessages.value = (listOf(captured) + _recentMessages.value).take(25)
    }

    private fun checkSafetyBlockReason(
        key: String,
        sender: String,
        text: String,
        isGroup: Boolean,
        replyInGroups: Boolean,
        replyToAll: Boolean,
        allowlistCsv: String
    ): String? {
        if (repliedNotificationKeys.contains(key)) return "Duplicate"
        val lower = text.lowercase()
        val sensitiveKeywords = listOf(
            "otp", "one-time", "one time password", "verification code", "verify code",
            "bank", "debited", "credited", "a/c", "account", "upi", "ifsc", "card ending",
            "payment", "transaction", "paid", "rs.", "inr", "password reset", "reset password",
            "login code", "2fa", "cvv", "pin"
        )
        if (sensitiveKeywords.any { lower.contains(it) }) {
            return "Sensitive/OTP/Banking guard"
        }
        if (isGroup && !replyInGroups) {
            return "Group replies disabled"
        }
        val lastSent = lastReplyTimestamps.entries
            .firstOrNull { it.key.endsWith(":$sender") }?.value ?: 0L
        if (System.currentTimeMillis() - lastSent < 45_000L) {
            return "Anti-loop cooldown"
        }
        if (!replyToAll) {
            val allowedContacts = allowlistCsv.split(",")
                .map { it.trim().lowercase() }
                .filter { it.isNotEmpty() }
            if (allowedContacts.none { sender.lowercase().contains(it) }) {
                return "Sender not in allowlist"
            }
        }
        return null
    }

    private fun findReplyAction(notification: Notification): Pair<Notification.Action, RemoteInput>? {
        val actions = notification.actions ?: return null
        for (action in actions) {
            val remoteInputs = action.remoteInputs ?: continue
            val input = remoteInputs.firstOrNull { it.allowFreeFormInput } ?: continue
            if (action.actionIntent != null) {
                return action to input
            }
        }
        return null
    }

    private suspend fun sendAiAutoReply(
        actionPair: Pair<Notification.Action, RemoteInput>,
        sender: String,
        incomingText: String
    ) {
        val language = SaniLanguage.fromCode(AutoReplyRuntimeCache.activeLanguageCode)
        val emotion = EmotionStyle.fromLabel(AutoReplyRuntimeCache.activeEmotionStyle)
        val result = aiRepository.generateAutoReplyMessage(sender, incomingText, language, emotion)
        result.onSuccess { replyText ->
            try {
                val (action, remoteInput) = actionPair
                val fillInIntent = Intent()
                val bundle = Bundle().apply {
                    putCharSequence(remoteInput.resultKey, replyText)
                }
                RemoteInput.addResultsToIntent(arrayOf(remoteInput), fillInIntent, bundle)
                action.actionIntent.send(applicationContext, 0, fillInIntent)
                Log.d(TAG, "Sent AI Auto-Reply to $sender")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send notification reply action", e)
            }
        }
    }

    companion object {
        private val _isConnected = MutableStateFlow(false)
        val isConnected = _isConnected.asStateFlow()

        private val _recentMessages = MutableStateFlow<List<CapturedMessageNotification>>(emptyList())
        val recentMessages = _recentMessages.asStateFlow()

        val SUPPORTED_MESSAGING_PACKAGES = mapOf(
            "com.whatsapp" to "WhatsApp",
            "com.whatsapp.w4b" to "WhatsApp Business",
            "org.telegram.messenger" to "Telegram",
            "com.google.android.apps.messaging" to "Google Messages",
            "com.android.mms" to "Android Messages",
            "com.samsung.android.messaging" to "Samsung Messages",
            "com.facebook.orca" to "Messenger",
            "org.thoughtcrime.securesms" to "Signal"
        )

        fun isNotificationAccessEnabled(context: Context): Boolean {
            val flat = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            ) ?: return false
            val component = ComponentName(context, SaniNotificationListenerService::class.java).flattenToString()
            return flat.split(':').any { it.equals(component, ignoreCase = true) }
        }
    }
}
