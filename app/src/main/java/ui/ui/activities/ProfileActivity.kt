package ui.ui.activities

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


    private lateinit var contactsRef : DatabaseReference

    private lateinit var notificationReference: DatabaseReference

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

        contactsRef = rootRef.child("Contacts")

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
                                .placeholder(R.drawable.ic_person)
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
                    //should be called dynamically
                }

                override fun onCancelled(error: DatabaseError) {
                    //TODO("Not yet implemented")
                }
            })
    }

    private fun manageChatRequest() {
        //if the user has sent a request
        chatRequestRef.child(currentUserIdFromDb).addValueEventListener(object :
            ValueEventListener {
            //check if we are not friends yet --> sender side
            //check if we are not friends yet --> receiver side
            //check if we are friends --> both sides
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.hasChild(currentUserIdFromIntent)) {
                    val requestType = snapshot.child(currentUserIdFromIntent)
                        .child("requestType").value.toString()
                    /*
                        Sender side
                     */
                    if (requestType == "sent") {
                        currentRequestState = "request_sent"
                        activityProfileBinding.messageRequestButton.apply {
                            text = "Cancel chat request"
                            setOnClickListener {
                                cancelChatRequest()
                            }
                        }
                    }
                    /*
                        receiver side
                     */
                    else if (requestType == "received") {
                        currentRequestState = "request_received"

                        //Accept chat request
                        activityProfileBinding.messageRequestButton.apply {
                            text = "Accept chat request"
                            isEnabled = true
                        }
                        //Decline chat request
                        activityProfileBinding.messageDeclineButton.apply {
                            visibility = View.VISIBLE
                            text = "Decline chat request"
                            isEnabled = true
                            setOnClickListener {
                                declineChatRequest()
                                //المفروض يحدث عندي ابدل كانسيل يبقى سيند تاني لانه رفض/
                            }
                        }

                    }
                }

                //already friends
                else {
                    contactsRef.child(currentUserIdFromDb).addListenerForSingleValueEvent(object :
                        ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            //we are friends?
                            if (snapshot.hasChild(currentUserIdFromIntent)) {
                                currentRequestState = "friends"
                                activityProfileBinding.messageRequestButton.text = "Remove this contact"
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Toast.makeText(this@ProfileActivity, error.message, Toast.LENGTH_SHORT).show()
                        }
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ProfileActivity, error.message, Toast.LENGTH_SHORT).show()
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
                //check if the user has already sent a request
                if (currentRequestState == "request_sent") {
                    declineChatRequest()
                }
                //check if the user has already received a request
                if (currentRequestState == "request_received") {
                   acceptChatRequest()
                }

                //check if the users are already friends
                if (currentRequestState == "friends") {
                    removeContact()
                }

            }
        }
        //if you are in your profile
        else{
            activityProfileBinding.messageRequestButton.visibility = View.INVISIBLE
        }
    }

    private fun declineChatRequest() {
        //receiver side
        chatRequestRef.child(currentUserIdFromIntent).child(currentUserIdFromDb)
            .removeValue().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    //Sender side
                    chatRequestRef.child(currentUserIdFromDb).child(currentUserIdFromIntent)
                    .removeValue()

                    activityProfileBinding.messageRequestButton.isEnabled = true
                    activityProfileBinding.messageRequestButton.text = "Send message request"
                    currentRequestState = "new"

                    //can be subistituted with block this user
                    activityProfileBinding.messageDeclineButton.isEnabled = false
                    activityProfileBinding.messageDeclineButton.visibility = View.INVISIBLE
                }
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

                               val requestChatNotificationMap = HashMap<String,String>()
                               requestChatNotificationMap.put("from",currentUserIdFromDb)
                               requestChatNotificationMap.put("type","request")

                               notificationReference = rootRef.child("Notifications")

                               notificationReference.child(currentUserIdFromIntent).push() .setValue(requestChatNotificationMap)
                                   .addOnCompleteListener {task ->
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
               }
    }

    private fun acceptChatRequest () {
        //to be shown that each one (sender and receiver) are now in contact
         contactsRef.child(currentUserIdFromDb).child(currentUserIdFromIntent).child("Contacts").setValue("saved")
             .addOnCompleteListener {task ->
                 if (task.isSuccessful) {
                     contactsRef.child(currentUserIdFromIntent).child(currentUserIdFromDb).child("Contacts").setValue("saved")
                         .addOnCompleteListener {task ->
                             if (task.isSuccessful) {
                                    //remove the chat request as both sender and receiver are now friends
                                    chatRequestRef.
                                    child(currentUserIdFromDb).
                                    child(currentUserIdFromIntent).
                                    removeValue()
                                        .addOnCompleteListener {task ->
                                            if (task.isSuccessful) {
                                                chatRequestRef.
                                                child(currentUserIdFromIntent).
                                                child(currentUserIdFromDb).
                                                removeValue()
                                                    .addOnCompleteListener {task ->
                                                        activityProfileBinding.messageRequestButton.apply {
                                                            isEnabled = true
                                                            currentRequestState = "friends"
                                                            text = "Remove this contact"
                                                            Toast.makeText(
                                                                this@ProfileActivity,
                                                                "Added to your contacts",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        }

                                                        activityProfileBinding.messageDeclineButton.apply {
                                                            visibility = View.INVISIBLE
                                                        }
                                                    }
                                            }
                                        }
                             }
                         }
                 }
             }
    }

    private fun removeContact(){
        contactsRef.child(currentUserIdFromDb).child(currentUserIdFromIntent)
            .removeValue().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    contactsRef.child(currentUserIdFromIntent).child(currentUserIdFromDb)
                        .removeValue().addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                activityProfileBinding.messageRequestButton.isEnabled = true
                                activityProfileBinding.messageRequestButton.text = "Send message request"
                                currentRequestState = "new"

                                activityProfileBinding.messageDeclineButton.isEnabled = false
                                activityProfileBinding.messageDeclineButton.visibility = View.INVISIBLE
                            }
                        }
                }
            }
    }

    private fun cancelChatRequest() {
        //sender side
        chatRequestRef.child(currentUserIdFromDb).child(currentUserIdFromIntent)
            .removeValue().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    //Sender side
                    chatRequestRef.child(currentUserIdFromIntent).child(currentUserIdFromDb)
                        .removeValue()

                    activityProfileBinding.messageRequestButton.isEnabled = true
                    activityProfileBinding.messageRequestButton.text = "Send message request"
                    currentRequestState = "new"

                    //can be subistituted with block this user
                    activityProfileBinding.messageDeclineButton.isEnabled = false
                    activityProfileBinding.messageDeclineButton.visibility = View.INVISIBLE
                }
            }


    }


    private fun fillTextViews(userName: String, userStatus: String) {
        activityProfileBinding.userNameTextView.text = userName
        activityProfileBinding.userStatusTextView.text = userStatus
    }


}