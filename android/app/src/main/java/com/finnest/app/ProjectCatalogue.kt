package com.finnest.app

import android.net.Uri
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

/**
 * Admin-published project catalogue.
 *
 * Read-only and public: the same publishable key the guidance library uses, and
 * RLS restricts this to status = 'published'. Nothing buyer-specific is here.
 */
data class ListedProject(
    val id: String,
    val name: String,
    val developer: String?,
    val city: String?,
    val locality: String?,
    val configuration: String?,
    val priceFrom: String?,
    val possession: String?,
    val approvalNote: String?,
    val summary: String?,
)

interface ProjectSource { fun load(): List<ListedProject> }

class PublicProjectClient(
    private val baseUrl: String = BuildConfig.GUIDANCE_URL,
    private val publishableKey: String = BuildConfig.GUIDANCE_PUBLISHABLE_KEY,
) : ProjectSource {
    override fun load(): List<ListedProject> {
        require(baseUrl.startsWith("https://") && publishableKey.startsWith("sb_publishable_"))
        val uri = Uri.parse(baseUrl.trimEnd('/') + "/rest/v1/finnest_projects").buildUpon()
            .appendQueryParameter(
                "select",
                "id,name,developer,city,locality,configuration,price_from_label,possession_label,approval_note,summary",
            )
            .appendQueryParameter("status", "eq.published")
            .appendQueryParameter("order", "sort_order.asc,name.asc")
            .appendQueryParameter("limit", "200")
            .build()
        val connection = URL(uri.toString()).openConnection() as HttpURLConnection
        connection.apply {
            requestMethod = "GET"; connectTimeout = 12000; readTimeout = 12000; instanceFollowRedirects = false
            setRequestProperty("apikey", publishableKey)
            setRequestProperty("Authorization", "Bearer $publishableKey")
            setRequestProperty("Accept", "application/json")
        }
        try {
            if (connection.responseCode !in 200..299) throw IllegalStateException("Catalogue request failed")
            val content = connection.inputStream.use { it.bufferedReader().readText() }
            if (content.length > 1_000_000) throw IllegalStateException("Catalogue response too large")
            return parse(content)
        } finally { connection.disconnect() }
    }

    companion object {
        fun parse(content: String): List<ListedProject> {
            val array = JSONArray(content)
            val out = ArrayList<ListedProject>(array.length())
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                val id = o.optString("id"); val name = o.optString("name")
                if (id.isBlank() || name.isBlank()) continue
                out.add(
                    ListedProject(
                        id = id,
                        name = name,
                        developer = o.optString("developer").takeIf { it.isNotBlank() },
                        city = o.optString("city").takeIf { it.isNotBlank() },
                        locality = o.optString("locality").takeIf { it.isNotBlank() },
                        configuration = o.optString("configuration").takeIf { it.isNotBlank() },
                        priceFrom = o.optString("price_from_label").takeIf { it.isNotBlank() },
                        possession = o.optString("possession_label").takeIf { it.isNotBlank() },
                        approvalNote = o.optString("approval_note").takeIf { it.isNotBlank() },
                        summary = o.optString("summary").takeIf { it.isNotBlank() },
                    ),
                )
            }
            return out
        }
    }
}
