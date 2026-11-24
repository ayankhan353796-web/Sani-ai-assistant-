package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalSaniTheme

private sealed interface MarkdownSegment {
    data class TextBlock(val content: String) : MarkdownSegment
    data class CodeBlock(val language: String, val code: String) : MarkdownSegment
}

@Composable
fun SaniMarkdownText(
    markdown: String,
    codeColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    val segments = remember(markdown) { parseMarkdownSegments(markdown) }
    val theme = LocalSaniTheme.current
    val clipboard = LocalClipboardManager.current

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        segments.forEach { segment ->
            when (segment) {
                is MarkdownSegment.TextBlock -> {
                    val annotated = remember(segment.content, textColor, codeColor) {
                        formatInlineMarkdown(segment.content, textColor, codeColor)
                    }
                    Text(
                        text = annotated,
                        style = MaterialTheme.typography.bodyLarge,
                        color = textColor
                    )
                }

                is MarkdownSegment.CodeBlock -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF120804))
                            .border(1.dp, codeColor.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(codeColor.copy(alpha = 0.14f))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = segment.language.ifBlank { "code" }.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = codeColor,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = { clipboard.setText(AnnotatedString(segment.code)) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ContentCopy,
                                    contentDescription = "Copy code",
                                    tint = theme.primaryText,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Text(
                            text = segment.code,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = codeColor,
                            modifier = Modifier
                                .horizontalScroll(rememberScrollState())
                                .padding(12.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun parseMarkdownSegments(input: String): List<MarkdownSegment> {
    val result = mutableListOf<MarkdownSegment>()
    val regex = Regex("```([a-zA-Z0-9_+-]*)\\n([\\s\\S]*?)```")
    var lastIndex = 0
    for (match in regex.findAll(input)) {
        if (match.range.first > lastIndex) {
            val plain = input.substring(lastIndex, match.range.first).trim()
            if (plain.isNotEmpty()) result.add(MarkdownSegment.TextBlock(plain))
        }
        val lang = match.groupValues[1]
        val code = match.groupValues[2].trimEnd()
        result.add(MarkdownSegment.CodeBlock(lang, code))
        lastIndex = match.range.last + 1
    }
    if (lastIndex < input.length) {
        val tail = input.substring(lastIndex).trim()
        if (tail.isNotEmpty()) result.add(MarkdownSegment.TextBlock(tail))
    }
    if (result.isEmpty() && input.isNotBlank()) {
        result.add(MarkdownSegment.TextBlock(input))
    }
    return result
}

private fun formatInlineMarkdown(
    text: String,
    baseColor: Color,
    codeColor: Color
): AnnotatedString {
    return buildAnnotatedString {
        val lines = text.lines()
        lines.forEachIndexed { lineIdx, rawLine ->
            val line = when {
                rawLine.startsWith("### ") -> rawLine.removePrefix("### ")
                rawLine.startsWith("## ") -> rawLine.removePrefix("## ")
                rawLine.startsWith("# ") -> rawLine.removePrefix("# ")
                rawLine.startsWith("- ") -> "• " + rawLine.removePrefix("- ")
                rawLine.startsWith("* ") -> "• " + rawLine.removePrefix("* ")
                else -> rawLine
            }
            val isHeading = rawLine.startsWith("#")
            var i = 0
            while (i < line.length) {
                when {
                    line.startsWith("**", i) -> {
                        val end = line.indexOf("**", i + 2)
                        if (end != -1) {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = baseColor)) {
                                append(line.substring(i + 2, end))
                            }
                            i = end + 2
                        } else {
                            append(line[i])
                            i++
                        }
                    }
                    line[i] == '`' -> {
                        val end = line.indexOf('`', i + 1)
                        if (end != -1) {
                            withStyle(
                                SpanStyle(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    color = codeColor,
                                    background = Color(0x33000000)
                                )
                            ) {
                                append(line.substring(i + 1, end))
                            }
                            i = end + 1
                        } else {
                            append(line[i])
                            i++
                        }
                    }
                    else -> {
                        if (isHeading) {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(line[i])
                            }
                        } else {
                            append(line[i])
                        }
                        i++
                    }
                }
            }
            if (lineIdx < lines.lastIndex) append("\n")
        }
    }
}
