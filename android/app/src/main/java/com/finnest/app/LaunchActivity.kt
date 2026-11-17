package com.finnest.app

import android.os.Bundle
import android.content.Intent
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class LaunchActivity: AppCompatActivity(){
    override fun onCreate(savedInstanceState: Bundle?){
        installSplashScreen();super.onCreate(savedInstanceState)
        val logo=android.widget.FrameLayout(this).apply {setBackgroundColor(FinnestUi.CREAM);alpha=0f;addView(FinnestUi.brand(this@LaunchActivity).apply{scaleX=1.5f;scaleY=1.5f},android.widget.FrameLayout.LayoutParams(-2,-2,android.view.Gravity.CENTER))}
        setContentView(logo);logo.animate().alpha(1f).setDuration(300).start()
        lifecycleScope.launch {
            val started=System.currentTimeMillis();val auth=AccountClient.get(this@LaunchActivity).auth
            val settings=PublicAppSettings.refresh(this@LaunchActivity)
            withTimeoutOrNull(5000) { runCatching {auth.awaitInitialization()};
                val current=auth.currentSessionOrNull()
                if(current!=null && current.expiresAt * 1000 <= System.currentTimeMillis())runCatching {auth.refreshCurrentSession()}
            };delay((1100-(System.currentTimeMillis()-started)).coerceAtLeast(0))
            if(auth.currentSessionOrNull()!=null)AccountSync.start(this@LaunchActivity)
            val prefs=getSharedPreferences("entry",MODE_PRIVATE)
            val phase=NativeLaunchRules.decide(auth.currentSessionOrNull()!=null,PublicAppSettings.completedVersion(this@LaunchActivity),settings.version,prefs.getBoolean("account_created",false),prefs.getBoolean("guest_session",false))
            val destination=if(phase==NativeLaunchPhase.ACCOUNT)AccountActivity::class.java else MainActivity::class.java
            startActivity(Intent(this@LaunchActivity,destination).putExtra("replay_onboarding",phase==NativeLaunchPhase.ONBOARDING).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK));finish()
        }
    }
}