package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.UserProfile
import com.example.data.themes.ThemesCatalog
import com.example.ui.components.SaniAvatarDisplay
import com.example.ui.components.SaniGlassCard
import com.example.ui.theme.LocalSaniTheme
import com.example.ui.viewmodel.SaniViewModel
import java.util.Calendar

@Composable
fun HomeScreen(
    viewModel: SaniViewModel,
    profile: UserProfile,
    onOpenChat: (String?) -> Unit,
    onOpenCamera: () -> Unit,
    onOpenThemesStudio: () -> Unit,
    onOpenCreatorHub: () -> Unit,
    onOpenDocuments: () -> Unit,
    onOpenPhoneControl: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val theme = LocalSaniTheme.current
    val context = LocalContext.current

    val isListening by viewModel.isListening.collectAsStateWithLifecycle()
    val isSpeaking by viewModel.isSpeaking.collectAsStateWithLifecycle()
    val audioLevel by viewModel.audioLevel.collectAsStateWithLifecycle()
    val partialTranscript by viewModel.partialTranscript.collectAsStateWithLifecycle()
    val homeVoiceTranscript by viewModel.homeVoiceTranscript.collectAsStateWithLifecycle()
    val homeVoiceReply by viewModel.homeVoiceReply.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val locationResult by viewModel.locationResult.collectAsStateWithLifecycle()
    val locationLoading by viewModel.locationLoading.collectAsStateWithLifecycle()
    val locationError by viewModel.locationError.collectAsStateWithLifecycle()

    var homePromptInput by remember { mutableStateOf("") }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.startHomeVoiceAssistant()
        } else {
            viewModel.showToast("Microphone permission is needed for Tap to Talk.")
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            viewModel.fetchCurrentLocation()
        } else {
            viewModel.showToast("Location permission denied. Cannot read GPS coordinates.")
        }
    }

    val triggerVoice = {
        if (isListening || isSpeaking) {
            viewModel.stopVoiceInputOrSpeech()
        } else {
            val hasMic = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (hasMic) {
                viewModel.startHomeVoiceAssistant()
            } else {
                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    val avatarOption = remember(profile.selectedAvatarId) {
        ThemesCatalog.avatarById(profile.selectedAvatarId)
    }

    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..20 -> "Good evening"
            else -> "Good night"
        }
    }

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 124.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier.widthIn(max = 620.dp)
        ) {
            // 1. Header
            item {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "SANI",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 2.sp
                                ),
                                color = theme.primaryText
                            )
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(theme.glowCyan, CircleShape)
                            )
                        }
                        Text(
                            text = "PERSONAL AI • LIVE",
                            style = MaterialTheme.typography.labelSmall,
                            color = theme.secondaryAccent,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(
                            shape = CircleShape,
                            color = theme.secondaryBackground.copy(alpha = 0.85f),
                            border = BorderStroke(1.dp, theme.primaryAccent.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("home_theme_button")
                                .clip(CircleShape)
                                .clickable(onClick = onOpenThemesStudio)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Palette,
                                    contentDescription = "Themes Studio",
                                    tint = theme.secondaryAccent
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = theme.secondaryBackground.copy(alpha = 0.85f),
                            border = BorderStroke(1.dp, theme.primaryAccent.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("home_profile_button")
                                .clip(CircleShape)
                                .clickable(onClick = onOpenCreatorHub)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (profile.photoUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = profile.photoUrl,
                                        contentDescription = "User Profile",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.Person,
                                        contentDescription = "User Profile",
                                        tint = theme.primaryText
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Greeting + Avatar + TAP TO TALK
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "$greeting, ${profile.displayName}",
                        style = MaterialTheme.typography.titleLarge,
                        color = theme.primaryText,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Ready when you are",
                        style = MaterialTheme.typography.bodyMedium,
                        color = theme.secondaryText
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(onClick = triggerVoice)
                            .testTag("home_avatar_tap")
                    ) {
                        SaniAvatarDisplay(
                            avatarOption = avatarOption,
                            emotionLabel = profile.emotionStyle,
                            isListening = isListening,
                            isSpeaking = isSpeaking,
                            audioLevel = audioLevel,
                            size = 220.dp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = if (isListening || isSpeaking) theme.glowCyan.copy(alpha = 0.18f)
                        else theme.primaryAccent.copy(alpha = 0.22f),
                        border = BorderStroke(
                            1.5.dp,
                            Brush.horizontalGradient(
                                listOf(theme.primaryAccent, theme.secondaryAccent, theme.glowCyan)
                            )
                        ),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(28.dp))
                            .clickable(onClick = triggerVoice)
                            .testTag("tap_to_talk_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Icon(
                                imageVector = if (isListening || isSpeaking) Icons.Filled.Stop else Icons.Filled.Mic,
                                contentDescription = "Tap to talk",
                                tint = if (isListening) theme.glowCyan else theme.secondaryAccent
                            )
                            Text(
                                text = when {
                                    isListening -> "LISTENING... TAP TO STOP"
                                    isSpeaking -> "SPEAKING... TAP TO STOP"
                                    isGenerating -> "THINKING..."
                                    else -> "TAP TO TALK"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                color = theme.primaryText,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            )
                        }
                    }

                    val liveText = when {
                        isListening && partialTranscript.isNotBlank() -> "\"$partialTranscript\""
                        !homeVoiceReply.isNullOrBlank() -> homeVoiceReply
                        !homeVoiceTranscript.isNullOrBlank() -> "You: $homeVoiceTranscript"
                        else -> null
                    }
                    if (!liveText.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        SaniGlassCard(
                            glowColor = theme.glowCyan,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (!homeVoiceTranscript.isNullOrBlank()) {
                                Text(
                                    text = "You: $homeVoiceTranscript",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = theme.secondaryAccent
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                            Text(
                                text = liveText!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = theme.primaryText
                            )
                        }
                    }
                }
            }

            // 3. ASK SANI (Music, Study, Journal)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionTitle("ASK SANI")
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AskSaniChip(
                            label = "Music",
                            icon = Icons.Filled.MusicNote,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                onOpenChat("Suggest a mood-boosting music playlist and lyrics vibe for me right now.")
                            }
                        )
                        AskSaniChip(
                            label = "Study",
                            icon = Icons.Filled.School,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                onOpenChat("Help me study! Ask me what topic I want to master today and make a clear study plan.")
                            }
                        )
                        AskSaniChip(
                            label = "Journal",
                            icon = Icons.Filled.Book,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                onOpenChat("Guide me through a thoughtful daily reflection and gratitude journal entry.")
                            }
                        )
                    }
                }
            }

            // 4. QUICK ACTIONS (6 cards, 2 columns)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionTitle("QUICK ACTIONS")
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        QuickActionCard(
                            title = "Documents",
                            subtitle = "Resume • Biodata • Letters",
                            icon = Icons.Filled.Description,
                            accent = theme.primaryAccent,
                            modifier = Modifier.weight(1f),
                            testTag = "qa_documents",
                            onClick = onOpenDocuments
                        )
                        QuickActionCard(
                            title = "Coding",
                            subtitle = "Websites • Apps • Code",
                            icon = Icons.Filled.Code,
                            accent = theme.glowCyan,
                            modifier = Modifier.weight(1f),
                            testTag = "qa_coding",
                            onClick = {
                                onOpenChat("Let's build something! Tell me what app, website, or code you want me to write.")
                            }
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        QuickActionCard(
                            title = "Live Vision",
                            subtitle = "Camera Q&A • Visual AI",
                            icon = Icons.Filled.CameraAlt,
                            accent = theme.glowPurple,
                            modifier = Modifier.weight(1f),
                            testTag = "qa_live_vision",
                            onClick = onOpenCamera
                        )
                        QuickActionCard(
                            title = "Phone Control",
                            subtitle = "Voice commands • Actions",
                            icon = Icons.Filled.PhoneAndroid,
                            accent = theme.secondaryAccent,
                            modifier = Modifier.weight(1f),
                            testTag = "qa_phone_control",
                            onClick = onOpenPhoneControl
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        QuickActionCard(
                            title = "Themes",
                            subtitle = "28 premium themes",
                            icon = Icons.Filled.Palette,
                            accent = theme.primaryAccent,
                            modifier = Modifier.weight(1f),
                            testTag = "qa_themes",
                            onClick = onOpenThemesStudio
                        )
                        QuickActionCard(
                            title = "Creator Hub",
                            subtitle = "Profile • Links • Memory",
                            icon = Icons.Filled.Hub,
                            accent = theme.glowCyan,
                            modifier = Modifier.weight(1f),
                            testTag = "qa_creator_hub",
                            onClick = onOpenCreatorHub
                        )
                    }
                }
            }

            // 5. YOUR LINKS
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SectionTitle("YOUR LINKS")
                        Text(
                            text = "Edit in Creator Hub",
                            style = MaterialTheme.typography.labelSmall,
                            color = theme.secondaryAccent,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(onClick = onOpenCreatorHub)
                                .padding(4.dp)
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        LinkPill(
                            label = "YouTube",
                            icon = Icons.Filled.PlayCircle,
                            url = profile.youtubeLink,
                            modifier = Modifier.weight(1f),
                            onClick = { openExternalUrl(context, profile.youtubeLink) }
                        )
                        LinkPill(
                            label = "Instagram",
                            icon = Icons.Filled.AutoAwesome,
                            url = profile.instagramLink,
                            modifier = Modifier.weight(1f),
                            onClick = { openExternalUrl(context, profile.instagramLink) }
                        )
                        LinkPill(
                            label = "Song",
                            icon = Icons.Filled.MusicNote,
                            url = profile.songLink,
                            modifier = Modifier.weight(1f),
                            onClick = { openExternalUrl(context, profile.songLink) }
                        )
                    }
                }
            }

            // 6. LOCATION
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionTitle("LOCATION")
                    SaniGlassCard(
                        glowColor = theme.glowCyan,
                        onClick = {
                            val fine = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                            val coarse = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                            if (fine || coarse) {
                                viewModel.fetchCurrentLocation()
                            } else {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_COARSE_LOCATION,
                                        Manifest.permission.ACCESS_FINE_LOCATION
                                    )
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_location_card")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(theme.glowCyan.copy(alpha = 0.16f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.LocationOn,
                                        contentDescription = "Live Location",
                                        tint = theme.glowCyan
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Live Location • Where am I?",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = theme.primaryText
                                    )
                                    Text(
                                        text = when {
                                            locationLoading -> "Fetching GPS coordinates..."
                                            locationResult != null -> locationResult!!.addressLabel
                                            !locationError.isNullOrBlank() -> locationError!!
                                            else -> "Tap to detect current location & open Maps"
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = theme.secondaryText,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            if (locationLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = theme.glowCyan,
                                    strokeWidth = 2.dp
                                )
                            } else if (locationResult != null) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = theme.primaryAccent.copy(alpha = 0.22f),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { viewModel.openLocationInMaps() }
                                ) {
                                    Text(
                                        text = "Maps",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = theme.secondaryAccent,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 7. HOME CHAT INPUT ("Ask SANI anything...")
            item {
                SaniGlassCard(
                    cornerRadius = 26.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = "Sparkle",
                            tint = theme.secondaryAccent,
                            modifier = Modifier
                                .size(22.dp)
                                .clickable { onOpenChat("Surprise me with a useful idea today!") }
                        )
                        Box(modifier = Modifier.weight(1f)) {
                            if (homePromptInput.isEmpty()) {
                                Text(
                                    text = "Ask SANI anything...",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = theme.secondaryText.copy(alpha = 0.7f)
                                )
                            }
                            BasicTextField(
                                value = homePromptInput,
                                onValueChange = { homePromptInput = it },
                                textStyle = MaterialTheme.typography.bodyLarge.copy(color = theme.primaryText),
                                cursorBrush = SolidColor(theme.primaryAccent),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("home_chat_input")
                            )
                        }
                        IconButton(
                            onClick = triggerVoice,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Mic,
                                contentDescription = "Voice input",
                                tint = theme.glowCyan
                            )
                        }
                        IconButton(
                            onClick = {
                                val q = homePromptInput.trim()
                                homePromptInput = ""
                                onOpenChat(q.ifEmpty { null })
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .background(theme.primaryAccent, CircleShape)
                                .testTag("home_chat_send")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send to SANI",
                                tint = theme.background,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
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
private fun AskSaniChip(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val theme = LocalSaniTheme.current
    SaniGlassCard(
        cornerRadius = 18.dp,
        onClick = onClick,
        modifier = modifier.minimumInteractiveComponentSize()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = theme.primaryAccent,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = theme.primaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val theme = LocalSaniTheme.current
    SaniGlassCard(
        cornerRadius = 22.dp,
        glowColor = accent,
        onClick = onClick,
        modifier = modifier
            .height(114.dp)
            .testTag(testTag)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .background(accent.copy(alpha = 0.18f), RoundedCornerShape(10.dp))
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = accent,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = theme.primaryText,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = theme.secondaryText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun LinkPill(
    label: String,
    icon: ImageVector,
    url: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val theme = LocalSaniTheme.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = theme.secondaryBackground.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, theme.secondaryAccent.copy(alpha = 0.45f)),
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = theme.secondaryAccent,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = theme.primaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun openExternalUrl(context: android.content.Context, rawUrl: String) {
    val normalized = if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) {
        rawUrl
    } else {
        "https://$rawUrl"
    }
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(normalized)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
    }
}
