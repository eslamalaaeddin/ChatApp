package com.example.whatsapp.ui.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.whatsapp.R
import android.content.Intent
import android.os.Handler
import android.view.Window
import com.example.whatsapp.ui.ui.activities.MainActivity
import android.view.WindowManager
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {
    var handler: Handler? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        makeFullScreen()
        setContentView(R.layout.activity_splash)
        showLogoAndNavigate()
    }

    private fun makeFullScreen() {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
    }

    private fun showLogoAndNavigate() {
        handler = Handler()
        handler?.postDelayed({

            if (FirebaseAuth.getInstance().currentUser == null) {
                sendUserToPhoneLogInActivity()
            }
            else {
                startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            }

            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }, 2500)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler!!.removeCallbacksAndMessages(null)
    }

    private fun sendUserToPhoneLogInActivity() {
        val loginIntent = Intent(this , PhoneLogInActivity::class.java)
        loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(loginIntent)
        finish()
    }
}