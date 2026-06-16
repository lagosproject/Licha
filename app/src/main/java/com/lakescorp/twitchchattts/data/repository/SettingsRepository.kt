package com.lakescorp.twitchchattts.data.repository

import kotlinx.coroutines.flow.Flow

/**
 * Single source of truth for all persisted application settings.
 *
 * # Storage Tiers
 * - OAuth token: [EncryptedSharedPreferences] (AES-256-GCM). Synchronous read/write.
 * - OAuth CSRF nonce: plain [SharedPreferences]. Synchronous read/write.
 * - All other settings: [DataStore<Preferences>]. Async [Flow]-based reads, suspend writes.
 *
 * # Encryption fallback
 * See [isStorageEncrypted] — if `false`, the OAuth token fell back to plain
 * SharedPreferences due to a KeyStore failure on this device.
 */
interface SettingsRepository {

    /** True when the OAuth token is protected by EncryptedSharedPreferences. */
    val isStorageEncrypted: Boolean

    // ── Synchronous (EncryptedSharedPreferences / plain SharedPreferences) ─────

    fun getOauthToken(): String
    fun setOauthToken(token: String)

    /** CSRF nonce for the in-flight OAuth authorization request. Not sensitive (integrity, not secrecy). */
    fun getPendingAuthState(): String
    fun setPendingAuthState(state: String)

    // ── DataStore-backed reactive streams ──────────────────────────────────────

    val channel: Flow<String>
    val isMuted: Flow<Boolean>
    val pitch: Flow<Float>
    val rate: Flow<Float>
    val volume: Flow<Float>
    val selectedVoice: Flow<String>
    val ignoredUsers: Flow<Set<String>>
    val ignoreNormal: Flow<Boolean>
    val ignoreSubs: Flow<Boolean>
    val ignoreMods: Flow<Boolean>

    // ── DataStore suspend writes ───────────────────────────────────────────────

    suspend fun setChannel(channel: String)
    suspend fun setMuted(muted: Boolean)
    suspend fun setPitch(pitch: Float)
    suspend fun setRate(rate: Float)
    suspend fun setVolume(volume: Float)
    suspend fun setSelectedVoice(voiceId: String)
    suspend fun addIgnoredUser(user: String)
    suspend fun removeIgnoredUser(user: String)
    suspend fun setIgnoreNormal(ignore: Boolean)
    suspend fun setIgnoreSubs(ignore: Boolean)
    suspend fun setIgnoreMods(ignore: Boolean)
}
