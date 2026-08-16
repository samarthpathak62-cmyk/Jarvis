package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AtmosphericDarkSecondary
import com.example.ui.theme.AtmosphericGlass
import com.example.ui.theme.AtmosphericGlassCard
import com.example.ui.theme.CyanAtmospheric
import com.example.ui.theme.GlassBorder

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    borderColor: Color = GlassBorder,
    borderWidth: Dp = 1.dp,
    elevation: Dp = 0.dp,
    backgroundColor: Color = AtmosphericGlass,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .shadow(elevation, shape)
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        backgroundColor,
                        AtmosphericDarkSecondary.copy(alpha = 0.65f)
                    )
                )
            )
            .border(
                border = BorderStroke(
                    borderWidth,
                    Brush.verticalGradient(
                        colors = listOf(
                            borderColor,
                            borderColor.copy(alpha = 0.25f)
                        )
                    )
                ),
                shape = shape
            )
    ) {
        content()
    }
}

