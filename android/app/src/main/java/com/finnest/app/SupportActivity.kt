package com.finnest.app

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.finnest.app.databinding.ActivityDeveloperChecksBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Help & feedback: the same support requests as the website's Help page. Requests reach the
 * admin Support inbox, and replies or status changes show here as soon as admins make them.
 */
class SupportActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDeveloperChecksBinding
    private lateinit var list: LinearLayout
    private lateinit var listStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeveloperChecksBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val scope = if (getSharedPreferences("entry", MODE_PRIVATE).getString("journey", "New Property") == "Resale Property") "resale_property" else "new_property"
        FinnestUi.decorate(this, scope, roadmap = false)
        binding.stepLabel.text = "HELP"
        binding.screenTitle.text = "Help & feedback"
        binding.screenDescription.text = "Ask a question, report a problem or tell us what would make FINNEST better. The team usually replies within two working days."
        binding.screenNotice.visibility = View.GONE
        binding.loading.visibility = View.GONE
        binding.saveLater.text = "Home"; binding.next.text = "Done"; binding.backBottom.text = "Back"
        binding.back.setOnClickListener { finish() }
        binding.backBottom.setOnClickListener { finish() }
        binding.saveLater.setOnClickListener { DashboardActivity.show(this) }
        binding.next.setOnClickListener { finish() }
        binding.next.isEnabled = true

        if (AccountClient.userId(this) == null) { signInPrompt(); return }

        val form = section("Send a message")
        val kind = Spinner(this).apply {
            adapter = ArrayAdapter(this@SupportActivity, android.R.layout.simple_spinner_item, CATEGORIES).apply { setDropDownViewResource(R.layout.finnest_spinner_dropdown_item) }
            contentDescription = "Type of message"
        }
        form.addView(FinnestUi.label(this, "Type", 13f, 600, FinnestUi.NAVY), gap(10))
        form.addView(kind, gap(6))
        val subject = EditText(this).apply { hint = "Subject"; inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES; filters = arrayOf(android.text.InputFilter.LengthFilter(120)); contentDescription = "Subject" }
        form.addView(FinnestUi.label(this, "Subject", 13f, 600, FinnestUi.NAVY), gap(14))
        form.addView(subject, gap(6))
        val message = EditText(this).apply {
            hint = "Tell us what happened or what you need"; minLines = 4; gravity = Gravity.TOP or Gravity.START
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            filters = arrayOf(android.text.InputFilter.LengthFilter(5000)); contentDescription = "Message"
        }
        form.addView(FinnestUi.label(this, "Message", 13f, 600, FinnestUi.NAVY), gap(14))
        form.addView(message, gap(6))
        val status = FinnestUi.body(this, "", 13f)
        lateinit var send: View
        send = FinnestUi.primaryButton(this, "Send message") {
            val title = subject.text.toString().trim(); val text = message.text.toString().trim()
            if (title.isEmpty()) { subject.error = "Add a short subject"; return@primaryButton }
            if (text.length < 5) { message.error = "Add a few more details"; return@primaryButton }
            send.isEnabled = false; status.text = "Sending…"
            val category = kind.selectedItem?.toString() ?: CATEGORIES.first()
            lifecycleScope.launch {
                val ok = withContext(Dispatchers.IO) { runCatching { submit(category, title, text) }.getOrDefault(false) }
                send.isEnabled = true
                if (ok) { subject.setText(""); message.setText(""); status.text = "Sent. Our reply will appear below."; load() }
                else status.text = "Couldn't send your message. Check your connection and try again."
            }
        }
        form.addView(send, gap(16))
        form.addView(status, gap(8))

        val mine = section("Your requests")
        listStatus = FinnestUi.body(this, "Loading…", 13.5f)
        mine.addView(listStatus, gap(6))
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        mine.addView(list, gap(4))
        load()
        // Status changes and replies from the admin Support inbox appear without a refresh.
        LiveTable.watch(this, "finnest_support_tickets") { load() }
    }

    private fun submit(category: String, title: String, text: String): Boolean {
        val user = AccountClient.userId(this) ?: return false
        val row = JSONObject().put("user_id", user).put("subject", "[$category] $title".take(150)).put("message", (text + "\n\n— Sent from the FINNEST Android app ${BuildConfig.VERSION_NAME}").take(5000))
        val (code, _) = AccountSync.request(this, "/rest/v1/finnest_support_tickets", "POST", row.toString())
        if (code !in 200..299) return false
        AccountSync.request(this, "/rest/v1/finnest_activity", "POST",
            JSONObject().put("user_id", user).put("event", "support_requested").put("platform", "android").put("detail", JSONObject().put("category", category)).toString())
        return true
    }

    private fun load() {
        val user = AccountClient.userId(this) ?: return
        lifecycleScope.launch {
            val rows = withContext(Dispatchers.IO) {
                runCatching {
                    val (code, body) = AccountSync.request(this@SupportActivity, "/rest/v1/finnest_support_tickets?select=id,subject,status,admin_note,created_at&user_id=eq.$user&order=created_at.desc&limit=20")
                    if (code in 200..299) JSONArray(body) else null
                }.getOrNull()
            }
            if (isFinishing || isDestroyed) return@launch
            list.removeAllViews()
            when {
                rows == null -> listStatus.text = "Couldn't load your requests. They'll appear when you're back online."
                rows.length() == 0 -> listStatus.text = "No requests yet."
                else -> {
                    listStatus.text = ""; listStatus.visibility = View.GONE
                    for (i in 0 until rows.length()) list.addView(ticket(rows.getJSONObject(i)), gap(if (i == 0) 4 else 10))
                }
            }
        }
    }

    private fun ticket(row: JSONObject): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(FinnestUi.dp(context, 14), FinnestUi.dp(context, 12), FinnestUi.dp(context, 14), FinnestUi.dp(context, 12))
        background = FinnestUi.shape(context, FinnestUi.CREAM, null, 12)
        val state = when (row.optString("status")) { "in_progress" -> "In progress"; "resolved" -> "Resolved"; else -> "Open" }
        addView(FinnestUi.label(context, row.optString("subject"), 14.5f, 600, FinnestUi.NAVY), LinearLayout.LayoutParams(-1, -2))
        addView(FinnestUi.label(context, "$state · ${date(row.optString("created_at"))}", 12.5f, 500, FinnestUi.MUTED), LinearLayout.LayoutParams(-1, -2).apply { topMargin = FinnestUi.dp(context, 3) })
        row.optString("admin_note").takeIf { it.isNotBlank() && it != "null" }?.let {
            addView(FinnestUi.body(context, "Reply: $it", 13.5f, FinnestUi.NAVY), LinearLayout.LayoutParams(-1, -2).apply { topMargin = FinnestUi.dp(context, 8) })
        }
        contentDescription = "${row.optString("subject")}. $state"
    }

    private fun date(iso: String): String = runCatching {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(parser.parse(iso.take(19))!!)
    }.getOrDefault("")

    private fun signInPrompt() {
        val card = section("Sign in to contact us")
        card.addView(FinnestUi.body(this, "Messages are linked to your account so we can reply to you securely. Signing in also keeps your journey on the website and this phone.", 14f), gap(6))
        card.addView(FinnestUi.primaryButton(this, "Log in") { startActivity(Intent(this, AccountActivity::class.java)) }, gap(14))
        card.addView(FinnestUi.link(this, "Create an account") { startActivity(Intent(this, AccountActivity::class.java).putExtra("signup", true)) }, gap(12))
    }

    private fun section(title: String): LinearLayout = FinnestUi.card(this).also { card ->
        card.orientation = LinearLayout.VERTICAL
        card.addView(FinnestUi.h2(this, title, 18f), LinearLayout.LayoutParams(-1, -2))
        binding.form.addView(card, LinearLayout.LayoutParams(-1, -2).apply { topMargin = FinnestUi.dp(this@SupportActivity, 14) })
    }

    private fun gap(top: Int) = LinearLayout.LayoutParams(-1, -2).apply { topMargin = FinnestUi.dp(this@SupportActivity, top) }

    companion object { val CATEGORIES = listOf("Question", "Problem", "Feedback", "Account") }
}
