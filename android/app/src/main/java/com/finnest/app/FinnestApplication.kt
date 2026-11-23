package com.finnest.app

import android.app.Application

/** App-wide setup: the online-only guard on every screen, notification channels and the notification check. */
class FinnestApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(OnlineGuard)
        FinnestNotifications.createChannels(this)
        runCatching { FinnestNotifications.schedule(this) }
        // Admin App settings: live updates, and every open screen re-checks maintenance mode when they change.
        AppSettingsLive.addListener { OnlineGuard.recheckAll() }
        runCatching { AppSettingsLive.start(this) }
    }
}
