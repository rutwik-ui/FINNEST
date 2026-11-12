package com.finnest.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.button.MaterialButton

/** Web-measured presentation only; existing form IDs, navigation and Room saves remain intact. */
object FinnestUi {
    private val fonts = mutableMapOf<Int, Typeface>()
    private fun dp(context: Context, n: Int) = (n * context.resources.displayMetrics.density).toInt()
    fun font(context: Context, weight: Int): Typeface = fonts.getOrPut(weight) {
        val base = ResourcesCompat.getFont(context, R.font.plus_jakarta) ?: Typeface.DEFAULT
        if (Build.VERSION.SDK_INT >= 28) Typeface.create(base, weight, false)
        else Typeface.create(base, if (weight >= 600) Typeface.BOLD else Typeface.NORMAL)
    }
    fun brand(context: Context): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; contentDescription = "FINNEST"
        fun word(value: String) = TextView(context).apply {
            text = value; textSize = 18f; letterSpacing = 0f; includeFontPadding = false
            setTextColor(context.getColor(R.color.navy)); typeface = font(context, 800)
        }
        addView(word("F")); addView(ImageView(context).apply {
            setImageResource(R.drawable.journey_home)
            imageTintList = ColorStateList.valueOf(context.getColor(R.color.red))
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, LinearLayout.LayoutParams(dp(context, 22), dp(context, 22)))
        addView(word("NNEST"))
    }
    private fun shape(context: Context, fill: Int, stroke: Int, radius: Int) = GradientDrawable().apply {
        cornerRadius = dp(context, radius).toFloat(); setColor(context.getColor(fill))
        setStroke(dp(context, 1), context.getColor(stroke))
    }
    private fun text(view: TextView) {
        view.letterSpacing = 0f
        val bold = view.typeface?.isBold == true
        val size = view.textSize / view.resources.displayMetrics.scaledDensity
        view.typeface = font(view.context, if (size >= 28) 800 else if (bold) 700 else 400)
        if (view !is EditText) view.includeFontPadding = false
        if (view !is Button && view !is EditText) view.setLineSpacing(0f, if (size >= 28) 1.1f else 1.5f)
        if (view is Button) {
            view.isAllCaps = false; view.textSize = 14f
            view.typeface = font(view.context, 700)
            view.minHeight = dp(view.context, 48); view.minimumHeight = dp(view.context, 48)
            view.setPadding(dp(view.context, 16), dp(view.context, 12), dp(view.context, 16), dp(view.context, 12))
            if (view is MaterialButton) { view.cornerRadius = dp(view.context, 10); view.insetTop = 0; view.insetBottom = 0 }
        }
        if (view is EditText) {
            view.textSize = 16f; view.setTextColor(view.context.getColor(R.color.navy))
            view.setHintTextColor(view.context.getColor(R.color.muted))
            view.background = shape(view.context, R.color.surface, R.color.line, 10)
            view.setPadding(dp(view.context, 12), dp(view.context, 12), dp(view.context, 12), dp(view.context, 12))
            view.minHeight = dp(view.context, 48)
        }
    }
    /** Called when dynamic fields are attached, so later-loaded forms receive the same typography. */
    fun styleTree(view: View) {
        if (view is JourneyRoadmapView) return
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) styleTree(view.getChildAt(i))
            view.setOnHierarchyChangeListener(object : ViewGroup.OnHierarchyChangeListener {
                override fun onChildViewAdded(parent: View?, child: View?) { child?.let { styleTree(it) } }
                override fun onChildViewRemoved(parent: View?, child: View?) = Unit
            })
        }
        if (view is TextView) text(view)
    }
    fun header(activity: Activity, header: LinearLayout, scope: String) {
        header.removeAllViews(); header.gravity = Gravity.CENTER_VERTICAL
        header.setPadding(dp(activity, 16), 0, dp(activity, 16), 0)
        header.setBackgroundColor(activity.getColor(R.color.surface))
        header.layoutParams.height = dp(activity, 60)
        header.addView(brand(activity), LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(TextView(activity).apply {
            text = if (scope == "resale_property") "Resale Property" else "New Property"
            textSize = 12f; typeface = font(activity, 700); setTextColor(activity.getColor(R.color.red_dark))
            gravity = Gravity.CENTER; includeFontPadding = false
            background = shape(activity, R.color.blush, R.color.red_line, 30)
            setPadding(dp(activity, 12), dp(activity, 7), dp(activity, 12), dp(activity, 7))
            isClickable = true; isFocusable = true; contentDescription = "Change journey"
            setOnClickListener {
                DraftRepository.afterPending(scope) { saved -> activity.runOnUiThread {
                    if (activity.isFinishing || activity.isDestroyed) return@runOnUiThread
                    if (saved) activity.startActivity(Intent(activity, MainActivity::class.java).putExtra("change_journey", true).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                    else androidx.appcompat.app.AlertDialog.Builder(activity).setTitle("Your changes could not be saved").setMessage("Keep editing and retry saving before changing journeys.").setPositiveButton("Keep editing", null).show()
                } }
            }
        }, LinearLayout.LayoutParams(-2, -2))
        val menu = ImageButton(activity).apply {
            setImageResource(R.drawable.journey_menu); imageTintList = ColorStateList.valueOf(activity.getColor(R.color.navy))
            background = shape(activity, R.color.surface, R.color.line, 11)
            contentDescription = "Open menu"; setPadding(dp(activity, 8), dp(activity, 8), dp(activity, 8), dp(activity, 8))
        }
        header.addView(menu, LinearLayout.LayoutParams(dp(activity, 38), dp(activity, 38)).apply { marginStart = dp(activity, 8) })
        menu.setOnClickListener {
            val popup = PopupMenu(activity, menu)
            listOf("Home", "Journey", "Projects", "Experts", "Profile", "Guidance", "Reminders", "About FINNEST", "Privacy & consent").forEachIndexed { i, title -> popup.menu.add(0, i, i, title) }
            popup.setOnMenuItemClickListener { item ->
                val intent = when (item.itemId) {
                    0 -> Intent(activity, if (scope == "resale_property") ResaleOverviewActivity::class.java else DashboardActivity::class.java)
                    1 -> Intent(activity, if (scope == "resale_property") ResaleOverviewActivity::class.java else RequirementsActivity::class.java)
                    2 -> if (scope == "resale_property") Intent(activity, ResaleOverviewActivity::class.java) else Intent(activity, ProjectsActivity::class.java)
                    3 -> Intent(activity, CommonActivity::class.java).putExtra("screen", "experts").putExtra("scope", scope)
                    4 -> Intent(activity, ProfileSettingsActivity::class.java)
                    5 -> Intent(activity, GuidanceActivity::class.java)
                    6 -> Intent(activity, RemindersActivity::class.java)
                    7 -> Intent(activity, CommonActivity::class.java).putExtra("screen", "about").putExtra("scope", scope)
                    else -> Intent(activity, PrivacyInfoActivity::class.java)
                }
                DraftRepository.afterPending(scope) { saved -> activity.runOnUiThread {
                    if (!activity.isFinishing && !activity.isDestroyed) {
                        if (saved) activity.startActivity(intent)
                        else androidx.appcompat.app.AlertDialog.Builder(activity).setTitle("Your changes could not be saved").setMessage("Keep editing and retry saving before leaving.").setPositiveButton("Keep editing", null).show()
                    }
                } }
                true
            }
            popup.show()
        }
    }
    fun decorate(activity: Activity, scope: String, current: String? = null, roadmap: Boolean = true) {
        val root = activity.findViewById<ViewGroup>(android.R.id.content).getChildAt(0) as? LinearLayout ?: return
        styleTree(root)
        val oldHeader = root.getChildAt(0) as? LinearLayout
        val home = activity.findViewById<TextView>(R.id.save_later)
        val back = activity.findViewById<View>(R.id.back)
        if (oldHeader != null) {
            if (home != null) oldHeader.removeView(home)
            if (back != null) { oldHeader.removeView(back); back.visibility = View.GONE; root.addView(back, LinearLayout.LayoutParams(0, 0)) }
            header(activity, oldHeader, scope)
        }
        val scroll = (0 until root.childCount).map { root.getChildAt(it) }.filterIsInstance<ScrollView>().firstOrNull() ?: return
        val body = scroll.getChildAt(0) as? LinearLayout ?: return
        body.setPadding(dp(activity, 16), dp(activity, 24), dp(activity, 16), dp(activity, 24))
        // Keep every existing action, but move it into the scrollable page rather than a cramped fixed bar.
        val footer = (0 until root.childCount).map { root.getChildAt(it) }.filterIsInstance<LinearLayout>().lastOrNull { it !== oldHeader && it.getChildCount() > 0 && it.findViewById<View>(R.id.back_bottom) != null }
        if (footer != null) {
            root.removeView(footer); footer.orientation = LinearLayout.VERTICAL
            footer.setBackgroundColor(activity.getColor(R.color.cream)); footer.setPadding(0, dp(activity, 20), 0, 0)
            for (i in 0 until footer.childCount) {
                val child = footer.getChildAt(i)
                if (child is Space) child.visibility = View.GONE
                else child.layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(activity, 10) }
            }
            if (home != null) footer.addView(home, LinearLayout.LayoutParams(-1, dp(activity, 48)))
            body.addView(footer)
        }
        val title = activity.findViewById<TextView>(R.id.screen_title)
        title?.let { it.textSize = 34f; it.typeface = font(activity, 800); it.setLineSpacing(0f, 1.1f) }
        activity.findViewById<TextView>(R.id.screen_description)?.let { it.textSize = 14f; it.setLineSpacing(0f, 1.6f) }
        activity.findViewById<TextView>(R.id.step_label)?.let { it.textSize = 11f; it.typeface = font(activity, 700) }
        activity.findViewById<TextView>(R.id.screen_notice)?.let {
            it.background = shape(activity, R.color.blush, R.color.red_line, 12)
            it.textSize = 13f; it.setTextColor(activity.getColor(R.color.ink_secondary)); it.setLineSpacing(0f, 1.6f)
        }
        if (roadmap) body.addView(JourneyRoadmapView(activity, scope, current), 0, LinearLayout.LayoutParams(-1, -2).apply {
            leftMargin = -body.paddingLeft; rightMargin = -body.paddingRight; topMargin = -body.paddingTop; bottomMargin = dp(activity, 24)
        })
    }
}
