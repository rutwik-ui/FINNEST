package com.finnest.app

import android.net.Uri
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

/** Public content only. Never pass an authenticated or privileged credential to this client. */
data class GuidanceItem(val id: String, val title: String, val body: String, val category: String, val updatedAt: String)
data class GuidanceCursor(val id: String, val updatedAt: String)
data class GuidancePage(val items: List<GuidanceItem>, val next: GuidanceCursor?)
interface GuidanceSource { fun load(category: String, cursor: GuidanceCursor?): GuidancePage }

object GuidanceContract {
    const val PAGE_SIZE = 50
    val categories = listOf("all", "general", "new_property", "resale_property")
    private val uuid = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
    private val timestamp = Regex("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:\\.\\d{1,6})?(?:Z|[+-]\\d{2}:\\d{2})$")
    fun valid(cursor: GuidanceCursor) = uuid.matches(cursor.id) && timestamp.matches(cursor.updatedAt)
    fun filter(cursor: GuidanceCursor): String {
        require(valid(cursor))
        return "(updated_at.lt.\"${cursor.updatedAt}\",and(updated_at.eq.\"${cursor.updatedAt}\",id.lt.${cursor.id}))"
    }
}

class PublicGuidanceClient(private val baseUrl: String, private val publishableKey: String) : GuidanceSource {
    override fun load(category: String, cursor: GuidanceCursor?): GuidancePage {
        require(category in GuidanceContract.categories)
        require(baseUrl.startsWith("https://") && publishableKey.startsWith("sb_publishable_"))
        val uri = Uri.parse(baseUrl.trimEnd('/') + "/rest/v1/finnest_guidance").buildUpon()
            .appendQueryParameter("select", "id,title,body,category,status,updated_at")
            .appendQueryParameter("status", "eq.published")
            .appendQueryParameter("order", "updated_at.desc,id.desc")
            .appendQueryParameter("limit", (GuidanceContract.PAGE_SIZE + 1).toString())
        if (category != "all") uri.appendQueryParameter("category", "eq.$category")
        if (cursor != null) uri.appendQueryParameter("or", GuidanceContract.filter(cursor))
        val connection = URL(uri.build().toString()).openConnection() as HttpURLConnection
        connection.apply {
            requestMethod = "GET"; connectTimeout = 12000; readTimeout = 12000; instanceFollowRedirects = false
            setRequestProperty("apikey", publishableKey); setRequestProperty("Authorization", "Bearer $publishableKey")
            setRequestProperty("Accept", "application/json")
        }
        try {
            if (connection.responseCode !in 200..299) throw IllegalStateException("Guidance request failed")
            val content = connection.inputStream.use { input ->
                val out = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val count = input.read(buffer)
                    if (count == -1) break
                    if (out.size() + count > 5_000_000) throw IllegalStateException("Guidance response too large")
                    out.write(buffer, 0, count)
                }
                out.toString("UTF-8")
            }
            return parse(content, category)
        } finally { connection.disconnect() }
    }
    companion object {
        fun parse(content: String, category: String): GuidancePage {
            require(category in GuidanceContract.categories)
            val array = JSONArray(content)
            require(array.length() <= GuidanceContract.PAGE_SIZE + 1)
            val items = mutableListOf<GuidanceItem>()
            val count = minOf(array.length(), GuidanceContract.PAGE_SIZE)
            for (i in 0 until count) {
                val obj = array.getJSONObject(i)
                val kind = obj.getString("category")
                if (obj.getString("status") != "published" || kind !in GuidanceContract.categories.drop(1) || (category != "all" && kind != category)) continue
                items.add(GuidanceItem(obj.getString("id"), obj.getString("title"), obj.getString("body"), kind, obj.getString("updated_at")))
            }
            val next = if (array.length() > GuidanceContract.PAGE_SIZE) {
                val row = array.getJSONObject(GuidanceContract.PAGE_SIZE - 1)
                GuidanceCursor(row.getString("id"), row.getString("updated_at")).also { require(GuidanceContract.valid(it)) }
            } else null
            return GuidancePage(items, next)
        }
    }
}
