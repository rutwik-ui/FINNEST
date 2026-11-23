package com.finnest.app

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Small account-level links with the website: notification choices (stored in the account's
 * `finnest_notifications` metadata, the same place the website keeps them) and sign-in activity.
 */
object AccountExtras {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private const val KEY = "finnest_notifications"
    private val kinds = listOf(FinnestNotifications.REMINDERS, FinnestNotifications.NUDGES, FinnestNotifications.SECURITY, FinnestNotifications.ANNOUNCEMENTS)

    /** Copies the account's saved choices onto this phone (website changes win when signed in). */
    fun pullNotificationPrefs(context: Context) {
        val app = context.applicationContext
        if (AccountClient.userId(app) == null) return
        scope.launch {
            runCatching {
                val (status, body) = AccountSync.request(app, "/auth/v1/user")
                if (status !in 200..299) return@runCatching
                val saved = JSONObject(body).optJSONObject("user_metadata")?.optJSONObject(KEY) ?: return@runCatching
                kinds.forEach { kind -> if (saved.has(kind)) FinnestNotifications.setLocal(app, kind, saved.optBoolean(kind, true)) }
            }
        }
    }

    /** Saves the phone's choices to the account, keeping the website-only "browser" setting as it is. */
    fun pushNotificationPrefs(context: Context) {
        val app = context.applicationContext
        if (AccountClient.userId(app) == null) return
        scope.launch {
            runCatching {
                val (status, body) = AccountSync.request(app, "/auth/v1/user")
                val merged = (if (status in 200..299) JSONObject(body).optJSONObject("user_metadata")?.optJSONObject(KEY) else null) ?: JSONObject()
                kinds.forEach { merged.put(it, FinnestNotifications.enabled(app, it)) }
                AccountSync.request(app, "/auth/v1/user", "PUT", JSONObject().put("data", JSONObject().put(KEY, merged)).toString())
            }
        }
    }

    /** Records a sign-in in the account's activity log, so it shows in admin with the platform. */
    fun logSignIn(context: Context, signup: Boolean) {
        val app = context.applicationContext
        val user = AccountClient.userId(app) ?: return
        scope.launch {
            runCatching {
                val event = JSONObject().put("user_id", user).put("event", if (signup) "signed_up" else "signed_in").put("platform", "android")
                    .put("detail", JSONObject().put("app_version", BuildConfig.VERSION_NAME))
                AccountSync.request(app, "/rest/v1/finnest_activity", "POST", event.toString())
            }
        }
    }

    /** Shares the New/Resale choice made as a guest once the person signs in. */
    fun shareJourneyChoice(context: Context) {
        val chosen = context.getSharedPreferences("entry", Context.MODE_PRIVATE).getString("journey", null) ?: return
        if (chosen == "New Property" || chosen == "Resale Property") DraftRepository.save(context, "new_property", "selected_journey", chosen)
    }
}
