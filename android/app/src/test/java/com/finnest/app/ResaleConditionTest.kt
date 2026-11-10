package com.finnest.app

import org.junit.Assert.assertEquals
import org.junit.Test

class ResaleConditionTest {
    @Test fun prototypePromptsAreCompleteAndKeysAreUnique() {
        val checks = ResaleCondition.checks
        assertEquals(12, checks.size)
        assertEquals(checks.size, checks.map { it.first }.toSet().size)
        assertEquals("Walls / ceilings / leaks", checks.first().second)
        assertEquals("Repairs", checks.last().second)
    }
}
