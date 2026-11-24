package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.model.SaniThemeConfig
import com.example.data.themes.ThemesCatalog

val LocalSaniTheme = staticCompositionLocalOf { ThemesCatalog.allThemes.first() }

val SaniShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

@Composable
fun SaniAssistantTheme(
    themeConfig: SaniThemeConfig = ThemesCatalog.allThemes.first(),
    content: @Composable () -> Unit
) {
    val colorScheme = darkColorScheme(
        primary = themeConfig.primaryAccent,
        onPrimary = Color(0xFF1E0B03),
        secondary = themeConfig.secondaryAccent,
        onSecondary = Color(0xFF1E0B03),
        tertiary = themeConfig.glowCyan,
        onTertiary = Color(0xFF041A1D),
        background = themeConfig.background,
        onBackground = themeConfig.primaryText,
        surface = themeConfig.secondaryBackground,
        onSurface = themeConfig.primaryText,
        surfaceVariant = themeConfig.secondaryBackground.copy(alpha = 0.85f),
        onSurfaceVariant = themeConfig.secondaryText,
        outline = themeConfig.primaryAccent.copy(alpha = 0.45f),
        error = Color(0xFFFF6B6B),
        onError = Color.White
    )

    CompositionLocalProvider(LocalSaniTheme provides themeConfig) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SaniTypography,
            shapes = SaniShapes,
            content = content
        )
    }
}
