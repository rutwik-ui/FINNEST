package com.finnest.app

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class NativeAppSettingsTest {
    private fun payload(count: Int) = JSONObject().put("onboardingVersion",2).put("guestEnabled",false).put("onboardingSlides",JSONArray().apply {
        repeat(count) { put(JSONObject().put("title","Home planning").put("body","Keep your notes together.").put("image","planning")) }
    })
    @Test fun allowsTwoToFourSlidesOnly() {
        assertEquals(2, PublicAppSettings.parse(payload(2)).slides.size)
        assertEquals(4, PublicAppSettings.parse(payload(4)).slides.size)
        assertThrows(IllegalArgumentException::class.java) { PublicAppSettings.parse(payload(1)) }
        assertThrows(IllegalArgumentException::class.java) { PublicAppSettings.parse(payload(5)) }
    }
    @Test fun appliesPublishedVersionAndGuestChoice() {
        val settings = PublicAppSettings.parse(payload(3))
        assertEquals(2,settings.version);assertEquals(false,settings.guestEnabled)
    }
    @Test fun readsHomeCopyMaintenanceAndSectionOrder() {
        val p = payload(2).put("homeTitle","Plan your home").put("homeSubtitle","Step by step.").put("maintenanceEnabled",true)
            .put("maintenanceMessage","Back at 6 pm.").put("sections",JSONArray(listOf("guidance","journey","progress","nextAction")))
        val s = PublicAppSettings.parse(p)
        assertEquals("Plan your home",s.homeTitle);assertEquals("Step by step.",s.homeSubtitle)
        assertEquals(true,s.maintenance);assertEquals("Back at 6 pm.",s.maintenanceMessage)
        assertEquals(listOf("guidance","journey","progress","nextAction"),s.sections)
    }
    @Test fun fallsBackToDefaultsForMissingOrInvalidHomeSettings() {
        val s = PublicAppSettings.parse(payload(2).put("sections",JSONArray(listOf("journey","journey"))))
        assertEquals("Your home-buying journey",s.homeTitle);assertEquals(false,s.maintenance)
        assertEquals(listOf("journey","nextAction","progress","guidance"),s.sections)
    }
}
