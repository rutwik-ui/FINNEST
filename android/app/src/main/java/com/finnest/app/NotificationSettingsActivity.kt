package com.finnest.app

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.finnest.app.databinding.ActivityDeveloperChecksBinding
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial

/** Choose which notifications FINNEST sends on this phone. */
class NotificationSettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDeveloperChecksBinding
    private lateinit var phoneStatus: TextView
    private lateinit var phoneAction: MaterialButton
    private val switches = mutableListOf<SwitchMaterial>()
    private var askedOnce = false
    private fun dp(n: Int) = FinnestUi.dp(this, n)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeveloperChecksBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val scope = if (getSharedPreferences("entry", MODE_PRIVATE).getString("journey", "New Property") == "Resale Property") "resale_property" else "new_property"
        FinnestUi.decorate(this, scope, roadmap = false)
        binding.stepLabel.text = "SETTINGS"
        binding.screenTitle.text = "Notifications"
        binding.screenDescription.text = if (AccountClient.userId(this) != null) "Choose what FINNEST tells you about. These choices are shared with the website." else "Choose what FINNEST tells you about on this phone."
        binding.screenNotice.visibility = View.GONE
        binding.loading.visibility = View.GONE
        binding.saveLater.text = "Home"; binding.next.text = "Done"; binding.backBottom.text = "Back"
        binding.back.setOnClickListener { finish() }
        binding.backBottom.setOnClickListener { finish() }
        binding.saveLater.setOnClickListener { DashboardActivity.show(this) }
        binding.next.setOnClickListener { finish() }
        binding.next.isEnabled = true

        val phone = section("Phone notifications")
        phoneStatus = FinnestUi.body(this, "", 14f)
        phone.addView(phoneStatus, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(4) })
        phoneAction = MaterialButton(this).apply { isAllCaps = false; contentDescription = "Allow notifications"; setOnClickListener { allow() } }
        phone.addView(phoneAction, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })
        row(phone, "Email", "Account emails, such as confirming your email address and resetting your password, are always sent.", null)

        val types = section("What you're notified about")
        FinnestNotifications.kinds.forEach { kind -> row(types, kind.title, kind.description, kind.id) }
        render()
    }

    override fun onResume() { super.onResume(); render() }

    private fun section(title: String): LinearLayout = FinnestUi.card(this).also { card ->
        card.orientation = LinearLayout.VERTICAL
        card.addView(FinnestUi.h2(this, title, 18f), LinearLayout.LayoutParams(-1, -2))
        binding.form.addView(card, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(14) })
    }

    private fun row(parent: LinearLayout, title: String, description: String, kind: String?) {
        val line = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(14), 0, dp(4)) }
        val text = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        text.addView(FinnestUi.label(this, title, 15f, 600, FinnestUi.NAVY), LinearLayout.LayoutParams(-1, -2))
        text.addView(FinnestUi.body(this, description, 13.5f), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(3) })
        line.addView(text, LinearLayout.LayoutParams(0, -2, 1f))
        if (kind == null) {
            line.addView(FinnestUi.label(this, "Always on", 12f, 600, FinnestUi.MUTED).apply {
                background = FinnestUi.shape(this@NotificationSettingsActivity, FinnestUi.CREAM, null, 999); setPadding(dp(10), dp(5), dp(10), dp(5))
            }, LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(12) })
        } else {
            val toggle = SwitchMaterial(this).apply {
                isChecked = FinnestNotifications.enabled(this@NotificationSettingsActivity, kind)
                contentDescription = title
                setOnCheckedChangeListener { _, on -> FinnestNotifications.setEnabled(this@NotificationSettingsActivity, kind, on) }
            }
            switches.add(toggle)
            line.addView(toggle, LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(12) })
        }
        parent.addView(line, LinearLayout.LayoutParams(-1, -2))
    }

    private fun render() {
        if (!::phoneStatus.isInitialized) return
        val on = FinnestNotifications.canPost(this)
        phoneStatus.text = if (on) "On. Reminders and updates appear in your phone's notification shade, even when FINNEST is closed."
            else "Off. Allow notifications so reminders and updates can reach you even when FINNEST is closed."
        phoneAction.text = if (on) "Manage in phone settings" else "Allow notifications"
        phoneAction.contentDescription = phoneAction.text
    }

    private fun allow() {
        when {
            FinnestNotifications.canPost(this) || (askedOnce && !FinnestNotifications.permissionGranted(this)) -> openSystemSettings()
            Build.VERSION.SDK_INT >= 33 && !FinnestNotifications.permissionGranted(this) -> { askedOnce = true; requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST) }
            else -> openSystemSettings()
        }
    }

    private fun openSystemSettings() {
        val intent = if (Build.VERSION.SDK_INT >= 26) Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
        runCatching { startActivity(intent) }
    }

    @Deprecated("Uses the platform permission callback")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        render()
    }

    companion object { const val REQUEST = 4107 }
}
