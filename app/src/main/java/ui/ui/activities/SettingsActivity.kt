package ui.ui.activities

import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.example.whatsapp.R
import com.example.whatsapp.databinding.ActivitySettingsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.squareup.picasso.Picasso
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView

private const val TAG = "SettingsActivity"
private const val PHONE_NUMBER = "phone number"
private const val GALLERY_PICK_NUMBER = 1
class SettingsActivity : AppCompatActivity() {
    private lateinit var activitySettingsBinding: ActivitySettingsBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var rootRef:DatabaseReference
    private lateinit var progressDialog: ProgressDialog
    private lateinit var userProfileImageReference: StorageReference

    private lateinit var currentUser: FirebaseUser
    private lateinit var phoneNumber:String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activitySettingsBinding = DataBindingUtil.setContentView(this, R.layout.activity_settings)

        setUpToolbar()

        auth = FirebaseAuth.getInstance()

        rootRef = FirebaseDatabase.getInstance().reference

        currentUser = auth.currentUser!!

        userProfileImageReference = FirebaseStorage.getInstance().reference.child("Profile images")

        phoneNumber = intent.getStringExtra(PHONE_NUMBER).toString()

        activitySettingsBinding.updateAccountButton.setOnClickListener {
            updateSettings()
        }

        //when clicking the image view

        activitySettingsBinding.pickPhotoImageView.setOnClickListener {
            val imageIntent = Intent()
            imageIntent.action = Intent.ACTION_GET_CONTENT
            imageIntent.type = "image/*"

            startActivityForResult(imageIntent, GALLERY_PICK_NUMBER)
        }

        retrieveUserInfo()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == GALLERY_PICK_NUMBER && resultCode == RESULT_OK && data != null) {
            val imageUri = data.data
            CropImage.activity()
                .setGuidelines(CropImageView.Guidelines.ON)
                .setAspectRatio(1, 1)
                .start(this);
        }

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            val result = CropImage.getActivityResult(data)

        if (resultCode == RESULT_OK) {

            getLoadingDialog()

            //to get the image uri
            val resultUri = result.uri
            val filePath = userProfileImageReference.child("${currentUser.uid}.jpg")
          filePath.putFile(resultUri).addOnCompleteListener { taskSnapshot ->
                if (taskSnapshot.isSuccessful) {

                   filePath.downloadUrl.addOnCompleteListener {task ->

                       if (task.isSuccessful) {
                           val imageUrl = task.result.toString()
                           rootRef.child("Users").child(currentUser.uid).child("image")
                               .setValue(imageUrl).addOnCompleteListener { task ->

                                   if (task.isSuccessful) {
                                   } else {
                                       Toast.makeText(this, task.exception?.message, Toast.LENGTH_SHORT).show()
                                   }
                               }
                       }

                       else{
                           Toast.makeText(this, task.exception?.message, Toast.LENGTH_SHORT).show()
                       }

                       progressDialog.dismiss()
                   }

                } else {
                    Toast.makeText(
                        this@SettingsActivity,
                        "profile image updated successfully",
                        Toast.LENGTH_SHORT
                    ).show()

                }
            }

        }
     }
    }

    private fun updateSettings() {
        val userName = activitySettingsBinding.userNameEditText.editableText.toString()
        val userStatus = activitySettingsBinding.userStatusEditText.editableText.toString()
        val userPhoneNumber = activitySettingsBinding.phoneNumberEditText.editableText.toString()

        if (userName.isNotEmpty() && userStatus.isNotEmpty()&& userPhoneNumber.isNotEmpty()) {
            currentUser = auth.currentUser!!
            val userMap = HashMap<String, Any>()
            userMap.put("uid", currentUser.uid)
            userMap.put("name", userName)
            userMap.put("status", userStatus)
            userMap.put("phoneNumber", userPhoneNumber)

            rootRef.child("Users").child(currentUser.uid).updateChildren(userMap)

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
                        val userImageUrl = snapshot.child("image").value.toString()
                        val userPhoneNumber = snapshot.child("phoneNumber").value.toString()

                        Picasso.get()
                            .load(userImageUrl)
                            .placeholder(R.drawable.ic_person)
                            .into(activitySettingsBinding.userImageView)

                        fillEditTexts(userName, userStatus,userPhoneNumber)
                    } else {
                        Toast.makeText(
                            this@SettingsActivity,
                            "Please update your information",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })
    }


    private fun fillEditTexts(userName: String, userStatus: String,userPhoneNumber:String) {
        activitySettingsBinding.userNameEditText.setText(userName)
        activitySettingsBinding.userStatusEditText.setText(userStatus)
        activitySettingsBinding.phoneNumberEditText.setText(userPhoneNumber)
    }

    private fun sendUserToMainActivity() {
        val mainActivityIntent = Intent(this, MainActivity::class.java)
        //to handle the back button logic
        mainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(mainActivityIntent)
        finish()
    }

    private fun getLoadingDialog() {
        progressDialog = ProgressDialog(this)
            .also {
                title = "Updating your account"
                it.setMessage("Please be patient, We are Updating your account.")
                it.setCanceledOnTouchOutside(false)
                it.show()
            }
    }

    private fun setUpToolbar() {
        setSupportActionBar(activitySettingsBinding.mainToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Settings"
        activitySettingsBinding.mainToolbar.setTitleTextColor(Color.WHITE)
        activitySettingsBinding.mainToolbar.overflowIcon?.setColorFilter(resources.getColor(R.color.white), PorterDuff.Mode.SRC_IN)
        activitySettingsBinding.mainToolbar.navigationIcon?.setColorFilter(resources.getColor(R.color.white), PorterDuff.Mode.SRC_IN)
    }
}