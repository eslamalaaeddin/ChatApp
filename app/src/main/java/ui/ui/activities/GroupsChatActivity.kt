package ui.ui.activities

import android.annotation.SuppressLint
import android.app.ActionBar
import android.app.Dialog
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.whatsapp.BottomSheetDialog
import com.example.whatsapp.R
import com.example.whatsapp.Utils
import com.example.whatsapp.Utils.DEVICE_TOKEN_CHILD
import com.example.whatsapp.Utils.MESSAGES_CHILD
import com.example.whatsapp.Utils.USERS_CHILD
import com.example.whatsapp.databinding.ActivityGroupsChatBinding
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import com.google.gson.Gson
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.video_player_dialog.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import models.PrivateMessageModel
import notifications.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

private const val GROUP_ID = "group id"
private const val USER_NAME = "user name"
private const val USER_IMAGE = "user image"
private const val RECEIVER_ID = "receiver id"
private const val REQUEST_NUM = 1
private const val TAG = "PrivateChatActivity"
private const val VIDEO_URL = "video url"


class GroupsChatActivity : VisibleActivity(), BottomSheetDialog.BottomSheetListener {



    companion object{

        const val ACTION_SHOW_NOTIFICATION =
            "ui.fragments.activities.PrivateChatActivity.SHOW_NOTIFICATION"
        const val PERM_PRIVATE = "ui.fragments.activities.PrivateChatActivity.PRIVATE"

        const val REQUEST_CODE = "REQUEST_CODE"
        const val NOTIFICATION = "NOTIFICATION"
    }

    private lateinit var activityGroupChatBinding: ActivityGroupsChatBinding


    private lateinit var auth: FirebaseAuth
    private lateinit var senderId:String
    private lateinit var receiverId:String
    private lateinit var groupId:String

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
    private lateinit var videoCallImageView: ImageView

    private lateinit var progressDialog: ProgressDialog

    private lateinit var bottomSheetDialog: BottomSheetDialog


    private var checker:String = ""
    private var url:String = ""
    private lateinit var fileUri:Uri

    private var recorder:MediaRecorder? = null
    private var fileName:String = ""
    private var backPressed = false

    private  var messagesAdapter = PrivateMessagesAdapter(emptyList())

    private var messagesList = mutableListOf<PrivateMessageModel>()

    private lateinit var alertBuilder: AlertDialog.Builder
    private lateinit var addNoteAlertDialog: AlertDialog
    private lateinit var view: View


    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityGroupChatBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_groups_chat)

        auth = FirebaseAuth.getInstance()

        senderId = auth.currentUser?.uid.toString()
        groupId = intent.getStringExtra(GROUP_ID).toString()
        receiverId = "123"

        Utils.senderId = senderId

        rootRef = FirebaseDatabase.getInstance().reference

        usersRef = rootRef.child(USERS_CHILD)

        currentUser = auth.currentUser!!

        currentUserId = currentUser.uid

        setUpToolbar()

        getTokens()

        getSenderName()

        activityGroupChatBinding.sendMessageButton.setOnClickListener {
            sendMessage()
            pushNotification()
        }

