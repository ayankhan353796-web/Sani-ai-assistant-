package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ChatMessage
import com.example.data.model.CodeColorOption
import com.example.data.model.SaniLanguage
import com.example.data.model.UserProfile
import com.example.services.SaniVoiceManager
import com.example.ui.components.SaniGlassCard
import com.example.ui.components.SaniMarkdownText
import com.example.ui.theme.LocalSaniTheme
import com.example.ui.viewmodel.SaniViewModel

@Composable
fun ChatScreen(
    viewModel: SaniViewModel,
    profile: UserProfile,
    initialPrompt: String?,
    onConsumedInitialPrompt: () -> Unit
) {
    val theme = LocalSaniTheme.current
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val activeConvId by viewModel.activeConversationId.collectAsStateWithLifecycle()
    val messages by viewModel.activeMessages.collectAsStateWithLifecycle()
    val streamingReply by viewModel.streamingReply.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val aiError by viewModel.aiError.collectAsStateWithLifecycle()
    val isListening by viewModel.isListening.collectAsStateWithLifecycle()

    var inputText by remember { mutableStateOf("") }
    var attachedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var attachedFileText by remember { mutableStateOf<String?>(null) }
    var attachmentLabel by remember { mutableStateOf<String?>(null) }
    var showHistoryDialog by remember { mutableStateOf(false) }

    val codeColor = remember(profile.codeColorId) {
        CodeColorOption.fromId(profile.codeColorId).color
    }
    val language = remember(profile.languageCode) {
        SaniLanguage.fromCode(profile.languageCode)
    }

    val listState = rememberLazyListState()

    LaunchedEffect(initialPrompt) {
        if (!initialPrompt.isNullOrBlank()) {
            viewModel.startNewChat()
            viewModel.sendChatMessage(initialPrompt)
            onConsumedInitialPrompt()
        }
    }

    LaunchedEffect(messages.size, streamingReply.length) {
        if (messages.isNotEmpty() || streamingReply.isNotBlank()) {
            val target = (messages.size + if (streamingReply.isNotBlank()) 1 else 0 - 1).coerceAtLeast(0)
            listState.animateScrollToItem(target)
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bmp = BitmapFactory.decodeStream(stream)
                    if (bmp != null) {
                        attachedBitmap = scaleBitmapDown(bmp, 1024)
                        attachmentLabel = "Image attached"
                    }
                }
            } catch (_: Exception) {
                viewModel.showToast("Could not load selected image.")
            }
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val fileName = resolveFileName(context, uri) ?: "document.txt"
                val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use {
                    it.readText()
                }
                if (!content.isNullOrBlank()) {
                    attachedFileText = content
                    attachmentLabel = fileName
                } else {
                    viewModel.showToast("Selected file is empty or binary.")
                }
            } catch (_: Exception) {
                viewModel.showToast("Could not read file content.")
            }
        }
    }

    val voiceHelper = remember(context) { SaniVoiceManager(context) }
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            voiceHelper.startListening(
                language = language,
                onResult = { spoken ->
                    inputText = if (inputText.isBlank()) spoken else "$inputText $spoken"
                },
                onError = { err -> viewModel.showToast(err) }
            )
        }
    }

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
                .padding(bottom = 96.dp)
        ) {
            // Top bar
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Column {
                    Text(
                        text = "SANI Chat",
                        style = MaterialTheme.typography.titleLarge,
                        color = theme.primaryText,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${language.displayName} • ${profile.emotionStyle}",
                        style = MaterialTheme.typography.labelSmall,
                        color = theme.secondaryAccent
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(
                        onClick = { showHistoryDialog = true },
                        modifier = Modifier.testTag("chat_history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.History,
                            contentDescription = "Conversation History",
                            tint = theme.primaryText
                        )
                    }
                    IconButton(
                        onClick = { viewModel.startNewChat() },
                        modifier = Modifier.testTag("chat_new_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AddComment,
                            contentDescription = "New Chat",
                            tint = theme.primaryAccent
                        )
                    }
                }
            }

            // Messages list
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (messages.isEmpty() && streamingReply.isBlank() && !isGenerating) {
                    item {
                        SaniGlassCard(
                            glowColor = theme.glowCyan,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    tint = theme.secondaryAccent
                                )
                                Text(
                                    text = "Namaste ${profile.displayName}! Ask me anything in Hindi, Hinglish, or English.",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = theme.primaryText
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Attach photos or code/text files, dictate with voice, or tap any prompt below.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = theme.secondaryText
                            )
                        }
                    }
                }

                items(messages, key = { it.id }) { msg ->
                    ChatMessageBubble(
                        message = msg,
                        codeColor = codeColor,
                        onCopy = {
                            clipboard.setText(AnnotatedString(msg.text))
                            viewModel.showToast("Copied to clipboard")
                        },
                        onShare = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, msg.text)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share SANI response"))
                        },
                        onSpeak = { viewModel.speakText(msg.text) },
                        onRegenerate = if (msg.role == "model" && msg.id == messages.lastOrNull()?.id) {
                            { viewModel.regenerateLastResponse() }
                        } else null
                    )
                }

                if (isGenerating || streamingReply.isNotBlank()) {
                    item {
                        SaniGlassCard(
                            glowColor = theme.glowCyan,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (streamingReply.isBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = theme.glowCyan,
                                        strokeWidth = 2.dp
                                    )
                                    Text(
                                        text = "SANI is thinking...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = theme.secondaryAccent
                                    )
                                }
                            } else {
                                SaniMarkdownText(
                                    markdown = streamingReply,
                                    codeColor = codeColor,
                                    textColor = theme.primaryText
                                )
                            }
                        }
                    }
                }

                if (!aiError.isNullOrBlank()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.18f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                        ) {
                            Text(
                                text = aiError!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = theme.primaryText,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }

            // Attachment preview
            if (attachmentLabel != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(theme.secondaryBackground)
                        .border(1.dp, theme.primaryAccent.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    attachedBitmap?.let { bmp ->
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Attached preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                    Text(
                        text = attachmentLabel!!,
                        style = MaterialTheme.typography.labelSmall,
                        color = theme.primaryText,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Remove attachment",
                        tint = theme.secondaryText,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable {
                                attachedBitmap = null
                                attachedFileText = null
                                attachmentLabel = null
                            }
                    )
                }
            }

            // Composer bar
            SaniGlassCard(
                cornerRadius = 24.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Image,
                            contentDescription = "Attach Image",
                            tint = theme.secondaryAccent
                        )
                    }
                    IconButton(
                        onClick = {
                            filePickerLauncher.launch(arrayOf("text/*", "application/json", "application/pdf"))
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AttachFile,
                            contentDescription = "Attach File",
                            tint = theme.secondaryText
                        )
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        if (inputText.isEmpty()) {
                            Text(
                                text = "Message SANI...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = theme.secondaryText.copy(alpha = 0.7f)
                            )
                        }
                        BasicTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = theme.primaryText),
                            cursorBrush = SolidColor(theme.primaryAccent),
                            maxLines = 4,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("chat_message_input")
                        )
                    }

                    IconButton(
                        onClick = {
                            val granted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (granted) {
                                voiceHelper.startListening(
                                    language = language,
                                    onResult = { spoken ->
                                        inputText = if (inputText.isBlank()) spoken else "$inputText $spoken"
                                    },
                                    onError = { err -> viewModel.showToast(err) }
                                )
                            } else {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Mic,
                            contentDescription = "Voice Message",
                            tint = if (isListening) theme.glowCyan else theme.secondaryText
                        )
                    }

                    IconButton(
                        onClick = {
                            val textToSend = inputText
                            val bmp = attachedBitmap
                            val fileTxt = attachedFileText
                            val label = attachmentLabel
                            inputText = ""
                            attachedBitmap = null
                            attachedFileText = null
                            attachmentLabel = null
                            viewModel.sendChatMessage(
                                prompt = textToSend,
                                attachedBitmap = bmp,
                                attachedFileText = fileTxt,
                                attachmentLabel = label
                            )
                        },
                        enabled = !isGenerating,
                        modifier = Modifier
                            .size(42.dp)
                            .background(theme.primaryAccent, CircleShape)
                            .testTag("chat_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = theme.background,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }

    if (showHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showHistoryDialog = false },
            containerColor = theme.secondaryBackground,
            titleContentColor = theme.primaryText,
            textContentColor = theme.secondaryText,
            title = { Text("Conversation History") },
            text = {
                if (conversations.isEmpty()) {
                    Text("No saved conversations yet. Start chatting with SANI!")
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.height(300.dp)
                    ) {
                        items(conversations, key = { it.id }) { conv ->
                            val isSelected = conv.id == activeConvId
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) theme.primaryAccent.copy(alpha = 0.2f) else theme.background,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) theme.primaryAccent else theme.primaryAccent.copy(alpha = 0.25f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.selectConversation(conv.id)
                                        showHistoryDialog = false
                                    }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(10.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = conv.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = theme.primaryText,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (conv.lastMessagePreview.isNotBlank()) {
                                            Text(
                                                text = conv.lastMessagePreview,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = theme.secondaryText,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    IconButton(onClick = { viewModel.deleteConversation(conv.id) }) {
                                        Icon(
                                            imageVector = Icons.Filled.DeleteOutline,
                                            contentDescription = "Delete chat",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.startNewChat()
                    showHistoryDialog = false
                }) {
                    Text("New Chat", color = theme.primaryAccent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showHistoryDialog = false }) {
                    Text("Close", color = theme.secondaryText)
                }
            }
        )
    }
}

@Composable
private fun ChatMessageBubble(
    message: ChatMessage,
    codeColor: androidx.compose.ui.graphics.Color,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onSpeak: () -> Unit,
    onRegenerate: (() -> Unit)?
) {
    val theme = LocalSaniTheme.current
    val isModel = message.role == "model"

    Column(
        horizontalAlignment = if (isModel) Alignment.Start else Alignment.End,
        modifier = Modifier.fillMaxWidth()
    ) {
        SaniGlassCard(
            cornerRadius = 20.dp,
            glowColor = if (isModel) theme.glowCyan else theme.primaryAccent,
            modifier = Modifier.fillMaxWidth(0.92f)
        ) {
            if (message.hasAttachment && message.attachmentLabel.isNotBlank()) {
                Text(
                    text = "Attachment: ${message.attachmentLabel}",
                    style = MaterialTheme.typography.labelSmall,
                    color = theme.secondaryAccent
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            if (isModel) {
                SaniMarkdownText(
                    markdown = message.text,
                    codeColor = codeColor,
                    textColor = theme.primaryText
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MessageActionChip(Icons.Filled.ContentCopy, "Copy", onCopy)
                    MessageActionChip(Icons.Filled.Share, "Share", onShare)
                    MessageActionChip(Icons.Filled.VolumeUp, "Speak", onSpeak)
                    if (onRegenerate != null) {
                        MessageActionChip(Icons.Filled.Refresh, "Retry", onRegenerate)
                    }
                }
            } else {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = theme.primaryText
                )
            }
        }
    }
}

@Composable
private fun MessageActionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val theme = LocalSaniTheme.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = theme.secondaryAccent,
            modifier = Modifier.size(15.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = theme.secondaryText
        )
    }
}

private fun scaleBitmapDown(bitmap: Bitmap, maxDim: Int): Bitmap {
    val w = bitmap.width
    val h = bitmap.height
    if (w <= maxDim && h <= maxDim) return bitmap
    val ratio = minOf(maxDim.toFloat() / w, maxDim.toFloat() / h)
    return Bitmap.createScaledBitmap(bitmap, (w * ratio).toInt(), (h * ratio).toInt(), true)
}

private fun resolveFileName(context: android.content.Context, uri: Uri): String? {
    val cursor = context.contentResolver.query(uri, null, null, null, null) ?: return null
    cursor.use {
        if (it.moveToFirst()) {
            val idx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0) return it.getString(idx)
        }
    }
    return null
}
