package com.finnest.app

import org.junit.Assert.assertEquals
import org.junit.Test

class ResaleFieldsTest {
    @Test fun prototypeFieldsHaveStableUniqueKeys() {
        assertEquals(10, ResaleFields.budget.size)
        assertEquals(8, ResaleFields.configuration.size)
        assertEquals(9, ResaleFields.location.size)
        listOf(ResaleFields.budget, ResaleFields.configuration, ResaleFields.location).forEach { group ->
            assertEquals(group.size, group.map { it.first }.toSet().size)
        }
    }
}
