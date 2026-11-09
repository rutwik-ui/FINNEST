package com.finnest.app

import org.junit.Assert.*
import org.junit.Test

class DashboardStepsTest {
    @Test fun allJourneyDestinationsAreUniqueAndPresent() {
        val steps = DashboardSteps.all
        assertEquals(9, steps.size)
        assertEquals(steps.size, steps.map { it.id }.toSet().size)
        assertEquals(listOf("requirements", "developer", "projects", "visit", "feedback", "loan", "legal", "financial", "review"), steps.map { it.id })
    }
}
