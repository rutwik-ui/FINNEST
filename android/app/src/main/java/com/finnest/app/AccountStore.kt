package com.finnest.app

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Session storage for the signed-in buyer.
 *
 * Tokens are bearer credentials: anyone holding them can act as this user until
 * they expire. They are therefore kept in EncryptedSharedPreferences (Android
 * Keystore-backed) rather than plain preferences, and never logged.
 */
data class AccountSession(
    val userId: String,
    val email: String?,
    val accessToken: String,
    val refreshToken: String,
    val expiresAtMillis: Long,
)

object AccountStore {
    private const val FILE = "finnest_account"
    private const val K_UID = "user_id"
    private const val K_EMAIL = "email"
    private const val K_ACCESS = "access_token"
    private const val K_REFRESH = "refresh_token"
    private const val K_EXPIRES = "expires_at"

    @Volatile private var cached: SharedPreferences? = null

    private fun prefs(context: Context): SharedPreferences? {
        cached?.let { return it }
        return synchronized(this) {
            cached ?: runCatching {
                val key = MasterKey.Builder(context.applicationContext)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
                EncryptedSharedPreferences.create(
                    context.applicationContext,
                    FILE,
                    key,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
                ).also { cached = it }
            }.getOrElse { null }
        }
    }

    fun load(context: Context): AccountSession? {
        val p = prefs(context) ?: return null
        val uid = p.getString(K_UID, null) ?: return null
        val access = p.getString(K_ACCESS, null) ?: return null
        val refresh = p.getString(K_REFRESH, null) ?: return null
        val expires = p.getLong(K_EXPIRES, 0L)
        if (uid.isBlank() || access.isBlank()) return null
        return AccountSession(uid, p.getString(K_EMAIL, null), access, refresh, expires)
    }

    fun save(context: Context, session: AccountSession): Boolean {
        val p = prefs(context) ?: return false
        return p.edit()
            .putString(K_UID, session.userId)
            .putString(K_EMAIL, session.email)
            .putString(K_ACCESS, session.accessToken)
            .putString(K_REFRESH, session.refreshToken)
            .putLong(K_EXPIRES, session.expiresAtMillis)
            .commit()
    }

    fun clear(context: Context) {
        prefs(context)?.edit()?.clear()?.commit()
    }

    fun isSignedIn(context: Context): Boolean = load(context) != null

    fun email(context: Context): String? = load(context)?.email

    /** Refresh slightly before expiry so a request never races the clock. */
    fun needsRefresh(session: AccountSession, now: Long = System.currentTimeMillis()): Boolean =
        session.expiresAtMillis in 1..(now + 60_000)
}
