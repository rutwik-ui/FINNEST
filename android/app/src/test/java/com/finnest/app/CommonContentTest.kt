package com.finnest.app

import org.junit.Assert.*
import org.junit.Test

class CommonContentTest {
    @Test fun rolesAreInformationalAndHaveUniqueTitles() {
        val roles = CommonContent.professionalRoles + CommonContent.optionalRoles
        assertEquals(9, roles.size)
        assertEquals(roles.size, roles.map { it.title }.toSet().size)
    }
    @Test fun riskLinksOnlyToExistingJourneySpecificChecklists() {
        val new = CommonContent.relatedChecks("new_property")
        val resale = CommonContent.relatedChecks("resale_property")
        assertEquals(44, new.size); assertEquals(45, resale.size)
        assertTrue(new.all { it.key.endsWith("_status") && it.destination in DashboardSteps.all.map { step -> step.id } })
        assertTrue(resale.all { it.key.endsWith("_status") && ResaleChecklists.find(it.destination) != null })
        assertEquals(new.size, new.map { it.key }.toSet().size)
        assertEquals(resale.size, resale.map { it.key }.toSet().size)
        assertTrue(CommonContent.relatedChecks("other").isEmpty())
    }
}
