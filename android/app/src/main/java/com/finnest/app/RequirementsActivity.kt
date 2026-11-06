package com.finnest.app

import android.os.Bundle
import android.content.Intent
import android.text.InputType
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import com.finnest.app.databinding.ActivityRequirementsBinding

/** First New Property step. Values are optional and stored only on this device. */
class RequirementsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRequirementsBinding
    private val journey = "new_property"
    private var loaded = false
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRequirementsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.back.setOnClickListener { leaveWhenSaved() }
        binding.backBottom.setOnClickListener { leaveWhenSaved() }
        binding.saveLater.setOnClickListener { leaveWhenSaved() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { leaveWhenSaved() }
        })
        binding.next.setOnClickListener {
            afterSaves { startActivity(Intent(this, DeveloperChecksActivity::class.java)) }
        }
        DraftRepository.load(this, journey) { draft ->
            runOnUiThread {
                if (!isFinishing && !isDestroyed) {
                    draft.fold(onSuccess = {
                        buildForm(it)
                        loaded = true
                        binding.loading.visibility = android.view.View.GONE
                        binding.next.isEnabled = true
                    }, onFailure = {
                        binding.loading.text = "Could not load your draft. Please return and try again."
                    })
                }
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
    private fun leaveWhenSaved() {
        if (!loaded) finish() else afterSaves { finish() }
    }

    private fun section(title: String, subtitle: String? = null): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            setBackgroundResource(R.drawable.card)
        }
        val params = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(14) }
        binding.form.addView(card, params)
        card.addView(TextView(this).apply { text = title; textSize = 20f; setTextColor(getColor(R.color.navy)); setTypeface(null, android.graphics.Typeface.BOLD) })
        if (subtitle != null) card.addView(TextView(this).apply { text = subtitle; textSize = 14f; setTextColor(getColor(R.color.muted)); setPadding(0, dp(4), 0, dp(4)) })
        return card
    }

    private fun label(parent: LinearLayout, text: String) {
        parent.addView(TextView(this).apply { this.text = text; textSize = 14f; setTextColor(getColor(R.color.navy)); setPadding(0, dp(14), 0, dp(4)) })
    }
    private fun input(parent: LinearLayout, draft: Map<String, String>, key: String, title: String, hintText: String, numeric: Boolean = false) {
        label(parent, title)
        val edit = EditText(this).apply {
            hint = hintText; textSize = 16f; setSingleLine(true)
            inputType = if (numeric) InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL else InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            setText(draft[key].orEmpty())
            contentDescription = title
        }
        parent.addView(edit, LinearLayout.LayoutParams(-1, -2))
        edit.doAfterTextChanged { save(key, it?.toString().orEmpty()) }
    }
    private fun choice(parent: LinearLayout, draft: Map<String, String>, key: String, title: String) {
        val check = CheckBox(this).apply {
            text = title; textSize = 16f; setTextColor(getColor(R.color.navy)); isChecked = draft[key] == "true"
            minHeight = dp(48)
        }
        parent.addView(check, LinearLayout.LayoutParams(-1, -2))
        check.setOnCheckedChangeListener { _, checked -> save(key, checked.toString()) }
    }
    private fun buildForm(draft: Map<String, String>) {
        val budget = section("Budget", "Optional estimates")
        input(budget, draft, "loan", "Approximate loan amount, if known", "₹ —", true)
        input(budget, draft, "down_payment", "Down payment", "₹ —", true)
        label(budget, "ONE-TIME / INITIAL COSTS")
        listOf("stamp_duty" to "Stamp duty / registration", "gst" to "GST (if applicable)", "brokerage" to "Brokerage", "interiors" to "Interiors", "contingency" to "Contingency").forEach { (key, title) -> input(budget, draft, key, title, "₹ —", true) }
        label(budget, "RECURRING COSTS")
        input(budget, draft, "emi", "Comfortable monthly EMI", "₹ / month", true)
        input(budget, draft, "maintenance", "Maintenance", "₹ / month", true)

        val location = section("Location", "Planning priorities")
        listOf("workplace" to "Workplace", "schools" to "Schools", "healthcare" to "Healthcare", "transport" to "Transport", "shopping" to "Shopping", "family" to "Family", "safety" to "Safety", "environment" to "Environment", "traffic" to "Traffic", "infrastructure" to "Future infrastructure", "development" to "Surrounding development").forEach { (key, title) -> choice(location, draft, "location_$key", title) }

        val configuration = section("Configuration", "Optional preferences")
        listOf("1bhk" to "1 BHK", "2bhk" to "2 BHK", "3bhk" to "3 BHK", "villa" to "Villa / duplex", "parking" to "Parking", "amenities" to "Amenities", "accessibility" to "Accessibility", "vastu" to "Vastu (optional)").forEach { (key, title) -> choice(configuration, draft, "config_$key", title) }
        input(configuration, draft, "carpet_area", "Approx. carpet area", "Add if known")
        input(configuration, draft, "accessibility_needs", "Accessibility needs", "Optional")
        val priorities = section("Needs & Priorities", "Place any selected preference into a tier when useful.")
        label(priorities, "Priority tiers will be available in a later phase. Your selections above are saved.")
    }
}
