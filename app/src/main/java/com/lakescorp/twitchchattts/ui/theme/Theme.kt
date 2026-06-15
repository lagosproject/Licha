package com.lakescorp.twitchchattts.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = TwitchPurple,
    secondary = TwitchPurpleLight,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = TextLight,
    onSecondary = TextLight,
    onBackground = TextLight,
    onSurface = TextLight,
    surfaceVariant = DarkCard
)

@Composable
fun TwitchChatTTSTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
