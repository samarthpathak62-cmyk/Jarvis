package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AIProvider
import com.example.data.model.OrbState
import com.example.ui.JarvisViewModel
import com.example.ui.Screen
import com.example.ui.components.CyberBackgroundLayout
import com.example.ui.components.FuturisticOrb
import com.example.ui.components.GlassCard
import com.example.ui.components.GlowingButton
import com.example.ui.theme.AtmosphericGlass
import com.example.ui.theme.AtmosphericGlassCard
import com.example.ui.theme.CyanAtmospheric
import com.example.ui.theme.CyanCore
import com.example.ui.theme.CyanHighlight
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassBorderActive
import com.example.ui.theme.GlassBorderCyan
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusOnline
import com.example.ui.theme.StatusThinking
import com.example.ui.theme.TextDarker
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AISetupScreen(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier,
    canSkip: Boolean = false
) {
    var selectedProvider by remember { mutableStateOf(viewModel.secureStorage.activeProvider) }
    var apiKeyInput by remember { mutableStateOf("") }
    var isKeyVisible by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }

    val isValidating by viewModel.isValidatingKey.collectAsState()
    val validationStateText by viewModel.keyValidationState.collectAsState()
    val focusManager = LocalFocusManager.current

    CyberBackgroundLayout(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = CyanAtmospheric,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "NEURAL LINK CALIBRATION",
                        color = CyanAtmospheric,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Connect AI Brain",
                    color = TextPrimary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Configure your preferred AI provider to power JARVIS.",
                    color = TextMuted,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Central Arc Reactor
            FuturisticOrb(
                state = if (isValidating) OrbState.THINKING else OrbState.IDLE,
                size = 100.dp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Main Setup Card
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                backgroundColor = AtmosphericGlass,
                borderColor = GlassBorder
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "SELECT PROVIDER",
                        color = CyanAtmospheric,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.5.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Provider Tabs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ProviderOptionTab(
                            title = "Google Gemini",
                            subtitle = "Direct REST API",
                            isSelected = selectedProvider == AIProvider.GEMINI,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                selectedProvider = AIProvider.GEMINI
                                statusMessage = null
                            }
                        )

                        ProviderOptionTab(
                            title = "OpenRouter",
                            subtitle = "Universal Gateway",
                            isSelected = selectedProvider == AIProvider.OPENROUTER,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                selectedProvider = AIProvider.OPENROUTER
                                statusMessage = null
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // API Key Input
                    Text(
                        text = if (selectedProvider == AIProvider.GEMINI) "GEMINI API KEY" else "OPENROUTER API KEY",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = {
                            apiKeyInput = it
                            statusMessage = null
                        },
                        placeholder = {
                            Text(
                                text = if (selectedProvider == AIProvider.GEMINI) "AIzaSy..." else "sk-or-v1-...",
                                color = TextDarker,
                                fontSize = 13.sp
                            )
                        },
                        visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                                Icon(
                                    imageVector = if (isKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (isKeyVisible) "Hide Key" else "Show Key",
                                    tint = TextMuted
                                )
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanAtmospheric,
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = CyanAtmospheric
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Key Security Hint
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = StatusOnline,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Stored securely with Android Hardware Keystore AES-256 GCM.",
                            color = TextDarker,
                            fontSize = 11.sp
                        )
                    }

                    // Existing key status if present
                    val existingMasked = viewModel.secureStorage.getMaskedApiKey(selectedProvider)
                    if (existingMasked != "Not Configured") {
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(AtmosphericGlassCard)
                                .border(1.dp, GlassBorderCyan, RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Current Key: $existingMasked",
                                    color = CyanAtmospheric,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "ACTIVE",
                                    color = StatusOnline,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    // Validation status or error
                    AnimatedVisibility(visible = validationStateText != null || statusMessage != null) {
                        val text = statusMessage ?: validationStateText ?: ""
                        val color = if (isSuccess) StatusOnline else if (isValidating) StatusThinking else StatusError
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isValidating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = StatusThinking
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            } else {
                                Icon(
                                    imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = color,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = text,
                                color = color,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Action Buttons
                    GlowingButton(
                        text = if (isValidating) "VERIFYING LINK..." else "VALIDATE & CONNECT BRAIN",
                        isLoading = isValidating,
                        enabled = apiKeyInput.isNotBlank() || existingMasked != "Not Configured",
                        onClick = {
                            focusManager.clearFocus()
                            if (apiKeyInput.isNotBlank()) {
                                viewModel.validateAndSaveApiKey(
                                    provider = selectedProvider,
                                    key = apiKeyInput
                                ) { success, msg ->
                                    isSuccess = success
                                    statusMessage = msg
                                    if (success) {
                                        viewModel.navigateTo(Screen.Chat)
                                    }
                                }
                            } else if (existingMasked != "Not Configured") {
                                viewModel.secureStorage.activeProvider = selectedProvider
                                viewModel.navigateTo(Screen.Chat)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (canSkip || viewModel.hasAnyApiKey()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        GlowingButton(
                            text = "Proceed to Terminal",
                            isPrimary = false,
                            onClick = {
                                viewModel.navigateTo(Screen.Chat)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Footer Developer Credit
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Text(
                    text = "JARVIS — Created by Roller_gaming",
                    color = TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
private fun ProviderOptionTab(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) CyanCore.copy(alpha = 0.2f) else AtmosphericGlassCard)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) CyanAtmospheric else GlassBorder,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = if (isSelected) CyanAtmospheric else TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = CyanAtmospheric,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                color = TextMuted,
                fontSize = 11.sp
            )
        }
    }
}

