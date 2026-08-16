package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.OrbState
import com.example.ui.theme.BlueAccent
import com.example.ui.theme.BlueElectric
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.StatusThinking
import com.example.ui.theme.TealAccent
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun FuturisticOrb(
    state: OrbState,
    audioRms: Float = 0f,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    onClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "OrbTransitions")

    // Continuous smooth rotation for rings
    val rotationFast by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (state == OrbState.THINKING) 2000 else 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "FastRotation"
    )

    val rotationReverse by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (state == OrbState.THINKING) 3000 else 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ReverseRotation"
    )

    // Breathing pulse for core
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    OrbState.THINKING -> 600
                    OrbState.LISTENING -> 800
                    OrbState.SPEAKING -> 700
                    OrbState.IDLE -> 2400
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "CorePulse"
    )

    val coreGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowAlpha"
    )

    val primaryColor = when (state) {
        OrbState.THINKING -> StatusThinking
        OrbState.LISTENING -> CyanNeon
        OrbState.SPEAKING -> TealAccent
        OrbState.IDLE -> CyanNeon
    }

    val secondaryColor = when (state) {
        OrbState.THINKING -> Color(0xFFFF9E00)
        OrbState.LISTENING -> BlueElectric
        OrbState.SPEAKING -> BlueAccent
        OrbState.IDLE -> BlueAccent
    }

    Box(
        modifier = modifier
            .size(size)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val radius = this.size.minDimension / 2f

            // 1. Outermost Ambient Radial Glow
            val glowRadius = radius * (pulseScale + (audioRms * 0.35f))
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.45f * coreGlowAlpha),
                        secondaryColor.copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = glowRadius.coerceAtLeast(10f)
                ),
                radius = glowRadius,
                center = center
            )

            // 2. Outer Segmented Arc Ring (Rotating Clockwise)
            rotate(rotationFast, pivot = center) {
                val outerRingRadius = radius * 0.88f
                val strokeWidth = 2.5.dp.toPx()
                for (i in 0 until 6) {
                    val startAngle = i * 60f + 10f
                    drawArc(
                        color = primaryColor.copy(alpha = 0.85f),
                        startAngle = startAngle,
                        sweepAngle = 40f,
                        useCenter = false,
                        topLeft = Offset(center.x - outerRingRadius, center.y - outerRingRadius),
                        size = androidx.compose.ui.geometry.Size(outerRingRadius * 2, outerRingRadius * 2),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                // Tech Tick Dots
                for (i in 0 until 12) {
                    val angle = Math.toRadians((i * 30.0)).toFloat()
                    val tickRadius = radius * 0.96f
                    val tickPos = Offset(
                        center.x + tickRadius * cos(angle),
                        center.y + tickRadius * sin(angle)
                    )
                    drawCircle(
                        color = if (i % 3 == 0) primaryColor else primaryColor.copy(alpha = 0.4f),
                        radius = if (i % 3 == 0) 2.2.dp.toPx() else 1.2.dp.toPx(),
                        center = tickPos
                    )
                }
            }

            // 3. Middle Counter-Rotating Reticle Ring
            rotate(rotationReverse, pivot = center) {
                val midRingRadius = radius * 0.68f
                val strokeWidth = 2.dp.toPx()
                for (i in 0 until 4) {
                    val startAngle = i * 90f + 15f
                    drawArc(
                        color = secondaryColor.copy(alpha = 0.9f),
                        startAngle = startAngle,
                        sweepAngle = 60f,
                        useCenter = false,
                        topLeft = Offset(center.x - midRingRadius, center.y - midRingRadius),
                        size = androidx.compose.ui.geometry.Size(midRingRadius * 2, midRingRadius * 2),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
            }

            // 4. Dynamic Audio Wave Ring (expands with RMS in listening/speaking mode)
            if (state == OrbState.LISTENING || state == OrbState.SPEAKING) {
                val audioWaveRadius = radius * (0.50f + audioRms * 0.40f)
                drawCircle(
                    color = primaryColor.copy(alpha = 0.6f + (audioRms * 0.4f)),
                    radius = audioWaveRadius,
                    center = center,
                    style = Stroke(width = (2f + audioRms * 4f).dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // 5. Inner Concentric Ring
            val innerRingRadius = radius * 0.42f
            drawCircle(
                color = primaryColor.copy(alpha = 0.5f),
                radius = innerRingRadius,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )

            // 6. Center Nuclear Core (Pulsing Arc Reactor)
            val coreRadius = radius * 0.28f * (if (state == OrbState.LISTENING || state == OrbState.SPEAKING) 1f + audioRms * 0.35f else pulseScale)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        CyanGlow,
                        primaryColor,
                        secondaryColor
                    ),
                    center = center,
                    radius = coreRadius.coerceAtLeast(4f)
                ),
                radius = coreRadius,
                center = center
            )

            // Center Bright Sparkle Core
            drawCircle(
                color = Color.White,
                radius = 3.dp.toPx(),
                center = center
            )
        }
    }
}
