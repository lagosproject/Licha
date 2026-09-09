package com.lakescorp.twitchchattts

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lakescorp.twitchchattts.data.TwitchIrcClient
import com.lakescorp.twitchchattts.data.auth.AuthManager
import com.lakescorp.twitchchattts.data.repository.SettingsRepository
import com.lakescorp.twitchchattts.domain.ChatSessionManager
import com.lakescorp.twitchchattts.domain.tts.TtsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
 * Responsibilities: Chat UI state, settings manipulation, and auth coordination.
 * The active background session (IRC WebSocket, TTS speaking, Foreground Service)
 * is managed by [ChatSessionManager] so chat listening continues uninterrupted
 * when the screen turns off or the app is in the background.
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authManager: AuthManager,
    private val ttsManager: TtsManager,
    private val chatSessionManager: ChatSessionManager,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    sealed interface ConnectionState {
        object Disconnected : ConnectionState
        object Connecting : ConnectionState
        object Connected : ConnectionState
        data class Error(val message: String) : ConnectionState
    }

    // ── Auth state (delegated to AuthManager) ─────────────────────────────────
    val loginState: StateFlow<AuthManager.LoginState> = authManager.loginState
    val username: StateFlow<String> = authManager.username
    val oauthToken: StateFlow<String> = authManager.oauthToken

    // Crypto availability is only known after the encrypted store is initialized off the main
    // thread, so it's reactive: defaults to true (optimistic) and is corrected in init.
    private val _isStorageEncrypted = MutableStateFlow(true)
    val isStorageEncrypted: StateFlow<Boolean> = _isStorageEncrypted.asStateFlow()

    // ── Connection state & Chat history (delegated to ChatSessionManager) ─────
    val connectionState: StateFlow<ConnectionState> = chatSessionManager.connectionState
    val chatHistory: StateFlow<List<TwitchIrcClient.TwitchChatMessage>> = chatSessionManager.chatHistory

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
    val keepScreenOn: StateFlow<Boolean> =
        settingsRepository.keepScreenOn.stateIn(viewModelScope, SharingStarted.Eagerly, false)

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

            // Wait for the encrypted store / token to load off the main thread before reading
            // them. After this, isStorageEncrypted reflects the real KeyStore outcome.
            authManager.awaitInitialized()
            _isStorageEncrypted.value = settingsRepository.isStorageEncrypted

            // Auto-login if a token was saved from a previous session
            val savedToken = authManager.oauthToken.value.trim()
            if (savedToken.isNotEmpty()) {
                triggerLogin()
            }
        }
    }

    // ── Auth delegation ───────────────────────────────────────────────────────

    fun setOauthToken(token: String) = authManager.setOauthToken(token)
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
        if (connectionState.value is ConnectionState.Disconnected) {
            connectToChat(username, authManager.oauthToken.value, currentChannel)
        }
    }

    // ── IRC Connection ────────────────────────────────────────────────────────

    fun connectToChat(username: String, token: String, targetChannel: String) {
        chatSessionManager.connectToChat(username, token, targetChannel)
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
        chatSessionManager.disconnect()
        authManager.logout()
    }

    fun setKeepScreenOn(value: Boolean) {
        viewModelScope.launch { settingsRepository.setKeepScreenOn(value) }
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

    override fun onCleared() {
        super.onCleared()
        // If not actively listening in the background, clean up TTS resources
        if (connectionState.value is ConnectionState.Disconnected) {
            ttsManager.shutdown()
        }
    }
}
