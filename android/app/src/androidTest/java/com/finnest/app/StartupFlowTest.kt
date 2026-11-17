package com.finnest.app

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class StartupFlowTest {
    private val inst get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = inst.targetContext
    private fun capture(name: String) {
        inst.waitForIdleSync(); SystemClock.sleep(350)
        val image = inst.uiAutomation.takeScreenshot() ?: throw AssertionError("Missing $name capture")
        val directory = File(InstrumentationRegistry.getArguments().getString("additionalTestOutputDir") ?: throw AssertionError("Missing screenshot destination"), "roadmap-screenshots").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        image.recycle()
    }
    @Test fun firstVisitAccountGuestDashboardAndReturn() {
        val prefs=context.getSharedPreferences("entry",0);prefs.edit().clear().commit()
        ActivityScenario.launch<Activity>(Intent(context, MainActivity::class.java).putExtra("replay_onboarding",true)).use {
            onView(withText("Buying a home is a big decision.")).check(matches(isDisplayed()));capture("startup-introduction")
            onView(withId(R.id.primary)).perform(click())
            onView(withText("Know what to check, one step at a time.")).check(matches(isDisplayed()));capture("startup-planning")
            onView(withId(R.id.primary)).perform(click())
            onView(withText("Get Started")).check(matches(isDisplayed()));capture("startup-final-slide")
            onView(withId(R.id.primary)).perform(click())
            onView(withContentDescription("Email")).check(matches(isDisplayed()));capture("startup-login")
            onView(withContentDescription("Sign Up")).perform(click());capture("startup-signup")
            onView(withText("Continue as guest")).perform(click())
            onView(withContentDescription("New Property")).perform(click())
            onView(withId(R.id.primary)).perform(click());SystemClock.sleep(1200);capture("startup-dashboard")
            onView(withContentDescription("Open menu")).perform(click());capture("startup-menu")
            onView(withText("Sign Up")).check(matches(isDisplayed()));onView(withText("Log In")).check(matches(isDisplayed()))
            onView(withContentDescription("Close menu")).perform(click())
            assertEquals(1, PublicAppSettings.completedVersion(context))
        }
        ActivityScenario.launch<Activity>(Intent(context, MainActivity::class.java)).use {
            SystemClock.sleep(1200);onView(withContentDescription("Open menu")).check(matches(isDisplayed()));capture("startup-returning-dashboard")
        }
        ActivityScenario.launch<Activity>(Intent(context, MainActivity::class.java).putExtra("replay_onboarding",true)).use {
            onView(withText("Buying a home is a big decision.")).check(matches(isDisplayed()));capture("startup-replay")
        }
    }
}