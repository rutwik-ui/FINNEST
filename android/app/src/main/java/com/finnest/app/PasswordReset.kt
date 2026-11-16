package com.finnest.app

import android.net.Uri
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Sends a Supabase password-recovery email.
 *
 * The reset link returns to the website, not the app, because the website can
 * open a browser session and prompt for a new password. The Android app then
 * simply signs in with the new password. That keeps one recovery path instead of
 * two, and avoids a deep-link handler collecting a sensitive token.
 */
object PasswordReset {
    fun request(baseUrl: String, publishableKey: String, email: String) {
        require(baseUrl.startsWith("https://"))
        val uri = Uri.parse(baseUrl.trimEnd('/') + "/auth/v1/recover").buildUpon()
            .appendQueryParameter("redirect_to", "$baseUrl/profile")
            .build()
        val connection = URL(uri.toString()).openConnection() as HttpURLConnection
        connection.apply {
            requestMethod = "POST"
            connectTimeout = 15000
            readTimeout = 15000
            instanceFollowRedirects = false
            doOutput = true
            setRequestProperty("apikey", publishableKey)
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
        }
        try {
            val body = JSONObject().put("email", email.trim()).toString()
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            if (connection.responseCode !in 200..299) throw IllegalStateException("Recovery request failed")
        } finally { connection.disconnect() }
    }
}
