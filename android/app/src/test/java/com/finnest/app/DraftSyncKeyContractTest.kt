package com.finnest.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The website and Android decided to share journey answers, but each had invented
 * its own spelling: the website nests the budget (`budget.down`), this app saves
 * flat keys (`down_payment`). Both were correct in isolation, so a down payment
 * typed on the phone never appeared on the laptop and nothing reported an error.
 *
 * DraftSync.CANONICAL_ALIASES is the bridge. These tests pin its shape and,
 * more importantly, compare it line by line with the website's copy so neither
 * side can gain an entry the other lacks.
 */
class DraftSyncKeyContractTest {

    @Test
    fun `every website budget key maps to a distinct local key`() {
        val local = DraftSync.CANONICAL_ALIASES.values.toList()
        assertEquals("two account keys point at one local key", local.size, local.toSet().size)
        assertTrue("alias table is empty", DraftSync.CANONICAL_ALIASES.isNotEmpty())
    }

    @Test
    fun `translates both directions`() {
        assertEquals("budget.down", DraftSync.toAccountKey("down_payment"))
        assertEquals("down_payment", DraftSync.toLocalKey("budget.down"))
        assertEquals("budget.stamp", DraftSync.toAccountKey("stamp_duty"))
    }

    @Test
    fun `passes unknown keys through untouched`() {
        // A field added on one side only must still round-trip, not disappear.
        assertEquals("future_field", DraftSync.toAccountKey("future_field"))
        assertEquals("future_field", DraftSync.toLocalKey("future_field"))
    }

    @Test
    fun `never maps a key onto itself`() {
        for ((account, local) in DraftSync.CANONICAL_ALIASES) {
            assertNotEquals("alias is a no-op for $account", account, local)
        }
    }

    @Test
    fun `agrees with the website alias table line for line`() {
        // Gradle's working directory depends on how the build is invoked, so walk up
        // from wherever we are instead of assuming one layout.
        val source = generateSequence(File(".").absoluteFile) { it.parentFile }
            .map { File(it, "web-app/src/finnest/draftSync.js") }
            .firstOrNull { it.exists() }
        assertTrue("cannot find the website's draftSync.js from ${File(".").absolutePath}", source != null)
        val block = source!!.readText()
            .substringAfter("CANONICAL_ALIASES = {")
            .substringBefore("};")
        val pairs = Regex("'([\\w.]+)':\\s*'([\\w]+)'")
            .findAll(block)
            .associate { it.groupValues[1] to it.groupValues[2] }

        assertEquals(
            "web-app/src/finnest/draftSync.js and DraftSync.kt disagree about shared field keys",
            pairs,
            DraftSync.CANONICAL_ALIASES,
        )
    }
}
