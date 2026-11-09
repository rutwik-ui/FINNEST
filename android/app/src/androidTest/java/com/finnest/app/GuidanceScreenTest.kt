package com.finnest.app

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.Matchers.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class GuidanceScreenTest {
    @Test fun displaysPagedContentAndSwitchesCategory() {
        val original = GuidanceActivity.sourceFactory
        val calls = AtomicInteger()
        GuidanceActivity.sourceFactory = { object : GuidanceSource {
            override fun load(category: String, cursor: GuidanceCursor?): GuidancePage {
                calls.incrementAndGet()
                return when {
                    category == "resale_property" -> GuidancePage(listOf(GuidanceItem("3", "Resale guidance", "Local advice", category, "")), null)
                    cursor != null -> GuidancePage(listOf(GuidanceItem("2", "Older guidance", "More advice", "general", "")), null)
                    else -> GuidancePage(listOf(GuidanceItem("1", "First guidance", "Plain text", "general", "")), this@GuidanceScreenTest.cursorForTest())
                }
            }
        } }
        try {
            ActivityScenario.launch(GuidanceActivity::class.java).use {
                // Async requests use a plain executor; wait for UI state without assuming Espresso knows about it.
                waitForText("First guidance")
                onView(withText("Load more guidance")).perform(scrollTo(), click())
                waitForText("Older guidance")
                onView(withContentDescription("Guidance category")).perform(scrollTo(), click())
                onData(allOf(`is`(instanceOf(String::class.java)), `is`("Resale property"))).perform(click())
                waitForText("Resale guidance")
                onView(withText("Resale guidance")).perform(scrollTo()).check(matches(isDisplayed()))
                assert(calls.get() >= 3)
            }
        } finally { GuidanceActivity.sourceFactory = original }
    }
    private fun cursorForTest() = GuidanceCursor("00000000-0000-4000-8000-000000000001", "2026-01-01T00:00:00Z")
    private fun waitForText(text: String) {
        val deadline = android.os.SystemClock.elapsedRealtime() + 15000
        var last: Throwable? = null
        while (android.os.SystemClock.elapsedRealtime() < deadline) {
            try { onView(withText(text)).check(matches(withEffectiveVisibility(Visibility.VISIBLE))); return }
            catch (e: androidx.test.espresso.NoMatchingViewException) { last = e }
            android.os.SystemClock.sleep(100)
        }
        throw AssertionError("Timed out waiting for $text", last)
    }
}
