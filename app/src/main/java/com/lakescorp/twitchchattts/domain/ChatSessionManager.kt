package com.lakescorp.twitchchattts.domain

import android.content.Context
import com.lakescorp.twitchchattts.ChatViewModel.ConnectionState
import com.lakescorp.twitchchattts.R
import com.lakescorp.twitchchattts.data.TwitchIrcClient
import com.lakescorp.twitchchattts.data.repository.SettingsRepository
import com.lakescorp.twitchchattts.di.ApplicationScope
import com.lakescorp.twitchchattts.domain.tts.TtsManager
import com.lakescorp.twitchchattts.service.ChatForegroundService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton coordinator responsible for maintaining the Twitch IRC session,
 * delegating TTS playback, and orchestrating the foreground service so playback
 * continues seamlessly when the screen is turned off or the app is in the background.
 */
@Singleton
class ChatSessionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ircClient: TwitchIrcClient,
    private val ttsManager: TtsManager,
    private val settingsRepository: SettingsRepository,
    private val filterService: ChatFilterService,
    @ApplicationScope private val appScope: CoroutineScope
) : TwitchIrcClient.IrcListener {

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val messageDeque = ArrayDeque<TwitchIrcClient.TwitchChatMessage>(100)
    private val _chatHistory = MutableStateFlow<List<TwitchIrcClient.TwitchChatMessage>>(emptyList())
    val chatHistory: StateFlow<List<TwitchIrcClient.TwitchChatMessage>> = _chatHistory.asStateFlow()

    @Volatile private var currentChannel = ""
    @Volatile private var lastSpeaker = ""

    fun connectToChat(username: String, token: String, targetChannel: String) {
        currentChannel = targetChannel
        _connectionState.value = ConnectionState.Connecting
        ircClient.disconnect()
        _chatHistory.value = emptyList()
        messageDeque.clear()
        lastSpeaker = ""
        ircClient.connect(username, token, targetChannel, this)
    }

    fun disconnect() {
        currentChannel = ""
        ircClient.disconnect()
        _connectionState.value = ConnectionState.Disconnected
        _chatHistory.value = emptyList()
        messageDeque.clear()
        lastSpeaker = ""
        ttsManager.stop()
        ChatForegroundService.stop(context)
    }

    override fun onConnected() {
        appScope.launch(Dispatchers.Main.immediate) {
            _connectionState.value = ConnectionState.Connected
            if (currentChannel.isNotEmpty()) {
                ChatForegroundService.start(context, currentChannel)
            }
        }
    }

    override fun onDisconnected(reason: String) {
        appScope.launch(Dispatchers.Main.immediate) {
            _connectionState.value = ConnectionState.Disconnected
            ChatForegroundService.stop(context)
        }
    }

    override fun onError(error: String) {
        appScope.launch(Dispatchers.Main.immediate) {
            _connectionState.value = ConnectionState.Error(error)
            ChatForegroundService.stop(context)
        }
    }

    override fun onMessageReceived(message: TwitchIrcClient.TwitchChatMessage) {
        appScope.launch {
            messageDeque.addLast(message)
            if (messageDeque.size > 100) messageDeque.removeFirst()
            _chatHistory.value = messageDeque.toList()

            if (settingsRepository.isMuted.first()) return@launch
            val ignoredUsers = settingsRepository.ignoredUsers.first()
            val ignoreMods = settingsRepository.ignoreMods.first()
            val ignoreSubs = settingsRepository.ignoreSubs.first()
            val ignoreNormal = settingsRepository.ignoreNormal.first()

            if (!filterService.shouldSpeak(
                    message = message,
                    ignoredUsers = ignoredUsers,
                    ignoreMods = ignoreMods,
                    ignoreSubs = ignoreSubs,
                    ignoreNormal = ignoreNormal
                )
            ) return@launch

            val saidWord = context.getString(R.string.said)
            val textToSpeak = if (message.displayName.equals(lastSpeaker, ignoreCase = true)) {
                message.cleanSpeechText
            } else {
                "${message.displayName.replace("_", " ")} $saidWord ${message.cleanSpeechText}"
            }

            if (message.cleanSpeechText.isNotEmpty()) {
                lastSpeaker = message.displayName
                ttsManager.speak(textToSpeak)
            }
        }
    }
}
