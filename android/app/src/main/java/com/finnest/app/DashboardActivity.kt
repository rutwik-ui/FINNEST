package com.finnest.app

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray

/** Home, laid out like the FINNEST mobile website home for the selected journey. Counts are local entries, not verification. */
class DashboardActivity : AppCompatActivity() {
    private lateinit var roadmap: JourneyRoadmapView
    private lateinit var scope: String
    private val main = Handler(Looper.getMainLooper())
    private var marked: TextView? = null; private var projects: TextView? = null; private var edited: TextView? = null
    private lateinit var totalLine: TextView; private lateinit var totalBar: ProgressBar
    private fun dp(value: Int) = FinnestUi.dp(this, value)
    private val resale get() = scope == "resale_property"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scope = intent.getStringExtra(SCOPE) ?: if (getSharedPreferences("entry", MODE_PRIVATE).getString("journey", "New Property") == "Resale Property") "resale_property" else "new_property"
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(FinnestUi.CREAM) }
        setContentView(root)
        val heading = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        root.addView(heading, LinearLayout.LayoutParams(-1, dp(60)))
        FinnestUi.header(this, heading, scope)
        val scroll = ScrollView(this).apply { isFillViewport = true; isVerticalScrollBarEnabled = false }
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        val outer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(outer)
        roadmap = JourneyRoadmapView(this, scope)
        roadmap.onProgress = { showTotal(it) }
        outer.addView(roadmap, LinearLayout.LayoutParams(-1, -2))
        val page = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(24), dp(16), dp(32)) }
        outer.addView(page, LinearLayout.LayoutParams(-1, -2))

        // .page-head
        page.addView(FinnestUi.eyebrow(this, "Your home-buying journey"), wrap(top = 7))
        page.addView(FinnestUi.h1(this, "Your home-buying journey"), full(top = 9))
        page.addView(FinnestUi.body(this, "A clearer path to a place of your own."), full(top = 9))

        // .journey-card with the photo on top (column-reverse on phones)
        val journey = FinnestUi.card(this, 0)
        page.addView(journey, full(top = 20))
        val photo = FrameLayout(this)
        photo.addView(ImageView(this).apply { setImageResource(R.drawable.home_building); scaleType = ImageView.ScaleType.CENTER_CROP; contentDescription = "Modern apartment building" }, FrameLayout.LayoutParams(-1, -1))
        photo.addView(View(this).apply { background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(0x00FFFFFF, Color.WHITE)) }, FrameLayout.LayoutParams(-1, dp(40), Gravity.BOTTOM))
        journey.addView(photo, LinearLayout.LayoutParams(-1, dp(150)))
        val journeyBody = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(11), dp(18), dp(18)) }
        journey.addView(journeyBody, full())
        journeyBody.addView(FinnestUi.eyebrow(this, "Selected journey"), wrap())
        journeyBody.addView(FinnestUi.h2(this, if (resale) "Resale Property" else "New Property"), full(top = 8))
        journeyBody.addView(FinnestUi.body(this, "Your journey, at your own pace."), full(top = 7))
        journeyBody.addView(FinnestUi.link(this, "Change journey") { changeJourney() }, wrap(top = 11))

        // .recommendation
        val rec = FinnestUi.card(this).apply { background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(0xFFFFF8F7.toInt(), Color.WHITE)).apply { cornerRadius = FinnestUi.dpf(this@DashboardActivity, 16f); setStroke(dp(1), FinnestUi.LINE) } }
        page.addView(rec, full(top = 17))
        rec.addView(FinnestUi.eyebrow(this, "Recommended next action"), wrap())
        rec.addView(FinnestUi.h2(this, if (resale) "Explore your journey" else "Define your requirements", 23f), full(top = 9))
        rec.addView(FinnestUi.body(this, if (resale) "Record your planning, condition and document questions in the resale areas." else "Start with what matters to you, then keep your questions and notes together."), full(top = 9))
        rec.addView(iconLine(R.drawable.journey_info, FinnestUi.RED, "Your entries are saved on this device and are not independently verified.", 12f, 0xFF85858B.toInt(), 17), full(top = 20))
        rec.addView(FinnestUi.primaryButton(this, if (resale) "View journey" else "Take this step") {
            startActivity(if (resale) Intent(this, ResaleOverviewActivity::class.java) else Intent(this, RequirementsActivity::class.java))
        }, full(top = 20))

        // .glance
        val glance = FinnestUi.card(this)
        page.addView(glance, full(top = 17))
        if (resale) {
            glance.addView(FinnestUi.h2(this, "Resale journey"), full())
            glance.addView(FinnestUi.body(this, "Explore the six prototype areas as local notes. New Property notes are kept separately."), full(top = 7))
            glance.addView(FinnestUi.link(this, "Open resale journey") { startActivity(Intent(this, ResaleOverviewActivity::class.java)) }, wrap(top = 11))
        } else {
            val head = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.BOTTOM }
            glance.addView(head, full())
            val titles = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            head.addView(titles, LinearLayout.LayoutParams(0, -2, 1f))
            titles.addView(FinnestUi.eyebrow(this, "Your progress"), wrap())
            titles.addView(FinnestUi.h2(this, "At a glance"), full(top = 7))
            head.addView(FinnestUi.link(this, "View journey") { startActivity(Intent(this, JourneySummaryActivity::class.java)) }, wrap())
            glance.addView(FinnestUi.label(this, "Counts below cover developer checks only; open the summary for all checklist groups.", 12f, 400, FinnestUi.MUTED, 0f, 19.2f), full(top = 4))
            val stats = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            glance.addView(stats, full(top = 19))
            marked = stat(stats, R.drawable.journey_check, 0xFF33A888.toInt(), "checks marked\nby you", 0)
            projects = stat(stats, R.drawable.journey_bookmark, 0xFF328BD2.toInt(), "saved projects", 8)
            edited = stat(stats, R.drawable.journey_clock, 0xFFEDA531.toInt(), "checks with\nyour entries", 8)
        }
        // Whole-journey recorded entries: durable Room data, shown in both journeys.
        totalLine = FinnestUi.label(this, "Loading saved progress…", 12f, 600, FinnestUi.INK).apply { contentDescription = "Journey recorded progress" }
        glance.addView(totalLine, full(top = 16))
        totalBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100; progressTintList = ColorStateList.valueOf(FinnestUi.RED); progressBackgroundTintList = ColorStateList.valueOf(FinnestUi.LINE)
        }
        glance.addView(totalBar, LinearLayout.LayoutParams(-1, dp(4)).apply { topMargin = dp(8) })

        // .helpful
        val help = FinnestUi.card(this)
        page.addView(help, full(top = 17))
        help.addView(FinnestUi.label(this, "Good to know", 16f, 700, FinnestUi.NAVY, -0.025f, 21.6f), full())
        help.addView(iconLine(R.drawable.journey_shield, FinnestUi.RED, "FINNEST helps you stay organized. It does not verify a property or replace qualified advice.", 13f, FinnestUi.MUTED, 21), full(top = 13))
        help.addView(FinnestUi.link(this, "Browse guidance") { startActivity(Intent(this, GuidanceActivity::class.java)) }, wrap(top = 13))

        // .local-note
        page.addView(iconLine(R.drawable.journey_lock, FinnestUi.FAINT, "Your progress stays on this device for now.", 12f, FinnestUi.FAINT, 15).apply { setPadding(dp(7), 0, 0, 0) }, full(top = 17))

        FinnestUi.bottomNav(this, root, scope, "home")
    }
    private fun full(top: Int = 0) = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(top) }
    private fun wrap(top: Int = 0) = LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(top) }
    private fun iconLine(icon: Int, tint: Int, value: String, size: Float, color: Int, iconSize: Int) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        addView(ImageView(this@DashboardActivity).apply { setImageResource(icon); imageTintList = ColorStateList.valueOf(tint); importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO },
            LinearLayout.LayoutParams(dp(iconSize), dp(iconSize)).apply { topMargin = dp(1) })
        addView(FinnestUi.label(this@DashboardActivity, value, size, 400, color, 0f, size * 1.55f), LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(if (iconSize > 18) 11 else 8) })
    }
    private fun stat(parent: LinearLayout, icon: Int, color: Int, caption: String, start: Int): TextView {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; background = FinnestUi.shape(this@DashboardActivity, FinnestUi.STAT_BG, FinnestUi.LINE, 13)
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }
        parent.addView(box, LinearLayout.LayoutParams(0, -1, 1f).apply { marginStart = dp(start) })
        box.addView(ImageView(this).apply {
            setImageResource(icon); imageTintList = ColorStateList.valueOf(Color.WHITE); setPadding(dp(5), dp(5), dp(5), dp(5))
            background = FinnestUi.shape(this@DashboardActivity, color, null, 9); importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, LinearLayout.LayoutParams(dp(27), dp(27)))
        val number = FinnestUi.label(this, "–", 21f, 700, FinnestUi.NAVY, 0f, 22f)
        box.addView(number, wrap(top = 8))
        box.addView(FinnestUi.label(this, caption, 10f, 400, 0xFF7D838B.toInt(), 0f, 13.5f), full(top = 5))
        return number
    }
    private fun showTotal(total: JourneyProgress.Count) {
        totalLine.text = "All steps: ${total.recorded} of ${total.total} entries recorded · ${total.percent}%"
        totalBar.progress = total.percent
    }
    private fun loadStats() {
        if (resale) return
        DraftRepository.loadSaved(this, "new_property") { result -> main.post {
            if (isFinishing || isDestroyed) return@post
            result.onSuccess { draft ->
                val keys = JourneyProgress.keys("new_property", "developer")
                marked?.text = keys.count { draft[it] == "Checked by you" }.toString()
                edited?.text = JourneyProgress.count("new_property", "developer", draft).recorded.toString()
                projects?.text = runCatching { val a = JSONArray(draft["shortlist_projects_v1"] ?: "[]"); (0 until a.length()).count { a.optJSONObject(it)?.optString("name").orEmpty().isNotBlank() } }.getOrDefault(0).toString()
            }.onFailure { listOf(marked, edited, projects).forEach { it?.text = "–" } }
        } }
    }
    private fun changeJourney() {
        startActivity(Intent(this, MainActivity::class.java).putExtra("change_journey", true).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)); finish()
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val requested = intent.getStringExtra(SCOPE)
        if (requested != null && requested != scope) { setIntent(intent); recreate() }
    }
    override fun onResume() { super.onResume(); if (::roadmap.isInitialized) roadmap.refresh(); loadStats() }
    companion object {
        const val SCOPE = "dashboard_scope"
        fun show(context: Context, scope: String? = null) {
            context.startActivity(Intent(context, DashboardActivity::class.java).apply { if (scope != null) putExtra(SCOPE, scope) }
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
        }
    }
}
