package ui.ui.activities

import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import com.example.whatsapp.R
import com.example.whatsapp.databinding.ActivityPhoneLogInBinding
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit
private const val PHONE_NUMBER = "phone number"
private const val TAG = "PhoneLogInActivity"
class PhoneLogInActivity : AppCompatActivity() {
    private lateinit var activityPhoneLogInBinding: ActivityPhoneLogInBinding
    private lateinit var storedVerificationId : String
    private lateinit var resendToken : PhoneAuthProvider.ForceResendingToken
    private lateinit var auth:FirebaseAuth
    private lateinit var callbacks : PhoneAuthProvider.OnVerificationStateChangedCallbacks

    private lateinit var progressDialog: ProgressDialog
    private lateinit var phoneNumber:String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activityPhoneLogInBinding = DataBindingUtil.setContentView(this ,
            R.layout.activity_phone_log_in
        )

        auth = FirebaseAuth.getInstance()

        activityPhoneLogInBinding.sendVerificationCodeButton.setOnClickListener {

            setVisibilityOnSuccess()

             phoneNumber = activityPhoneLogInBinding.phoneNumberEditText.editableText.toString()

            if (phoneNumber.isNotEmpty()) {
                Log.i(TAG, "onCreate: $phoneNumber")
                getLoadingDialog()
                PhoneAuthProvider.getInstance().verifyPhoneNumber(
                    phoneNumber,        // Phone number to verify
                    60,                 // Timeout duration
                    TimeUnit.SECONDS,   // Unit of timeout
                    this,               // Activity (for callback binding)
                    callbacks)
            }

            else{
                Toast.makeText(this, "Enter a valid phone number", Toast.LENGTH_SHORT).show()
            }

        }

        callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(p0: PhoneAuthCredential) {
                signInWithPhoneAuthCredential(p0)
            }

            override fun onVerificationFailed(p0: FirebaseException) {
                Toast.makeText(this@PhoneLogInActivity, p0.message.toString(), Toast.LENGTH_SHORT).show()
                setVisibilityOnVerificationFailure()
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                progressDialog.dismiss()
                storedVerificationId = verificationId
                resendToken = token
                setVisibilityOnSuccess()
                Toast.makeText(this@PhoneLogInActivity, "Code has been sent, please check your phone", Toast.LENGTH_SHORT).show()

            }
        }

        activityPhoneLogInBinding.verifyButton.setOnClickListener {
            activityPhoneLogInBinding.sendVerificationCodeButton.visibility = View.INVISIBLE
            activityPhoneLogInBinding.phoneNumberEditText.visibility = View.INVISIBLE

            val verificationCode = activityPhoneLogInBinding.verificationCodeEditText.editableText.toString()

            if (verificationCode.isNotEmpty()) {

                val credential = PhoneAuthProvider.getCredential(storedVerificationId, verificationCode)
                signInWithPhoneAuthCredential(credential)
            }
            else{
                Toast.makeText(this, "Please enter your verification code", Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun setVisibilityOnSuccess () {
        activityPhoneLogInBinding.phoneNumberEditText.visibility = View.INVISIBLE
        activityPhoneLogInBinding.verificationCodeEditText.visibility = View.VISIBLE
        activityPhoneLogInBinding.sendVerificationCodeButton.visibility = View.INVISIBLE
        activityPhoneLogInBinding.verifyButton.visibility = View.VISIBLE
    }

    private fun setVisibilityOnVerificationFailure () {
        activityPhoneLogInBinding.phoneNumberEditText.visibility = View.VISIBLE
        activityPhoneLogInBinding.verificationCodeEditText.visibility = View.INVISIBLE
        activityPhoneLogInBinding.sendVerificationCodeButton.visibility = View.VISIBLE
        activityPhoneLogInBinding.verifyButton.visibility = View.INVISIBLE
    }
    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->

                if (task.isSuccessful) {
                    progressDialog.dismiss()
                    Toast.makeText(this, "Congratulations!", Toast.LENGTH_SHORT).show()
                    sendUserToMainActivity()
                } else {
                    Toast.makeText(this, task.exception?.message, Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun getLoadingDialog() {
        progressDialog = ProgressDialog(this)
            .also {
                title = "Verifying you account"
                it.setMessage("Please be patient, We are verifying your account.")
                it.setCanceledOnTouchOutside(false)
                it.show()
            }
    }

    private fun sendUserToMainActivity() {
        val mainActivityIntent = Intent(this, MainActivity::class.java)
        mainActivityIntent.putExtra(PHONE_NUMBER,phoneNumber)
        startActivity(mainActivityIntent)
    }
}