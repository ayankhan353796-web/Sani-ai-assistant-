package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalSaniTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class GoldenParticle(
    val xFraction: Float,
    val yFraction: Float,
    val radiusDp: Float,
    val speedFactor: Float,
    val phase: Float,
    val isAccent: Boolean
)

@Composable
fun FloatingParticlesBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val theme = LocalSaniTheme.current
    val particles = remember {
        val rng = Random(42)
        List(26) {
            GoldenParticle(
                xFraction = rng.nextFloat(),
                yFraction = rng.nextFloat(),
                radiusDp = 1.4f + rng.nextFloat() * 2.6f,
                speedFactor = 0.5f + rng.nextFloat() * 0.9f,
                phase = rng.nextFloat() * 6.28f,
                isAccent = rng.nextFloat() > 0.35f
            )
        }
    }
    val infiniteTransition = rememberInfiniteTransition(label = "particles")
    val drift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "drift"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        theme.background,
                        theme.secondaryBackground.copy(alpha = 0.72f),
                        theme.background
                    )
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        theme.primaryAccent.copy(alpha = 0.16f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.5f, size.height * 0.22f),
                    radius = size.width * 0.85f
                ),
                radius = size.width * 0.85f,
                center = Offset(size.width * 0.5f, size.height * 0.22f)
            )

            particles.forEach { p ->
                val yNorm = ((p.yFraction - drift * p.speedFactor) % 1f + 1f) % 1f
                val xWobble = sin((drift * 2f * PI + p.phase)).toFloat() * 14f
                val cx = (p.xFraction * size.width + xWobble).coerceIn(0f, size.width)
                val cy = yNorm * size.height
                val pulse = 0.35f + 0.5f * ((sin((drift * 4f * PI + p.phase)) + 1.0) / 2.0).toFloat()
                val color = if (p.isAccent) theme.secondaryAccent else theme.primaryAccent
                drawCircle(
                    color = color.copy(alpha = pulse * 0.55f),
                    radius = p.radiusDp.dp.toPx(),
                    center = Offset(cx, cy)
                )
            }
        }
        content()
    }
}

@Composable
fun SaniGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 22.dp,
    glowColor: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val theme = LocalSaniTheme.current
    val borderColor = glowColor ?: theme.primaryAccent
    val shape = RoundedCornerShape(cornerRadius)
    val cardModifier = modifier
        .clip(shape)
        .background(
            Brush.linearGradient(
                colors = listOf(
                    theme.secondaryBackground.copy(alpha = 0.86f),
                    theme.background.copy(alpha = 0.90f)
                )
            )
        )
        .border(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    borderColor.copy(alpha = 0.65f),
                    theme.secondaryAccent.copy(alpha = 0.28f),
                    borderColor.copy(alpha = 0.55f)
                )
            ),
            shape = shape
        )
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)

    Column(
        modifier = cardModifier.padding(16.dp),
        content = content
    )
}

@Composable
fun SaniCentralOrb(
    primaryColor: Color,
    secondaryColor: Color,
    isListening: Boolean,
    isSpeaking: Boolean,
    audioLevel: Float,
    size: Dp = 66.dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalSaniTheme.current
    val transition = rememberInfiniteTransition(label = "centralOrb")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbRotation"
    )
    val pulse by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbPulse"
    )

    val activeBoost = if (isListening || isSpeaking) (0.12f + audioLevel * 0.25f) else 0f

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size + 14.dp)
            .minimumInteractiveComponentSize()
            .testTag("bottom_nav_orb")
            .clip(CircleShape)
            .clickable(onClick = onClick)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val baseRadius = (size.toPx() / 2f) * (pulse + activeBoost * 0.25f)

            // Outer cyan/purple aura ring
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        theme.glowCyan.copy(alpha = 0.35f + activeBoost),
                        primaryColor.copy(alpha = 0.22f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = this.size.minDimension / 2f
                ),
                radius = this.size.minDimension / 2f,
                center = center
            )

            // Cyan glowing circular ring
            drawCircle(
                color = theme.glowCyan.copy(alpha = 0.85f),
                radius = baseRadius * 0.98f,
                center = center,
                style = Stroke(width = 2.2.dp.toPx())
            )

            // Purple/primary gradient sphere center
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        secondaryColor.copy(alpha = 0.95f),
                        primaryColor,
                        theme.background
                    ),
                    center = Offset(center.x - baseRadius * 0.2f, center.y - baseRadius * 0.2f),
                    radius = baseRadius * 1.1f
                ),
                radius = baseRadius * 0.84f,
                center = center
            )

            // Rotating orbital particles
            val particleCount = 6
            for (i in 0 until particleCount) {
                val angleRad = ((rotation + i * (360f / particleCount)) * PI / 180.0)
                val px = center.x + cos(angleRad).toFloat() * baseRadius * 1.02f
                val py = center.y + sin(angleRad).toFloat() * baseRadius * 1.02f
                drawCircle(
                    color = if (i % 2 == 0) theme.glowCyan else theme.secondaryAccent,
                    radius = 2.2.dp.toPx(),
                    center = Offset(px, py)
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "S",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                ),
                color = Color.White
            )
            if (isListening || isSpeaking) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(3) { idx ->
                        val h = 4.dp + (audioLevel * 8f * ((idx % 2) + 1)).dp
                        Box(
                            modifier = Modifier
                                .size(width = 2.dp, height = h.coerceAtMost(12.dp))
                                .background(theme.glowCyan, CircleShape)
                        )
                    }
                }
            }
        }
    }
}

