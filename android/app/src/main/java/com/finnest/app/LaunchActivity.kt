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

class LaunchActivity: AppCompatActivity(){
    override fun onCreate(savedInstanceState: Bundle?){
        installSplashScreen();super.onCreate(savedInstanceState)
        val logo=ImageView(this).apply {setImageResource(R.drawable.finnest_logo);scaleType=ImageView.ScaleType.CENTER_INSIDE;setBackgroundColor(FinnestUi.CREAM);alpha=0f}
        setContentView(logo);logo.animate().alpha(1f).setDuration(300).start()
        lifecycleScope.launch {
            val started=System.currentTimeMillis();val auth=AccountClient.get(this@LaunchActivity).auth
            runCatching {auth.awaitInitialization()};delay((1100-(System.currentTimeMillis()-started)).coerceAtLeast(0))
            if(auth.currentSessionOrNull()!=null)AccountSync.start(this@LaunchActivity)
            val prefs=getSharedPreferences("entry",MODE_PRIVATE)
            val destination=if(!prefs.getBoolean("intro_complete",false))MainActivity::class.java else if(auth.currentSessionOrNull()!=null)MainActivity::class.java else AccountActivity::class.java
            startActivity(Intent(this@LaunchActivity,destination).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK));finish()
        }
    }
}