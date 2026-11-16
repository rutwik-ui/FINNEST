package com.finnest.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Parsing and safety rules for the admin-published project catalogue. */
class ProjectCatalogueTest {

    private val payload = """
        [
          {"id":"11111111-1111-4111-8111-111111111111","name":"Parkside","developer":"Acme",
           "city":"Mumbai","locality":"Powai","configuration":"2 & 3 BHK","price_from_label":"₹1.2 Cr",
           "possession_label":"Dec 2027","approval_note":"MahaRERA registered","summary":"Near the lake"},
          {"id":"22222222-2222-4222-8222-222222222222","name":"Lakeview"}
        ]
    """.trimIndent()

    @Test
    fun `parses every published field`() {
        val rows = PublicProjectClient.parse(payload)
        assertEquals(2, rows.size)
        val first = rows.first()
        assertEquals("Parkside", first.name)
        assertEquals("Acme", first.developer)
        assertEquals("Powai", first.locality)
        assertEquals("₹1.2 Cr", first.priceFrom)
        assertEquals("MahaRERA registered", first.approvalNote)
    }

    @Test
    fun `missing optional fields stay null instead of empty strings`() {
        val second = PublicProjectClient.parse(payload)[1]
        assertEquals("Lakeview", second.name)
        assertNull(second.developer)
        assertNull(second.city)
        assertNull(second.priceFrom)
        assertNull(second.summary)
    }

    @Test
    fun `rows without an id or name are dropped`() {
        val rows = PublicProjectClient.parse("""[{"id":"","name":"No id"},{"id":"x","name":""}]""")
        assertTrue(rows.isEmpty())
    }

    @Test
    fun `an empty catalogue is not an error`() {
        assertTrue(PublicProjectClient.parse("[]").isEmpty())
    }
}
