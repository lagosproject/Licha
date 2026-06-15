package com.lakescorp.twitchchattts.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context,
    private val dataStore: DataStore<Preferences>
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

    private val encryptedPrefs: SharedPreferences by lazy {
        try {
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
    }

    init {
        // Migrate legacy plain oauth token to EncryptedSharedPreferences if present
        val legacyToken = plainPrefs.getString("oauth", "") ?: ""
        if (legacyToken.isNotEmpty() && encryptedPrefs.getString("oauth", "").isNullOrEmpty()) {
            encryptedPrefs.edit().putString("oauth", legacyToken).apply()
            plainPrefs.edit().remove("oauth").apply()
        }
    }

    override fun getOauthToken(): String = encryptedPrefs.getString("oauth", "") ?: ""
    override fun setOauthToken(token: String) {
        encryptedPrefs.edit().putString("oauth", token).apply()
    }

    // ClientId stays in plain SharedPreferences (not sensitive; synchronous read needed in AuthManagerImpl constructor)
    override fun getClientId(): String = plainPrefs.getString("clientId", "") ?: ""
    override fun setClientId(id: String) {
        plainPrefs.edit().putString("clientId", id).apply()
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
