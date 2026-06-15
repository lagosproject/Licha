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

        setContent {
            TwitchChatTTSTheme {
                var currentScreen by remember { mutableStateOf(Screen.Login) }
                val loginState by viewModel.loginState.collectAsState()

                LaunchedEffect(loginState) {
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
