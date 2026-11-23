package com.finnest.app

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.finnest.app.databinding.ActivityDeveloperChecksBinding

/**
 * Sign in, create an account, or sign out.
 *
 * Signing in is what makes the same journey appear on the website. The screen is
 * explicit that guest data stays on the device until an account exists, so a
 * buyer is never told their notes are backed up when they are not.
 */
class AccountActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDeveloperChecksBinding
    private val auth = SupabaseAuth()
    private var busy = false
    private var signedIn = false
    private var mode = "signin"
    private var statusView: TextView? = null
    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    private val scope by lazy {
        if (getSharedPreferences("entry", MODE_PRIVATE).getString("journey", "New Property") == "Resale Property") "resale_property" else "new_property"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeveloperChecksBinding.inflate(layoutInflater); setContentView(binding.root)
        FinnestUi.decorate(this, scope, roadmap = true)
        binding.loading.visibility = View.GONE
        binding.stepLabel.text = "YOUR ACCOUNT"
        binding.screenTitle.text = "Sync across devices"
        binding.screenDescription.text = "Sign in so your journey is available on the website and any phone or tablet you use."
        binding.screenNotice.text = "Your answers are private to your account. FINNEST never asks for your bank passwords, card numbers or identity documents."
        binding.saveLater.text = "Home"; binding.next.text = "Back to home"
        binding.back.setOnClickListener { finish() }; binding.backBottom.text = "Back"; binding.backBottom.setOnClickListener { finish() }
        binding.next.setOnClickListener { home() }
        binding.saveLater.setOnClickListener { home() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) { override fun handleOnBackPressed() { finish() } })
        render()
    }

    private fun home() {
        if (scope == "resale_property") DashboardActivity.show(this, "resale_property") else DashboardActivity.show(this)
    }

    private fun label(parent: LinearLayout, value: String, bold: Boolean = false) {
        parent.addView(TextView(this).apply {
            text = value; textSize = if (bold) 17f else 14f; setTextColor(getColor(R.color.navy))
            setPadding(0, dp(10), 0, dp(6)); if (bold) setTypeface(null, android.graphics.Typeface.BOLD)
        })
    }

    private fun card(title: String): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(8), dp(16), dp(16)); setBackgroundResource(R.drawable.card)
        binding.form.addView(this, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(14) }); label(this, title, true)
    }

    private fun field(parent: LinearLayout, hint: String, password: Boolean = false): EditText = EditText(this).apply {
        this.hint = hint; contentDescription = hint; textSize = 16f; setSingleLine(true)
        inputType = if (password) InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        else InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        parent.addView(this, LinearLayout.LayoutParams(-1, -2))
    }

    private fun render() {
        binding.form.removeAllViews()
        val session = AccountStore.load(this)
        signedIn = session != null

        val account = card(if (signedIn) "Signed in" else "Your account")
        if (signedIn) {
            label(account, session?.email ?: "Signed in on this device")
            label(account, DraftSync.statusText(DraftSync.SyncState.SYNCED))
            account.addView(button("Sync this journey now") { syncNow() }, LinearLayout.LayoutParams(-1, -2))
            account.addView(button("Sign out") { confirmSignOut() }, LinearLayout.LayoutParams(-1, -2))
            label(account, "Signing out removes the session from this device. Answers already synced stay in your account; answers never synced stay on this device only.")
        } else {
            label(account, "Guest mode is active. Your journey is saved on this device only and cannot be opened on another device.")
            label(account, "Email")
            val email = field(account, "you@example.com")
            label(account, "Password")
            val password = field(account, "At least 8 characters", password = true)
            statusView = TextView(this).apply { textSize = 14f; setTextColor(getColor(R.color.red)); setPadding(0, dp(8), 0, 0); visibility = View.GONE }
            account.addView(statusView)

            account.addView(button(if (mode == "signin") "Sign in" else "Create account") {
                val e = email.text.toString().trim(); val p = password.text.toString()
                if (!e.contains("@") || e.length < 5) { showStatus("Enter a valid email address."); return@button }
                if (mode == "signup" && p.length < 8) { showStatus("Use a password with at least 8 characters."); return@button }
                if (p.isEmpty()) { showStatus("Enter your password."); return@button }
                submit(e, p)
            }, LinearLayout.LayoutParams(-1, -2))

            account.addView(button(if (mode == "signin") "I need an account" else "I already have an account") {
                mode = if (mode == "signin") "signup" else "signin"; render()
            }, LinearLayout.LayoutParams(-1, -2))
            account.addView(button("Forgot password?") { forgot(email.text.toString().trim()) }, LinearLayout.LayoutParams(-1, -2))
        }

        val privacy = card("What syncs")
        label(privacy, "Synced to your account: journey answers, checklist statuses, shortlist names, visit notes, reminders and your profile name and city.")
        label(privacy, "Never uploaded: passwords, bank or card details, identity documents. Guest data is uploaded only after you sign in.")
        label(privacy, "You can delete synced data from the website's Profile page. Deleting on this device removes the local copy only.")
        binding.next.isEnabled = !busy
    }

    private fun button(title: String, click: () -> Unit) = com.google.android.material.button.MaterialButton(this).apply {
        text = title; textSize = 14f; isAllCaps = false; minHeight = dp(48); setOnClickListener { if (!busy) click() }
    }

    private fun showStatus(message: String) {
        statusView?.let { it.text = message; it.visibility = View.VISIBLE }
    }

    private fun setBusy(value: Boolean) {
        busy = value
        binding.loading.visibility = if (value) View.VISIBLE else View.GONE
        binding.loading.text = if (mode == "signup") "Creating your account…" else "Signing in…"
        binding.next.isEnabled = !value
    }

    private fun submit(email: String, password: String) {
        setBusy(true)
        val done: (AuthResult) -> Unit = { result ->
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                setBusy(false)
                when (result) {
                    is AuthResult.Success -> {
                        // Pull anything already saved on another device before rendering.
                        DraftSync.reconcile(this, scope) { runOnUiThread { render(); uploadGuestData() } }
                    }
                    is AuthResult.Failure -> { showStatus(result.message); render() }
                }
            }
        }
        if (mode == "signup") auth.signUp(this, email, password, done) else auth.signIn(this, email, password, done)
    }

    /** Push guest answers that were written before the account existed. */
    private fun uploadGuestData() {
        DraftSync.reconcile(this, "new_property") { }
        DraftSync.reconcile(this, "resale_property") { }
        DraftSync.reconcile(this, "local_profile") { }
    }

    private fun syncNow() {
        setBusy(true)
        DraftSync.reconcile(this, scope) { result ->
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                setBusy(false)
                result.fold(
                    onSuccess = { render(); AlertDialog.Builder(this).setTitle("Sync complete")
                        .setMessage("Your journey is saved to your account.").setPositiveButton("OK", null).show() },
                    onFailure = { AlertDialog.Builder(this).setTitle("Could not sync")
                        .setMessage("Your answers are still safe on this device. Check your internet connection and try again.")
                        .setPositiveButton("OK", null).show() },
                )
            }
        }
    }

    private fun confirmSignOut() {
        AlertDialog.Builder(this).setTitle("Sign out?")
            .setMessage("Your synced answers stay in your account. Anything not yet synced stays on this device.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Sign out") { _, _ ->
                setBusy(true)
                auth.signOut(this, {
                    runOnUiThread {
                        if (isFinishing || isDestroyed) return@runOnUiThread
                        setBusy(false); render()
                    }
                })
            }.show()
    }

    private fun forgot(email: String) {
        if (!email.contains("@")) { showStatus("Enter your email above, then tap Forgot password."); return }
        setBusy(true)
        // Reuses the same HTTPS path as sign-in; failures are shown, never swallowed.
        Thread {
            val ok = runCatching { PasswordReset.request(BuildConfig.GUIDANCE_URL, BuildConfig.GUIDANCE_PUBLISHABLE_KEY, email) }.isSuccess
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                setBusy(false)
                AlertDialog.Builder(this).setTitle(if (ok) "Check your email" else "Could not send the email")
                    .setMessage(if (ok) "If an account exists for $email, a reset link is on its way. Open it on any device, then sign in here."
                    else "Check your internet connection and try again.")
                    .setPositiveButton("OK", null).show()
            }
        }.start()
    }

    companion object {
        fun show(from: android.content.Context) {
            from.startActivity(Intent(from, AccountActivity::class.java))
        }
    }
}
