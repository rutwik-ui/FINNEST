package com.finnest.app

import org.junit.Assert.assertEquals
import org.junit.Test

class LegalDueDiligenceTest {
    @Test fun prototypePromptsHaveUniqueStableKeys() {
        val checks = LegalDueDiligence.checks
        assertEquals(9, checks.size)
        assertEquals(checks.size, checks.map { it.first }.toSet().size)
        assertEquals("Ownership & title chain", checks.first().second)
        assertEquals("Possession, cancellation & refund clauses", checks.last().second)
    }
}
