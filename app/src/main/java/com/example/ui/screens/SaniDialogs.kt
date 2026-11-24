package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AutoReplySettings
import com.example.data.model.SaniLanguage
import com.example.data.model.UserProfile
import com.example.data.themes.ThemesCatalog
import com.example.services.PhoneControlAction
import com.example.services.SaniAccessibilityService
import com.example.services.SaniNotificationListenerService
import com.example.ui.theme.LocalSaniTheme
import com.example.ui.viewmodel.SaniViewModel

@Composable
fun LanguageDialog(
    currentLanguageCode: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    val theme = LocalSaniTheme.current
    var selectedCode by remember(currentLanguageCode) { mutableStateOf(currentLanguageCode) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = theme.secondaryBackground,
        titleContentColor = theme.primaryText,
        textContentColor = theme.secondaryText,
        title = {
            Text("Language", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                SaniLanguage.entries.forEach { lang ->
                    val isSelected = selectedCode == lang.code
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) theme.primaryAccent.copy(alpha = 0.16f) else theme.background.copy(alpha = 0.35f))
                            .clickable { selectedCode = lang.code }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .testTag("lang_option_${lang.code}")
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { selectedCode = lang.code },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = theme.primaryAccent,
                                unselectedColor = theme.secondaryText
                            )
                        )
                        Column {
                            Text(
                                text = lang.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                color = theme.primaryText
                            )
                            Text(
                                text = lang.nativeSubtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = theme.secondaryText
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(selectedCode) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = theme.primaryAccent,
                    contentColor = theme.background
                ),
                modifier = Modifier.testTag("language_save_button")
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = theme.secondaryText)
            }
        }
    )
}

@Composable
fun AutoReplyDialog(
    initialSettings: AutoReplySettings,
    onDismiss: () -> Unit,
    onSave: (AutoReplySettings) -> Unit
) {
    val theme = LocalSaniTheme.current
    var enabled by remember(initialSettings) { mutableStateOf(initialSettings.enabled) }
    var allowedPackages by remember(initialSettings) { mutableStateOf(initialSettings.allowedPackages) }
    var replyToAll by remember(initialSettings) { mutableStateOf(initialSettings.replyToAll) }
    var replyInGroups by remember(initialSettings) { mutableStateOf(initialSettings.replyInGroups) }
    var contactsCsv by remember(initialSettings) { mutableStateOf(initialSettings.contactAllowlistCsv) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = theme.secondaryBackground,
        titleContentColor = theme.primaryText,
        textContentColor = theme.secondaryText,
        title = {
            Text("AI Auto-Reply", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Auto-Reply sends directly through supported notification reply actions. It stays OFF until you enable it and choose apps + permitted contacts.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = theme.secondaryText
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(theme.primaryAccent.copy(alpha = 0.16f))
                        .clickable { enabled = !enabled }
                        .padding(8.dp)
                        .testTag("auto_reply_main_checkbox")
                ) {
                    Checkbox(
                        checked = enabled,
                        onCheckedChange = { enabled = it },
                        colors = CheckboxDefaults.colors(checkedColor = theme.primaryAccent)
                    )
                    Text(
                        text = "Enable direct AI Auto-Reply",
                        style = MaterialTheme.typography.titleMedium,
                        color = theme.primaryText,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Allowed Apps",
                    style = MaterialTheme.typography.labelLarge,
                    color = theme.secondaryAccent
                )
                ThemesCatalog.supportedAutoReplyApps.forEach { app ->
                    val checked = allowedPackages.contains(app.packageName)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                allowedPackages = if (checked) allowedPackages - app.packageName
                                else allowedPackages + app.packageName
                            }
                            .padding(vertical = 2.dp)
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { isChecked ->
                                allowedPackages = if (isChecked) allowedPackages + app.packageName
                                else allowedPackages - app.packageName
                            },
                            colors = CheckboxDefaults.colors(checkedColor = theme.primaryAccent)
                        )
                        Text(app.displayName, style = MaterialTheme.typography.bodyMedium, color = theme.primaryText)
                    }
                }

                HorizontalDivider(color = theme.primaryAccent.copy(alpha = 0.2f))

                Text(
                    text = "Reply Rules",
                    style = MaterialTheme.typography.labelLarge,
                    color = theme.secondaryAccent
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { replyToAll = !replyToAll }
                ) {
                    Checkbox(
                        checked = replyToAll,
                        onCheckedChange = { replyToAll = it },
                        colors = CheckboxDefaults.colors(checkedColor = theme.primaryAccent)
                    )
                    Text(
                        text = "Koi bhi message kare, sabko auto-reply karo.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = theme.primaryText
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { replyInGroups = !replyInGroups }
                ) {
                    Checkbox(
                        checked = replyInGroups,
                        onCheckedChange = { replyInGroups = it },
                        colors = CheckboxDefaults.colors(checkedColor = theme.primaryAccent)
                    )
                    Text(
                        text = "Groups me bhi reply karo (spam ho sakta hai).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = theme.primaryText
                    )
                }

                OutlinedTextField(
                    value = contactsCsv,
                    onValueChange = { contactsCsv = it },
                    label = { Text("Permitted contacts (comma-separated)") },
                    placeholder = { Text("e.g. Mom, Rahul, Priya") },
                    colors = saniTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = theme.background.copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, theme.glowCyan.copy(alpha = 0.45f))
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(10.dp)
                    ) {
                        Icon(Icons.Filled.Shield, null, tint = theme.glowCyan, modifier = Modifier.size(18.dp))
                        Text(
                            text = "Safety active: Blocks OTPs, banking, payments, password resets, duplicate replies & infinite loops.",
                            style = MaterialTheme.typography.labelSmall,
                            color = theme.secondaryText
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        AutoReplySettings(
                            enabled = enabled,
                            allowedPackages = allowedPackages,
                            replyToAll = replyToAll,
                            replyInGroups = replyInGroups,
                            contactAllowlistCsv = contactsCsv
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = theme.primaryAccent,
                    contentColor = theme.background
                ),
                modifier = Modifier.testTag("auto_reply_save_button")
            ) {
                Text("SAVE", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = theme.secondaryText)
            }
        }
    )
}

