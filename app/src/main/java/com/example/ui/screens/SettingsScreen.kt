package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AIProvider
import com.example.data.model.VoiceSettings
import com.example.ui.JarvisViewModel
import com.example.ui.Screen
import com.example.ui.components.CyberBackgroundLayout
import com.example.ui.components.GlassCard
import com.example.ui.components.GlowingButton
import com.example.ui.theme.AtmosphericDarkSurface
import com.example.ui.theme.AtmosphericGlass
import com.example.ui.theme.AtmosphericGlassCard
import com.example.ui.theme.CyanAtmospheric
import com.example.ui.theme.CyanCore
import com.example.ui.theme.CyanHighlight
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassBorderCyan
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusOnline
import com.example.ui.theme.TextDarker
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val userProfile by viewModel.userProfile.collectAsState()

    var activeProvider by remember { mutableStateOf(viewModel.secureStorage.activeProvider) }
    var selectedModel by remember { mutableStateOf(viewModel.secureStorage.getSelectedModel(activeProvider)) }
    var temperature by remember { mutableFloatStateOf(viewModel.secureStorage.temperature) }

    var voiceSettings by remember { mutableStateOf(viewModel.secureStorage.getVoiceSettings()) }

    var showKeyEditDialog by remember { mutableStateOf(false) }
    var newApiKeyInput by remember { mutableStateOf("") }
    var showClearDataDialog by remember { mutableStateOf(false) }
    var isModelDropdownExpanded by remember { mutableStateOf(false) }

    val modelsForProvider = viewModel.availableModels.filter { it.provider == activeProvider }

    CyberBackgroundLayout(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.navigateTo(Screen.Chat) },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(CyanAtmospheric.copy(alpha = 0.12f))
                        .border(1.dp, CyanAtmospheric.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Terminal",
                        tint = CyanAtmospheric
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "SETTINGS // CONFIGURATION",
                        color = CyanAtmospheric,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "JARVIS Terminal System Preferences",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            // 1. Google Account Card
            SectionHeader(title = "USER AUTHENTICATION", icon = Icons.Default.AccountCircle)
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                backgroundColor = AtmosphericGlass,
                borderColor = GlassBorder
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(CyanCore.copy(alpha = 0.2f))
                                .border(1.dp, CyanAtmospheric, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = userProfile.displayName.take(1).uppercase(),
                                color = CyanAtmospheric,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = userProfile.displayName,
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (userProfile.email.isNotBlank()) userProfile.email else "Local Authenticated User",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.signOut() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Sign Out",
                            tint = StatusError
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. AI Brain & Provider Configuration
            SectionHeader(title = "AI BRAIN ARCHITECTURE", icon = Icons.Default.Psychology)
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                backgroundColor = AtmosphericGlass,
                borderColor = GlassBorder
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Active AI Provider",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ProviderPill(
                            name = "Google Gemini",
                            isSelected = activeProvider == AIProvider.GEMINI,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                activeProvider = AIProvider.GEMINI
                                viewModel.secureStorage.activeProvider = AIProvider.GEMINI
                                selectedModel = viewModel.secureStorage.getSelectedModel(AIProvider.GEMINI)
                            }
                        )

                        ProviderPill(
                            name = "OpenRouter",
                            isSelected = activeProvider == AIProvider.OPENROUTER,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                activeProvider = AIProvider.OPENROUTER
                                viewModel.secureStorage.activeProvider = AIProvider.OPENROUTER
                                selectedModel = viewModel.secureStorage.getSelectedModel(AIProvider.OPENROUTER)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // API Key Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Configured Key (${activeProvider.displayName})",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                            Text(
                                text = viewModel.secureStorage.getMaskedApiKey(activeProvider),
                                color = CyanAtmospheric,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Row {
                            IconButton(onClick = {
                                newApiKeyInput = ""
                                showKeyEditDialog = true
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Key",
                                    tint = CyanAtmospheric,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            if (viewModel.secureStorage.hasApiKey(activeProvider)) {
                                IconButton(onClick = {
                                    viewModel.removeApiKey(activeProvider)
                                    Toast.makeText(context, "API Key removed.", Toast.LENGTH_SHORT).show()
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Remove Key",
                                        tint = StatusError,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Model Selection
                    Text(
                        text = "Foundation Model",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    ExposedDropdownMenuBox(
                        expanded = isModelDropdownExpanded,
                        onExpandedChange = { isModelDropdownExpanded = !isModelDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedModel,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isModelDropdownExpanded) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanAtmospheric,
                                unfocusedBorderColor = GlassBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )

                        ExposedDropdownMenu(
                            expanded = isModelDropdownExpanded,
                            onDismissRequest = { isModelDropdownExpanded = false },
                            modifier = Modifier.background(AtmosphericDarkSurface)
                        ) {
                            modelsForProvider.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(text = option.name, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            Text(text = option.description, color = TextMuted, fontSize = 11.sp)
                                        }
                                    },
                                    onClick = {
                                        selectedModel = option.id
                                        viewModel.secureStorage.setSelectedModel(activeProvider, option.id)
                                        isModelDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Temperature Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Creativity (Temperature)",
                            color = TextPrimary,
                            fontSize = 13.sp
                        )
                        Text(
                            text = String.format("%.2f", temperature),
                            color = CyanAtmospheric,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Slider(
                        value = temperature,
                        onValueChange = {
                            temperature = it
                            viewModel.secureStorage.temperature = it
                        },
                        valueRange = 0.0f..1.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = CyanAtmospheric,
                            activeTrackColor = CyanAtmospheric,
                            inactiveTrackColor = Color(0x2206B6D4)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. Real Stock Calm Voice & TTS Configuration
            SectionHeader(title = "STOCK CALM VOICE SYNTHESIS", icon = Icons.Default.RecordVoiceOver)
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                backgroundColor = AtmosphericGlass,
                borderColor = GlassBorder
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Auto-Vocalize Responses",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Speak responses using built-in calm voice",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }

                        Switch(
                            checked = voiceSettings.autoSpeak,
                            onCheckedChange = {
                                val updated = voiceSettings.copy(autoSpeak = it)
                                voiceSettings = updated
                                viewModel.secureStorage.saveVoiceSettings(updated)
                                viewModel.voiceManager.applyVoiceSettings(updated)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AtmosphericDarkSurface,
                                checkedTrackColor = CyanAtmospheric,
                                uncheckedTrackColor = AtmosphericGlassCard
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Real Local Male Voice Engine (Realistic & Expressive)",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Authentic deep offline British-Hinglish voice with realistic laughter and emotion",
                        color = TextMuted,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Featured Voice: Expressive British Hinglish (with Laughter)
                    VoiceProfileCard(
                        title = "✨ Expressive British-Hinglish",
                        subtitle = "🇬🇧 Iconic UK + Real Laughter & Emotion",
                        isSelected = voiceSettings.voiceType == "expressive_british_hinglish",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val updated = voiceSettings.copy(voiceType = "expressive_british_hinglish", pitch = 0.85f, speechRate = 0.94f)
                            voiceSettings = updated
                            viewModel.secureStorage.saveVoiceSettings(updated)
                            viewModel.voiceManager.applyVoiceSettings(updated)
                            viewModel.voiceManager.testVoice("Haha! Greetings Commander. Main aapka JARVIS hoon, all systems ready.")
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Voice Profile selection - Row 1
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        VoiceProfileCard(
                            title = "JARVIS British",
                            subtitle = "🇬🇧 Iconic UK Male",
                            isSelected = voiceSettings.voiceType == "jarvis_british_male" || voiceSettings.voiceType == "calm_british",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val updated = voiceSettings.copy(voiceType = "jarvis_british_male", pitch = 0.82f, speechRate = 0.95f)
                                voiceSettings = updated
                                viewModel.secureStorage.saveVoiceSettings(updated)
                                viewModel.voiceManager.applyVoiceSettings(updated)
                                viewModel.voiceManager.testVoice("JARVIS online, Commander. All systems calibrated.")
                            }
                        )
                        VoiceProfileCard(
                            title = "Python David",
                            subtitle = "🇺🇸 pyttsx3 SAPI5",
                            isSelected = voiceSettings.voiceType == "python_david_male" || voiceSettings.voiceType == "calm_natural",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val updated = voiceSettings.copy(voiceType = "python_david_male", pitch = 0.85f, speechRate = 1.0f)
                                voiceSettings = updated
                                viewModel.secureStorage.saveVoiceSettings(updated)
                                viewModel.voiceManager.applyVoiceSettings(updated)
                                viewModel.voiceManager.testVoice("System ready. Python local audio core initialized.")
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Voice Profile selection - Row 2
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        VoiceProfileCard(
                            title = "Deep Baritone",
                            subtitle = "⚡ Low Bass Male",
                            isSelected = voiceSettings.voiceType == "deep_baritone_male" || voiceSettings.voiceType == "deep_resonant",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val updated = voiceSettings.copy(voiceType = "deep_baritone_male", pitch = 0.75f, speechRate = 0.92f)
                                voiceSettings = updated
                                viewModel.secureStorage.saveVoiceSettings(updated)
                                viewModel.voiceManager.applyVoiceSettings(updated)
                                viewModel.voiceManager.testVoice("Deep resonance active. Command interface locked.")
                            }
                        )
                        VoiceProfileCard(
                            title = "Hinglish Male",
                            subtitle = "🇮🇳 Prabhat / en-IN",
                            isSelected = voiceSettings.voiceType == "hinglish_indian_male",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val updated = voiceSettings.copy(voiceType = "hinglish_indian_male", pitch = 0.88f, speechRate = 1.0f)
                                voiceSettings = updated
                                viewModel.secureStorage.saveVoiceSettings(updated)
                                viewModel.voiceManager.applyVoiceSettings(updated)
                                viewModel.voiceManager.testVoice("Namaste Commander. Main aapka JARVIS assistant hoon.")
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Voice Profile selection - Row 3
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        VoiceProfileCard(
                            title = "Cyber Synth",
                            subtitle = "🤖 Robotic AI Male",
                            isSelected = voiceSettings.voiceType == "cyber_robotic_male" || voiceSettings.voiceType == "smooth_neutral",
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                val updated = voiceSettings.copy(voiceType = "cyber_robotic_male", pitch = 0.68f, speechRate = 0.90f)
                                voiceSettings = updated
                                viewModel.secureStorage.saveVoiceSettings(updated)
                                viewModel.voiceManager.applyVoiceSettings(updated)
                                viewModel.voiceManager.testVoice("Neural synthesizer linked. Ready for instructions.")
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Realistic Laughter & Emotion Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Realistic Laughter Simulation (ChatGPT / Gemini style)",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Synthesizes expressive laughter cues, chuckles and pitch modulation",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                        Switch(
                            checked = voiceSettings.enableLaughterSimulation,
                            onCheckedChange = {
                                val updated = voiceSettings.copy(enableLaughterSimulation = it)
                                voiceSettings = updated
                                viewModel.secureStorage.saveVoiceSettings(updated)
                                viewModel.voiceManager.applyVoiceSettings(updated)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AtmosphericDarkSurface,
                                checkedTrackColor = CyanAtmospheric,
                                uncheckedTrackColor = AtmosphericGlassCard
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Speech Rate Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Voice Speed (Rate)", color = TextMuted, fontSize = 12.sp)
                        Text(text = "${String.format("%.2f", voiceSettings.speechRate)}x", color = CyanAtmospheric, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }
                    Slider(
                        value = voiceSettings.speechRate,
                        onValueChange = {
                            val updated = voiceSettings.copy(speechRate = it)
                            voiceSettings = updated
                            viewModel.secureStorage.saveVoiceSettings(updated)
                            viewModel.voiceManager.applyVoiceSettings(updated)
                        },
                        valueRange = 0.6f..1.4f,
                        colors = SliderDefaults.colors(
                            thumbColor = CyanAtmospheric,
                            activeTrackColor = CyanAtmospheric,
                            inactiveTrackColor = Color(0x2206B6D4)
                        )
                    )

                    // Speech Pitch Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Voice Pitch (Deepness / Frequency)", color = TextMuted, fontSize = 12.sp)
                        Text(text = "${String.format("%.2f", voiceSettings.pitch)}x", color = CyanAtmospheric, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }
                    Slider(
                        value = voiceSettings.pitch,
                        onValueChange = {
                            val updated = voiceSettings.copy(pitch = it)
                            voiceSettings = updated
                            viewModel.secureStorage.saveVoiceSettings(updated)
                            viewModel.voiceManager.applyVoiceSettings(updated)
                        },
                        valueRange = 0.5f..1.3f,
                        colors = SliderDefaults.colors(
                            thumbColor = CyanAtmospheric,
                            activeTrackColor = CyanAtmospheric,
                            inactiveTrackColor = Color(0x2206B6D4)
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    GlowingButton(
                        text = "🔊 Test Male Voice Preview",
                        icon = Icons.Default.PlayArrow,
                        isPrimary = false,
                        onClick = {
                            val sample = when (voiceSettings.voiceType) {
                                "hinglish_indian_male" -> "Namaste Commander, JARVIS local male voice engine active."
                                "deep_baritone_male", "deep_resonant" -> "JARVIS deep resonance online. All systems operational."
                                "cyber_robotic_male" -> "Cyber synthesizer active. Standing by for command."
                                "python_david_male", "calm_natural" -> "Python David voice initialized. Ready to assist you."
                                else -> "Greetings Commander. I am JARVIS, created by Roller_gaming."
                            }
                            viewModel.voiceManager.testVoice(sample)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4. Device Automation & Accessibility
            SectionHeader(title = "DEVICE AUTOMATION", icon = Icons.Default.Security)
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                backgroundColor = AtmosphericGlass,
                borderColor = GlassBorder
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val isAccEnabled = viewModel.isAccessibilityEnabled()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Screen Inspection & UI Control",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (isAccEnabled) "Service is ACTIVE. Screen reading & app controls enabled." else "Service is DISABLED. Tap below to enable in Android Settings.",
                                color = if (isAccEnabled) StatusOnline else StatusError,
                                fontSize = 11.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isAccEnabled) StatusOnline.copy(alpha = 0.15f) else StatusError.copy(alpha = 0.15f))
                                .border(1.dp, if (isAccEnabled) StatusOnline else StatusError, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isAccEnabled) "ENABLED" else "DISABLED",
                                color = if (isAccEnabled) StatusOnline else StatusError,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    GlowingButton(
                        text = if (isAccEnabled) "Manage Accessibility Settings" else "Enable JARVIS Automation Service",
                        isPrimary = !isAccEnabled,
                        onClick = { viewModel.openAccessibilitySettings() },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 5. Chat & Memory Management
            SectionHeader(title = "DATA & MEMORY", icon = Icons.Default.Delete)
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                backgroundColor = AtmosphericGlass,
                borderColor = GlassBorder
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Local Chat Storage",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "All dialogues are saved strictly on-device using Room SQLite.",
                        color = TextMuted,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    GlowingButton(
                        text = "Clear All Memory & Reset",
                        icon = Icons.Default.Delete,
                        isPrimary = false,
                        onClick = { showClearDataDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 5. About & Developer Section
            SectionHeader(title = "ABOUT & TRANSPARENCY", icon = Icons.Default.Security)
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                backgroundColor = AtmosphericGlass,
                borderColor = GlassBorder
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "JARVIS — Created by Roller_gaming",
                        color = CyanAtmospheric,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Developer: Roller_gaming",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Active AI Provider: ${activeProvider.displayName} (${selectedModel})",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Encryption: Hardware Keystore AES-256 GCM\nLocal Database: Room Persistence Architecture\nVoice: Local Built-in Android Audio Synthesis",
                        color = TextDarker,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }

        // Edit API Key Dialog
        if (showKeyEditDialog) {
            AlertDialog(
                onDismissRequest = { showKeyEditDialog = false },
                containerColor = AtmosphericDarkSurface,
                title = {
                    Text(
                        text = "Update API Key (${activeProvider.displayName})",
                        color = CyanAtmospheric,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Enter your new API key. It will be encrypted into the Android Keystore.",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = newApiKeyInput,
                            onValueChange = { newApiKeyInput = it },
                            placeholder = { Text("Paste API Key here...", color = TextDarker) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanAtmospheric,
                                unfocusedBorderColor = GlassBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (newApiKeyInput.isNotBlank()) {
                            viewModel.validateAndSaveApiKey(activeProvider, newApiKeyInput) { success, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                if (success) {
                                    showKeyEditDialog = false
                                }
                            }
                        }
                    }) {
                        Text("Validate & Save", color = CyanAtmospheric, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showKeyEditDialog = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            )
        }

        // Clear All Data Confirmation Dialog
        if (showClearDataDialog) {
            AlertDialog(
                onDismissRequest = { showClearDataDialog = false },
                containerColor = AtmosphericDarkSurface,
                title = {
                    Text(
                        text = "Purge All Data?",
                        color = StatusError,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = "This will permanently wipe all local chat history, keys, and session data. Are you sure?",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        showClearDataDialog = false
                        viewModel.clearAllData()
                    }) {
                        Text("Purge Everything", color = StatusError, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDataDialog = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CyanAtmospheric,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            color = CyanAtmospheric,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun ProviderPill(
    name: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) CyanCore.copy(alpha = 0.25f) else AtmosphericGlassCard)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) CyanAtmospheric else GlassBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name,
            color = if (isSelected) CyanAtmospheric else TextPrimary,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun VoiceProfileCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) CyanCore.copy(alpha = 0.22f) else AtmosphericGlassCard)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) CyanAtmospheric else GlassBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    color = if (isSelected) CyanAtmospheric else TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                )
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(CyanAtmospheric)
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = if (isSelected) TextPrimary.copy(alpha = 0.8f) else TextMuted,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun VoiceTonePill(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) CyanCore.copy(alpha = 0.2f) else AtmosphericGlassCard)
            .border(
                width = if (isSelected) 1.2.dp else 0.8.dp,
                color = if (isSelected) CyanAtmospheric else GlassBorder,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) CyanAtmospheric else TextSecondary,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

