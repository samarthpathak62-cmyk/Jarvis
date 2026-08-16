package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AtmosphericDark
import com.example.ui.theme.AtmosphericGlass
import com.example.ui.theme.AtmosphericGlassCard
import com.example.ui.theme.CyanAtmospheric
import com.example.ui.theme.CyanCore
import com.example.ui.theme.CyanHighlight
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassBorderActive
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun GlowingButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    isPrimary: Boolean = true,
    shape: Shape = RoundedCornerShape(24.dp),
    testTag: String = "glowing_button"
) {
    val backgroundBrush = if (isPrimary) {
        if (enabled) {
            Brush.horizontalGradient(
                colors = listOf(CyanAtmospheric, CyanCore)
            )
        } else {
            Brush.horizontalGradient(
                colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A))
            )
        }
    } else {
        Brush.horizontalGradient(
            colors = listOf(AtmosphericGlassCard, AtmosphericGlass)
        )
    }

    val contentColor = if (isPrimary) {
        if (enabled) AtmosphericDark else Color(0xFF64748B)
    } else {
        CyanAtmospheric
    }

    Box(
        modifier = modifier
            .testTag(testTag)
            .defaultMinSize(minHeight = 48.dp)
            .shadow(
                elevation = if (isPrimary && enabled) 10.dp else 0.dp,
                shape = shape,
                ambientColor = CyanAtmospheric,
                spotColor = CyanAtmospheric
            )
            .clip(shape)
            .background(backgroundBrush)
            .then(
                if (!isPrimary) {
                    Modifier.border(
                        border = BorderStroke(1.dp, if (enabled) GlassBorderActive else GlassBorder),
                        shape = shape
                    )
                } else Modifier
            )
            .clickable(
                enabled = enabled && !isLoading,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = if (isPrimary) AtmosphericDark else CyanAtmospheric),
                onClick = onClick
            )
            .padding(horizontal = 22.dp, vertical = 13.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = contentColor,
                strokeWidth = 2.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    color = contentColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

