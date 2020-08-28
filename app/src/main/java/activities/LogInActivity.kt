package activities

import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import com.example.whatsapp.R
import com.example.whatsapp.Utils.DEVICE_TOKEN_CHILD
import com.example.whatsapp.Utils.USERS_CHILD
import com.example.whatsapp.databinding.ActivityLogInBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.iid.FirebaseInstanceId

private const val TAG = "LogInActivity"
class LogInActivity : AppCompatActivity() {


    private  var currentUser:FirebaseUser? = null
    //data binding instance
    private lateinit var activityLogInBinding : ActivityLogInBinding

    //firebase authentication instance
    private lateinit var auth: FirebaseAuth

    private lateinit var userRef:DatabaseReference

    //progress dialog
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityLogInBinding = DataBindingUtil.setContentView(this, R.layout.activity_log_in)

        auth = FirebaseAuth.getInstance()

        userRef = FirebaseDatabase.getInstance().reference.child(USERS_CHILD)

        activityLogInBinding.newAccountTextView.setOnClickListener {
            sendUserToRegisterActivity()
        }

        activityLogInBinding.logInButton.setOnClickListener {
            val mail = activityLogInBinding.mailEditText.editableText.toString()
            val password = activityLogInBinding.passwordEditText.editableText.toString()

            if (mail.isNotEmpty() && password.isNotEmpty()) {
                progressDialog = ProgressDialog(this)
                    .also {
                        title = "Verifying you account"
                        it.setMessage("Please be patient, We are verifying your account.")
                        it.setCanceledOnTouchOutside(false)
                        it.show()
                    }
                verifyAccount(mail,password)
            }

            else{
                Toast.makeText(this, "Invalid mail or password!", Toast.LENGTH_SHORT).show()
            }

        }

        activityLogInBinding.phoneLogInButton.setOnClickListener {
            sendUserToPhoneLogInActivity()
        }
    }


    private fun sendUserToMainActivity() {
        val mainActivityIntent = Intent(this, MainActivity::class.java)
        //to handle the back button logic
        mainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(mainActivityIntent)
        finish()
    }

    private fun sendUserToPhoneLogInActivity() {
        val phoneLogInIntent = Intent(this, PhoneLogInActivity::class.java)
        //to handle the back button logic
        startActivity(phoneLogInIntent)
    }

    private fun sendUserToRegisterActivity() {
        val registerIntent = Intent(this, RegisterActivity::class.java)
        startActivity(registerIntent)
    }

    private fun verifyAccount(mail:String, password:String) {
        auth.signInWithEmailAndPassword(mail,password)
            .addOnCompleteListener {task ->
                if (task.isSuccessful) {
                         startActivity(Intent(this, MainActivity::class.java))
                }
                else{
                    Toast.makeText(this, task.exception?.message, Toast.LENGTH_SHORT).show()
                }
                progressDialog.dismiss()
            }
    }
}