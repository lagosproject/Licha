package com.lakescorp.twitchchattts.domain.tts

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production implementation of [TtsManager].
 *
 * Parameters (pitch, rate, volume, voice) set before [initialize] is called are
 * stored internally and applied automatically once the TTS engine reports ready.
 * This avoids the race condition where the engine initializes asynchronously but
 * the caller wants to configure it immediately.
 */
@Singleton
class TtsManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : TtsManager {

    private val _availableVoices = MutableStateFlow<List<TtsManager.VoiceInfo>>(emptyList())
    override val availableVoices: StateFlow<List<TtsManager.VoiceInfo>> = _availableVoices.asStateFlow()

    private var tts: TextToSpeech? = null

    // Pending parameters — applied once the engine is ready
    private var currentPitch = 1.0f
    private var currentRate = 1.0f
    private var currentVolume = 1.0f
    private var currentVoiceId = ""

    override fun initialize() {
        if (tts != null) return  // Already initialized
        tts = TextToSpeech(context) { status ->
            if (status != TextToSpeech.ERROR) {
                tts?.language = Locale.getDefault()
                // Apply any parameters that were set before the engine was ready
                tts?.setPitch(currentPitch)
                tts?.setSpeechRate(currentRate)
                loadAndApplyVoices()
            } else {
                Log.e("TtsManager", "Failed to initialize TTS engine")
            }
        }
    }

    private fun loadAndApplyVoices() {
        try {
            val voiceList = tts?.voices
                ?.filterNot { it.features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) }
                ?.map { TtsManager.VoiceInfo(id = it.name, displayName = buildDisplayName(it)) }
                ?.sortedBy { it.displayName }
                ?: emptyList()
            _availableVoices.value = voiceList

            // Apply the saved voice selection (stored before engine was ready)
            if (currentVoiceId.isNotEmpty()) {
                tts?.voices?.find { it.name == currentVoiceId }?.let { tts?.voice = it }
            }
        } catch (e: Exception) {
            Log.e("TtsManager", "Error loading voices: ${e.message}")
        }
    }

    private fun buildDisplayName(voice: Voice): String {
        val locale = voice.locale
        val languageName = locale.getDisplayLanguage(Locale.getDefault()).replaceFirstChar { it.uppercase() }
        val countryName = locale.getDisplayCountry(Locale.getDefault())
        val type = if (voice.isNetworkConnectionRequired) "Network" else "Local"
        val suffix = if (countryName.isNotEmpty()) " ($countryName, $type)" else " ($type)"
        val uid = voice.name.substringAfterLast("-", "").take(4).uppercase()
        return "$languageName$suffix #${uid.ifEmpty { "Default" }}"
    }

    override fun speak(text: String) {
        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, currentVolume)
        }
        tts?.speak(text, TextToSpeech.QUEUE_ADD, params, System.currentTimeMillis().toString())
    }

    override fun setPitch(value: Float) {
        currentPitch = value
        tts?.setPitch(value)
    }

    override fun setRate(value: Float) {
        currentRate = value
        tts?.setSpeechRate(value)
    }

    override fun setVolume(value: Float) {
        currentVolume = value
        // Volume is applied per-utterance via Bundle, so no TTS call needed here
    }

    override fun selectVoice(voiceId: String) {
        currentVoiceId = voiceId
        val engine = tts ?: return  // Engine not ready; will be applied in loadAndApplyVoices()
        if (voiceId.isEmpty()) {
            engine.voice = engine.defaultVoice
        } else {
            engine.voices?.find { it.name == voiceId }?.let { engine.voice = it }
        }
    }

    override fun stop() {
        tts?.stop()
    }

    override fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
