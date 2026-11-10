package com.finnest.app

import android.app.Activity
import android.os.SystemClock
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class ResaleJourneyTest {
    private fun waitReady(type: Class<out Activity> = ResaleRequirementsActivity::class.java) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val deadline = SystemClock.elapsedRealtime() + 15000
        while (SystemClock.elapsedRealtime() < deadline) {
            val ready = AtomicReference(false)
            instrumentation.runOnMainSync {
                val activity: Activity? = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).firstOrNull()
                ready.set(type.isInstance(activity) && activity?.findViewById<View>(R.id.next)?.isEnabled == true)
            }
            if (ready.get()) return
            SystemClock.sleep(100)
        }
        throw AssertionError("Resale draft did not load")
    }
    @Test fun conditionNotePersistsAcrossReturnToOverview() {
        val value = "Ask about leak " + System.currentTimeMillis()
        ActivityScenario.launch(ResaleOverviewActivity::class.java).use {
            onView(withText("Property condition  →")).perform(scrollTo(), click())
            waitReady(ResaleConditionActivity::class.java)
            onView(withContentDescription("Walls / ceilings / leaks Note / observation")).perform(scrollTo(), replaceText(value), closeSoftKeyboard())
            onView(withId(R.id.save_later)).perform(click())
            onView(withText("Property condition  →")).perform(scrollTo(), click())
            waitReady(ResaleConditionActivity::class.java)
            onView(withContentDescription("Walls / ceilings / leaks Note / observation")).perform(scrollTo()).check(matches(withText(value)))
        }
    }
    @Test fun resaleSectionsSaveAndSummaryShowsEntries() {
        val unique = "Follow up " + System.currentTimeMillis()
        ActivityScenario.launch(ResaleOverviewActivity::class.java).use {
            onView(withText("Seller, ownership & title  →")).perform(scrollTo(), click())
            waitReady(ResaleConditionActivity::class.java)
            onView(withContentDescription("Seller identity & authority Note / observation")).perform(scrollTo(), replaceText(unique), closeSoftKeyboard())
            onView(withId(R.id.next)).perform(click())
            waitReady(ResaleConditionActivity::class.java)
            onView(withContentDescription("Maintenance dues amount")).perform(scrollTo(), replaceText("1250"), closeSoftKeyboard())
            onView(withId(R.id.next)).perform(click())
            waitReady(ResaleConditionActivity::class.java)
            onView(withContentDescription("Lawyer review status")).perform(scrollTo(), replaceText("Appointment needed"), closeSoftKeyboard())
            onView(withId(R.id.next)).perform(click())
            waitReady(ResaleConditionActivity::class.java)
            onView(withContentDescription("Written promises status")).perform(scrollTo(), replaceText("Discuss"), closeSoftKeyboard())
            onView(withId(R.id.next)).perform(click())
            waitReady(ResaleSummaryActivity::class.java)
            onView(withText("Note: $unique")).perform(scrollTo()).check(matches(isDisplayed()))
            onView(withText("Amount (₹): 1250")).perform(scrollTo()).check(matches(isDisplayed()))
            onView(withText("Lawyer review status: Appointment needed")).perform(scrollTo()).check(matches(isDisplayed()))
            onView(withText("Status: Discuss")).perform(scrollTo()).check(matches(isDisplayed()))
        }
    }
    @Test fun resaleRequirementsReopenWithoutOverwritingNewProperty() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val latch = CountDownLatch(1)
        DraftRepository.save(context, "new_property", "budget_budget", "321")
        DraftRepository.afterPending("new_property") { latch.countDown() }
        assert(latch.await(10, TimeUnit.SECONDS))
        ActivityScenario.launch(ResaleOverviewActivity::class.java).use {
            onView(withText("Requirements & location  →")).perform(scrollTo(), click())
            waitReady()
            onView(withContentDescription("Resale Budget (₹)")).perform(scrollTo(), replaceText("123456"), closeSoftKeyboard())
            onView(withText("Lift")).perform(scrollTo(), click())
            onView(withId(R.id.save_later)).perform(click())
            onView(withText("Requirements & location  →")).perform(scrollTo(), click())
            waitReady()
            onView(withContentDescription("Resale Budget (₹)")).perform(scrollTo()).check(matches(withText("123456")))
            onView(withText("Lift")).perform(scrollTo()).check(matches(isChecked()))
        }
        val result = AtomicReference<Map<String, String>>()
        val loaded = CountDownLatch(1)
        DraftRepository.load(context, "new_property") { result.set(it.getOrThrow()); loaded.countDown() }
        assert(loaded.await(10, TimeUnit.SECONDS))
        org.junit.Assert.assertEquals("321", result.get()["budget_budget"])
    }
}
