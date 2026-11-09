package com.finnest.app

import android.app.DatePickerDialog
import android.os.Bundle
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
import java.util.Calendar
import java.util.Locale

class ResaleConditionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDeveloperChecksBinding
    private val scope = "resale_property"
    private var loaded = false
    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeveloperChecksBinding.inflate(layoutInflater); setContentView(binding.root)
        binding.stepLabel.text = "RESALE PROPERTY · YOUR REVIEW"
        binding.screenTitle.text = "Condition inspection"
        binding.screenDescription.text = "Record what you observe; arrange a qualified technical inspection for concerns. This checklist cannot assess structural safety."
        binding.screenNotice.text = "Keep your observations, information shared by a seller, evidence reviewed and professional advice distinct. Status choices are provisional."
        binding.back.setOnClickListener { leave { finish() } }; binding.backBottom.setOnClickListener { leave { finish() } }
        binding.saveLater.setOnClickListener { leave { ResaleOverviewActivity.show(this) } }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) { override fun handleOnBackPressed() { leave { finish() } } })
        binding.next.setOnClickListener { leave {
            AlertDialog.Builder(this).setTitle("Saved on this device")
                .setMessage("Seller, ownership and title is not available yet. You can return to your resale overview.")
                .setPositiveButton("Overview") { _, _ -> ResaleOverviewActivity.show(this) }.show()
        } }
        DraftRepository.load(this, scope) { result -> runOnUiThread {
            if (isFinishing || isDestroyed) return@runOnUiThread
            result.fold(onSuccess = { buildForm(it); loaded = true; binding.loading.visibility = View.GONE; binding.next.isEnabled = true },
                onFailure = { binding.loading.text = "Could not load your resale draft. Return and try again." })
        } }
    }
    private fun save(key: String, value: String) = DraftRepository.save(this, scope, key, value)
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
    private fun label(parent: LinearLayout, text: String, bold: Boolean = false) {
        parent.addView(TextView(this).apply {
            this.text = text; textSize = if (bold) 17f else 14f; setTextColor(getColor(R.color.navy)); setPadding(0, dp(12), 0, dp(4))
            if (bold) setTypeface(null, android.graphics.Typeface.BOLD)
        })
    }
    private fun buildForm(draft: Map<String, String>) {
        val statuses = listOf("Not checked", "Checked by you", "Information provided", "Pending / unknown", "Professional review")
        ResaleCondition.checks.forEach { (id, title) ->
            val card = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(8), dp(16), dp(16)); setBackgroundResource(R.drawable.card) }
            binding.form.addView(card, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(14) })
            label(card, title, true)
            val prefix = "condition_$id"
            val key = "${prefix}_status"
            val saved = draft[key] ?: "Not checked"
            val choices = if (saved in statuses) statuses else listOf(saved) + statuses
            val spinner = Spinner(this).apply {
                adapter = ArrayAdapter(this@ResaleConditionActivity, android.R.layout.simple_spinner_dropdown_item, choices)
                setSelection(choices.indexOf(saved)); contentDescription = "$title status"
            }
            card.addView(spinner, LinearLayout.LayoutParams(-1, dp(48)))
            var last = saved
            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, itemId: Long) {
                    val selected = choices[position]; if (selected != last) { last = selected; save(key, selected) }
                }
            }
            fun input(suffix: String, caption: String, hintText: String) {
                label(card, caption)
                val fieldKey = "${prefix}_$suffix"
                card.addView(EditText(this).apply {
                    hint = hintText; textSize = 16f; contentDescription = "$title $caption"
                    inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                    setText(draft[fieldKey].orEmpty()); doAfterTextChanged { save(fieldKey, it?.toString().orEmpty()) }
                }, LinearLayout.LayoutParams(-1, -2))
            }
            input("note", "Note / observation", "Optional")
            input("source", "Source / context", "Who provided this?")
            label(card, "Date / follow-up date, if any")
            val dateKey = "${prefix}_date"
            var date = draft[dateKey].orEmpty()
            val button = Button(this).apply { isAllCaps = false }
            fun update() { button.text = date.ifEmpty { "Choose date (optional)" }; button.contentDescription = "$title follow-up date: ${button.text}" }
            update()
            button.setOnClickListener {
                val cal = Calendar.getInstance()
                val parts = date.split("/").mapNotNull { it.toIntOrNull() }
                if (parts.size == 3) cal.set(parts[2], parts[1] - 1, parts[0])
                DatePickerDialog(this, { _, year, month, day ->
                    date = String.format(Locale.ROOT, "%02d/%02d/%04d", day, month + 1, year); update(); save(dateKey, date)
                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
            }
            card.addView(button, LinearLayout.LayoutParams(-1, -2))
            card.addView(Button(this).apply { text = "Clear date"; isAllCaps = false; contentDescription = "Clear $title date"; setOnClickListener { date = ""; update(); save(dateKey, "") } })
        }
    }
}
