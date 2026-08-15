package com.example.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.AISetupScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen

@Composable
fun JarvisApp(
    viewModel: JarvisViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val currentScreen by viewModel.currentScreen.collectAsState()

    Crossfade(
        targetState = currentScreen,
        animationSpec = tween(durationMillis = 350),
        label = "ScreenCrossfade",
        modifier = modifier.fillMaxSize()
    ) { screen ->
        when (screen) {
            is Screen.Splash -> SplashScreen(viewModel = viewModel)
            is Screen.Auth -> AuthScreen(viewModel = viewModel)
            is Screen.AISetup -> AISetupScreen(viewModel = viewModel)
            is Screen.Chat -> ChatScreen(viewModel = viewModel)
            is Screen.Settings -> SettingsScreen(viewModel = viewModel)
        }
    }
}
