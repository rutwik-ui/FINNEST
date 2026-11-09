package com.finnest.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.graphics.Typeface
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat

/** A local-data entry point: links are available regardless of a user's chosen order. */
class DashboardActivity : AppCompatActivity() {
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scroll = ScrollView(this).apply { setBackgroundColor(getColor(R.color.cream)); isFillViewport = true }
        val page = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(22), dp(22), dp(22), dp(28)) }
        scroll.addView(page); setContentView(scroll)
        val heading = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        page.addView(heading, LinearLayout.LayoutParams(-1, -2))
        val brand = text("FINNEST", 20f, R.color.navy, true).apply { letterSpacing = .12f }
        heading.addView(brand, LinearLayout.LayoutParams(0, -2, 1f))
        heading.addView(Button(this).apply {
            text = "Change journey"; isAllCaps = false; textSize = 12f
            setOnClickListener { startActivity(Intent(this@DashboardActivity, MainActivity::class.java).putExtra("change_journey", true).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)); finish() }
        })
        addText(page, "YOUR HOME-BUYING JOURNEY", 12f, R.color.red, true, 26)
        addText(page, greeting(), 30f, R.color.navy, true, 8, true)
        addText(page, "A clear next step, at your pace.", 16f, R.color.muted, false, 8)
        val overview = panel(page)
        addText(overview, "YOUR JOURNEY", 12f, R.color.red, true, 0)
        addText(overview, "New Property", 22f, R.color.navy, true, 8, true)
        addText(overview, "Your notes and estimates stay on this device. Open any step to review or update them.", 15f, R.color.muted, false, 8)
        val review = Button(this).apply { text = "Open final review  →"; isAllCaps = false; setOnClickListener { open("review") } }
        overview.addView(review, LinearLayout.LayoutParams(-1, dp(54)).apply { topMargin = dp(14) })
        addText(page, "Your journey steps", 21f, R.color.navy, true, 26, true)
        addText(page, "Choose a section to continue. A checklist is not a property approval.", 14f, R.color.muted, false, 5)
        DashboardSteps.all.forEach { step ->
            val row = panel(page)
            val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            row.addView(top)
            val number = text(step.number, 14f, R.color.red, true).apply { gravity = Gravity.CENTER; setBackgroundResource(R.drawable.dashboard_badge); setPadding(dp(4), dp(4), dp(4), dp(4)) }
            top.addView(number, LinearLayout.LayoutParams(dp(46), dp(46)))
            val titleBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(14), 0, 0, 0) }
            top.addView(titleBox, LinearLayout.LayoutParams(0, -2, 1f))
            addText(titleBox, step.title, 16f, R.color.navy, true, 0)
            addText(titleBox, step.subtitle, 13f, R.color.muted, false, 4)
            top.addView(text("→", 22f, R.color.red, true))
            row.isClickable = true; row.isFocusable = true; row.contentDescription = "${step.number}. ${step.title}. ${step.subtitle}. Open step."
            row.setOnClickListener { open(step.id) }
        }
        val privacy = panel(page)
        addText(privacy, "A note on your data", 16f, R.color.navy, true, 0)
        addText(privacy, "This is a local guest journey. Android account sync, documents and expert connections are not available yet.", 14f, R.color.muted, false, 6)
    }
    private fun text(value: String, size: Float, color: Int, bold: Boolean) = TextView(this).apply {
        text = value; textSize = size; setTextColor(getColor(color)); if (bold) setTypeface(typeface, Typeface.BOLD); setLineSpacing(dp(2).toFloat(), 1f)
    }
    private fun addText(parent: LinearLayout, value: String, size: Float, color: Int, bold: Boolean, margin: Int, heading: Boolean = false) {
        val view = text(value, size, color, bold)
        if (heading) view.typeface = Typeface.create(ResourcesCompat.getFont(this, R.font.montserrat), Typeface.BOLD)
        parent.addView(view, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(margin) })
    }
    private fun panel(parent: LinearLayout): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(17), dp(18), dp(17)); setBackgroundResource(R.drawable.card)
        parent.addView(this, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })
    }
    private fun open(id: String) {
        val intent = when (id) {
            "requirements" -> Intent(this, RequirementsActivity::class.java)
            "developer" -> Intent(this, DeveloperChecksActivity::class.java)
            "projects" -> Intent(this, ProjectsActivity::class.java)
            "visit" -> Intent(this, DeveloperChecksActivity::class.java).putExtra("visit_observations", true)
            "feedback" -> Intent(this, DeveloperChecksActivity::class.java).putExtra("resident_feedback", true)
            "loan" -> Intent(this, LoanGuideActivity::class.java)
            "legal" -> Intent(this, DeveloperChecksActivity::class.java).putExtra("legal_due_diligence", true)
            "financial" -> Intent(this, FinancialDueDiligenceActivity::class.java)
            "review" -> Intent(this, FinalReviewActivity::class.java)
            else -> return
        }
        startActivity(intent)
    }
    private fun greeting(): String = when (java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)) {
        in 5..11 -> "Good morning"; in 12..16 -> "Good afternoon"; else -> "Good evening"
    }
    companion object {
        fun show(context: Context) {
            context.startActivity(Intent(context, DashboardActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
        }
    }
}
