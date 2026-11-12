package com.finnest.app

import android.graphics.Bitmap
import android.os.SystemClock
import android.view.View
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class JourneyRoadmapTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private fun save(scope: String, key: String, value: String) {
        DraftRepository.save(context, scope, key, value)
        val ready = CountDownLatch(1)
        DraftRepository.afterPending(scope) { assertTrue(it); ready.countDown() }
        assertTrue(ready.await(10, TimeUnit.SECONDS))
    }
    private fun <T : android.app.Activity> awaitText(scenario: ActivityScenario<T>, expected: String) {
        val deadline = SystemClock.elapsedRealtime() + 15000
        while (SystemClock.elapsedRealtime() < deadline) {
            var ready = false
            scenario.onActivity { activity ->
                fun find(view: View): Boolean = if (view is TextView && view.text.toString().contains(expected)) true
                    else if (view is android.view.ViewGroup) (0 until view.childCount).any { find(view.getChildAt(it)) } else false
                ready = find(activity.findViewById(android.R.id.content))
            }
            if (ready) return
            SystemClock.sleep(100)
        }
        fail("Progress did not show $expected")
    }
    private fun capture(name: String) {
        instrumentation.waitForIdleSync()
        val bitmap = instrumentation.uiAutomation.takeScreenshot() ?: throw AssertionError("Screenshot unavailable")
        val dir = File(context.getExternalFilesDir(null), "roadmap-screenshots").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        instrumentation.uiAutomation.executeShellCommand("mkdir -p /data/local/tmp/roadmap-screenshots; screencap -p /data/local/tmp/roadmap-screenshots/$name.png").use { descriptor ->
            android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { it.readBytes() }
        }
    }
    @Test fun editedFormUpdatesDurableProgressAndReopens() {
        val key = "loan"
        val original = mutableMapOf<String, String>()
        val loaded = CountDownLatch(1)
        DraftRepository.loadSaved(context, "new_property") { original.putAll(it.getOrThrow()); loaded.countDown() }
        assertTrue(loaded.await(10, TimeUnit.SECONDS))
        val base = JourneyProgress.total("new_property", original.toMap() + (key to ""))
        save("new_property", key, "")
        try {
            ActivityScenario.launch(DashboardActivity::class.java).use { scenario ->
                awaitText(scenario, "${base.recorded} of ${base.total}")
                capture("01-new-dashboard")
            }
            ActivityScenario.launch(RequirementsActivity::class.java).use { scenario ->
                val deadline = SystemClock.elapsedRealtime() + 15000
                var ready = false
                while (!ready && SystemClock.elapsedRealtime() < deadline) { scenario.onActivity { ready = it.findViewById<View>(R.id.next).isEnabled }; SystemClock.sleep(100) }
                assertTrue(ready)
                onView(withContentDescription("Approximate loan amount, if known")).perform(scrollTo(), replaceText("7500000"), closeSoftKeyboard())
                awaitText(scenario, "${base.recorded + 1} of ${base.total}")
                capture("02-new-form-saved")
            }
            ActivityScenario.launch(DashboardActivity::class.java).use { scenario ->
                awaitText(scenario, "${base.recorded + 1} of ${base.total}")
                capture("03-new-dashboard-readback")
            }
            ActivityScenario.launch(ResaleOverviewActivity::class.java).use { scenario ->
                val rows = mutableMapOf<String, String>(); val done = CountDownLatch(1)
                DraftRepository.loadSaved(context, "resale_property") { rows.putAll(it.getOrThrow()); done.countDown() }
                assertTrue(done.await(10, TimeUnit.SECONDS))
                val total = JourneyProgress.total("resale_property", rows)
                awaitText(scenario, "${total.recorded} of ${total.total}"); capture("04-resale-dashboard")
            }
            ActivityScenario.launch(ResaleRequirementsActivity::class.java).use { scenario ->
                val deadline = SystemClock.elapsedRealtime() + 15000
                var ready = false
                while (!ready && SystemClock.elapsedRealtime() < deadline) { scenario.onActivity { ready = it.findViewById<View>(R.id.next).isEnabled }; SystemClock.sleep(100) }
                assertTrue(ready); capture("05-resale-form")
            }
        } finally { save("new_property", key, original[key].orEmpty()) }
    }
}