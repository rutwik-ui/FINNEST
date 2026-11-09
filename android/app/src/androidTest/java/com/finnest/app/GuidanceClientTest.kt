package com.finnest.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GuidanceClientTest {
    private fun row(i: Int, status: String = "published", category: String = "general") = JSONObject()
        .put("id", "00000000-0000-4000-8000-%012d".format(i)).put("title", "Title $i").put("body", "Body")
        .put("status", status).put("category", category).put("updated_at", "2026-11-05T08:02:03Z")
    @Test fun filtersDraftsAndOtherCategoriesEvenIfReturned() {
        val rows = JSONArray().put(row(1)).put(row(2, "draft")).put(row(3, "published", "new_property"))
        val page = PublicGuidanceClient.parse(rows.toString(), "general")
        assertEquals(listOf("Title 1"), page.items.map { it.title }); assertNull(page.next)
    }
    @Test fun returnsCursorForPageBoundary() {
        val rows = JSONArray(); repeat(51) { rows.put(row(it)) }
        val page = PublicGuidanceClient.parse(rows.toString(), "all")
        assertEquals(50, page.items.size)
        assertEquals("00000000-0000-4000-8000-000000000049", page.next?.id)
    }
}
