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
import org.json.JSONArray

class JourneySummaryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDeveloperChecksBinding
    private var filterIndex = 0
    private var loadGeneration = 0
    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeveloperChecksBinding.inflate(layoutInflater)
        setContentView(binding.root)
        FinnestUi.decorate(this, "new_property", "review")
        filterIndex = savedInstanceState?.getInt("filter", 0)?.coerceIn(JourneySummary.filters.indices) ?: 0
        binding.stepLabel.text = "YOUR PROGRESS"
        binding.screenTitle.text = "My Journey"
        binding.screenDescription.text = "A clear view of your steps, notes and questions. Your guest draft is saved on this device."
        binding.screenNotice.text = "Checklist statuses are your entries; nothing is independently verified by FINNEST."
        binding.back.setOnClickListener { finish() }
        binding.backBottom.setOnClickListener { DashboardActivity.show(this) }
        binding.backBottom.text = "Dashboard"
        binding.saveLater.text = "Home"
        binding.saveLater.setOnClickListener { DashboardActivity.show(this) }
        binding.next.text = "Back to dashboard"
        binding.next.setOnClickListener { DashboardActivity.show(this) }
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("filter", filterIndex)
        super.onSaveInstanceState(outState)
    }
    override fun onResume() {
        super.onResume()
        val generation = ++loadGeneration
        binding.loading.visibility = View.VISIBLE
        binding.loading.text = "Loading your local journey…"
        binding.form.removeAllViews()
        binding.next.isEnabled = false
        DraftRepository.load(this, "new_property") { result -> runOnUiThread {
            if (isFinishing || isDestroyed || generation != loadGeneration) return@runOnUiThread
            result.fold(onSuccess = { draft ->
                buildForm(draft); binding.loading.visibility = View.GONE; binding.next.isEnabled = true
            }, onFailure = { binding.loading.text = "Could not load your journey. Return to the dashboard and try again." })
        } }
    }
    private fun label(parent: LinearLayout, text: String, bold: Boolean = false): TextView = TextView(this).apply {
        this.text = text; textSize = if (bold) 17f else 14f; setTextColor(getColor(R.color.navy))
        setPadding(0, dp(10), 0, dp(4)); if (bold) setTypeface(null, android.graphics.Typeface.BOLD)
        parent.addView(this)
    }
    private fun card(title: String): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(8), dp(16), dp(16)); setBackgroundResource(R.drawable.card)
        binding.form.addView(this, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(14) }); label(this, title, true)
    }
    private fun buildForm(draft: Map<String, String>) {
        val checks = ReviewData.checks(draft)
        val progress = card("Journey type · New Property")
        label(progress, "${checks.count { it.status == "Checked by you" }} marked Checked by you · ${checks.count { it.status != null }} with a saved status")
        label(progress, "These counts reflect statuses you selected, not completed steps or verified evidence. Unselected statuses are not recorded.")
        val filter = card("Filter checklist")
        val spinner = Spinner(this).apply {
            adapter = ArrayAdapter(this@JourneySummaryActivity, android.R.layout.simple_spinner_dropdown_item, JourneySummary.filters)
            setSelection(filterIndex); contentDescription = "Filter checklist"
        }
        filter.addView(spinner, LinearLayout.LayoutParams(-1, dp(48)))
        label(filter, "Filtered from your entries; pending includes items with no selected status.")
        val results = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        filter.addView(results)
        fun render(index: Int) {
            results.removeAllViews()
            val items = JourneySummary.filter(checks, index)
            if (items.isEmpty()) label(results, "No items match this filter yet.")
            items.forEach { c -> label(results, "${c.group} · ${c.title}\n${c.status ?: "Not checked (no status selected)"}") }
        }
        render(filterIndex)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, itemId: Long) { filterIndex = position; render(position) }
        }
        val screens = card("Journey screens")
        label(screens, "Open a section to review or edit your local draft.")
        DashboardSteps.all.forEach { step ->
            screens.addView(Button(this).apply {
                text = "${step.number}  ${step.title}  →"; isAllCaps = false; contentDescription = "Open ${step.title}"
                setOnClickListener { JourneyNavigation.intent(this@JourneySummaryActivity, step.id)?.let { startActivity(it) } }
            }, LinearLayout.LayoutParams(-1, -2))
        }
        val projects = card("Projects")
        runCatching { JSONArray(draft["shortlist_projects_v1"] ?: "[]") }.fold(onSuccess = { array ->
            if (array.length() == 0) label(projects, "No projects entered yet.")
            for (i in 0 until array.length()) label(projects, array.optJSONObject(i)?.optString("name")?.takeIf { it.isNotBlank() } ?: "A saved project could not be displayed.")
        }, onFailure = { label(projects, "Saved projects could not be read; the data has not been changed.") })
    }
}
