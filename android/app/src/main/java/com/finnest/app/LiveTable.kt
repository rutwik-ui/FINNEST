package com.finnest.app

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Calls [onChange] on the main thread whenever rows of a public table change, for as long as the screen is open. */
object LiveTable {
    @OptIn(FlowPreview::class)
    fun watch(activity: AppCompatActivity, table: String, onChange: () -> Unit) {
        val app = activity.applicationContext
        activity.lifecycleScope.launch(Dispatchers.IO) {
            val channel = AccountClient.get(app).channel("android-$table-${java.util.UUID.randomUUID()}")
            try {
                // Admin bulk edits arrive as many events; one reload covers them.
                channel.postgresChangeFlow<PostgresAction>("public") { this.table = table }
                    .debounce(600).onEach { withContext(Dispatchers.Main) { if (!activity.isFinishing && !activity.isDestroyed) onChange() } }.launchIn(this)
                channel.subscribe(true)
                awaitCancellation()
            } catch (_: Exception) {
            } finally { withContext(NonCancellable) { runCatching { channel.unsubscribe() } } }
        }
    }
}
