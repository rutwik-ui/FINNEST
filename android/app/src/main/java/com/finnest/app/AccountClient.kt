package com.finnest.app

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.SessionManager
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.user.UserSession
import io.github.jan.supabase.realtime.Realtime
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class EncryptedSessionManager(context: Context) : SessionManager {
    private val prefs = EncryptedSharedPreferences.create(context, "finnest-account-session",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)
    override suspend fun saveSession(session: UserSession) { check(prefs.edit().putString("session", Json.encodeToString(session)).commit()) }
    override suspend fun loadSession(): UserSession? = prefs.getString("session", null)?.let { Json.decodeFromString<UserSession>(it) }
    override suspend fun deleteSession() { check(prefs.edit().remove("session").commit()) }
}

object AccountClient {
    private var instance: io.github.jan.supabase.SupabaseClient? = null
    fun get(context: Context): io.github.jan.supabase.SupabaseClient = instance ?: synchronized(this) {
        instance ?: createSupabaseClient(BuildConfig.GUIDANCE_URL, BuildConfig.GUIDANCE_PUBLISHABLE_KEY) {
            install(Auth) { sessionManager = EncryptedSessionManager(context.applicationContext); scheme = "finnest"; host = "auth" }
            install(Realtime)
        }.also { instance = it }
    }
    fun userId(context: Context): String? = get(context).auth.currentUserOrNull()?.id
    fun scoped(context: Context, journey: String) = userId(context)?.let { "account:$it:$journey" } ?: journey
}