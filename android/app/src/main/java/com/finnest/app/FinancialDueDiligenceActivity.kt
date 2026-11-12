package com.finnest.app

import android.app.DatePickerDialog
import android.os.Bundle
import android.content.Intent
import android.text.InputType
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
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
import java.util.Calendar
import java.util.Locale

class FinancialDueDiligenceActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDeveloperChecksBinding
    private val journey = "new_property"
    private var loaded = false
    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeveloperChecksBinding.inflate(layoutInflater)
        setContentView(binding.root)
        FinnestUi.decorate(this, journey, "financial")
        binding.stepLabel.text = "STEP 7 OF 8 · NEW PROPERTY"
        binding.screenTitle.text = "Financial due diligence"
        binding.screenDescription.text = "Record costs and loan terms you want to clarify. Rates and fees are not supplied here."
        binding.screenNotice.text = "Rates and fees are not supplied here. This section is only for your notes and questions."
        binding.back.setOnClickListener { leave() }
        binding.backBottom.setOnClickListener { leave() }
        binding.saveLater.setOnClickListener { if (loaded) afterSaves { DashboardActivity.show(this) } else DashboardActivity.show(this) }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { leave() }
        })
        binding.next.setOnClickListener { afterSaves {
            startActivity(Intent(this, FinalReviewActivity::class.java))
        } }
        DraftRepository.load(this, journey) { result -> runOnUiThread {
            if (!isFinishing && !isDestroyed) result.fold(onSuccess = {
                buildForm(it); loaded = true; binding.loading.visibility = View.GONE; binding.next.isEnabled = true
            }, onFailure = { binding.loading.text = "Could not load your draft. Please return and try again." })
        } }
    }
    private fun save(key: String, value: String) = DraftRepository.save(this, journey, "financial_$key", value)
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
    private fun label(parent: LinearLayout, value: String, bold: Boolean = false): TextView = TextView(this).apply {
        text = value; textSize = if (bold) 17f else 14f; setTextColor(getColor(R.color.navy))
        setPadding(0, dp(12), 0, dp(4)); if (bold) setTypeface(null, android.graphics.Typeface.BOLD)
        parent.addView(this)
    }
    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(8), dp(16), dp(16)); setBackgroundResource(R.drawable.card)
        binding.form.addView(this, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(14) })
    }
    private fun buildForm(draft: Map<String, String>) {
        var refresh: () -> Unit = {}
        fun input(parent: LinearLayout, key: String, title: String, hintText: String, numeric: Boolean = false): EditText {
            label(parent, title)
            return EditText(this).apply {
                hint = hintText; textSize = 16f; contentDescription = title
                inputType = if (numeric) InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL else InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                setText(draft["financial_$key"].orEmpty())
                parent.addView(this, LinearLayout.LayoutParams(-1, -2))
                doAfterTextChanged { save(key, it?.toString().orEmpty()); refresh() }
            }
        }
        val initialCard = card()
        label(initialCard, "One-time / initial costs", true)
        label(initialCard, "Enter estimates only when you have a source. Costs vary by location and circumstances; no tax rate is assumed.")
        val initial = FinancialCosts.initial.map { (id, title) -> input(initialCard, "initial_$id", "$title (₹)", "Unknown / not provided", true) }
        val initialTotal = label(initialCard, "", true)
        label(initialCard, "Only the sum of entered amounts, not a complete transaction cost. Blank amounts remain unknown.")
        val recurringCard = card()
        label(recurringCard, "Recurring costs", true)
        label(recurringCard, "Kept separate from initial costs.")
        val recurring = FinancialCosts.recurring.map { (id, title) -> input(recurringCard, "recurring_$id", "$title (₹)", "Unknown", true) }
        val recurringTotal = label(recurringCard, "", true)
        label(recurringCard, "Only entered amounts are summed; blank amounts remain unknown.")
        fun showTotal(title: String, fields: List<EditText>): String = when (val total = FinancialCosts.total(fields.map { it.text.toString() })) {
            FinancialCosts.Total.Empty -> "$title: Not available"
            FinancialCosts.Total.Invalid -> "$title: Check amounts (0–₹1 trillion, up to two decimal places)"
            is FinancialCosts.Total.Entered -> {
                val amount = NumberFormat.getNumberInstance(Locale("en", "IN")).apply { minimumFractionDigits = 2; maximumFractionDigits = 2 }.format(total.amount)
                "$title: ₹$amount (${total.count} of ${fields.size} entered)"
            }
        }
        refresh = { initialTotal.text = showTotal("Entered one-time estimate total", initial); recurringTotal.text = showTotal("Entered recurring total / month", recurring) }
        refresh()
        val promptCard = card()
        label(promptCard, "Cost and document prompts", true)
        label(promptCard, "Status choices are provisional; these are your notes, not verified findings.")
        val statuses = listOf("Not checked", "Checked by you", "Information provided", "Pending / unknown", "Professional review")
        FinancialCosts.initial.drop(1).forEach { (id, title) ->
            label(promptCard, title, true)
            val key = "prompt_${id}_status"
            val saved = draft["financial_$key"] ?: statuses.first()
            val choices = if (saved in statuses) statuses else listOf(saved) + statuses
            val spinner = Spinner(this).apply {
                adapter = ArrayAdapter(this@FinancialDueDiligenceActivity, android.R.layout.simple_spinner_dropdown_item, choices)
                setSelection(choices.indexOf(saved)); contentDescription = "$title status"
            }
            promptCard.addView(spinner, LinearLayout.LayoutParams(-1, dp(48)))
            var last = saved
            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, itemId: Long) {
                    val selected = choices[position]; if (selected != last) { last = selected; save(key, selected) }
                }
            }
            input(promptCard, "prompt_${id}_note", "Note / observation", "Optional")
            input(promptCard, "prompt_${id}_source", "Source / context", "Who provided this?")
            label(promptCard, "Date / follow-up date, if any")
            val dateKey = "prompt_${id}_date"
            var date = draft["financial_$dateKey"].orEmpty()
            val button = Button(this).apply { isAllCaps = false }
            fun update() { button.text = date.ifEmpty { "Choose date (optional)" }; button.contentDescription = "$title follow-up date: ${button.text}" }
            update()
            button.setOnClickListener {
                val cal = Calendar.getInstance()
                val parts = date.split("/").mapNotNull { it.toIntOrNull() }
                if (parts.size == 3) cal.set(parts[2], parts[1] - 1, parts[0])
                DatePickerDialog(this, { _, year, month, day -> date = String.format(Locale.ROOT, "%02d/%02d/%04d", day, month + 1, year); update(); save(dateKey, date) }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
            }
            promptCard.addView(button)
            promptCard.addView(Button(this).apply { text = "Clear date"; isAllCaps = false; contentDescription = "Clear $title date"; setOnClickListener { date = ""; update(); save(dateKey, "") } })
        }
        val comparison = card()
        label(comparison, "Loan comparison", true)
        label(comparison, "No lender offers are provided. Compare only based on current written terms from lenders. This form records one set of terms.")
        listOf("lender" to "Lender", "rate_type" to "Rate & type", "fees" to "Fees", "tenure" to "Tenure", "emi" to "EMI", "interest" to "Estimated total interest", "prepayment" to "Prepayment terms").forEach { (id, title) -> input(comparison, "comparison_$id", title, "Unknown / not provided") }
    }
}
