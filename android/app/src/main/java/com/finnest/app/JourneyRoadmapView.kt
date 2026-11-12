package com.finnest.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.*

/** Native image, horizontal steps and durable Room-backed progress, shared by both journeys. */
class JourneyRoadmapView(context: Context, private val scope: String, private val current: String? = null) : LinearLayout(context) {
    data class Step(val id: String, val title: String, val icon: Int)
    private val steps = if (scope == "resale_property") listOf(
        Step("requirements", "Requirements & location", R.drawable.journey_clipboard),
        Step("condition", "Property condition", R.drawable.journey_search),
        Step("seller", "Seller, ownership & title", R.drawable.journey_user),
        Step("society", "Society documents & dues", R.drawable.journey_building),
        Step("legal", "Legal review", R.drawable.journey_handshake),
        Step("handover", "Agreement & handover", R.drawable.journey_key),
        Step("summary", "Journey summary", R.drawable.journey_file)
    ) else listOf(
        Step("requirements", "Requirements & budget", R.drawable.journey_clipboard),
        Step("developer", "Developer checks", R.drawable.journey_search),
        Step("projects", "Shortlist projects", R.drawable.journey_bookmark),
        Step("visit", "Visit observations", R.drawable.journey_file),
        Step("loan", "Home loan guide", R.drawable.journey_landmark),
        Step("legal", "Legal due diligence", R.drawable.journey_handshake),
        Step("financial", "Financial due diligence", R.drawable.journey_rupee),
        Step("review", "Final review", R.drawable.journey_check)
    )
    private val main = Handler(Looper.getMainLooper())
    private val numbers = mutableMapOf<String, TextView>()
    private val rows = mutableMapOf<String, LinearLayout>()
    private val horizontal: HorizontalScrollView
    private var generation = 0
    /** Receives durable recorded-entry totals so pages can show truthful progress. */
    var onProgress: ((JourneyProgress.Count) -> Unit)? = null
    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    init {
        // Website `.roadmap` on phones: 200px banner, then a white strip of 44px step chips and a 1px line.
        orientation = VERTICAL; setBackgroundColor(context.getColor(R.color.surface))
        addView(ImageView(context).apply {
            setImageResource(R.drawable.parkside_community); scaleType = ImageView.ScaleType.CENTER_CROP
            contentDescription = "A couple walking through a green residential community beside an apartment building"
        }, LayoutParams(-1, dp(200)))
        horizontal = HorizontalScrollView(context).apply { isHorizontalScrollBarEnabled = false; isFillViewport = false; overScrollMode = OVER_SCROLL_NEVER }
        val strip = LinearLayout(context).apply { orientation = HORIZONTAL; setPadding(dp(16), dp(12), dp(8), dp(12)) }
        horizontal.addView(strip)
        steps.forEachIndexed { index, step ->
            val active = step.id == current
            val row = LinearLayout(context).apply {
                orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(7), dp(4), dp(12), dp(4))
                isClickable = true; isFocusable = true
                background = android.graphics.drawable.RippleDrawable(ColorStateList.valueOf(0x14E7373E), GradientDrawable().apply {
                    cornerRadius = dp(8).toFloat(); setColor(if (active) FinnestUi.BLUSH else android.graphics.Color.WHITE)
                    setStroke(dp(1), if (active) FinnestUi.RED else FinnestUi.LINE)
                }, null)
                contentDescription = step.title + if (active) ". Current step" else ". Open step"
                setOnClickListener { if (!active) open(step.id) }
            }
            strip.addView(row, LayoutParams(-2, dp(44)).apply { marginEnd = dp(8) })
            val number = FinnestUi.label(context, (index + 1).toString(), 11f, 700, FinnestUi.INK).apply { gravity = Gravity.CENTER; minWidth = dp(12) }
            row.addView(number, LayoutParams(-2, -2)); numbers[step.id] = number
            row.addView(ImageView(context).apply {
                setImageResource(step.icon)
                imageTintList = ColorStateList.valueOf(if (active) android.graphics.Color.WHITE else FinnestUi.INK)
                setPadding(dp(5), dp(5), dp(5), dp(5))
                if (active) background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(FinnestUi.RED) }
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }, LayoutParams(dp(26), dp(26)).apply { marginStart = dp(7); marginEnd = dp(7) })
            row.addView(FinnestUi.label(context, step.title, 12f, 600, if (active) FinnestUi.RED_DARK else FinnestUi.INK).apply { setSingleLine(true) }, LayoutParams(-2, -2))
            rows[step.id] = row
        }
        addView(horizontal, LayoutParams(-1, -2))
        addView(View(context).apply { setBackgroundColor(FinnestUi.LINE) }, LayoutParams(-1, dp(1)))
        current?.let { id -> horizontal.post { rows[id]?.let { horizontal.scrollTo((it.left - dp(16)).coerceAtLeast(0), 0) } } }
    }
    private val savedListener: (String) -> Unit = { changed -> if (changed == scope) main.post { refresh() } }
    override fun onAttachedToWindow() { super.onAttachedToWindow(); DraftRepository.observeSaved(savedListener); refresh() }
    override fun onDetachedFromWindow() { DraftRepository.removeSavedObserver(savedListener); generation++; super.onDetachedFromWindow() }
    fun refresh() {
        val requested = ++generation
        DraftRepository.loadSaved(context, scope) { result -> main.post {
            if (!isAttachedToWindow || requested != generation) return@post
            result.onSuccess { draft ->
                onProgress?.invoke(JourneyProgress.total(scope, draft))
                steps.forEachIndexed { index, step ->
                    val count = JourneyProgress.count(scope, step.id, draft)
                    val started = count.total > 0 && count.recorded > 0
                    // A tick marks a step with recorded entries; it is not a verification or approval.
                    numbers[step.id]?.apply { text = if (started) "✓" else (index + 1).toString(); setTextColor(if (started) 0xFF2E9A7C.toInt() else FinnestUi.INK) }
                    val state = if (count.total == 0) "" else ". ${count.recorded} of ${count.total} entries recorded"
                    rows[step.id]?.contentDescription = "${step.title}$state${if (step.id == current) ". Current step" else ". Open step"}"
                }
            }
        } }
    }
    private fun open(id: String) {
        val destination = if (scope == "resale_property") when (id) {
            "requirements" -> Intent(context, ResaleRequirementsActivity::class.java)
            "summary" -> Intent(context, ResaleSummaryActivity::class.java)
            else -> Intent(context, ResaleConditionActivity::class.java).putExtra("resale_section", id)
        } else JourneyNavigation.intent(context, id)
        if (destination == null) return
        DraftRepository.afterPending(scope) { saved -> main.post {
            val activity = context as? Activity ?: return@post
            if (activity.isFinishing || activity.isDestroyed) return@post
            if (saved) activity.startActivity(destination)
            else androidx.appcompat.app.AlertDialog.Builder(context).setTitle("Your changes could not be saved")
                .setMessage("Check device storage and save your changes before changing steps.").setPositiveButton("Keep editing", null).show()
        } }
    }
}