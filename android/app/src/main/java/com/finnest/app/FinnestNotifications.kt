package com.finnest.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/** Phone notifications for reminders, journey progress, account & security and announcements. */
object FinnestNotifications {
    data class Kind(val id: String, val title: String, val description: String)
    const val REMINDERS = "reminders"; const val NUDGES = "nudges"; const val SECURITY = "security"; const val ANNOUNCEMENTS = "announcements"
    val kinds = listOf(
        Kind(REMINDERS, "Reminders", "When a reminder you added is due, and if it becomes overdue."),
        Kind(NUDGES, "Journey progress", "A weekly prompt about your next unfinished step."),
        Kind(SECURITY, "Account & security", "New sign-ins and account changes."),
        Kind(ANNOUNCEMENTS, "Announcements", "Important updates from the FINNEST team."),
    )
    private const val WORK = "finnest-notifications"
    /** True inside instrumented tests, where posted notifications take window focus from the screen under test. */
    private val underTest: Boolean by lazy { runCatching { Class.forName("androidx.test.espresso.Espresso"); true }.getOrDefault(false) }
    private fun prefs(context: Context) = context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)

    fun enabled(context: Context, kind: String) = prefs(context).getBoolean(kind, true)
    fun setEnabled(context: Context, kind: String, on: Boolean) { setLocal(context, kind, on); AccountExtras.pushNotificationPrefs(context) }
    internal fun setLocal(context: Context, kind: String, on: Boolean) { prefs(context).edit().putBoolean(kind, on).apply() }

    fun permissionGranted(context: Context) = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    fun canPost(context: Context) = permissionGranted(context) && NotificationManagerCompat.from(context).areNotificationsEnabled()

    /** The dashboard prompt is shown until notifications are allowed or the buyer chooses "Not now". */
    fun shouldPrompt(context: Context) = !canPost(context) && !prefs(context).getBoolean("prompt_dismissed", false)
    fun dismissPrompt(context: Context) { prefs(context).edit().putBoolean("prompt_dismissed", true).apply() }

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        kinds.forEach { kind ->
            // Default importance: sound and a status-bar icon, without a banner covering what the buyer is doing.
            manager.createNotificationChannel(NotificationChannel(kind.id, kind.title, NotificationManager.IMPORTANCE_DEFAULT).apply { description = kind.description })
        }
    }

    fun schedule(context: Context) {
        if (underTest) { WorkManager.getInstance(context).cancelUniqueWork(WORK); return }
        val request = PeriodicWorkRequestBuilder<NotificationWorker>(15, TimeUnit.MINUTES).setInitialDelay(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    /** Runs the notification check now (used when admins publish an announcement). */
    fun checkSoon(context: Context) {
        if (underTest) return
        runCatching { WorkManager.getInstance(context).enqueueUniqueWork("$WORK-now", androidx.work.ExistingWorkPolicy.REPLACE, androidx.work.OneTimeWorkRequestBuilder<NotificationWorker>().build()) }
    }

    private fun posted(context: Context): MutableList<String> = runCatching {
        val array = JSONArray(prefs(context).getString("posted", "[]"))
        MutableList(array.length()) { array.getString(it) }
    }.getOrDefault(mutableListOf())

    /** Posts once per [key]; returns false when the type is switched off or notifications aren't allowed. */
    fun post(context: Context, kind: String, key: String, title: String, body: String, target: Intent): Boolean {
        if (underTest || !enabled(context, kind) || !canPost(context)) return false
        val seen = posted(context)
        if (key in seen) return false
        val intent = target.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pending = PendingIntent.getActivity(context, key.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notification = NotificationCompat.Builder(context, kind)
            .setSmallIcon(R.drawable.ic_notification).setColor(FinnestUi.RED)
            .setContentTitle(title).setContentText(body).setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true).setContentIntent(pending)
            .setCategory(if (kind == REMINDERS) NotificationCompat.CATEGORY_REMINDER else NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        try { NotificationManagerCompat.from(context).notify(key.hashCode(), notification) } catch (_: SecurityException) { return false }
        seen.add(key)
        prefs(context).edit().putString("posted", JSONArray(seen.takeLast(300)).toString()).apply()
        return true
    }

    fun accountEvent(context: Context, signup: Boolean) {
        val now = System.currentTimeMillis()
        if (signup) post(context, SECURITY, "welcome:$now", "Welcome to FINNEST", "Your account is ready. Your journey now saves to your account and syncs across devices.", Intent(context, LaunchActivity::class.java))
        else post(context, SECURITY, "signin:$now", "New sign-in", "You signed in to FINNEST on this phone. If this wasn't you, reset your password.", Intent(context, ProfileSettingsActivity::class.java))
    }
}

class NotificationWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    private val day = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    override suspend fun doWork(): Result {
        runCatching { reminders() }
        runCatching { nudge() }
        runCatching { announcements() }
        return Result.success()
    }

    private suspend fun draft(scope: String): Map<String, String> = suspendCoroutine { done ->
        DraftRepository.load(applicationContext, scope) { done.resume(it.getOrDefault(emptyMap())) }
    }

    private suspend fun reminders() {
        val raw = draft("local_reminders")["reminders_v1"] ?: return
        val entries = JSONObject(raw).optJSONArray("entries") ?: return
        val today = day.format(Calendar.getInstance().time)
        val oldest = day.format(Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -30) }.time)
        for (i in 0 until entries.length()) {
            val entry = entries.optJSONObject(i) ?: continue
            val date = entry.optString("date"); val title = entry.optString("title")
            if (title.isBlank() || !ReminderDate.valid(date) || date > today || date < oldest) continue
            val dueToday = date == today
            FinnestNotifications.post(applicationContext, FinnestNotifications.REMINDERS, "rem:${entry.optString("id")}:$date",
                if (dueToday) "Reminder due today" else "Reminder overdue", title, Intent(applicationContext, RemindersActivity::class.java))
        }
    }

    private suspend fun nudge() {
        val now = Calendar.getInstance()
        if (now.get(Calendar.HOUR_OF_DAY) !in 10..19) return // keep prompts to daytime
        val resale = applicationContext.getSharedPreferences("entry", Context.MODE_PRIVATE).getString("journey", "New Property") == "Resale Property"
        val scope = if (resale) "resale_property" else "new_property"
        val fields = draft(scope)
        val next = (if (resale) {
            (listOf("requirements" to "Requirements & location") + ResaleChecklists.sections.map { it.id to it.title })
                .firstOrNull { JourneyProgress.count(scope, it.first, fields).recorded == 0 }?.second
        } else {
            val ids = listOf("requirements", "developer", "projects", "visit", "loan", "legal", "financial", "review")
            ids.firstOrNull { JourneyProgress.count(scope, it, fields).recorded == 0 }
                ?.let { id -> DashboardSteps.all.firstOrNull { it.id == id }?.title }
        }) ?: return
        val week = "${now.get(Calendar.YEAR)}-${now.get(Calendar.WEEK_OF_YEAR)}"
        val journey = if (resale) "Resale Property" else "New Property"
        FinnestNotifications.post(applicationContext, FinnestNotifications.NUDGES, "nudge:$scope:$next:$week",
            "Next step: $next", "Pick up your $journey journey where you left off.",
            Intent(applicationContext, DashboardActivity::class.java).putExtra(DashboardActivity.SCOPE, scope))
    }

    private fun announcements() {
        val connection = URL(BuildConfig.GUIDANCE_URL + "/rest/v1/finnest_app_settings?select=payload&state=eq.published&limit=1").openConnection() as HttpURLConnection
        connection.connectTimeout = 5000; connection.readTimeout = 5000; connection.instanceFollowRedirects = false
        connection.setRequestProperty("apikey", BuildConfig.GUIDANCE_PUBLISHABLE_KEY)
        connection.setRequestProperty("Accept", "application/json")
        val text = try {
            if (connection.responseCode !in 200..299) return
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally { connection.disconnect() }
        val list = JSONArray(text).optJSONObject(0)?.optJSONObject("payload")?.optJSONArray("announcements") ?: return
        val iso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        val cutoff = System.currentTimeMillis() - 3L * 24 * 60 * 60 * 1000
        for (i in 0 until minOf(list.length(), 20)) {
            val item = list.optJSONObject(i) ?: continue
            val id = item.optString("id"); val title = item.optString("title").trim(); val body = item.optString("body").trim()
            val time = runCatching { iso.parse(item.optString("publishedAt").take(19))?.time }.getOrNull() ?: continue
            if (id.isBlank() || title.isEmpty() || body.isEmpty() || time < cutoff || time > System.currentTimeMillis() + 60_000) continue
            FinnestNotifications.post(applicationContext, FinnestNotifications.ANNOUNCEMENTS, "ann:$id", title, body, Intent(applicationContext, LaunchActivity::class.java))
        }
    }
}
