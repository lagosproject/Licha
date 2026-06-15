package com.lakescorp.twitchchattts.data

import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * HTTP client for Twitch API operations.
 *
 * All methods are suspending functions so callers can use structured concurrency
 * directly without callback bridges. OkHttp calls are cancelled automatically
 * when the calling coroutine scope is cancelled (e.g., on ViewModel.onCleared).
 */
@Singleton
class TwitchApiClient @Inject constructor(
    private val client: OkHttpClient
) {

    /**
     * Validates [oauthToken] against the Twitch ID API.
     *
     * @return The authenticated username ("login" field) on success.
     * @throws IOException On network failure.
     * @throws IllegalStateException On HTTP error or malformed response.
     */
    suspend fun validateToken(oauthToken: String): String {
        val token = if (oauthToken.startsWith("oauth:", ignoreCase = true)) {
            oauthToken.substring(6)
        } else {
            oauthToken
        }

        val request = Request.Builder()
            .url("https://id.twitch.tv/oauth2/validate")
            .addHeader("Authorization", "Bearer $token")
            .get()
            .build()

        return suspendCancellableCoroutine { cont ->
            val call = client.newCall(request)
            cont.invokeOnCancellation { call.cancel() }

            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    cont.resumeWithException(IOException(e.message ?: "Network error occurred", e))
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        if (!response.isSuccessful) {
                            cont.resumeWithException(
                                IllegalStateException("Validation failed: HTTP ${response.code}")
                            )
                            return
                        }
                        val body = response.body?.string()
                        if (body.isNullOrEmpty()) {
                            cont.resumeWithException(
                                IllegalStateException("Empty response from validation server")
                            )
                            return
                        }
                        try {
                            val username = JSONObject(body).getString("login")
                            cont.resume(username)
                        } catch (e: Exception) {
                            cont.resumeWithException(
                                IllegalStateException("Failed to parse response: ${e.message}", e)
                            )
                        }
                    }
                }
            })
        }
    }
}
