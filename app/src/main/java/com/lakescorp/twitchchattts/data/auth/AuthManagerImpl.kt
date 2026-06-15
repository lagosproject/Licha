package com.lakescorp.twitchchattts.data.auth

import android.util.Log
import com.lakescorp.twitchchattts.data.TwitchApiClient
import com.lakescorp.twitchchattts.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthManagerImpl @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val apiClient: TwitchApiClient
) : AuthManager {

    private val _loginState = MutableStateFlow<AuthManager.LoginState>(AuthManager.LoginState.Idle)
    override val loginState: StateFlow<AuthManager.LoginState> = _loginState.asStateFlow()

    private val _username = MutableStateFlow("")
    override val username: StateFlow<String> = _username.asStateFlow()

    // OAuth token and clientId are kept synchronous (EncryptedSharedPreferences / plain prefs)
    private val _oauthToken = MutableStateFlow(settingsRepository.getOauthToken())
    override val oauthToken: StateFlow<String> = _oauthToken.asStateFlow()

    private val _clientId = MutableStateFlow(
        settingsRepository.getClientId().takeIf { it.isNotEmpty() } ?: ""
    )
    override val clientId: StateFlow<String> = _clientId.asStateFlow()

    override fun setOauthToken(token: String) {
        _oauthToken.value = token
        settingsRepository.setOauthToken(token)
    }

    override fun setClientId(id: String) {
        _clientId.value = id
        settingsRepository.setClientId(id)
    }

    override fun getAuthorizeUrl(): String? {
        val cid = _clientId.value.trim()
        if (cid.isEmpty()) return null
        return "https://id.twitch.tv/oauth2/authorize?client_id=$cid&redirect_uri=http://localhost&response_type=token&scope=chat:read+chat:edit"
    }

    override fun handleDeepLink(intentUri: String?): Boolean {
        if (intentUri == null) return false
        Log.d("AuthManager", "OAuth deep link received. Processing fragment.")
        return try {
            val fragment = intentUri.substringAfter("#", "")
            if (fragment.isEmpty()) return false

            val params = fragment.split("&").associate {
                val pair = it.split("=", limit = 2)
                if (pair.size == 2) pair[0] to pair[1] else pair[0] to ""
            }
            val token = params["access_token"]
            if (!token.isNullOrEmpty()) {
                setOauthToken(token)
                true
            } else {
                _loginState.value = AuthManager.LoginState.Error("No access token found in redirect URL")
                false
            }
        } catch (e: Exception) {
            Log.e("AuthManager", "Error parsing deep link: ${e.message}")
            _loginState.value = AuthManager.LoginState.Error("Error parsing login redirect: ${e.message}")
            false
        }
    }

    override suspend fun validateAndLogin(): String? {
        val token = _oauthToken.value.trim()
        if (token.isEmpty()) {
            _loginState.value = AuthManager.LoginState.Error("OAuth Token cannot be empty")
            return null
        }
        _loginState.value = AuthManager.LoginState.Loading
        return try {
            val username = apiClient.validateToken(token)
            _username.value = username
            _loginState.value = AuthManager.LoginState.Success(username)
            username
        } catch (e: Exception) {
            _loginState.value = AuthManager.LoginState.Error(e.message ?: "Validation failed")
            null
        }
    }

    override fun logout() {
        _username.value = ""
        _loginState.value = AuthManager.LoginState.Idle
    }
}
