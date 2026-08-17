package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.local.entity.ChatMessageEntity
import com.example.data.model.OrbState
import com.example.ui.JarvisViewModel
import com.example.ui.Screen
import com.example.ui.components.CyberBackgroundLayout
import com.example.ui.components.FuturisticOrb
import com.example.ui.components.GlassCard
import com.example.ui.theme.AtmosphericDark
import com.example.ui.theme.AtmosphericGlass
import com.example.ui.theme.AtmosphericGlassCard
import com.example.ui.theme.CyanAtmospheric
import com.example.ui.theme.CyanCore
import com.example.ui.theme.CyanHighlight
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassBorderActive
import com.example.ui.theme.GlassBorderCyan
import com.example.ui.theme.GlassBorderSubtle
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusOnline
import com.example.ui.theme.StatusThinking
import com.example.ui.theme.TealAccent
import com.example.ui.theme.TextDarker
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun ChatScreen(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()

    val messages by viewModel.messages.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val statusText by viewModel.statusText.collectAsState()
    val isListening by viewModel.voiceManager.isListening.collectAsState()
    val isSpeaking by viewModel.voiceManager.isSpeaking.collectAsState()
    val audioRms by viewModel.voiceManager.audioRms.collectAsState()
    val recognizedText by viewModel.voiceManager.recognizedText.collectAsState()

    var textInput by remember { mutableStateOf("") }

    val orbState = viewModel.computeOrbState(
        isListening = isListening,
        isSpeaking = isSpeaking,
        isGenerating = isGenerating
    )

    // Permission launcher for microphone
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startVoiceInput()
        } else {
            Toast.makeText(context, "Microphone permission required for voice command.", Toast.LENGTH_SHORT).show()
        }
    }

    // Scroll to bottom on new messages
    LaunchedEffect(messages.size, isGenerating) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    CyberBackgroundLayout(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            // Atmospheric Top Navigation & Status Bar
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(20.dp),
                borderWidth = 1.dp,
                borderColor = GlassBorder
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Pulsing status dot
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(
                                    when (orbState) {
                                        OrbState.THINKING -> StatusThinking
                                        OrbState.LISTENING -> CyanAtmospheric
                                        OrbState.SPEAKING -> TealAccent
                                        OrbState.IDLE -> StatusOnline
                                    }
                                )
                                .shadow(4.dp, CircleShape, ambientColor = CyanAtmospheric, spotColor = CyanAtmospheric)
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = "JARVIS AI TERMINAL",
                                color = CyanAtmospheric,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = statusText,
                                color = TextMuted,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Stop speaking button if TTS is speaking
                        if (isSpeaking) {
                            IconButton(
                                onClick = { viewModel.stopSpeaking() },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(StatusThinking.copy(alpha = 0.2f))
                                    .border(1.dp, StatusThinking.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = "Stop Speech",
                                    tint = StatusThinking,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        // Clear Chat Action
                        IconButton(
                            onClick = { viewModel.clearCurrentChat() },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(AtmosphericGlass)
                                .border(1.dp, GlassBorder, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear Chat",
                                tint = TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Settings Navigation
                        IconButton(
                            onClick = { viewModel.navigateTo(Screen.Settings) },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(AtmosphericGlass)
                                .border(1.dp, GlassBorder, CircleShape)
                                .testTag("settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = CyanAtmospheric,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Compact Central Holographic Orb HUD Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                FuturisticOrb(
                    state = orbState,
                    audioRms = audioRms,
                    size = if (messages.isEmpty()) 130.dp else 68.dp,
                    onClick = {
                        if (isSpeaking) {
                            viewModel.stopSpeaking()
                        } else if (isListening) {
                            viewModel.stopVoiceInput()
                        } else {
                            val hasPerm = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasPerm) {
                                viewModel.startVoiceInput()
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    }
                )
            }

            // Voice Listening Live Transcript Overlay
            AnimatedVisibility(
                visible = isListening,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut()
            ) {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(14.dp),
                    borderColor = CyanAtmospheric
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = CyanAtmospheric,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (recognizedText.isNotBlank()) recognizedText else "Listening to your voice...",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Chat Messages Stream
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (messages.isEmpty()) {
                    EmptyStateWelcome(
                        onSelectPrompt = { prompt ->
                            viewModel.sendMessage(prompt)
                        }
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(messages, key = { it.id }) { message ->
                            ChatMessageBubble(
                                message = message,
                                onCopy = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("JARVIS Message", message.content)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                },
                                onSpeak = {
                                    viewModel.speakMessage(message.content)
                                }
                            )
                        }
                    }
                }
            }

            // Quick Prompt Chips & Automation Triggers
            if (messages.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val quickPrompts = listOf(
                        "⚡ 5s baad YouTube kholo",
                        "📱 Screen analyze karo",
                        "🔍 Search on YouTube",
                        "🤖 Who created you?",
                        "💡 Write title & description"
                    )
                    items(quickPrompts) { prompt ->
                        val cleanPrompt = prompt.replace(Regex("^[⚡📱🔍🤖💡]\\s*"), "")
                        QuickChip(text = prompt, onClick = { viewModel.sendMessage(cleanPrompt) })
                    }
                }
            }

            // Atmospheric Bottom Input Bar (rounded-[32px] with glow)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(AtmosphericGlass)
                    .border(
                        width = 1.dp,
                        color = if (isGenerating) StatusThinking else GlassBorder,
                        shape = RoundedCornerShape(28.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Voice Mic Button
                    IconButton(
                        onClick = {
                            if (isListening) {
                                viewModel.stopVoiceInput()
                            } else {
                                val hasPerm = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasPerm) {
                                    viewModel.startVoiceInput()
                                } else {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (isListening) CyanAtmospheric.copy(alpha = 0.25f) else AtmosphericGlassCard)
                            .border(1.dp, if (isListening) CyanAtmospheric else GlassBorderSubtle, CircleShape)
                            .testTag("voice_input_button")
                    ) {
                        Icon(
                            imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Voice Input",
                            tint = if (isListening) CyanAtmospheric else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Text Input
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = {
                            Text(
                                text = "Ask JARVIS anything...",
                                color = TextDarker,
                                fontSize = 14.sp
                            )
                        },
                        singleLine = false,
                        maxLines = 4,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (textInput.isNotBlank() && !isGenerating) {
                                viewModel.sendMessage(textInput)
                                textInput = ""
                                focusManager.clearFocus()
                            }
                        }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = CyanAtmospheric
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_text_input")
                    )

                    // Send Button
                    IconButton(
                        onClick = {
                            if (textInput.isNotBlank() && !isGenerating) {
                                viewModel.sendMessage(textInput)
                                textInput = ""
                                focusManager.clearFocus()
                            }
                        },
                        enabled = textInput.isNotBlank() && !isGenerating,
                        modifier = Modifier
                            .size(42.dp)
                            .shadow(
                                elevation = if (textInput.isNotBlank()) 8.dp else 0.dp,
                                shape = CircleShape,
                                ambientColor = CyanAtmospheric,
                                spotColor = CyanAtmospheric
                            )
                            .clip(CircleShape)
                            .background(
                                if (textInput.isNotBlank() && !isGenerating) {
                                    Brush.horizontalGradient(listOf(CyanAtmospheric, CyanCore))
                                } else {
                                    Brush.horizontalGradient(listOf(AtmosphericGlassCard, AtmosphericGlass))
                                }
                            )
                            .testTag("send_button")
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = AtmosphericDark,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Send",
                                tint = if (textInput.isNotBlank()) AtmosphericDark else TextDarker,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(
    message: ChatMessageEntity,
    onCopy: () -> Unit,
    onSpeak: () -> Unit
) {
    val isUser = message.sender == "user"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            // JARVIS Avatar Reticle Badge
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(CyanCore.copy(alpha = 0.15f))
                    .border(1.dp, CyanAtmospheric.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "J",
                    color = CyanAtmospheric,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.fillMaxWidth(if (isUser) 0.85f else 0.88f),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            // Sender label
            Text(
                text = if (isUser) "YOU" else "JARVIS // ASSISTANT",
                color = if (isUser) CyanAtmospheric.copy(alpha = 0.8f) else TextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )

            // Message Card matching Atmospheric glassmorphic bubbles
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 16.dp
                        )
                    )
                    .background(
                        if (isUser) {
                            // User bubble: Atmospheric subtle glass
                            Brush.linearGradient(
                                listOf(
                                    AtmosphericGlassCard,
                                    AtmosphericGlass
                                )
                            )
                        } else {
                            // Assistant bubble: Cyan subtle luminous glass
                            Brush.linearGradient(
                                listOf(
                                    CyanCore.copy(alpha = 0.14f),
                                    CyanAtmospheric.copy(alpha = 0.06f)
                                )
                            )
                        }
                    )
                    .border(
                        width = 1.dp,
                        color = if (message.isError) {
                            StatusError
                        } else if (isUser) {
                            GlassBorder
                        } else {
                            GlassBorderCyan
                        },
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 16.dp
                        )
                    )
                    .padding(14.dp)
            ) {
                Column {
                    Text(
                        text = message.content,
                        color = if (message.isError) StatusError else if (isUser) TextSecondary else TextPrimary,
                        fontSize = 14.sp,
                        lineHeight = 21.sp
                    )

                    if (message.isStreaming) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                color = CyanAtmospheric,
                                strokeWidth = 1.5.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Transmitting response stream...",
                                color = TextMuted,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Action buttons for Assistant messages
                    if (!isUser && !message.isStreaming && !message.isError) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(
                                onClick = onSpeak,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = "Speak Response",
                                    tint = CyanAtmospheric,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = onCopy,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy Message",
                                    tint = TextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyStateWelcome(
    onSelectPrompt: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "JARVIS IS ONLINE",
            color = CyanAtmospheric,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 4.sp,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Created by Roller_gaming",
            color = TextMuted,
            fontSize = 12.sp,
            letterSpacing = 1.5.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Voice, device automation, & neural reasoning active.",
            color = TextDarker,
            fontSize = 11.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        Column(
            modifier = Modifier.fillMaxWidth(0.96f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PromptSuggestionCard(
                title = "⚡ Time-Delayed Automation",
                prompt = "5 second baad YouTube kholo",
                onClick = { onSelectPrompt("5 second baad YouTube kholo") }
            )
            PromptSuggestionCard(
                title = "📱 Screen Analysis",
                prompt = "Screen analyze karo aur batao kya dikh raha hai",
                onClick = { onSelectPrompt("Screen analyze karo aur batao kya dikh raha hai") }
            )
            PromptSuggestionCard(
                title = "🔍 In-App Search",
                prompt = "Search on YouTube: best lo-fi coding music",
                onClick = { onSelectPrompt("Search on YouTube: best lo-fi coding music") }
            )
            PromptSuggestionCard(
                title = "🤖 Creator & Identity",
                prompt = "Who created you and what are your capabilities?",
                onClick = { onSelectPrompt("Who created you and what are your capabilities?") }
            )
        }
    }
}

@Composable
private fun PromptSuggestionCard(
    title: String,
    prompt: String,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        backgroundColor = AtmosphericGlass,
        borderColor = GlassBorder
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(CyanCore.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = CyanAtmospheric,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = prompt,
                    color = TextMuted,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun QuickChip(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(AtmosphericGlass)
            .border(1.dp, GlassBorderCyan, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(
            text = text,
            color = TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

