package com.lakescorp.twitchchattts.data.auth

import kotlinx.coroutines.flow.StateFlow

/**
 * Manages Twitch OAuth authentication state and token lifecycle.
 * Responsible for token validation, deep-link parsing, and login state.
 */
interface AuthManager {

    sealed interface LoginState {
        object Idle : LoginState
        object Loading : LoginState
        data class Success(val username: String) : LoginState
        data class Error(val message: String) : LoginState
    }

    val loginState: StateFlow<LoginState>
    val username: StateFlow<String>
    val oauthToken: StateFlow<String>
    val clientId: StateFlow<String>

    fun setOauthToken(token: String)
    fun setClientId(id: String)

    /** Builds the Twitch OAuth authorization URL, or null if no Client ID is set. */
    fun getAuthorizeUrl(): String?

    /**
     * Parses the OAuth redirect URI fragment and extracts the access token.
     * Stores the token if found; updates [loginState] to Error otherwise.
     * Returns `true` if a token was successfully extracted.
     */
    fun handleDeepLink(intentUri: String?): Boolean

    /**
     * Validates the current [oauthToken] against the Twitch API.
     * Updates [loginState] accordingly.
     * Returns the username on success, null on failure.
     */
    suspend fun validateAndLogin(): String?

    /** Resets authentication state without clearing the stored token. */
    fun logout()
}
