package com.lakescorp.twitchchattts

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.lakescorp.twitchchattts.data.auth.AuthManager
import com.lakescorp.twitchchattts.ui.screens.ChatScreen
import com.lakescorp.twitchchattts.ui.screens.LoginScreen
import com.lakescorp.twitchchattts.ui.screens.SettingsScreen
import com.lakescorp.twitchchattts.ui.theme.TwitchChatTTSTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: ChatViewModel by viewModels()

    enum class Screen { Login, Chat, Settings }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)

        val isScreenshotMode = intent?.getBooleanExtra("screenshot_mode", false) == true
        val targetScreenStr = intent?.getStringExtra("target_screen") ?: "Login"
        val screenshotLang = intent?.getStringExtra("lang") ?: "en-US"

        if (isScreenshotMode) {
            viewModel.injectMockData(screenshotLang)
        }

        setContent {
            TwitchChatTTSTheme {
                val startScreen = remember {
                    if (isScreenshotMode) {
                        try {
                            Screen.valueOf(targetScreenStr)
                        } catch (e: Exception) {
                            Screen.Login
                        }
                    } else {
                        Screen.Login
                    }
                }
                var currentScreen by remember { mutableStateOf(startScreen) }
                val loginState by viewModel.loginState.collectAsState()

                LaunchedEffect(loginState) {
                    if (isScreenshotMode) return@LaunchedEffect
                    when (loginState) {
                        is AuthManager.LoginState.Success -> {
                            if (currentScreen == Screen.Login) currentScreen = Screen.Chat
                        }
                        is AuthManager.LoginState.Idle -> currentScreen = Screen.Login
                        else -> {}
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (currentScreen) {
                        Screen.Login -> LoginScreen(
                            viewModel = viewModel,
                            modifier = Modifier.fillMaxSize()
                        )
                        Screen.Chat -> ChatScreen(
                            viewModel = viewModel,
                            onNavigateToSettings = { currentScreen = Screen.Settings },
                            modifier = Modifier.fillMaxSize()
                        )
                        Screen.Settings -> SettingsScreen(
                            viewModel = viewModel,
                            onBack = { currentScreen = Screen.Chat },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val data = intent?.dataString
        if (data != null && data.startsWith("http://localhost")) {
            viewModel.handleDeepLink(data)
        }
    }
}
