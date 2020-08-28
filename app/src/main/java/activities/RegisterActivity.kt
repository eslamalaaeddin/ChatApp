package activities

import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import com.example.whatsapp.R
import com.example.whatsapp.Utils
import com.example.whatsapp.Utils.DEVICE_TOKEN_CHILD
import com.example.whatsapp.Utils.USERS_CHILD
import com.example.whatsapp.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.iid.FirebaseInstanceId

class RegisterActivity : AppCompatActivity() {
    //data binding instance
    private lateinit var activityRegisterBinding : ActivityRegisterBinding

    //firebase authentication instance
    private lateinit var auth: FirebaseAuth

    //progress dialog
    private lateinit var progressDialog: ProgressDialog

    //Database reference
    private lateinit var rootRef:DatabaseReference
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activityRegisterBinding = DataBindingUtil.setContentView(this, R.layout.activity_register)
        //go to log in activity
        activityRegisterBinding.haveAccountTextView.setOnClickListener {
            sendUserToLoginActivity()
        }

        auth = FirebaseAuth.getInstance()

        rootRef = FirebaseDatabase.getInstance().reference

        //if create account button is clicked
        activityRegisterBinding.createAccountButton.setOnClickListener {
            val mail = activityRegisterBinding.mailEditText.editableText.toString()
            val password = activityRegisterBinding.passwordEditText.editableText.toString()

            if (mail.isNotEmpty() && password.isNotEmpty() ) {
                progressDialog = ProgressDialog(this)
                    .also {
                        title = "Creating new account"
                        it.setMessage("Please be patient, We are creating your account.")
                        it.setCanceledOnTouchOutside(true)
                        it.show()
                    }
                createAccount(mail,password)
            }

            else{
                Toast.makeText(this, "Invalid mail or password!", Toast.LENGTH_SHORT).show()
            }


        }
    }

    private fun sendUserToLoginActivity() {
        val loginIntent = Intent(this, LogInActivity::class.java)
        startActivity(loginIntent)
    }
    

    private fun createAccount(mail:String, password:String) {
        auth.createUserWithEmailAndPassword(mail,password)
            .addOnCompleteListener {task ->
                if (task.isSuccessful) {
                    val currentUserId = auth.currentUser?.uid.toString()

                    //create a (Users) object that contains all ids of the users
                    rootRef.child("Users").child(currentUserId).setValue("")

                    Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()
                    sendUserToMainActivity()
                }
                else{
                    Toast.makeText(this, task.exception?.message, Toast.LENGTH_SHORT).show()
                }
                progressDialog.dismiss()
            }
    }

    private fun sendUserToMainActivity() {
        val mainActivityIntent = Intent(this, MainActivity::class.java)
        //to handle the back button logic
        mainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(mainActivityIntent)
        finish()
    }


}