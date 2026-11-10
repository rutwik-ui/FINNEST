package com.finnest.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.finnest.app.databinding.ActivityDeveloperChecksBinding

/** Informational common screens; no expert profiles, messaging or assessment service. */
class CommonActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDeveloperChecksBinding
    private val screen by lazy { intent.getStringExtra("screen") ?: "experts" }
    private val scope by lazy { if (intent.getStringExtra("scope") == "resale_property") "resale_property" else "new_property" }
    private var selected: RelatedCheck? = null
    private var busy = false
    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeveloperChecksBinding.inflate(layoutInflater); setContentView(binding.root)
        binding.loading.visibility = View.GONE
        binding.back.setOnClickListener { leave { finish() } }; binding.backBottom.text = "Back"; binding.backBottom.setOnClickListener { leave { finish() } }
        binding.saveLater.text = "Home"; binding.saveLater.setOnClickListener { leave { home() } }
        binding.next.text = "Back to home"; binding.next.isEnabled = true; binding.next.setOnClickListener { leave { home() } }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) { override fun handleOnBackPressed() { leave { finish() } } })
        when (screen) {
            "experts" -> experts()
            "expert_detail" -> expertDetail()
            "risk" -> risk()
            "about" -> about()
            else -> finish()
        }
    }
    private fun leave(action: () -> Unit) {
        if (busy) return
        if (screen != "risk") { action(); return }
        DraftRepository.afterPending(scope) { ok -> runOnUiThread {
            if (isFinishing || isDestroyed) return@runOnUiThread
            if (ok) action() else AlertDialog.Builder(this).setTitle("Could not save your draft")
                .setMessage("Check device storage and retry before leaving.")
                .setPositiveButton("Retry") { _, _ -> DraftRepository.retryFailed(this, scope) { runOnUiThread { leave(action) } } }
                .setNegativeButton("Keep editing", null).show()
        } }
    }
    private fun home() {
        if (getSharedPreferences("entry", MODE_PRIVATE).getString("journey", "New Property") == "Resale Property") ResaleOverviewActivity.show(this)
        else DashboardActivity.show(this)
    }
    private fun label(parent: LinearLayout, value: String, bold: Boolean = false): TextView = TextView(this).apply {
        text = value; textSize = if (bold) 17f else 14f; setTextColor(getColor(R.color.navy)); setPadding(0, dp(10), 0, dp(6))
        if (bold) setTypeface(null, android.graphics.Typeface.BOLD); parent.addView(this)
    }
    private fun card(title: String): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(8), dp(16), dp(16)); setBackgroundResource(R.drawable.card)
        binding.form.addView(this, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(14) }); label(this, title, true)
    }
    private fun button(parent: LinearLayout, title: String, action: () -> Unit) {
        parent.addView(Button(this).apply { text = title; isAllCaps = false; setOnClickListener { action() } }, LinearLayout.LayoutParams(-1, -2))
    }
    private fun open(screen: String) { startActivity(Intent(this, CommonActivity::class.java).putExtra("screen", screen).putExtra("scope", scope)) }
    private fun experts() {
        binding.stepLabel.text = "FIND THE RIGHT KIND OF HELP"; binding.screenTitle.text = "Experts directory"
        binding.screenDescription.text = "Know what each professional can help with and when a conversation may be useful."
        binding.screenNotice.text = "No experts are configured. No profiles, credentials, ratings, availability, prices or contact details are supplied."
        val filters = card("Category")
        label(filters, "Filter informational roles only; these are not available professionals. Journey-stage filters are not specified in the reference.")
        val roleNames = listOf("All roles") + CommonContent.professionalRoles.map { it.title } + CommonContent.optionalRoles.map { it.title }
        val spinner = Spinner(this).apply { adapter = ArrayAdapter(this@CommonActivity, android.R.layout.simple_spinner_dropdown_item, roleNames); contentDescription = "Expert role category" }
        filters.addView(spinner, LinearLayout.LayoutParams(-1, dp(48)))
        val results = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }; binding.form.addView(results)
        fun render(index: Int) {
            results.removeAllViews()
            val roles = CommonContent.professionalRoles + CommonContent.optionalRoles
            val visible = if (index == 0) roles else roles.filter { it.title == roleNames[index] }
            visible.forEach { role ->
                val block = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(8), dp(16), dp(16)); setBackgroundResource(R.drawable.card) }
                results.addView(block, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })
                label(block, role.title, true); label(block, role.description)
                if (role in CommonContent.optionalRoles) label(block, "Optional personal preference—not legal, financial or technical due diligence.")
            }
        }
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) { render(position) }
        }
        val detail = card("Expert detail & contact")
        label(detail, "No individual expert profiles or contact services are configured.")
        button(detail, "View expert detail state") { open("expert_detail") }
    }
    private fun expertDetail() {
        binding.stepLabel.text = "EXPERT DETAIL"; binding.screenTitle.text = "A clearer way to ask for help"
        binding.screenDescription.text = "Professional help can support informed decisions."
        binding.screenNotice.text = "No booking, messaging, contact-sharing or payment service is connected."
        val empty = card("No configured expert selected")
        label(empty, "There are no expert profiles available. No request can be sent and no professional contact information is collected here.")
        val disabled = Button(this).apply { text = "Request a conversation · unavailable"; isAllCaps = false; isEnabled = false }
        empty.addView(disabled, LinearLayout.LayoutParams(-1, -2))
        binding.next.text = "Back to directory"; binding.next.setOnClickListener { leave { finish() } }
    }
    private fun about() {
        binding.stepLabel.text = "ABOUT FINNEST"; binding.screenTitle.text = "A clearer way to approach home buying"
        binding.screenDescription.text = "A place to keep questions and notes together as you consider your next steps."
        binding.screenNotice.text = "FINNEST is decision support, not a lender, legal adviser or property inspector."
        val questions = card("Questions to keep in view")
        listOf("What do I know, and where did it come from?", "What is still unknown or pending?", "Which decisions call for qualified advice?", "What is important to me and my household?").forEach { label(questions, it) }
        val limits = card("Important limits")
        label(limits, "FINNEST does not verify documents, certify a property, guarantee safety or determine loan eligibility. Local requirements vary; consult qualified professionals.")
        val data = card("Your data in this Android app")
        label(data, "Guest journey entries and reminders are stored on this device. Uninstalling removes them; backup and account sync are not available. Published guidance is fetched online from Supabase. No expert contact service is connected.")
    }
    private fun risk() {
        binding.stepLabel.text = "UNDERSTAND A CONCERN"; binding.screenTitle.text = "A key promise is only verbal"
        binding.screenDescription.text = "A measured guide to one kind of concern and information you could ask for next."
        binding.screenNotice.text = "Illustrative pattern only. This is not a finding about a property, person or seller. A verbal promise alone does not establish fraud or safety."
        label(card("Why it may matter"), "If an important term affects price, timing, permitted use or handover, relying on an unwritten understanding can make it harder to clarify what was agreed.")
        val questions = card("Consider asking")
        listOf("Can this term be included clearly in the written agreement?", "Which document or person can confirm the detail?", "Does a qualified local lawyer recommend any additional review?").forEach { label(questions, it) }
        val record = card("Keep your record precise")
        label(record, "Separate what you observed, what someone told you, what evidence has been reviewed and what remains unknown. Pause and seek qualified advice if a material concern remains unresolved.")
        label(record, "Related checklist item")
        val checks = CommonContent.relatedChecks(scope)
        val options = listOf("Choose a relevant item") + checks.map { "${it.group} · ${it.title}" }
        val spinner = Spinner(this).apply { adapter = ArrayAdapter(this@CommonActivity, android.R.layout.simple_spinner_dropdown_item, options); contentDescription = "Related checklist item" }
        record.addView(spinner, LinearLayout.LayoutParams(-1, dp(48)))
        val save = Button(this).apply { text = "Save as pending"; isAllCaps = false; isEnabled = false }
        record.addView(save, LinearLayout.LayoutParams(-1, -2))
        val openCheck = Button(this).apply { text = "Open related checklist"; isAllCaps = false; isEnabled = false }
        record.addView(openCheck, LinearLayout.LayoutParams(-1, -2))
        val message = label(record, "Select an item to mark it pending or open its checklist.")
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selected = if (position == 0) null else checks[position - 1]
                save.isEnabled = selected != null && !busy; openCheck.isEnabled = selected != null && !busy
            }
        }
        save.setOnClickListener {
            val check = selected ?: return@setOnClickListener
            busy = true; spinner.isEnabled = false; save.isEnabled = false; openCheck.isEnabled = false
            DraftRepository.load(this, scope) { result -> runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                result.fold(onSuccess = { draft ->
                    val previous = draft[check.key]?.takeIf { it.isNotBlank() } ?: "Not selected"
                    AlertDialog.Builder(this).setTitle("Mark as pending?")
                        .setMessage("${check.group} · ${check.title}\nCurrent saved status: $previous\n\nThis replaces the status with Pending / unknown. Notes, sources and dates are not changed.")
                        .setNegativeButton("Cancel") { _, _ -> busy = false; spinner.isEnabled = true; save.isEnabled = true; openCheck.isEnabled = true }
                        .setOnCancelListener { busy = false; spinner.isEnabled = true; save.isEnabled = true; openCheck.isEnabled = true }
                        .setPositiveButton("Mark pending") { _, _ ->
                            DraftRepository.save(this, scope, check.key, "Pending / unknown")
                            fun finishSave() {
                                DraftRepository.afterPending(scope) { ok -> runOnUiThread {
                                    if (isFinishing || isDestroyed) return@runOnUiThread
                                    busy = false; spinner.isEnabled = true; save.isEnabled = true; openCheck.isEnabled = true
                                    message.text = if (ok) "Saved as Pending / unknown on this device." else "Could not save. Retry before leaving this screen."
                                    if (!ok) AlertDialog.Builder(this).setTitle("Could not save your draft")
                                        .setMessage("Check device storage and retry.")
                                        .setPositiveButton("Retry") { _, _ -> busy = true; spinner.isEnabled = false; save.isEnabled = false; openCheck.isEnabled = false; DraftRepository.retryFailed(this, scope) { finishSave() } }
                                        .setNegativeButton("Keep editing", null).show()
                                } }
                            }
                            finishSave()
                        }.show()
                }, onFailure = { busy = false; spinner.isEnabled = true; save.isEnabled = true; openCheck.isEnabled = true; message.text = "Could not load the saved status. Please try again." })
            } }
        }
        openCheck.setOnClickListener {
            val check = selected ?: return@setOnClickListener
            if (scope == "resale_property") startActivity(Intent(this, ResaleConditionActivity::class.java).putExtra("resale_section", check.destination))
            else JourneyNavigation.intent(this, check.destination)?.let { startActivity(it) }
        }
        button(record, "Professional help") { open("experts") }
        binding.next.text = "Return to previous screen"; binding.next.setOnClickListener { leave { finish() } }
    }
}
