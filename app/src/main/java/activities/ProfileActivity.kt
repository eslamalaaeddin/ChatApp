package activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import com.example.whatsapp.R
import com.example.whatsapp.databinding.ActivityFindFriendsBinding
import com.example.whatsapp.databinding.ActivityProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.squareup.picasso.Picasso
private const val USER_ID = "user id"
class ProfileActivity : AppCompatActivity() {
    private lateinit var activityProfileBinding: ActivityProfileBinding

    private lateinit var rootRef : DatabaseReference

    private lateinit var chatRequestRef : DatabaseReference

    private lateinit var auth: FirebaseAuth

    private lateinit var currentUserIdFromDb:String

    private lateinit var currentUserIdFromIntent:String

    private lateinit var currentRequestState :String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activityProfileBinding = DataBindingUtil.setContentView(this, R.layout.activity_profile)

        rootRef = FirebaseDatabase.getInstance().reference

        auth = FirebaseAuth.getInstance()

        chatRequestRef = rootRef.child("Chat requests")

        currentUserIdFromIntent = intent.getStringExtra(USER_ID).toString()

        currentUserIdFromDb = auth.currentUser?.uid.toString()

        currentRequestState = "new"

    }

    override fun onStart() {
        super.onStart()
        retrieveUserInfo()
    }

    private fun retrieveUserInfo() {
        rootRef.child("Users").child(currentUserIdFromIntent)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists() &&
                        snapshot.hasChild("name")

                       ) {

                        val userName = snapshot.child("name").value.toString()
                        val userStatus = snapshot.child("status").value.toString()

                        //if the user has an image
                        if ( snapshot.hasChild("image")){
                            val userImageUrl = snapshot.child("image").value.toString()
                            Picasso.get()
                                .load(userImageUrl)
                                .placeholder(R.drawable.dummy_avatar)
                                .into(activityProfileBinding.userImageView)
                        }


                        fillTextViews(userName, userStatus)


                    } else {
                        Toast.makeText(
                            this@ProfileActivity,
                            "Pasdasdasdasdasdasdasdasdasdas",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    manageChatRequest()
                }

                override fun onCancelled(error: DatabaseError) {
                    //TODO("Not yet implemented")
                }
            })
    }

    private fun manageChatRequest() {
        //if the user has sent a request
        chatRequestRef.child(currentUserIdFromDb).addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.hasChild(currentUserIdFromIntent)) {
                    val requestType = snapshot.child(currentUserIdFromIntent).child("requestType").value.toString()

                    if (requestType == "sent") {
                        currentRequestState = "request_sent"
                        activityProfileBinding.messageRequestButton.text = "Cancel chat request"
                    }

                    else{

                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })

        if ( currentUserIdFromDb != currentUserIdFromIntent) {
            //Send chat request
            activityProfileBinding.messageRequestButton.setOnClickListener { messageRequestButton ->
                messageRequestButton.isEnabled = false

                //check if the users are new to each others
                if (currentRequestState == "new") {
                    sendChatRequest()
                }

                if (currentRequestState == "request_sent") {

                }
            }
        }
        //if you are in self chat
        else{
            activityProfileBinding.messageRequestButton.visibility = View.INVISIBLE
        }
    }

   private fun sendChatRequest() {
       //currentUserIdFromDb == sender
       //currentUserIdFromIntent == receiver

       chatRequestRef.
       child(currentUserIdFromDb).
       child(currentUserIdFromIntent).
       child("requestType").
       setValue("sent").addOnCompleteListener { task ->

                   if (task.isSuccessful) {
                       chatRequestRef.
                       child(currentUserIdFromIntent).
                       child(currentUserIdFromDb).
                       child("requestType").
                       setValue("received").
                       addOnCompleteListener{task ->
                           if (task.isSuccessful) {
                               activityProfileBinding.messageRequestButton.apply {
                                   isEnabled = true
                                   currentRequestState = "request_sent"
                                   this.text = "Cancel chat request"
                               }

                           }
                       }
                   }
               }
    }


    private fun fillTextViews(userName: String, userStatus: String) {
        activityProfileBinding.userNameTextView.text = userName
        activityProfileBinding.userStatusTextView.text = userStatus
    }


}