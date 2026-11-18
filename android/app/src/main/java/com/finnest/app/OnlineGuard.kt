package com.finnest.app

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/**
 * FINNEST is online-only. While the phone has no working connection (mobile data or Wi-Fi),
 * every screen is covered by a "No internet connection" view; it lifts by itself when the
 * connection returns. Screens stay underneath, so typing in progress is not lost.
 */
object OnlineGuard : Application.ActivityLifecycleCallbacks {
    private const val GATE_TAG = "finnest_offline_gate"
    private val main = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor()
    private val callbacks = mutableMapOf<Activity, ConnectivityManager.NetworkCallback>()

    /** True when the default network can reach the internet. Runs network I/O: call off the main thread. */
    fun hasInternet(context: Context): Boolean {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return true
        val caps = cm.getNetworkCapabilities(cm.activeNetwork ?: return false) ?: return false
        if (!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) return false
        if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) return true
        // Not yet validated by Android (for example right after switching networks): ask FINNEST's backend.
        return runCatching {
            val connection = URL(BuildConfig.GUIDANCE_URL + "/auth/v1/health").openConnection() as HttpURLConnection
            connection.connectTimeout = 4000; connection.readTimeout = 4000; connection.instanceFollowRedirects = false
            connection.setRequestProperty("apikey", BuildConfig.GUIDANCE_PUBLISHABLE_KEY)
            try { connection.responseCode in 200..499 } finally { connection.disconnect() }
        }.getOrDefault(false)
    }

    private fun refresh(activity: Activity) {
        worker.execute {
            val online = hasInternet(activity.applicationContext)
            main.post { if (!activity.isFinishing && !activity.isDestroyed) { if (online) hide(activity) else show(activity) } }
        }
    }

    override fun onActivityResumed(activity: Activity) {
        val cm = activity.getSystemService(ConnectivityManager::class.java) ?: return
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = refresh(activity)
            override fun onLost(network: Network) = refresh(activity)
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) = refresh(activity)
        }
        runCatching { cm.registerDefaultNetworkCallback(callback); callbacks[activity] = callback }
        refresh(activity)
    }

    override fun onActivityPaused(activity: Activity) {
        val callback = callbacks.remove(activity) ?: return
        runCatching { activity.getSystemService(ConnectivityManager::class.java)?.unregisterNetworkCallback(callback) }
    }

    private fun content(activity: Activity): ViewGroup? = activity.findViewById(android.R.id.content)

    private fun hide(activity: Activity) {
        val root = content(activity) ?: return
        root.findViewWithTag<View>(GATE_TAG)?.let { root.removeView(it) }
    }

    private fun show(activity: Activity) {
        val root = content(activity) ?: return
        if (root.findViewWithTag<View>(GATE_TAG) != null) return
        root.addView(gate(activity), FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }

    private fun gate(activity: Activity): View {
        val dp = { n: Int -> FinnestUi.dp(activity, n) }
        val cover = FrameLayout(activity).apply {
            tag = GATE_TAG
            setBackgroundColor(FinnestUi.CREAM)
            isClickable = true; isFocusable = true // swallow touches meant for the screen underneath
            elevation = FinnestUi.dpf(activity, 64f)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        }
        val card = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(28), dp(32), dp(28), dp(26))
            background = FinnestUi.shape(activity, Color.WHITE, FinnestUi.LINE, 16)
        }
        val badge = FrameLayout(activity).apply { background = FinnestUi.shape(activity, FinnestUi.BLUSH, null, 14) }
        badge.addView(ImageView(activity).apply {
            setImageResource(R.drawable.journey_wifi_off); imageTintList = ColorStateList.valueOf(FinnestUi.RED_DARK)
            contentDescription = null
        }, FrameLayout.LayoutParams(dp(26), dp(26), Gravity.CENTER))
        card.addView(badge, LinearLayout.LayoutParams(dp(56), dp(56)))
        card.addView(FinnestUi.label(activity, "No internet connection", 21f, 700, FinnestUi.NAVY, -0.015f).apply { gravity = Gravity.CENTER },
            LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(16) })
        card.addView(FinnestUi.body(activity, "FINNEST needs the internet to work. Turn on mobile data or Wi-Fi, then try again.", 14.5f).apply { gravity = Gravity.CENTER },
            LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })
        lateinit var status: TextView
        val retry = FinnestUi.primaryButton(activity, "Try again") {
            status.text = "Checking…"
            worker.execute {
                val online = hasInternet(activity.applicationContext)
                main.post { if (online) hide(activity) else status.text = "Still offline. We'll reconnect automatically." }
            }
        }
        card.addView(retry, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(20) })
        status = FinnestUi.label(activity, "We'll reconnect automatically when you're back online.", 12.5f, 400, FinnestUi.MUTED).apply { gravity = Gravity.CENTER }
        card.addView(status, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(14) })
        cover.addView(card, FrameLayout.LayoutParams(-1, -2, Gravity.CENTER).apply { marginStart = dp(24); marginEnd = dp(24) })
        cover.contentDescription = "No internet connection. FINNEST needs the internet to work."
        return cover
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) { callbacks.remove(activity) }
}
