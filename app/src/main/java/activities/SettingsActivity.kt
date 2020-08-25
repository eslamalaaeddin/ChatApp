package activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import com.example.whatsapp.R
import com.example.whatsapp.databinding.ActivitySettingsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
private const val GALLERY_PICK_NUMBER = 1
class SettingsActivity : AppCompatActivity() {
    private lateinit var activitySettingsBinding: ActivitySettingsBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var rootRef:DatabaseReference

    private lateinit var currentUser: FirebaseUser
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        rootRef = FirebaseDatabase.getInstance().reference

        currentUser = auth.currentUser!!

        activitySettingsBinding = DataBindingUtil.setContentView(this, R.layout.activity_settings)

        activitySettingsBinding.updateAccountButton.setOnClickListener {
            updateSettings()
        }

        //when clicking the image view

        activitySettingsBinding.userImageView.setOnClickListener {
            val imageIntent = Intent()
            imageIntent.action = Intent.ACTION_GET_CONTENT
            imageIntent.type = "image/*"

            startActivityForResult(imageIntent, GALLERY_PICK_NUMBER)
        }

        retrieveUserInfo()
    }

    private fun updateSettings() {
        val userName = activitySettingsBinding.userNameEditText.editableText.toString()
        val userStatus = activitySettingsBinding.userStatusEditText.editableText.toString()

        if (userName.isNotEmpty() && userStatus.isNotEmpty()) {
            currentUser = auth.currentUser!!
            val userMap = HashMap<String,String>()
            userMap.put("uid",currentUser.uid)
            userMap.put("name",userName)
            userMap.put("status",userStatus)

            rootRef.child("Users").child(currentUser.uid).setValue(userMap)

                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                        sendUserToMainActivity()
                    }

                    else{
                        Toast.makeText(this, "${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
        else{
            Toast.makeText(this, "Invalid user name or status", Toast.LENGTH_SHORT).show()
        }
    }

    private fun retrieveUserInfo() {
        rootRef.child("Users").child(currentUser.uid)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists() && snapshot.hasChild("name") && snapshot.hasChild("status")) {
                        val userName = snapshot.child("name").value.toString()
                        val userStatus = snapshot.child("status").value.toString()

                        fillEditTexts(userName, userStatus)
                    } else {
                        Toast.makeText(this@SettingsActivity, "Please update your information", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }
            })
    }


    private fun fillEditTexts(userName:String , userStatus:String) {
        activitySettingsBinding.userNameEditText.setText(userName)
        activitySettingsBinding.userStatusEditText.setText(userStatus)
    }

    private fun sendUserToMainActivity() {
        val mainActivityIntent = Intent(this, MainActivity::class.java)
        //to handle the back button logic
        mainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(mainActivityIntent)
        finish()
    }
}