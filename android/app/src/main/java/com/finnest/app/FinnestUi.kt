package com.finnest.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.UnderlineSpan
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.*
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.button.MaterialButton

/**
 * Presentation measured from the FINNEST mobile website (412px viewport): header, step chips,
 * cards, type scale and bottom tabs. Existing form IDs, navigation targets and Room saves remain intact.
 */
object FinnestUi {
    private val fonts = mutableMapOf<Int, Typeface>()
    const val RED = 0xFFE7373E.toInt(); const val RED_TEXT = 0xFFDB343B.toInt(); const val RED_DARK = 0xFFC92A31.toInt()
    const val NAVY = 0xFF141C28.toInt(); const val INK = 0xFF3A4452.toInt(); const val LABEL = 0xFF293341.toInt()
    const val MUTED = 0xFF6B7480.toInt(); const val SOFT = 0xFF858D98.toInt(); const val FAINT = 0xFF93999F.toInt()
    const val LINE = 0xFFE9E6E1.toInt(); const val BLUSH = 0xFFFFF3F2.toInt(); const val PILL_LINE = 0xFFF5D9D8.toInt()
    const val NOTICE_LINE = 0xFFF1C9C7.toInt(); const val NOTICE_BG = 0xFFFFF4F3.toInt(); const val CREAM = 0xFFF7F6F2.toInt()
    const val STAT_BG = 0xFFF7F6F2.toInt(); const val OUTLINE_LINE = 0xFFE8CCCC.toInt()

    fun dp(context: Context, n: Int) = (n * context.resources.displayMetrics.density).toInt()
    fun dpf(context: Context, n: Float) = n * context.resources.displayMetrics.density

    /** Static Plus Jakarta Sans instances (400/600/700/800), identical to the website weights. */
    fun font(context: Context, weight: Int): Typeface {
        val w = when { weight >= 800 -> 800; weight >= 700 -> 700; weight >= 550 -> 600; else -> 400 }
        return fonts.getOrPut(w) {
            val res = when (w) { 800 -> R.font.jakarta_extrabold; 700 -> R.font.jakarta_bold; 600 -> R.font.jakarta_semibold; else -> R.font.jakarta_regular }
            ResourcesCompat.getFont(context, res) ?: Typeface.DEFAULT
        }
    }
    private fun weightOf(view: TextView): Int {
        val tf = view.typeface ?: return 400
        if (android.os.Build.VERSION.SDK_INT >= 28 && tf.weight >= 600) return tf.weight
        return if (tf.isBold) 700 else 400
    }
    private fun markStyled(view: View) = view.setTag(R.id.finnest_styled, true)
    private fun isStyled(view: View) = view.getTag(R.id.finnest_styled) == true

