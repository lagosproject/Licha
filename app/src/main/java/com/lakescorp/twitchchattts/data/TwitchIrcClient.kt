package com.lakescorp.twitchchattts.data

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TwitchIrcClient @Inject constructor(
    sharedClient: OkHttpClient
) {

    private val client = sharedClient.newBuilder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()
    private var webSocket: WebSocket? = null
    private var isClosed = false
    private var listener: IrcListener? = null

    interface IrcListener {
        fun onConnected()
        fun onDisconnected(reason: String)
        fun onMessageReceived(message: TwitchChatMessage)
        fun onError(error: String)
    }

    data class TwitchChatMessage(
        val senderName: String,
        val displayName: String,
        val messageText: String,
        val cleanSpeechText: String,
        val isMod: Boolean,
        val isSub: Boolean,
        val isBroadcaster: Boolean
    )

    fun connect(username: String, oauthToken: String, channel: String, listener: IrcListener) {
        this.listener = listener
        val token = if (oauthToken.startsWith("oauth:", ignoreCase = true)) {
            oauthToken
        } else {
            "oauth:$oauthToken"
        }

        val request = Request.Builder()
            .url("wss://irc-ws.chat.twitch.tv:443")
            .build()

        isClosed = false
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                if (isClosed) return
                // CAP REQ must be sent first, before PASS/NICK/JOIN, per Twitch IRC spec.
                // This ensures all subsequent messages arrive with their full @tags metadata.
                webSocket.send("CAP REQ :twitch.tv/tags twitch.tv/commands twitch.tv/membership")
                webSocket.send("PASS $token")
                webSocket.send("NICK ${username.lowercase()}")
                // Join the specified channel
                val targetChannel = channel.lowercase().trim().removePrefix("#")
                webSocket.send("JOIN #$targetChannel")
                listener.onConnected()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (isClosed) return
                handleRawMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                listener.onDisconnected("Closing: $reason ($code)")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                listener.onDisconnected("Closed: $reason ($code)")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (isClosed) return
                listener.onError(t.message ?: "Connection failure")
            }
        })
    }

    fun disconnect() {
        isClosed = true
        webSocket?.close(1000, "User disconnected")
        webSocket = null
    }

    private fun handleRawMessage(raw: String) {
        val lines = raw.split("\r\n", "\n")
        for (line in lines) {
            if (line.isEmpty()) continue

            if (line.startsWith("PING")) {
                webSocket?.send("PONG :tmi.twitch.tv")
                Log.d("TwitchIrcClient", "Sent PONG")
                continue
            }

            if (line.contains("PRIVMSG")) {
                val parsed = parseIrcMessage(line)
                if (parsed != null) {
                    listener?.onMessageReceived(parsed)
                }
            }
        }
    }

    private fun parseIrcMessage(line: String): TwitchChatMessage? {
        var remainingLine = line.trim()
        if (remainingLine.isEmpty()) return null

        var tags = emptyMap<String, String>()
        
        // 1. Parse Tags
        if (remainingLine.startsWith("@")) {
            val spaceIndex = remainingLine.indexOf(' ')
            if (spaceIndex == -1) return null
            val tagsStr = remainingLine.substring(1, spaceIndex)
            tags = parseTags(tagsStr)
            remainingLine = remainingLine.substring(spaceIndex + 1).trim()
        }

        // 2. Parse Prefix
        var prefix = ""
        if (remainingLine.startsWith(":")) {
            val spaceIndex = remainingLine.indexOf(' ')
            if (spaceIndex == -1) return null
            prefix = remainingLine.substring(1, spaceIndex)
            remainingLine = remainingLine.substring(spaceIndex + 1).trim()
        }

        // 3. Parse Command
        val spaceIndex = remainingLine.indexOf(' ')
        val command = if (spaceIndex != -1) {
            remainingLine.substring(0, spaceIndex)
        } else {
            remainingLine
        }
        
        if (command != "PRIVMSG") return null

        val parameters = if (spaceIndex != -1) {
            remainingLine.substring(spaceIndex + 1).trim()
        } else {
            ""
        }

        // 4. Parse Sender from Prefix
        val sender = if (prefix.contains("!")) {
            prefix.substring(0, prefix.indexOf("!"))
        } else {
            prefix
        }

        // 5. Parse Message Text from Parameters (format: <target> :<message>)
        val messageText = if (parameters.startsWith(":")) {
            parameters.substring(1)
        } else {
            val colonIndex = parameters.indexOf(" :")
            if (colonIndex != -1) {
                parameters.substring(colonIndex + 2)
            } else {
                val spaceIndexParam = parameters.indexOf(' ')
                if (spaceIndexParam != -1) parameters.substring(spaceIndexParam + 1) else parameters
            }
        }

        val displayName = tags["display-name"]?.takeIf { it.isNotEmpty() } ?: sender
        val isMod = tags["mod"] == "1"
        val isSub = tags["subscriber"] == "1"
        val badges = tags["badges"] ?: ""
        // Broadcaster status is determined solely by the badges IRC tag.
        // The previous fallback (sender == displayName) incorrectly flagged ordinary
        // users whose display name happened to match their login.
        val isBroadcaster = badges.contains("broadcaster/")

        val emotes = tags["emotes"] ?: ""
        val cleanSpeech = cleanMessageForSpeech(messageText, emotes)

        return TwitchChatMessage(
            senderName = sender,
            displayName = displayName,
            messageText = messageText,
            cleanSpeechText = cleanSpeech,
            isMod = isMod,
            isSub = isSub,
            isBroadcaster = isBroadcaster
        )
    }

    private fun parseTags(tagsStr: String): Map<String, String> {
        val tags = mutableMapOf<String, String>()
        val parts = tagsStr.split(";")
        for (part in parts) {
            val eqIndex = part.indexOf('=')
            if (eqIndex != -1) {
                val key = part.substring(0, eqIndex)
                val value = part.substring(eqIndex + 1)
                tags[key] = value
            } else {
                tags[part] = ""
            }
        }
        return tags
    }

    private fun cleanMessageForSpeech(messageText: String, emotesTag: String): String {
        var text = cleanEmotes(messageText, emotesTag)
        text = cleanEmojis(text)
        return text
    }

    private fun cleanEmotes(messageText: String, emotesTag: String): String {
        if (emotesTag.isEmpty() || messageText.isEmpty()) return messageText

        val isEmote = BooleanArray(messageText.length)
        try {
            val emotes = emotesTag.split("/")
            for (emote in emotes) {
                val parts = emote.split(":")
                if (parts.size == 2) {
                    val ranges = parts[1].split(",")
                    for (r in ranges) {
                        val bounds = r.split("-")
                        if (bounds.size == 2) {
                            val start = bounds[0].toIntOrNull()
                            val end = bounds[1].toIntOrNull()
                            if (start != null && end != null && start <= end) {
                                for (i in start..end) {
                                    if (i in isEmote.indices) {
                                        isEmote[i] = true
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            return messageText
        }

        val sb = java.lang.StringBuilder(messageText.length)
        for (i in messageText.indices) {
            if (!isEmote[i]) {
                sb.append(messageText[i])
            } else {
                if (sb.isNotEmpty() && sb.last() != ' ') {
                    sb.append(' ')
                }
            }
        }
        return cleanSpaces(sb.toString())
    }

    private fun cleanSpaces(text: String): String {
        val sb = java.lang.StringBuilder(text.length)
        var lastWasSpace = false
        for (char in text) {
            if (char.isWhitespace()) {
                if (!lastWasSpace) {
                    sb.append(' ')
                    lastWasSpace = true
                }
            } else {
                sb.append(char)
                lastWasSpace = false
            }
        }
        return sb.toString().trim()
    }

    /**
     * Strips emoji from [text] using Unicode code-point classification.
     *
     * Iterates over Unicode scalar values (code points) rather than Java `char` values
     * to correctly handle surrogate pairs (all emoji outside the BMP, e.g. 😀 U+1F600).
     * This covers:
     *  - All Emoji in the Supplementary Multilingual Plane (U+1F000–U+1FFFF)
     *  - Miscellaneous Symbols and Dingbats (U+2600–U+27BF)
     *  - Enclosed Alphanumerics and keycap sequences (U+20E3 + digit)
     *  - Regional indicator symbols / flag sequences (U+1F1E0–U+1F1FF surrogate pairs)
     *  - Skin tone modifiers (U+1F3FB–U+1F3FF)
     *  - Zero-width joiner (U+200D) used in compound emoji sequences
     *  - Variation selector-16 (U+FE0F) that renders text as emoji
     */
    private fun cleanEmojis(text: String): String {
        val sb = StringBuilder(text.length)
        var i = 0
        while (i < text.length) {
            val codePoint = text.codePointAt(i)
            val charCount = Character.charCount(codePoint)

            val isEmoji = when {
                // Zero-width joiner (used in compound emoji like family sequences)
                codePoint == 0x200D -> true
                // Variation selector-16 (forces emoji presentation)
                codePoint == 0xFE0F -> true
                // Combining enclosing keycap (used in 1️⃣, *️⃣, etc.)
                codePoint == 0x20E3 -> true
                // Miscellaneous Symbols, Dingbats, arrows, etc. (U+2600–U+27BF)
                codePoint in 0x2600..0x27BF -> true
                // Enclosed Alphanumerics Supplement (U+1F100–U+1F1FF) — includes regional indicators
                codePoint in 0x1F100..0x1F1FF -> true
                // Mahjong through Dominos, playing cards, etc. (U+1F000–U+1F0FF)
                codePoint in 0x1F000..0x1F0FF -> true
                // Enclosed Ideographic Supplement (U+1F200–U+1F2FF)
                codePoint in 0x1F200..0x1F2FF -> true
                // Misc Symbols and Pictographs, Emoticons, Transport, Supplemental (U+1F300–U+1FBFF)
                // This single range covers the vast majority of all modern emoji
                codePoint in 0x1F300..0x1FBFF -> true
                // OTHER_SYMBOL category covers ©, ™, ®, ♻, and other symbol characters
                Character.getType(codePoint) == Character.OTHER_SYMBOL.toInt() -> true
                else -> false
            }

            if (!isEmoji) {
                sb.appendCodePoint(codePoint)
            } else if (sb.isNotEmpty() && sb.last() != ' ') {
                // Replace emoji with a space so adjacent words don't merge
                sb.append(' ')
            }
            i += charCount
        }
        return cleanSpaces(sb.toString())
    }
}
