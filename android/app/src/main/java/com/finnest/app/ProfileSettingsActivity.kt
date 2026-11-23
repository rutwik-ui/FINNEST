package com.finnest.app

import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
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
import androidx.lifecycle.lifecycleScope
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.launch

/**
 * Profile, account status and an explicit, scoped local reset.
 *
 * Local deletion removes the device copy only. Synced answers live in the
 * buyer's account and are removed from the website's Profile page, which is
 * stated on screen so nobody assumes "delete" reached the cloud.
 */
class ProfileSettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDeveloperChecksBinding
    private val profileScope = "local_profile"
    private var loaded = false
    private var deleting = false
    private var resetButton: Button? = null
    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeveloperChecksBinding.inflate(layoutInflater); setContentView(binding.root)
        FinnestUi.decorate(this, if (getSharedPreferences("entry", MODE_PRIVATE).getString("journey", "New Property") == "Resale Property") "resale_property" else "new_property", roadmap = true)
        binding.stepLabel.text = "YOUR PROFILE"; binding.screenTitle.text = "Profile & settings"
        binding.screenDescription.text = "Review your device-only profile and data settings."
        binding.screenNotice.text = "Sign in to keep this journey on your account and open it on the website or another device."
        binding.saveLater.text = "Home"; binding.next.text = "Back to dashboard"
        binding.back.setOnClickListener { leave { finish() } }; binding.backBottom.setOnClickListener { leave { finish() } }
        binding.saveLater.setOnClickListener { leave { home() } }; binding.next.setOnClickListener { leave { home() } }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) { override fun handleOnBackPressed() { leave { finish() } } })
        DraftRepository.load(this, profileScope) { result -> runOnUiThread {
            if (isFinishing || isDestroyed) return@runOnUiThread
            result.fold(onSuccess = { buildForm(it); loaded = true; binding.loading.visibility = View.GONE; binding.next.isEnabled = true },
                onFailure = { binding.loading.text = "Could not load local profile. Return and try again." })
        } }
    }
    private fun home() {
        if (getSharedPreferences("entry", MODE_PRIVATE).getString("journey", "New Property") == "Resale Property") DashboardActivity.show(this, "resale_property") else DashboardActivity.show(this)
    }
    private fun leave(action: () -> Unit) {
        if (deleting) return
        if (!loaded) { action(); return }
        DraftRepository.afterPending(profileScope) { ok -> runOnUiThread {
            if (isFinishing || isDestroyed) return@runOnUiThread
            if (ok) action() else AlertDialog.Builder(this).setTitle("Could not save your profile")
                .setMessage("Check device storage and retry before leaving.")
                .setPositiveButton("Retry") { _, _ -> DraftRepository.retryFailed(this, profileScope) { runOnUiThread { leave(action) } } }
                .setNegativeButton("Keep editing", null).show()
        } }
    }
    private fun label(parent: LinearLayout, value: String, bold: Boolean = false) {
        parent.addView(TextView(this).apply { text = value; textSize = if (bold) 17f else 14f; setTextColor(getColor(R.color.navy)); setPadding(0, dp(10), 0, dp(6)); if (bold) setTypeface(null, android.graphics.Typeface.BOLD) })
    }
    private fun card(title: String): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(8), dp(16), dp(16)); setBackgroundResource(R.drawable.card)
        binding.form.addView(this, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(14) }); label(this, title, true)
    }
    private fun button(parent: LinearLayout, title: String, click: () -> Unit): Button = Button(this).apply {
        text = title; isAllCaps = false; setOnClickListener { leave(click) }; parent.addView(this, LinearLayout.LayoutParams(-1, -2))
    }
    private fun buildForm(draft: Map<String, String>) {
        val account=card("Your account")
        val user=AccountClient.get(this).auth.currentUserOrNull()
        if(user==null) button(account,"Log In / Sign Up") { startActivity(Intent(this,AccountActivity::class.java)) }
        else {
            label(account,"Signed in as ${user.email.orEmpty()}")
            button(account,"Sync now") { AccountSync.enqueue(this) }
            button(account,"Sign out") { lifecycleScope.launch { runCatching { AccountClient.get(this@ProfileSettingsActivity).auth.signOut() }.onSuccess { AccountSync.stop();startActivity(Intent(this@ProfileSettingsActivity,AccountActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)) }.onFailure { AlertDialog.Builder(this@ProfileSettingsActivity).setMessage("Could not sign out. Please retry online.").setPositiveButton("OK",null).show() } } }
        }
        val profile = card("Your profile")
        button(profile,"Replay introduction") { startActivity(Intent(this,MainActivity::class.java).putExtra("replay_onboarding",true)) }
        val journey = getSharedPreferences("entry", MODE_PRIVATE).getString("journey", null) ?: "Not selected"
        label(profile, "Selected journey: $journey")
        listOf("name" to "Name", "city" to "City / area").forEach { (key, title) ->
            label(profile, "$title (optional)")
            profile.addView(EditText(this).apply {
                hint = "Not provided"; contentDescription = "Profile $title"; textSize = 16f
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                filters = arrayOf(InputFilter.LengthFilter(100)); setText(draft[key].orEmpty())
                doAfterTextChanged { DraftRepository.save(this@ProfileSettingsActivity, profileScope, key, it?.toString().orEmpty()) }
            }, LinearLayout.LayoutParams(-1, -2))
        }
        val account = card("Your account")
        val session = AccountStore.load(this)
        if (session != null) {
            label(account, "Signed in as ${session.email ?: "your account"}")
            label(account, "Journey answers, checklist statuses, shortlist, visit notes, reminders and this profile are synced to your account.")
            button(account, "Sync now") { syncNow() }
            button(account, "Manage account / sign out") { startActivity(Intent(this, AccountActivity::class.java)) }
        } else {
            label(account, "You are using FINNEST as a guest. Everything is saved on this device only, so it will not appear on the website or on another phone.")
            button(account, "Sign in or create an account") { startActivity(Intent(this, AccountActivity::class.java)) }
        }

        val storage = card("Save on this device")
        label(storage, "Saving to this device is always on, so the app works without internet. Uninstalling deletes the local copy; anything synced to your account can still be opened on the website.")
        val notifications = card("Notifications")
        label(notifications, "Reminders are in-app dated notes only. This version does not schedule operating-system notifications.")
        button(notifications, "Review reminders") { startActivity(Intent(this, RemindersActivity::class.java)) }
        val files = card("Documents")
        label(files, "Keep copies of documents with your journey. They're private to your account and also appear on the website.")
        button(files, "My documents") { startActivity(Intent(this, DocumentsActivity::class.java)) }
        val help = card("Help & feedback")
        label(help, "Ask a question, report a problem or suggest an improvement. Replies appear in the app.")
        button(help, "Contact support") { startActivity(Intent(this, SupportActivity::class.java)) }
        val privacy = card("Privacy & information")
        button(privacy, "Privacy & data use") { startActivity(Intent(this, PrivacyInfoActivity::class.java)) }
        button(privacy, "About & disclaimer") { startActivity(Intent(this, CommonActivity::class.java).putExtra("screen", "about")) }
        val reset = card("Delete local data")
        label(reset, "Deletes both New Property and Resale drafts, profile fields and reminders from this device. The selected journey and onboarding preference remain. It does not delete anything already synced to your account — use the website's Profile page for that. This cannot be undone.")
        resetButton = Button(this).apply { text = "Delete local journey data"; isAllCaps = false; setOnClickListener { confirmDelete() } }
        reset.addView(resetButton, LinearLayout.LayoutParams(-1, -2))
    }
    private fun syncNow() {
        DraftSync.reconcile(this, profileScope) { }
        DraftSync.reconcile(this, "new_property") { a ->
            DraftSync.reconcile(this, "resale_property") { b ->
                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    val ok = a.isSuccess && b.isSuccess
                    AlertDialog.Builder(this)
                        .setTitle(if (ok) "Sync complete" else "Could not sync")
                        .setMessage(if (ok) "Your journey and profile are saved to your account."
                        else "Your answers are still safe on this device. Check your internet connection and try again.")
                        .setPositiveButton("OK", null).show()
                }
            }
        }
    }

    private fun confirmDelete() {
        if (deleting) return
        val input = EditText(this).apply { hint = "Type DELETE"; contentDescription = "Delete confirmation"; inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS }
        val dialog = AlertDialog.Builder(this).setTitle("Delete local data?")
            .setMessage("Permanently delete both journeys, profile fields and reminders on this device? Type DELETE to confirm. Selected journey and onboarding preferences remain.")
            .setView(input).setNegativeButton("Cancel", null).setPositiveButton("Delete", null).create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (input.text.toString() != "DELETE") { input.error = "Type DELETE exactly to confirm"; return@setOnClickListener }
                dialog.dismiss(); deleteData()
            }
        }
        dialog.show()
    }
    private fun deleteData() {
        deleting = true; resetButton?.isEnabled = false; binding.next.isEnabled = false
        DraftRepository.deleteScopes(this, LocalData.resetScopes) { result -> runOnUiThread {
            if (isFinishing || isDestroyed) return@runOnUiThread
            deleting = false; resetButton?.isEnabled = true; binding.next.isEnabled = true
            result.fold(onSuccess = {
                // Destroy old form activities so stale in-memory drafts cannot be written back after deletion.
                val target = if (getSharedPreferences("entry", MODE_PRIVATE).getString("journey", "New Property") == "Resale Property") ResaleOverviewActivity::class.java else DashboardActivity::class.java
                startActivity(Intent(this, target).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            }, onFailure = { AlertDialog.Builder(this).setTitle("Could not delete local data")
                .setMessage("No success was reported. Check device storage and try again.").setPositiveButton("OK", null).show() })
        } }
    }
}
