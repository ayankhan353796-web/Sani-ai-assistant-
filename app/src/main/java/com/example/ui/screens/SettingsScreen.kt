package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MarkChatUnread
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.ReplyAll
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.CodeColorOption
import com.example.data.model.EmotionStyle
import com.example.data.model.SaniLanguage
import com.example.data.model.UserProfile
import com.example.data.themes.ThemesCatalog
import com.example.firebase.signOutUser
import com.example.services.PhoneControlAction
import com.example.services.SaniAccessibilityService
import com.example.services.SaniNotificationListenerService
import com.example.ui.components.SaniGlassCard
import com.example.ui.theme.LocalSaniTheme
import com.example.ui.viewmodel.SaniViewModel
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    viewModel: SaniViewModel,
    profile: UserProfile,
    onOpenCreatorHub: () -> Unit,
    onOpenThemesStudio: () -> Unit,
    onOpenLanguageDialog: () -> Unit,
    onOpenAutoReplyDialog: () -> Unit,
    onOpenPhoneControl: () -> Unit
) {
    val theme = LocalSaniTheme.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val geminiStatus by viewModel.geminiStatus.collectAsStateWithLifecycle()
    val isTestingGemini by viewModel.isTestingGemini.collectAsStateWithLifecycle()
    val premiumMessage by viewModel.premiumMessage.collectAsStateWithLifecycle()
    val customKeyOverride by viewModel.customKeyOverride.collectAsStateWithLifecycle()
    val autoReplySettings by viewModel.autoReplySettings.collectAsStateWithLifecycle()
    val messageReadingEnabled by viewModel.messageReadingEnabled.collectAsStateWithLifecycle()
    val recentNotifications by SaniNotificationListenerService.recentMessages.collectAsStateWithLifecycle()

    var accessKeyInput by remember { mutableStateOf("") }
    var customKeyInput by remember(customKeyOverride) { mutableStateOf(customKeyOverride) }

    val accessibilityEnabled = remember(context) {
        SaniAccessibilityService.isServiceEnabledInSettings(context)
    }
    val notificationAccessEnabled = remember(context) {
        SaniNotificationListenerService.isNotificationAccessEnabled(context)
    }

    val micPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.updateProfileField(mapOf("heySaniEnabled" to granted))
        if (!granted) {
            viewModel.showToast("Microphone permission required for Hey SANI.")
        }
    }

    val language = SaniLanguage.fromCode(profile.languageCode)
    val activeTheme = ThemesCatalog.themeById(profile.selectedThemeId)

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 124.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.widthIn(max = 640.dp)
        ) {
            item {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    color = theme.primaryText,
                    fontWeight = FontWeight.Bold
                )
            }

            // 1. PROFILE
            item {
                SettingsSectionHeader("PROFILE")
                Spacer(modifier = Modifier.height(8.dp))
                SaniGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(theme.primaryAccent.copy(alpha = 0.2f))
                        ) {
                            if (profile.photoUrl.isNotBlank()) {
                                AsyncImage(
                                    model = profile.photoUrl,
                                    contentDescription = "Profile photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Person,
                                    contentDescription = "Profile",
                                    tint = theme.primaryAccent,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = profile.displayName,
                                style = MaterialTheme.typography.titleLarge,
                                color = theme.primaryText,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Profile, links & AI memory",
                                style = MaterialTheme.typography.bodyMedium,
                                color = theme.secondaryText
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = theme.primaryAccent.copy(alpha = 0.22f),
                            border = BorderStroke(1.dp, theme.primaryAccent),
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable(onClick = onOpenCreatorHub)
                                .testTag("settings_edit_profile")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = "Edit Profile",
                                    tint = theme.secondaryAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Edit",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = theme.primaryText
                                )
                            }
                        }
                    }
                }
            }

            // 2. PREMIUM
            item {
                SettingsSectionHeader("PREMIUM")
                Spacer(modifier = Modifier.height(8.dp))
                SaniGlassCard(
                    glowColor = theme.secondaryAccent,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Verified,
                            contentDescription = null,
                            tint = if (profile.premiumTier == "verified") theme.glowCyan else theme.secondaryAccent
                        )
                        Text(
                            text = if (profile.premiumTier == "verified") "Verified Pro Subscription"
                            else "Standard Free Plan",
                            style = MaterialTheme.typography.titleMedium,
                            color = theme.primaryText,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = accessKeyInput,
                        onValueChange = { accessKeyInput = it },
                        label = { Text("Enter Access Key") },
                        singleLine = true,
                        colors = saniTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("premium_key_input")
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { viewModel.validatePremiumKey(accessKeyInput) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = theme.secondaryAccent,
                            contentColor = theme.background
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("validate_premium_button")
                    ) {
                        Text("Validate & Save Subscription", fontWeight = FontWeight.Bold)
                    }
                    if (!premiumMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = premiumMessage!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = theme.secondaryText
                        )
                    }
                }
            }

            // 3. AI & VOICE
            item {
                SettingsSectionHeader("AI & VOICE")
                Spacer(modifier = Modifier.height(8.dp))
                SaniGlassCard(modifier = Modifier.fillMaxWidth()) {
                    // 1. Hey SANI
                    SettingsToggleRow(
                        icon = Icons.Filled.Mic,
                        title = "Hey SANI Wake Word",
                        subtitle = if (profile.heySaniEnabled) "Active when SANI is open (mic ready)" else "Disabled",
                        checked = profile.heySaniEnabled,
                        onCheckedChange = { wantEnable ->
                            if (wantEnable) {
                                val granted = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                                if (granted) {
                                    viewModel.updateProfileField(mapOf("heySaniEnabled" to true))
                                } else {
                                    micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            } else {
                                viewModel.updateProfileField(mapOf("heySaniEnabled" to false))
                            }
                        }
                    )

                    SettingsDivider()

                    // 2. Gemini API Key & Connection Test
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Filled.Key, null, tint = theme.glowCyan)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Gemini AI Connection",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = theme.primaryText
                                )
                                Text(
                                    text = geminiStatus,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = theme.glowCyan
                                )
                            }
                        }
                        OutlinedTextField(
                            value = customKeyInput,
                            onValueChange = { customKeyInput = it },
                            label = { Text("Optional Custom Key Override (Device Only)") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            colors = saniTextFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { viewModel.saveCustomKeyOverride(customKeyInput) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = theme.secondaryBackground,
                                    contentColor = theme.primaryText
                                ),
                                border = BorderStroke(1.dp, theme.primaryAccent),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Save Config")
                            }
                            Button(
                                onClick = { viewModel.testGeminiConnection() },
                                enabled = !isTestingGemini,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = theme.primaryAccent,
                                    contentColor = theme.background
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("test_gemini_button")
                            ) {
                                if (isTestingGemini) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = theme.background,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("Test Connection", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    SettingsDivider()

                    // 3. Voice
                    SettingsNavigationRow(
                        icon = Icons.Filled.RecordVoiceOver,
                        title = "Voice",
                        value = if (profile.voiceName == "default") "System Default • Tap Mic Studio"
                        else profile.voiceName,
                        onClick = onOpenThemesStudio
                    )

                    SettingsDivider()

                    // 4. Emotion Style
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Filled.SentimentSatisfied, null, tint = theme.primaryAccent)
                            Text(
                                text = "Emotion Style",
                                style = MaterialTheme.typography.titleMedium,
                                color = theme.primaryText
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            EmotionStyle.entries.forEach { style ->
                                val selected = profile.emotionStyle == style.label
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (selected) theme.primaryAccent else theme.background,
                                    border = BorderStroke(1.dp, theme.primaryAccent.copy(alpha = 0.6f)),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable {
                                            viewModel.updateProfileField(mapOf("emotionStyle" to style.label))
                                        }
                                ) {
                                    Text(
                                        text = style.label,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (selected) theme.background else theme.primaryText,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }

                    SettingsDivider()

                    // 5. Emotion Level (1 to 5)
                    Column {
                        val level = profile.emotionLevel.toInt().coerceIn(1, 5)
                        val levelText = when (level) {
                            1, 2 -> "Calm"
                            3 -> "Normal"
                            else -> "Expressive"
                        }
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Emotion Level",
                                style = MaterialTheme.typography.titleMedium,
                                color = theme.primaryText
                            )
                            Text(
                                text = "$level / 5 • $levelText",
                                style = MaterialTheme.typography.labelLarge,
                                color = theme.secondaryAccent
                            )
                        }
                        Slider(
                            value = level.toFloat(),
                            onValueChange = { raw ->
                                viewModel.updateProfileField(mapOf("emotionLevel" to raw.roundToInt().toLong()))
                            },
                            valueRange = 1f..5f,
                            steps = 3,
                            colors = SliderDefaults.colors(
                                thumbColor = theme.primaryAccent,
                                activeTrackColor = theme.primaryAccent
                            )
                        )
                    }

                    SettingsDivider()

                    // 6. Keep Chatting
                    SettingsToggleRow(
                        icon = Icons.Filled.AutoAwesome,
                        title = "Keep Chatting",
                        subtitle = "Continue voice conversations naturally until stopped",
                        checked = profile.keepChatting,
                        onCheckedChange = {
                            viewModel.updateProfileField(mapOf("keepChatting" to it))
                        }
                    )

                    SettingsDivider()

                    // 7. Language
                    SettingsNavigationRow(
                        icon = Icons.Filled.Language,
                        title = "Language",
                        value = language.displayName,
                        testTag = "settings_language_row",
                        onClick = onOpenLanguageDialog
                    )
                }
            }

            // 4. APPEARANCE
            item {
                SettingsSectionHeader("APPEARANCE")
                Spacer(modifier = Modifier.height(8.dp))
                SaniGlassCard(modifier = Modifier.fillMaxWidth()) {
                    SettingsNavigationRow(
                        icon = Icons.Filled.Palette,
                        title = "Themes Studio",
                        value = "${activeTheme.name} (28 themes)",
                        testTag = "settings_themes_row",
                        onClick = onOpenThemesStudio
                    )
                    SettingsDivider()
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Filled.Code, null, tint = theme.glowCyan)
                            Text(
                                text = "Code Colour",
                                style = MaterialTheme.typography.titleMedium,
                                color = theme.primaryText
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CodeColorOption.entries.forEach { option ->
                                val selected = profile.codeColorId == option.id
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (selected) option.color.copy(alpha = 0.25f) else theme.background,
                                    border = BorderStroke(
                                        width = if (selected) 2.dp else 1.dp,
                                        color = option.color
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable {
                                            viewModel.updateProfileField(mapOf("codeColorId" to option.id))
                                        }
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .background(option.color, CircleShape)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = option.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = theme.primaryText
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. PHONE CONTROL
            item {
                SettingsSectionHeader("PHONE CONTROL")
                Spacer(modifier = Modifier.height(8.dp))
                SaniGlassCard(modifier = Modifier.fillMaxWidth()) {
                    SettingsNavigationRow(
                        icon = Icons.Filled.PhoneAndroid,
                        title = "Accessibility Phone Control",
                        value = if (accessibilityEnabled) "Enabled • Tap to manage" else "Disabled • Tap for setup",
                        onClick = onOpenPhoneControl
                    )
                    SettingsDivider()
                    SettingsNavigationRow(
                        icon = Icons.Filled.Lock,
                        title = "Screen Lock Automation",
                        value = if (accessibilityEnabled) "Tap to lock screen now" else "Requires Accessibility Service",
                        onClick = {
                            val ok = SaniAccessibilityService.perform(PhoneControlAction.LOCK_SCREEN)
                            if (!ok) {
                                context.startActivity(
                                    Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }
                        }
                    )
                    SettingsDivider()
                    SettingsToggleRow(
                        icon = Icons.Filled.MarkChatUnread,
                        title = "Message Reading (WhatsApp & SMS)",
                        subtitle = if (notificationAccessEnabled) {
                            "${recentNotifications.size} recent messages • Access granted"
                        } else {
                            "Grant Notification Access in Android Settings"
                        },
                        checked = messageReadingEnabled && notificationAccessEnabled,
                        onCheckedChange = { enable ->
                            if (!notificationAccessEnabled) {
                                context.startActivity(
                                    Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            } else {
                                viewModel.setMessageReadingEnabled(enable)
                            }
                        }
                    )
                    SettingsDivider()
                    SettingsNavigationRow(
                        icon = Icons.Filled.ReplyAll,
                        title = "AI Auto-Reply",
                        value = if (autoReplySettings.enabled) "ON (${autoReplySettings.allowedPackages.size} apps)" else "OFF by default",
                        testTag = "settings_auto_reply_row",
                        onClick = onOpenAutoReplyDialog
                    )
                }
            }

            // 6. ACCOUNT SIGN OUT
            item {
                Button(
                    onClick = {
                        signOutUser(credentialManager = credentialManager, scope = scope)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = theme.secondaryBackground,
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("sign_out_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign Out", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    val theme = LocalSaniTheme.current
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = theme.secondaryAccent,
        letterSpacing = 1.6.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun SettingsDivider() {
    val theme = LocalSaniTheme.current
    HorizontalDivider(
        color = theme.primaryAccent.copy(alpha = 0.2f),
        modifier = Modifier.padding(vertical = 12.dp)
    )
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val theme = LocalSaniTheme.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(icon, contentDescription = null, tint = theme.primaryAccent)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = theme.primaryText)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = theme.secondaryText)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = theme.background,
                checkedTrackColor = theme.primaryAccent
            )
        )
    }
}

@Composable
private fun SettingsNavigationRow(
    icon: ImageVector,
    title: String,
    value: String,
    testTag: String? = null,
    onClick: () -> Unit
) {
    val theme = LocalSaniTheme.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = theme.primaryAccent)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = theme.primaryText)
            Text(value, style = MaterialTheme.typography.bodyMedium, color = theme.secondaryText)
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = theme.secondaryText
        )
    }
}

@Composable
fun saniTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = LocalSaniTheme.current.primaryText,
    unfocusedTextColor = LocalSaniTheme.current.primaryText,
    focusedBorderColor = LocalSaniTheme.current.primaryAccent,
    unfocusedBorderColor = LocalSaniTheme.current.primaryAccent.copy(alpha = 0.4f),
    focusedLabelColor = LocalSaniTheme.current.secondaryAccent,
    unfocusedLabelColor = LocalSaniTheme.current.secondaryText,
    cursorColor = LocalSaniTheme.current.primaryAccent
)
