package com.finnest.app

import android.os.Bundle
import android.content.Intent
import android.view.View
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.finnest.app.databinding.ActivityMainBinding

/** Guest entry flow; cloud accounts are not yet available. */
class EntryViewModel : ViewModel() { var page = 0; var selected: String? = null }

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var model: EntryViewModel
    private val prefs by lazy { getSharedPreferences("entry", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val header = binding.root.getChildAt(0) as android.widget.LinearLayout
        header.removeViewAt(1)
        header.addView(FinnestUi.brand(this))
        header.background = android.graphics.drawable.LayerDrawable(arrayOf(android.graphics.drawable.ColorDrawable(FinnestUi.LINE), android.graphics.drawable.ColorDrawable(android.graphics.Color.WHITE))).apply { setLayerInset(1, 0, 0, 0, FinnestUi.dp(this@MainActivity, 1)) }
        binding.illustration.background = FinnestUi.shape(this, FinnestUi.BLUSH, null, 18)
        binding.illustration.clipToOutline = true
        FinnestUi.styleTree(binding.root)
        binding.back.visibility = View.GONE
        model = ViewModelProvider(this)[EntryViewModel::class.java]
        if (savedInstanceState == null && prefs.getBoolean("intro_complete", false)) model.page = 3
        model.selected = model.selected ?: prefs.getString("journey", null)
        if (savedInstanceState == null && !intent.getBooleanExtra("change_journey", false) &&
            prefs.getBoolean("intro_complete", false)) {
            when (model.selected) {
                "New Property" -> DashboardActivity.show(this, "new_property")
                "Resale Property" -> DashboardActivity.show(this, "resale_property")
            }
        }
        binding.back.setOnClickListener { goBack() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { if (model.page > 0) goBack() else finish() }
        })
        binding.primary.setOnClickListener {
            if (model.page < 2) model.page++ else if (model.page == 2) finishIntro()
            else if (model.page == 3 && model.selected != null) {
                prefs.edit().putString("journey", model.selected).apply()
                if (model.selected == "New Property") DashboardActivity.show(this, "new_property") else DashboardActivity.show(this, "resale_property")
            }
            else if (model.page == 4 && model.selected == "New Property") { DashboardActivity.show(this, "new_property") }
            else if (model.page == 4) model.page = 3
            render()
        }
        binding.secondary.setOnClickListener { if (model.page < 3) { finishIntro(); render() } else if (model.page == 4) { model.page = 3; render() } }
        render()
    }
    private fun finishIntro() {
        prefs.edit().putBoolean("intro_complete", true).apply(); model.page = 3
        if(AccountClient.userId(this)==null && !prefs.getBoolean("guest_session",false)) {
            startActivity(Intent(this,AccountActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK));finish()
        }
    }
    private fun goBack() { if (model.page > 0) { model.page--; render() } }
    private fun lp(top: Int = 12) = android.widget.LinearLayout.LayoutParams(-1, -2).apply { topMargin = FinnestUi.dp(this@MainActivity, top) }
    /** Website-style list card: a small red badge beside each line. */
    private fun listCard(rows: List<Pair<String, String>>) {
        val card = FinnestUi.card(this, 16)
        rows.forEachIndexed { i, (badge, text) ->
            val row = android.widget.LinearLayout(this).apply { gravity = android.view.Gravity.CENTER_VERTICAL; setPadding(0, FinnestUi.dp(context, 8), 0, FinnestUi.dp(context, 8)) }
            row.addView(FinnestUi.label(this, badge, 13f, 700, FinnestUi.RED_TEXT).apply { gravity = android.view.Gravity.CENTER; background = FinnestUi.shape(context, FinnestUi.BLUSH, FinnestUi.PILL_LINE, 10) }, android.widget.LinearLayout.LayoutParams(FinnestUi.dp(this, 34), FinnestUi.dp(this, 34)))
            row.addView(FinnestUi.label(this, text, 15f, 600, FinnestUi.NAVY), android.widget.LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = FinnestUi.dp(this@MainActivity, 14) })
            card.addView(row)
            if (i < rows.size - 1) card.addView(View(this).apply { setBackgroundColor(FinnestUi.LINE) }, android.widget.LinearLayout.LayoutParams(-1, FinnestUi.dp(this, 1)).apply { marginStart = FinnestUi.dp(this@MainActivity, 48) })
        }
        binding.details.addView(card, lp(4))
    }
    private fun note(text: String) {
        val card = android.widget.LinearLayout(this).apply { background = FinnestUi.shape(context, FinnestUi.NOTICE_BG, FinnestUi.NOTICE_LINE, 14); setPadding(FinnestUi.dp(context, 16), FinnestUi.dp(context, 14), FinnestUi.dp(context, 16), FinnestUi.dp(context, 14)) }
        card.addView(FinnestUi.body(this, text, 13f, FinnestUi.INK))
        binding.details.addView(card, lp())
    }
    private fun choice(title: String, text: String, selected: Boolean, action: () -> Unit) {
        val card = FinnestUi.card(this, 18).apply {
            background = FinnestUi.shape(context, if (selected) FinnestUi.BLUSH else android.graphics.Color.WHITE, if (selected) FinnestUi.RED else FinnestUi.LINE, 16)
            isClickable = true; isFocusable = true; contentDescription = title; setOnClickListener { action() }
        }
        val top = android.widget.LinearLayout(this).apply { gravity = android.view.Gravity.CENTER_VERTICAL }
        top.addView(FinnestUi.h2(this, title, 18f), android.widget.LinearLayout.LayoutParams(0, -2, 1f))
        top.addView(FinnestUi.label(this, if (selected) "✓" else "→", 16f, 800, if (selected) android.graphics.Color.WHITE else FinnestUi.RED).apply { gravity = android.view.Gravity.CENTER; background = FinnestUi.shape(context, if (selected) FinnestUi.RED else FinnestUi.BLUSH, null, 14) }, android.widget.LinearLayout.LayoutParams(FinnestUi.dp(this, 28), FinnestUi.dp(this, 28)))
        card.addView(top)
        card.addView(FinnestUi.body(this, text), lp(6))
        binding.details.addView(card, lp())
    }
    private fun render(): Unit = with(binding) {
        details.removeAllViews()
        back.isEnabled = model.page > 0
        back.alpha = if (back.isEnabled) 1f else .35f
        illustration.isVisible = model.page < 3
        illustration.scaleType=android.widget.ImageView.ScaleType.CENTER_INSIDE
        progress.isVisible = model.page < 3
        caption.isVisible = model.page == 0
        secondary.isVisible = model.page < 3 || model.page == 4
        primary.isEnabled = model.page != 3 || model.selected != null
        primary.text = if (model.page == 3) if (model.selected == null) "Choose a journey to continue" else "Continue with ${model.selected}" else if (model.page == 4 && model.selected == "New Property") "Open requirements & budget  →" else if (model.page == 4) "Change journey" else "Continue  →"
        secondary.text = if (model.page == 4) "Back to journey selection" else "Skip"
        when (model.page) {
            0 -> { eyebrow.text = "YOUR HOME-BUYING JOURNEY"; title.text = "Buying a home is a big decision."; description.text = "There are questions to ask, documents to understand and choices to make. You do not have to figure it all out at once."; illustration.setImageResource(R.drawable.intro_1); caption.text = "A calmer place to think through your next steps." }
            1 -> { eyebrow.text = "A CLEARER WAY FORWARD"; title.text = "Know what to check, one step at a time."; description.text = "Keep your own questions and notes together, and see what still needs attention."; illustration.setImageResource(R.drawable.intro_2); listCard(listOf("01" to "Organize what matters to you", "02" to "Keep track of unknowns", "03" to "Know when to seek qualified help")); note("FINNEST supports your decision-making; it does not verify a property or replace qualified professionals.") }
            2 -> { eyebrow.text = "YOUR DECISION, YOUR PACE"; title.text = "Move forward with more clarity."; description.text = "A checklist is a way to stay oriented—not an approval to buy."; illustration.setImageResource(R.drawable.intro_3); listCard(listOf("✓" to "Checked by you", "≡" to "Information provided", "?" to "Pending / unknown", "★" to "Professional review")) }
            3 -> { eyebrow.text = "CHOOSE YOUR JOURNEY"; title.text = "What kind of property journey are you exploring?"; description.text = "Choose one path. Each keeps its own checklist and progress."; choice("New Property", "Plan steps to explore: project and developer checks, visits, loans, and due diligence.", model.selected == "New Property") { model.selected = "New Property"; render() }; choice("Resale Property", "Areas to review: property condition, seller ownership, society records, dues, legal review, and handover.", model.selected == "Resale Property") { model.selected = "Resale Property"; render() }; note("Not sure? New property is typically being sold for the first time; resale has a prior owner and may involve society records.") }
            else -> { eyebrow.text = "YOUR JOURNEY"; title.text = "${model.selected} selected."; description.text = if (model.selected == "New Property") "Your choice is saved on this device. Start your requirements and budget or return later." else "Your choice is saved on this device. The resale journey is coming in a later phase." }
        }
        progress.progress = model.page + 1
    }
}
