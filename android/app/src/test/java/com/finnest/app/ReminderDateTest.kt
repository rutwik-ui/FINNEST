package com.finnest.app

import org.junit.Assert.*
import org.junit.Test

class ReminderDateTest {
    @Test fun validatesCalendarDatesAndLeapYears() {
        assertTrue(ReminderDate.valid("2028-02-29"))
        assertFalse(ReminderDate.valid("2027-02-29"))
        assertFalse(ReminderDate.valid("2026-13-01"))
        assertFalse(ReminderDate.valid("2026-1-01"))
        assertFalse(ReminderDate.valid("2101-01-01"))
    }
    @Test fun formatsDatesDeterministically() {
        assertEquals("2026-11-05", ReminderDate.fromParts(2026, 11, 5))
        assertEquals("05/11/2026", ReminderDate.display("2026-11-05"))
    }
}
