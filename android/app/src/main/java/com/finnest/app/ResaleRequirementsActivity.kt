package com.finnest.app

import android.os.Bundle
import android.content.Intent
import android.text.InputType
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import com.finnest.app.databinding.ActivityDeveloperChecksBinding

class ResaleRequirementsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDeveloperChecksBinding
    private val scope = "resale_property"
    private var loaded = false
    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeveloperChecksBinding.inflate(layoutInflater); setContentView(binding.root)
        FinnestUi.decorate(this, scope, "requirements")
        binding.stepLabel.text = "RESALE · PLANNING NOTES"
        binding.screenTitle.text = "Requirements & location"
        binding.screenDescription.text = "Optional preferences and estimated costs for your resale search."
        binding.screenNotice.text = "Cost reminders only. Applicable costs vary; no tax or fee rate is assumed. Blank amounts remain unknown."
        binding.back.setOnClickListener { leave { finish() } }; binding.backBottom.setOnClickListener { leave { finish() } }
        binding.saveLater.setOnClickListener { leave { DashboardActivity.show(this, "resale_property") } }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) { override fun handleOnBackPressed() { leave { finish() } } })
        binding.next.setOnClickListener { leave { startActivity(Intent(this, ResaleConditionActivity::class.java)) } }
        DraftRepository.load(this, scope) { result -> runOnUiThread {
            if (isFinishing || isDestroyed) return@runOnUiThread
            result.fold(onSuccess = { buildForm(it); loaded = true; binding.loading.visibility = View.GONE; binding.next.isEnabled = true },
                onFailure = { binding.loading.text = "Could not load your resale draft. Return and try again." })
        } }
    }
    private fun leave(action: () -> Unit) {
        if (!loaded) { action(); return }
        DraftRepository.afterPending(scope) { saved -> runOnUiThread {
            if (isFinishing || isDestroyed) return@runOnUiThread
            if (saved) action() else AlertDialog.Builder(this).setTitle("Could not save your resale draft")
                .setMessage("Check device storage and retry before leaving.")
                .setPositiveButton("Retry") { _, _ -> DraftRepository.retryFailed(this, scope) { runOnUiThread { leave(action) } } }
                .setNegativeButton("Keep editing", null).show()
        } }
    }
    private fun label(parent: LinearLayout, value: String, bold: Boolean = false) = TextView(this).apply {
        text = value; textSize = if (bold) 19f else 14f; setTextColor(getColor(R.color.navy)); setPadding(0, dp(12), 0, dp(4))
        if (bold) setTypeface(null, android.graphics.Typeface.BOLD); parent.addView(this)
    }
    private fun card(title: String): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(8), dp(16), dp(16)); setBackgroundResource(R.drawable.card)
        binding.form.addView(this, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(14) }); label(this, title, true)
    }
    private fun buildForm(draft: Map<String, String>) {
        fun input(parent: LinearLayout, key: String, title: String, numeric: Boolean = false) {
            label(parent, title)
            parent.addView(EditText(this).apply {
                hint = if (numeric) "Unknown / not provided" else "Optional"; textSize = 16f; contentDescription = "Resale $title"
                inputType = if (numeric) InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL else InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                setText(draft[key].orEmpty())
                doAfterTextChanged { DraftRepository.save(this@ResaleRequirementsActivity, scope, key, it?.toString().orEmpty()) }
            }, LinearLayout.LayoutParams(-1, -2))
        }
        fun choice(parent: LinearLayout, key: String, title: String) {
            parent.addView(CheckBox(this).apply {
                text = title; textSize = 16f; minHeight = dp(48); setTextColor(getColor(R.color.navy)); isChecked = draft[key] == "true"
                setOnCheckedChangeListener { _, checked -> DraftRepository.save(this@ResaleRequirementsActivity, scope, key, checked.toString()) }
            }, LinearLayout.LayoutParams(-1, -2))
        }
        val budget = card("Budget & comfort")
        label(budget, "Estimates are optional and only for your planning. No current rates or fees are supplied.")
        ResaleFields.budget.forEach { (key, title) -> input(budget, "budget_$key", "$title (₹)", true) }
        val config = card("Configuration & practical needs")
        ResaleFields.configuration.forEach { (key, title) -> choice(config, "config_$key", title) }
        input(config, "carpet_area", "Carpet area")
        input(config, "building_age", "Building age preference")
        input(config, "timeline", "Timeline or family preferences")
        val location = card("Location priorities")
        ResaleFields.location.forEach { (key, title) -> choice(location, "location_$key", title) }
    }
}
