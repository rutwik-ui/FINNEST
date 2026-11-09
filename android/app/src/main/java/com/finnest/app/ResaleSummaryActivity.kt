package com.finnest.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.finnest.app.databinding.ActivityDeveloperChecksBinding

/** Summarizes recorded data only; no status is interpreted as verified or completed. */
class ResaleSummaryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDeveloperChecksBinding
    private var generation = 0
    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeveloperChecksBinding.inflate(layoutInflater); setContentView(binding.root)
        binding.stepLabel.text = "YOUR RESALE JOURNEY"
        binding.screenTitle.text = "Resale journey summary"
        binding.screenDescription.text = "A view of your locally saved notes and planning preferences."
        binding.screenNotice.text = "Your entries are not verified findings or a recommendation to proceed. Seek qualified advice for unresolved questions."
        binding.back.setOnClickListener { finish() }; binding.backBottom.setOnClickListener { finish() }
        binding.saveLater.text = "Overview"; binding.saveLater.setOnClickListener { ResaleOverviewActivity.show(this) }
        binding.next.text = "Back to resale overview"; binding.next.setOnClickListener { ResaleOverviewActivity.show(this) }
    }
    override fun onResume() {
        super.onResume()
        val request = ++generation
        binding.form.removeAllViews(); binding.loading.visibility = View.VISIBLE; binding.loading.text = "Loading your local resale notes…"; binding.next.isEnabled = false
        DraftRepository.load(this, "resale_property") { result -> runOnUiThread {
            if (isFinishing || isDestroyed || request != generation) return@runOnUiThread
            result.fold(onSuccess = { buildForm(it); binding.loading.visibility = View.GONE; binding.next.isEnabled = true },
                onFailure = { binding.loading.text = "Could not load resale notes. Return and try again." })
        } }
    }
    private fun label(parent: LinearLayout, value: String, bold: Boolean = false) {
        parent.addView(TextView(this).apply { text = value; textSize = if (bold) 17f else 14f; setTextColor(getColor(R.color.navy)); setPadding(0, dp(10), 0, dp(4)); if (bold) setTypeface(null, android.graphics.Typeface.BOLD) })
    }
    private fun card(title: String): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(8), dp(16), dp(16)); setBackgroundResource(R.drawable.card)
        binding.form.addView(this, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(14) }); label(this, title, true)
    }
    private fun buildForm(draft: Map<String, String>) {
        val planning = card("Requirements & location")
        val lines = mutableListOf<String>()
        ResaleFields.budget.forEach { (id, title) -> draft["budget_$id"]?.takeIf { it.isNotBlank() }?.let { lines.add("$title: ₹$it") } }
        ResaleFields.configuration.forEach { (id, title) -> if (draft["config_$id"] == "true") lines.add(title) }
        ResaleFields.location.forEach { (id, title) -> if (draft["location_$id"] == "true") lines.add("Location: $title") }
        listOf("carpet_area" to "Carpet area", "building_age" to "Building age preference", "timeline" to "Timeline or family preferences").forEach { (id, title) -> draft[id]?.takeIf { it.isNotBlank() }?.let { lines.add("$title: $it") } }
        if (lines.isEmpty()) label(planning, "No planning entries yet. Blank estimates are unknown.") else lines.forEach { label(planning, it) }
        planning.addView(Button(this).apply { text = "Open requirements & location"; isAllCaps = false; setOnClickListener { startActivity(Intent(this@ResaleSummaryActivity, ResaleRequirementsActivity::class.java)) } })
        ResaleChecklists.sections.forEach { section ->
            val block = card(section.title)
            var recorded = 0
            section.checks.forEach { (id, title) ->
                val prefix = "${section.id}_$id"
                val fields = listOf("status" to "Status", "amount" to "Amount (₹)", "note" to "Note", "source" to "Source", "date" to "Follow-up date")
                    .mapNotNull { (key, caption) -> draft["${prefix}_$key"]?.takeIf { it.isNotBlank() }?.let { "$caption: $it" } }
                if (fields.isNotEmpty()) { recorded++; label(block, title, true); fields.forEach { label(block, it) } }
            }
            if (recorded == 0) label(block, "No entries recorded. Unselected statuses remain ${section.defaultStatus.lowercase()}.")
            if (section.id == "legal") draft["legal_lawyer_status"]?.takeIf { it.isNotBlank() }?.let { label(block, "Lawyer review status: $it") }
            block.addView(Button(this).apply { text = "Open ${section.title}"; isAllCaps = false; setOnClickListener { startActivity(Intent(this@ResaleSummaryActivity, ResaleConditionActivity::class.java).putExtra("resale_section", section.id)) } })
        }
        val help = card("Questions worth pausing over")
        label(help, "An unwritten promise is an illustrative concern, not a finding about any property.")
        help.addView(Button(this).apply { text = "Understand an illustrative concern"; isAllCaps = false; setOnClickListener { startActivity(Intent(this@ResaleSummaryActivity, CommonActivity::class.java).putExtra("screen", "risk").putExtra("scope", "resale_property")) } })
        val footer = card("About this summary")
        label(footer, "These six areas do not define the complete resale journey. Entries remain on this device and have not been independently verified.")
    }
}
