package activities

import android.content.Intent
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import com.example.whatsapp.R
import com.example.whatsapp.Utils.USERS_CHILD
import com.example.whatsapp.databinding.ActivityCallingBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.squareup.picasso.Picasso

private const val RECEIVER_ID = "receiver id"
class CallingActivity : AppCompatActivity() {
    private lateinit var activityCallingBinding: ActivityCallingBinding

    //receiver info.
    private lateinit var receiverId:String
    private lateinit var receiverName:String
    private lateinit var receiverImageUrl:String

    //sender info.
    private lateinit var senderId:String
    private lateinit var senderName:String
    private lateinit var senderImageUrl:String

    private var checker = ""
    private var callingId = ""
    private var ringingId = ""

    private lateinit var mediaPlayer: MediaPlayer

    private lateinit var rootRef:DatabaseReference
    private lateinit var usersRef:DatabaseReference
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityCallingBinding = DataBindingUtil.setContentView(this, R.layout.activity_calling)
        rootRef = FirebaseDatabase.getInstance().reference
        usersRef = rootRef.child(USERS_CHILD)

        //senderId
        senderId = FirebaseAuth.getInstance().currentUser?.uid.toString()

        //receiverId
        receiverId = intent.getStringExtra(RECEIVER_ID).toString()

        getReceiverInfo()
        mediaPlayer = MediaPlayer.create(this,R.raw.ringing)

        //on click cancel button
        activityCallingBinding.cancelCallButton.setOnClickListener {
            checker = "clicked"
            cancelCall()
            mediaPlayer.stop()
        }

        //on click accept button
        activityCallingBinding.acceptCallButton.setOnClickListener {

            val pickedUpMap = HashMap<String,Any>()
            pickedUpMap.put("picked","picked")
            usersRef.child(senderId).child("Ringing").updateChildren(pickedUpMap).addOnCompleteListener {
                if (it.isComplete) {
                    mediaPlayer.stop()
                    val videoChatIntent = Intent(this,VideoChatActivity::class.java)
                    startActivity(videoChatIntent)
                }
            }
        }


    }
//
//    private fun cancelCall() {
//        //form sender
//        senderSide()
//
//        //from receiver
//        receiverSide()
//    }
//
//    override fun onStart() {
//        super.onStart()
//
////        //form sender
////        senderSide()
////
////        //from receiver
////        receiverSide()
//        mediaPlayer.start()
//
//        usersRef.child(receiverId).addListenerForSingleValueEvent(object : ValueEventListener{
//            override fun onDataChange(snapshot: DataSnapshot) {
//                if (checker != "clicked" && !snapshot.hasChild("Calling") && !snapshot.hasChild("Ringing")) {
//
//
//                    val callingInfo = HashMap<String,Any>()
//                    callingInfo.put("calling",receiverId )
//
//                    usersRef.child(senderId).child("Calling").updateChildren(callingInfo)
//                        .addOnCompleteListener {
//                            if (it.isSuccessful){
//                                val ringingInfo = HashMap<String,Any>()
//
//                                ringingInfo.put("ringing",senderId )
//
//                                usersRef.child(receiverId).child("Ringing").updateChildren(ringingInfo)
//                            }
//                        }
//
//                }
//                else{
//
//                }
//            }
//
//            override fun onCancelled(error: DatabaseError) {
//                TODO("Not yet implemented")
//            }
//        })
//
//        usersRef.addValueEventListener(object : ValueEventListener{
//            override fun onDataChange(snapshot: DataSnapshot) {
//                if (snapshot.child(senderId).hasChild("Ringing") && !snapshot.child(senderId).hasChild("Calling")){
//                    activityCallingBinding.acceptCallButton.visibility = View.VISIBLE
//                }
//
//                if (snapshot.child(receiverId).child("Ringing").hasChild("picked")){
//                    mediaPlayer.stop()
//                    val videoChatIntent = Intent(this@CallingActivity,VideoChatActivity::class.java)
//                    startActivity(videoChatIntent)
//                }
//            }
//
//            override fun onCancelled(error: DatabaseError) {
//                TODO("Not yet implemented")
//            }
//        })
//    }
//    //cancel from sender side
//    private fun senderSide() {
//        usersRef.child(senderId).child("Calling")
//            .addListenerForSingleValueEvent(object : ValueEventListener{
//                override fun onDataChange(snapshot: DataSnapshot) {
//                    if (snapshot.exists() && snapshot.hasChild("calling")){
//                        callingId = snapshot.child("calling").value.toString()
//
//                        usersRef.child(callingId).child("Ringing")
//                            .removeValue()
//                            .addOnCompleteListener {
//                                if (it.isSuccessful){
//                                    usersRef.child(senderId).child("Calling")
//                                        .removeValue()
//                                        .addOnCompleteListener {
//                                            // startActivity(Intent(this@CallingActivity,MainActivity::class.java))
//                                            receiverSide()
//                                            finish()
//                                        }
//                                }
//                            }
//
//                    }
//                    else{
//                        //startActivity(Intent(this@CallingActivity,MainActivity::class.java))
//                        finish()
//                    }
//                }
//
//                override fun onCancelled(error: DatabaseError) {
//                    TODO("Not yet implemented")
//                }
//            })
//    }
//
//    //cancel from receiver side
//    private fun receiverSide () {
//        usersRef.child(senderId).child("Ringing")
//            .addListenerForSingleValueEvent(object : ValueEventListener{
//                override fun onDataChange(snapshot: DataSnapshot) {
//                    if (snapshot.exists() && snapshot.hasChild("ringing")){
//                        ringingId = snapshot.child("ringing").value.toString()
//
//                        usersRef.child(ringingId).child("Calling")
//                            .removeValue()
//                            .addOnCompleteListener {
//                                if (it.isSuccessful){
//                                    usersRef.child(senderId).child("Ringing")
//                                        .removeValue()
//                                        .addOnCompleteListener {
//                                            // startActivity(Intent(this@CallingActivity,MainActivity::class.java))
//                                            senderSide()
//                                            finish()
//                                        }
//                                }
//                            }
//
//                    }
//                    else{
//                        //startActivity(Intent(this@CallingActivity,MainActivity::class.java))
//                        finish()
//                    }
//                }
//
//                override fun onCancelled(error: DatabaseError) {
//                    TODO("Not yet implemented")
//                }
//            })
//    }
//

    private fun getReceiverInfo() {
        usersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                //if receiver exists
                if (snapshot.child(receiverId).exists()) {
                    receiverName = snapshot.child(receiverId).child("name").value.toString()
                    receiverImageUrl = snapshot.child(receiverId).child("image").value.toString()

                    activityCallingBinding.callerNameTextView.text = receiverName

                    if (receiverImageUrl.isNotEmpty()) {
                        Picasso.get()
                            .load(receiverImageUrl)
                            .placeholder(R.drawable.dummy_avatar)
                            .into(activityCallingBinding.callerImageView)
                    }

                }

                //if sender exists
                if (snapshot.child(senderId).exists()) {
                    senderName = snapshot.child(senderId).child("name").value.toString()
                    senderImageUrl = snapshot.child(senderId).child("image").value.toString()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@CallingActivity, error.message, Toast.LENGTH_SHORT).show()
            }
        })
    }
}