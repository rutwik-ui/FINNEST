package com.finnest.app

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.finnest.app.databinding.ActivityDeveloperChecksBinding
import java.util.concurrent.Executors

class GuidanceActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDeveloperChecksBinding
    private val executor = Executors.newSingleThreadExecutor()
    private var generation = 0
    private var index = 0
    private var loading = false
    private var cursor: GuidanceCursor? = null
    private var items = mutableListOf<GuidanceItem>()
    private lateinit var body: LinearLayout
    private lateinit var status: TextView
    private lateinit var more: Button
    private lateinit var reload: Button
    private val source by lazy { sourceFactory() }
    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeveloperChecksBinding.inflate(layoutInflater); setContentView(binding.root)
        FinnestUi.decorate(this, if (getSharedPreferences("entry", MODE_PRIVATE).getString("journey", "New Property") == "Resale Property") "resale_property" else "new_property", roadmap = false)
        binding.stepLabel.text = "GUIDANCE LIBRARY"
        binding.screenTitle.text = "Helpful guidance"
        binding.screenDescription.text = "Browse published guidance for your home-buying journey. Internet connection required."
        binding.screenNotice.text = "General information only; not property verification or a substitute for qualified advice."
        binding.loading.visibility = View.GONE
        binding.back.setOnClickListener { finish() }; binding.backBottom.setOnClickListener { finish() }
        binding.saveLater.text = "Home"; binding.saveLater.setOnClickListener { DashboardActivity.show(this) }
        binding.backBottom.text = "Back"; binding.next.text = "Back to dashboard"; binding.next.isEnabled = true
        binding.next.setOnClickListener { DashboardActivity.show(this) }
        val card = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(12), dp(16), dp(16)); setBackgroundResource(R.drawable.card) }
        binding.form.addView(card, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(14) })
        label(card, "Category", true)
        val spinner = Spinner(this).apply {
            adapter = ArrayAdapter(this@GuidanceActivity, android.R.layout.simple_spinner_dropdown_item, listOf("All topics", "General", "New property", "Resale property"))
            contentDescription = "Guidance category"
        }
        card.addView(spinner, LinearLayout.LayoutParams(-1, dp(48)))
        reload = Button(this).apply { text = "Refresh guidance"; isAllCaps = false; setOnClickListener { fetch(true) } }
        card.addView(reload)
        status = label(card, "", false)
        body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }; binding.form.addView(body)
        more = Button(this).apply { text = "Load more guidance"; isAllCaps = false; visibility = View.GONE; setOnClickListener { fetch(false) } }
        binding.form.addView(more, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, itemId: Long) { index = position; fetch(true) }
        }
    }
    private fun label(parent: LinearLayout, value: String, bold: Boolean): TextView = TextView(this).apply {
        text = value; textSize = if (bold) 17f else 15f; setTextColor(getColor(R.color.navy)); setPadding(0, dp(10), 0, dp(6))
        if (bold) setTypeface(null, android.graphics.Typeface.BOLD); parent.addView(this)
    }
    private fun fetch(reset: Boolean) {
        if (!reset && (loading || cursor == null)) return
        val request = ++generation
        val category = GuidanceContract.categories[index]
        val next = if (reset) null else cursor
        if (reset) { items.clear(); cursor = null; render() }
        loading = true; reload.isEnabled = false; more.isEnabled = false
        status.text = if (reset) "Loading published guidance…" else "Loading more guidance…"
        executor.execute {
            val result = runCatching { source.load(category, next) }
            runOnUiThread {
                if (isFinishing || isDestroyed || request != generation) return@runOnUiThread
                loading = false; reload.isEnabled = true; more.isEnabled = true
                result.fold(onSuccess = { page ->
                    val ids = items.map { it.id }.toMutableSet()
                    items.addAll(page.items.filter { ids.add(it.id) }); cursor = page.next
                    status.text = if (items.isEmpty() && cursor == null) "No published guidance yet. Try another category or return later." else "Published guidance may change. Refresh to see the latest version."
                    render()
                }, onFailure = {
                    status.text = if (reset) "Could not load guidance. Check your connection and select Refresh guidance to retry." else "Could not load more guidance. Your current results are still shown; try again."
                    more.visibility = if (cursor != null) View.VISIBLE else View.GONE
                })
            }
        }
    }
    private fun render() {
        body.removeAllViews()
        items.forEach { item ->
            val card = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(10), dp(16), dp(16)); setBackgroundResource(R.drawable.card) }
            body.addView(card, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(14) })
            label(card, when (item.category) { "new_property" -> "NEW PROPERTY"; "resale_property" -> "RESALE PROPERTY"; else -> "GENERAL" }, true)
            label(card, item.title, true); label(card, item.body, false)
        }
        more.visibility = if (cursor != null) View.VISIBLE else View.GONE
    }
    override fun onDestroy() { generation++; executor.shutdownNow(); super.onDestroy() }
    companion object {
        // Test code can inject a fake; the production factory uses only the public project key.
        var sourceFactory: () -> GuidanceSource = { PublicGuidanceClient(BuildConfig.GUIDANCE_URL, BuildConfig.GUIDANCE_PUBLISHABLE_KEY) }
    }
}
