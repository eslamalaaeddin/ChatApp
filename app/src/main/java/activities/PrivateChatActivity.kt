package activities

import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.whatsapp.PrivateMessageModel
import com.example.whatsapp.R
import com.example.whatsapp.Utils
import com.example.whatsapp.Utils.DEVICE_TOKEN_CHILD
import com.example.whatsapp.Utils.MESSAGES_CHILD
import com.example.whatsapp.Utils.USERS_CHILD
import com.example.whatsapp.databinding.ActivityPrivateChatBinding
import com.example.whatsapp.databinding.PrivateMessageLayoutBinding
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageTask
import com.google.firebase.storage.UploadTask
import com.google.gson.Gson
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import notifications.NotificationData
import notifications.PushNotification
import notifications.RetrofitInstance
import okhttp3.internal.Util
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

private const val USER_ID = "user id"
private const val USER_NAME = "user name"
private const val USER_IMAGE = "user image"
private const val IMAGE_REQUEST_CODE = 1
private const val TAG = "PrivateChatActivity"
class PrivateChatActivity : AppCompatActivity() {

    private lateinit var activityPrivateChatBinding: ActivityPrivateChatBinding

    private lateinit var privateMessageLayoutBinding: PrivateMessageLayoutBinding

    private lateinit var auth: FirebaseAuth
    private lateinit var senderId:String
    private lateinit var receiverId:String

    private lateinit var currentUser : FirebaseUser
    private lateinit var currentUserId : String

    private lateinit var senderToken:String
    private lateinit var receiverToken:String

    private lateinit var rootRef: DatabaseReference

    private lateinit var usersRef: DatabaseReference

    private lateinit var currentUserName: String

    private lateinit var messageSenderId : String
    private lateinit var messageReceiverId : String

    private lateinit var currentMessage:String
    private lateinit var currentDate :String
    private lateinit var currentTime :String

    private lateinit var userImageView: ImageView
    private lateinit var userNameTextView: TextView
    private lateinit var lastSeenTextView: TextView

    private lateinit var progressDialog: ProgressDialog


    private var checker:String = ""
    private var url:String = ""
    private lateinit var fileUri:Uri


    private var backPressed = false

    private  var messagesAdapter = PrivateMessagesAdapter(emptyList())

    private var messagesList = mutableListOf<PrivateMessageModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityPrivateChatBinding =
            DataBindingUtil.setContentView(this,R.layout.activity_private_chat)

        auth = FirebaseAuth.getInstance()

        senderId = auth.currentUser?.uid.toString()
        receiverId = intent.getStringExtra(USER_ID).toString()

        Utils.senderId = senderId

        rootRef = FirebaseDatabase.getInstance().reference

        usersRef = rootRef.child(USERS_CHILD)

        currentUser = auth.currentUser!!

        currentUserId = currentUser.uid

        setUpToolbar()

        getTokens()

        getSenderName()

        activityPrivateChatBinding.sendMessageButton.setOnClickListener {
            sendMessage()
            pushNotification()
        }

        messagesAdapter = PrivateMessagesAdapter(messagesList)
        activityPrivateChatBinding.privateChatRecyclerView.adapter = messagesAdapter
        val linearLayout = LinearLayoutManager(this)
      //  linearLayout.reverseLayout = true
        activityPrivateChatBinding.privateChatRecyclerView.layoutManager = linearLayout
        activityPrivateChatBinding.privateChatRecyclerView.scrollToPosition(messagesAdapter.itemCount -1)

        activityPrivateChatBinding.attachFileButton.setOnClickListener {
            val options = arrayOf("images","PDF","MS Word")

            val alertDialogBuilder = AlertDialog.Builder(this)
                .setTitle("Choose file type")
                .setItems(options,object : DialogInterface.OnClickListener{
                    override fun onClick(p0: DialogInterface?, i: Int) {
                       when(i) {
                           //images
                           0 -> {
                               checker = "image"
                               val imagesIntent = Intent(Intent.ACTION_GET_CONTENT)
                               imagesIntent.type = "image/*"
                                startActivityForResult(Intent.createChooser(imagesIntent,"Choose an image"),
                                    IMAGE_REQUEST_CODE)
                           }
                           //PDF
                           1 -> {
                               checker = "pdf"
                           }
                           //MS Word
                           2 -> {
                               checker= "docx"
                           }
                       }
                    }
                })
                getLoadingDialog()
        }



    }

