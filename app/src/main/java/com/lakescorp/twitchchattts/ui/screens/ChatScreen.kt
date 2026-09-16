package com.lakescorp.twitchchattts.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lakescorp.twitchchattts.ChatViewModel
import com.lakescorp.twitchchattts.R
import com.lakescorp.twitchchattts.data.TwitchIrcClient
import com.lakescorp.twitchchattts.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val channel by viewModel.channel.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val chatHistory by viewModel.chatHistory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()

    // TTS Quick Controls
    val pitch by viewModel.pitch.collectAsState()
    val rate by viewModel.rate.collectAsState()
    val volume by viewModel.volume.collectAsState()

    var showQuickControls by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // Filtered Chat History
    val filteredChat = remember(chatHistory, searchQuery) {
        if (searchQuery.isEmpty()) {
            chatHistory
        } else {
            chatHistory.filter {
                it.displayName.contains(searchQuery, ignoreCase = true) ||
                        it.messageText.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // Auto Scroll to Bottom on New Message
    LaunchedEffect(filteredChat.size) {
        if (filteredChat.isNotEmpty()) {
            listState.animateScrollToItem(filteredChat.lastIndex)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "#$channel",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextLight,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showQuickControls = !showQuickControls }) {
                        Icon(
                            imageVector = if (showQuickControls) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.Tune,
                            contentDescription = stringResource(id = R.string.cd_quick_audio_tuning),
                            tint = if (showQuickControls) TwitchPurpleLight else TextLight
                        )
                    }

                    IconButton(onClick = { viewModel.toggleMute() }) {
                        Icon(
                            imageVector = if (isMuted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                            contentDescription = stringResource(id = if (isMuted) R.string.cd_unmute_speech else R.string.cd_mute_speech),
                            tint = if (isMuted) AlertRed else AlertGreen
                        )
                    }

                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = stringResource(id = R.string.cd_settings),
                            tint = TextLight
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface
                )
            )
        },
        containerColor = DarkBackground,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Hero Connection & Play/Pause Banner
            HeroConnectionBanner(
                connectionState = connectionState,
                onPause = { viewModel.disconnect() },
                onResume = { viewModel.reconnect() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text(stringResource(id = R.string.filter_logs), color = TextMuted) },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = stringResource(id = R.string.cd_search),
                        tint = TextMuted
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(
                                imageVector = Icons.Rounded.Clear,
                                contentDescription = stringResource(id = R.string.cd_clear_search),
                                tint = TextMuted
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextLight,
                    unfocusedTextColor = TextLight,
                    focusedBorderColor = TwitchPurple,
                    unfocusedBorderColor = BorderColor,
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface
                ),
                shape = RoundedCornerShape(24.dp)
            )

            // Quick Audio Controls Drawer overlay
            AnimatedVisibility(
                visible = showQuickControls,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.linearGradient(listOf(TwitchPurple, BorderColor))
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(id = R.string.quick_tts_settings),
                                fontWeight = FontWeight.Bold,
                                color = TwitchPurpleLight,
                                fontSize = 13.sp
                            )
                            TextButton(
                                onClick = {
                                    viewModel.setPitch(1.0f)
                                    viewModel.setRate(1.0f)
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text(
                                    text = stringResource(id = R.string.reset_audio),
                                    color = TwitchPurpleLight,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Pitch
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(id = R.string.speech_pitch),
                                modifier = Modifier.width(90.dp),
                                color = TextLight,
                                fontSize = 12.sp
                            )
                            Slider(
                                value = pitch,
                                onValueChange = { viewModel.setPitch(it) },
                                valueRange = 0.5f..2.0f,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(activeTrackColor = TwitchPurple, thumbColor = TwitchPurple)
                            )
                            Text(
                                text = stringResource(id = R.string.pitch_format, pitch),
                                color = TwitchPurpleLight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(44.dp),
                                textAlign = TextAlign.End
                            )
                        }

                        // Speed
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(id = R.string.speech_rate),
                                modifier = Modifier.width(90.dp),
                                color = TextLight,
                                fontSize = 12.sp
                            )
                            Slider(
                                value = rate,
                                onValueChange = { viewModel.setRate(it) },
                                valueRange = 0.5f..2.0f,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(activeTrackColor = TwitchPurple, thumbColor = TwitchPurple)
                            )
                            Text(
                                text = stringResource(id = R.string.speed_format, rate),
                                color = TwitchPurpleLight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(44.dp),
                                textAlign = TextAlign.End
                            )
                        }

                        // Volume
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(id = R.string.tts_volume),
                                modifier = Modifier.width(90.dp),
                                color = TextLight,
                                fontSize = 12.sp
                            )
                            Slider(
                                value = volume,
                                onValueChange = { viewModel.setVolume(it) },
                                valueRange = 0.0f..1.0f,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(activeTrackColor = TwitchPurple, thumbColor = TwitchPurple)
                            )
                            Text(
                                text = stringResource(id = R.string.volume_format, (volume * 100).toInt()),
                                color = TwitchPurpleLight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(44.dp),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            }

            // Chat Logs
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(filteredChat) { chatMessage ->
                        ChatMessageItem(chatMessage)
                    }
                }

                if (filteredChat.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (searchQuery.isNotEmpty()) {
                            Text(
                                text = stringResource(id = R.string.no_matching_results),
                                color = TextMuted,
                                fontSize = 14.sp
                            )
                        } else if (connectionState is ChatViewModel.ConnectionState.Disconnected ||
                            connectionState is ChatViewModel.ConnectionState.Error
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = if (connectionState is ChatViewModel.ConnectionState.Error) {
                                        stringResource(id = R.string.error_status)
                                    } else {
                                        stringResource(id = R.string.offline_status)
                                    },
                                    color = TextMuted,
                                    fontSize = 14.sp
                                )
                                Button(
                                    onClick = { viewModel.reconnect() },
                                    colors = ButtonDefaults.buttonColors(containerColor = TwitchPurple),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Rounded.PlayArrow, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(stringResource(id = R.string.start_listening), fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            Text(
                                text = stringResource(id = R.string.chat_logs_appear),
                                color = TextMuted,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HeroConnectionBanner(
    connectionState: ChatViewModel.ConnectionState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isConnected = connectionState is ChatViewModel.ConnectionState.Connected
    val isConnecting = connectionState is ChatViewModel.ConnectionState.Connecting
    val isError = connectionState is ChatViewModel.ConnectionState.Error

    val cardBorder = when {
        isConnected -> AlertGreen.copy(alpha = 0.35f)
        isConnecting -> TwitchPurple.copy(alpha = 0.35f)
        isError -> AlertRed.copy(alpha = 0.35f)
        else -> BorderColor
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = DarkSurface,
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(cardBorder)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                StatusIndicator(connectionState)
                val errorFallback = stringResource(id = R.string.error_status)
                val statusText = when (connectionState) {
                    is ChatViewModel.ConnectionState.Connected -> stringResource(id = R.string.banner_listening)
                    is ChatViewModel.ConnectionState.Connecting -> stringResource(id = R.string.connecting_status)
                    is ChatViewModel.ConnectionState.Disconnected -> stringResource(id = R.string.banner_paused)
                    is ChatViewModel.ConnectionState.Error -> connectionState.message.ifEmpty { errorFallback }
                }
                Text(
                    text = statusText,
                    color = when {
                        isConnected -> AlertGreen
                        isConnecting -> TwitchPurpleLight
                        isError -> AlertRed
                        else -> TextMuted
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isConnected || isConnecting) {
                OutlinedButton(
                    onClick = onPause,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = AlertRed
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(AlertRed.copy(alpha = 0.5f))
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Pause,
                        contentDescription = stringResource(id = R.string.pause_btn),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(id = R.string.pause_btn),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Button(
                    onClick = onResume,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TwitchPurple,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(
                        imageVector = if (isError) Icons.Rounded.Refresh else Icons.Rounded.PlayArrow,
                        contentDescription = stringResource(id = if (isError) R.string.retry_btn else R.string.resume_btn),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(id = if (isError) R.string.retry_btn else R.string.resume_btn),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun StatusIndicator(connectionState: ChatViewModel.ConnectionState) {
    // Only animate when there is an active connection — avoids 60fps Choreographer
    // callbacks when Disconnected or Error (where the pulse ring is not rendered).
    val isActive = connectionState is ChatViewModel.ConnectionState.Connected ||
            connectionState is ChatViewModel.ConnectionState.Connecting

    val scale = if (isActive) {
        val infiniteTransition = rememberInfiniteTransition(label = "Pulse")
        infiniteTransition.animateFloat(
            initialValue = 0.7f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "Scale"
        ).value
    } else {
        1.0f
    }

    val color = when (connectionState) {
        is ChatViewModel.ConnectionState.Connected -> AlertGreen
        is ChatViewModel.ConnectionState.Connecting -> AlertOrange
        is ChatViewModel.ConnectionState.Disconnected -> AlertRed
        is ChatViewModel.ConnectionState.Error -> AlertRed
    }

    Box(
        modifier = Modifier
            .size(16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isActive) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.4f * scale))
            )
        }
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
    }
}

@Composable
fun ChatMessageItem(message: TwitchIrcClient.TwitchChatMessage) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                if (message.isBroadcaster) {
                    BadgeTag(stringResource(id = R.string.badge_broadcaster), Color(0xFFE91E63))
                } else if (message.isMod) {
                    BadgeTag(stringResource(id = R.string.badge_moderator), Color(0xFF00AD82))
                } else if (message.isSub) {
                    BadgeTag(stringResource(id = R.string.badge_subscriber), TwitchPurpleLight)
                }

                Text(
                    text = message.displayName,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        message.isBroadcaster -> Color(0xFFFF4081)
                        message.isMod -> Color(0xFF10B981)
                        message.isSub -> TwitchPurpleLight
                        else -> TextLight
                    },
                    fontSize = 14.sp
                )
            }

            Text(
                text = message.messageText,
                color = TextLight,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun BadgeTag(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(4.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(color.copy(alpha = 0.4f))
        )
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
        )
    }
}
