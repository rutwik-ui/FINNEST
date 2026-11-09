package com.finnest.app

import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import com.finnest.app.databinding.ActivityDeveloperChecksBinding
import java.text.NumberFormat
import java.util.Locale

class LoanGuideActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDeveloperChecksBinding
    private val journey = "new_property"
    private var loaded = false
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeveloperChecksBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.stepLabel.text = "STEP 5 OF 8 · NEW PROPERTY"
        binding.screenTitle.text = "Home loan guide"
        binding.screenDescription.text = "Explore an illustrative monthly payment and prepare questions for lenders."
        binding.screenNotice.text = "This estimate is not a lender offer, eligibility check or promise of approval. Actual terms, fees and rates vary."
        binding.back.setOnClickListener { leave() }
        binding.backBottom.setOnClickListener { leave() }
        binding.saveLater.setOnClickListener { leave() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { leave() }
        })
        binding.next.setOnClickListener { afterSaves {
            AlertDialog.Builder(this).setTitle("Draft saved on this device")
                .setMessage("Legal due diligence is not available yet. You can return to this guide later.")
                .setPositiveButton("OK") { _, _ -> finish() }.show()
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
    private fun label(parent: LinearLayout, text: String, bold: Boolean = false): TextView = TextView(this).apply {
        this.text = text; textSize = if (bold) 17f else 14f; setTextColor(getColor(R.color.navy))
        setPadding(0, dp(12), 0, dp(4)); if (bold) setTypeface(null, android.graphics.Typeface.BOLD)
        parent.addView(this)
    }
    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(8), dp(16), dp(16)); setBackgroundResource(R.drawable.card)
        binding.form.addView(this, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(14) })
    }
    private fun buildForm(draft: Map<String, String>) {
        val values = mutableMapOf<String, EditText>()
        var refresh: () -> Unit = {}
        fun input(parent: LinearLayout, key: String, title: String, hintText: String, numeric: Boolean = false, integer: Boolean = false) {
            label(parent, title)
            val edit = EditText(this).apply {
                hint = hintText; textSize = 16f; contentDescription = title
                inputType = if (numeric) InputType.TYPE_CLASS_NUMBER or (if (integer) 0 else InputType.TYPE_NUMBER_FLAG_DECIMAL)
                    else InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                setText(draft["loan_$key"].orEmpty())
            }
            parent.addView(edit, LinearLayout.LayoutParams(-1, -2)); values[key] = edit
            edit.doAfterTextChanged { DraftRepository.save(this, journey, "loan_$key", it?.toString().orEmpty()); refresh() }
        }
        val assumptions = card()
        input(assumptions, "amount", "Loan amount (₹)", "Enter an estimate", true)
        input(assumptions, "rate", "Annual interest rate (%)", "Enter if known", true)
        input(assumptions, "years", "Tenure (years)", "Whole years", true, true)
        input(assumptions, "lender", "Lender name (optional)", "Not provided")
        val context = card()
        label(context, "Optional planning context", true)
        label(context, "These are your inputs only; FINNEST does not determine affordability or eligibility.")
        input(context, "income", "Monthly household income estimate", "Unknown", true)
        input(context, "obligations", "Monthly obligations estimate", "Unknown", true)
        val resultCard = card()
        label(resultCard, "ILLUSTRATIVE MONTHLY EMI", true)
        val result = label(resultCard, "", true)
        label(resultCard, "Fixed-rate formula from your assumptions; excludes fees, insurance, rate changes and lender-specific terms. Enter an amount up to ₹1 trillion, rate 0–100%, and whole years 1–50.")
        refresh = {
            val amount = values.getValue("amount").text.toString()
            val rate = values.getValue("rate").text.toString()
            val years = values.getValue("years").text.toString()
            val estimate = LoanEstimate.monthly(amount, rate, years)
            result.text = when {
                amount.isBlank() -> "Add a loan amount"
                rate.isBlank() || years.isBlank() -> "Add the rate and tenure"
                estimate == null -> "Check the amount, rate and tenure"
                else -> "₹${NumberFormat.getNumberInstance(Locale("en", "IN")).apply { minimumFractionDigits = 2; maximumFractionDigits = 2 }.format(estimate)} / month"
            }
        }
        refresh()
        val questions = card()
        label(questions, "Questions for a lender", true)
        listOf("What is the effective rate and how can it change?", "Which processing, legal or other fees apply?", "What are the prepayment and late-payment terms?", "What happens if disbursement timing changes?").forEach { label(questions, it) }
        val documents = card()
        label(documents, "Documents to ask the lender about", true)
        label(documents, "Requirements vary by lender and location. Track the conversation without uploading personal documents here. Status choices are provisional.")
        val statuses = listOf("Not checked", "Checked by you", "Information provided", "Pending / unknown", "Professional review")
        listOf("identity" to "Identity and address requirements", "income_docs" to "Income documentation requirements", "bank" to "Bank statement period requested", "property" to "Property documents required", "fees" to "Processing and legal fee schedule").forEach { (id, title) ->
            label(documents, title, true)
            val key = "loan_${id}_status"
            val saved = draft[key] ?: statuses.first()
            // Keep unrecognized stored values visible rather than replacing them with a default.
            val choices = if (saved in statuses) statuses else listOf(saved) + statuses
            val spinner = Spinner(this).apply {
                adapter = ArrayAdapter(this@LoanGuideActivity, android.R.layout.simple_spinner_dropdown_item, choices)
                setSelection(choices.indexOf(saved)); contentDescription = "$title status"
            }
            documents.addView(spinner, LinearLayout.LayoutParams(-1, dp(48)))
            var last = saved
            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, itemId: Long) {
                    val selected = choices[position]; if (selected != last) { last = selected; DraftRepository.save(this@LoanGuideActivity, journey, key, selected) }
                }
            }
            input(documents, "${id}_note", "Note / source (optional)", "Optional note / source")
        }
    }
}
