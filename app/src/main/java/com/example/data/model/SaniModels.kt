package com.example.data.model

import androidx.annotation.Keep
import androidx.compose.ui.graphics.Color
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Creator",
    val bio: String = "Exploring the universe with SANI AI",
    val photoUrl: String = "",
    val youtubeLink: String = "https://www.youtube.com",
    val instagramLink: String = "https://www.instagram.com",
    val songLink: String = "https://open.spotify.com",
    val selectedThemeId: String = "solar_flare",
    val selectedAvatarId: String = "original_sani",
    val selectedOrbId: String = "orb_cyber_purple",
    val codeColorId: String = "cyan",
    val languageCode: String = "hi-Latn",
    val emotionStyle: String = "Friendly",
    val emotionLevel: Long = 3L,
    val keepChatting: Boolean = false,
    val heySaniEnabled: Boolean = false,
    val voiceName: String = "default",
    val voiceSensitivity: Long = 5L,
    val micStyleId: String = "neon_ring",
    val premiumTier: String = "free",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        val raw = mapOf<String, Any?>(
            "userId" to userId,
            "displayName" to displayName.take(60).ifBlank { "Creator" },
            "bio" to bio.take(240),
            "photoUrl" to photoUrl.take(500),
            "youtubeLink" to youtubeLink.take(300),
            "instagramLink" to instagramLink.take(300),
            "songLink" to songLink.take(300),
            "selectedThemeId" to selectedThemeId,
            "selectedAvatarId" to selectedAvatarId,
            "selectedOrbId" to selectedOrbId,
            "codeColorId" to codeColorId,
            "languageCode" to languageCode,
            "emotionStyle" to emotionStyle,
            "emotionLevel" to emotionLevel.coerceIn(1L, 5L),
            "keepChatting" to keepChatting,
            "heySaniEnabled" to heySaniEnabled,
            "voiceName" to voiceName.take(80),
            "voiceSensitivity" to voiceSensitivity.coerceIn(1L, 10L),
            "micStyleId" to micStyleId,
            "premiumTier" to premiumTier,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }
}

@Keep
@IgnoreExtraProperties
data class UserMemory(
    @DocumentId val id: String = "",
    val userId: String = "",
    val content: String = "",
    val category: String = "Personal",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ChatConversation(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "New Chat",
    val lastMessagePreview: String = "",
    val languageCode: String = "hi-Latn",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val role: String = "user",
    val text: String = "",
    val hasAttachment: Boolean = false,
    val attachmentLabel: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

enum class SaniLanguage(
    val code: String,
    val displayName: String,
    val nativeSubtitle: String,
    val ttsTag: String,
    val aiInstruction: String
) {
    HINGLISH(
        code = "hi-Latn",
        displayName = "Hindi / Hinglish",
        nativeSubtitle = "Default • Natural Hindi + English mix",
        ttsTag = "hi-IN",
        aiInstruction = "Respond naturally in friendly Hinglish (Hindi written smoothly in Latin/Devanagari as appropriate, mixed with clear English technical terms). Use warm Indian conversational phrasing."
    ),
    ENGLISH(
        code = "en",
        displayName = "English",
        nativeSubtitle = "Global English",
        ttsTag = "en-US",
        aiInstruction = "Respond in crisp, expressive, natural English."
    ),
    URDU(
        code = "ur",
        displayName = "Urdu (Pakistani)",
        nativeSubtitle = "اردو • Polite & expressive",
        ttsTag = "ur-PK",
        aiInstruction = "Respond in polite, natural Pakistani Urdu (Urdu script or clear Roman Urdu when helpful)."
    ),
    BANGLA(
        code = "bn",
        displayName = "Bangla (Bengali)",
        nativeSubtitle = "বাংলা • Warm & articulate",
        ttsTag = "bn-IN",
        aiInstruction = "Respond in warm, natural Bengali (Bangla script)."
    ),
    PUNJABI(
        code = "pa",
        displayName = "Punjabi",
        nativeSubtitle = "ਪੰਜਾਬੀ • Energetic & friendly",
        ttsTag = "pa-IN",
        aiInstruction = "Respond in friendly, natural Punjabi (Gurmukhi or clear Punjabi phrasing)."
    ),
    TAMIL(
        code = "ta",
        displayName = "Tamil",
        nativeSubtitle = "தமிழ் • Clear & helpful",
        ttsTag = "ta-IN",
        aiInstruction = "Respond in natural, helpful Tamil."
    );

    companion object {
        fun fromCode(code: String): SaniLanguage =
            entries.firstOrNull { it.code == code } ?: HINGLISH
    }
}

enum class EmotionStyle(val label: String, val tagline: String, val systemDirective: String) {
    GIRLFRIEND("Girlfriend", "Warm, caring & affectionate", "Be deeply affectionate, caring, playful, and emotionally attentive like a supportive partner."),
    AGENT("Agent", "Sharp, tactical & mission-focused", "Be decisive, razor-sharp, structured, and tactical like an elite intelligence specialist."),
    FRIENDLY("Friendly", "Cheerful, empathetic & upbeat", "Be warm, upbeat, approachable, and encouraging like a close best friend."),
    PROFESSIONAL("Professional", "Polished, concise & executive", "Be polished, clear, objective, and executive-ready."),
    ASSISTANT("Assistant", "Helpful, balanced & proactive", "Be a helpful, intelligent, proactive personal AI assistant.");

    companion object {
        fun fromLabel(label: String): EmotionStyle =
            entries.firstOrNull { it.label == label } ?: FRIENDLY
    }
}

enum class CodeColorOption(val id: String, val label: String, val color: Color) {
    CYAN("cyan", "Cyan", Color(0xFF35DDEB)),
    PURPLE("purple", "Purple", Color(0xFF913CFF)),
    ORANGE("orange", "Orange", Color(0xFFFF852A)),
    GREEN("green", "Green", Color(0xFF34D399)),
    BLUE("blue", "Blue", Color(0xFF60A5FA));

    companion object {
        fun fromId(id: String): CodeColorOption =
            entries.firstOrNull { it.id == id } ?: CYAN
    }
}

data class SaniThemeConfig(
    val id: String,
    val name: String,
    val category: String,
    val background: Color,
    val secondaryBackground: Color,
    val primaryAccent: Color,
    val secondaryAccent: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val glowCyan: Color,
    val glowPurple: Color
)

data class AvatarOption(
    val id: String,
    val name: String,
    val subtitle: String,
    val kind: AvatarKind,
    val primaryColor: Color,
    val secondaryColor: Color
)

enum class AvatarKind {
    ORIGINAL_SANI,
    PARTICLE_SPHERE,
    LOGO_ORB
}

data class MicStyleOption(
    val id: String,
    val name: String,
    val description: String
)

data class AutoReplyApp(
    val packageName: String,
    val displayName: String
)

data class AutoReplySettings(
    val enabled: Boolean = false,
    val allowedPackages: Set<String> = emptySet(),
    val replyToAll: Boolean = false,
    val replyInGroups: Boolean = false,
    val contactAllowlistCsv: String = ""
)
