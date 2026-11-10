package com.finnest.app

import org.junit.Assert.assertEquals
import org.junit.Test

class VisitObservationsTest {
    @Test fun promptsHaveUniqueStableKeys() {
        val checks = VisitObservations.checks
        assertEquals(14, checks.size)
        assertEquals(checks.size, checks.map { it.first }.toSet().size)
        assertEquals("Carpet area", checks.first().second)
        assertEquals("Garden / play area", checks.last().second)
    }
}
