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
        binding.stepLabel.text = "A SEPARATE HOME-BUYING JOURNEY"
        binding.screenTitle.text = "Resale Property Journey"
        binding.screenDescription.text = "Explore key areas when considering a resale home, at your own pace."
        binding.screenNotice.text = "These are the subject areas defined in the prototype, not a complete resale journey. Requirements and condition notes are available; other areas are coming later."
        binding.loading.visibility = View.GONE
        binding.back.setOnClickListener { changeJourney() }
        binding.backBottom.text = "Change journey"; binding.backBottom.setOnClickListener { changeJourney() }
        binding.saveLater.text = "Change journey"; binding.saveLater.setOnClickListener { changeJourney() }
        binding.next.text = "Open requirements  →"; binding.next.isEnabled = true
        binding.next.setOnClickListener { openRequirements() }
        val card = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(12), dp(16), dp(16)); setBackgroundResource(R.drawable.card) }
        binding.form.addView(card, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(14) })
        card.addView(TextView(this).apply { text = "Confirmed subject areas"; textSize = 20f; setTextColor(getColor(R.color.navy)); setTypeface(null, android.graphics.Typeface.BOLD) })
        card.addView(Button(this).apply { text = "Requirements & location  →"; isAllCaps = false; setOnClickListener { openRequirements() } }, LinearLayout.LayoutParams(-1, -2))
        card.addView(Button(this).apply { text = "Property condition  →"; isAllCaps = false; setOnClickListener { startActivity(Intent(this@ResaleOverviewActivity, ResaleConditionActivity::class.java)) } }, LinearLayout.LayoutParams(-1, -2))
        listOf("Seller, ownership & title", "Society documents & dues", "Legal review", "Agreement, registration & handover").forEach { title ->
            card.addView(TextView(this).apply {
                text = "$title · coming later"; textSize = 15f; setTextColor(getColor(R.color.muted)); setPadding(0, dp(12), 0, dp(12))
            })
        }
        binding.form.addView(TextView(this).apply {
            text = "FINNEST does not inspect a property, verify documents or provide legal advice."; textSize = 14f; setTextColor(getColor(R.color.muted)); setPadding(0, dp(14), 0, 0)
        })
    }
    private fun openRequirements() { startActivity(Intent(this, ResaleRequirementsActivity::class.java)) }
    private fun changeJourney() { startActivity(Intent(this, MainActivity::class.java).putExtra("change_journey", true).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)); finish() }
    companion object {
        fun show(context: Context) { context.startActivity(Intent(context, ResaleOverviewActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)) }
    }
}
