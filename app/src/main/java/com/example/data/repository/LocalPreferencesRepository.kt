package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AutoReplySettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

private val Context.saniDataStore: DataStore<Preferences> by preferencesDataStore(name = "sani_local_prefs")

object AutoReplyRuntimeCache {
    private val _settings = MutableStateFlow(AutoReplySettings())
    val settings = _settings.asStateFlow()

    @Volatile
    var activeLanguageCode: String = "hi-Latn"

    @Volatile
    var activeEmotionStyle: String = "Friendly"

    @Volatile
    var messageReadingEnabled: Boolean = true

    fun update(newSettings: AutoReplySettings) {
        _settings.value = newSettings
    }
}

class LocalPreferencesRepository(private val context: Context) {

    private object Keys {
        val AUTO_REPLY_ENABLED = booleanPreferencesKey("auto_reply_enabled")
        val AUTO_REPLY_APPS = stringSetPreferencesKey("auto_reply_apps")
        val AUTO_REPLY_ALL = booleanPreferencesKey("auto_reply_all")
        val AUTO_REPLY_GROUPS = booleanPreferencesKey("auto_reply_groups")
        val AUTO_REPLY_CONTACTS = stringPreferencesKey("auto_reply_contacts")
        val MESSAGE_READING_ENABLED = booleanPreferencesKey("message_reading_enabled")
        val CUSTOM_KEY_NOTE = stringPreferencesKey("custom_key_note")
        val VOICE_ANIMATION_STYLE = stringPreferencesKey("voice_animation_style")
    }

    val autoReplySettingsFlow: Flow<AutoReplySettings> = context.saniDataStore.data.map { prefs ->
        val settings = AutoReplySettings(
            enabled = prefs[Keys.AUTO_REPLY_ENABLED] ?: false,
            allowedPackages = prefs[Keys.AUTO_REPLY_APPS] ?: emptySet(),
            replyToAll = prefs[Keys.AUTO_REPLY_ALL] ?: false,
            replyInGroups = prefs[Keys.AUTO_REPLY_GROUPS] ?: false,
            contactAllowlistCsv = prefs[Keys.AUTO_REPLY_CONTACTS] ?: ""
        )
        AutoReplyRuntimeCache.update(settings)
        settings
    }

    val messageReadingEnabledFlow: Flow<Boolean> = context.saniDataStore.data.map { prefs ->
        val enabled = prefs[Keys.MESSAGE_READING_ENABLED] ?: true
        AutoReplyRuntimeCache.messageReadingEnabled = enabled
        enabled
    }

    val voiceAnimationStyleFlow: Flow<String> = context.saniDataStore.data.map { prefs ->
        prefs[Keys.VOICE_ANIMATION_STYLE] ?: "Aurora Pulse"
    }

    val customKeyOverrideFlow: Flow<String> = context.saniDataStore.data.map { prefs ->
        prefs[Keys.CUSTOM_KEY_NOTE] ?: ""
    }

    suspend fun saveAutoReplySettings(settings: AutoReplySettings) {
        AutoReplyRuntimeCache.update(settings)
        context.saniDataStore.edit { prefs ->
            prefs[Keys.AUTO_REPLY_ENABLED] = settings.enabled
            prefs[Keys.AUTO_REPLY_APPS] = settings.allowedPackages
            prefs[Keys.AUTO_REPLY_ALL] = settings.replyToAll
            prefs[Keys.AUTO_REPLY_GROUPS] = settings.replyInGroups
            prefs[Keys.AUTO_REPLY_CONTACTS] = settings.contactAllowlistCsv
        }
    }

    suspend fun setMessageReadingEnabled(enabled: Boolean) {
        AutoReplyRuntimeCache.messageReadingEnabled = enabled
        context.saniDataStore.edit { prefs ->
            prefs[Keys.MESSAGE_READING_ENABLED] = enabled
        }
    }

    suspend fun setVoiceAnimationStyle(style: String) {
        context.saniDataStore.edit { prefs ->
            prefs[Keys.VOICE_ANIMATION_STYLE] = style
        }
    }

    suspend fun setCustomKeyOverride(value: String) {
        context.saniDataStore.edit { prefs ->
            prefs[Keys.CUSTOM_KEY_NOTE] = value.trim()
        }
    }
}
