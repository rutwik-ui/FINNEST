package com.finnest.app

import android.content.Context
import android.os.Handler
import android.os.Looper
import io.github.jan.supabase.realtime.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.atomic.AtomicInteger

/**
 * Keeps the published admin "App settings" current on the phone: a Realtime subscription reloads
 * them as soon as an admin publishes, and screens re-check them when they open (at most every
 * five minutes) in case the live connection dropped.
 */
object AppSettingsLive {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val main = Handler(Looper.getMainLooper())
    private var job: Job? = null
    private val listeners = CopyOnWriteArraySet<() -> Unit>()
    val revision = AtomicInteger(0)

    fun addListener(listener: () -> Unit) { listeners.add(listener) }
    fun removeListener(listener: () -> Unit) { listeners.remove(listener) }

    fun start(context: Context) {
        val app = context.applicationContext
        if (job?.isActive == true) return
        job = scope.launch {
            val channel = AccountClient.get(app).channel("android-settings-${java.util.UUID.randomUUID()}")
            try {
                channel.postgresChangeFlow<PostgresAction>("public") { table = "finnest_app_settings"; filter = "state=eq.published" }
                    .onEach { refreshNow(app) }.launchIn(this)
                channel.subscribe(true)
                awaitCancellation()
            } catch (_: Exception) {
            } finally { withContext(NonCancellable) { runCatching { channel.unsubscribe() } } }
        }
    }

    suspend fun refreshNow(context: Context) {
        val app = context.applicationContext
        val before = PublicAppSettings.cached(app)
        val after = PublicAppSettings.refresh(app)
        if (after != before) {
            revision.incrementAndGet()
            main.post { listeners.forEach { it() } }
        }
        // New announcements reach the phone straight away instead of at the next 15-minute check.
        FinnestNotifications.checkSoon(app)
    }

    fun refreshIfStale(context: Context) {
        val app = context.applicationContext
        if (System.currentTimeMillis() - PublicAppSettings.fetchedAt(app) > 5 * 60_000L) scope.launch { refreshNow(app) }
    }

    fun refreshInBackground(context: Context, then: () -> Unit) {
        val app = context.applicationContext
        scope.launch { refreshNow(app); main.post(then) }
    }
}
