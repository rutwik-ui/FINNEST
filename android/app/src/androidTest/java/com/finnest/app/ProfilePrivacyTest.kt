package com.finnest.app

import android.content.Context
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
import org.hamcrest.Matchers.containsString
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class ProfilePrivacyTest {
    private val context: Context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun awaitScope(scope: String): Map<String, String> {
        val result = AtomicReference<Result<Map<String, String>>>()
        val latch = CountDownLatch(1)
        DraftRepository.load(context, scope) { result.set(it); latch.countDown() }
        assertTrue(latch.await(10, TimeUnit.SECONDS)); return result.get().getOrThrow()
    }
    private fun waitForScreen(type: Class<*>) {
        val inst = InstrumentationRegistry.getInstrumentation()
        val end = SystemClock.elapsedRealtime() + 15000
        while (SystemClock.elapsedRealtime() < end) {
            val ready = AtomicReference(false)
            inst.runOnMainSync {
                val activity = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).firstOrNull()
                ready.set(type.isInstance(activity) && (activity !is ProfileSettingsActivity || activity.findViewById<View>(R.id.next).isEnabled))
            }
            if (ready.get()) return
            SystemClock.sleep(100)
        }
        throw AssertionError("Timed out waiting for ${type.simpleName}")
    }
    @Test fun profileAndPrivacyDescribeActualLocalBehavior() {
        ActivityScenario.launch(ProfileSettingsActivity::class.java).use {
            waitForScreen(ProfileSettingsActivity::class.java)
            onView(withContentDescription("Profile Name")).perform(scrollTo(), replaceText("Test buyer"), closeSoftKeyboard())
            onView(withText("Privacy & data use")).perform(scrollTo(), click())
            onView(withText("Privacy & data use")).check(matches(isDisplayed()))
            onView(withText(containsString("does not upload your journey drafts"))).perform(scrollTo()).check(matches(isDisplayed()))
            androidx.test.espresso.Espresso.pressBack()
            waitForScreen(ProfileSettingsActivity::class.java)
            onView(withContentDescription("Profile Name")).perform(scrollTo()).check(matches(withText("Test buyer")))
        }
    }
    @Test fun confirmedResetDeletesOnlyStatedScopesAndKeepsJourneyPreference() {
        context.getSharedPreferences("entry", Context.MODE_PRIVATE).edit().putString("journey", "New Property").commit()
        LocalData.resetScopes.forEach { DraftRepository.save(context, it, "reset_test", "present") }
        DraftRepository.save(context, "unrelated_test", "keep", "safe")
        val queued = CountDownLatch(1); DraftRepository.afterPending("unrelated_test") { queued.countDown() }
        assertTrue(queued.await(10, TimeUnit.SECONDS))
        ActivityScenario.launch(ProfileSettingsActivity::class.java).use {
            waitForScreen(ProfileSettingsActivity::class.java)
            onView(withText("Delete local journey data")).perform(scrollTo(), click())
            onView(withText("Cancel")).perform(click())
            assertEquals("present", awaitScope("new_property")["reset_test"])
            onView(withText("Delete local journey data")).perform(scrollTo(), click())
            onView(withContentDescription("Delete confirmation")).perform(replaceText("DELETE"), closeSoftKeyboard())
            onView(withText("Delete")).perform(click())
            waitForScreen(DashboardActivity::class.java)
            LocalData.resetScopes.forEach { assertTrue("Scope $it should be empty", awaitScope(it).isEmpty()) }
            assertEquals("safe", awaitScope("unrelated_test")["keep"])
            assertEquals("New Property", context.getSharedPreferences("entry", Context.MODE_PRIVATE).getString("journey", null))
        }
    }
}
