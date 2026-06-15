package com.lakescorp.twitchchattts

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lakescorp.twitchchattts.data.TwitchIrcClient
import com.lakescorp.twitchchattts.data.auth.AuthManager
import com.lakescorp.twitchchattts.data.repository.SettingsRepository
import com.lakescorp.twitchchattts.domain.ChatFilterService
import com.lakescorp.twitchchattts.domain.tts.TtsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Thin coordinator ViewModel.
 *
 * Responsibilities: IRC connection lifecycle, chat history, search, and
 * delegating TTS, auth, and filter decisions to dedicated domain services.
 *
 * All heavy logic now lives in:
 *  - [AuthManager]       — OAuth token, login state, deep-link parsing
 *  - [TtsManager]        — TTS engine lifecycle, voice selection, speech params
 *  - [ChatFilterService] — shouldSpeak() predicate
 *  - [SettingsRepository] — DataStore persistence
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authManager: AuthManager,
    private val ttsManager: TtsManager,
    private val ircClient: TwitchIrcClient,
    private val settingsRepository: SettingsRepository,
    private val filterService: ChatFilterService
) : ViewModel(), TwitchIrcClient.IrcListener {

    companion object {
        // Hardcode your Twitch Client ID here if you want to hide the input field by default.
        const val DEFAULT_CLIENT_ID = ""
    }

    // ── Auth state (delegated to AuthManager) ─────────────────────────────────
    val loginState: StateFlow<AuthManager.LoginState> = authManager.loginState
    val username: StateFlow<String> = authManager.username
    val oauthToken: StateFlow<String> = authManager.oauthToken
    val clientId: StateFlow<String> = authManager.clientId
    val isStorageEncrypted: Boolean = settingsRepository.isStorageEncrypted

    // ── Connection state ──────────────────────────────────────────────────────
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    // ── Chat history ──────────────────────────────────────────────────────────
    private val messageDeque = ArrayDeque<TwitchIrcClient.TwitchChatMessage>(100)
    private val _chatHistory = MutableStateFlow<List<TwitchIrcClient.TwitchChatMessage>>(emptyList())
    val chatHistory: StateFlow<List<TwitchIrcClient.TwitchChatMessage>> = _chatHistory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // ── TTS voices (delegated to TtsManager) ─────────────────────────────────
    val availableVoices: StateFlow<List<TtsManager.VoiceInfo>> = ttsManager.availableVoices

    // ── Settings — reactive StateFlows backed by DataStore ────────────────────
    val channel: StateFlow<String> =
        settingsRepository.channel.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val isMuted: StateFlow<Boolean> =
        settingsRepository.isMuted.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val pitch: StateFlow<Float> =
        settingsRepository.pitch.stateIn(viewModelScope, SharingStarted.Eagerly, 1.0f)
    val rate: StateFlow<Float> =
        settingsRepository.rate.stateIn(viewModelScope, SharingStarted.Eagerly, 1.0f)
    val volume: StateFlow<Float> =
        settingsRepository.volume.stateIn(viewModelScope, SharingStarted.Eagerly, 1.0f)
    val selectedVoiceName: StateFlow<String> =
        settingsRepository.selectedVoice.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val ignoredUsers: StateFlow<Set<String>> =
        settingsRepository.ignoredUsers.stateIn(viewModelScope, SharingStarted.Eagerly, setOf("nightbot"))
    val ignoreNormal: StateFlow<Boolean> =
        settingsRepository.ignoreNormal.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val ignoreSubs: StateFlow<Boolean> =
        settingsRepository.ignoreSubs.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val ignoreMods: StateFlow<Boolean> =
        settingsRepository.ignoreMods.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private var lastSpeaker = ""

    sealed interface ConnectionState {
        object Disconnected : ConnectionState
        object Connecting : ConnectionState
        object Connected : ConnectionState
        data class Error(val message: String) : ConnectionState
    }

    init {
        viewModelScope.launch {
            // Configure TtsManager with stored parameters before initializing the engine.
            // TtsManagerImpl stores these and applies them once the engine reports ready,
            // fixing the bug where TTS params were not restored after app restart.
            ttsManager.setPitch(settingsRepository.pitch.first())
            ttsManager.setRate(settingsRepository.rate.first())
            ttsManager.setVolume(settingsRepository.volume.first())
            settingsRepository.selectedVoice.first().let { if (it.isNotEmpty()) ttsManager.selectVoice(it) }
            ttsManager.initialize()

            // Auto-login if a token was saved from a previous session
            val savedToken = authManager.oauthToken.value.trim()
            if (savedToken.isNotEmpty()) {
                triggerLogin()
            }
        }
    }

    // ── Auth delegation ───────────────────────────────────────────────────────

    fun setOauthToken(token: String) = authManager.setOauthToken(token)
    fun setClientId(id: String) = authManager.setClientId(id)
    fun getAuthorizeUrl(): String? = authManager.getAuthorizeUrl()

    fun handleDeepLink(intentUri: String?) {
        val tokenSet = authManager.handleDeepLink(intentUri)
        if (tokenSet) {
            viewModelScope.launch { triggerLogin() }
        }
    }

    fun validateAndLogin() {
        viewModelScope.launch { triggerLogin() }
    }

    /**
     * Validates credentials and connects to chat on success.
     * Called from [init], [handleDeepLink], and [validateAndLogin].
     */
    private suspend fun triggerLogin() {
        val username = authManager.validateAndLogin() ?: return
        val currentChannel = channel.value.ifEmpty { username }
        if (channel.value.isEmpty()) {
            settingsRepository.setChannel(currentChannel)
        }
        connectToChat(username, authManager.oauthToken.value, currentChannel)
    }

    // ── IRC Connection ────────────────────────────────────────────────────────

    fun connectToChat(username: String, token: String, targetChannel: String) {
        _connectionState.value = ConnectionState.Connecting
        ircClient.disconnect()
        _chatHistory.value = emptyList()
        messageDeque.clear()
        lastSpeaker = ""
        ircClient.connect(username, token, targetChannel, this)
    }

    fun switchChannel(newChannel: String) {
        val cleanChannel = newChannel.trim().lowercase().removePrefix("#")
        if (cleanChannel.isNotEmpty()) {
            viewModelScope.launch { settingsRepository.setChannel(cleanChannel) }
            val currentUsername = authManager.username.value
            val currentToken = authManager.oauthToken.value
            if (currentUsername.isNotEmpty() && currentToken.isNotEmpty()) {
                connectToChat(currentUsername, currentToken, cleanChannel)
            }
        }
    }

    fun logout() {
        ircClient.disconnect()
        authManager.logout()
        _connectionState.value = ConnectionState.Disconnected
        _chatHistory.value = emptyList()
        messageDeque.clear()
        lastSpeaker = ""
        ttsManager.stop()
    }

    // ── Search ────────────────────────────────────────────────────────────────

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // ── TTS controls ──────────────────────────────────────────────────────────

    fun toggleMute() {
        val newValue = !isMuted.value
        viewModelScope.launch { settingsRepository.setMuted(newValue) }
        if (newValue) ttsManager.stop()
    }

    fun setPitch(value: Float) {
        ttsManager.setPitch(value)
        viewModelScope.launch { settingsRepository.setPitch(value) }
    }

    fun setRate(value: Float) {
        ttsManager.setRate(value)
        viewModelScope.launch { settingsRepository.setRate(value) }
    }

    fun setVolume(value: Float) {
        ttsManager.setVolume(value)
        viewModelScope.launch { settingsRepository.setVolume(value) }
    }

    fun selectVoice(voiceId: String) {
        ttsManager.selectVoice(voiceId)
        viewModelScope.launch { settingsRepository.setSelectedVoice(voiceId) }
    }

    // ── Filter settings ───────────────────────────────────────────────────────

    fun setIgnoreNormal(value: Boolean) {
        viewModelScope.launch { settingsRepository.setIgnoreNormal(value) }
    }

    fun setIgnoreSubs(value: Boolean) {
        viewModelScope.launch { settingsRepository.setIgnoreSubs(value) }
    }

    fun setIgnoreMods(value: Boolean) {
        viewModelScope.launch { settingsRepository.setIgnoreMods(value) }
    }

    fun addIgnoredUser(user: String) {
        val cleanUser = user.trim().lowercase()
        if (cleanUser.isNotEmpty()) {
            viewModelScope.launch { settingsRepository.addIgnoredUser(cleanUser) }
        }
    }

    fun removeIgnoredUser(user: String) {
        viewModelScope.launch { settingsRepository.removeIgnoredUser(user.trim().lowercase()) }
    }

    // ── IRC Listener callbacks ────────────────────────────────────────────────

    override fun onConnected() {
        viewModelScope.launch(Dispatchers.Main.immediate) {
            _connectionState.value = ConnectionState.Connected
        }
    }

    override fun onDisconnected(reason: String) {
        viewModelScope.launch(Dispatchers.Main.immediate) {
            _connectionState.value = ConnectionState.Disconnected
        }
    }

    override fun onMessageReceived(message: TwitchIrcClient.TwitchChatMessage) {
        viewModelScope.launch {
            messageDeque.addLast(message)
            if (messageDeque.size > 100) messageDeque.removeFirst()
            _chatHistory.value = messageDeque.toList()

            if (isMuted.value) return@launch
            if (!filterService.shouldSpeak(
                    message = message,
                    ignoredUsers = ignoredUsers.value,
                    ignoreMods = ignoreMods.value,
                    ignoreSubs = ignoreSubs.value,
                    ignoreNormal = ignoreNormal.value
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

    override fun onError(error: String) {
        viewModelScope.launch(Dispatchers.Main.immediate) {
            _connectionState.value = ConnectionState.Error(error)
        }
    }

    fun injectMockData(lang: String) {
        viewModelScope.launch {
            _connectionState.value = ConnectionState.Connected
            settingsRepository.setChannel("licha")
            
            val mockMessages = when (lang) {
                "es-ES" -> listOf(
                    TwitchIrcClient.TwitchChatMessage("system", "Sistema", "Síntesis de voz inicializada correctamente.", "Síntesis de voz inicializada correctamente.", false, false, false),
                    TwitchIrcClient.TwitchChatMessage("nightbot", "Nightbot", "¡Recuerda suscribirte al canal para obtener beneficios!", "¡Recuerda suscribirte al canal para obtener beneficios!", true, false, false),
                    TwitchIrcClient.TwitchChatMessage("gamer_x", "Gamer_X", "¡Hola! Me encanta el stream de hoy.", "¡Hola! Me encanta el stream de hoy.", false, true, false),
                    TwitchIrcClient.TwitchChatMessage("speedy_07", "Speedy_07", "¿Puedes oír mi mensaje de voz a texto?", "¿Puedes oír mi mensaje de voz a texto?", false, false, false),
                    TwitchIrcClient.TwitchChatMessage("techgeek", "TechGeek", "¡Licha hace que los streams de VR sean mucho más fáciles!", "¡Licha hace que los streams de VR sean mucho más fáciles!", true, true, false)
                )
                "fr-FR" -> listOf(
                    TwitchIrcClient.TwitchChatMessage("system", "Système", "Synthèse vocale initialisée avec succès.", "Synthèse vocale initialisée avec succès.", false, false, false),
                    TwitchIrcClient.TwitchChatMessage("nightbot", "Nightbot", "Pensez à vous abonner pour soutenir la chaîne !", "Pensez à vous abonner pour soutenir la chaîne !", true, false, false),
                    TwitchIrcClient.TwitchChatMessage("gamer_x", "Gamer_X", "Salut ! Super stream aujourd'hui.", "Salut ! Super stream aujourd'hui.", false, true, false),
                    TwitchIrcClient.TwitchChatMessage("speedy_07", "Speedy_07", "Est-ce que tu entends mon message vocal ?", "Est-ce que tu entends mon message vocal ?", false, false, false),
                    TwitchIrcClient.TwitchChatMessage("techgeek", "TechGeek", "Licha rend les streams VR tellement plus simples !", "Licha rend les streams VR tellement plus simples !", true, true, false)
                )
                else -> listOf(
                    TwitchIrcClient.TwitchChatMessage("system", "System", "Speech synthesis initialized successfully.", "Speech synthesis initialized successfully.", false, false, false),
                    TwitchIrcClient.TwitchChatMessage("nightbot", "Nightbot", "Remember to subscribe to the channel for benefits!", "Remember to subscribe to the channel for benefits!", true, false, false),
                    TwitchIrcClient.TwitchChatMessage("gamer_x", "Gamer_X", "Hello streamer! Love the gameplay today.", "Hello streamer! Love the gameplay today.", false, true, false),
                    TwitchIrcClient.TwitchChatMessage("speedy_07", "Speedy_07", "Can you hear my text to speech message?", "Can you hear my text to speech message?", false, false, false),
                    TwitchIrcClient.TwitchChatMessage("techgeek", "TechGeek", "This Licha reader makes VR streams so much easier!", "This Licha reader makes VR streams so much easier!", true, true, false)
                )
            }
            _chatHistory.value = mockMessages
        }
    }

    override fun onCleared() {
        super.onCleared()
        ircClient.disconnect()
        ttsManager.shutdown()
    }
}
