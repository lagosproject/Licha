package com.lakescorp.twitchchattts.data.auth

import android.util.Log
import com.lakescorp.twitchchattts.BuildConfig
import com.lakescorp.twitchchattts.data.TwitchApiClient
import com.lakescorp.twitchchattts.data.repository.SettingsRepository
import com.lakescorp.twitchchattts.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthManagerImpl @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val apiClient: TwitchApiClient,
    @ApplicationScope private val appScope: CoroutineScope
) : AuthManager {

    private val _loginState = MutableStateFlow<AuthManager.LoginState>(AuthManager.LoginState.Idle)
    override val loginState: StateFlow<AuthManager.LoginState> = _loginState.asStateFlow()

    private val _username = MutableStateFlow("")
    override val username: StateFlow<String> = _username.asStateFlow()

    // Token starts empty and is hydrated off the main thread (the read hits EncryptedSharedPreferences,
    // whose first access triggers KeyStore init). Callers depending on the value await [awaitInitialized].
    private val _oauthToken = MutableStateFlow("")
    override val oauthToken: StateFlow<String> = _oauthToken.asStateFlow()

    // Kicked off at construction on Dispatchers.IO (appScope); awaited via awaitInitialized().
    private val initialLoad = appScope.async {
        _oauthToken.value = settingsRepository.getOauthToken()
    }

    override suspend fun awaitInitialized() {
        initialLoad.await()
    }

    override fun setOauthToken(token: String) {
        // In-memory update is synchronous so oauthToken.value is immediately correct;
        // the encrypted persist is pushed to appScope so it never touches the main thread.
        _oauthToken.value = token
        appScope.launch { settingsRepository.setOauthToken(token) }
    }

    override fun getAuthorizeUrl(): String? {
        // Client ID is a compile-time constant (see app/build.gradle); there is no runtime setter.
        val cid = BuildConfig.TWITCH_CLIENT_ID.trim()
        if (cid.isEmpty()) return null
        // CSRF protection: generate a random nonce, persist it, and require it back unchanged
        // in the redirect (validated in handleDeepLink). See RFC 9700 / CWE-352.
        val state = java.util.UUID.randomUUID().toString()
        settingsRepository.setPendingAuthState(state)
        // NOTE: redirect_uri stays http://localhost because Twitch rejects custom schemes and we
        // have no domain for verified App Links. With no backend possible, this is the only
        // serverless option; `state` mitigates CSRF but token interception remains a known,
        // accepted residual risk under these constraints.
        return "https://id.twitch.tv/oauth2/authorize?client_id=$cid" +
            "&redirect_uri=http://localhost&response_type=token" +
            "&scope=chat:read+chat:edit&state=$state"
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

            // CSRF check: the returned state must match the nonce we issued. Consume it either
            // way so a nonce can never be replayed.
            val expectedState = settingsRepository.getPendingAuthState()
            val returnedState = params["state"]
            settingsRepository.setPendingAuthState("")
            if (expectedState.isEmpty() || returnedState != expectedState) {
                Log.w("AuthManager", "OAuth state mismatch — rejecting redirect.")
                _loginState.value = AuthManager.LoginState.Error("Login verification failed. Please try again.")
                return false
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
