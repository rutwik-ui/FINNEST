package com.finnest.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.core.content.res.ResourcesCompat

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
    private val status: TextView
    private val progress: ProgressBar
    private val labels = mutableMapOf<String, TextView>()
    private val rows = mutableMapOf<String, LinearLayout>()
    private val horizontal: HorizontalScrollView
    private var generation = 0
    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    private fun caption(value: String, size: Float, color: Int, bold: Boolean = false) = TextView(context).apply {
        text = value; textSize = size; setTextColor(context.getColor(color)); letterSpacing = 0f
        typeface = Typeface.create(ResourcesCompat.getFont(context, R.font.plus_jakarta), if (bold) Typeface.BOLD else Typeface.NORMAL)
    }
    init {
        orientation = VERTICAL; setBackgroundColor(context.getColor(R.color.surface))
        addView(ImageView(context).apply {
            setImageResource(R.drawable.parkside_community); scaleType = ImageView.ScaleType.CENTER_CROP
            contentDescription = "A couple walking through a green residential community"
        }, LayoutParams(-1, dp(200)))
        horizontal = HorizontalScrollView(context).apply { isHorizontalScrollBarEnabled = false; isFillViewport = false }
        val strip = LinearLayout(context).apply { orientation = HORIZONTAL; setPadding(dp(12), dp(12), dp(12), dp(8)) }
        horizontal.addView(strip)
        steps.forEachIndexed { index, step ->
            val active = step.id == current
            val row = LinearLayout(context).apply {
                orientation = VERTICAL; gravity = Gravity.CENTER_HORIZONTAL; setPadding(dp(6), dp(6), dp(6), dp(6))
                isClickable = true; isFocusable = true
                background = GradientDrawable().apply { cornerRadius = dp(8).toFloat(); setColor(context.getColor(if (active) R.color.blush else R.color.surface)) }
                setOnClickListener { if (!active) open(step.id) }
            }
            strip.addView(row, LayoutParams(dp(112), -2).apply { marginEnd = dp(6) })
            row.addView(caption(String.format(java.util.Locale.ROOT, "%02d", index + 1), 10f, R.color.muted, true))
            row.addView(ImageView(context).apply {
                setImageResource(step.icon); imageTintList = ColorStateList.valueOf(context.getColor(if (active) R.color.surface else R.color.red))
                setPadding(dp(12), dp(12), dp(12), dp(12))
                background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(context.getColor(if (active) R.color.red else R.color.blush)) }
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }, LayoutParams(dp(48), dp(48)).apply { topMargin = dp(6) })
            row.addView(caption(step.title, 12f, if (active) R.color.red else R.color.navy, true).apply { gravity = Gravity.CENTER; minHeight = dp(50); setPadding(0, dp(6), 0, 0) }, LayoutParams(-1, -2))
            val label = caption("Loading…", 10f, R.color.muted).apply { gravity = Gravity.CENTER }
            row.addView(label); labels[step.id] = label; rows[step.id] = row
        }
        addView(horizontal, LayoutParams(-1, -2))
        status = caption("Loading saved form progress…", 12f, R.color.navy, true).apply { setPadding(dp(16), dp(10), dp(16), dp(6)); contentDescription = "Journey recorded progress" }
        addView(status)
        progress = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100; progressTintList = ColorStateList.valueOf(context.getColor(R.color.red)); progressBackgroundTintList = ColorStateList.valueOf(context.getColor(R.color.line))
        }
        addView(progress, LayoutParams(-1, dp(6)).apply { marginStart = dp(16); marginEnd = dp(16) })
        addView(caption("Saved on this device · Entries are not property verification", 11f, R.color.muted).apply { setPadding(dp(16), dp(6), dp(16), dp(12)) })
        current?.let { id -> horizontal.post { rows[id]?.let { horizontal.scrollTo((it.left - dp(16)).coerceAtLeast(0), 0) } } }
    }
    private val savedListener: (String) -> Unit = { changed -> if (changed == scope) main.post { refresh() } }
    override fun onAttachedToWindow() { super.onAttachedToWindow(); DraftRepository.observeSaved(savedListener); refresh() }
    override fun onDetachedFromWindow() { DraftRepository.removeSavedObserver(savedListener); generation++; super.onDetachedFromWindow() }
    fun refresh() {
        val requested = ++generation
        DraftRepository.loadSaved(context, scope) { result -> main.post {
            if (!isAttachedToWindow || requested != generation) return@post
            result.fold(onSuccess = { draft ->
                val total = JourneyProgress.total(scope, draft)
                status.text = "${total.recorded} of ${total.total} entries recorded · ${total.percent}%"
                progress.progress = total.percent
                steps.forEach { step ->
                    val count = JourneyProgress.count(scope, step.id, draft)
                    val text = if (count.total == 0) "Summary" else "${count.recorded}/${count.total} recorded"
                    labels[step.id]?.text = text
                    rows[step.id]?.contentDescription = "${step.title}. $text${if (step.id == current) ". Current step" else ". Open step"}"
                }
            }, onFailure = { status.text = "Saved progress unavailable"; progress.progress = 0; labels.values.forEach { it.text = "Unavailable" } })
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