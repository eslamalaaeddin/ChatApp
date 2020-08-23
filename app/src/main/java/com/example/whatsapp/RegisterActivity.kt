package com.example.whatsapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import com.example.whatsapp.databinding.ActivityLogInBinding
import com.example.whatsapp.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {
    private lateinit var activityRegisterBinding : ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityRegisterBinding = DataBindingUtil.setContentView(this,R.layout.activity_register)
        activityRegisterBinding.haveAccountTextView.setOnClickListener {
            sendUserToLoginActivity()
        }
    }

    private fun sendUserToLoginActivity() {
        val loginIntent = Intent(this,LogInActivity::class.java)
        startActivity(loginIntent)
    }
}