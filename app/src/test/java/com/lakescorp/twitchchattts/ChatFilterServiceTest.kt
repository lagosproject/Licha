package com.lakescorp.twitchchattts

import com.lakescorp.twitchchattts.data.TwitchIrcClient
import com.lakescorp.twitchchattts.domain.ChatFilterService
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ChatFilterServiceTest {

    private lateinit var filterService: ChatFilterService

    @Before
    fun setUp() {
        filterService = ChatFilterService()
    }

    private fun createMessage(
        sender: String = "testuser",
        text: String = "Hello world",
        isMod: Boolean = false,
        isSub: Boolean = false,
        isBroadcaster: Boolean = false
    ): TwitchIrcClient.TwitchChatMessage {
        return TwitchIrcClient.TwitchChatMessage(
            senderName = sender,
            displayName = sender,
            messageText = text,
            cleanSpeechText = text,
            isMod = isMod,
            isSub = isSub,
            isBroadcaster = isBroadcaster
        )
    }

    @Test
    fun commandMessagesAreSilenced() {
        val msg = createMessage(text = "!command")
        assertFalse(filterService.shouldSpeak(msg, emptySet(), ignoreMods = false, ignoreSubs = false, ignoreNormal = false))
    }

    @Test
    fun ignoredUsersAreSilenced() {
        val msg = createMessage(sender = "nightbot")
        assertFalse(filterService.shouldSpeak(msg, setOf("nightbot"), ignoreMods = false, ignoreSubs = false, ignoreNormal = false))
    }

    @Test
    fun broadcasterMessagesAreAlwaysSpoken() {
        val msg = createMessage(sender = "streamer", isMod = true, isBroadcaster = true)
        assertTrue(filterService.shouldSpeak(msg, emptySet(), ignoreMods = true, ignoreSubs = true, ignoreNormal = true))
    }

    @Test
    fun normalUserFilteredWhenIgnoreNormalIsTrue() {
        val msg = createMessage(sender = "viewer", isMod = false, isSub = false)
        assertFalse(filterService.shouldSpeak(msg, emptySet(), ignoreMods = false, ignoreSubs = false, ignoreNormal = true))
        assertTrue(filterService.shouldSpeak(msg, emptySet(), ignoreMods = false, ignoreSubs = false, ignoreNormal = false))
    }

    @Test
    fun subFilteredWhenIgnoreSubsIsTrue() {
        val msg = createMessage(sender = "subscriber", isMod = false, isSub = true)
        assertFalse(filterService.shouldSpeak(msg, emptySet(), ignoreMods = false, ignoreSubs = true, ignoreNormal = false))
        assertTrue(filterService.shouldSpeak(msg, emptySet(), ignoreMods = false, ignoreSubs = false, ignoreNormal = false))
    }

    @Test
    fun modFilteredWhenIgnoreModsIsTrue() {
        val msg = createMessage(sender = "moderator", isMod = true, isSub = true)
        assertFalse(filterService.shouldSpeak(msg, emptySet(), ignoreMods = true, ignoreSubs = false, ignoreNormal = false))
        assertTrue(filterService.shouldSpeak(msg, emptySet(), ignoreMods = false, ignoreSubs = false, ignoreNormal = false))
    }
}
