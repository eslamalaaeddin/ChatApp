package com.example.whatsapp

import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import com.example.whatsapp.databinding.ActivityLogInBinding
import com.example.whatsapp.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth

class RegisterActivity : AppCompatActivity() {
    //data binding instance
    private lateinit var activityRegisterBinding : ActivityRegisterBinding

    //firebase authentication instance
    private lateinit var auth: FirebaseAuth

    //progress dialog
    private lateinit var progressDialog: ProgressDialog
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activityRegisterBinding = DataBindingUtil.setContentView(this,R.layout.activity_register)
        //go to log in activity
        activityRegisterBinding.haveAccountTextView.setOnClickListener {
            sendUserToLoginActivity()
        }

        auth = FirebaseAuth.getInstance()

        //if create account button is clicked
        activityRegisterBinding.createAccountButton.setOnClickListener {
            val mail = activityRegisterBinding.mailEditText.editableText.toString()
            val password = activityRegisterBinding.passwordEditText.editableText.toString()
            progressDialog = ProgressDialog(this)
                .also {
                    title = "Creating new account"
                    it.setMessage("Please be patient, We are creating your account.")
                    it.setCanceledOnTouchOutside(true)
                    it.show()
                }
            createAccount(mail,password)
        }
    }

    private fun sendUserToLoginActivity() {
        val loginIntent = Intent(this,LogInActivity::class.java)
        startActivity(loginIntent)
    }

    private fun createAccount(mail:String, password:String) {
        auth.createUserWithEmailAndPassword(mail,password)
            .addOnCompleteListener {task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()
                }
                else{
                    Toast.makeText(this, task.exception?.message, Toast.LENGTH_SHORT).show()
                }
                progressDialog.dismiss()
            }
    }
}