//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        if (requestCode == IMAGE_REQUEST_CODE && resultCode == RESULT_OK && data!=null && data.data!=null) {
//
//          getLoadingDialog()
//
//            fileUri = data.data!!
//
//            if (checker!="image") {
//
//            }
//            else if (checker =="image") {
//                val storageRef = FirebaseStorage.getInstance().reference.child("Image files")
//
//                val messageSenderRef = "${MESSAGES_CHILD}/$senderId/$receiverId"
//                val messageReceiverRef = "${MESSAGES_CHILD}/$receiverId/$senderId"
//
//                val userMessageKeyRef = rootRef.child(MESSAGES_CHILD).
//                child(senderId).child(receiverId).push()
//
//                val messagePushId = userMessageKeyRef.key.toString()
//
//                val filePath = storageRef.child("$messagePushId.jpg")
//                val uploadTask = filePath.putFile(fileUri)
//
//                uploadTask.continueWithTask(object :
//                    Continuation<UploadTask.TaskSnapshot, Task<Uri>> {
//                    override fun then(task: Task<UploadTask.TaskSnapshot>): Task<Uri> {
//                        if(!task.isSuccessful)
//                        {
//                            throw task.exception!!
//                        }
//                        return filePath.downloadUrl
//                    }
//                }).addOnCompleteListener {
//                    if (it.isSuccessful) {
//                        url = it.result.toString()
//
//                        val messageImageBody = HashMap<String,Any>()
//                        messageImageBody.put("message",url)
//                        messageImageBody.put("name",fileUri.lastPathSegment.toString())
//                        messageImageBody.put("type",checker)
//                        messageImageBody.put("from",senderId)
//                        messageImageBody.put("to",receiverId)
//                        messageImageBody.put("messageKey",messagePushId)
//                        messageImageBody.put("date",currentDate)
//                        messageImageBody.put("time",currentTime)
//
//                        val messageBodyDetails = HashMap<String,Any>()
//                        messageBodyDetails.put("$messageSenderRef/$messagePushId",messageImageBody)
//                        messageBodyDetails.put("$messageReceiverRef/$messagePushId",messageImageBody)
//
//                        rootRef.updateChildren(messageBodyDetails).addOnCompleteListener {task ->
//                            if (!task.isSuccessful) {
//                                Toast.makeText(this, task.exception?.message, Toast.LENGTH_SHORT).show()
//
//                            }
//                        }
//
//                    }
//                }
//
//            }
//            else{
//                Toast.makeText(this, "Choose a valid attachment", Toast.LENGTH_SHORT).show()
//            }
//        }
//        progressDialog.dismiss()
//    }


    override fun onStart() {
        super.onStart()
        rootRef.child(MESSAGES_CHILD).child(senderId).child(receiverId).addChildEventListener(
            object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    val ourMessages = snapshot.getValue(PrivateMessageModel::class.java)

                    if (ourMessages != null) {
                        messagesList.add(ourMessages)
                        messagesAdapter.notifyDataSetChanged()
                        //to scroll to the bottom of recycler view
                        if (messagesList.isNotEmpty()) {
                            activityPrivateChatBinding.privateChatRecyclerView.smoothScrollToPosition(
                                messagesList.size - 1
                            )
                        }
                    }
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {
                }

                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })

        updateUserStatus("online")
        displayLastSeen()
    }


    private fun setUpToolbar() {

        val toolbarView = LayoutInflater.from(this).inflate(R.layout.custom_toolbar,null)

        setSupportActionBar(activityPrivateChatBinding.mainToolbar)
       // supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowCustomEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        supportActionBar?.customView = toolbarView

        Log.i(TAG, "WWW setUpToolbar: $senderId")
        Log.i(TAG, "WWW setUpToolbar: $receiverId")


        userImageView = findViewById(R.id.user_image_view)
        userNameTextView = findViewById(R.id.user_name_text_view)
        lastSeenTextView = findViewById(R.id.user_last_seen)

            usersRef.child(receiverId).addValueEventListener(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {

                    userNameTextView.text = snapshot.child("name").value.toString()

                    val imageUrl = snapshot.child("image").value.toString()
                    if (imageUrl.isNotEmpty()) {
                        Picasso.get()
                            .load(imageUrl)
                            .placeholder(R.drawable.dummy_avatar)
                            .into(userImageView)
                    }

                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }
            })



