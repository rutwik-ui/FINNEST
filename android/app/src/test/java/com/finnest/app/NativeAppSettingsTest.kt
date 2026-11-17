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
}