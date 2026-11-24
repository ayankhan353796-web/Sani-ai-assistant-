package com.example.ui.screens

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.UserProfile
import com.example.data.themes.ThemesCatalog
import com.example.ui.components.LogoOrbCanvas
import com.example.ui.components.ParticleSphereCanvas
import com.example.ui.components.SaniAvatarDisplay
import com.example.ui.components.SaniGlassCard
import com.example.ui.theme.LocalSaniTheme
import com.example.ui.viewmodel.SaniViewModel
import kotlin.math.roundToInt

@Composable
fun ThemesStudioScreen(
    viewModel: SaniViewModel,
    profile: UserProfile,
    onBack: () -> Unit
) {
    val theme = LocalSaniTheme.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Themes", "Avatar", "Mic")

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 660.dp)
                .padding(horizontal = 16.dp)
                .padding(bottom = 96.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 8.dp)
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("studio_back_button")) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = theme.primaryText
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Themes Studio",
                        style = MaterialTheme.typography.headlineMedium,
                        color = theme.primaryText,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Choose your theme, SANI avatar or voice button",
                        style = MaterialTheme.typography.bodyMedium,
                        color = theme.secondaryText
                    )
                }
            }

            // 3 Studio Tabs
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                tabs.forEachIndexed { idx, label ->
                    val active = selectedTab == idx
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = if (active) theme.primaryAccent else theme.secondaryBackground,
                        border = BorderStroke(1.dp, theme.primaryAccent.copy(alpha = 0.55f)),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { selectedTab = idx }
                            .testTag("studio_tab_${label.lowercase()}")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(vertical = 10.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (active) theme.background else theme.primaryText,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            when (selectedTab) {
                0 -> ThemesGridTab(
                    selectedThemeId = profile.selectedThemeId,
                    onSelectTheme = { themeId ->
                        viewModel.updateProfileField(mapOf("selectedThemeId" to themeId))
                    }
                )
                1 -> AvatarStudioTab(
                    profile = profile,
                    onSelectAvatar = { avatarId ->
                        viewModel.updateProfileField(mapOf("selectedAvatarId" to avatarId))
                    },
                    onSelectOrb = { orbId ->
                        viewModel.updateProfileField(
                            mapOf(
                                "selectedAvatarId" to orbId,
                                "selectedOrbId" to orbId
                            )
                        )
                    }
                )
                2 -> MicStudioTab(
                    viewModel = viewModel,
                    profile = profile
                )
            }
        }
    }
}

@Composable
private fun ThemesGridTab(
    selectedThemeId: String,
    onSelectTheme: (String) -> Unit
) {
    val theme = LocalSaniTheme.current
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(ThemesCatalog.allThemes, key = { it.id }) { item ->
            val selected = item.id == selectedThemeId
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = item.secondaryBackground,
                border = BorderStroke(
                    width = if (selected) 2.5.dp else 1.dp,
                    color = if (selected) item.glowCyan else item.primaryAccent.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onSelectTheme(item.id) }
                    .testTag("theme_card_${item.id}")
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Live mini preview banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(item.background, item.secondaryBackground, item.primaryAccent.copy(alpha = 0.5f))
                                )
                            )
                            .border(1.dp, item.secondaryAccent.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(horizontal = 10.dp)
                        ) {
                            Box(Modifier.size(16.dp).background(item.primaryAccent, CircleShape))
                            Box(Modifier.size(16.dp).background(item.secondaryAccent, CircleShape))
                            Box(Modifier.size(16.dp).background(item.glowCyan, CircleShape))
                        }
                        if (selected) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                                    .size(22.dp)
                                    .background(item.glowCyan, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Selected",
                                    tint = item.background,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = item.primaryText,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (selected) "Active • ${item.category}" else item.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) theme.glowCyan else item.secondaryText
                    )
                }
            }
        }
    }
}

