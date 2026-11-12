package com.finnest.app

import android.app.Activity
import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.res.ResourcesCompat

object FinnestUi {
    private fun dp(context: Context, n: Int) = (n * context.resources.displayMetrics.density).toInt()
    fun brand(context: Context): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; contentDescription = "FINNEST"
        fun word(value: String) = TextView(context).apply {
            text = value; textSize = 20f; letterSpacing = 0f; setTextColor(context.getColor(R.color.navy))
            typeface = Typeface.create(ResourcesCompat.getFont(context, R.font.plus_jakarta), Typeface.BOLD)
        }
        addView(word("F")); addView(ImageView(context).apply {
            setImageResource(R.drawable.journey_home); imageTintList = android.content.res.ColorStateList.valueOf(context.getColor(R.color.red))
        }, LinearLayout.LayoutParams(dp(context, 28), dp(context, 28)))
        addView(word("NNEST"))
    }
    /** Reuse the existing form views and IDs; no field or save behavior is replaced. */
    fun decorate(activity: Activity, scope: String, current: String? = null) {
        val content = activity.findViewById<ViewGroup>(android.R.id.content)
        val root = content.getChildAt(0) as? LinearLayout ?: return
        val header = root.getChildAt(0) as? LinearLayout
        if (header != null) {
            val oldBrand = (0 until header.childCount).map { header.getChildAt(it) }.filterIsInstance<TextView>().firstOrNull { it.text.toString() == "FINNEST" }
            if (oldBrand != null) { val index = header.indexOfChild(oldBrand); header.removeView(oldBrand); header.addView(brand(activity), index) }
            activity.findViewById<View>(R.id.back)?.visibility = View.GONE
        }
        val scroll = (0 until root.childCount).map { root.getChildAt(it) }.filterIsInstance<ScrollView>().firstOrNull() ?: return
        val body = scroll.getChildAt(0) as? LinearLayout ?: return
        val banner = JourneyRoadmapView(activity, scope, current)
        body.addView(banner, 0, LinearLayout.LayoutParams(-1, -2).apply {
            leftMargin = -body.paddingLeft; rightMargin = -body.paddingRight; topMargin = -body.paddingTop; bottomMargin = dp(activity, 22)
        })
    }
}