enum class SaniNavTab(val route: String, val label: String) {
    HOME("home", "Home"),
    CHAT("chat", "Chat"),
    CAMERA("camera", "Camera"),
    SETTINGS("settings", "Settings")
}

@Composable
fun SaniFloatingBottomBar(
    currentRoute: String,
    orbPrimaryColor: Color,
    orbSecondaryColor: Color,
    isListening: Boolean,
    isSpeaking: Boolean,
    audioLevel: Float,
    onNavigate: (String) -> Unit,
    onOrbClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalSaniTheme.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            shape = RoundedCornerShape(30.dp),
            color = theme.secondaryBackground.copy(alpha = 0.94f),
            border = BorderStroke(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        theme.primaryAccent.copy(alpha = 0.6f),
                        theme.glowCyan.copy(alpha = 0.6f),
                        theme.secondaryAccent.copy(alpha = 0.6f)
                    )
                )
            ),
            shadowElevation = 14.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(66.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomNavItem(
                    tab = SaniNavTab.HOME,
                    selected = currentRoute == SaniNavTab.HOME.route,
                    selectedIcon = Icons.Filled.Home,
                    unselectedIcon = Icons.Outlined.Home,
                    onClick = { onNavigate(SaniNavTab.HOME.route) }
                )
                BottomNavItem(
                    tab = SaniNavTab.CHAT,
                    selected = currentRoute == SaniNavTab.CHAT.route,
                    selectedIcon = Icons.Filled.ChatBubble,
                    unselectedIcon = Icons.Outlined.ChatBubbleOutline,
                    onClick = { onNavigate(SaniNavTab.CHAT.route) }
                )
                Spacer(modifier = Modifier.size(64.dp))
                BottomNavItem(
                    tab = SaniNavTab.CAMERA,
                    selected = currentRoute == SaniNavTab.CAMERA.route,
                    selectedIcon = Icons.Filled.CameraAlt,
                    unselectedIcon = Icons.Outlined.CameraAlt,
                    onClick = { onNavigate(SaniNavTab.CAMERA.route) }
                )
                BottomNavItem(
                    tab = SaniNavTab.SETTINGS,
                    selected = currentRoute == SaniNavTab.SETTINGS.route,
                    selectedIcon = Icons.Filled.Settings,
                    unselectedIcon = Icons.Outlined.Settings,
                    onClick = { onNavigate(SaniNavTab.SETTINGS.route) }
                )
            }
        }

        SaniCentralOrb(
            primaryColor = orbPrimaryColor,
            secondaryColor = orbSecondaryColor,
            isListening = isListening,
            isSpeaking = isSpeaking,
            audioLevel = audioLevel,
            size = 64.dp,
            onClick = onOrbClick,
            modifier = Modifier
                .offset(y = (-14).dp)
                .shadow(12.dp, CircleShape)
        )
    }
}

@Composable
private fun BottomNavItem(
    tab: SaniNavTab,
    selected: Boolean,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    onClick: () -> Unit
) {
    val theme = LocalSaniTheme.current
    val tint = if (selected) theme.primaryAccent else theme.secondaryText
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag("bottom_nav_${tab.route}")
    ) {
        Icon(
            imageVector = if (selected) selectedIcon else unselectedIcon,
            contentDescription = tab.label,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = tab.label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
