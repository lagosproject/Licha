package com.lakescorp.twitchchattts.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.lakescorp.twitchchattts.di.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>,
    @ApplicationScope private val appScope: CoroutineScope
) : SettingsRepository {

    // ── EncryptedSharedPreferences (OAuth token) ──────────────────────────────

    /**
     * True when the OAuth token is protected by EncryptedSharedPreferences (AES-256-GCM).
     * False on KeyStore failure fallback — the ViewModel can expose a security warning.
     */
    override var isStorageEncrypted: Boolean = true
        private set

    private val plainPrefs: SharedPreferences =
        context.getSharedPreferences("TwitchChatTTSLegacy", Context.MODE_PRIVATE)

    // KeyStore-backed encrypted prefs. Initialization (MasterKeys.getOrCreate +
    // EncryptedSharedPreferences.create) performs KeyStore/disk I/O that can take hundreds
    // of ms on a cold start, so it MUST NOT run on the main thread. It is lazy and is warmed
    // up from [appScope] (Dispatchers.IO) in init; any accidental first access still pays the
    // cost on whatever thread touches it, but the warm-up guarantees that thread isn't main.
    private val encryptedPrefs: SharedPreferences by lazy { createEncryptedPrefs() }

    private fun createEncryptedPrefs(): SharedPreferences {
        val prefs = try {
            // NOTE: MasterKeys is deprecated in favour of MasterKey.Builder, but that API only
            // exists in security-crypto 1.1.0-alpha — we intentionally stay on the stable 1.0.0
            // release rather than ship an alpha dependency. MasterKeys remains fully functional
            // and uses the same default key alias, so the migration is purely cosmetic and can be
            // done if/when 1.1.0 stabilises (existing tokens would stay decryptable).
            @Suppress("DEPRECATION")
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            EncryptedSharedPreferences.create(
                "TwitchChatTTSSecureSettings",
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e(
                "SettingsRepository",
                "EncryptedSharedPreferences unavailable — falling back to plain prefs. Token will NOT be encrypted.",
                e
            )
            isStorageEncrypted = false
            plainPrefs
        }
        // Migrate legacy plain oauth token into the (now-created) encrypted store, if present.
        // Runs as part of first initialization so it inherits the off-main-thread guarantee.
        val legacyToken = plainPrefs.getString("oauth", "") ?: ""
        if (legacyToken.isNotEmpty() && prefs.getString("oauth", "").isNullOrEmpty()) {
            prefs.edit().putString("oauth", legacyToken).apply()
            plainPrefs.edit().remove("oauth").apply()
        }
        return prefs
    }

    init {
        // Warm up the KeyStore-backed prefs off the main thread. The DI graph is built on the
        // main thread (ViewModel injection), so without this the lazy init would block it.
        appScope.launch { encryptedPrefs }
    }

    override fun getOauthToken(): String = encryptedPrefs.getString("oauth", "") ?: ""
    override fun setOauthToken(token: String) {
        encryptedPrefs.edit().putString("oauth", token).apply()
    }

    // OAuth CSRF nonce persisted in plain prefs so it survives the round-trip to the external
    // browser (and possible process death) before the redirect comes back.
    override fun getPendingAuthState(): String = plainPrefs.getString("pendingAuthState", "") ?: ""
    override fun setPendingAuthState(state: String) {
        plainPrefs.edit().putString("pendingAuthState", state).apply()
    }

    // ── DataStore preference keys ──────────────────────────────────────────────

    private object Keys {
        // Key names intentionally match the old SharedPreferences keys so that
        // SharedPreferencesMigration (configured in DataStoreModule) copies values correctly.
        val CHANNEL = stringPreferencesKey("channel")
        val IS_MUTED = booleanPreferencesKey("isMuted")
        val PITCH = floatPreferencesKey("pitch")
        val RATE = floatPreferencesKey("rate")
        val VOLUME = floatPreferencesKey("volume")
        val SELECTED_VOICE = stringPreferencesKey("selectedVoice")
        val IGNORED_USERS = stringSetPreferencesKey("ignoredUsers")
        val IGNORE_NORMAL = booleanPreferencesKey("ignoreNormalUsers")
        val IGNORE_SUBS = booleanPreferencesKey("ignoreSubscribers")
        val IGNORE_MODS = booleanPreferencesKey("ignoreModerators")
    }

    // ── Flows (safe reads — emit defaults on IO errors) ────────────────────────

    private fun <T> DataStore<Preferences>.safeFlow(default: T, extract: (Preferences) -> T): Flow<T> =
        data.catch { cause ->
            if (cause is IOException) {
                Log.e("SettingsRepository", "DataStore read error", cause)
                emit(emptyPreferences())
            } else throw cause
        }.map { extract(it) }

    override val channel: Flow<String> = dataStore.safeFlow("") { it[Keys.CHANNEL] ?: "" }
    override val isMuted: Flow<Boolean> = dataStore.safeFlow(false) { it[Keys.IS_MUTED] ?: false }
    override val pitch: Flow<Float> = dataStore.safeFlow(1.0f) { it[Keys.PITCH] ?: 1.0f }
    override val rate: Flow<Float> = dataStore.safeFlow(1.0f) { it[Keys.RATE] ?: 1.0f }
    override val volume: Flow<Float> = dataStore.safeFlow(1.0f) { it[Keys.VOLUME] ?: 1.0f }
    override val selectedVoice: Flow<String> = dataStore.safeFlow("") { it[Keys.SELECTED_VOICE] ?: "" }
    override val ignoredUsers: Flow<Set<String>> =
        dataStore.safeFlow(setOf("nightbot")) { it[Keys.IGNORED_USERS] ?: setOf("nightbot") }
    override val ignoreNormal: Flow<Boolean> = dataStore.safeFlow(false) { it[Keys.IGNORE_NORMAL] ?: false }
    override val ignoreSubs: Flow<Boolean> = dataStore.safeFlow(false) { it[Keys.IGNORE_SUBS] ?: false }
    override val ignoreMods: Flow<Boolean> = dataStore.safeFlow(false) { it[Keys.IGNORE_MODS] ?: false }

    // ── Suspend writes ─────────────────────────────────────────────────────────

    override suspend fun setChannel(channel: String) {
        dataStore.edit { it[Keys.CHANNEL] = channel }
    }

    override suspend fun setMuted(muted: Boolean) {
        dataStore.edit { it[Keys.IS_MUTED] = muted }
    }

    override suspend fun setPitch(pitch: Float) {
        dataStore.edit { it[Keys.PITCH] = pitch }
    }

    override suspend fun setRate(rate: Float) {
        dataStore.edit { it[Keys.RATE] = rate }
    }

    override suspend fun setVolume(volume: Float) {
        dataStore.edit { it[Keys.VOLUME] = volume }
    }

    override suspend fun setSelectedVoice(voiceId: String) {
        dataStore.edit { it[Keys.SELECTED_VOICE] = voiceId }
    }

    override suspend fun addIgnoredUser(user: String) {
        val cleanUser = user.trim().lowercase()
        if (cleanUser.isEmpty()) return
        dataStore.edit { prefs ->
            val current = prefs[Keys.IGNORED_USERS] ?: setOf("nightbot")
            prefs[Keys.IGNORED_USERS] = current + cleanUser
        }
    }

    override suspend fun removeIgnoredUser(user: String) {
        val cleanUser = user.trim().lowercase()
        dataStore.edit { prefs ->
            val current = prefs[Keys.IGNORED_USERS] ?: setOf("nightbot")
            prefs[Keys.IGNORED_USERS] = current - cleanUser
        }
    }

    override suspend fun setIgnoreNormal(ignore: Boolean) {
        dataStore.edit { it[Keys.IGNORE_NORMAL] = ignore }
    }

    override suspend fun setIgnoreSubs(ignore: Boolean) {
        dataStore.edit { it[Keys.IGNORE_SUBS] = ignore }
    }

    override suspend fun setIgnoreMods(ignore: Boolean) {
        dataStore.edit { it[Keys.IGNORE_MODS] = ignore }
    }
}