@Composable
fun CreatorHubDialog(
    viewModel: SaniViewModel,
    profile: UserProfile,
    onDismiss: () -> Unit
) {
    val theme = LocalSaniTheme.current
    val memories by viewModel.memories.collectAsStateWithLifecycle()

    var displayName by remember(profile.displayName) { mutableStateOf(profile.displayName) }
    var bio by remember(profile.bio) { mutableStateOf(profile.bio) }
    var youtube by remember(profile.youtubeLink) { mutableStateOf(profile.youtubeLink) }
    var instagram by remember(profile.instagramLink) { mutableStateOf(profile.instagramLink) }
    var song by remember(profile.songLink) { mutableStateOf(profile.songLink) }
    var newMemory by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = theme.secondaryBackground,
        titleContentColor = theme.primaryText,
        textContentColor = theme.secondaryText,
        title = { Text("Creator Hub", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    colors = saniTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Bio") },
                    colors = saniTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = youtube,
                    onValueChange = { youtube = it },
                    label = { Text("YouTube Link") },
                    singleLine = true,
                    colors = saniTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = instagram,
                    onValueChange = { instagram = it },
                    label = { Text("Instagram Link") },
                    singleLine = true,
                    colors = saniTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = song,
                    onValueChange = { song = it },
                    label = { Text("Song Link") },
                    singleLine = true,
                    colors = saniTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider(color = theme.primaryAccent.copy(alpha = 0.2f))
                Text("AI Memories (${memories.size})", style = MaterialTheme.typography.labelLarge, color = theme.secondaryAccent)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newMemory,
                        onValueChange = { newMemory = it },
                        label = { Text("Teach SANI a memory") },
                        singleLine = true,
                        colors = saniTextFieldColors(),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            if (newMemory.isNotBlank()) {
                                viewModel.addMemory(newMemory, "Personal")
                                newMemory = ""
                            }
                        }
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Add Memory", tint = theme.primaryAccent)
                    }
                }
                memories.forEach { mem ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(theme.background.copy(alpha = 0.5f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = mem.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = theme.primaryText,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.deleteMemory(mem.id) }, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Filled.DeleteOutline, "Delete memory", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.updateProfileField(
                        mapOf(
                            "displayName" to displayName.trim().ifBlank { "Creator" }.take(60),
                            "bio" to bio.trim().take(240),
                            "youtubeLink" to youtube.trim().take(300),
                            "instagramLink" to instagram.trim().take(300),
                            "songLink" to song.trim().take(300)
                        )
                    )
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = theme.primaryAccent,
                    contentColor = theme.background
                )
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = theme.secondaryText)
            }
        }
    )
}

