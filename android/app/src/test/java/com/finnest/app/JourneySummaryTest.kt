package com.finnest.app

import org.junit.Assert.*
import org.junit.Test

class JourneySummaryTest {
    @Test fun filtersSeparateUnknownPendingAndSavedStatuses() {
        val checks = ReviewData.checks(mapOf("developer_track_record_status" to "Checked by you", "developer_rera_status" to "Pending / unknown", "developer_land_title_status" to "Other"))
        assertEquals(3, JourneySummary.filter(checks, 0).size)
        assertEquals(42, JourneySummary.filter(checks, 1).size)
        assertEquals("Track record / delivery", JourneySummary.filter(checks, 2).single().title)
        assertEquals("Other", JourneySummary.filter(checks, 5).single().status)
        assertTrue(JourneySummary.filter(checks, 99).isEmpty())
    }
}
