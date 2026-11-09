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
    private fun waitReady() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val deadline = SystemClock.elapsedRealtime() + 15000
        while (SystemClock.elapsedRealtime() < deadline) {
            val ready = AtomicReference(false)
            instrumentation.runOnMainSync {
                val activity: Activity? = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).firstOrNull()
                ready.set(activity is ResaleRequirementsActivity && activity.findViewById<View>(R.id.next)?.isEnabled == true)
            }
            if (ready.get()) return
            SystemClock.sleep(100)
        }
        throw AssertionError("Resale draft did not load")
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
