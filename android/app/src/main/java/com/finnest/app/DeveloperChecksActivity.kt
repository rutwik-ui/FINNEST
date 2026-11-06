package com.finnest.app

import android.app.DatePickerDialog
import android.os.Bundle
import android.content.Intent
import android.text.InputType
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.AdapterView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import com.finnest.app.databinding.ActivityDeveloperChecksBinding
import java.util.Calendar
import java.util.Locale

/** Buyer-entered notes only; none of these statuses represent independent verification. */
class DeveloperChecksActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDeveloperChecksBinding
    private val journey = "new_property"
    private var loaded = false
    private val isVisit by lazy { intent.getBooleanExtra("visit_observations", false) }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeveloperChecksBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (isVisit) {
            binding.stepLabel.text = "STEP 4A · NEW PROPERTY"
            binding.screenTitle.text = "What to notice on a visit"
            binding.screenDescription.text = "Record your own observations during a visit. A visual visit is not a technical inspection."
            binding.screenNotice.text = "A visit is not an inspection. Do not enter restricted areas or rely on a visual check for safety."
        }
        binding.back.setOnClickListener { leaveWhenSaved() }
        binding.backBottom.setOnClickListener { leaveWhenSaved() }
        binding.saveLater.setOnClickListener { leaveWhenSaved() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { leaveWhenSaved() }
        })
        binding.next.setOnClickListener {
            afterSaves {
                if (isVisit) {
                    AlertDialog.Builder(this).setTitle("Draft saved on this device")
                        .setMessage("Resident feedback notes are not available yet. You can return to your visit observations later.")
                        .setPositiveButton("OK") { _, _ -> finish() }.show()
                } else startActivity(Intent(this, ProjectsActivity::class.java))
            }
        }
        DraftRepository.load(this, journey) { result ->
            runOnUiThread {
                if (!isFinishing && !isDestroyed) result.fold(onSuccess = {
                    buildForm(it)
                    loaded = true
                    binding.loading.visibility = View.GONE
                    binding.next.isEnabled = true
                }, onFailure = { binding.loading.text = "Could not load your draft. Please return and try again." })
            }
        }
    }
    private fun save(key: String, value: String) = DraftRepository.save(this, journey, key, value)
    private fun afterSaves(action: () -> Unit) {
        DraftRepository.afterPending(journey) { saved ->
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                if (saved) action() else AlertDialog.Builder(this).setTitle("Could not save your draft")
                    .setMessage("Please check your device storage. Retry saving before leaving this screen.")
                    .setPositiveButton("Retry") { _, _ ->
                        DraftRepository.retryFailed(this, journey) { runOnUiThread { afterSaves(action) } }
                    }.setNegativeButton("Keep editing", null).show()
            }
        }
    }
    private fun leaveWhenSaved() { if (!loaded) finish() else afterSaves { finish() } }
    private fun label(parent: LinearLayout, text: String, bold: Boolean = false) {
        parent.addView(TextView(this).apply {
            this.text = text; textSize = if (bold) 17f else 14f; setTextColor(getColor(R.color.navy))
            setPadding(0, dp(12), 0, dp(4))
            if (bold) setTypeface(null, android.graphics.Typeface.BOLD)
        })
    }
    private fun input(parent: LinearLayout, draft: Map<String, String>, key: String, title: String, hintText: String) {
        label(parent, title)
        val edit = EditText(this).apply {
            hint = hintText; textSize = 16f; inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            setText(draft[key].orEmpty()); contentDescription = title
        }
        parent.addView(edit, LinearLayout.LayoutParams(-1, -2))
        edit.doAfterTextChanged { save(key, it?.toString().orEmpty()) }
    }
    private fun date(parent: LinearLayout, draft: Map<String, String>, key: String, title: String) {
        label(parent, "Date / follow-up date, if any")
        var value = draft[key].orEmpty()
        val button = Button(this).apply { isAllCaps = false }
        fun update() { button.text = if (value.isEmpty()) "Choose date (optional)" else value; button.contentDescription = "$title, follow-up date: ${button.text}" }
        update()
        button.setOnClickListener {
            val calendar = Calendar.getInstance()
            val parts = value.split("/").mapNotNull { it.toIntOrNull() }
            if (parts.size == 3) calendar.set(parts[2], parts[1] - 1, parts[0])
            DatePickerDialog(this, { _, year, month, day ->
                value = String.format(Locale.ROOT, "%02d/%02d/%04d", day, month + 1, year)
                update(); save(key, value)
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }
        parent.addView(button, LinearLayout.LayoutParams(-1, -2))
        val clear = Button(this).apply { text = "Clear date"; isAllCaps = false; setOnClickListener { value = ""; update(); save(key, "") } }
        parent.addView(clear, LinearLayout.LayoutParams(-1, -2))
    }
    private fun buildForm(draft: Map<String, String>) {
        val checks = if (isVisit) listOf(
            "carpet_area" to "Carpet area", "light" to "Light", "ventilation" to "Ventilation",
            "noise" to "Noise", "water" to "Water", "power_backup" to "Power backup",
            "parking" to "Parking", "lifts" to "Lifts", "fire_safety" to "Fire-safety provisions",
            "elderly_access" to "Elderly access", "security" to "Security",
            "construction" to "Visible construction", "open_space" to "Open space", "garden" to "Garden / play area"
        ) else listOf(
            "track_record" to "Track record / delivery",
            "rera" to "Project RERA registration / disclosures",
            "land_title" to "Land ownership / title",
            "approvals" to "Approvals / sanctioned plans",
            "legal_history" to "Credible legal history",
            "financial_stability" to "Financial stability where evidence exists"
        )
        // Only "Not checked" is visible in the dropdown screenshot. Other labels come from the
        // introductory status legend and remain provisional until the expanded dropdown is supplied.
        val statuses = listOf("Not checked", "Checked by you", "Information provided", "Pending / unknown", "Professional review")
        checks.forEach { (id, title) ->
            val card = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(8), dp(16), dp(16)); setBackgroundResource(R.drawable.card) }
            binding.form.addView(card, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(14) })
            label(card, title, true)
            val prefix = if (isVisit) "visit" else "developer"
            val key = "${prefix}_${id}_status"
            val spinner = Spinner(this).apply {
                adapter = ArrayAdapter(this@DeveloperChecksActivity, android.R.layout.simple_spinner_dropdown_item, statuses)
                setSelection(statuses.indexOf(draft[key] ?: "Not checked").coerceAtLeast(0))
                contentDescription = "$title status"
            }
            card.addView(spinner, LinearLayout.LayoutParams(-1, dp(48)))
            var lastSelected = statuses[statuses.indexOf(draft[key] ?: "Not checked").coerceAtLeast(0)]
            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, itemId: Long) {
                    // Do not write the displayed default merely because the screen was opened.
                    val selected = statuses[position]
                    if (selected != lastSelected) {
                        lastSelected = selected
                        save(key, selected)
                    }
                }
            }
            input(card, draft, "${prefix}_${id}_note", "Note / observation", "Optional")
            input(card, draft, "${prefix}_${id}_source", "Source / context", "Who provided this?")
            date(card, draft, "${prefix}_${id}_date", title)
        }
    }
}
