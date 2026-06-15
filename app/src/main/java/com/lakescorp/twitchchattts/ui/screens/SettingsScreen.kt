package com.lakescorp.twitchchattts.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lakescorp.twitchchattts.ChatViewModel
import com.lakescorp.twitchchattts.R
import com.lakescorp.twitchchattts.ui.theme.*

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ChatViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // State Collection
    val channel by viewModel.channel.collectAsState()
    val ignoreNormal by viewModel.ignoreNormal.collectAsState()
    val ignoreSubs by viewModel.ignoreSubs.collectAsState()
    val ignoreMods by viewModel.ignoreMods.collectAsState()
    val ignoredUsers by viewModel.ignoredUsers.collectAsState()

    val pitch by viewModel.pitch.collectAsState()
    val rate by viewModel.rate.collectAsState()
    val volume by viewModel.volume.collectAsState()
    val selectedVoiceName by viewModel.selectedVoiceName.collectAsState()
    val availableVoices by viewModel.availableVoices.collectAsState()

    var channelInput by remember { mutableStateOf(channel) }
    var ignoredUserInput by remember { mutableStateOf("") }
    var dropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(channel) {
        channelInput = channel
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.settings), fontWeight = FontWeight.Bold, color = TextLight) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
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
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // SECTION 0: TARGET TWITCH CHANNEL TO LISTEN TO
            Text(
                text = stringResource(id = R.string.target_channel),
                style = MaterialTheme.typography.titleMedium,
                color = TwitchPurpleLight,
                fontWeight = FontWeight.Bold
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(id = R.string.listen_to_channel),
                        fontSize = 13.sp,
                        color = TextMuted,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = channelInput,
                            onValueChange = { channelInput = it },
                            placeholder = { Text(stringResource(id = R.string.channel_name_placeholder), color = TextMuted) },
                            singleLine = true,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Chat,
                                    contentDescription = "Channel Icon",
                                    tint = TwitchPurpleLight
                                )
                            },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextLight,
                                unfocusedTextColor = TextLight,
                                focusedBorderColor = TwitchPurple,
                                unfocusedBorderColor = BorderColor,
                                focusedContainerColor = DarkCard,
                                unfocusedContainerColor = DarkCard
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (channelInput.isNotEmpty()) {
                                    viewModel.switchChannel(channelInput)
                                }
                            },
                            enabled = channelInput.trim().isNotEmpty() && channelInput.trim().lowercase() != channel.lowercase(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TwitchPurple,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(52.dp)
                        ) {
                            Text(stringResource(id = R.string.switch_btn), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // SECTION 1: ROLE FILTERS
            Text(
                text = stringResource(id = R.string.ignore_by_role),
                style = MaterialTheme.typography.titleMedium,
                color = TwitchPurpleLight,
                fontWeight = FontWeight.Bold
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(id = R.string.normal_users), fontWeight = FontWeight.Medium, color = TextLight)
                            Text(stringResource(id = R.string.ignore_normal_desc), fontSize = 12.sp, color = TextMuted)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Switch(
                            checked = ignoreNormal,
                            onCheckedChange = { viewModel.setIgnoreNormal(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = TwitchPurple)
                        )
                    }

                    HorizontalDivider(color = BorderColor, modifier = Modifier.padding(vertical = 4.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(id = R.string.subscribers), fontWeight = FontWeight.Medium, color = TextLight)
                            Text(stringResource(id = R.string.ignore_subs_desc), fontSize = 12.sp, color = TextMuted)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Switch(
                            checked = ignoreSubs,
                            onCheckedChange = { viewModel.setIgnoreSubs(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = TwitchPurple)
                        )
                    }

                    HorizontalDivider(color = BorderColor, modifier = Modifier.padding(vertical = 4.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(id = R.string.moderators), fontWeight = FontWeight.Medium, color = TextLight)
                            Text(stringResource(id = R.string.ignore_mods_desc), fontSize = 12.sp, color = TextMuted)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Switch(
                            checked = ignoreMods,
                            onCheckedChange = { viewModel.setIgnoreMods(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = TwitchPurple)
                        )
                    }
                }
            }

            // SECTION 2: TTS TUNING
            Text(
                text = stringResource(id = R.string.tts_config),
                style = MaterialTheme.typography.titleMedium,
                color = TwitchPurpleLight,
                fontWeight = FontWeight.Bold
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    // Voice Dropdown Selection
                    Column {
                        Text(stringResource(id = R.string.speech_voice), fontWeight = FontWeight.Medium, color = TextLight)
                        Spacer(modifier = Modifier.height(8.dp))

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { dropdownExpanded = true },
                                colors = CardDefaults.outlinedCardColors(
                                    containerColor = DarkCard
                                ),
                                border = CardDefaults.outlinedCardBorder().copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(BorderColor)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val currentVoiceInfo = availableVoices.find { it.id == selectedVoiceName }
                                    Text(
                                        text = currentVoiceInfo?.displayName ?: stringResource(id = R.string.default_voice),
                                        color = TextLight,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "▼",
                                        color = TextMuted,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = dropdownExpanded,
                                onDismissRequest = { dropdownExpanded = false },
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .background(DarkCard)
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(id = R.string.default_voice), color = TextLight) },
                                    onClick = {
                                        viewModel.selectVoice("")
                                        dropdownExpanded = false
                                    }
                                )
                                availableVoices.forEach { voice ->
                                    DropdownMenuItem(
                                        text = { Text(voice.displayName, color = TextLight) },
                                        onClick = {
                                            viewModel.selectVoice(voice.id)
                                            dropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Pitch Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(stringResource(id = R.string.speech_pitch), fontWeight = FontWeight.Medium, color = TextLight)
                            Text(String.format(java.util.Locale.US, "%.1fx", pitch), color = TwitchPurpleLight, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = pitch,
                            onValueChange = { viewModel.setPitch(it) },
                            valueRange = 0.5f..2.0f,
                            colors = SliderDefaults.colors(
                                activeTrackColor = TwitchPurple,
                                thumbColor = TwitchPurple
                            )
                        )
                    }

                    // Rate Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(stringResource(id = R.string.speech_rate), fontWeight = FontWeight.Medium, color = TextLight)
                            Text(String.format(java.util.Locale.US, "%.1fx", rate), color = TwitchPurpleLight, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = rate,
                            onValueChange = { viewModel.setRate(it) },
                            valueRange = 0.5f..2.0f,
                            colors = SliderDefaults.colors(
                                activeTrackColor = TwitchPurple,
                                thumbColor = TwitchPurple
                            )
                        )
                    }

                    // Volume Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(stringResource(id = R.string.tts_volume), fontWeight = FontWeight.Medium, color = TextLight)
                            Text(String.format(java.util.Locale.US, "%d%%", (volume * 100).toInt()), color = TwitchPurpleLight, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = volume,
                            onValueChange = { viewModel.setVolume(it) },
                            valueRange = 0.0f..1.0f,
                            colors = SliderDefaults.colors(
                                activeTrackColor = TwitchPurple,
                                thumbColor = TwitchPurple
                            )
                        )
                    }
                }
            }

            // SECTION 3: IGNORED USERS
            Text(
                text = stringResource(id = R.string.ignored_users),
                style = MaterialTheme.typography.titleMedium,
                color = TwitchPurpleLight,
                fontWeight = FontWeight.Bold
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = ignoredUserInput,
                            onValueChange = { ignoredUserInput = it },
                            label = { Text(stringResource(id = R.string.ignored_username_label), color = TextMuted) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextLight,
                                unfocusedTextColor = TextLight,
                                focusedBorderColor = TwitchPurple,
                                unfocusedBorderColor = BorderColor,
                                focusedContainerColor = DarkCard,
                                unfocusedContainerColor = DarkCard
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (ignoredUserInput.isNotEmpty()) {
                                    viewModel.addIgnoredUser(ignoredUserInput)
                                    ignoredUserInput = ""
                                }
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = TwitchPurple,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.size(52.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Add User")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ignoredUsers.forEach { user ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = DarkCard,
                                border = CardDefaults.outlinedCardBorder().copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(BorderColor)
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = user,
                                        color = TextLight,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove User",
                                        tint = AlertRed,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable { viewModel.removeIgnoredUser(user) }
                                    )
                                }
                            }
                        }

                        if (ignoredUsers.isEmpty()) {
                            Text(
                                text = stringResource(id = R.string.no_ignored_users),
                                fontSize = 12.sp,
                                color = TextMuted,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // LOG OUT BUTTON
            Button(
                onClick = {
                    viewModel.logout()
                    onBack()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = AlertRed,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Log Out")
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(id = R.string.log_out), fontWeight = FontWeight.Bold)
            }
        }
    }
}
