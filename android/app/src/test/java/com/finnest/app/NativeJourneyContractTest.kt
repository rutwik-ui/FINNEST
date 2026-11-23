package com.finnest.app
import org.junit.Assert.*
import org.junit.Test
import org.json.JSONObject

class NativeJourneyContractTest {
    @Test fun nativeDownPaymentMapsToWebsiteBudgetAndReadsBack(){
        val payload=JSONObject()
        assertTrue(NativeJourneyContract.apply(payload,"new_property","down_payment","375000"))
        assertEquals("375000",payload.getJSONObject("budget").getString("down"))
        assertEquals("375000",NativeJourneyContract.decode(payload,"new_property")["down_payment"])
    }
    @Test fun nativeDeveloperKeyMapsToWebsiteTitleWithoutLosingRemoteSource(){
        val payload=JSONObject("{\"checks\":{\"title\":{\"source\":\"Registered document\"}}}")
        assertTrue(NativeJourneyContract.apply(payload,"new_property","developer_land_title_note","Ask lawyer"))
        assertEquals("Ask lawyer",payload.getJSONObject("checks").getJSONObject("title").getString("note"))
        assertEquals("Registered document",payload.getJSONObject("checks").getJSONObject("title").getString("source"))
    }
    @Test fun sharesTheSelectedJourneyWithTheWebsite() {
        val payload = org.json.JSONObject()
        org.junit.Assert.assertTrue(NativeJourneyContract.apply(payload, "new_property", "selected_journey", "Resale Property"))
        org.junit.Assert.assertEquals("Resale Property", payload.getString("journey"))
        org.junit.Assert.assertEquals("Resale Property", NativeJourneyContract.decode(payload, "new_property")["selected_journey"])
        NativeJourneyContract.apply(payload, "new_property", "selected_journey", "Something else")
        org.junit.Assert.assertEquals("Resale Property", payload.getString("journey"))
    }
}
