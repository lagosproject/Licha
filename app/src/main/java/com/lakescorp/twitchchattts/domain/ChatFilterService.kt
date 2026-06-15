package com.lakescorp.twitchchattts.domain

import com.lakescorp.twitchchattts.data.TwitchIrcClient
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pure domain service encapsulating chat message filter logic.
 *
 * All filter settings are accepted as explicit parameters so this class has
 * no Android framework dependencies and is trivially unit-testable.
 */
@Singleton
class ChatFilterService @Inject constructor() {

    /**
     * Returns `true` if [message] should be spoken by TTS, `false` if it should be silenced.
     *
     * @param message      The incoming Twitch IRC message.
     * @param ignoredUsers Lowercased set of usernames that are always silenced.
     * @param ignoreMods   When true, moderator messages are skipped.
     * @param ignoreSubs   When true, subscriber (non-mod) messages are skipped.
     * @param ignoreNormal When true, messages from plain viewers are skipped.
     */
    fun shouldSpeak(
        message: TwitchIrcClient.TwitchChatMessage,
        ignoredUsers: Set<String>,
        ignoreMods: Boolean,
        ignoreSubs: Boolean,
        ignoreNormal: Boolean
    ): Boolean {
        if (message.messageText.startsWith("!")) return false
        if (ignoredUsers.contains(message.senderName.lowercase())) return false
        if (message.isBroadcaster) return true
        if (message.isMod && ignoreMods) return false
        if (message.isSub && !message.isMod && ignoreSubs) return false
        if (!message.isMod && !message.isSub && ignoreNormal) return false
        return true
    }
}
