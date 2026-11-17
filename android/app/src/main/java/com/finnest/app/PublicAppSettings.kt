package com.finnest.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class IntroductionSlide(val title: String, val body: String, val image: String)
data class NativeAppSettings(val version: Int, val guestEnabled: Boolean, val slides: List<IntroductionSlide>)
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
        val items = payload.optJSONArray("onboardingSlides") ?: return defaults.copy(version = version, guestEnabled = payload.getBoolean("guestEnabled"))
        require(items.length() in 2..4)
        val slides = (0 until items.length()).map { index ->
            val item = items.getJSONObject(index)
            val title = item.getString("title").trim(); val body = item.getString("body").trim(); val image = item.getString("image")
            require(title.length in 1..120 && body.length in 1..500 && image in listOf("building", "planning", "family"))
            IntroductionSlide(title, body, image)
        }
        return NativeAppSettings(version, payload.getBoolean("guestEnabled"), slides)
    }
    fun cached(context: Context): NativeAppSettings = runCatching {
        parse(JSONObject(context.getSharedPreferences("published_content", Context.MODE_PRIVATE).getString("settings", null) ?: return defaults))
    }.getOrDefault(defaults)
    suspend fun refresh(context: Context): NativeAppSettings = withContext(Dispatchers.IO) {
        val fallback = cached(context)
        runCatching {
            val connection = URL(BuildConfig.GUIDANCE_URL + "/rest/v1/finnest_app_settings?select=payload&state=eq.published&limit=1").openConnection() as HttpURLConnection
            connection.connectTimeout = 2500; connection.readTimeout = 2500; connection.instanceFollowRedirects = false
            connection.setRequestProperty("apikey", BuildConfig.GUIDANCE_PUBLISHABLE_KEY)
            connection.setRequestProperty("Accept", "application/json")
            try {
                check(connection.responseCode in 200..299)
                val text = connection.inputStream.use { input ->
                    val output = java.io.ByteArrayOutputStream(); val bytes = ByteArray(4096)
                    while (true) { val count = input.read(bytes); if (count < 0) break; check(output.size() + count <= 20000); output.write(bytes, 0, count) }
                    output.toString("UTF-8")
                }
                val payload = JSONArray(text).getJSONObject(0).getJSONObject("payload")
                val settings = parse(payload)
                check(context.getSharedPreferences("published_content", Context.MODE_PRIVATE).edit().putString("settings", payload.toString()).commit())
                settings
            } finally { connection.disconnect() }
        }.getOrDefault(fallback)
    }
    fun completedVersion(context: Context): Int {
        val prefs = context.getSharedPreferences("entry", Context.MODE_PRIVATE)
        return prefs.getInt("intro_version", if (prefs.getBoolean("intro_complete", false)) 1 else 0)
    }
    fun complete(context: Context, guest: Boolean) {
        check(context.getSharedPreferences("entry", Context.MODE_PRIVATE).edit()
            .putInt("intro_version", cached(context).version).putBoolean("intro_complete", true).putBoolean("guest_session", guest).commit())
    }
}