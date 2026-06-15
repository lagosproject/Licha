package com.lakescorp.twitchchattts.domain.tts

import kotlinx.coroutines.flow.StateFlow

/**
 * Manages the Android TextToSpeech engine lifecycle, voice enumeration,
 * and all speech parameter configuration.
 *
 * Abstracted behind an interface so the ViewModel does not depend on
 * the Android TTS framework directly, enabling unit testing.
 */
interface TtsManager {

    data class VoiceInfo(val id: String, val displayName: String)

    /** Emits the list of installed voices once the engine is initialized. */
    val availableVoices: StateFlow<List<VoiceInfo>>

    /**
     * Initializes the TTS engine asynchronously.
     * Parameters set via [setPitch]/[setRate]/[setVolume]/[selectVoice] before
     * this call are queued and applied automatically once the engine is ready.
     */
    fun initialize()

    /** Speaks [text] using current engine parameters. No-op if not yet initialized. */
    fun speak(text: String)

    fun setPitch(value: Float)
    fun setRate(value: Float)
    fun setVolume(value: Float)

    /**
     * Selects the voice with the given [voiceId]. Pass an empty string to
     * revert to the engine's default voice.
     */
    fun selectVoice(voiceId: String)

    /** Stops any in-progress utterance immediately. */
    fun stop()

    /** Releases all TTS engine resources. Must be called from onCleared(). */
    fun shutdown()
}