//        activityPrivateChatBinding.mainToolbar.setTitleTextColor(Color.WHITE)
//        activityPrivateChatBinding.mainToolbar.overflowIcon?.setColorFilter(resources.getColor(R.color.white), PorterDuff.Mode.SRC_IN)
      //  activityPrivateChatBinding.mainToolbar.navigationIcon?.setColorFilter(resources.getColor(R.color.white), PorterDuff.Mode.SRC_IN)
    }


    private fun sendMessage() {
         currentMessage = activityPrivateChatBinding.sendMessageEditText.editableText.toString()

        if(currentMessage.isNotEmpty()) {
            val messageSenderRef = "${MESSAGES_CHILD}/$senderId/$receiverId"
            val messageReceiverRef = "${MESSAGES_CHILD}/$receiverId/$senderId"

            val userMessageKeyRef = rootRef.child(MESSAGES_CHILD).
            child(senderId).child(receiverId).push()

            val messagePushId = userMessageKeyRef.key.toString()

            val calender = Calendar.getInstance()
            //get date and time
            val dateFormat = SimpleDateFormat("MMM dd, yyyy")
            val timeFormat = SimpleDateFormat("hh:mm a")

             currentDate = dateFormat.format(calender.time)
             currentTime = timeFormat.format(calender.time)

            val messageTextBody = HashMap<String,Any>()
            messageTextBody.put("message",currentMessage)
            messageTextBody.put("type","text")
            messageTextBody.put("from",senderId)
            messageTextBody.put("to",receiverId)
            messageTextBody.put("messageKey",messagePushId)
            messageTextBody.put("date",currentDate)
            messageTextBody.put("time",currentTime)

            val messageBodyDetails = HashMap<String,Any>()
            messageBodyDetails.put("$messageSenderRef/$messagePushId",messageTextBody)
            messageBodyDetails.put("$messageReceiverRef/$messagePushId",messageTextBody)

            rootRef.updateChildren(messageBodyDetails).addOnCompleteListener {task ->
                if (!task.isSuccessful) {
                    Toast.makeText(this, task.exception?.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        else{

        }
        activityPrivateChatBinding.sendMessageEditText.text.clear()
    }

    private fun getTokens() {
        usersRef.child(senderId).child(DEVICE_TOKEN_CHILD).addValueEventListener(object :
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                senderToken = snapshot.value.toString()
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })
        usersRef.child(receiverId).child(DEVICE_TOKEN_CHILD).addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    receiverToken = snapshot.value.toString()
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }
            })
    }

    private fun pushNotification(){
      val notification =   PushNotification(
            NotificationData(currentUserName, currentMessage,Utils.senderId), receiverToken)
        if (currentMessage.isNotEmpty()){
            sendNotification(notification)
        }
    }

    private fun sendNotification(notification: PushNotification) = CoroutineScope(Dispatchers.IO).launch {
        try {
            val response = RetrofitInstance.api.postNotification(notification)
            if(response.isSuccessful) {
                Log.d(TAG, "Response: ${Gson().toJson(response)}")
            } else {
                Log.e(TAG, response.errorBody().toString())
            }
        } catch(e: Exception) {
            Log.e(TAG, e.toString())
        }
    }

    private fun getSenderName(){
        usersRef.child(senderId).child("name").addValueEventListener(object :ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                currentUserName =  snapshot.value.toString()
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })

    }

    private fun displayLastSeen(){
        rootRef.child("Users").child(receiverId).addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {

                val time = snapshot.child("state").child("time").value.toString()
                val date = snapshot.child("state").child("date").value.toString()
                val state = snapshot.child("state").child("state").value.toString()

                if (state == "offline") {

                    lastSeenTextView.text =  "Last seen: $time, $date"
                }
                else if (state == "online") {
                    lastSeenTextView.apply {
                        text = state
                        //setBackgroundResource(R.color.colorPrimary)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })
    }

    private fun updateUserStatus (state:String) {
        var currentDate = ""
        var currentTime = ""

        val calender = Calendar.getInstance()
        //get date and time
        val dateFormat = SimpleDateFormat("MMM dd, yyyy")
        val timeFormat = SimpleDateFormat("hh:mm a")

        currentDate = dateFormat.format(calender.time)
        currentTime = timeFormat.format(calender.time)

        val userStateMap = HashMap<String,Any> ()
        userStateMap.put("date",currentDate)
        userStateMap.put("time",currentTime)
        userStateMap.put("state",state)

        rootRef.child(USERS_CHILD).child(currentUserId).child(Utils.STATE_CHILD).updateChildren(userStateMap)


    }

//    private fun getLoadingDialog() {
//        progressDialog = ProgressDialog(this)
//            .also {
//                title = "Attachment is uploading"
//                it.setCanceledOnTouchOutside(false)
//                it.show()
//            }
//    }

    override fun onStop() {
        super.onStop()
        if (backPressed){

        }
        else{
              updateUserStatus("offline")
        }

    }

    override fun onBackPressed() {
        super.onBackPressed()
          backPressed = true
    }


   inner class PrivateMessagesAdapter (private val messages:List<PrivateMessageModel>) :
        RecyclerView.Adapter<PrivateMessagesAdapter.PopularMoviesViewHolder>() {

        inner class PopularMoviesViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
             val senderMessageTextView : TextView = itemView.findViewById(R.id.sender_message_text)
             val receiverMessageTextView : TextView = itemView.findViewById(R.id.receiver_message_text)
             val senderMessageTimeTextView:TextView = itemView.findViewById(R.id.sender_time_sent_text_view)
             val receiverMessageTimeTextView:TextView = itemView.findViewById(R.id.receiver_time_sent_text_view)
             val senderMessageImageView : ImageView = itemView.findViewById(R.id.sender_message_image_view)
             val receiverMessageImageView : ImageView = itemView.findViewById(R.id.receiver_message_image_view)

            val senderMessageLayout : LinearLayout = itemView.findViewById(R.id.sender_message_layout)
            val receiverMessageLayout : LinearLayout = itemView.findViewById(R.id.receiver_message_layout)



            init {
                itemView.setOnClickListener(this)
            }

            fun bind(messageModel: PrivateMessageModel) {
                senderMessageTextView.text = messageModel.message
            }

            override fun onClick(item: View?) {

            }

        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PopularMoviesViewHolder {

            val view = LayoutInflater.from(parent.context).inflate(R.layout.private_message_layout,parent,false)
            return PopularMoviesViewHolder(view)
        }

        override fun getItemCount(): Int {
            return messages.size
        }

        override fun onBindViewHolder(holder: PopularMoviesViewHolder, position: Int) {

             messageSenderId = auth.currentUser?.uid.toString()

            val myMessages = messages[position]

            val fromUserId = myMessages.from
            val fromMessagesType = myMessages.type

            usersRef = rootRef.child(Utils.USERS_CHILD).child(fromUserId)


            if (fromMessagesType == "text") {
                holder.senderMessageImageView.visibility = View.GONE
                holder.receiverMessageImageView.visibility = View.GONE

                if (fromUserId == messageSenderId) {
                    holder.senderMessageTextView.setBackgroundResource(R.drawable.sender_messages_background)
                    holder.senderMessageTextView.text = myMessages.message
                    holder.senderMessageTimeTextView.text = myMessages.time
                    holder.senderMessageTextView.visibility = View.VISIBLE
                    holder.senderMessageLayout.visibility = View.VISIBLE
                    holder.senderMessageTimeTextView.visibility = View.VISIBLE


                    holder.receiverMessageTextView.visibility = View.GONE
                    holder.receiverMessageLayout.visibility = View.GONE
                    holder.receiverMessageTimeTextView.visibility = View.GONE

                }

                else{
                    holder.senderMessageTextView.visibility = View.GONE
                    holder.senderMessageLayout.visibility = View.GONE
                    holder.senderMessageTimeTextView.visibility = View.GONE

                    holder.receiverMessageTextView.visibility = View.VISIBLE
                    holder.receiverMessageLayout.visibility = View.VISIBLE
                    holder.receiverMessageTimeTextView.visibility = View.VISIBLE

                    holder.receiverMessageTextView.setBackgroundResource(R.drawable.receiver_messages_background)
                    holder.receiverMessageTextView.text = myMessages.message
                    holder.receiverMessageTimeTextView.text = myMessages.time
                }
            }
        }
    }




}