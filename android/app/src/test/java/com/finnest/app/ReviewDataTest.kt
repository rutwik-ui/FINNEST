package com.finnest.app

import org.junit.Assert.*
import org.junit.Test

class ReviewDataTest {
    @Test fun emptyDraftDoesNotImplyVerification() {
        val checks = ReviewData.checks(emptyMap())
        assertEquals(44, checks.size)
        assertTrue(checks.all { it.status == null && ReviewData.pending(it) })
        assertEquals(checks.size, checks.map { it.key }.toSet().size)
    }
    @Test fun preservesExplicitStatusesAndNoteFields() {
        val checks = ReviewData.checks(mapOf("legal_ownership_status" to "Professional review", "legal_ownership_note" to "Ask lawyer", "financial_prompt_tax_status" to "Pending / unknown", "visit_light_status" to "Unrecognized"))
        val legal = checks.first { it.key == "legal_ownership" }
        assertEquals("Ask lawyer", legal.note)
        assertEquals("Professional review", legal.status)
        assertFalse(ReviewData.pending(legal))
        assertTrue(ReviewData.pending(checks.first { it.key == "financial_prompt_tax" }))
        assertEquals("Unrecognized", checks.first { it.key == "visit_light" }.status)
    }
}