//        messagesAdapter = PrivateMessagesAdapter(messagesList)
//        activityPrivateChatBinding.privateChatRecyclerView.adapter = messagesAdapter
        val linearLayout = LinearLayoutManager(this)
        //  linearLayout.reverseLayout = true
        activityGroupChatBinding.privateChatRecyclerView.layoutManager = linearLayout
        activityGroupChatBinding.privateChatRecyclerView.scrollToPosition(messagesAdapter.itemCount - 1)

        activityGroupChatBinding.attachFileButton.setOnClickListener {
            bottomSheetDialog = BottomSheetDialog()
            bottomSheetDialog.show(supportFragmentManager, "exampleBottomSheet")
        }

        activityGroupChatBinding.cameraButton.setOnClickListener {
            takePhoto()
        }

        activityGroupChatBinding.sendMessageEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun afterTextChanged(text: Editable?) {
                if (text.toString().isEmpty()){
                    activityGroupChatBinding.sendMessageButton.visibility = View.INVISIBLE
                    activityGroupChatBinding.sendVoiceMessageButton.visibility = View.VISIBLE
                    activityGroupChatBinding.cameraButton.visibility = View.VISIBLE
                }else{
                    activityGroupChatBinding.sendMessageButton.visibility = View.VISIBLE
                    activityGroupChatBinding.sendVoiceMessageButton.visibility = View.INVISIBLE
                    activityGroupChatBinding.cameraButton.visibility = View.GONE
                }
            }
        })

        fileName = Environment.getExternalStorageDirectory().absolutePath
        fileName+= "/recorded_message.3gp"

        activityGroupChatBinding.sendVoiceMessageButton.setOnTouchListener { p0, motionEvent ->
            if (motionEvent?.action == MotionEvent.ACTION_DOWN) {
                startRecording()
                Toast.makeText(this, "Recording Starts", Toast.LENGTH_SHORT).show()
            } else if (motionEvent?.action == MotionEvent.ACTION_UP) {
                stopRecording()
                Toast.makeText(this, "Recording Stops", Toast.LENGTH_SHORT).show()
                sendVoiceMessage()
            }
            true
        }

    }


    @SuppressLint("SimpleDateFormat")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_NUM && resultCode == RESULT_OK  && checker!="captured image" && data != null && data.data != null) {

            getLoadingDialog()

            //documents
            if (checker!="image" && checker!="video" && checker!="audio" && checker!="captured image" ) {
                fileUri = data.data!!
                val storageRef = FirebaseStorage.getInstance().reference.child("Document files")


                val userMessageKeyRef = rootRef.child(USERS_CHILD).child("Groups").child(MESSAGES_CHILD).
                push()

                val messagePushId = userMessageKeyRef.key.toString()

                val filePath = storageRef.child("$messagePushId.$checker")

                filePath.putFile(fileUri!!).addOnCompleteListener { task ->
                    if (task.isSuccessful) {

                        val calender = Calendar.getInstance()
                        //get date and time
                        val dateFormat = SimpleDateFormat("MMM dd, yyyy")
                        val timeFormat = SimpleDateFormat("hh:mm a")

                        currentDate = dateFormat.format(calender.time)
                        currentTime = timeFormat.format(calender.time)

                        filePath.downloadUrl.addOnCompleteListener {
                            rootRef.child(USERS_CHILD).child("Groups")
                                .child(groupId).child(
                                    MESSAGES_CHILD
                                )
                                .child(messagePushId).child("message")
                                .setValue(it.result.toString()).addOnCompleteListener {

                                }
                        }

                        val messageImageBody = HashMap<String, Any>()
                        //
                        messageImageBody.put("name", fileUri.lastPathSegment.toString())
                        messageImageBody.put("type", checker)
                        messageImageBody.put("from", senderId)
                        messageImageBody.put("messageKey", messagePushId)
                        messageImageBody.put("date", currentDate)
                        messageImageBody.put("time", currentTime)

                        val messageBodyDetails = HashMap<String, Any>()
                        messageBodyDetails.put(messagePushId, messageImageBody)



                        rootRef.child(USERS_CHILD).child("Groups").child(groupId)
                            .child(MESSAGES_CHILD).updateChildren(messageBodyDetails)
                        progressDialog.dismiss()
                    }
                }.addOnFailureListener{
                    progressDialog.dismiss()
                    Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
                }.addOnProgressListener {
                    val progress = ((100.0 * it.bytesTransferred) / it.totalByteCount).toInt()
                    progressDialog.show()
                    progressDialog.setMessage("$progress % Uploading...")
                }
            }

            else if (checker == "video") {
                fileUri = data.data!!
                val storageRef = FirebaseStorage.getInstance().reference.child("Video files")


                val userMessageKeyRef = rootRef.child(USERS_CHILD).child("Groups").child(MESSAGES_CHILD).
                push()

                val messagePushId = userMessageKeyRef.key.toString()

                val filePath = storageRef.child("$messagePushId.mp4")

                filePath.putFile(fileUri).addOnCompleteListener { task ->
                    if (task.isSuccessful) {

                        val calender = Calendar.getInstance()
                        //get date and time
                        val dateFormat = SimpleDateFormat("MMM dd, yyyy")
                        val timeFormat = SimpleDateFormat("hh:mm a")

                        currentDate = dateFormat.format(calender.time)
                        currentTime = timeFormat.format(calender.time)

                        filePath.downloadUrl.addOnCompleteListener {
                            rootRef.child(USERS_CHILD).child("Groups")
                                .child(groupId).child(
                                    MESSAGES_CHILD
                                )
                                .child(messagePushId).child("message")
                                .setValue(it.result.toString()).addOnCompleteListener {

                                }
                        }

                        val messageImageBody = HashMap<String, Any>()
                        //
                        messageImageBody.put("name", fileUri.lastPathSegment.toString())
                        messageImageBody.put("type", checker)
                        messageImageBody.put("from", senderId)
                        messageImageBody.put("messageKey", messagePushId)
                        messageImageBody.put("date", currentDate)
                        messageImageBody.put("time", currentTime)

                        val messageBodyDetails = HashMap<String, Any>()
                        messageBodyDetails.put(messagePushId, messageImageBody)



                        rootRef.child(USERS_CHILD).child("Groups").child(groupId)
                            .child(MESSAGES_CHILD).updateChildren(messageBodyDetails)
                        progressDialog.dismiss()
                    }
                }.addOnFailureListener{
                    progressDialog.dismiss()
                    Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
                }.addOnProgressListener {
                    val progress = ((100.0 * it.bytesTransferred) / it.totalByteCount).toInt()
                    progressDialog.show()
                    progressDialog.setMessage("$progress % Uploading...")
                }

            }

            else if (checker == "audio") {
                fileUri = data.data!!
                val storageRef = FirebaseStorage.getInstance().reference.child("Audio files")


                val userMessageKeyRef = rootRef.child(USERS_CHILD).child("Groups").child(MESSAGES_CHILD).
                push()

                val messagePushId = userMessageKeyRef.key.toString()

                val filePath = storageRef.child("$messagePushId.$checker")

                filePath.putFile(fileUri).addOnCompleteListener { task ->
                    if (task.isSuccessful) {

                        val calender = Calendar.getInstance()
                        //get date and time
                        val dateFormat = SimpleDateFormat("MMM dd, yyyy")
                        val timeFormat = SimpleDateFormat("hh:mm a")

                        currentDate = dateFormat.format(calender.time)
                        currentTime = timeFormat.format(calender.time)

                        filePath.downloadUrl.addOnCompleteListener {
                            rootRef.child(USERS_CHILD).child("Groups")
                                .child(groupId).child(
                                    MESSAGES_CHILD
                                )
                                .child(messagePushId).child("message")
                                .setValue(it.result.toString()).addOnCompleteListener {

                                }
                        }

                        val messageImageBody = HashMap<String, Any>()
                        //
                        messageImageBody.put("name", fileUri.lastPathSegment.toString())
                        messageImageBody.put("type", checker)
                        messageImageBody.put("from", senderId)
                        messageImageBody.put("messageKey", messagePushId)
                        messageImageBody.put("date", currentDate)
                        messageImageBody.put("time", currentTime)

                        val messageBodyDetails = HashMap<String, Any>()
                        messageBodyDetails.put(messagePushId, messageImageBody)



                        rootRef.child(USERS_CHILD).child("Groups").child(groupId)
                            .child(MESSAGES_CHILD).updateChildren(messageBodyDetails)
                        progressDialog.dismiss()
                    }
                }.addOnFailureListener{
                    progressDialog.dismiss()
                    Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
                }.addOnProgressListener {
                    val progress = ((100.0 * it.bytesTransferred) / it.totalByteCount).toInt()
                    progressDialog.show()
                    progressDialog.setMessage("$progress % Uploading...")
                }

            }

            else if (checker =="image") {
                fileUri = data.data!!
                val storageRef = FirebaseStorage.getInstance().reference.child("Image files")


                val userMessageKeyRef = rootRef.child(USERS_CHILD).child("Groups").child(MESSAGES_CHILD).
                push()

                val messagePushId = userMessageKeyRef.key.toString()

                val filePath = storageRef.child("$messagePushId.$checker")

                filePath.putFile(fileUri).addOnCompleteListener { task ->
                    if (task.isSuccessful) {

                        val calender = Calendar.getInstance()
                        //get date and time
                        val dateFormat = SimpleDateFormat("MMM dd, yyyy")
                        val timeFormat = SimpleDateFormat("hh:mm a")

                        currentDate = dateFormat.format(calender.time)
                        currentTime = timeFormat.format(calender.time)

                        filePath.downloadUrl.addOnCompleteListener {
                            rootRef.child(USERS_CHILD).child("Groups")
                                .child(groupId).child(
                                    MESSAGES_CHILD
                                )
                                .child(messagePushId).child("message")
                                .setValue(it.result.toString()).addOnCompleteListener {

                                }
                        }

                        val messageImageBody = HashMap<String, Any>()
                        //
                        messageImageBody.put("name", fileUri.lastPathSegment.toString())
                        messageImageBody.put("type", checker)
                        messageImageBody.put("from", senderId)
                        messageImageBody.put("messageKey", messagePushId)
                        messageImageBody.put("date", currentDate)
                        messageImageBody.put("time", currentTime)

                        val messageBodyDetails = HashMap<String, Any>()
                        messageBodyDetails.put(messagePushId, messageImageBody)



                        rootRef.child(USERS_CHILD).child("Groups").child(groupId)
                            .child(MESSAGES_CHILD).updateChildren(messageBodyDetails)
                        progressDialog.dismiss()
                    }
                }.addOnFailureListener{
                    progressDialog.dismiss()
                    Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
                }.addOnProgressListener {
                    val progress = ((100.0 * it.bytesTransferred) / it.totalByteCount).toInt()
                    progressDialog.show()
                    progressDialog.setMessage("$progress % Uploading...")
                }
            }

        }

        else if (checker =="captured image" && requestCode == REQUEST_NUM && resultCode == RESULT_OK ) {

            //getLoadingDialog()
            val storageRef = FirebaseStorage.getInstance().reference.child("Image files")

            val image = data?.extras!!["data"] as Bitmap?

            val stream = ByteArrayOutputStream()
            image!!.compress(Bitmap.CompressFormat.PNG, 100, stream)

            val b = stream.toByteArray()


            val userMessageKeyRef = rootRef.child(USERS_CHILD).child("Groups").child(MESSAGES_CHILD).
            push()

            val messagePushId = userMessageKeyRef.key.toString()

            val filePath = storageRef.child("$messagePushId.jpg")
            val uploadTask = filePath.putBytes(b)

            uploadTask.continueWithTask { task ->
                if (!task.isSuccessful) {
                    throw task.exception!!
                }
                filePath.downloadUrl
            }.addOnCompleteListener {
                if (it.isSuccessful) {
                    url = it.result.toString()

                    val calender = Calendar.getInstance()
                    //get date and time
                    val dateFormat = SimpleDateFormat("MMM dd, yyyy")
                    val timeFormat = SimpleDateFormat("hh:mm a")

                    currentDate = dateFormat.format(calender.time)
                    currentTime = timeFormat.format(calender.time)


                    val messageImageBody = HashMap<String, Any>()
                    messageImageBody.put("message", url)
                    messageImageBody.put("name", "image $messagePushId")
                    messageImageBody.put("type", checker)
                    messageImageBody.put("from", senderId)
                    messageImageBody.put("messageKey", messagePushId)
                    messageImageBody.put("date", currentDate)
                    messageImageBody.put("time", currentTime)

                    val messageBodyDetails = HashMap<String, Any>()
                    messageBodyDetails.put(messagePushId, messageImageBody)



                    rootRef.child(USERS_CHILD).child("Groups").child(groupId)
                        .child(MESSAGES_CHILD).updateChildren(messageBodyDetails)
                  //  progressDialog.dismiss()

                }
            }

        }

        else{
            Toast.makeText(this, "Choose a valid attachment", Toast.LENGTH_SHORT).show()
        }