@Composable
private fun AvatarStudioTab(
    profile: UserProfile,
    onSelectAvatar: (String) -> Unit,
    onSelectOrb: (String) -> Unit
) {
    val theme = LocalSaniTheme.current
    LazyColumn(
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Original SANI
        item {
            Text(
                text = "ORIGINAL SANI",
                style = MaterialTheme.typography.labelLarge,
                color = theme.secondaryAccent,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            val isOriginalSelected = profile.selectedAvatarId == ThemesCatalog.originalSaniAvatar.id
            SaniGlassCard(
                glowColor = if (isOriginalSelected) theme.glowCyan else theme.primaryAccent,
                onClick = { onSelectAvatar(ThemesCatalog.originalSaniAvatar.id) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("avatar_original_sani")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    SaniAvatarDisplay(
                        avatarOption = ThemesCatalog.originalSaniAvatar,
                        emotionLabel = profile.emotionStyle,
                        isListening = false,
                        isSpeaking = false,
                        audioLevel = 0f,
                        size = 88.dp
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Original SANI",
                            style = MaterialTheme.typography.titleLarge,
                            color = theme.primaryText,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Animated female character with blinking, lip-sync & emotional aura (Default)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = theme.secondaryText
                        )
                    }
                    if (isOriginalSelected) {
                        Icon(Icons.Filled.Check, contentDescription = "Active", tint = theme.glowCyan)
                    }
                }
            }
        }

        // Particle Spheres
        item {
            Text(
                text = "PARTICLE SPHERES",
                style = MaterialTheme.typography.labelLarge,
                color = theme.secondaryAccent,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ThemesCatalog.particleSpheres.chunked(2).forEach { pair ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        pair.forEach { sphere ->
                            val selected = profile.selectedAvatarId == sphere.id
                            SaniGlassCard(
                                glowColor = if (selected) theme.glowCyan else sphere.primaryColor,
                                onClick = { onSelectAvatar(sphere.id) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("avatar_${sphere.id}")
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    ParticleSphereCanvas(
                                        primaryColor = sphere.primaryColor,
                                        secondaryColor = sphere.secondaryColor,
                                        rotation = 45f,
                                        boost = 0.25f,
                                        modifier = Modifier.size(72.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = sphere.name,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = theme.primaryText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (selected) "Selected" else sphere.subtitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (selected) theme.glowCyan else theme.secondaryText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 12 SANI Logo Orbs
        item {
            Text(
                text = "SANI LOGO ORBS (12 UNIQUE ORBS)",
                style = MaterialTheme.typography.labelLarge,
                color = theme.secondaryAccent,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ThemesCatalog.logoOrbs.chunked(3).forEach { rowOrbs ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        rowOrbs.forEach { orb ->
                            val selected = profile.selectedAvatarId == orb.id || profile.selectedOrbId == orb.id
                            SaniGlassCard(
                                cornerRadius = 18.dp,
                                glowColor = if (selected) theme.glowCyan else orb.primaryColor,
                                onClick = { onSelectOrb(orb.id) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("orb_${orb.id}")
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    LogoOrbCanvas(
                                        primaryColor = orb.primaryColor,
                                        secondaryColor = orb.secondaryColor,
                                        rotation = 30f,
                                        boost = 0.2f,
                                        modifier = Modifier.size(62.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = orb.name.removeSuffix(" Orb"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (selected) theme.glowCyan else theme.primaryText,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MicStudioTab(
    viewModel: SaniViewModel,
    profile: UserProfile
) {
    val theme = LocalSaniTheme.current
    val availableVoices by viewModel.availableVoices.collectAsStateWithLifecycle()
    val currentAnimationStyle by viewModel.voiceAnimationStyle.collectAsStateWithLifecycle()

    var selectedVoiceName by remember(profile.voiceName) { mutableStateOf(profile.voiceName) }
    var selectedMicStyle by remember(profile.micStyleId) { mutableStateOf(profile.micStyleId) }
    var sensitivity by remember(profile.voiceSensitivity) { mutableFloatStateOf(profile.voiceSensitivity.toFloat()) }
    var animStyle by remember(currentAnimationStyle) { mutableStateOf(currentAnimationStyle) }

    LazyColumn(
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            SaniGlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Voice Selection & Preview",
                    style = MaterialTheme.typography.titleMedium,
                    color = theme.primaryText,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                val voiceChoices = listOf("default") + availableVoices.take(8).map { it.name }
                voiceChoices.forEach { vName ->
                    val selected = selectedVoiceName == vName
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) theme.primaryAccent.copy(alpha = 0.18f) else theme.background.copy(alpha = 0.4f))
                            .clickable { selectedVoiceName = vName }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = if (vName == "default") "SANI System Default" else vName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = theme.primaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.previewVoice(vName) },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = "Preview Voice",
                                tint = theme.glowCyan
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }

        item {
            SaniGlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Microphone Button Style",
                    style = MaterialTheme.typography.titleMedium,
                    color = theme.primaryText,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                ThemesCatalog.micStyles.forEach { mic ->
                    val selected = selectedMicStyle == mic.id
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) theme.primaryAccent.copy(alpha = 0.2f) else theme.background.copy(alpha = 0.3f))
                            .clickable { selectedMicStyle = mic.id }
                            .padding(10.dp)
                    ) {
                        Icon(Icons.Filled.Mic, null, tint = if (selected) theme.glowCyan else theme.secondaryAccent)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(mic.name, style = MaterialTheme.typography.labelLarge, color = theme.primaryText)
                            Text(mic.description, style = MaterialTheme.typography.bodyMedium, color = theme.secondaryText)
                        }
                        if (selected) Icon(Icons.Filled.Check, null, tint = theme.glowCyan)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }

        item {
            SaniGlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Voice Sensitivity", style = MaterialTheme.typography.titleMedium, color = theme.primaryText)
                    Text("${sensitivity.roundToInt()} / 10", style = MaterialTheme.typography.labelLarge, color = theme.secondaryAccent)
                }
                Slider(
                    value = sensitivity,
                    onValueChange = { sensitivity = it },
                    valueRange = 1f..10f,
                    steps = 8,
                    colors = SliderDefaults.colors(
                        thumbColor = theme.glowCyan,
                        activeTrackColor = theme.glowCyan
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Voice Animation", style = MaterialTheme.typography.titleMedium, color = theme.primaryText)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Aurora Pulse", "Sonar Wave", "Particle Orbit").forEach { style ->
                        val selected = animStyle == style
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (selected) theme.glowCyan.copy(alpha = 0.22f) else theme.background,
                            border = BorderStroke(1.dp, if (selected) theme.glowCyan else theme.primaryAccent.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { animStyle = style }
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Icon(Icons.Filled.GraphicEq, null, tint = theme.glowCyan, modifier = Modifier.size(18.dp))
                                Text(style, style = MaterialTheme.typography.labelSmall, color = theme.primaryText, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = {
                    viewModel.updateProfileField(
                        mapOf(
                            "voiceName" to selectedVoiceName,
                            "micStyleId" to selectedMicStyle,
                            "voiceSensitivity" to sensitivity.roundToInt().toLong()
                        )
                    )
                    viewModel.setVoiceAnimationStyle(animStyle)
                    viewModel.showToast("Mic & voice studio settings saved")
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = theme.primaryAccent,
                    contentColor = theme.background
                ),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_mic_studio_button")
            ) {
                Text("Save Mic Settings", fontWeight = FontWeight.Bold)
            }
        }
    }
}
