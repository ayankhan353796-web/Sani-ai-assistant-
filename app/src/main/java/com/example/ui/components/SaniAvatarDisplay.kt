package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AvatarKind
import com.example.data.model.AvatarOption
import com.example.ui.theme.LocalSaniTheme
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun SaniAvatarDisplay(
    avatarOption: AvatarOption,
    emotionLabel: String,
    isListening: Boolean,
    isSpeaking: Boolean,
    audioLevel: Float,
    size: Dp = 230.dp,
    modifier: Modifier = Modifier
) {
    val theme = LocalSaniTheme.current
    val transition = rememberInfiniteTransition(label = "avatarAnim")
    val breathing by transition.animateFloat(
        initialValue = 0.985f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing"
    )
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    val lipSyncPulse by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(220, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "lipSync"
    )

    var isBlinking by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(3600L)
            isBlinking = true
            delay(130L)
            isBlinking = false
        }
    }

    val activeRingColor = when {
        isListening -> theme.glowCyan
        isSpeaking -> theme.primaryAccent
        else -> theme.secondaryAccent
    }
    val speakingWave = if (isSpeaking) lipSyncPulse else if (isListening) audioLevel else 0f

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size + 32.dp)
    ) {
        // Outer reactive sonar / particle halo
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val maxR = this.size.minDimension / 2f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        activeRingColor.copy(alpha = if (isListening || isSpeaking) 0.42f else 0.22f),
                        theme.glowPurple.copy(alpha = 0.14f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = maxR
                ),
                radius = maxR,
                center = center
            )

            if (isListening || isSpeaking) {
                val ringScale = 0.88f + speakingWave * 0.12f
                drawCircle(
                    color = activeRingColor.copy(alpha = 0.65f),
                    radius = maxR * ringScale,
                    center = center,
                    style = Stroke(width = 2.4.dp.toPx())
                )
            }
        }

        when (avatarOption.kind) {
            AvatarKind.ORIGINAL_SANI -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(size)
                        .scale(breathing)
                        .clip(CircleShape)
                        .border(
                            width = 2.5.dp,
                            brush = Brush.linearGradient(
                                listOf(
                                    theme.primaryAccent,
                                    theme.glowCyan,
                                    theme.secondaryAccent
                                )
                            ),
                            shape = CircleShape
                        )
                        .background(theme.background)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_sani_avatar),
                        contentDescription = "SANI AI Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Subtle natural blink & headphone glow overlay
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        if (isBlinking) {
                            drawRect(
                                color = theme.background.copy(alpha = 0.18f),
                                topLeft = Offset(0f, this.size.height * 0.30f),
                                size = androidx.compose.ui.geometry.Size(this.size.width, this.size.height * 0.08f)
                            )
                        }
                        if (isListening || isSpeaking) {
                            drawCircle(
                                color = activeRingColor.copy(alpha = 0.28f * speakingWave),
                                radius = this.size.minDimension * 0.48f,
                                style = Stroke(width = 5.dp.toPx())
                            )
                        }
                    }

                    // Lip-sync voice equalizer pill overlay when speaking or listening
                    if (isSpeaking || isListening) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = theme.background.copy(alpha = 0.85f),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 14.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                val bars = 7
                                for (i in 0 until bars) {
                                    val factor = if (i % 2 == 0) speakingWave else (1.1f - speakingWave).coerceIn(0.2f, 1f)
                                    val h = (5f + factor * 14f).dp
                                    Box(
                                        modifier = Modifier
                                            .size(width = 3.dp, height = h)
                                            .background(activeRingColor, CircleShape)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            AvatarKind.PARTICLE_SPHERE -> {
                ParticleSphereCanvas(
                    primaryColor = avatarOption.primaryColor,
                    secondaryColor = avatarOption.secondaryColor,
                    rotation = rotation,
                    boost = speakingWave,
                    modifier = Modifier
                        .size(size)
                        .scale(breathing)
                )
            }

            AvatarKind.LOGO_ORB -> {
                LogoOrbCanvas(
                    primaryColor = avatarOption.primaryColor,
                    secondaryColor = avatarOption.secondaryColor,
                    rotation = rotation,
                    boost = speakingWave,
                    modifier = Modifier
                        .size(size)
                        .scale(breathing)
                )
            }
        }

        // Emotion style badge
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = theme.secondaryBackground.copy(alpha = 0.92f),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                theme.secondaryAccent.copy(alpha = 0.6f)
            ),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 6.dp, end = 6.dp)
        ) {
            Text(
                text = when {
                    isListening -> "Listening"
                    isSpeaking -> "Speaking"
                    else -> emotionLabel
                },
                style = MaterialTheme.typography.labelSmall,
                color = theme.primaryText,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun ParticleSphereCanvas(
    primaryColor: Color,
    secondaryColor: Color,
    rotation: Float,
    boost: Float,
    modifier: Modifier = Modifier
) {
    val dots = remember {
        val rng = Random(108)
        List(72) {
            Triple(
                rng.nextFloat() * 360f,
                0.25f + rng.nextFloat() * 0.72f,
                1.8f + rng.nextFloat() * 3.2f
            )
        }
    }
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f * 0.9f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    primaryColor.copy(alpha = 0.35f + boost * 0.25f),
                    secondaryColor.copy(alpha = 0.14f),
                    Color.Transparent
                ),
                center = center,
                radius = radius
            ),
            radius = radius,
            center = center
        )
        dots.forEachIndexed { idx, (angleDeg, distFrac, dotSize) ->
            val rad = ((angleDeg + rotation * (if (idx % 2 == 0) 1f else -0.7f)) * PI / 180.0)
            val wobble = 1f + boost * 0.14f * sin(rad * 3.0).toFloat()
            val px = center.x + cos(rad).toFloat() * radius * distFrac * wobble
            val py = center.y + sin(rad).toFloat() * radius * distFrac * wobble
            drawCircle(
                color = if (idx % 3 == 0) secondaryColor else primaryColor,
                radius = dotSize.dp.toPx(),
                center = Offset(px, py)
            )
        }
    }
}

@Composable
fun LogoOrbCanvas(
    primaryColor: Color,
    secondaryColor: Color,
    rotation: Float,
    boost: Float,
    modifier: Modifier = Modifier
) {
    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2f * 0.86f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        secondaryColor,
                        primaryColor,
                        Color(0xFF1A0A04)
                    ),
                    center = Offset(center.x - radius * 0.25f, center.y - radius * 0.25f),
                    radius = radius * 1.15f
                ),
                radius = radius,
                center = center
            )
            drawCircle(
                color = secondaryColor.copy(alpha = 0.8f),
                radius = radius * (1.06f + boost * 0.08f),
                center = center,
                style = Stroke(width = 2.5.dp.toPx())
            )
            for (i in 0 until 8) {
                val rad = ((rotation + i * 45f) * PI / 180.0)
                val px = center.x + cos(rad).toFloat() * radius * 1.12f
                val py = center.y + sin(rad).toFloat() * radius * 1.12f
                drawCircle(
                    color = if (i % 2 == 0) secondaryColor else primaryColor,
                    radius = 3.dp.toPx(),
                    center = Offset(px, py)
                )
            }
        }
        Text(
            text = "SANI",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 3.sp
            ),
            color = Color.White
        )
    }
}
