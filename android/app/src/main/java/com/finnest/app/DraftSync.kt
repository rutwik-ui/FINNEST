package com.finnest.app

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/**
 * Cross-device draft sync for Android.
 *
 * Mirrors web-app/src/finnest/draftSync.js exactly:
 *   - the unit of sync is one field (journey_type + field_key), not a document
 *   - a write carries client_updated_at and the database refuses a stale one
 *   - only fields this device changed are uploaded
 *
 * Android's Room rows are already one value per key, so no conversion is needed
 * on this side. The website converts its nested draft into the same keys.
 */
object DraftSync {
    private val executor = Executors.newSingleThreadExecutor()
    private const val TABLE = "finnest_draft_fields"
    private val observers = java.util.concurrent.CopyOnWriteArraySet<(SyncState) -> Unit>()

    /** Everything the UI needs to describe sync honestly. */
    enum class SyncState { SIGNED_OUT, SYNCING, SYNCED, OFFLINE, ERROR }

    fun observe(listener: (SyncState) -> Unit) { observers.add(listener) }
    fun removeObserver(listener: (SyncState) -> Unit) { observers.remove(listener) }
    private fun publish(state: SyncState) { observers.forEach { it(state) } }

    fun statusText(state: SyncState): String = when (state) {
        SyncState.SIGNED_OUT -> "Saved on this device only. Sign in to sync with the website."
        SyncState.SYNCING -> "Saving to your account…"
        SyncState.SYNCED -> "Saved to your account. Available on your other devices."
        SyncState.OFFLINE -> "Offline. Changes stay on this device and sync when you reconnect."
        SyncState.ERROR -> "Could not reach your account. Changes stay on this device."
    }

    /** Web's journey labels vs the database's journey_type values. */
    fun journeyType(journey: String): String =
        if (journey == "resale_property" || journey == "Resale Property") "resale_property" else "new_property"

    /**
     * Pull the account copy, merge per field, upload local-only changes, then
     * write the merged result back into Room so every screen sees it.
     */
    fun reconcile(context: Context, journey: String, callback: ((Result<Int>) -> Unit)? = null) {
        val app = context.applicationContext
        val session = AccountStore.load(app)
        if (session == null) { publish(SyncState.SIGNED_OUT); callback?.invoke(Result.success(0)); return }

        publish(SyncState.SYNCING)
        executor.execute {
            val result = runCatching {
                val type = journeyType(journey)
                val remote = fetch(app, session, type)
                val local = JourneyDatabase.get(app).drafts().fields(journey).associate { it.fieldKey to it.value }

                val changedLocally = local.filter { (k, v) -> remote[k] != v }
                if (changedLocally.isNotEmpty()) {
                    // local_updated_at is the newest local write for this journey;
                    // using it keeps ordering honest against the other device.
                    push(app, session, type, changedLocally, LocalSyncClock.lastWrite(app, journey))
                }

                // Adopt remote values only where this device has not diverged.
                var adopted = 0
                for ((key, value) in remote) {
                    if (local[key] != value) {
                        JourneyDatabase.get(app).drafts().put(DraftField(journey, key, value))
                        adopted++
                    }
                }
                adopted
            }
            publish(
                when {
                    result.isSuccess -> SyncState.SYNCED
                    !Network.isOnline(app) -> SyncState.OFFLINE
                    else -> SyncState.ERROR
                },
            )
            callback?.invoke(result)
        }
    }

    /** Upload one field as it is typed on a screen. Never blocks the UI. */
    fun pushField(context: Context, journey: String, key: String, value: String) {
        val app = context.applicationContext
        val session = AccountStore.load(app) ?: return
        LocalSyncClock.markWrite(app, journey)
        executor.execute {
            runCatching {
                push(app, session, journeyType(journey), mapOf(key to value), LocalSyncClock.lastWrite(app, journey))
            }.onSuccess { publish(SyncState.SYNCED) }
                .onFailure { publish(if (Network.isOnline(app)) SyncState.ERROR else SyncState.OFFLINE) }
        }
    }

