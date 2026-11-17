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
    override fun onCreate(savedInstanceState: Bundle?){super.onCreate(savedInstanceState);signup=intent.getBooleanExtra("signup",false);render()}
    private fun render(){
        val c=this
        val page=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL;setBackgroundColor(FinnestUi.CREAM) }
        val header=LinearLayout(this).apply { gravity=Gravity.CENTER_VERTICAL;setPadding(FinnestUi.dp(c,16),0,FinnestUi.dp(c,16),0);background=android.graphics.drawable.LayerDrawable(arrayOf(android.graphics.drawable.ColorDrawable(FinnestUi.LINE),android.graphics.drawable.ColorDrawable(android.graphics.Color.WHITE))).apply{setLayerInset(1,0,0,0,FinnestUi.dp(c,1))} }
        header.addView(FinnestUi.brand(this));page.addView(header,LinearLayout.LayoutParams(-1,FinnestUi.dp(c,60)))
        val root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL;setPadding(FinnestUi.dp(c,20),FinnestUi.dp(c,28),FinnestUi.dp(c,20),FinnestUi.dp(c,32)) }
        page.addView(ScrollView(this).apply { addView(root) },LinearLayout.LayoutParams(-1,0,1f))
        setContentView(page)
        fun gap(n:Int)=LinearLayout.LayoutParams(-1,-2).apply{topMargin=FinnestUi.dp(c,n)}
        root.addView(FinnestUi.eyebrow(this,if(signup)"Create your account" else "Welcome back"))
        root.addView(FinnestUi.h1(this,if(signup)"Start your journey." else "Your journey, connected."),gap(10))
        root.addView(FinnestUi.body(this,"Keep your plans and questions together in your FINNEST account — on the app and the website."),gap(10))
        val form=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};root.addView(form,gap(24))
        val tabs=LinearLayout(this).apply{background=FinnestUi.shape(c,FinnestUi.CREAM,FinnestUi.LINE,12);setPadding(FinnestUi.dp(c,4),FinnestUi.dp(c,4),FinnestUi.dp(c,4),FinnestUi.dp(c,4))}
        for(isSignup in listOf(false,true)){val on=isSignup==signup
            tabs.addView(FinnestUi.label(this,if(isSignup)"Sign Up" else "Log In",15f,700,if(on)FinnestUi.RED_DARK else FinnestUi.MUTED).apply{gravity=Gravity.CENTER;background=if(on)FinnestUi.shape(c,FinnestUi.BLUSH,null,8) else null;isClickable=true;isFocusable=true;contentDescription=text;setOnClickListener{signup=isSignup;render()}},LinearLayout.LayoutParams(0,FinnestUi.dp(c,44),1f))}
        form.addView(tabs)
        val email=EditText(this).apply { hint="you@example.com";contentDescription="Email";inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS }
        val password=EditText(this).apply { hint=if(signup)"At least 8 characters" else "Your password";contentDescription="Password";inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD }
        form.addView(FinnestUi.label(this,"Email",14f,700,FinnestUi.LABEL),gap(20));form.addView(email,gap(8))
        form.addView(FinnestUi.label(this,"Password",14f,700,FinnestUi.LABEL),gap(16));form.addView(password,gap(8))
        val root0=root;val formRef=form
        val message=FinnestUi.body(this,"",13f);formRef.addView(message,gap(10))
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
                    if(auth.currentSessionOrNull()!=null){PublicAppSettings.complete(this@AccountActivity,false);getSharedPreferences("entry",MODE_PRIVATE).edit().putBoolean("account_created",true).apply();AccountSync.start(this@AccountActivity);startActivity(Intent(this@AccountActivity,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK));finish()}
                    else message.text="Account created. Email confirmation is still enabled for this service; check your inbox."
                }catch(_:Exception){message.text="Could not continue. Check your email, password and connection, then retry."}finally{submit.isEnabled=true}
            }
        };formRef.addView(submit,gap(14))
        if(!signup)formRef.addView(FinnestUi.link(this,"Forgot password?"){
            val address=email.text.toString().trim();if(!android.util.Patterns.EMAIL_ADDRESS.matcher(address).matches()){email.error="Enter your email first";return@link}
            lifecycleScope.launch {try{AccountClient.get(this@AccountActivity).auth.resetPasswordForEmail(address);message.text="If an account exists, a reset link has been sent."}catch(_:Exception){message.text="Could not send reset link. Please retry online."}}
        }.apply{gravity=Gravity.CENTER},gap(16))
        if(PublicAppSettings.cached(this).guestEnabled){
            val guest=com.google.android.material.button.MaterialButton(this).apply {strokeWidth=FinnestUi.dp(c,1);text="Continue as guest";isAllCaps=false;setOnClickListener {PublicAppSettings.complete(this@AccountActivity,true);startActivity(Intent(this@AccountActivity,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK));finish()}}
            root0.addView(guest,gap(24))
        };FinnestUi.styleTree(page)
    }
}