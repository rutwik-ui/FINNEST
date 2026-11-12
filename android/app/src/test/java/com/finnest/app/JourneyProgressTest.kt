package com.finnest.app

import org.junit.Assert.*
import org.junit.Test

class JourneyProgressTest {
    @Test fun untouchedFormsRemainZero() {
        assertEquals(0, JourneyProgress.total("new_property", emptyMap()).recorded)
        assertEquals(0, JourneyProgress.total("resale_property", emptyMap()).recorded)
    }
    @Test fun recordedAndClearedPlanningEntriesAreAccurate() {
        assertEquals(30, JourneyProgress.count("new_property", "requirements", emptyMap()).total)
        assertEquals(2, JourneyProgress.count("new_property", "requirements", mapOf("loan" to "100", "config_2bhk" to "true", "location_workplace" to "false", "down_payment" to "")).recorded)
        assertEquals(0, JourneyProgress.count("new_property", "requirements", mapOf("loan" to "", "config_2bhk" to "false")).recorded)
    }
    @Test fun pendingStatusIsRecordedInputNotVerifiedCompletion() {
        val count = JourneyProgress.count("new_property", "developer", mapOf("developer_rera_status" to "Pending / unknown", "developer_track_record_note" to "Question"))
        assertEquals(1, count.recorded); assertEquals(6, count.total)
    }
    @Test fun emptyShortlistDoesNotCount() {
        assertEquals(0, JourneyProgress.count("new_property", "projects", mapOf("shortlist_projects_v1" to "[]")).recorded)
        assertEquals(1, JourneyProgress.count("new_property", "projects", mapOf("shortlist_projects_v1" to "[{\"name\":\"Parkside\"}]")).recorded)
    }
    @Test fun journeyKeysRemainSeparate() {
        val draft = mapOf("budget_budget" to "500", "seller_identity_status" to "Information provided")
        assertEquals(2, JourneyProgress.total("resale_property", draft).recorded)
        assertEquals(0, JourneyProgress.total("new_property", draft).recorded)
        assertEquals(7, JourneyProgress.keys("resale_property", "society").size)
    }
    @Test fun percentageUsesActualRecordedEntries() {
        assertEquals(25, JourneyProgress.Count(1, 4).percent)
        assertEquals(0, JourneyProgress.Count(0, 0).percent)
    }
}