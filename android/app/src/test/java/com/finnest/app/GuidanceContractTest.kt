package com.finnest.app

import org.junit.Assert.*
import org.junit.Test

class GuidanceContractTest {
    @Test fun cursorUsesTimestampAndUuid() {
        val c = GuidanceCursor("00000000-0000-4000-8000-000000000001", "2026-11-05T08:02:03.123456+00:00")
        assertTrue(GuidanceContract.valid(c))
        assertEquals("(updated_at.lt.\"2026-11-05T08:02:03.123456+00:00\",and(updated_at.eq.\"2026-11-05T08:02:03.123456+00:00\",id.lt.00000000-0000-4000-8000-000000000001))", GuidanceContract.filter(c))
    }
    @Test fun rejectsUnsafeCursorValues() {
        assertFalse(GuidanceContract.valid(GuidanceCursor("x),status.eq.draft", "2026-11-05T08:02:03Z")))
        assertFalse(GuidanceContract.valid(GuidanceCursor("00000000-0000-4000-8000-000000000001", "2026-11-05,foo")))
    }
}
