package com.finnest.app

import org.junit.Assert.*
import org.junit.Test

class ResaleChecklistsTest {
    @Test fun visibleAreasAndPromptsHaveStableUniqueKeys() {
        val sections = ResaleChecklists.sections
        assertEquals(listOf("condition", "seller", "society", "legal", "handover"), sections.map { it.id })
        assertEquals(listOf(12, 5, 7, 8, 13), sections.map { it.checks.size })
        sections.forEach { assertEquals(it.checks.size, it.checks.map { p -> p.first }.toSet().size) }
        assertTrue(ResaleChecklists.find("society")!!.hasAmount)
        assertTrue(ResaleChecklists.find("handover")!!.freeTextStatus)
        assertNull(ResaleChecklists.find("unknown"))
    }
}
