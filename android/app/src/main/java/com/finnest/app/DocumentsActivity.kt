package com.finnest.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.finnest.app.databinding.ActivityDeveloperChecksBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * My documents: the same private document store as the website's Profile page. Files go to the
 * account's own folder in private storage and open through short-lived links only.
 */
class DocumentsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDeveloperChecksBinding
    private lateinit var status: TextView
    private lateinit var list: LinearLayout
    private lateinit var uploadButton: View
    private var resale = false

    private val picker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) upload(uri) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeveloperChecksBinding.inflate(layoutInflater)
        setContentView(binding.root)
        resale = getSharedPreferences("entry", MODE_PRIVATE).getString("journey", "New Property") == "Resale Property"
        FinnestUi.decorate(this, if (resale) "resale_property" else "new_property", roadmap = false)
        binding.stepLabel.text = "ACCOUNT"
        binding.screenTitle.text = "My documents"
        binding.screenDescription.text = "Keep copies of your documents with your journey. They're private to your account and also appear on the website. FINNEST support staff can view them; FINNEST doesn't verify documents."
        binding.screenNotice.visibility = View.GONE
        binding.loading.visibility = View.GONE
        binding.saveLater.text = "Home"; binding.next.text = "Done"; binding.backBottom.text = "Back"
        binding.back.setOnClickListener { finish() }
        binding.backBottom.setOnClickListener { finish() }
        binding.saveLater.setOnClickListener { DashboardActivity.show(this) }
        binding.next.setOnClickListener { finish() }
        binding.next.isEnabled = true

        val card = FinnestUi.card(this).apply { orientation = LinearLayout.VERTICAL }
        binding.form.addView(card, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(14) })
        if (AccountClient.userId(this) == null) {
            card.addView(FinnestUi.h2(this, "Sign in to keep documents", 18f), LinearLayout.LayoutParams(-1, -2))
            card.addView(FinnestUi.body(this, "Documents are stored privately in your account, so you need to be signed in to upload or open them.", 14f), gap(6))
            card.addView(FinnestUi.primaryButton(this, "Log in") { startActivity(Intent(this, AccountActivity::class.java)) }, gap(14))
            card.addView(FinnestUi.link(this, "Create an account") { startActivity(Intent(this, AccountActivity::class.java).putExtra("signup", true)) }, gap(12))
            return
        }
        card.addView(FinnestUi.h2(this, "Your files", 18f), LinearLayout.LayoutParams(-1, -2))
        card.addView(FinnestUi.body(this, "PDF, JPG or PNG, up to 10 MB each.", 13.5f), gap(4))
        uploadButton = FinnestUi.primaryButton(this, "Upload a file") { picker.launch(arrayOf("application/pdf", "image/jpeg", "image/png")) }
        card.addView(uploadButton, gap(14))
        status = FinnestUi.body(this, "", 13f).apply { visibility = View.GONE }
        card.addView(status, gap(8))
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        card.addView(list, gap(10))
        load()
        LiveTable.watch(this, "finnest_documents") { load() }
    }

    private fun say(text: String) { status.text = text; status.visibility = if (text.isEmpty()) View.GONE else View.VISIBLE }

    private fun upload(uri: Uri) {
        val user = AccountClient.userId(this) ?: return
        val type = contentResolver.getType(uri) ?: ""
        var name = "document"; var size = -1L
        runCatching {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { c ->
                if (c.moveToFirst()) { c.getString(0)?.let { name = it }; if (!c.isNull(1)) size = c.getLong(1) }
            }
        }
        if (type !in ALLOWED) { say("Upload a PDF, JPG or PNG file."); return }
        if (size > MAX_BYTES) { say("Files must be 10 MB or smaller."); return }
        uploadButton.isEnabled = false; say("Uploading…")
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    // Read at most one byte past the limit, so a file without a reported size can't exhaust memory.
                    val bytes = contentResolver.openInputStream(uri)?.use { input ->
                        val out = java.io.ByteArrayOutputStream(); val buffer = ByteArray(64 * 1024); var total = 0L
                        while (true) { val n = input.read(buffer); if (n < 0) break; total += n; if (total > MAX_BYTES) return@runCatching "too_big"; out.write(buffer, 0, n) }
                        out.toByteArray()
                    } ?: return@runCatching "failed"
                    val path = "$user/${java.util.UUID.randomUUID()}-${safeName(name)}"
                    val (code, _) = AccountSync.upload(this@DocumentsActivity, "/storage/v1/object/$BUCKET/${encode(path)}", bytes, type)
                    if (code !in 200..299) return@runCatching "failed"
                    val row = JSONObject().put("user_id", user).put("path", path).put("name", name.take(150)).put("journey_type", if (resale) "resale" else "new")
                    val (saved, _) = AccountSync.request(this@DocumentsActivity, "/rest/v1/finnest_documents", "POST", row.toString())
                    if (saved !in 200..299) return@runCatching "failed"
                    AccountSync.request(this@DocumentsActivity, "/rest/v1/finnest_activity", "POST",
                        JSONObject().put("user_id", user).put("event", "document_uploaded").put("platform", "android").put("detail", JSONObject().put("name", name.take(150))).toString())
                    "ok"
                }.getOrDefault("failed")
            }
            if (isFinishing || isDestroyed) return@launch
            uploadButton.isEnabled = true
            say(when (result) { "ok" -> "Uploaded."; "too_big" -> "Files must be 10 MB or smaller."; else -> "Upload failed. Check your connection and try again." })
            if (result == "ok") load()
        }
    }

    private fun load() {
        val user = AccountClient.userId(this) ?: return
        lifecycleScope.launch {
            val rows = withContext(Dispatchers.IO) {
                runCatching {
                    val (code, body) = AccountSync.request(this@DocumentsActivity, "/rest/v1/finnest_documents?select=id,name,path,journey_type,uploaded_at&user_id=eq.$user&order=uploaded_at.desc&limit=200")
                    if (code in 200..299) JSONArray(body) else null
                }.getOrNull()
            }
            if (isFinishing || isDestroyed) return@launch
            list.removeAllViews()
            when {
                rows == null -> list.addView(FinnestUi.body(this@DocumentsActivity, "Couldn't load your documents. Check your connection.", 13.5f))
                rows.length() == 0 -> list.addView(FinnestUi.body(this@DocumentsActivity, "No documents yet.", 13.5f))
                else -> for (i in 0 until rows.length()) list.addView(item(rows.getJSONObject(i)), gap(if (i == 0) 0 else 8))
            }
        }
    }

    private fun item(row: JSONObject): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(14), dp(12), dp(14), dp(12)); background = FinnestUi.shape(context, FinnestUi.CREAM, null, 12)
        val text = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        text.addView(FinnestUi.label(context, row.optString("name"), 14.5f, 600, FinnestUi.NAVY).apply { maxLines = 2 }, LinearLayout.LayoutParams(-1, -2))
        val journey = if (row.optString("journey_type") == "resale") "Resale" else "New property"
        text.addView(FinnestUi.label(context, "$journey · ${date(row.optString("uploaded_at"))}", 12.5f, 500, FinnestUi.MUTED), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(3) })
        addView(text, LinearLayout.LayoutParams(0, -2, 1f))
        addView(FinnestUi.label(context, "Open", 13f, 600, FinnestUi.RED_DARK), LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(12) })
        isClickable = true; isFocusable = true
        contentDescription = "Open ${row.optString("name")}"
        setOnClickListener { open(row.optString("path")) }
    }

    private fun open(path: String) {
        say("Opening…")
        lifecycleScope.launch {
            val url = withContext(Dispatchers.IO) {
                runCatching {
                    val (code, body) = AccountSync.request(this@DocumentsActivity, "/storage/v1/object/sign/$BUCKET/${encode(path)}", "POST", JSONObject().put("expiresIn", 120).toString())
                    if (code !in 200..299) null else JSONObject(body).optString("signedURL").takeIf { it.isNotEmpty() }?.let { BuildConfig.GUIDANCE_URL + "/storage/v1" + it }
                }.getOrNull()
            }
            if (isFinishing || isDestroyed) return@launch
            if (url == null) { say("Couldn't open this file. Please try again."); return@launch }
            say("")
            runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }.onFailure { say("No app on this phone can open this file.") }
        }
    }

    private fun safeName(value: String) = value.replace(Regex("[^\\w.\\-]+"), "_").takeLast(80)
    private fun encode(path: String) = path.split('/').joinToString("/") { URLEncoder.encode(it, "UTF-8").replace("+", "%20") }
    private fun date(iso: String): String = runCatching {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(parser.parse(iso.take(19))!!)
    }.getOrDefault("")
    private fun dp(n: Int) = FinnestUi.dp(this, n)
    private fun gap(top: Int) = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(top) }

    companion object {
        const val BUCKET = "finnest-private-documents"
        const val MAX_BYTES = 10L * 1024 * 1024
        val ALLOWED = setOf("application/pdf", "image/jpeg", "image/png")
    }
}
