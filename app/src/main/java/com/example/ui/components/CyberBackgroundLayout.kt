package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.AtmosphericDark
import com.example.ui.theme.AtmosphericDarkSecondary
import com.example.ui.theme.AtmosphericDarkSurface
import com.example.ui.theme.BlueAtmospheric
import com.example.ui.theme.CyanAtmospheric
import com.example.ui.theme.CyanCore

@Composable
fun CyberBackgroundLayout(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        AtmosphericDark,
                        AtmosphericDarkSurface,
                        AtmosphericDarkSecondary
                    )
                )
            )
    ) {
        // Atmospheric Ambient Lighting Blurs & Subtle HUD Micro-Grid
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height

            // 1. Top-Center/Left Cyan Atmospheric Ambient Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        CyanCore.copy(alpha = 0.15f),
                        CyanAtmospheric.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    center = Offset(canvasW * 0.35f, canvasH * 0.18f),
                    radius = canvasW * 0.75f
                ),
                radius = canvasW * 0.75f,
                center = Offset(canvasW * 0.35f, canvasH * 0.18f)
            )

            // 2. Bottom-Right Deep Blue Atmospheric Ambient Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        BlueAtmospheric.copy(alpha = 0.22f),
                        Color.Transparent
                    ),
                    center = Offset(canvasW * 0.85f, canvasH * 0.82f),
                    radius = canvasW * 0.8f
                ),
                radius = canvasW * 0.8f,
                center = Offset(canvasW * 0.85f, canvasH * 0.82f)
            )

            // 3. Center Subtle Radial Ambient Halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        CyanAtmospheric.copy(alpha = 0.04f),
                        Color.Transparent
                    ),
                    center = Offset(canvasW * 0.5f, canvasH * 0.5f),
                    radius = canvasW * 0.5f
                ),
                radius = canvasW * 0.5f,
                center = Offset(canvasW * 0.5f, canvasH * 0.5f)
            )

            // 4. Subtle Micro-Dot Grid for futuristic depth
            val step = 44f
            val dotColor = Color(0x1222D3EE)
            var y = 22f
            while (y < canvasH) {
                var x = 22f
                while (x < canvasW) {
                    drawCircle(
                        color = dotColor,
                        radius = 1f,
                        center = Offset(x, y)
                    )
                    x += step
                }
                y += step
            }
        }

        content()
    }
}

