package com.finnest.app

import android.content.Context
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/** Result of an account call. Failures carry a message safe to show to a buyer. */
sealed class AuthResult {
    data class Success(val session: AccountSession) : AuthResult()
    data class Failure(val message: String) : AuthResult()
}

/**
 * Minimal Supabase Auth client over HTTPS.
 *
 * Deliberately small: sign up, sign in, refresh, sign out. No third-party SDK, so
 * the APK stays lean and the request shape is auditable. The publishable key is
 * public by design; authorisation is enforced by Supabase RLS, not by this key.
 */
class SupabaseAuth(
    private val baseUrl: String = BuildConfig.GUIDANCE_URL,
    private val publishableKey: String = BuildConfig.GUIDANCE_PUBLISHABLE_KEY,
) {
    private val executor = Executors.newSingleThreadExecutor()

    init {
        require(baseUrl.startsWith("https://")) { "Auth requires HTTPS" }
        require(publishableKey.startsWith("sb_publishable_")) { "A publishable key is required" }
    }

    fun signIn(context: Context, email: String, password: String, callback: (AuthResult) -> Unit) {
        val app = context.applicationContext
        executor.execute {
            val result = runCatching {
                request(
                    path = "/auth/v1/token?grant_type=password",
                    method = "POST",
                    body = JSONObject().put("email", email.trim()).put("password", password).toString(),
                )
            }.fold(
                onSuccess = { json ->
                    val session = parseSession(json)
                    if (session == null) AuthResult.Failure("The account response was not understood.")
                    else if (AccountStore.save(app, session)) AuthResult.Success(session)
                    else AuthResult.Failure("Could not store your session securely on this device.")
                },
                onFailure = { AuthResult.Failure(messageFor(it)) },
            )
            callback(result)
        }
    }

    fun signUp(context: Context, email: String, password: String, callback: (AuthResult) -> Unit) {
        val app = context.applicationContext
        executor.execute {
            val result = runCatching {
                request(
                    path = "/auth/v1/signup",
                    method = "POST",
                    body = JSONObject().put("email", email.trim()).put("password", password).toString(),
                )
            }.fold(
                onSuccess = { json ->
                    // With email confirmation enabled Supabase returns a user but no
                    // session. That is a success that needs confirmation, not a login.
                    val session = parseSession(json)
                    if (session != null && AccountStore.save(app, session)) AuthResult.Success(session)
                    else AuthResult.Failure("Account created. Open the confirmation link we emailed you, then sign in here.")
                },
                onFailure = { AuthResult.Failure(messageFor(it)) },
            )
            callback(result)
        }
    }

    /** Exchange a refresh token. Returns null when the session is no longer valid. */
    fun refresh(context: Context, callback: (AuthResult) -> Unit) {
        val app = context.applicationContext
        val existing = AccountStore.load(app)
            ?: return callback(AuthResult.Failure("Not signed in."))
        executor.execute {
            val result = runCatching {
                request(
                    path = "/auth/v1/token?grant_type=refresh_token",
                    method = "POST",
                    body = JSONObject().put("refresh_token", existing.refreshToken).toString(),
                )
            }.fold(
                onSuccess = { json ->
                    val session = parseSession(json)
                    if (session != null && AccountStore.save(app, session)) AuthResult.Success(session)
                    else {
                        AccountStore.clear(app)
                        AuthResult.Failure("Your session expired. Please sign in again.")
                    }
                },
                onFailure = {
                    // A revoked refresh token is fatal; a network failure is not.
                    if (it is HttpFailure && it.code in 400..499) {
                        AccountStore.clear(app)
                        AuthResult.Failure("Your session expired. Please sign in again.")
                    } else AuthResult.Failure(messageFor(it))
                },
            )
            callback(result)
        }
    }

    fun signOut(context: Context, callback: (() -> Unit)? = null) {
        val app = context.applicationContext
        val existing = AccountStore.load(app)
        AccountStore.clear(app)
        if (existing == null) { callback?.invoke(); return }
        executor.execute {
            // Best effort: local sign-out must succeed even when offline.
            runCatching {
                request(
                    path = "/auth/v1/logout",
                    method = "POST",
                    body = "{}",
                    bearer = existing.accessToken,
                )
            }
            callback?.invoke()
        }
    }

    /** Returns a session with a token that is valid for at least another minute. */
    fun validSession(context: Context, callback: (AccountSession?) -> Unit) {
        val existing = AccountStore.load(context)
        if (existing == null) return callback(null)
        if (!AccountStore.needsRefresh(existing)) return callback(existing)
        refresh(context) { result ->
            callback((result as? AuthResult.Success)?.session)
        }
    }

    private fun parseSession(json: JSONObject): AccountSession? {
        val user = json.optJSONObject("user") ?: return null
        val id = user.optString("id").orEmpty()
        val access = json.optString("access_token").orEmpty()
        val refresh = json.optString("refresh_token").orEmpty()
        if (id.isBlank() || access.isBlank() || refresh.isBlank()) return null
        val expiresIn = json.optLong("expires_in", 3600L)
        return AccountSession(
            userId = id,
            email = user.optString("email").takeIf { it.isNotBlank() },
            accessToken = access,
            refreshToken = refresh,
            expiresAtMillis = System.currentTimeMillis() + expiresIn * 1000,
        )
    }

    private class HttpFailure(val code: Int, message: String) : Exception(message)

    private fun request(path: String, method: String, body: String, bearer: String? = null): JSONObject {
        val connection = URL(baseUrl.trimEnd('/') + path).openConnection() as HttpURLConnection
        connection.apply {
            requestMethod = method
            connectTimeout = 15000
            readTimeout = 15000
            instanceFollowRedirects = false
            doOutput = true
            setRequestProperty("apikey", publishableKey)
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            if (bearer != null) setRequestProperty("Authorization", "Bearer $bearer")
        }
        try {
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use(BufferedReader::readText).orEmpty()
            if (code !in 200..299) {
                val reason = runCatching {
                    val json = JSONObject(text)
                    json.optString("msg").ifBlank { json.optString("error_description").ifBlank { json.optString("message") } }
                }.getOrNull().orEmpty()
                throw HttpFailure(code, reason.ifBlank { "Request failed ($code)" })
            }
            if (text.isBlank()) return JSONObject()
            return JSONObject(text)
        } finally {
            connection.disconnect()
        }
    }

    /** Map low-level failures to something a buyer can act on. */
    private fun messageFor(error: Throwable): String = when {
        error is HttpFailure && error.code == 400 && error.message.contains("Invalid login", true) ->
            "That email and password do not match."
        error is HttpFailure && error.code == 422 ->
            "Check the email address and use a password with at least 8 characters."
        error is HttpFailure && error.code == 429 ->
            "Too many attempts. Wait a minute and try again."
        error is HttpFailure && error.message.isNotBlank() -> error.message
        error is java.net.UnknownHostException || error is java.net.SocketTimeoutException ->
            "No internet connection. Check your network and try again."
        else -> "Could not reach your account. Try again in a moment."
    }
}
