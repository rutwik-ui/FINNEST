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
        text = value; textSize = size; includeFontPadding = false; setTextColor(context.getColor(color)); letterSpacing = 0f
        typeface = FinnestUi.font(context, if (bold) 600 else 400)
    }
    init {
        orientation = VERTICAL; setBackgroundColor(context.getColor(R.color.surface))
        addView(ImageView(context).apply {
            setImageResource(R.drawable.parkside_community); scaleType = ImageView.ScaleType.CENTER_CROP
            contentDescription = "A couple walking through a green residential community"
        }, LayoutParams(-1, dp(200)))
        horizontal = HorizontalScrollView(context).apply { isHorizontalScrollBarEnabled = false; isFillViewport = false }
        val strip = LinearLayout(context).apply { orientation = HORIZONTAL; setPadding(dp(16), dp(12), dp(16), dp(12)) }
        horizontal.addView(strip)
        steps.forEachIndexed { index, step ->
            val active = step.id == current
            val row = LinearLayout(context).apply {
                orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(7), dp(4), dp(12), dp(4))
                isClickable = true; isFocusable = true
                background = GradientDrawable().apply {
                    cornerRadius = dp(8).toFloat()
                    setColor(context.getColor(if (active) R.color.blush else R.color.surface))
                    setStroke(dp(1), context.getColor(if (active) R.color.red else R.color.line))
                }
                setOnClickListener { if (!active) open(step.id) }
            }
            val titleWidth = caption(step.title, 12f, R.color.ink_secondary, true).paint.measureText(step.title).toInt()
            strip.addView(row, LayoutParams(titleWidth + dp(73), dp(52)).apply { marginEnd = dp(8) })
            row.addView(caption((index + 1).toString(), 11f, R.color.ink_secondary), LayoutParams(dp(12), -2))
            row.addView(ImageView(context).apply {
                setImageResource(step.icon)
                imageTintList = ColorStateList.valueOf(context.getColor(if (active) R.color.surface else R.color.ink_secondary))
                setPadding(dp(4), dp(4), dp(4), dp(4))
                if (active) background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(context.getColor(R.color.red)) }
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }, LayoutParams(dp(26), dp(26)).apply { marginStart = dp(7); marginEnd = dp(7) })
            val textColumn = LinearLayout(context).apply { orientation = VERTICAL }
            row.addView(textColumn, LayoutParams(-2, -2))
            textColumn.addView(caption(step.title, 12f, if (active) R.color.red_dark else R.color.ink_secondary, true).apply {
                setSingleLine(true); includeFontPadding = false
            })
            val label = caption("Loading…", 10f, R.color.muted).apply { setSingleLine(true); includeFontPadding = false }
            textColumn.addView(label, LayoutParams(-2, -2).apply { topMargin = dp(3) })
            labels[step.id] = label; rows[step.id] = row
        }
        addView(horizontal, LayoutParams(-1, -2))
        status = caption("Loading saved form progress…", 12f, R.color.navy, true).apply { setPadding(dp(16), dp(10), dp(16), dp(6)); contentDescription = "Journey recorded progress" }
        addView(status)
        progress = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            minHeight = 0; max = 100; progressTintList = ColorStateList.valueOf(context.getColor(R.color.red)); progressBackgroundTintList = ColorStateList.valueOf(context.getColor(R.color.line))
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