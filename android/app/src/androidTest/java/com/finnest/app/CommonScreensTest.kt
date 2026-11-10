package com.finnest.app

import android.content.Intent
import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.hamcrest.Matchers.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class CommonScreensTest {
    @Test fun expertDirectoryFilterAndDisabledContact() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        ActivityScenario.launch<CommonActivity>(Intent(context, CommonActivity::class.java).putExtra("screen", "experts")).use {
            onView(withContentDescription("Expert role category")).perform(scrollTo(), click())
            onData(allOf(`is`(instanceOf(String::class.java)), `is`("Property lawyer"))).perform(click())
            onView(withText("Title, agreements, disclosures and local requirements.")).perform(scrollTo()).check(matches(isDisplayed()))
            onView(withText("View expert detail state")).perform(scrollTo(), click())
            onView(withText("Request a conversation · unavailable")).perform(scrollTo()).check(matches(not(isEnabled())))
        }
    }
    @Test fun selectedRiskStatusIsSavedInChosenJourneyOnly() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val pre = CountDownLatch(1)
        DraftRepository.save(context, "new_property", "developer_track_record_status", "Checked by you")
        DraftRepository.afterPending("new_property") { pre.countDown() }
        assertTrue(pre.await(10, TimeUnit.SECONDS))
        ActivityScenario.launch<CommonActivity>(Intent(context, CommonActivity::class.java).putExtra("screen", "risk").putExtra("scope", "new_property")).use {
            onView(withContentDescription("Related checklist item")).perform(scrollTo(), click())
            onData(allOf(`is`(instanceOf(String::class.java)), `is`("Developer checks · Track record / delivery"))).perform(click())
            onView(withText("Save as pending")).perform(scrollTo(), click())
            waitForText("Mark as pending?")
            onView(withText("Mark pending")).perform(click())
            waitForText("Saved as Pending / unknown on this device.")
        }
        val result = AtomicReference<Map<String, String>>()
        val latch = CountDownLatch(1)
        DraftRepository.load(context, "new_property") { result.set(it.getOrThrow()); latch.countDown() }
        assertTrue(latch.await(10, TimeUnit.SECONDS))
        assertEquals("Pending / unknown", result.get()["developer_track_record_status"])
    }
    private fun waitForText(text: String) {
        val deadline = SystemClock.elapsedRealtime() + 15000
        while (SystemClock.elapsedRealtime() < deadline) {
            try { onView(withText(text)).check(matches(withEffectiveVisibility(Visibility.VISIBLE))); return }
            catch (_: androidx.test.espresso.NoMatchingViewException) { SystemClock.sleep(100) }
        }
        throw AssertionError("Timed out waiting for $text")
    }
}