    /** A text view styled to a website token and excluded from later automatic restyling. */
    fun label(context: Context, value: CharSequence, sizeSp: Float, weight: Int, color: Int, spacingEm: Float = 0f, lineHeightSp: Float = 0f) = TextView(context).apply {
        text = value; setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp); typeface = font(context, weight); setTextColor(color)
        letterSpacing = spacingEm; includeFontPadding = false
        if (lineHeightSp > 0) setLineSpacing(0f, lineHeightSp / sizeSp)
        markStyled(this)
    }
    fun eyebrow(context: Context, value: String) = label(context, value.uppercase(), 11f, 700, RED_TEXT, 0.14f)
    fun h1(context: Context, value: String) = label(context, redDot(value), 34f, 800, NAVY, -0.025f, 37.4f)
    fun h2(context: Context, value: String, size: Float = 19f) = label(context, value, size, 700, NAVY, -0.025f, size * 1.25f)
    fun body(context: Context, value: String, size: Float = 14f, color: Int = MUTED) = label(context, value, size, 400, color, 0f, size * 1.6f)
    fun redDot(value: String): CharSequence {
        val base = value.trimEnd().removeSuffix(".")
        if (base.isEmpty() || !base.last().isLetterOrDigit()) return value
        return SpannableStringBuilder(base).append(".").apply { setSpan(ForegroundColorSpan(0xFFE3383E.toInt()), length - 1, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) }
    }
    /** Red underlined link with a trailing arrow, as `.text-link` on the website. */
    fun link(context: Context, value: String, action: () -> Unit) = label(context, SpannableString(value).apply { setSpan(UnderlineSpan(), 0, length, 0) }, 14f, 700, 0xFFDB373D.toInt()).apply {
        val arrow = context.getDrawable(R.drawable.journey_arrow)?.mutate()?.apply { setTint(0xFFDB373D.toInt()); setBounds(0, 0, dp(context, 16), dp(context, 16)) }
        setCompoundDrawables(null, null, arrow, null); compoundDrawablePadding = dp(context, 7)
        gravity = Gravity.CENTER_VERTICAL; minHeight = dp(context, 32)
        isClickable = true; isFocusable = true; setOnClickListener { action() }
        background = ripple(context)
    }
    private fun ripple(context: Context) = TypedValue().let { tv ->
        context.theme.resolveAttribute(android.R.attr.selectableItemBackground, tv, true); context.getDrawable(tv.resourceId)
    }
    fun shape(context: Context, fill: Int, stroke: Int?, radius: Int) = GradientDrawable().apply {
        cornerRadius = dpf(context, radius.toFloat()); setColor(fill)
        if (stroke != null) setStroke(dp(context, 1), stroke)
    }
    /** `.card`: white, 1px warm line, 16px radius, 18px padding and a soft shadow. */
    fun card(context: Context, padding: Int = 18) = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL; background = shape(context, Color.WHITE, LINE, 16)
        setPadding(dp(context, padding), dp(context, padding), dp(context, padding), dp(context, padding))
        elevation = dpf(context, 1.5f); outlineSpotShadowColorCompat(this); clipToOutline = true
    }
    private fun outlineSpotShadowColorCompat(view: View) {
        if (android.os.Build.VERSION.SDK_INT >= 28) { view.outlineSpotShadowColor = 0x33141C28; view.outlineAmbientShadowColor = 0x22141C28 }
    }
    fun primaryButton(context: Context, value: String, action: () -> Unit) = MaterialButton(context).apply {
        text = value; setOnClickListener { action() }; stylePrimary(this); markStyled(this)
        icon = context.getDrawable(R.drawable.journey_arrow); iconGravity = MaterialButton.ICON_GRAVITY_TEXT_END
        iconTint = ColorStateList.valueOf(Color.WHITE); iconSize = dp(context, 20); iconPadding = dp(context, 12)
    }
    private fun stylePrimary(view: MaterialButton) {
        val c = view.context
        view.isAllCaps = false; view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f); view.typeface = font(c, 700); view.letterSpacing = 0.005f
        view.backgroundTintList = ColorStateList(arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf()), intArrayOf(0xFFF3A6A9.toInt(), RED))
        view.setTextColor(Color.WHITE); view.cornerRadius = dp(c, 10); view.insetTop = 0; view.insetBottom = 0
        view.strokeWidth = 0; view.minHeight = dp(c, 50); view.minimumHeight = dp(c, 50)
        view.setPadding(dp(c, 20), dp(c, 13), dp(c, 20), dp(c, 13)); view.stateListAnimator = null; view.elevation = dpf(c, 2f)
    }
    private fun styleOutline(view: MaterialButton) {
        val c = view.context
        view.isAllCaps = false; view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f); view.typeface = font(c, 700); view.letterSpacing = 0f
        view.backgroundTintList = ColorStateList.valueOf(Color.WHITE); view.setTextColor(0xFFD4343B.toInt())
        view.strokeColor = ColorStateList.valueOf(OUTLINE_LINE); view.strokeWidth = dp(c, 1)
        view.cornerRadius = dp(c, 10); view.insetTop = 0; view.insetBottom = 0; view.minHeight = dp(c, 48); view.minimumHeight = dp(c, 48)
        view.setPadding(dp(c, 17), dp(c, 12), dp(c, 17), dp(c, 12)); view.stateListAnimator = null; view.elevation = 0f
        view.rippleColor = ColorStateList.valueOf(0x22E7373E)
    }
    /** Plain buttons created in code become website list rows instead of white-on-white boxes. */
    private fun styleRow(view: Button) {
        val c = view.context
        view.isAllCaps = false; view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f); view.typeface = font(c, 600); view.letterSpacing = 0f
        view.setTextColor(ColorStateList(arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf()), intArrayOf(0xFFA0A6AC.toInt(), NAVY)))
        view.background = android.graphics.drawable.RippleDrawable(ColorStateList.valueOf(0x14E7373E), shape(c, Color.WHITE, LINE, 12), null)
        view.backgroundTintList = null; view.stateListAnimator = null; view.elevation = 0f
        view.gravity = Gravity.START or Gravity.CENTER_VERTICAL
        view.minHeight = dp(c, 52); view.minimumHeight = dp(c, 52); view.setPadding(dp(c, 16), dp(c, 12), dp(c, 16), dp(c, 12))
        // Keep the exact label (tests and screen readers match it); only the arrow takes the brand red.
        val label = view.text.toString(); val at = label.lastIndexOf('→')
        if (at >= 0) view.text = SpannableString(label).apply { setSpan(ForegroundColorSpan(RED), at, at + 1, 0) }
        (view.layoutParams as? ViewGroup.MarginLayoutParams)?.let { it.topMargin = maxOf(it.topMargin, dp(c, 8)) }
        if (view.layoutParams == null) view.post { (view.layoutParams as? ViewGroup.MarginLayoutParams)?.let { it.topMargin = maxOf(it.topMargin, dp(c, 8)); view.requestLayout() } }
    }
    private fun text(view: TextView) {
        if (isStyled(view)) return
        markStyled(view)
        val c = view.context
        val size = view.textSize / c.resources.displayMetrics.scaledDensity
        val weight = weightOf(view)
        if (view is MaterialButton) {
            if (view.id == R.id.back_bottom || view.strokeWidth > 0) styleOutline(view) else stylePrimary(view)
            return
        }
        if (view is Button) { if (view !is CompoundButton) { styleRow(view); return } }
        if (view is EditText) {
            view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f); view.typeface = font(c, 600); view.letterSpacing = 0f
            view.setTextColor(0xFF273341.toInt()); view.setHintTextColor(0xFFA0A6AC.toInt())
            view.background = shape(c, Color.WHITE, 0xFFE3E2DF.toInt(), 10)
            view.setPadding(dp(c, 13), dp(c, 11), dp(c, 13), dp(c, 11)); view.minHeight = dp(c, 46)
            return
        }
        if (view is CompoundButton) {
            view.typeface = font(c, 400); view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f); view.setTextColor(0xFF4A5664.toInt())
            view.buttonTintList = ColorStateList(arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()), intArrayOf(RED, 0xFFB9BEC4.toInt()))
            view.minHeight = dp(c, 44); return
        }
        view.includeFontPadding = false
        val content = view.text?.toString().orEmpty()
        val caps = content.length > 2 && content.any { it.isLetter() } && content == content.uppercase() && size <= 13
        when {
            caps -> { view.typeface = font(c, 700); view.letterSpacing = 0.14f; view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f); view.setLineSpacing(0f, 1.5f) }
            size >= 28 -> { view.typeface = font(c, 800); view.letterSpacing = -0.025f; view.setLineSpacing(0f, 1.1f) }
            size >= 18 && weight >= 600 -> { view.typeface = font(c, 700); view.letterSpacing = -0.025f; view.setLineSpacing(0f, 1.25f) }
            weight >= 600 -> { view.typeface = font(c, 700); view.letterSpacing = if (size >= 15) -0.02f else 0f; view.setLineSpacing(0f, 1.4f) }
            else -> { view.typeface = font(c, 400); view.letterSpacing = 0f; view.setLineSpacing(0f, 1.6f) }
        }
    }
    private var cardConstant: android.graphics.drawable.Drawable.ConstantState? = null
    private fun cardState(context: Context) = cardConstant ?: context.getDrawable(R.drawable.card)?.constantState.also { cardConstant = it }
    /** Applied to every attached view, including forms that load later. */
    fun styleTree(view: View) {
        if (view is JourneyRoadmapView || view.id == R.id.finnest_bottom_nav) return
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) styleTree(view.getChildAt(i))
            view.setOnHierarchyChangeListener(object : ViewGroup.OnHierarchyChangeListener {
                override fun onChildViewAdded(parent: View?, child: View?) { child?.let { styleTree(it) } }
                override fun onChildViewRemoved(parent: View?, child: View?) = Unit
            })
            // Legacy `card` backgrounds gain the website card shadow and padding once.
            val bg = view.background
            if (!isStyled(view) && bg != null && bg.constantState != null && bg.constantState == cardState(view.context)) {
                markStyled(view); view.elevation = dpf(view.context, 1.5f); outlineSpotShadowColorCompat(view)
                view.setPadding(dp(view.context, 18), dp(view.context, 18), dp(view.context, 18), dp(view.context, 18))
            }
        }
        if (view is TextView) text(view)
    }

    fun brand(context: Context): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; contentDescription = "FINNEST home"
        fun word(value: String) = label(context, value, 18f, 800, 0xFF141E2A.toInt(), 0.16f).apply { setSingleLine(true) }
        addView(word("F"))
        addView(ImageView(context).apply {
            setImageResource(R.drawable.journey_house); imageTintList = ColorStateList.valueOf(RED)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, LinearLayout.LayoutParams(dp(context, 22), dp(context, 22)).apply { marginStart = dp(context, 1); marginEnd = dp(context, 2) })
        addView(word("NNEST"))
    }
    private fun leave(activity: Activity, scope: String, intent: Intent, message: String = "Keep editing and retry saving before leaving.") {
        DraftRepository.afterPending(scope) { saved -> activity.runOnUiThread {
            if (activity.isFinishing || activity.isDestroyed) return@runOnUiThread
            if (saved) { activity.startActivity(intent); activity.overridePendingTransition(0, 0) }
            else androidx.appcompat.app.AlertDialog.Builder(activity).setTitle("Your changes could not be saved").setMessage(message).setPositiveButton("Keep editing", null).show()
        } }
    }
    fun home(activity: Activity, scope: String) = Intent(activity, DashboardActivity::class.java).putExtra(DashboardActivity.SCOPE, scope)
        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    private fun destination(activity: Activity, scope: String, tab: String): Intent {
        val resale = scope == "resale_property"
        return when (tab) {
            "home" -> home(activity, scope)
            "journey" -> Intent(activity, if (resale) ResaleOverviewActivity::class.java else RequirementsActivity::class.java)
            "projects" -> Intent(activity, if (resale) ResaleOverviewActivity::class.java else ProjectsActivity::class.java)
            "experts" -> Intent(activity, CommonActivity::class.java).putExtra("screen", "experts").putExtra("scope", scope)
            "guidance" -> Intent(activity, GuidanceActivity::class.java)
            "reminders" -> Intent(activity, RemindersActivity::class.java)
            "about" -> Intent(activity, CommonActivity::class.java).putExtra("screen", "about").putExtra("scope", scope)
            "privacy" -> Intent(activity, PrivacyInfoActivity::class.java)
            else -> Intent(activity, ProfileSettingsActivity::class.java)
        }.apply { if (tab != "home") addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP) }
    }
    fun header(activity: Activity, header: LinearLayout, scope: String) {
        header.removeAllViews(); header.gravity = Gravity.CENTER_VERTICAL
        header.setPadding(dp(activity, 16), 0, dp(activity, 16), 0)
        header.background = android.graphics.drawable.LayerDrawable(arrayOf(ColorDrawable(LINE), ColorDrawable(Color.WHITE))).apply { setLayerInset(1, 0, 0, 0, dp(activity, 1)) }
        header.layoutParams.height = dp(activity, 60); header.elevation = 0f
        val brand = brand(activity).apply {
            isClickable = true; isFocusable = true; background = ripple(activity)
            setOnClickListener { if (activity !is DashboardActivity) leave(activity, scope, home(activity, scope)) }
        }
        header.addView(brand, LinearLayout.LayoutParams(-2, dp(activity, 44)))
        header.addView(Space(activity), LinearLayout.LayoutParams(0, 1, 1f))
        header.addView(label(activity, if (scope == "resale_property") "Resale Property" else "New Property", 12f, 700, 0xFFD93037.toInt()).apply {
            gravity = Gravity.CENTER; background = shape(activity, BLUSH, PILL_LINE, 30)
            setPadding(dp(activity, 12), dp(activity, 7), dp(activity, 12), dp(activity, 7)); minHeight = dp(activity, 34)
            isClickable = true; isFocusable = true; contentDescription = "Change journey"
            setOnClickListener { leave(activity, scope, Intent(activity, MainActivity::class.java).putExtra("change_journey", true).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP), "Keep editing and retry saving before changing journeys.") }
        }, LinearLayout.LayoutParams(-2, -2))
        val menu = ImageButton(activity).apply {
            setImageResource(R.drawable.journey_menu); imageTintList = ColorStateList.valueOf(0xFF18212D.toInt())
            background = shape(activity, Color.WHITE, LINE, 11); scaleType = ImageView.ScaleType.CENTER_INSIDE
            contentDescription = "Open menu"; setPadding(dp(activity, 8), dp(activity, 8), dp(activity, 8), dp(activity, 8))
        }
        header.addView(menu, LinearLayout.LayoutParams(dp(activity, 38), dp(activity, 38)).apply { marginStart = dp(activity, 10) })
        menu.setOnClickListener { showMenu(activity, header, menu, scope) }
    }
    /** The website's dropdown menu card: icon + label rows under the header. */
    private fun showMenu(activity: Activity, header: View, button: ImageButton, scope: String) {
        val items = listOf(
            Triple("home", "Home", R.drawable.journey_house), Triple("journey", "Journey", R.drawable.journey_sliders),
            Triple("projects", "Projects", R.drawable.journey_doc), Triple("experts", "Experts", R.drawable.journey_help),
            Triple("profile", "Profile", R.drawable.journey_person), Triple("guidance", "Guidance", R.drawable.journey_bookmark),
            Triple("reminders", "Reminders", R.drawable.journey_clock), Triple("about", "About FINNEST", R.drawable.journey_info),
            Triple("privacy", "Privacy & consent", R.drawable.journey_shield))
        val list = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL; background = shape(activity, Color.WHITE, LINE, 16)
            setPadding(dp(activity, 8), dp(activity, 8), dp(activity, 8), dp(activity, 8)); elevation = dpf(activity, 10f)
        }
        val popup = PopupWindow(list, activity.resources.displayMetrics.widthPixels - dp(activity, 24), ViewGroup.LayoutParams.WRAP_CONTENT, true).apply {
            elevation = dpf(activity, 12f); setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT)); isOutsideTouchable = true
        }
        items.forEach { (tab, title, icon) ->
            list.addView(LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(activity, 12), 0, dp(activity, 12), 0); background = ripple(activity)
                isClickable = true; isFocusable = true; contentDescription = title
                addView(ImageView(activity).apply { setImageResource(icon); imageTintList = ColorStateList.valueOf(0xFF263140.toInt()) }, LinearLayout.LayoutParams(dp(activity, 18), dp(activity, 18)))
                addView(label(activity, title, 15f, 500, 0xFF263140.toInt()), LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(activity, 12) })
                setOnClickListener { popup.dismiss(); leave(activity, scope, destination(activity, scope, tab)) }
            }, LinearLayout.LayoutParams(-1, dp(activity, 48)))
        }
        button.setImageResource(R.drawable.journey_close); button.contentDescription = "Close menu"
        popup.setOnDismissListener { button.setImageResource(R.drawable.journey_menu); button.contentDescription = "Open menu" }
        popup.showAsDropDown(header, dp(activity, 12), 0)
    }
    /** The website's fixed bottom tabs: Home, Journey, Projects, Experts and Profile. */
    fun bottomNav(activity: Activity, root: LinearLayout, scope: String, active: String?) {
        root.findViewById<View>(R.id.finnest_bottom_nav)?.let { root.removeView(it) }
        val bar = LinearLayout(activity).apply {
            id = R.id.finnest_bottom_nav; orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(activity, 6), dp(activity, 6), dp(activity, 6), dp(activity, 6))
            background = GradientDrawable().apply {
                setColor(Color.WHITE); val r = dpf(activity, 18f); cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f); setStroke(dp(activity, 1), 0xFFEDECE9.toInt())
            }
            elevation = dpf(activity, 14f)
        }
        listOf(Triple("home", "Home", R.drawable.journey_house), Triple("journey", "Journey", R.drawable.journey_sliders),
            Triple("projects", "Projects", R.drawable.journey_doc), Triple("experts", "Experts", R.drawable.journey_help),
            Triple("profile", "Profile", R.drawable.journey_person)).forEach { (tab, title, icon) ->
            val on = tab == active
            val color = if (on) RED else SOFT
            bar.addView(LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
                background = if (on) shape(activity, BLUSH, null, 12) else android.graphics.drawable.RippleDrawable(ColorStateList.valueOf(0x14E7373E), null, shape(activity, Color.WHITE, null, 12))
                isClickable = true; isFocusable = true; contentDescription = title + if (on) ", current tab" else ""
                isSelected = on
                addView(ImageView(activity).apply { setImageResource(icon); imageTintList = ColorStateList.valueOf(color) }, LinearLayout.LayoutParams(dp(activity, 22), dp(activity, 22)))
                addView(label(activity, title, 11f, if (on) 700 else 600, color).apply { gravity = Gravity.CENTER; setSingleLine(true) }, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(activity, 3) })
                setOnClickListener { if (!on || tab == "journey") leave(activity, scope, destination(activity, scope, tab)) }
            }, LinearLayout.LayoutParams(0, dp(activity, 59), 1f).apply { marginStart = dp(activity, 1); marginEnd = dp(activity, 1) })
        }
        root.addView(bar, LinearLayout.LayoutParams(-1, -2))
    }
    private fun activeTab(activity: Activity): String? = when (activity) {
        is DashboardActivity -> "home"
        is ProjectsActivity -> "projects"
        is ProfileSettingsActivity -> "profile"
        is CommonActivity -> when (activity.intent.getStringExtra("screen")) { "experts", "expert_detail" -> "experts"; else -> null }
        is GuidanceActivity, is RemindersActivity, is PrivacyInfoActivity -> null
        else -> "journey"
    }
    /** Keeps a page title in the website form: 34sp/800 with the brand red full stop. */
    private fun keepRedDot(title: TextView) {
        title.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                if (!title.viewTreeObserver.isAlive || title.getTag(R.id.finnest_bottom_nav) == "form-title") return true
                val current = title.text ?: return true
                val styled = current is Spanned && current.getSpans(0, current.length, ForegroundColorSpan::class.java).isNotEmpty()
                if (styled || current.isEmpty()) return true
                val dotted = redDot(current.toString())
                if (dotted is Spanned) { title.text = dotted; return false }
                return true
            }
        })
    }
    private fun notice(activity: Activity, it: TextView) {
        it.background = shape(activity, NOTICE_BG, NOTICE_LINE, 12); it.elevation = 0f
        it.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f); it.typeface = font(activity, 400); it.setTextColor(0xFF515B68.toInt()); it.setLineSpacing(0f, 1.5f)
        it.setPadding(dp(activity, 15), dp(activity, 15), dp(activity, 15), dp(activity, 15))
        val info = activity.getDrawable(R.drawable.journey_info)?.mutate()?.apply { setTint(RED); setBounds(0, 0, dp(activity, 18), dp(activity, 18)) }
        it.setCompoundDrawablesRelative(info, null, null, null); it.compoundDrawablePadding = dp(activity, 12)
        (it.layoutParams as? ViewGroup.MarginLayoutParams)?.topMargin = dp(activity, 18)
    }
    /**
     * The website's /journey page head ("One step at a time.") and its Requirements/Developer tabs.
     * The step's own title becomes the 19sp form heading beneath it, exactly as on the website.
     */
    private fun journeyHead(activity: Activity, page: LinearLayout, current: String?) {
        val head = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        head.addView(eyebrow(activity, "New Property · Your journey"), LinearLayout.LayoutParams(-2, -2))
        head.addView(h1(activity, "One step at a time"), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(activity, 9) })
        head.addView(body(activity, "Make a plan, keep your notes, and know what to ask next."), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(activity, 9) })
        val tabsScroll = HorizontalScrollView(activity).apply { isHorizontalScrollBarEnabled = false; overScrollMode = View.OVER_SCROLL_NEVER }
        val tabs = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL; background = shape(activity, Color.WHITE, LINE, 16)
            setPadding(dp(activity, 6), dp(activity, 6), dp(activity, 6), dp(activity, 6))
        }
        tabsScroll.addView(tabs)
        listOf(Triple("requirements", "01", "Requirements & Budget"), Triple("developer", "02", "Developer checks")).forEach { (id, n, title) ->
            val on = id == current
            tabs.addView(LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(activity, 12), 0, dp(activity, 12), 0)
                background = if (on) shape(activity, 0xFFFFF0EF.toInt(), null, 9) else ripple(activity)
                isClickable = true; isFocusable = true; contentDescription = "$title tab" + if (on) ", selected" else ""
                addView(label(activity, n, 13f, 600, 0xFF9B9FA4.toInt()), LinearLayout.LayoutParams(-2, -2))
                addView(label(activity, title, 13f, 700, if (on) 0xFFD93239.toInt() else 0xFF52606C.toInt()).apply { setSingleLine(true) }, LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(activity, 10) })
                if (on) addView(ImageView(activity).apply { setImageResource(R.drawable.journey_arrow); imageTintList = ColorStateList.valueOf(0xFFD93239.toInt()); rotation = 0f },
                    LinearLayout.LayoutParams(dp(activity, 14), dp(activity, 14)).apply { marginStart = dp(activity, 12) })
                setOnClickListener { if (!on) leave(activity, "new_property", JourneyNavigation.intent(activity, id)) }
            }, LinearLayout.LayoutParams(-2, dp(activity, 40)))
        }
        head.addView(tabsScroll, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(activity, 21) })
        page.addView(head, 0, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(activity, 24) })
        // Demote the step title to the website's 19sp form heading.
        for (i in 1 until page.childCount) {
            val t = page.getChildAt(i) as? TextView ?: continue
            if (t.textSize / activity.resources.displayMetrics.scaledDensity >= 28) {
                t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19f); t.typeface = font(activity, 700); t.letterSpacing = -0.025f; t.setLineSpacing(0f, 1.25f)
                t.setTag(R.id.finnest_bottom_nav, "form-title")
            }
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
        val scroll = (0 until root.childCount).map { root.getChildAt(it) }.filterIsInstance<ScrollView>().firstOrNull()
        val body = scroll?.getChildAt(0) as? LinearLayout
        if (scroll != null && body != null) {
            scroll.isVerticalScrollBarEnabled = false; scroll.overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
            body.setPadding(dp(activity, 16), dp(activity, 24), dp(activity, 16), dp(activity, 32))
            // Website order on phones: primary action first, then the outline Back action, then a text link.
            val footer = (0 until root.childCount).map { root.getChildAt(it) }.filterIsInstance<LinearLayout>().lastOrNull { it !== oldHeader && it.childCount > 0 && it.findViewById<View>(R.id.back_bottom) != null }
            if (footer != null) {
                root.removeView(footer); footer.orientation = LinearLayout.VERTICAL
                footer.setBackgroundColor(Color.TRANSPARENT); footer.setPadding(0, dp(activity, 12), 0, 0)
                val next = footer.findViewById<View>(R.id.next); val backBottom = footer.findViewById<View>(R.id.back_bottom)
                val others = (0 until footer.childCount).map { footer.getChildAt(it) }.filter { it !== next && it !== backBottom && it !is Space }
                footer.removeAllViews()
                (listOfNotNull(next, backBottom) + others).forEach { footer.addView(it, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(activity, 10) }) }
                if (home != null) {
                    home.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f); home.typeface = font(activity, 700); home.setTextColor(0xFFDB373D.toInt())
                    home.gravity = Gravity.CENTER; home.background = ripple(activity)
                    footer.addView(home, LinearLayout.LayoutParams(-1, dp(activity, 46)))
                }
                body.addView(footer)
            }
            activity.findViewById<TextView>(R.id.screen_title)?.let {
                it.setTextSize(TypedValue.COMPLEX_UNIT_SP, 34f); it.typeface = font(activity, 800); it.letterSpacing = -0.025f
                it.setLineSpacing(0f, 1.1f); it.setTextColor(NAVY); keepRedDot(it)
                (it.layoutParams as? ViewGroup.MarginLayoutParams)?.topMargin = dp(activity, 9)
            }
            activity.findViewById<TextView>(R.id.screen_description)?.let {
                it.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f); it.typeface = font(activity, 400); it.setTextColor(MUTED); it.setLineSpacing(0f, 1.6f)
                (it.layoutParams as? ViewGroup.MarginLayoutParams)?.topMargin = dp(activity, 9)
            }
            activity.findViewById<TextView>(R.id.step_label)?.let { it.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f); it.typeface = font(activity, 700); it.letterSpacing = 0.14f; it.setTextColor(RED_TEXT) }
            activity.findViewById<TextView>(R.id.screen_notice)?.let { notice(activity, it) }
            // Large page titles keep the website's red full stop; notices gain the blush style and info icon.
            for (i in 0 until body.childCount) {
                val child = body.getChildAt(i) as? TextView ?: continue
                if (child.id == R.id.screen_title || child.textSize / activity.resources.displayMetrics.scaledDensity >= 28) keepRedDot(child)
                else if (child.id != R.id.screen_notice && child.background?.constantState != null && child.background.constantState == cardState(activity)) notice(activity, child)
            }
            if (scope == "new_property" && (current == "requirements" || current == "developer") && activity.intent.extras?.keySet()?.none { it in setOf("visit_observations", "resident_feedback", "legal_due_diligence") } != false) journeyHead(activity, body, current)
            if (roadmap) {
                scroll.removeView(body)
                val outer = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
                outer.addView(JourneyRoadmapView(activity, scope, current), LinearLayout.LayoutParams(-1, -2))
                outer.addView(body, LinearLayout.LayoutParams(-1, -2))
                scroll.addView(outer)
            }
        }
        bottomNav(activity, root, scope, activeTab(activity))
    }
}
