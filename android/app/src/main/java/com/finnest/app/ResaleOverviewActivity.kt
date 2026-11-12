package com.finnest.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.finnest.app.databinding.ActivityDeveloperChecksBinding

/** Overview of the six subject areas shown in the prototype, not a complete journey. */
class ResaleOverviewActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDeveloperChecksBinding
    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeveloperChecksBinding.inflate(layoutInflater); setContentView(binding.root)
        FinnestUi.decorate(this, "resale_property")
        binding.stepLabel.text = "A SEPARATE HOME-BUYING JOURNEY"
        binding.screenTitle.text = "Resale Property Journey"
        binding.screenDescription.text = "Explore key areas when considering a resale home, at your own pace."
        binding.screenNotice.text = "These are the subject areas defined in the prototype. They are saved as local notes on this device and are not a complete resale journey."
        binding.loading.visibility = View.GONE
        binding.back.setOnClickListener { changeJourney() }
        binding.backBottom.text = "Change journey"; binding.backBottom.setOnClickListener { changeJourney() }
        binding.saveLater.text = "Home"; binding.saveLater.setOnClickListener { DashboardActivity.show(this, "resale_property") }
        binding.next.text = "Open requirements  →"; binding.next.isEnabled = true
        binding.next.setOnClickListener { openRequirements() }
        val card = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(12), dp(16), dp(16)); setBackgroundResource(R.drawable.card) }
        binding.form.addView(card, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(14) })
        card.addView(TextView(this).apply { text = "Confirmed subject areas"; textSize = 20f; setTextColor(getColor(R.color.navy)); setTypeface(null, android.graphics.Typeface.BOLD) })
        card.addView(Button(this).apply { text = "Requirements & location  →"; isAllCaps = false; setOnClickListener { openRequirements() } }, LinearLayout.LayoutParams(-1, -2))
        ResaleChecklists.sections.forEach { section ->
            val label = when (section.id) { "condition" -> "Property condition"; "handover" -> "Agreement, registration & handover"; else -> section.title }
            card.addView(Button(this).apply { text = "$label  →"; isAllCaps = false; setOnClickListener { startActivity(Intent(this@ResaleOverviewActivity, ResaleConditionActivity::class.java).putExtra("resale_section", section.id)) } }, LinearLayout.LayoutParams(-1, -2))
        }
        card.addView(Button(this).apply { text = "View resale summary  →"; isAllCaps = false; setOnClickListener { startActivity(Intent(this@ResaleOverviewActivity, ResaleSummaryActivity::class.java)) } }, LinearLayout.LayoutParams(-1, -2))
        val help = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(8), dp(16), dp(16)); setBackgroundResource(R.drawable.card) }
        binding.form.addView(help, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(14) })
        listOf("Experts directory" to "experts", "Understand a concern" to "risk", "About FINNEST" to "about").forEach { (title, screen) ->
            help.addView(Button(this).apply { text = title; isAllCaps = false; setOnClickListener { startActivity(Intent(this@ResaleOverviewActivity, CommonActivity::class.java).putExtra("screen", screen).putExtra("scope", "resale_property")) } }, LinearLayout.LayoutParams(-1, -2))
        }
        help.addView(Button(this).apply { text = "Profile & settings"; isAllCaps = false; setOnClickListener { startActivity(Intent(this@ResaleOverviewActivity, ProfileSettingsActivity::class.java)) } }, LinearLayout.LayoutParams(-1, -2))
        binding.form.addView(TextView(this).apply {
            text = "FINNEST does not inspect a property, verify documents or provide legal advice."; textSize = 14f; setTextColor(getColor(R.color.muted)); setPadding(0, dp(14), 0, 0)
        })
    }
    private fun openRequirements() { startActivity(Intent(this, ResaleRequirementsActivity::class.java)) }
    override fun onResume() { super.onResume(); refreshRoadmaps(binding.root) }
    private fun refreshRoadmaps(view: View) {
        if (view is JourneyRoadmapView) view.refresh()
        if (view is android.view.ViewGroup) (0 until view.childCount).forEach { refreshRoadmaps(view.getChildAt(it)) }
    }
    private fun changeJourney() { startActivity(Intent(this, MainActivity::class.java).putExtra("change_journey", true).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)); finish() }
    companion object {
        fun show(context: Context) { context.startActivity(Intent(context, ResaleOverviewActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)) }
    }
}
