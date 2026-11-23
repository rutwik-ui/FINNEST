package com.finnest.app

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Session storage for the signed-in buyer.
 *
 * Tokens are bearer credentials: anyone holding them can act as this user until
 * they expire. They are therefore encrypted with an AES-256-GCM key that lives in
 * the Android Keystore and never leaves it. No third-party crypto dependency is
 * used, so the APK has one less supply-chain surface.
 *
 * A device that cannot create a Keystore key (rare, e.g. a broken keystore after
 * an OS downgrade) falls back to not persisting the session at all rather than
 * writing a token in cleartext. The buyer then simply signs in again next launch.
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
    private const val KEY_ALIAS = "finnest.session.v1"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val GCM_TAG_BITS = 128

    private const val K_PAYLOAD = "payload"
    private const val K_UID = "user_id"
    private const val K_EMAIL = "email"
    private const val K_EXPIRES = "expires_at"

    @Volatile private var cached: SharedPreferences? = null

    private fun prefs(context: Context): SharedPreferences? {
        cached?.let { return it }
        return synchronized(this) {
            cached ?: context.applicationContext
                .getSharedPreferences(FILE, Context.MODE_PRIVATE)
                .also { cached = it }
        }
    }

    // ------------------------------------------------------------------ crypto
    private fun secretKey(): SecretKey? = runCatching {
        val store = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (store.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey
            ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE).apply {
                init(
                    KeyGenParameterSpec.Builder(
                        KEY_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                    )
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(256)
                        // Deliberately not tied to user authentication: a sync must
                        // work when the app is reopened without a biometric prompt.
                        .build(),
                )
            }.generateKey()
    }.getOrNull()

    /** iv + ciphertext, base64. Returns null when the Keystore is unavailable. */
    private fun seal(plain: String): String? = runCatching {
        val key = secretKey() ?: return null
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val encrypted = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)
    }.getOrNull()

    private fun open(sealed: String): String? = runCatching {
        val key = secretKey() ?: return null
        val bytes = Base64.decode(sealed, Base64.NO_WRAP)
        if (bytes.size <= 12) return null
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, bytes, 0, 12))
        String(cipher.doFinal(bytes, 12, bytes.size - 12), Charsets.UTF_8)
    }.getOrNull()

    // ------------------------------------------------------------------ storage
    fun load(context: Context): AccountSession? {
        val p = prefs(context) ?: return null
        val sealed = p.getString(K_PAYLOAD, null) ?: return null
        val plain = open(sealed) ?: return null
        // "access\u0000refresh" — one sealed blob, so the two tokens cannot drift apart.
        val parts = plain.split('\u0000')
        if (parts.size != 2 || parts[0].isBlank() || parts[1].isBlank()) return null
        val uid = p.getString(K_UID, null) ?: return null
        return AccountSession(
            userId = uid,
            email = p.getString(K_EMAIL, null),
            accessToken = parts[0],
            refreshToken = parts[1],
            expiresAtMillis = p.getLong(K_EXPIRES, 0L),
        )
    }

    fun save(context: Context, session: AccountSession): Boolean {
        val p = prefs(context) ?: return false
        val sealed = seal("${session.accessToken}\u0000${session.refreshToken}") ?: return false
        return p.edit()
            .putString(K_PAYLOAD, sealed)
            .putString(K_UID, session.userId)
            .putString(K_EMAIL, session.email)
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
