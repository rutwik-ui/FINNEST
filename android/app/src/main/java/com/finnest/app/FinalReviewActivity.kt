package com.finnest.app

import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import com.finnest.app.databinding.ActivityDeveloperChecksBinding
import org.json.JSONArray
import java.text.NumberFormat
import java.util.Locale

class FinalReviewActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDeveloperChecksBinding
    private val journey = "new_property"
    private var loaded = false
    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeveloperChecksBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.stepLabel.text = "STEP 8 OF 8 · NEW PROPERTY"
        binding.screenTitle.text = "Final review & next actions"
        binding.screenDescription.text = "A summary of what you recorded—not an approval or recommendation to proceed."
        binding.screenNotice.text = "Pause on unresolved material questions. Consider qualified legal, financial and technical advice before making commitments."
        binding.next.text = "Save & go to dashboard  →"
        binding.back.setOnClickListener { leave() }
        binding.backBottom.setOnClickListener { leave() }
        binding.saveLater.setOnClickListener { if (loaded) afterSaves { DashboardActivity.show(this) } else DashboardActivity.show(this) }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) { override fun handleOnBackPressed() { leave() } })
        binding.next.setOnClickListener { afterSaves {
            DashboardActivity.show(this)
        } }
        DraftRepository.load(this, journey) { result -> runOnUiThread {
            if (!isFinishing && !isDestroyed) result.fold(onSuccess = {
                buildForm(it); loaded = true; binding.loading.visibility = View.GONE; binding.next.isEnabled = true
            }, onFailure = { binding.loading.text = "Could not load your draft. Please return and try again." })
        } }
    }
    private fun afterSaves(action: () -> Unit) {
        DraftRepository.afterPending(journey) { saved -> runOnUiThread {
            if (isFinishing || isDestroyed) return@runOnUiThread
            if (saved) action() else AlertDialog.Builder(this).setTitle("Could not save your draft")
                .setMessage("Check device storage and retry before leaving.")
                .setPositiveButton("Retry") { _, _ -> DraftRepository.retryFailed(this, journey) { runOnUiThread { afterSaves(action) } } }
                .setNegativeButton("Keep editing", null).show()
        } }
    }
    private fun leave() { if (!loaded) finish() else afterSaves { finish() } }
    private fun label(parent: LinearLayout, value: String, bold: Boolean = false) = TextView(this).apply {
        text = value; textSize = if (bold) 17f else 14f; setTextColor(getColor(R.color.navy))
        setPadding(0, dp(10), 0, dp(4)); if (bold) setTypeface(null, android.graphics.Typeface.BOLD)
        parent.addView(this)
    }
    private fun card(title: String): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(8), dp(16), dp(16)); setBackgroundResource(R.drawable.card)
        binding.form.addView(this, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(14) }); label(this, title, true)
    }
    private fun buildForm(draft: Map<String, String>) {
        val checks = ReviewData.checks(draft)
        val counts = card("Keep the categories distinct")
        label(counts, "Status labels are your entries, not independent verification. An unselected item remains not checked.")
        val recorded = checks.count { it.status != null }
        label(counts, "$recorded of ${checks.size} checklist items have an explicitly saved status.")
        listOf("Checked by you", "Information provided", "Professional review", "Pending / unknown", "Not checked").forEach { status ->
            label(counts, "$status: ${checks.count { it.status == status }} explicitly recorded")
        }
        val other = checks.count { it.status != null && it.status !in listOf("Checked by you", "Information provided", "Professional review", "Pending / unknown", "Not checked") }
        if (other > 0) label(counts, "Other saved statuses: $other")
        label(counts, "No status selected: ${checks.count { it.status == null }}")
        val assumptions = card("Budget & loan assumptions")
        label(assumptions, "Your entries only; unknown values are not treated as confirmed offers or fees.")
        fun show(key: String) = draft[key]?.takeIf { it.isNotBlank() } ?: "Not provided"
        listOf("loan_amount" to "Estimated loan amount (₹)", "down_payment" to "Down payment (₹)",
            "loan_rate" to "Interest rate (%)", "loan_years" to "Tenure (years)", "emi" to "Comfortable EMI (₹ / month)",
            "financial_initial_price" to "Property price entered (₹)").forEach { (key, title) -> label(assumptions, "$title: ${show(key)}") }
        val estimate = LoanEstimate.monthly(draft["loan_amount"].orEmpty(), draft["loan_rate"].orEmpty(), draft["loan_years"].orEmpty())
        val format = NumberFormat.getNumberInstance(Locale("en", "IN")).apply { minimumFractionDigits = 2; maximumFractionDigits = 2 }
        label(assumptions, "Illustrative fixed-rate EMI: ${estimate?.let { "₹${format.format(it)} / month" } ?: "Not available"}")
        val costs = FinancialCosts.total(FinancialCosts.initial.map { draft["financial_initial_${it.first}"].orEmpty() })
        val costText = when (costs) {
            FinancialCosts.Total.Empty -> "Not available"
            FinancialCosts.Total.Invalid -> "Check saved cost inputs"
            is FinancialCosts.Total.Entered -> "₹${format.format(costs.amount)} (${costs.count} of ${FinancialCosts.initial.size} entered; not a complete cost)"
        }
        label(assumptions, "Entered one-time cost estimate: $costText")
        val projects = card("Your saved projects")
        runCatching { JSONArray(draft["shortlist_projects_v1"] ?: "[]") }.fold(onSuccess = { array ->
            if (array.length() == 0) label(projects, "No projects entered yet.")
            for (i in 0 until array.length()) {
                val project = array.optJSONObject(i)
                label(projects, project?.optString("name")?.takeIf { it.isNotBlank() } ?: "A saved project could not be displayed.")
            }
            label(projects, "Only project names are saved; comparison details are not available yet.")
        }, onFailure = { label(projects, "Saved projects could not be read; the data has not been changed.") })
        val notes = card("Notes you actually entered")
        val noted = checks.filter { it.note.isNotBlank() || it.source.isNotBlank() || it.date.isNotBlank() }
        if (noted.isEmpty()) label(notes, "No checklist notes, sources or dates entered.")
        noted.forEach { c ->
            label(notes, "${c.group} · ${c.title}", true)
            if (c.note.isNotBlank()) label(notes, "Note: ${c.note}")
            if (c.source.isNotBlank()) label(notes, "Source: ${c.source}")
            if (c.date.isNotBlank()) label(notes, "Follow-up date: ${c.date}")
        }
        runCatching { JSONArray(draft["feedback_notes"] ?: "[]") }.fold(onSuccess = { array ->
            if (array.length() == 0) label(notes, "No resident feedback notes added.")
            for (i in 0 until array.length()) {
                val note = array.optJSONObject(i)
                if (note == null) { label(notes, "A saved feedback note could not be displayed."); continue }
                label(notes, "Resident feedback · ${note.optString("topic")}", true)
                listOf("date" to "Date", "context" to "Context", "shared" to "Shared", "concern" to "Concern / question", "followup" to "Follow-up").forEach { (key, title) ->
                    val value = note.optString(key); if (value.isNotBlank()) label(notes, "$title: $value")
                }
            }
        }, onFailure = { label(notes, "Resident feedback notes could not be read; the data has not been changed.") })
        if (listOf("topic", "date", "context", "shared", "concern", "followup").any { !draft["feedback_pending_$it"].isNullOrBlank() })
            label(notes, "An unfinished resident feedback entry is saved on Step 4B but has not been added as a note.")
        val pending = card("Pending and unknown")
        val pendingChecks = checks.filter(ReviewData::pending)
        label(pending, "${pendingChecks.size} items are not checked or pending, including items with no selected status. Review them in their journey steps.")
        pendingChecks.forEach { label(pending, "${it.group} · ${it.title}: ${it.status ?: "Not checked (no status selected)"}") }
        val concern = card("Potential concerns")
        label(concern, "The current provisional checklist does not offer a Concern status. Read your notes and resident feedback above; FINNEST does not assess them automatically.")
        val risk = card("Examples worth pausing over")
        label(risk, "Pressure to pay, important unwritten promises, refusal to share relevant documents, unclear title or unclear payment arrangements are discussion prompts, not findings about a property. Seek qualified advice when material questions remain unresolved.")
        val professional = card("Professional review")
        val professionalChecks = checks.filter { it.status == "Professional review" }
        label(professional, "${professionalChecks.size} items marked Professional review by you. FINNEST does not resolve these questions.")
        professionalChecks.forEach { label(professional, "${it.group} · ${it.title}") }
        label(professional, "No expert profiles or referral service are configured yet.")
        val actions = card("Your next actions")
        label(actions, "Questions to resolve or people to consult")
        actions.addView(EditText(this).apply {
            hint = "Enter next actions"; textSize = 16f; contentDescription = "Your next actions"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 3; setText(draft["review_next_actions"].orEmpty())
            doAfterTextChanged { DraftRepository.save(this@FinalReviewActivity, journey, "review_next_actions", it?.toString().orEmpty()) }
        }, LinearLayout.LayoutParams(-1, -2))
        actions.addView(Button(this).apply {
            text = "Save summary"; isAllCaps = false
            setOnClickListener { afterSaves { AlertDialog.Builder(this@FinalReviewActivity).setMessage("Your next actions are saved on this device.").setPositiveButton("OK", null).show() } }
        })
    }
}
