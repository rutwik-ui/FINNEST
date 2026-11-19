package com.finnest.app

import android.content.Intent
import android.net.Uri
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import io.github.jan.supabase.realtime.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.Executors

data class Expert(val id: String, val name: String, val specialty: String, val city: String, val phone: String, val email: String, val bio: String)

/** Parses the PostgREST response for published experts; blank names are skipped. */
object ExpertsContract {
    fun parse(text: String): List<Expert> {
        val rows = JSONArray(text)
        return (0 until rows.length()).mapNotNull { i ->
            val o = rows.optJSONObject(i) ?: return@mapNotNull null
            fun s(key: String) = if (o.isNull(key)) "" else o.optString(key).trim()
            Expert(s("id"), s("name"), s("specialty"), s("city"), s("phone"), s("email"), s("bio")).takeIf { it.id.isNotEmpty() && it.name.isNotEmpty() }
        }
    }
    fun matches(expert: Expert, role: String?, query: String): Boolean {
        if (role != null && expert.specialty != role) return false
        val q = query.trim().lowercase()
        return q.isEmpty() || listOf(expert.name, expert.specialty, expert.city).any { it.lowercase().contains(q) }
    }
}

/**
 * The published experts from the admin panel, the same rows the website shows. Loads from Supabase
 * and reloads live when an admin adds, edits or removes an expert.
 */
class ExpertsDirectory(private val activity: AppCompatActivity, private val parent: LinearLayout, private val roleFilter: () -> String?) {
    private val io = Executors.newSingleThreadExecutor()
    private val live = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var all: List<Expert> = emptyList()
    private var query = ""
    private var shown = PAGE
    private var generation = 0
    private val status: TextView
    private val list = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
    private val more: Button
    private fun dp(n: Int) = FinnestUi.dp(activity, n)

