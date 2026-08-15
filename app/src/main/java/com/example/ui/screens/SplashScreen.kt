package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.OrbState
import com.example.ui.JarvisViewModel
import com.example.ui.Screen
import com.example.ui.components.CyberBackgroundLayout
import com.example.ui.components.FuturisticOrb
import com.example.ui.components.GlassCard
import com.example.ui.components.GlowingButton
import com.example.ui.theme.AtmosphericGlass
import com.example.ui.theme.CyanAtmospheric
import com.example.ui.theme.CyanHighlight
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.StatusOnline
import com.example.ui.theme.TextDarker
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier
) {
    var bootProgress by remember { mutableFloatStateOf(0.1f) }
    var bootStep by remember { mutableIntStateOf(0) }
    var showContinueButton by remember { mutableStateOf(false) }

    val animatedProgress by animateFloatAsState(
        targetValue = bootProgress,
        animationSpec = tween(durationMillis = 400, easing = LinearEasing),
        label = "bootProgress"
    )

    LaunchedEffect(Unit) {
        delay(400)
        bootStep = 1
        bootProgress = 0.35f
        delay(450)
        bootStep = 2
        bootProgress = 0.70f
        delay(500)
        bootStep = 3
        bootProgress = 1.0f
        delay(500)
        showContinueButton = true

        // Auto-navigate directly to AI API Key Setup if no key exists
        delay(800)
        if (viewModel.hasAnyApiKey()) {
            val user = viewModel.secureStorage.getUserSession()
            if (user.isAuthenticated) {
                viewModel.navigateTo(Screen.Chat)
            } else {
                viewModel.navigateTo(Screen.Auth)
            }
        } else {
            // First screen asks to enter Gemini or OpenRouter API key directly
            viewModel.navigateTo(Screen.AISetup)
        }
    }

    CyberBackgroundLayout(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Terminal Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 28.dp)
            ) {
                Text(
                    text = "J A R V I S",
                    color = CyanAtmospheric,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 8.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "CREATED BY ROLLER_GAMING",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 3.sp
                )
            }

            // Central Holographic Reactor
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                FuturisticOrb(
                    state = OrbState.IDLE,
                    size = 160.dp
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Diagnostic Boot Status Glass Card
                GlassCard(
                    modifier = Modifier.fillMaxWidth(0.92f),
                    shape = RoundedCornerShape(16.dp),
                    backgroundColor = AtmosphericGlass,
                    borderColor = GlassBorder
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        DiagnosticRow(
                            icon = Icons.Default.Memory,
                            label = "Neural Matrix Architecture",
                            status = if (bootStep >= 1) "ONLINE" else "INITIALIZING",
                            isComplete = bootStep >= 1
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        DiagnosticRow(
                            icon = Icons.Default.Security,
                            label = "Hardware Keystore Encryption",
                            status = if (bootStep >= 2) "ARMED" else "PENDING",
                            isComplete = bootStep >= 2
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        DiagnosticRow(
                            icon = Icons.Default.Speed,
                            label = "Voice & Audio Synthesis Core",
                            status = if (bootStep >= 3) "READY" else "STANDBY",
                            isComplete = bootStep >= 3
                        )
                    }
                }
            }

            // Bottom Progress Bar & Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            ) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(4.dp),
                    color = CyanAtmospheric,
                    trackColor = Color(0x2206B6D4)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = if (bootStep >= 3) "SYSTEM INITIALIZATION COMPLETE" else "CALIBRATING QUANTUM INTERFACE...",
                    color = if (bootStep >= 3) StatusOnline else TextDarker,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.5.sp
                )

                AnimatedVisibility(
                    visible = showContinueButton,
                    enter = fadeIn() + slideInVertically()
                ) {
                    GlowingButton(
                        text = "INITIALIZE TERMINAL",
                        onClick = {
                            if (viewModel.hasAnyApiKey()) {
                                val user = viewModel.secureStorage.getUserSession()
                                if (user.isAuthenticated) {
                                    viewModel.navigateTo(Screen.Chat)
                                } else {
                                    viewModel.navigateTo(Screen.Auth)
                                }
                            } else {
                                viewModel.navigateTo(Screen.AISetup)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .padding(top = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DiagnosticRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    status: String,
    isComplete: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isComplete) CyanAtmospheric else TextDarker,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = label,
                color = if (isComplete) TextPrimary else TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = status,
                color = if (isComplete) StatusOnline else TextMuted,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            if (isComplete) {
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = StatusOnline,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

