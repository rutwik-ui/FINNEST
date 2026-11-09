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
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.Locale

/** Buyer-entered notes only; none of these statuses represent independent verification. */
class DeveloperChecksActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDeveloperChecksBinding
    private val journey = "new_property"
    private var loaded = false
    private val isLegal by lazy { intent.getBooleanExtra("legal_due_diligence", false) }
    private val isFeedback by lazy { intent.getBooleanExtra("resident_feedback", false) }
    private val isVisit by lazy { intent.getBooleanExtra("visit_observations", false) }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeveloperChecksBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (isLegal) {
            binding.stepLabel.text = "STEP 6 OF 8 · NEW PROPERTY"
            binding.screenTitle.text = "Legal due diligence"
            binding.screenDescription.text = "Questions for an appropriately qualified local lawyer. Document relevance and requirements vary by location."
            binding.screenNotice.text = "FINNEST does not determine title, legal clearance or whether a property is safe to buy. Status choices are provisional."
        } else if (isFeedback) {
            binding.stepLabel.text = "STEP 4B · NEW PROPERTY"
            binding.screenTitle.text = "Resident feedback notes"
            binding.screenDescription.text = "Record what residents share in your own words. Your notes stay on this device. Avoid sensitive personal information."
            binding.screenNotice.text = "Individual accounts are not independently verified by FINNEST. Seek evidence and professional advice where appropriate."
        } else if (isVisit) {
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
                if (isLegal) {
                    startActivity(Intent(this, FinancialDueDiligenceActivity::class.java))
                } else if (isFeedback) {
                    startActivity(Intent(this, LoanGuideActivity::class.java))
                } else if (isVisit) {
                    startActivity(Intent(this, DeveloperChecksActivity::class.java).putExtra("resident_feedback", true))
                } else startActivity(Intent(this, ProjectsActivity::class.java))
            }
        }
        DraftRepository.load(this, journey) { result ->
            runOnUiThread {
                if (!isFinishing && !isDestroyed) result.fold(onSuccess = {
                    if (isFeedback) buildFeedback(it) else buildForm(it)
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
    private fun buildFeedback(draft: Map<String, String>) {
        val fields = listOf("topic" to "Topic (free text)", "date" to "Date (optional)",
            "context" to "Who / setting (optional)", "shared" to "What was shared?",
            "concern" to "Concern or question (optional)", "followup" to "Follow-up (optional)")
        val edits = mutableMapOf<String, EditText>()
        fields.forEach { (key, title) ->
            label(binding.form, title)
            val edit = EditText(this).apply {
                hint = if (key == "date") "e.g. 15/11/2026" else "Enter $title"
                textSize = 16f
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                setText(draft["feedback_pending_$key"].orEmpty())
                contentDescription = title
            }
            binding.form.addView(edit, LinearLayout.LayoutParams(-1, -2))
            edit.doAfterTextChanged { save("feedback_pending_$key", it?.toString().orEmpty()) }
            edits[key] = edit
        }
        // Preserve the original serialized data if parsing fails; never silently overwrite it.
        val raw = draft["feedback_notes"] ?: "[]"
        val notes = try { JSONArray(raw) } catch (_: Exception) { null }
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val add = Button(this).apply { text = "Add feedback note"; isAllCaps = false }
        binding.form.addView(add)
        binding.form.addView(list)
        fun renderNotes() {
            list.removeAllViews()
            label(list, "Notes entered: ${notes?.length() ?: 0}", true)
            if (notes == null) { label(list, "Could not read saved notes. They have not been changed; please do not add new notes."); return }
            for (i in 0 until notes.length()) {
                val note = notes.optJSONObject(i)
                if (note == null) { label(list, "A saved note could not be displayed."); continue }
                val card = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(8), dp(16), dp(16)); setBackgroundResource(R.drawable.card) }
                list.addView(card, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(14) })
                fields.forEach { (key, title) ->
                    val value = note.optString(key)
                    if (value.isNotBlank()) label(card, "$title: $value", key == "topic")
                }
                card.addView(Button(this).apply {
                    text = "Delete note"; isAllCaps = false
                    setOnClickListener {
                        AlertDialog.Builder(this@DeveloperChecksActivity).setTitle("Delete this note?")
                            .setMessage("This removes the note from this device.")
                            .setNegativeButton("Cancel", null).setPositiveButton("Delete") { _, _ ->
                                notes.remove(i); save("feedback_notes", notes.toString()); renderNotes()
                            }.show()
                    }
                })
            }
        }
        add.isEnabled = notes != null
        add.setOnClickListener {
            if (edits.getValue("topic").text.toString().isBlank() || edits.getValue("shared").text.toString().isBlank()) {
                AlertDialog.Builder(this).setMessage("Enter a topic and what was shared before adding a note. Other fields are optional.").setPositiveButton("OK", null).show()
            } else if (notes != null) {
                val note = JSONObject()
                fields.forEach { (key, _) -> note.put(key, edits.getValue(key).text.toString().trim()) }
                notes.put(note)
                save("feedback_notes", notes.toString())
                fields.forEach { (key, _) -> edits.getValue(key).setText("") }
                renderNotes()
            }
        }
        renderNotes()
    }
    private fun buildForm(draft: Map<String, String>) {
        val checks = if (isLegal) LegalDueDiligence.checks else if (isVisit) VisitObservations.checks else ReviewData.developerChecks
        // Only "Not checked" is visible in the dropdown screenshot. Other labels come from the
        // introductory status legend and remain provisional until the expanded dropdown is supplied.
        val statuses = listOf("Not checked", "Checked by you", "Information provided", "Pending / unknown", "Professional review")
        checks.forEach { (id, title) ->
            val card = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(8), dp(16), dp(16)); setBackgroundResource(R.drawable.card) }
            binding.form.addView(card, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(14) })
            label(card, title, true)
            val prefix = if (isLegal) "legal" else if (isVisit) "visit" else "developer"
            val key = "${prefix}_${id}_status"
            val saved = draft[key] ?: "Not checked"
            val choices = if (saved in statuses) statuses else listOf(saved) + statuses
            val spinner = Spinner(this).apply {
                adapter = ArrayAdapter(this@DeveloperChecksActivity, android.R.layout.simple_spinner_dropdown_item, choices)
                setSelection(choices.indexOf(saved))
                contentDescription = "$title status"
            }
            card.addView(spinner, LinearLayout.LayoutParams(-1, dp(48)))
            var lastSelected = saved
            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, itemId: Long) {
                    // Do not write the displayed default merely because the screen was opened.
                    val selected = choices[position]
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
        if (isLegal) {
            val guidance = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(8), dp(16), dp(16)); setBackgroundResource(R.drawable.card) }
            binding.form.addView(guidance, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(14) })
            label(guidance, "Want legal guidance?", true)
            label(guidance, "Consider when independent advice from an appropriately qualified local property lawyer may help. No expert profiles are configured; FINNEST cannot connect you to a lawyer yet.")
        }
    }
}
