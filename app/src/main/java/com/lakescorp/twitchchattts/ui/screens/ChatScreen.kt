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
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatusIndicator(connectionState)

                        Column {
                            Text(
                                text = "twitch.tv/$channel",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextLight,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = when (connectionState) {
                                    is ChatViewModel.ConnectionState.Connected -> stringResource(id = R.string.connected_status)
                                    is ChatViewModel.ConnectionState.Connecting -> stringResource(id = R.string.connecting_status)
                                    is ChatViewModel.ConnectionState.Disconnected -> stringResource(id = R.string.offline_status)
                                    is ChatViewModel.ConnectionState.Error -> stringResource(id = R.string.error_status)
                                },
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showQuickControls = !showQuickControls }) {
                        Icon(
                            imageVector = if (showQuickControls) Icons.Default.KeyboardArrowUp else Icons.Default.Tune,
                            contentDescription = "Quick Audio Tuning",
                            tint = if (showQuickControls) TwitchPurpleLight else TextLight
                        )
                    }

                    IconButton(onClick = { viewModel.toggleMute() }) {
                        Icon(
                            imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = if (isMuted) "Unmute Speech" else "Mute Speech",
                            tint = if (isMuted) AlertRed else AlertGreen
                        )
                    }

                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
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
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text(stringResource(id = R.string.filter_logs), color = TextMuted) },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = TextMuted
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search",
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
                        Text(stringResource(id = R.string.quick_tts_settings), fontWeight = FontWeight.Bold, color = TwitchPurpleLight, fontSize = 13.sp)

                        // Pitch
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(id = R.string.speech_pitch), modifier = Modifier.width(100.dp), color = TextLight, fontSize = 12.sp)
                            Slider(
                                value = pitch,
                                onValueChange = { viewModel.setPitch(it) },
                                valueRange = 0.5f..2.0f,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(activeTrackColor = TwitchPurple, thumbColor = TwitchPurple)
                            )
                        }

                        // Speed
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(id = R.string.speech_rate), modifier = Modifier.width(100.dp), color = TextLight, fontSize = 12.sp)
                            Slider(
                                value = rate,
                                onValueChange = { viewModel.setRate(it) },
                                valueRange = 0.5f..2.0f,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(activeTrackColor = TwitchPurple, thumbColor = TwitchPurple)
                            )
                        }

                        // Volume
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(id = R.string.tts_volume), modifier = Modifier.width(100.dp), color = TextLight, fontSize = 12.sp)
                            Slider(
                                value = volume,
                                onValueChange = { viewModel.setVolume(it) },
                                valueRange = 0.0f..1.0f,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(activeTrackColor = TwitchPurple, thumbColor = TwitchPurple)
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
                        Text(
                            text = if (searchQuery.isNotEmpty()) stringResource(id = R.string.no_matching_results) else stringResource(id = R.string.chat_logs_appear),
                            color = TextMuted,
                            fontSize = 14.sp
                        )
                    }
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
                    BadgeTag("Broadcaster", Color(0xFFE91E63))
                } else if (message.isMod) {
                    BadgeTag("Mod", Color(0xFF00AD82))
                } else if (message.isSub) {
                    BadgeTag("Sub", TwitchPurpleLight)
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