//        progressDialog.dismiss()
    }


    override fun onStart() {
        super.onStart()
        rootRef.child(USERS_CHILD).child("Groups").child(groupId).child(MESSAGES_CHILD).addChildEventListener(
            object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
//                    messagesList.clear()
                    val ourMessages = snapshot.getValue(PrivateMessageModel::class.java)
                    if (ourMessages != null) {
                        messagesList.add(ourMessages)
                        messagesAdapter = PrivateMessagesAdapter(messagesList)
                        activityGroupChatBinding.privateChatRecyclerView.adapter = messagesAdapter
                        messagesAdapter.notifyDataSetChanged()
                        //to scroll to the bottom of recycler view
                        if (messagesList.isNotEmpty()) {
                            activityGroupChatBinding.privateChatRecyclerView.smoothScrollToPosition(
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

        val toolbarView = LayoutInflater.from(this).inflate(R.layout.custom_toolbar, null)

        setSupportActionBar(activityGroupChatBinding.mainToolbar)
        // supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowCustomEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        supportActionBar?.customView = toolbarView

        Log.i(TAG, "WWW setUpToolbar: $senderId")
        Log.i(TAG, "WWW setUpToolbar: $receiverId")


        userImageView = findViewById(R.id.user_image_view)
        userNameTextView = findViewById(R.id.user_name_text_view)
        lastSeenTextView = findViewById(R.id.user_last_seen)

        usersRef.child(receiverId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                userNameTextView.text = snapshot.child("name").value.toString()

                val imageUrl = snapshot.child("image").value.toString()
                if (imageUrl.isNotEmpty()) {
                    Picasso.get()
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_group)
                        .into(userImageView)
                }

            }

            override fun onCancelled(error: DatabaseError) {

            }
        })



//        activityPrivateChatBinding.mainToolbar.setTitleTextColor(Color.WHITE)
//        activityPrivateChatBinding.mainToolbar.overflowIcon?.setColorFilter(resources.getColor(R.color.white), PorterDuff.Mode.SRC_IN)
        //  activityPrivateChatBinding.mainToolbar.navigationIcon?.setColorFilter(resources.getColor(R.color.white), PorterDuff.Mode.SRC_IN)
    }


    private fun sendMessage() {
        currentMessage = activityGroupChatBinding.sendMessageEditText.editableText.toString()

        if(currentMessage.isNotEmpty()) {

            val userMessageKeyRef = rootRef.child(USERS_CHILD).child("Groups").child(MESSAGES_CHILD).
            push()

            val messagePushId = userMessageKeyRef.key.toString()

            val calender = Calendar.getInstance()
            //get date and time
            val dateFormat = SimpleDateFormat("MMM dd, yyyy")
            val timeFormat = SimpleDateFormat("hh:mm a")

            currentDate = dateFormat.format(calender.time)
            currentTime = timeFormat.format(calender.time)

            val messageTextBody = HashMap<String, Any>()
            messageTextBody.put("message", currentMessage)
            messageTextBody.put("type", "text")
            messageTextBody.put("from", senderId)
            messageTextBody.put("messageKey", messagePushId)
            messageTextBody.put("date", currentDate)
            messageTextBody.put("time", currentTime)

            val messageBodyDetails = HashMap<String, Any>()
            messageBodyDetails.put(messagePushId, messageTextBody)

            rootRef.child(USERS_CHILD).child("Groups").child(groupId).child(MESSAGES_CHILD).updateChildren(messageBodyDetails).addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Toast.makeText(this, task.exception?.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        else{

        }
        //send a broadcast intent
        // sendBroadcast(Intent(ACTION_SHOW_NOTIFICATION), PERM_PRIVATE)
        activityGroupChatBinding.sendMessageEditText.text.clear()
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
        usersRef.child(receiverId).child(DEVICE_TOKEN_CHILD).addValueEventListener(object :
            ValueEventListener {
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
            NotificationData(currentUserName, currentMessage, Utils.senderId), receiverToken
        )
        if (currentMessage.isNotEmpty()){
            sendNotification(notification)
        }
    }

    private fun pushVideoChatNotificationRequest () {
        val notification =   PushVideoChatNotification(
            VideoChatNotificationData(
                currentUserName,
                "I Want to make a video chat with you",
                Utils.senderId
            ), receiverToken
        )
        sendVideoChatNotification(notification)
    }

    private fun sendVideoChatNotification(notification: PushVideoChatNotification) = CoroutineScope(
        Dispatchers.IO
    ).launch {
        try {
            val response = RetrofitInstance.api.postVideoNotification(notification)
            if(response.isSuccessful) {
                Log.d(TAG, "Response: ${Gson().toJson(response)}")
            } else {
                Log.e(TAG, response.errorBody().toString())
            }
        } catch (e: Exception) {
            Log.e(TAG, e.toString())
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
        } catch (e: Exception) {
            Log.e(TAG, e.toString())
        }
    }

    private fun getSenderName(){
        usersRef.child(senderId).child("name").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                currentUserName = snapshot.value.toString()
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })

    }

    private fun displayLastSeen(){
        rootRef.child("Users").child(receiverId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                val time = snapshot.child("state").child("time").value.toString()
                val date = snapshot.child("state").child("date").value.toString()
                val state = snapshot.child("state").child("state").value.toString()

                if (state == "offline") {

                    lastSeenTextView.text = "Last seen: $time, $date"
                } else if (state == "online") {
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

    private fun updateUserStatus(state: String) {
        var currentDate = ""
        var currentTime = ""

        val calender = Calendar.getInstance()
        //get date and time
        val dateFormat = SimpleDateFormat("MMM dd, yyyy")
        val timeFormat = SimpleDateFormat("hh:mm a")

        currentDate = dateFormat.format(calender.time)
        currentTime = timeFormat.format(calender.time)

        val userStateMap = HashMap<String, Any> ()
        userStateMap.put("date", currentDate)
        userStateMap.put("time", currentTime)
        userStateMap.put("state", state)

        rootRef.child(USERS_CHILD).child(currentUserId).child(Utils.STATE_CHILD).updateChildren(
            userStateMap
        )


    }

    private fun getLoadingDialog() {
        progressDialog = ProgressDialog(this)
            .also {
                title = "Attachment is uploading"
                it.setCanceledOnTouchOutside(false)
                it.show()
            }
    }

    override fun onStop() {
        super.onStop()
        if (backPressed){
            updateUserStatus("online")
        }
        else{
            updateUserStatus("offline")

        }

    }

    override fun onDestroy() {
        super.onDestroy()
        if (backPressed){
            updateUserStatus("online")
        }
        else{
            updateUserStatus("offline")

        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        backPressed = true
    }


    inner class PrivateMessagesAdapter(private val messages: List<PrivateMessageModel>) :
        RecyclerView.Adapter<PrivateMessagesAdapter.PopularMoviesViewHolder>() {

        inner class PopularMoviesViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener,View.OnLongClickListener {
            val senderMessageTextView : TextView = itemView.findViewById(R.id.sender_message_text)
            val receiverMessageTextView : TextView = itemView.findViewById(R.id.receiver_message_text)
            val senderMessageTimeTextView:TextView = itemView.findViewById(R.id.sender_time_sent_text_view)
            val receiverMessageTimeTextView:TextView = itemView.findViewById(R.id.receiver_time_sent_text_view)
            val senderMessageImageView : ImageView = itemView.findViewById(R.id.sender_message_image_view)
            val receiverMessageImageView : ImageView = itemView.findViewById(R.id.receiver_message_image_view)

            val senderMessageLayout : LinearLayout = itemView.findViewById(R.id.sender_message_layout)
            val receiverMessageLayout : LinearLayout = itemView.findViewById(R.id.receiver_message_layout)

            val senderMessageFrame : FrameLayout = itemView.findViewById(R.id.sender_message_Frame_view)
            val receiverMessageFrame : FrameLayout = itemView.findViewById(R.id.receiver_message_Frame_view)

            val senderMessagePlay : ImageView = itemView.findViewById(R.id.sender_message_play_image_view)
            val receiverMessagePlay : ImageView = itemView.findViewById(R.id.receiver_message_play_view)




            init {
                itemView.setOnClickListener(this)
                itemView.setOnLongClickListener(this)
            }

            fun bind(messageModel: PrivateMessageModel) {
                senderMessageTextView.text = messageModel.message


            }


            fun adjustMargins () {
                val param = receiverMessageLayout.layoutParams as ViewGroup.MarginLayoutParams
                param.setMargins(4, 0, 0, 0)
                receiverMessageLayout.layoutParams = param
            }

            fun resetMargins () {
                val param = receiverMessageLayout.layoutParams as ViewGroup.MarginLayoutParams
                param.setMargins(4, 8, 4, 0)
                receiverMessageLayout.layoutParams = param
            }

            override fun onClick(item: View?) {

            }

            //to get message date
            override fun onLongClick(p0: View?): Boolean {
                var messageDate =messages[adapterPosition].date
                val currentDate = SimpleDateFormat("MMM dd, yyyy").format(Calendar.getInstance().time)
                if (messageDate == currentDate) {
                    Snackbar.make(p0!!, "Sent: today", Snackbar.LENGTH_LONG).show()
                }
                else{
                    Snackbar.make(p0!!, "Sent: $messageDate", Snackbar.LENGTH_LONG).show()
                }

                return true
            }


        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PopularMoviesViewHolder {

            val view = LayoutInflater.from(parent.context).inflate(
                R.layout.private_message_layout,
                parent,
                false
            )
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

            //what appears to the receiver
            if (fromUserId == currentUserId) {
                holder.adjustMargins()
            }
            else{
                holder.resetMargins()
            }



            if (fromMessagesType == "text") {
                holder.senderMessageImageView.visibility = View.GONE
                holder.receiverMessageImageView.visibility = View.GONE

                holder.senderMessagePlay.visibility = View.GONE
                holder.receiverMessagePlay.visibility = View.GONE
                holder.receiverMessageFrame.visibility = View.GONE
                holder.senderMessageFrame.visibility = View.GONE

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

            else if (fromMessagesType == "image" || fromMessagesType == "captured image") {

                holder.receiverMessageTextView.visibility = View.GONE
                holder.senderMessageTextView.visibility = View.GONE
                holder.senderMessagePlay.visibility = View.GONE
                holder.receiverMessagePlay.visibility = View.GONE

                if (fromUserId == messageSenderId) {
                    holder.senderMessageTimeTextView.visibility = View.VISIBLE
                    holder.senderMessageTimeTextView.text = myMessages.time
                    holder.receiverMessageLayout.visibility = View.GONE


                    holder.senderMessageImageView.visibility = View.VISIBLE
                    holder.receiverMessageImageView.visibility = View.GONE
                    Picasso.get()
                        .load(myMessages.message)
                        .into(holder.senderMessageImageView)

                    holder.senderMessageImageView.setOnClickListener {
                        showImage(myMessages.message)
                    }

                }

                else{

                    holder.receiverMessageTimeTextView.visibility = View.VISIBLE
                    holder.receiverMessageTimeTextView.text = myMessages.time
                    holder.senderMessageLayout.visibility = View.GONE

                    holder.receiverMessageImageView.visibility = View.VISIBLE
                    holder.senderMessageImageView.visibility = View.GONE
                    Picasso.get()
                        .load(myMessages.message)
                        .into(holder.receiverMessageImageView)

                    holder.receiverMessageImageView.setOnClickListener {
                        showImage(myMessages.message)
                    }
                }
            }

            else if (fromMessagesType == "video") {

                holder.receiverMessageTextView.visibility = View.GONE
                holder.senderMessageTextView.visibility = View.GONE

                if (fromUserId == messageSenderId) {
                    holder.senderMessageTimeTextView.visibility = View.VISIBLE
                    holder.senderMessageTimeTextView.text = myMessages.time
                    holder.receiverMessageLayout.visibility = View.GONE


                    holder.senderMessageImageView.visibility = View.VISIBLE
                    holder.receiverMessageImageView.visibility = View.GONE

                    //   holder.senderMessageImageView.setImageResource(R.drawable.ic_video)
                    holder.receiverMessageImageView.visibility = View.GONE

                    //Chosen frame interval
                    val interval: Long = 1* 1000
                    val options: RequestOptions = RequestOptions().frame(interval)

                    Glide.with(this@GroupsChatActivity)
                        .asBitmap().load(myMessages.message).apply(options).into(holder.senderMessageImageView)

                    holder.senderMessageImageView.setOnClickListener {

                        val videoIntent = Intent(
                            this@GroupsChatActivity,
                            VideoPlayerActivity::class.java
                        )
                        videoIntent.putExtra(VIDEO_URL, myMessages.message)
                        startActivity(videoIntent)

                        // showVideoPlayerDialog(myMessages.message)


                    }

                }

                else{

                    holder.receiverMessageTimeTextView.visibility = View.VISIBLE
                    holder.receiverMessageTimeTextView.text = myMessages.time
                    holder.senderMessageLayout.visibility = View.GONE

                    holder.receiverMessageImageView.visibility = View.VISIBLE
                    holder.senderMessageImageView.visibility = View.GONE
                    //
                    //  holder.receiverMessageImageView.setImageResource(R.drawable.ic_video)


                    val interval: Long = 1* 1000
                    val options: RequestOptions = RequestOptions().frame(interval)

                    Glide.with(this@GroupsChatActivity)
                        .asBitmap().load(myMessages.message).apply(options).into(holder.receiverMessageImageView)



                    holder.receiverMessageImageView.setOnClickListener {
                        val videoIntent = Intent(
                            this@GroupsChatActivity,
                            VideoPlayerActivity::class.java
                        )
                        videoIntent.putExtra(VIDEO_URL, myMessages.message)
                        startActivity(videoIntent)

                        //showVideoPlayerDialog(myMessages.message)

                    }
                }
            }

            else if (fromMessagesType == "audio") {

                holder.receiverMessageTextView.visibility = View.GONE
                holder.senderMessageTextView.visibility = View.GONE
                holder.senderMessagePlay.visibility = View.GONE
                holder.receiverMessagePlay.visibility = View.GONE

                if (fromUserId == messageSenderId) {
                    holder.senderMessageTimeTextView.visibility = View.VISIBLE
                    holder.senderMessageTimeTextView.text = myMessages.time
                    holder.receiverMessageLayout.visibility = View.GONE


                    holder.senderMessageImageView.visibility = View.VISIBLE
                    holder.receiverMessageImageView.visibility = View.GONE

                    holder.senderMessageImageView.setImageResource(R.drawable.ic_audio)
                    holder.receiverMessageImageView.visibility = View.GONE

                    holder.senderMessageImageView.setOnClickListener {

                        val videoIntent = Intent(
                            this@GroupsChatActivity,
                            VideoPlayerActivity::class.java
                        )
                        videoIntent.putExtra(VIDEO_URL, myMessages.message)
                        startActivity(videoIntent)

                        // showVideoPlayerDialog(myMessages.message)


                    }

                }

                else{

                    holder.receiverMessageTimeTextView.visibility = View.VISIBLE
                    holder.receiverMessageTimeTextView.text = myMessages.time
                    holder.senderMessageLayout.visibility = View.GONE

                    holder.receiverMessageImageView.visibility = View.VISIBLE
                    holder.senderMessageImageView.visibility = View.GONE

                    holder.receiverMessageImageView.setImageResource(R.drawable.ic_audio)

                    holder.receiverMessageImageView.setOnClickListener {
                        val videoIntent = Intent(
                            this@GroupsChatActivity,
                            VideoPlayerActivity::class.java
                        )
                        videoIntent.putExtra(VIDEO_URL, myMessages.message)
                        startActivity(videoIntent)

                        //showVideoPlayerDialog(myMessages.message)

                    }
                }
            }

            else if(fromMessagesType =="docx" || fromMessagesType=="pdf"){
                holder.receiverMessageTextView.visibility = View.GONE
                holder.senderMessageTextView.visibility = View.GONE
                holder.senderMessagePlay.visibility = View.GONE
                holder.receiverMessagePlay.visibility = View.GONE
                if (fromUserId == messageSenderId) {
                    holder.senderMessageTimeTextView.visibility = View.VISIBLE
                    holder.senderMessageTimeTextView.text = myMessages.time
                    holder.receiverMessageLayout.visibility = View.GONE


                    holder.senderMessageImageView.visibility = View.VISIBLE
                    holder.receiverMessageImageView.visibility = View.GONE

                    holder.senderMessageImageView.setImageResource(R.drawable.ic_file)
                    holder.receiverMessageImageView.visibility = View.GONE

                }
                else{
                    holder.receiverMessageTimeTextView.visibility = View.VISIBLE
                    holder.receiverMessageTimeTextView.text = myMessages.time
                    holder.senderMessageLayout.visibility = View.GONE

                    holder.receiverMessageImageView.visibility = View.VISIBLE
                    holder.senderMessageImageView.visibility = View.GONE

                    holder.receiverMessageImageView.setImageResource(R.drawable.ic_file)

                }
            }

//            if (fromUserId == senderId) {
//                holder.itemView.setOnLongClickListener {
//
//                    if (myMessages.type == "pdf" || myMessages.type == "docx"){
//                        val options = arrayOf("Download","Delete for me","Delete for every one", "Cancel")
//                        val alertDialogBuilder = AlertDialog.Builder(holder.itemView.context)
//                        alertDialogBuilder.setTitle("Delete message?")
//
//                        alertDialogBuilder.setItems(options) { dialogInterface, position->
//                            when(position) {
//                                //Download
//                                0 -> {
//                                    val downloadIntent = Intent(Intent.ACTION_VIEW, Uri.parse(myMessages.message))
//                                    holder.itemView.context.startActivity(downloadIntent)
//                                }
//                                //Delete for me
//                                1 -> {
//
//                                }
//                                //Delete for every one
//                                2 -> {}
//
//                            }
//                        }.show()
//                    }
//
//                    else if (myMessages.type == "text"){
//                        val options = arrayOf("Delete for me","Delete for every one", "Cancel")
//                        val alertDialogBuilder = AlertDialog.Builder(holder.itemView.context)
//                        alertDialogBuilder.setTitle("Delete message?")
//
//                        alertDialogBuilder.setItems(options) { dialogInterface, position->
//                            when(position) {
//                                //Delete for me
//                                0 -> {
//                                    val downloadIntent = Intent(Intent.ACTION_VIEW, Uri.parse(myMessages.message))
//                                    holder.itemView.context.startActivity(downloadIntent)
//                                }
//                                //Delete for every one
//                                1 -> {
//
//                                }
//
//                            }
//                        }.show()
//                    }
//
//                    else if (myMessages.type == "image"){
//                        val options = arrayOf("View","Download","Delete for me","Delete for every one", "Cancel")
//                        val alertDialogBuilder = AlertDialog.Builder(holder.itemView.context)
//                        alertDialogBuilder.setTitle("Delete message?")
//
//                        alertDialogBuilder.setItems(options) { dialogInterface, position->
//                            when(position) {
//                                //Delete for me
//                                0 -> {
//                                    val downloadIntent = Intent(Intent.ACTION_VIEW, Uri.parse(myMessages.message))
//                                    holder.itemView.context.startActivity(downloadIntent)
//                                }
//                                //Delete for every one
//                                1 -> {
//
//                                }
//
//                            }
//                        }.show()
//                    }
//
//                    return@setOnLongClickListener true
//
//                }
//            }
//
//            else{
//                holder.itemView.setOnLongClickListener {
//
//                    if (myMessages.type == "pdf" || myMessages.type == "docx"){
//                        val options = arrayOf("Download","Delete for me", "Cancel")
//                        val alertDialogBuilder = AlertDialog.Builder(holder.itemView.context)
//                        alertDialogBuilder.setTitle("Delete message?")
//
//                        alertDialogBuilder.setItems(options) { dialogInterface, position->
//                            when(position) {
//                                //Download
//                                0 -> {
//                                    val downloadIntent = Intent(Intent.ACTION_VIEW, Uri.parse(myMessages.message))
//                                    holder.itemView.context.startActivity(downloadIntent)
//                                }
//                                //Delete for me
//                                1 -> {
//
//                                }
//                                //Delete for every one
//                                2 -> {}
//
//                            }
//                        }.show()
//                    }
//
//                    else if (myMessages.type == "text"){
//                        val options = arrayOf("Delete for me", "Cancel")
//                        val alertDialogBuilder = AlertDialog.Builder(holder.itemView.context)
//                        alertDialogBuilder.setTitle("Delete message?")
//
//                        alertDialogBuilder.setItems(options) { dialogInterface, position->
//                            when(position) {
//                                //Delete for me
//                                0 -> {
//                                    val downloadIntent = Intent(Intent.ACTION_VIEW, Uri.parse(myMessages.message))
//                                    holder.itemView.context.startActivity(downloadIntent)
//                                }
//                                //Delete for every one
//                                1 -> {
//
//                                }
//
//                            }
//                        }.show()
//                    }
//
//                    else if (myMessages.type == "image"){
//                        val options = arrayOf("View","Download","Delete for me", "Cancel")
//                        val alertDialogBuilder = AlertDialog.Builder(holder.itemView.context)
//                        alertDialogBuilder.setTitle("Delete message?")
//
//                        alertDialogBuilder.setItems(options) { dialogInterface, position->
//                            when(position) {
//                                //Delete for me
//                                0 -> {
//                                    val downloadIntent = Intent(Intent.ACTION_VIEW, Uri.parse(myMessages.message))
//                                    holder.itemView.context.startActivity(downloadIntent)
//                                }
//                                //Delete for every one
//                                1 -> {
//
//                                }
//
//                            }
//                        }.show()
//                    }
//
//                    return@setOnLongClickListener true
//
//                }
//            }
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getItemViewType(position: Int): Int {
            return position
        }


    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.private_chat_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.establish_video_chat -> makeVideoCall()

//            R.id.establish_audio_chat -> makeAudioCall ()

        }
        return super.onOptionsItemSelected(item)
    }

    private fun deleteSentMessages () {

    }

    private fun makeVideoCall () {
        val callingIntent = Intent(this, VideoChatActivity::class.java)
        callingIntent.putExtra(RECEIVER_ID, receiverId)
        startActivity(callingIntent)
        pushVideoChatNotificationRequest()
    }

    private fun makeAudioCall () {
        val callingIntent = Intent(this, VoiceCallActivity::class.java)
        callingIntent.putExtra(RECEIVER_ID, receiverId)
        startActivity(callingIntent)
        //pushVideoChatNotificationRequest()
    }

    private fun showAudioPlayerDialog(url: String) {
        alertBuilder = AlertDialog.Builder(this)
        view = layoutInflater.inflate(R.layout.video_player_dialog, null)
        alertBuilder.setView(view)

        addNoteAlertDialog =  alertBuilder.create()
        addNoteAlertDialog.show()

        view.videoView.setVideoPath(url)
        view.videoView.start()

        val mediaController = MediaController(this)
        view.videoView.setMediaController(mediaController)
        mediaController.setAnchorView(view.videoView)


    }

    override fun onFabClicked(textUnderFab: String?) {
        when(textUnderFab) {
            "PDF" -> {
                sendMeToPDFsStorage()
            }
            "Ms Word" -> {
                sendMeToMSWordStorage()
            }
            "Image" -> {
                sendMeToImagesStorage()
            }
            "Audio" -> {
                sendMeToAudioStorage()
            }
            "Video" -> {
                sendMeToVideosStorage()
            }
            "Contact" -> {
                Toast.makeText(this, "Not implemented yet", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun sendMeToAudioStorage() {
        checker = "audio"
        val audioIntent = Intent(Intent.ACTION_GET_CONTENT)
        audioIntent.type = "audio/*"
        startActivityForResult(
            Intent.createChooser(
                audioIntent,
                "Choose an audio file"
            ),
            REQUEST_NUM
        )
    }

    private fun sendMeToMSWordStorage() {
        checker = "docx"
        val imagesIntent = Intent(Intent.ACTION_GET_CONTENT)
        imagesIntent.type = "application/msword"
        startActivityForResult(
            Intent.createChooser(
                imagesIntent,
                "Choose an MS Word file"
            ),
            REQUEST_NUM
        )
    }

    private fun sendMeToPDFsStorage() {
        checker = "pdf"
        val imagesIntent = Intent(Intent.ACTION_GET_CONTENT)
        imagesIntent.type = "application/pdf"
        startActivityForResult(
            Intent.createChooser(
                imagesIntent,
                "Choose a PDF file"
            ),
            REQUEST_NUM
        )
    }

    private fun sendMeToVideosStorage() {
        checker = "video"
        val videoIntent = Intent(Intent.ACTION_GET_CONTENT)
        videoIntent.type = "video/*"
        startActivityForResult(
            Intent.createChooser(
                videoIntent,
                "Choose a video"
            ),
            REQUEST_NUM
        )
    }

    private fun sendMeToImagesStorage() {
        checker = "image"
        val imagesIntent = Intent(Intent.ACTION_GET_CONTENT)
        imagesIntent.type = "image/*"
        startActivityForResult(Intent.createChooser(imagesIntent, "Choose an image"), REQUEST_NUM)
    }

    fun showImage(imageUrl: String) {
        val builder = Dialog(this)
        builder.requestWindowFeature(Window.FEATURE_NO_TITLE)
        builder.window?.setBackgroundDrawable(
            ColorDrawable(Color.TRANSPARENT)
        )
        builder.setOnDismissListener {
            //nothing;
        }
        val imageView = ImageView(this)

        Picasso.get()
            .load(imageUrl)
            .into(imageView)


        builder.addContentView(
            imageView, ActionBar.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        builder.show()
    }

    private fun takePhoto () {
        checker = "captured image"
        val captureImage = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(captureImage, REQUEST_NUM)
    }

    private fun startRecording() {
        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setOutputFile(fileName)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)

            try {
                prepare()
            } catch (e: IOException) {
                Log.e(TAG, "startRecording: prepare() failed" )
            }

            start()
        }
    }

    private fun stopRecording() {
        recorder?.apply {
            stop()
            release()
        }
        recorder = null
    }

    @SuppressLint("SimpleDateFormat")
    private fun sendVoiceMessage() {
        fileUri = Uri.fromFile(File(fileName))
        val storageRef = FirebaseStorage.getInstance().reference.child("Recorded Messages")

        val messageSenderRef = "${MESSAGES_CHILD}/$senderId/$receiverId"
        val messageReceiverRef = "${MESSAGES_CHILD}/$receiverId/$senderId"

        val userMessageKeyRef = rootRef.child(MESSAGES_CHILD).
        child(senderId).child(receiverId).push()

        val messagePushId = userMessageKeyRef.key.toString()

        val filePath = storageRef.child("$messagePushId.mp3")
        val uploadTask = filePath.putFile(fileUri)

        uploadTask.continueWithTask { task ->
            if (!task.isSuccessful) {
                throw task.exception!!
            }
            filePath.downloadUrl
        }.addOnCompleteListener {
            if (it.isSuccessful) {
                url = it.result.toString()

                val calender = Calendar.getInstance()
                //get date and time
                val dateFormat = SimpleDateFormat("MMM dd, yyyy")
                val timeFormat = SimpleDateFormat("hh:mm a")

                currentDate = dateFormat.format(calender.time)
                currentTime = timeFormat.format(calender.time)


                val messageImageBody = HashMap<String, Any>()
                messageImageBody["message"] = url
                messageImageBody["name"] = fileUri.lastPathSegment.toString()
                messageImageBody["type"] = "audio"
                messageImageBody["from"] = senderId
                messageImageBody["to"] = receiverId
                messageImageBody["messageKey"] = messagePushId
                messageImageBody["date"] = currentDate
                messageImageBody["time"] = currentTime

                val messageBodyDetails = HashMap<String, Any>()
                messageBodyDetails["$messageSenderRef/$messagePushId"] = messageImageBody
                messageBodyDetails["$messageReceiverRef/$messagePushId"] = messageImageBody

                rootRef.updateChildren(messageBodyDetails).addOnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        Toast.makeText(this, task.exception?.message, Toast.LENGTH_SHORT).show()

                    }
                }

            }
        }
    }

}