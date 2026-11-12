package com.finnest.app

import android.app.Activity
import android.os.SystemClock
import android.view.View
import android.widget.EditText
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.hamcrest.Matchers.allOf
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class DashboardNavigationTest {
    private fun waitForReview() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val deadline = SystemClock.elapsedRealtime() + 15000
        while (SystemClock.elapsedRealtime() < deadline) {
            val current = AtomicReference<Activity?>()
            instrumentation.runOnMainSync {
                val activities = androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED)
                current.set(activities.firstOrNull())
            }
            val ready = AtomicReference(false)
            instrumentation.runOnMainSync {
                val activity = current.get()
                ready.set(activity is FinalReviewActivity && activity.findViewById<View>(R.id.next)?.isEnabled == true)
            }
            if (ready.get()) return
            SystemClock.sleep(100)
        }
        throw AssertionError("Final review did not finish loading")
    }

    private fun waitForReminders() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val deadline = SystemClock.elapsedRealtime() + 15000
        while (SystemClock.elapsedRealtime() < deadline) {
            val ready = AtomicReference(false)
            instrumentation.runOnMainSync {
                val activity = androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED).firstOrNull()
                ready.set(activity is RemindersActivity && activity.findViewById<View>(R.id.next)?.isEnabled == true)
            }
            if (ready.get()) return
            SystemClock.sleep(100)
        }
        throw AssertionError("Reminders did not finish loading")
    }
    @Test fun addReopenAndDeleteDatedLocalReminder() {
        val title = "Inspect documents " + System.currentTimeMillis()
        ActivityScenario.launch(DashboardActivity::class.java).use {
            onView(withText("Open reminders  →")).perform(scrollTo(), click())
            waitForReminders()
            onView(withContentDescription("Reminder title")).perform(scrollTo(), replaceText(title), closeSoftKeyboard())
            onView(withContentDescription("Reminder date")).perform(scrollTo(), click())
            onView(withText("OK")).perform(click())
            onView(withText("Add reminder")).perform(scrollTo(), click())
            onView(withText(title)).perform(scrollTo()).check(matches(isDisplayed()))
            onView(withId(R.id.next)).perform(scrollTo(), click())
            onView(withText("Open reminders  →")).perform(scrollTo(), click())
            waitForReminders()
            onView(withText(title)).perform(scrollTo()).check(matches(isDisplayed()))
            onView(withContentDescription("Delete reminder: $title")).perform(scrollTo(), click())
            onView(withText("Delete")).perform(click())
            onView(withText("No dated notes yet. Add one above when you want to revisit something.")).perform(scrollTo()).check(matches(isDisplayed()))
        }
    }

    @Test fun dashboardReviewSaveReturnAndReopenPreservesNextActions() {
        val value = "Follow up on title " + System.currentTimeMillis()
        ActivityScenario.launch(DashboardActivity::class.java).use {
            onView(withText("Open final review  →")).perform(scrollTo(), click())
            waitForReview()
            onView(allOf(isAssignableFrom(EditText::class.java), withContentDescription("Your next actions")))
                .perform(scrollTo(), replaceText(value))
            onView(withId(R.id.next)).perform(scrollTo(), click())
            onView(withText("My Journey")).perform(scrollTo()).check(matches(isDisplayed()))
            onView(withId(R.id.back_bottom)).perform(scrollTo(), click())
            onView(withText("Your journey steps")).perform(scrollTo()).check(matches(isDisplayed()))
            onView(withText("Open final review  →")).perform(scrollTo(), click())
            waitForReview()
            onView(allOf(isAssignableFrom(EditText::class.java), withContentDescription("Your next actions")))
                .perform(scrollTo()).check(matches(withText(value)))
            onView(withId(R.id.save_later)).perform(scrollTo(), click())
            onView(withText("Your journey steps")).perform(scrollTo()).check(matches(isDisplayed()))
        }
    }
}
