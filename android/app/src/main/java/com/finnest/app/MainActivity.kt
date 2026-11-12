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
    private fun finishIntro() { prefs.edit().putBoolean("intro_complete", true).apply(); model.page = 3 }
    private fun goBack() { if (model.page > 0) { model.page--; render() } }
    private fun detail(text: String, action: (() -> Unit)? = null) {
        val row = TextView(this).apply {
            this.text = text; textSize = 16f; setTextColor(getColor(R.color.navy)); setPadding(20, 20, 20, 20)
            setBackgroundResource(R.drawable.card)
            if (action != null) { isClickable = true; isFocusable = true; setOnClickListener { action() } }
        }
        val params = android.widget.LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 10 }
        binding.details.addView(row, params)
    }
    private fun render(): Unit = with(binding) {
        details.removeAllViews()
        back.isEnabled = model.page > 0
        back.alpha = if (back.isEnabled) 1f else .35f
        illustration.isVisible = model.page < 3
        progress.isVisible = model.page < 3
        caption.isVisible = model.page == 0
        secondary.isVisible = model.page < 3 || model.page == 4
        primary.isEnabled = model.page != 3 || model.selected != null
        primary.text = if (model.page == 3) "Choose a journey to continue  →" else if (model.page == 4 && model.selected == "New Property") "Open requirements & budget  →" else if (model.page == 4) "Change journey" else "Continue  →"
        secondary.text = if (model.page == 4) "Back to journey selection" else "Skip"
        when (model.page) {
            0 -> { eyebrow.text = "YOUR HOME-BUYING JOURNEY"; title.text = "Buying a home is a big decision."; description.text = "There are questions to ask, documents to understand and choices to make. You do not have to figure it all out at once."; illustration.setImageResource(R.drawable.intro_1); caption.text = "A calmer place to think through your next steps." }
            1 -> { eyebrow.text = "A CLEARER WAY FORWARD"; title.text = "Know what to check, one step at a time."; description.text = "Keep your own questions and notes together, and see what still needs attention."; illustration.setImageResource(R.drawable.intro_2); detail("01   Organize what matters to you\n\n02   Keep track of unknowns\n\n03   Know when to seek qualified help"); detail("FINNEST supports your decision-making; it does not verify a property or replace qualified professionals.") }
            2 -> { eyebrow.text = "YOUR DECISION, YOUR PACE"; title.text = "Move forward with more clarity."; description.text = "A checklist is a way to stay oriented—not an approval to buy."; illustration.setImageResource(R.drawable.intro_3); detail("✓   Checked by you\n\n▤   Information provided\n\n?   Pending / unknown\n\n♙   Professional review") }
            3 -> { eyebrow.text = "CHOOSE YOUR JOURNEY"; title.text = "What kind of property journey are you exploring?"; description.text = "Choose one path. Each keeps its own checklist and progress."; detail("${if (model.selected == "New Property") "✓  " else ""}New Property\n\nPlan steps to explore: project and developer checks, visits, loans, and due diligence.  →") { model.selected = "New Property"; render() }; detail("${if (model.selected == "Resale Property") "✓  " else ""}Resale Property\n\nAreas to review: property condition, seller ownership, society records, dues, legal review, and handover.  →") { model.selected = "Resale Property"; render() }; detail("Not sure? New property is typically being sold for the first time; resale has a prior owner and may involve society records.") }
            else -> { eyebrow.text = "YOUR JOURNEY"; title.text = "${model.selected} selected."; description.text = if (model.selected == "New Property") "Your choice is saved on this device. Start your requirements and budget or return later." else "Your choice is saved on this device. The resale journey is coming in a later phase."; detail("Your data stays on this device for now. Account sign-in and cloud synchronization are not yet available.") }
        }
        progress.progress = model.page + 1
    }
}
