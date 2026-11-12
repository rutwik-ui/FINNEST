package com.finnest.app

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.finnest.app.databinding.ActivityDeveloperChecksBinding

class PrivacyInfoActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDeveloperChecksBinding
    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeveloperChecksBinding.inflate(layoutInflater); setContentView(binding.root)
        FinnestUi.decorate(this, if (getSharedPreferences("entry", MODE_PRIVATE).getString("journey", "New Property") == "Resale Property") "resale_property" else "new_property", roadmap = true)
        binding.stepLabel.text = "YOUR DATA"; binding.screenTitle.text = "Privacy & data use"
        binding.screenDescription.text = "How this Android version handles your information."
        binding.screenNotice.text = "Avoid recording sensitive details on a shared device."
        binding.loading.visibility = View.GONE; binding.next.isEnabled = true
        binding.back.setOnClickListener { finish() }; binding.backBottom.setOnClickListener { finish() }
        binding.next.text = "Back to profile"; binding.next.setOnClickListener { finish() }
        binding.saveLater.text = "Back"; binding.saveLater.setOnClickListener { finish() }
        section("Stored on this device", "Profile fields, journey drafts and reminders are stored locally in the app's private storage. The app does not add its own encryption. Device saving is currently always on; no session-only mode is available. Android backup is disabled, and uninstalling removes these entries.")
        section("Network requests", "The guidance library fetches public guidance from Supabase when opened. It does not upload your journey drafts. Android sign-in, backup and synchronization are not implemented.")
        section("Optional services", "This version does not provide expert contact sharing, marketing messages or analytics integrations. No consent switches are presented for services that do not exist. Notification alerts are not scheduled.")
        section("Deleting local data", "Use Delete local journey data in Profile & settings to delete both local journey drafts, profile fields and reminders. The chosen journey and onboarding preference remain; website data and Supabase records are not affected.")
    }
    private fun section(title: String, body: String) {
        val card = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(12), dp(16), dp(16)); setBackgroundResource(R.drawable.card) }
        binding.form.addView(card, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(14) })
        card.addView(TextView(this).apply { text = title; textSize = 17f; setTextColor(getColor(R.color.navy)); setTypeface(null, android.graphics.Typeface.BOLD) })
        card.addView(TextView(this).apply { text = body; textSize = 14f; setTextColor(getColor(R.color.navy)); setPadding(0, dp(10), 0, 0) })
    }
}
