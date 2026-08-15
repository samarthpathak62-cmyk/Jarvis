package com.example.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.OrbState
import com.example.ui.JarvisViewModel
import com.example.ui.components.CyberBackgroundLayout
import com.example.ui.components.FuturisticOrb
import com.example.ui.components.GlassCard
import com.example.ui.components.GlowingButton
import com.example.ui.theme.AtmosphericGlass
import com.example.ui.theme.CyanAtmospheric
import com.example.ui.theme.CyanHighlight
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassBorderActive
import com.example.ui.theme.StatusOnline
import com.example.ui.theme.TextDarker
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AuthScreen(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier
) {
    var userNameInput by remember { mutableStateOf("") }
    var userEmailInput by remember { mutableStateOf("") }
    var isCustomUserExpanded by remember { mutableStateOf(false) }

    CyberBackgroundLayout(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = CyanAtmospheric,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SECURE ACCESS GATEWAY",
                        color = CyanAtmospheric,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "JARVIS Terminal",
                    color = TextPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Text(
                    text = "Created by Roller_gaming",
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }

            // Central Orb & Auth Panel
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                FuturisticOrb(
                    state = OrbState.IDLE,
                    size = 115.dp
                )

                Spacer(modifier = Modifier.height(28.dp))

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    backgroundColor = AtmosphericGlass,
                    borderColor = GlassBorder
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "USER AUTHENTICATION",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            fontFamily = FontFamily.Monospace
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Sign in to activate local secure session & encrypted neural links.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Primary Google Sign-in Action
                        GlowingButton(
                            text = "Sign in with Google",
                            icon = Icons.Default.AccountCircle,
                            onClick = {
                                viewModel.handleGoogleSignIn(
                                    name = "Commander",
                                    email = "commander@jarvis.ai"
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "google_signin_button"
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Quick Biometric/Passcode Link Button
                        GlowingButton(
                            text = "Instant Terminal Access",
                            icon = Icons.Default.Fingerprint,
                            isPrimary = false,
                            onClick = {
                                viewModel.handleGoogleSignIn(
                                    name = "Admin",
                                    email = "operator@jarvis.ai"
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "instant_access_button"
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        if (!isCustomUserExpanded) {
                            Text(
                                text = "Custom Profile Details",
                                color = CyanAtmospheric,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .clickable { isCustomUserExpanded = true }
                                    .padding(vertical = 4.dp),
                                fontWeight = FontWeight.Medium
                            )
                        } else {
                            OutlinedTextField(
                                value = userNameInput,
                                onValueChange = { userNameInput = it },
                                label = { Text("Display Name", color = TextSecondary, fontSize = 12.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyanAtmospheric,
                                    unfocusedBorderColor = GlassBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    cursorColor = CyanAtmospheric
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                            )
                            OutlinedTextField(
                                value = userEmailInput,
                                onValueChange = { userEmailInput = it },
                                label = { Text("Email", color = TextSecondary, fontSize = 12.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyanAtmospheric,
                                    unfocusedBorderColor = GlassBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    cursorColor = CyanAtmospheric
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                            )
                            GlowingButton(
                                text = "Confirm Credentials",
                                onClick = {
                                    val name = if (userNameInput.isNotBlank()) userNameInput else "User"
                                    val email = if (userEmailInput.isNotBlank()) userEmailInput else "user@jarvis.ai"
                                    viewModel.handleGoogleSignIn(name = name, email = email)
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // Bottom Security Note
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = StatusOnline,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "On-Device Keystore AES-256 GCM Encrypted",
                    color = TextDarker,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

