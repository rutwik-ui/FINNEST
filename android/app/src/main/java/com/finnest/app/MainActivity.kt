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

/** Shared introduction followed by account access and journey selection. */
class EntryViewModel : ViewModel() { var page = 0; var selected: String? = null }

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var model: EntryViewModel
    private val prefs by lazy { getSharedPreferences("entry", MODE_PRIVATE) }
    private val settings by lazy { PublicAppSettings.cached(this) }
    private val choicePage get() = settings.slides.size

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val header = binding.root.getChildAt(0) as android.widget.LinearLayout
        header.removeViewAt(1)
        header.addView(FinnestUi.brand(this))
        header.background = android.graphics.drawable.LayerDrawable(arrayOf(android.graphics.drawable.ColorDrawable(FinnestUi.LINE), android.graphics.drawable.ColorDrawable(android.graphics.Color.WHITE))).apply { setLayerInset(1, 0, 0, 0, FinnestUi.dp(this@MainActivity, 1)) }
        binding.illustration.background = FinnestUi.shape(this, FinnestUi.BLUSH, null, 8)
        binding.illustration.clipToOutline = true
        FinnestUi.styleTree(binding.root)
        val body = binding.illustration.parent as android.widget.LinearLayout
        body.setPadding(FinnestUi.dp(this,20),FinnestUi.dp(this,24),FinnestUi.dp(this,20),FinnestUi.dp(this,24))
        body.removeView(binding.illustration);body.addView(binding.illustration,0)
        (binding.illustration.layoutParams as android.widget.LinearLayout.LayoutParams).apply { height=FinnestUi.dp(this@MainActivity,220);topMargin=0;bottomMargin=FinnestUi.dp(this@MainActivity,24) }
        binding.title.textSize=30f;binding.title.letterSpacing=0f;FinnestUi.spacing(binding.title,1.2f)
        binding.description.textSize=15f;FinnestUi.spacing(binding.description,1.6f)
        body.removeView(binding.secondary);header.addView(android.widget.Space(this),android.widget.LinearLayout.LayoutParams(0,1,1f))
        header.addView(binding.secondary,android.widget.LinearLayout.LayoutParams(-2,FinnestUi.dp(this,44)))
        binding.back.visibility = View.GONE
        model = ViewModelProvider(this)[EntryViewModel::class.java]
        if (savedInstanceState == null && !intent.getBooleanExtra("replay_onboarding",false) && (PublicAppSettings.completedVersion(this)>=settings.version || AccountClient.userId(this)!=null)) model.page = choicePage
        model.selected = model.selected ?: prefs.getString("journey", null)
        if (savedInstanceState == null && !intent.getBooleanExtra("change_journey", false) && !intent.getBooleanExtra("replay_onboarding",false) && model.page==choicePage) {
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
            if (model.page < choicePage - 1) model.page++ else if (model.page == choicePage - 1) finishIntro()
            else if (model.page == choicePage && model.selected != null) {
                prefs.edit().putString("journey", model.selected).apply()
                // Share the choice with the website when signed in (guests keep it on this phone).
                if (AccountClient.userId(this) != null) model.selected?.let { DraftRepository.save(this, "new_property", "selected_journey", it) }
                if (model.selected == "New Property") DashboardActivity.show(this, "new_property") else DashboardActivity.show(this, "resale_property")
            }
            render()
        }
        binding.secondary.setOnClickListener { if (model.page < choicePage) { finishIntro(); render() } }
        var touchX=0f
        binding.illustration.setOnTouchListener { _,event ->
            if(event.action==android.view.MotionEvent.ACTION_DOWN)touchX=event.x
            if(event.action==android.view.MotionEvent.ACTION_UP && kotlin.math.abs(event.x-touchX)>60 && model.page<choicePage){model.page=(model.page+if(event.x<touchX)1 else -1).coerceIn(0,choicePage-1);render()}
            true
        }
        render()
    }
    private fun finishIntro() {
        model.page = choicePage
        if(AccountClient.userId(this)==null) {
            startActivity(Intent(this,AccountActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK));finish()
        } else { PublicAppSettings.complete(this,false); DashboardActivity.show(this,if(model.selected=="Resale Property")"resale_property" else "new_property");finish() }
    }
    private fun goBack() { if (model.page in 1 until choicePage) { model.page--; render() } else finish() }
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
        illustration.isVisible = model.page < choicePage
        illustration.scaleType=android.widget.ImageView.ScaleType.CENTER_CROP
        progress.isVisible = false; caption.isVisible = false
        secondary.isVisible = model.page < choicePage
        primary.isEnabled = model.page != choicePage || model.selected != null
        primary.text = if (model.page == choicePage) if (model.selected == null) "Choose a journey to continue" else "Continue with ${model.selected}" else if(model.page==choicePage-1)"Get Started" else "Continue"
        secondary.text = "Skip"
        if(model.page<choicePage) {
            val slide=settings.slides[model.page];eyebrow.text="YOUR HOME-BUYING JOURNEY";title.text=slide.title;description.text=slide.body
            illustration.setImageResource(when(slide.image){"building"->R.drawable.onboarding_choose;"family"->R.drawable.onboarding_check;else->R.drawable.onboarding_steps})
            val dots=android.widget.LinearLayout(this@MainActivity).apply{gravity=android.view.Gravity.CENTER}
            settings.slides.forEachIndexed { i,_ -> dots.addView(View(this@MainActivity).apply{background=FinnestUi.shape(context,if(i==model.page)FinnestUi.RED else FinnestUi.LINE,null,4)},android.widget.LinearLayout.LayoutParams(FinnestUi.dp(this@MainActivity,if(i==model.page)28 else 8),FinnestUi.dp(this@MainActivity,8)).apply{marginStart=FinnestUi.dp(this@MainActivity,4);marginEnd=FinnestUi.dp(this@MainActivity,4)}) }
            details.addView(dots,lp(8))
        } else { eyebrow.text = "CHOOSE YOUR JOURNEY"; title.text = "What kind of property journey are you exploring?"; description.text = "Choose one path. Each keeps its own checklist and progress."; choice("New Property", "Plan steps to explore: project and developer checks, visits, loans, and due diligence.", model.selected == "New Property") { model.selected = "New Property"; render() }; choice("Resale Property", "Areas to review: property condition, seller ownership, society records, dues, legal review, and handover.", model.selected == "Resale Property") { model.selected = "Resale Property"; render() } }
        progress.progress = model.page + 1
    }
}
