package com.finnest.app

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class VisualScreensTest {
    private val inst get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = inst.targetContext
    private fun capture(name: String, intent: Intent) {
        ActivityScenario.launch<Activity>(intent).use {
            SystemClock.sleep(1400); inst.waitForIdleSync()
            val image = inst.uiAutomation.takeScreenshot() ?: throw AssertionError("No screen for $name")
            val dir = File(InstrumentationRegistry.getArguments().getString("additionalTestOutputDir") ?: throw AssertionError("Missing test output directory"), "roadmap-screenshots").apply { mkdirs() }
            File(dir, "$name.png").outputStream().use { out -> image.compress(Bitmap.CompressFormat.PNG, 100, out) }
            image.recycle()
            it.onActivity { activity ->
                fun scroll(view: android.view.View) {
                    if (view is android.widget.ScrollView) view.fullScroll(android.view.View.FOCUS_DOWN)
                    if (view is android.widget.HorizontalScrollView) view.fullScroll(android.view.View.FOCUS_RIGHT)
                    if (view is android.view.ViewGroup) (0 until view.childCount).forEach { index -> scroll(view.getChildAt(index)) }
                }
                scroll(activity.findViewById(android.R.id.content))
            }
            inst.waitForIdleSync(); SystemClock.sleep(200)
            val bottom = inst.uiAutomation.takeScreenshot() ?: throw AssertionError("No footer for $name")
            File(dir, "$name-bottom.png").outputStream().use { out -> bottom.compress(Bitmap.CompressFormat.PNG, 100, out) }
            bottom.recycle()
        }
    }
    @Test fun captureAllNativeScreens() {
        val screens = listOf(
            "new-home" to Intent(context, DashboardActivity::class.java),
            "new-requirements" to Intent(context, RequirementsActivity::class.java),
            "new-developer" to Intent(context, DeveloperChecksActivity::class.java),
            "new-projects" to Intent(context, ProjectsActivity::class.java),
            "new-visit" to JourneyNavigation.intent(context, "visit"),
            "new-feedback" to JourneyNavigation.intent(context, "feedback"),
            "new-loan" to Intent(context, LoanGuideActivity::class.java),
            "new-legal" to JourneyNavigation.intent(context, "legal"),
            "new-financial" to Intent(context, FinancialDueDiligenceActivity::class.java),
            "new-review" to Intent(context, FinalReviewActivity::class.java),
            "new-summary" to Intent(context, JourneySummaryActivity::class.java),
            "resale-home" to Intent(context, ResaleOverviewActivity::class.java),
            "resale-requirements" to Intent(context, ResaleRequirementsActivity::class.java),
            "resale-summary" to Intent(context, ResaleSummaryActivity::class.java),
            "profile" to Intent(context, ProfileSettingsActivity::class.java),
            "reminders" to Intent(context, RemindersActivity::class.java),
            "privacy" to Intent(context, PrivacyInfoActivity::class.java),
            "guidance" to Intent(context, GuidanceActivity::class.java)
        ) + ResaleChecklists.sections.map { "resale-${it.id}" to Intent(context, ResaleConditionActivity::class.java).putExtra("resale_section", it.id) } +
            listOf("experts", "expert_detail", "risk", "about").map { "common-$it" to Intent(context, CommonActivity::class.java).putExtra("screen", it) }
        screens.forEach { (name, intent) -> if (intent != null) capture("screen-$name", intent) }
    }
    @Test fun captureNarrowAndLargeTextPhones() {
        fun command(value: String) { inst.uiAutomation.executeShellCommand(value).use { android.os.ParcelFileDescriptor.AutoCloseInputStream(it).readBytes() } }
        try {
            command("wm size 960x1800"); command("wm density 480")
            capture("narrow-resale", Intent(context, ResaleOverviewActivity::class.java))
            command("settings put system font_scale 1.3")
            capture("large-text-resale", Intent(context, ResaleOverviewActivity::class.java))
        } finally { command("settings put system font_scale 1.0"); command("wm size reset"); command("wm density reset") }
    }
}