    init {
        val search = EditText(activity).apply {
            hint = "Name, city or specialty"; inputType = EditorInfo.TYPE_CLASS_TEXT; imeOptions = EditorInfo.IME_ACTION_SEARCH
            setSingleLine(true); contentDescription = "Search experts"
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
                override fun afterTextChanged(s: Editable?) { query = s?.toString().orEmpty(); shown = PAGE; render() }
            })
        }
        parent.addView(TextView(activity).apply { text = "Search experts" }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })
        parent.addView(search, LinearLayout.LayoutParams(-1, -2))
        status = FinnestUi.body(activity, "Loading experts…", 13.5f)
        parent.addView(status, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })
        parent.addView(list, LinearLayout.LayoutParams(-1, -2))
        more = Button(activity).apply { text = "Show more experts"; isAllCaps = false; visibility = View.GONE; setOnClickListener { shown += PAGE; render() } }
        parent.addView(more, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })
    }

    fun start() {
        load()
        // Live updates: any change to the experts table reloads the list, as on the website.
        live.launch {
            val channel = AccountClient.get(activity.applicationContext).channel("android-experts-${java.util.UUID.randomUUID()}")
            try {
                channel.postgresChangeFlow<PostgresAction>("public") { table = "finnest_experts" }.onEach { activity.runOnUiThread { load() } }.launchIn(this)
                channel.subscribe(true)
                kotlinx.coroutines.awaitCancellation()
            } catch (_: Exception) {
            } finally { withContext(NonCancellable) { runCatching { channel.unsubscribe() } } }
        }
    }

    fun stop() { generation++; live.cancel(); io.shutdownNow() }

    fun refilter() { shown = PAGE; render() }

    private fun load() {
        val request = ++generation
        if (all.isEmpty()) status.text = "Loading experts…"
        io.execute {
            val result = runCatching { fetch() }
            activity.runOnUiThread {
                if (activity.isFinishing || activity.isDestroyed || request != generation) return@runOnUiThread
                result.fold(onSuccess = { all = it; render() }, onFailure = {
                    status.text = if (all.isEmpty()) "Couldn't load experts. Check your connection and try again." else "Couldn't refresh experts. Showing the last list."
                    if (all.isEmpty()) { list.removeAllViews(); list.addView(Button(activity).apply { text = "Try again"; isAllCaps = false; setOnClickListener { load() } }) }
                })
            }
        }
    }

    private fun fetch(): List<Expert> {
        val select = URLEncoder.encode("id,name,specialty,city,phone,email,bio", "UTF-8")
        val connection = URL("${BuildConfig.GUIDANCE_URL}/rest/v1/finnest_experts?select=$select&status=eq.published&order=name.asc&limit=1000").openConnection() as HttpURLConnection
        connection.connectTimeout = 10000; connection.readTimeout = 15000; connection.instanceFollowRedirects = false
        connection.setRequestProperty("apikey", BuildConfig.GUIDANCE_PUBLISHABLE_KEY)
        connection.setRequestProperty("Accept", "application/json")
        try {
            val code = connection.responseCode
            if (code !in 200..299) error("HTTP $code")
            return ExpertsContract.parse(connection.inputStream.bufferedReader().use { it.readText() })
        } finally { connection.disconnect() }
    }

    private fun render() {
        val role = roleFilter()
        val visible = all.filter { ExpertsContract.matches(it, role, query) }
        list.removeAllViews()
        status.text = when {
            all.isEmpty() -> "No experts are listed yet."
            visible.isEmpty() -> "No matching experts. Try a different search or category."
            else -> "${visible.size} ${if (visible.size == 1) "expert" else "experts"} · listed by FINNEST. Check credentials yourself before engaging anyone."
        }
        visible.take(shown).forEach { list.addView(card(it), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) }) }
        more.visibility = if (visible.size > shown) View.VISIBLE else View.GONE
        more.text = "Show more experts (${visible.size - minOf(shown, visible.size)} more)"
    }

    private fun card(expert: Expert): View = FinnestUi.card(activity).apply {
        orientation = LinearLayout.VERTICAL
        val head = LinearLayout(activity).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        head.addView(android.widget.FrameLayout(activity).apply {
            background = FinnestUi.shape(activity, FinnestUi.BLUSH, null, 999)
            addView(ImageView(activity).apply { setImageResource(R.drawable.journey_user); imageTintList = android.content.res.ColorStateList.valueOf(FinnestUi.RED_DARK) },
                android.widget.FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER))
        }, LinearLayout.LayoutParams(dp(40), dp(40)))
        val names = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        names.addView(FinnestUi.label(activity, expert.name, 16f, 600, FinnestUi.NAVY), LinearLayout.LayoutParams(-1, -2))
        names.addView(FinnestUi.label(activity, listOf(expert.specialty, expert.city).filter { it.isNotEmpty() }.joinToString(" · "), 13f, 500, FinnestUi.MUTED), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(2) })
        head.addView(names, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(12) })
        addView(head, LinearLayout.LayoutParams(-1, -2))
        if (expert.bio.isNotEmpty()) addView(FinnestUi.body(activity, expert.bio, 14f).apply { maxLines = 4; ellipsize = android.text.TextUtils.TruncateAt.END }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })
        val actions = LinearLayout(activity).apply { orientation = LinearLayout.HORIZONTAL }
        if (expert.phone.isNotEmpty()) actions.addView(com.google.android.material.button.MaterialButton(activity).apply {
            text = "Call"; contentDescription = "Call ${expert.name}"
            setOnClickListener { runCatching { activity.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + expert.phone.filter { c -> c.isDigit() || c == '+' }))) } }
        }, LinearLayout.LayoutParams(0, -2, 1f))
        if (expert.email.isNotEmpty()) actions.addView(com.google.android.material.button.MaterialButton(activity).apply {
            text = "Email"; contentDescription = "Email ${expert.name}"
            setOnClickListener { runCatching { activity.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + expert.email))) } }
        }, LinearLayout.LayoutParams(0, -2, 1f).apply { if (expert.phone.isNotEmpty()) marginStart = dp(10) })
        if (actions.childCount > 0) addView(actions, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })
    }

    private companion object { const val PAGE = 25 }
}