@Composable
fun DocumentsDialog(
    viewModel: SaniViewModel,
    onDismiss: () -> Unit
) {
    val theme = LocalSaniTheme.current
    val clipboard = LocalClipboardManager.current
    val output by viewModel.documentOutput.collectAsStateWithLifecycle()
    val generating by viewModel.documentGenerating.collectAsStateWithLifecycle()

    var docType by remember { mutableStateOf("Resume") }
    var details by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = {
            viewModel.clearDocumentOutput()
            onDismiss()
        },
        containerColor = theme.secondaryBackground,
        titleContentColor = theme.primaryText,
        textContentColor = theme.secondaryText,
        title = { Text("SANI Document Studio", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Resume", "Biodata", "Letter").forEach { type ->
                        val selected = docType == type
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) theme.primaryAccent else theme.background,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { docType = type }
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                                Text(
                                    text = type,
                                    color = if (selected) theme.background else theme.primaryText,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = details,
                    onValueChange = { details = it },
                    label = { Text("Enter key details (name, skills, purpose)") },
                    minLines = 3,
                    colors = saniTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { viewModel.generateDocument(docType, details) },
                    enabled = !generating && details.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = theme.primaryAccent,
                        contentColor = theme.background
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (generating) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = theme.background, strokeWidth = 2.dp)
                    } else {
                        Text("Generate $docType with Gemini", fontWeight = FontWeight.Bold)
                    }
                }
                if (!output.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = theme.background,
                        border = BorderStroke(1.dp, theme.glowCyan.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Generated $docType", color = theme.glowCyan, fontWeight = FontWeight.Bold)
                                IconButton(
                                    onClick = {
                                        clipboard.setText(AnnotatedString(output!!))
                                        viewModel.showToast("Document copied")
                                    },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Filled.ContentCopy, "Copy document", tint = theme.primaryText, modifier = Modifier.size(16.dp))
                                }
                            }
                            Text(output!!, style = MaterialTheme.typography.bodyMedium, color = theme.primaryText)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                viewModel.clearDocumentOutput()
                onDismiss()
            }) {
                Text("Done", color = theme.primaryAccent)
            }
        }
    )
}

@Composable
fun PhoneControlDialog(
    viewModel: SaniViewModel,
    onDismiss: () -> Unit
) {
    val theme = LocalSaniTheme.current
    val context = LocalContext.current
    val accessibilityConnected by SaniAccessibilityService.isConnected.collectAsStateWithLifecycle()
    val isEnabledInSettings = remember(context) {
        SaniAccessibilityService.isServiceEnabledInSettings(context)
    }
    val recentMessages by SaniNotificationListenerService.recentMessages.collectAsStateWithLifecycle()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = theme.secondaryBackground,
        titleContentColor = theme.primaryText,
        textContentColor = theme.secondaryText,
        title = { Text("Phone Control", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (accessibilityConnected || isEnabledInSettings)
                        "Accessibility Service is active. Tap any supported action or say \"Lock screen\" / \"Go home\" to SANI."
                    else
                        "Enable Sani Phone Control in Android Accessibility Settings to run voice-triggered device actions (never bypasses secure lockscreen).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = theme.secondaryText
                )

                Button(
                    onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = theme.primaryAccent,
                        contentColor = theme.background
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open Accessibility Settings", fontWeight = FontWeight.Bold)
                }

                PhoneControlAction.entries.forEach { action ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = theme.background,
                        border = BorderStroke(1.dp, theme.primaryAccent.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                val ok = SaniAccessibilityService.perform(action)
                                viewModel.showToast(
                                    if (ok) "${action.label} executed"
                                    else "Enable Accessibility Service first"
                                )
                            }
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(action.label, style = MaterialTheme.typography.titleMedium, color = theme.primaryText)
                            Text(action.description, style = MaterialTheme.typography.labelSmall, color = theme.secondaryText)
                        }
                    }
                }

                if (recentMessages.isNotEmpty()) {
                    HorizontalDivider(color = theme.primaryAccent.copy(alpha = 0.2f))
                    Text("Recent Messages", style = MaterialTheme.typography.labelLarge, color = theme.glowCyan)
                    recentMessages.take(5).forEach { msg ->
                        Text(
                            text = "${msg.appName} • ${msg.sender}: ${msg.text} (${msg.autoReplyStatus})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = theme.primaryText
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = theme.primaryAccent)
            }
        }
    )
}
