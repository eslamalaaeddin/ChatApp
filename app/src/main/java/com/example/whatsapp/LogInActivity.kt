package com.example.whatsapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import com.example.whatsapp.databinding.ActivityLogInBinding
import com.google.firebase.auth.FirebaseUser

class LogInActivity : AppCompatActivity() {


    private  var currentUser:FirebaseUser? = null
    //data binding instance
    private lateinit var activityLogInBinding : ActivityLogInBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
       activityLogInBinding = DataBindingUtil.setContentView(this,R.layout.activity_log_in)

        activityLogInBinding.newAccountTextView.setOnClickListener {
            sendUserToRegisterActivity()
        }
    }

    override fun onStart() {
        super.onStart()
        if(currentUser != null) {
            sendUserToMainActivity()
        }
    }

    private fun sendUserToMainActivity() {
        val mainIntent = Intent(this,MainActivity::class.java)
        startActivity(mainIntent)
    }

    private fun sendUserToRegisterActivity() {
        val registerIntent = Intent(this,RegisterActivity::class.java)
        startActivity(registerIntent)
    }
}