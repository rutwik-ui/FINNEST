package com.finnest.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class IntroductionSlide(val title: String, val body: String, val image: String)
data class NativeAppSettings(
    val version: Int, val guestEnabled: Boolean, val slides: List<IntroductionSlide>,
    val homeTitle: String = DEFAULT_TITLE, val homeSubtitle: String = DEFAULT_SUBTITLE,
    val maintenance: Boolean = false, val maintenanceMessage: String = DEFAULT_MAINTENANCE,
    val sections: List<String> = DEFAULT_SECTIONS,
)
const val DEFAULT_TITLE = "Your home-buying journey"
const val DEFAULT_SUBTITLE = "A clearer path to a place of your own."
const val DEFAULT_MAINTENANCE = "FINNEST is being updated. Please check back in a little while."
val DEFAULT_SECTIONS = listOf("journey", "nextAction", "progress", "guidance")
/** Only published, public settings enter this device cache. No session or buyer data is sent. */
object PublicAppSettings {
    private val defaults = NativeAppSettings(1, true, listOf(
        IntroductionSlide("Buying a home is a big decision.", "There are questions to ask, documents to understand and choices to make. You do not have to figure it all out at once.", "building"),
        IntroductionSlide("Know what to check, one step at a time.", "Keep your own questions and notes together, and see what still needs attention.", "planning"),
        IntroductionSlide("Move forward with more clarity.", "A checklist is a way to stay oriented—not an approval to buy. Get qualified help when you need it.", "family")
    ))
    fun parse(payload: JSONObject): NativeAppSettings {
        val version = payload.optInt("onboardingVersion", 1)
        require(version in 1..10000)
        // Home text, maintenance and section order come from the same admin "App settings" as the website.
        fun text(key: String, fallback: String, max: Int) = payload.optString(key, "").trim().take(max).ifEmpty { fallback }
        val order = payload.optJSONArray("sections")?.let { a -> (0 until a.length()).map { a.optString(it) } }?.filter { it in DEFAULT_SECTIONS }?.distinct()
        val home = { base: NativeAppSettings -> base.copy(
            homeTitle = text("homeTitle", DEFAULT_TITLE, 120), homeSubtitle = text("homeSubtitle", DEFAULT_SUBTITLE, 300),
            maintenance = payload.optBoolean("maintenanceEnabled", false), maintenanceMessage = text("maintenanceMessage", DEFAULT_MAINTENANCE, 500),
            sections = if (order != null && order.size == DEFAULT_SECTIONS.size) order else DEFAULT_SECTIONS) }
        val items = payload.optJSONArray("onboardingSlides") ?: return home(defaults.copy(version = version, guestEnabled = payload.getBoolean("guestEnabled")))
        require(items.length() in 2..4)
        val slides = (0 until items.length()).map { index ->
            val item = items.getJSONObject(index)
            val title = item.getString("title").trim(); val body = item.getString("body").trim(); val image = item.getString("image")
            require(title.length in 1..120 && body.length in 1..500 && image in listOf("building", "planning", "family"))
            IntroductionSlide(title, body, image)
        }
        return home(NativeAppSettings(version, payload.getBoolean("guestEnabled"), slides))
    }
    fun cached(context: Context): NativeAppSettings = runCatching {
        parse(JSONObject(context.getSharedPreferences("published_content", Context.MODE_PRIVATE).getString("settings", null) ?: return defaults))
    }.getOrDefault(defaults)
    suspend fun refresh(context: Context): NativeAppSettings = withContext(Dispatchers.IO) {
        val fallback = cached(context)
        runCatching {
            val connection = URL(BuildConfig.GUIDANCE_URL + "/rest/v1/finnest_app_settings?select=payload&state=eq.published&limit=1").openConnection() as HttpURLConnection
            connection.connectTimeout = 4000; connection.readTimeout = 4000; connection.instanceFollowRedirects = false
            connection.setRequestProperty("apikey", BuildConfig.GUIDANCE_PUBLISHABLE_KEY)
            connection.setRequestProperty("Accept", "application/json")
            try {
                check(connection.responseCode in 200..299)
                val text = connection.inputStream.use { input ->
                    val output = java.io.ByteArrayOutputStream(); val bytes = ByteArray(4096)
                    while (true) { val count = input.read(bytes); if (count < 0) break; check(output.size() + count <= 200_000); output.write(bytes, 0, count) }
                    output.toString("UTF-8")
                }
                val payload = JSONArray(text).getJSONObject(0).getJSONObject("payload")
                val settings = parse(payload)
                check(context.getSharedPreferences("published_content", Context.MODE_PRIVATE).edit().putString("settings", payload.toString()).putLong("fetched_at", System.currentTimeMillis()).commit())
                settings
            } finally { connection.disconnect() }
        }.getOrDefault(fallback)
    }
    fun fetchedAt(context: Context) = context.getSharedPreferences("published_content", Context.MODE_PRIVATE).getLong("fetched_at", 0L)
    fun completedVersion(context: Context): Int {
        val prefs = context.getSharedPreferences("entry", Context.MODE_PRIVATE)
        return prefs.getInt("intro_version", if (prefs.getBoolean("intro_complete", false)) 1 else 0)
    }
    fun complete(context: Context, guest: Boolean) {
        check(context.getSharedPreferences("entry", Context.MODE_PRIVATE).edit()
            .putInt("intro_version", cached(context).version).putBoolean("intro_complete", true).putBoolean("guest_session", guest).commit())
    }
}