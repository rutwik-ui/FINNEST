package com.finnest.app

import android.os.Bundle
import android.content.Intent
import android.text.InputType
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.launch

class AccountActivity: AppCompatActivity() {
    private var signup=false
    override fun onCreate(savedInstanceState: Bundle?){super.onCreate(savedInstanceState);render()}
    private fun render(){
        val root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL;setPadding(FinnestUi.dp(context,24),FinnestUi.dp(context,32),FinnestUi.dp(context,24),FinnestUi.dp(context,24));setBackgroundColor(FinnestUi.CREAM) }
        setContentView(ScrollView(this).apply { addView(root) })
        root.addView(FinnestUi.brand(this));root.addView(FinnestUi.h1(this,"Your journey, connected"));root.addView(FinnestUi.body(this,"Keep your plans and questions together in your FINNEST account."))
        val tabs=LinearLayout(this)
        for(isSignup in listOf(false,true)){tabs.addView(FinnestUi.primaryButton(this,if(isSignup)"Sign Up" else "Log In"){signup=isSignup;render()},LinearLayout.LayoutParams(0,-2,1f))};root.addView(tabs)
        val email=EditText(this).apply { hint="Email";contentDescription="Email";inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS }
        val password=EditText(this).apply { hint="Password";contentDescription="Password";inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD }
        root.addView(email);root.addView(password)
        val message=FinnestUi.body(this,"");root.addView(message)
        lateinit var submit: Button
        submit=FinnestUi.primaryButton(this,if(signup)"Create account" else "Log In"){
            val address=email.text.toString().trim();val secret=password.text.toString()
            if(!android.util.Patterns.EMAIL_ADDRESS.matcher(address).matches()){email.error="Enter a valid email address";return@primaryButton}
            if(secret.isEmpty()||(signup&&secret.length<8)){password.error="Use at least eight characters";return@primaryButton}
            submit.isEnabled=false;message.text="Please wait…"
            lifecycleScope.launch {
                try {
                    val auth=AccountClient.get(this@AccountActivity).auth
                    if(signup)auth.signUpWith(Email){this.email=address;this.password=secret} else auth.signInWith(Email){this.email=address;this.password=secret}
                    password.setText("")
                    if(auth.currentSessionOrNull()!=null){AccountSync.start(this@AccountActivity);startActivity(Intent(this@AccountActivity,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))}
                    else message.text="Account created. Email confirmation is still enabled for this service; check your inbox."
                }catch(_:Exception){message.text="Could not continue. Check your email, password and connection, then retry."}finally{submit.isEnabled=true}
            }
        };root.addView(submit)
        root.addView(FinnestUi.link(this,"Forgot password?"){
            val address=email.text.toString().trim();if(!android.util.Patterns.EMAIL_ADDRESS.matcher(address).matches()){email.error="Enter your email first";return@link}
            lifecycleScope.launch {try{AccountClient.get(this@AccountActivity).auth.resetPasswordForEmail(address);message.text="If an account exists, a reset link has been sent."}catch(_:Exception){message.text="Could not send reset link. Please retry online."}}
        })
        val guest=Button(this).apply {text="Continue as guest";isAllCaps=false;setOnClickListener {getSharedPreferences("entry",MODE_PRIVATE).edit().putBoolean("guest_session",true).apply();startActivity(Intent(this@AccountActivity,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))}}
        root.addView(guest);FinnestUi.styleTree(root)
    }
}