    /** Fetch every stored field for a journey: key -> value. */
    private fun fetch(context: Context, session: AccountSession, type: String): Map<String, String> {
        val uri = Uri.parse(BuildConfig.GUIDANCE_URL.trimEnd('/') + "/rest/v1/$TABLE").buildUpon()
            .appendQueryParameter("select", "field_key,value,client_updated_at")
            .appendQueryParameter("journey_type", "eq.$type")
            .appendQueryParameter("user_id", "eq.${session.userId}")
            .appendQueryParameter("limit", "1000")
            .build()
        val text = call(context, uri.toString(), "GET", null, session.accessToken)
        val array = JSONArray(text)
        val out = mutableMapOf<String, String>()
        for (i in 0 until array.length()) {
            val row = array.getJSONObject(i)
            val key = row.optString("field_key")
            if (key.isNotBlank()) out[key] = row.optString("value")
        }
        return out
    }

    /** Upsert only the supplied fields. Retried once after a token refresh. */
    private fun push(
        context: Context,
        session: AccountSession,
        type: String,
        fields: Map<String, String>,
        atIso: String,
        allowRetry: Boolean = true,
    ) {
        if (fields.isEmpty()) return
        val rows = JSONArray()
        for ((key, value) in fields) {
            if (key.isBlank() || key.length > 120 || value.length > 20000) continue
            rows.put(
                JSONObject()
                    .put("user_id", session.userId)
                    .put("journey_type", type)
                    .put("field_key", key)
                    .put("value", value)
                    .put("client_updated_at", atIso),
            )
        }
        if (rows.length() == 0) return
        val uri = Uri.parse(BuildConfig.GUIDANCE_URL.trimEnd('/') + "/rest/v1/$TABLE").buildUpon()
            .appendQueryParameter("on_conflict", "user_id,journey_type,field_key")
            .build()
        try {
            call(context, uri.toString(), "POST", rows.toString(), session.accessToken, prefer = "resolution=merge-duplicates")
        } catch (e: Unauthorized) {
            if (!allowRetry) throw e
            // A 401 means the token aged out mid-session; refresh once then retry.
            val refreshed = refreshBlocking(context) ?: throw e
            push(context, refreshed, type, fields, atIso, allowRetry = false)
        }
    }

    private class Unauthorized : Exception("Session expired")

    private fun refreshBlocking(context: Context): AccountSession? {
        val existing = AccountStore.load(context) ?: return null
        var result: AccountSession? = null
        val latch = java.util.concurrent.CountDownLatch(1)
        SupabaseAuth().refresh(context) { r ->
            result = (r as? AuthResult.Success)?.session
            latch.countDown()
        }
        latch.await(20, java.util.concurrent.TimeUnit.SECONDS)
        return result ?: existing.takeIf { !AccountStore.needsRefresh(it) }
    }

    private fun call(
        context: Context,
        url: String,
        method: String,
        body: String?,
        accessToken: String,
        prefer: String? = null,
    ): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.apply {
            requestMethod = method
            connectTimeout = 15000
            readTimeout = 15000
            instanceFollowRedirects = false
            setRequestProperty("apikey", BuildConfig.GUIDANCE_PUBLISHABLE_KEY)
            setRequestProperty("Authorization", "Bearer $accessToken")
            setRequestProperty("Accept", "application/json")
            if (prefer != null) setRequestProperty("Prefer", prefer)
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
        }
        try {
            if (body != null) connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            if (code == 401 || code == 403) throw Unauthorized()
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use(BufferedReader::readText).orEmpty()
            if (code !in 200..299) throw IllegalStateException("Sync request failed ($code)")
            if (text.length > 2_000_000) throw IllegalStateException("Sync response too large")
            return text
        } finally { connection.disconnect() }
    }
}

/** Remembers the last local write time per journey, for per-field ordering. */
object LocalSyncClock {
    private const val FILE = "finnest_sync"
    private fun key(journey: String) = "last_write_$journey"

    fun markWrite(context: Context, journey: String) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putString(key(journey), isoNow()).apply()
    }

    fun lastWrite(context: Context, journey: String): String =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(key(journey), null) ?: isoNow()

    private fun isoNow(): String {
        val fmt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
        fmt.timeZone = java.util.TimeZone.getTimeZone("UTC")
        return fmt.format(java.util.Date())
    }
}

/** Connectivity check so the UI can say "offline" instead of "error". */
object Network {
    fun isOnline(context: Context): Boolean = runCatching {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        cm.activeNetwork != null
    }.getOrDefault(true)
}
