package ui.ui.activities

import android.annotation.SuppressLint
import android.app.ActionBar
import android.app.Dialog
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.media.MediaPlayer
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
import com.example.whatsapp.databinding.ActivityPrivateChatBinding
import com.example.whatsapp.databinding.PrivateMessageLayoutBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.clear_chat_dialog.view.*
import kotlinx.android.synthetic.main.create_group_dialog.view.*
import kotlinx.android.synthetic.main.create_group_dialog.view.cancel_button
import kotlinx.android.synthetic.main.create_group_dialog.view.user_image_view
import kotlinx.android.synthetic.main.custom_toolbar.view.*
import kotlinx.android.synthetic.main.private_message_layout.*
import kotlinx.android.synthetic.main.video_player_dialog.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import models.GroupModel
import models.PrivateMessageModel
import notifications.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

private const val USER_ID = "user id"
private const val RECEIVER_ID = "receiver id"
private const val REQUEST_NUM = 1
private const val TAG = "PrivateChatActivity"
private const val VIDEO_URL = "video url"


class PrivateChatActivity : VisibleActivity(), BottomSheetDialog.BottomSheetListener {



    companion object{

        const val ACTION_SHOW_NOTIFICATION =
            "ui.fragments.activities.PrivateChatActivity.SHOW_NOTIFICATION"
        const val PERM_PRIVATE = "ui.fragments.activities.PrivateChatActivity.PRIVATE"

        const val REQUEST_CODE = "REQUEST_CODE"
        const val NOTIFICATION = "NOTIFICATION"
    }

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

    private var clicked = false

    private lateinit var currentMessage:String
    private lateinit var currentDate :String
    private lateinit var currentTime :String

    private lateinit var userImageView: ImageView
    private lateinit var userNameTextView: TextView
    private lateinit var lastSeenTextView: TextView
    private lateinit var upButtonImageView: ImageView

    private lateinit var progressDialog: ProgressDialog

    private lateinit var bottomSheetDialog: BottomSheetDialog

    private lateinit var myMenu: Menu

    private lateinit var myItemView: View
    private lateinit var myViewHolder: RecyclerView.ViewHolder

    private var time : Int = 0
    private var messageTimeInSeconds : Int = 0
    private lateinit var messageTime : String

    private var longClick = false

    private var checker:String = ""
    private var url:String = ""
    private lateinit var fileUri:Uri

    private var blocked = false

    private var backPressed = false

    private var shortClick = false

    private  var messagesAdapter = PrivateMessagesAdapter(emptyList())

    private var messagesList = mutableListOf<PrivateMessageModel>()

    private lateinit var alertBuilder: AlertDialog.Builder
    private lateinit var addNoteAlertDialog: AlertDialog
    private lateinit var view: View

    private var recorder:MediaRecorder? = null
    private var fileName:String = ""

    private lateinit var toolbarView:View

    private  var keysSelectedOnLongClick = HashMap<Int,String>()

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityPrivateChatBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_private_chat)

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

        progressDialog = ProgressDialog(this)
        progressDialog.setCanceledOnTouchOutside(false)

        activityPrivateChatBinding.sendMessageButton.setOnClickListener {
            if (!blocked){
                sendMessage()
                pushNotification()
            }

            else{
                showUnBlockContactDialog()
            }

        }

        val linearLayout = LinearLayoutManager(this)
        linearLayout.stackFromEnd = true
        activityPrivateChatBinding.privateChatRecyclerView.layoutManager = linearLayout
        activityPrivateChatBinding.privateChatRecyclerView.scrollToPosition(messagesAdapter.itemCount - 1)

        activityPrivateChatBinding.attachFileButton.setOnClickListener {

            if (!blocked){
                bottomSheetDialog = BottomSheetDialog()
                bottomSheetDialog.show(supportFragmentManager, "exampleBottomSheet")
            }

            else{
                showUnBlockContactDialog()
            }



        }

        activityPrivateChatBinding.cameraButton.setOnClickListener {

            if (!blocked){
                takePhoto()
            }

            else{
                showUnBlockContactDialog()
            }
        }

        activityPrivateChatBinding.sendMessageEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun afterTextChanged(text: Editable?) {
                if (text.toString().isEmpty()) {
                    activityPrivateChatBinding.sendMessageButton.visibility = View.INVISIBLE
                    activityPrivateChatBinding.sendVoiceMessageButton.visibility = View.VISIBLE
                    activityPrivateChatBinding.cameraButton.visibility = View.VISIBLE
                } else {
                    activityPrivateChatBinding.sendMessageButton.visibility = View.VISIBLE
                    activityPrivateChatBinding.sendVoiceMessageButton.visibility = View.INVISIBLE
                    activityPrivateChatBinding.cameraButton.visibility = View.GONE
                }
            }
        })

        fileName = Environment.getExternalStorageDirectory().absolutePath
        fileName+= "/recorded_message.3gp"

        activityPrivateChatBinding.sendVoiceMessageButton.setOnTouchListener { p0, motionEvent ->

            if (!blocked){

                if (motionEvent?.action == MotionEvent.ACTION_DOWN) {
                    time = (System.currentTimeMillis() / 1000).toInt()
                    soundOnStartVoiceMessage()

                } else if (motionEvent?.action == MotionEvent.ACTION_UP) {
                    messageTimeInSeconds =  ((System.currentTimeMillis() / 1000) - time ).toInt()

                    val minutes: Int = messageTimeInSeconds % 3600 / 60
                    val secs: Int = messageTimeInSeconds % 60
                    messageTime = java.lang.String.format(
                        Locale.getDefault(),
                        "%02d:%02d", minutes, secs
                    )

                    soundOnReleaseVoiceMessage()

                }

            }

            else{
                showUnBlockContactDialog()
            }

            true
        }

        activityPrivateChatBinding.blockInfoTextView.setOnClickListener {
            showUnBlockContactDialog()
        }


        activityPrivateChatBinding.mainToolbar.setOnClickListener {
                activityPrivateChatBinding.replyMessageLayout.visibility = View.VISIBLE
        }

        activityPrivateChatBinding.cancelReplyingImageView.setOnClickListener {
            activityPrivateChatBinding.replyMessageLayout.visibility = View.GONE
        }



    }


    private fun checkForUpButton() {
        if (!longClick) {
            finish()
            longClick = true
        }
        else{
            clearMenuAndShowOriginalMenu()
            if (keysSelectedOnLongClick.isNotEmpty()) {
                //to remove any background
               onStart()
            }
            myItemView.setBackgroundResource(android.R.color.transparent)
          //  messagesAdapter.getItemViewType(myViewHolder.adapterPosition)
            longClick = false
        }
    }

    private fun clearMenuAndShowMessageStuff() {
        myMenu.clear()
        menuInflater.inflate(R.menu.long_click_menu,myMenu)
        toolbarView.user_image_view_custom.visibility = View.GONE
        toolbarView.user_name_text_view_custom.visibility = View.GONE
        toolbarView.user_last_seen_custom.visibility = View.GONE
    }

    private fun clearMenuAndShowOriginalMenu() {
        myMenu.clear()
        menuInflater.inflate(R.menu.private_chat_menu,myMenu)
        toolbarView.user_image_view_custom.visibility = View.VISIBLE
        toolbarView.user_name_text_view_custom.visibility = View.VISIBLE
        toolbarView.user_last_seen_custom.visibility = View.VISIBLE
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

                val messageSenderRef = "${MESSAGES_CHILD}/$senderId/$receiverId"
                val messageReceiverRef = "${MESSAGES_CHILD}/$receiverId/$senderId"

                val userMessageKeyRef = rootRef.child(MESSAGES_CHILD).
                child(senderId).child(receiverId).push()

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
                            rootRef.child("Messages").child(senderId).child(receiverId)
                                .child(messagePushId).child("message")
                                .setValue(it.result.toString()).addOnCompleteListener {

                                }
                        }


                        val messageImageBody = HashMap<String, Any>()

                        messageImageBody["name"] = fileUri.lastPathSegment.toString()
                        messageImageBody["type"] = checker
                        messageImageBody["from"] = senderId
                        messageImageBody["to"] = receiverId
                        messageImageBody["messageKey"] = messagePushId
                        messageImageBody["date"] = currentDate
                        messageImageBody["time"] = currentTime
                        messageImageBody["seen"] = "no"

                        val messageBodyDetails = HashMap<String, Any>()
                        messageBodyDetails["$messageSenderRef/$messagePushId"] = messageImageBody
                        messageBodyDetails["$messageReceiverRef/$messagePushId"] = messageImageBody



                        rootRef.updateChildren(messageBodyDetails)
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

                val messageSenderRef = "${MESSAGES_CHILD}/$senderId/$receiverId"
                val messageReceiverRef = "${MESSAGES_CHILD}/$receiverId/$senderId"

                val userMessageKeyRef = rootRef.child(MESSAGES_CHILD).
                child(senderId).child(receiverId).push()

                val messagePushId = userMessageKeyRef.key.toString()

                val filePath = storageRef.child("$messagePushId.mp4")
                val uploadTask = filePath.putFile(fileUri).addOnFailureListener{
                    progressDialog.dismiss()
                    Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
                }.addOnProgressListener {
                    val progress = ((100.0 * it.bytesTransferred) / it.totalByteCount).toInt()
                    progressDialog.show()
                    progressDialog.setMessage("$progress % Uploading...")
                }

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
                        messageImageBody["type"] = checker
                        messageImageBody["from"] = senderId
                        messageImageBody["to"] = receiverId
                        messageImageBody["messageKey"] = messagePushId
                        messageImageBody["date"] = currentDate
                        messageImageBody["time"] = currentTime
                        messageImageBody["seen"] = "no"
                        val messageBodyDetails = HashMap<String, Any>()
                        messageBodyDetails["$messageSenderRef/$messagePushId"] = messageImageBody
                        messageBodyDetails["$messageReceiverRef/$messagePushId"] = messageImageBody

                        rootRef.updateChildren(messageBodyDetails).addOnCompleteListener { task ->
                            if (!task.isSuccessful) {
                                Toast.makeText(this, task.exception?.message, Toast.LENGTH_SHORT).show()

                            }
                            progressDialog.dismiss()
                        }

                    }

                }


            }

            else if (checker == "audio") {
                fileUri = data.data!!
                val storageRef = FirebaseStorage.getInstance().reference.child("Audio files")

                val messageSenderRef = "${MESSAGES_CHILD}/$senderId/$receiverId"
                val messageReceiverRef = "${MESSAGES_CHILD}/$receiverId/$senderId"

                val userMessageKeyRef = rootRef.child(MESSAGES_CHILD).
                child(senderId).child(receiverId).push()

                val messagePushId = userMessageKeyRef.key.toString()

                val filePath = storageRef.child("$messagePushId.mp3")
                val uploadTask = filePath.putFile(fileUri).addOnFailureListener{
                    progressDialog.dismiss()
                    Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
                }.addOnProgressListener {
                    val progress = ((100.0 * it.bytesTransferred) / it.totalByteCount).toInt()
                    progressDialog.show()
                    progressDialog.setMessage("$progress % Uploading...")
                }

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
                        messageImageBody["type"] = checker
                        messageImageBody["from"] = senderId
                        messageImageBody["to"] = receiverId
                        messageImageBody["messageKey"] = messagePushId
                        messageImageBody["date"] = currentDate
                        messageImageBody["time"] = currentTime
                        messageImageBody["seen"] = "no"

                        val messageBodyDetails = HashMap<String, Any>()
                        messageBodyDetails["$messageSenderRef/$messagePushId"] = messageImageBody
                        messageBodyDetails["$messageReceiverRef/$messagePushId"] = messageImageBody

                        rootRef.updateChildren(messageBodyDetails).addOnCompleteListener { task ->
                            if (!task.isSuccessful) {
                                Toast.makeText(this, task.exception?.message, Toast.LENGTH_SHORT).show()

                            }
                            progressDialog.dismiss()
                        }

                    }

                }

            }



            else if (checker =="image") {
                fileUri = data.data!!
                val storageRef = FirebaseStorage.getInstance().reference.child("Image files")

                val messageSenderRef = "${MESSAGES_CHILD}/$senderId/$receiverId"
                val messageReceiverRef = "${MESSAGES_CHILD}/$receiverId/$senderId"

                val userMessageKeyRef = rootRef.child(MESSAGES_CHILD).
                child(senderId).child(receiverId).push()

                val messagePushId = userMessageKeyRef.key.toString()

                val filePath = storageRef.child("$messagePushId.jpg")
                val uploadTask = filePath.putFile(fileUri).addOnFailureListener{
                    progressDialog.dismiss()
                    Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
                }.addOnProgressListener {
                    val progress = ((100.0 * it.bytesTransferred) / it.totalByteCount).toInt()
                    progressDialog.show()
                    progressDialog.setMessage("$progress % Uploading...")
                }

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
                        messageImageBody["type"] = checker
                        messageImageBody["from"] = senderId
                        messageImageBody["to"] = receiverId
                        messageImageBody["messageKey"] = messagePushId
                        messageImageBody["date"] = currentDate
                        messageImageBody["time"] = currentTime
                        messageImageBody["seen"] = "no"
                        val messageBodyDetails = HashMap<String, Any>()
                        messageBodyDetails["$messageSenderRef/$messagePushId"] = messageImageBody
                        messageBodyDetails["$messageReceiverRef/$messagePushId"] = messageImageBody

                        rootRef.updateChildren(messageBodyDetails).addOnCompleteListener { task ->
                            if (!task.isSuccessful) {
                                Toast.makeText(this, task.exception?.message, Toast.LENGTH_SHORT).show()

                            }
                            progressDialog.dismiss()
                        }

                    }
                }


            }

        }

        else if (checker =="captured image" && requestCode == REQUEST_NUM && resultCode == RESULT_OK ) {

            //getLoadingDialog()
            val storageRef = FirebaseStorage.getInstance().reference.child("Image files")

            val image = data?.extras!!["data"] as Bitmap?

//           val width = image?.width
//           val height = image?.height
//
//            val size: Int = (image?.rowBytes)!! * (image.height)
//            val byteBuffer: ByteBuffer = ByteBuffer.allocate(size)
//            image.copyPixelsToBuffer(byteBuffer)
//            val byteArray = byteBuffer.array()
////

            val stream = ByteArrayOutputStream()
            image!!.compress(Bitmap.CompressFormat.PNG, 100, stream)

            val b = stream.toByteArray()

            val messageSenderRef = "${MESSAGES_CHILD}/$senderId/$receiverId"
            val messageReceiverRef = "${MESSAGES_CHILD}/$receiverId/$senderId"

            val userMessageKeyRef = rootRef.child(MESSAGES_CHILD).
            child(senderId).child(receiverId).push()

            val messagePushId = userMessageKeyRef.key.toString()

            val filePath = storageRef.child("$messagePushId.jpg")
            val uploadTask = filePath.putBytes(b).addOnFailureListener{
                progressDialog.dismiss()
                Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            }.addOnProgressListener {
                val progress = ((100.0 * it.bytesTransferred) / it.totalByteCount).toInt()
                progressDialog.show()
                progressDialog.setMessage("$progress % Uploading...")
            }

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
                    messageImageBody["name"] = "image $messagePushId"
                    messageImageBody["type"] = checker
                    messageImageBody["from"] = senderId
                    messageImageBody["to"] = receiverId
                    messageImageBody["messageKey"] = messagePushId
                    messageImageBody["date"] = currentDate
                    messageImageBody["time"] = currentTime
                    messageImageBody["seen"] = "no"
                    val messageBodyDetails = HashMap<String, Any>()
                    messageBodyDetails["$messageSenderRef/$messagePushId"] = messageImageBody
                    messageBodyDetails["$messageReceiverRef/$messagePushId"] = messageImageBody

                    rootRef.updateChildren(messageBodyDetails).addOnCompleteListener { task ->
                        if (!task.isSuccessful) {
                            Toast.makeText(this, task.exception?.message, Toast.LENGTH_SHORT).show()

                        }
                        progressDialog.dismiss()
                    }

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
        retrieveMessages()
        updateUserStatus("online")
        displayLastSeen()
        checkBlockedOrNot()
    }


    private fun setUpToolbar() {

         toolbarView = LayoutInflater.from(this).inflate(R.layout.custom_toolbar, null)



        setSupportActionBar(activityPrivateChatBinding.mainToolbar)
//        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowCustomEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        supportActionBar?.customView = toolbarView


        Log.i(TAG, "WWW setUpToolbar: $senderId")
        Log.i(TAG, "WWW setUpToolbar: $receiverId")


        userImageView = findViewById(R.id.user_image_view_custom)
        userNameTextView = findViewById(R.id.user_name_text_view_custom)
        lastSeenTextView = findViewById(R.id.user_last_seen_custom)
        upButtonImageView = findViewById(R.id.up_button_custom)


        userImageView.setOnClickListener {
            Toast.makeText(this, messagesList[0].messageKey, Toast.LENGTH_SHORT).show()
        }

            usersRef.child(receiverId).addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {

                    userNameTextView.text = snapshot.child("name").value.toString()

                    val imageUrl = snapshot.child("image").value.toString()
                    if (imageUrl.isNotEmpty()) {
                        Picasso.get()
                            .load(imageUrl)
                            .placeholder(R.drawable.ic_person)
                            .into(userImageView)
                    }

                }

                override fun onCancelled(error: DatabaseError) {
                }
            })

        upButtonImageView.setOnClickListener { checkForUpButton() }

        activityPrivateChatBinding.mainToolbar.setTitleTextColor(Color.WHITE)
        activityPrivateChatBinding.mainToolbar.overflowIcon?.setColorFilter(resources.getColor(R.color.white), PorterDuff.Mode.SRC_IN)
        activityPrivateChatBinding.mainToolbar.navigationIcon?.setColorFilter(resources.getColor(R.color.white), PorterDuff.Mode.SRC_IN)
    }


    @SuppressLint("SimpleDateFormat")
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

            val messageTextBody = HashMap<String, Any>()
            messageTextBody["message"] = currentMessage
            messageTextBody["type"] = "text"
            messageTextBody["from"] = senderId
            messageTextBody["to"] = receiverId
            messageTextBody["messageKey"] = messagePushId
            messageTextBody["date"] = currentDate
            messageTextBody["time"] = currentTime
            messageTextBody["seen"] = "no"

            val messageBodyDetails = HashMap<String, Any>()
            messageBodyDetails["$messageSenderRef/$messagePushId"] = messageTextBody
            messageBodyDetails["$messageReceiverRef/$messagePushId"] = messageTextBody

            val map = HashMap<String, Any>()
            map["to"] = receiverId
            map["from"] = senderId

            rootRef.child(USERS_CHILD).child(currentUserId).child("Chats").updateChildren(map).addOnCompleteListener {
                if (it.isComplete) {

                    rootRef.child(USERS_CHILD).child(receiverId).child("Chats").updateChildren(map)
                }
            }

            rootRef.updateChildren(messageBodyDetails).addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Toast.makeText(this, task.exception?.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        else{

        }
        //send a broadcast intent
       // sendBroadcast(Intent(ACTION_SHOW_NOTIFICATION), PERM_PRIVATE)
        activityPrivateChatBinding.sendMessageEditText.text.clear()
    }

    private fun getTokens() {
        usersRef.child(senderId).child(DEVICE_TOKEN_CHILD).addValueEventListener(object :
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                senderToken = snapshot.value.toString()
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
        usersRef.child(receiverId).child(DEVICE_TOKEN_CHILD).addValueEventListener(object :
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                receiverToken = snapshot.value.toString()
            }

            override fun onCancelled(error: DatabaseError) {
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
            @SuppressLint("SetTextI18n")
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

    @SuppressLint("SimpleDateFormat")
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
            backPressed = false
        }
        else{
              updateUserStatus("offline")

        }

    }

    override fun onDestroy() {
        super.onDestroy()
        if (backPressed){
            updateUserStatus("online")
            backPressed = false
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


            val receiverMessageGeneralLayout : LinearLayout = itemView.findViewById(R.id.receiver_message_general_layout)
            val receiverNameTextView : TextView = itemView.findViewById(R.id.receiver_name_text_view)

            val senderMessageGeneralLayout : LinearLayout = itemView.findViewById(R.id.sender_message_general_layout)
            val senderNameTextView : TextView = itemView.findViewById(R.id.sender_name_text_view)

            val messageSentTimeTextView:TextView = itemView.findViewById(R.id.message_time_sent_text_view)
            val messageReceivedTimeTextView:TextView = itemView.findViewById(R.id.message_time_received_text_view)

            val globalSenderLayout:LinearLayout = itemView.findViewById(R.id.sender_message_layout)
            val globalReceiverLayout:LinearLayout = itemView.findViewById(R.id.receiver_message_general_layout)



            init {
                itemView.setOnClickListener(this)
                itemView.setOnLongClickListener(this)
                myItemView = itemView
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
                if ( keysSelectedOnLongClick.containsKey(adapterPosition)){
                    item?.setBackgroundResource(android.R.color.transparent)
                    keysSelectedOnLongClick.remove(adapterPosition)
                    shortClick = true

                    if (keysSelectedOnLongClick.isEmpty()){
                        clearMenuAndShowOriginalMenu()
                    }
                }

                if (longClick && !shortClick) {
                    item?.setBackgroundResource(R.color.light_blue)
                    keysSelectedOnLongClick[adapterPosition] = messagesList[adapterPosition].messageKey
                }

                shortClick = false

            }

            //to get message date
            @SuppressLint("SimpleDateFormat")
            override fun onLongClick(view: View?): Boolean {
                longClick = true

                view?.setBackgroundResource(R.color.light_blue)
                keysSelectedOnLongClick[adapterPosition] = messagesList[adapterPosition].messageKey
                if (messages[adapterPosition].type == "pdf" || messages[adapterPosition].type == "docx"){
                    clearMenuAndShowMessageStuff()
                }

                else if (messages[adapterPosition].type == "text"){
                    clearMenuAndShowMessageStuff()

                }

                else if (messages[adapterPosition].type == "image" || messages[adapterPosition].type == "captured image"){
                    clearMenuAndShowMessageStuff()
                }

                else if (messages[adapterPosition].type == "audio"){
                    clearMenuAndShowMessageStuff()
                }

                else if (messages[adapterPosition].type == "video"){
                    clearMenuAndShowMessageStuff()
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

            myViewHolder = PopularMoviesViewHolder(view)

            return PopularMoviesViewHolder(view)
        }

        override fun getItemCount(): Int {
            return messages.size
        }

        override fun onBindViewHolder(holder: PopularMoviesViewHolder, position: Int) {

             messageSenderId = auth.currentUser?.uid.toString()

            val currentMessage = messages[position]

            val fromUserId = currentMessage.from
            val fromMessagesType = currentMessage.type

            usersRef = rootRef.child(USERS_CHILD).child(fromUserId)

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

                holder.messageSentTimeTextView.visibility = View.GONE
                holder.messageReceivedTimeTextView.visibility = View.GONE

                if (fromUserId == messageSenderId) {
                    holder.senderMessageTextView.setBackgroundResource(R.drawable.sender_messages_background)
                    holder.senderMessageTextView.text = currentMessage.message
                    holder.senderMessageTimeTextView.text = currentMessage.time
                    holder.senderMessageTextView.visibility = View.VISIBLE
                    holder.senderMessageLayout.visibility = View.VISIBLE
                    holder.senderMessageTimeTextView.visibility = View.VISIBLE


                    holder.receiverMessageTextView.visibility = View.GONE
                    holder.receiverMessageLayout.visibility = View.GONE
                    holder.receiverMessageTimeTextView.visibility = View.GONE
                    holder.receiverMessageGeneralLayout.visibility = View.GONE




                }

                else{
                    holder.senderMessageTextView.visibility = View.GONE
                    holder.senderMessageLayout.visibility = View.GONE
                    holder.senderMessageTimeTextView.visibility = View.GONE
                    holder.senderMessageGeneralLayout.visibility = View.GONE
                    holder.senderNameTextView.visibility = View.GONE


                    holder.receiverMessageTextView.visibility = View.VISIBLE
                    holder.receiverMessageLayout.visibility = View.VISIBLE
                    holder.receiverMessageTimeTextView.visibility = View.VISIBLE
                    holder.receiverMessageGeneralLayout.visibility = View.VISIBLE

                    holder.receiverMessageTextView.setBackgroundResource(R.drawable.receiver_messages_background)
                    holder.receiverMessageTextView.text = currentMessage.message
                    holder.receiverMessageTimeTextView.text = currentMessage.time





                }
            }

            else if (fromMessagesType == "image" || fromMessagesType == "captured image") {

                holder.receiverMessageTextView.visibility = View.GONE
                holder.senderMessageTextView.visibility = View.GONE
                holder.senderMessagePlay.visibility = View.GONE
                holder.receiverMessagePlay.visibility = View.GONE




                holder.messageSentTimeTextView.visibility = View.GONE
                holder.messageReceivedTimeTextView.visibility = View.GONE

//                holder.receiverMessageGeneralLayout.layoutParams = LinearLayout.LayoutParams(512,512)
//                holder.senderMessageGeneralLayout.layoutParams = LinearLayout.LayoutParams(512,512)

                if (fromUserId == messageSenderId) {
                    holder.senderMessageTimeTextView.visibility = View.VISIBLE
                    holder.senderMessageTimeTextView.text = currentMessage.time
                    holder.receiverMessageLayout.visibility = View.GONE
                    holder.receiverMessageGeneralLayout.visibility = View.GONE

                    holder.senderMessageImageView.visibility = View.VISIBLE
                    holder.receiverMessageImageView.visibility = View.GONE


                    Picasso.get()
                        .load(currentMessage.message)
                        .into(holder.senderMessageImageView)

                    holder.senderMessageImageView.setOnClickListener {
                        showSentImage(currentMessage.message)
                    }

                }

                else{

                    holder.receiverMessageTimeTextView.visibility = View.VISIBLE
                    holder.receiverMessageTimeTextView.text = currentMessage.time
                    holder.senderMessageLayout.visibility = View.GONE
                    holder.senderMessageGeneralLayout.visibility = View.GONE
                    holder.senderNameTextView.visibility = View.GONE

                    holder.receiverMessageImageView.visibility = View.VISIBLE
                    holder.senderMessageImageView.visibility = View.GONE
                    holder.receiverMessageGeneralLayout.visibility = View.VISIBLE




                    Picasso.get()
                        .load(currentMessage.message)
                        .into(holder.receiverMessageImageView)

                    holder.receiverMessageImageView.setOnClickListener {
                        showSentImage(currentMessage.message)
                    }
                }
            }

            else if (fromMessagesType == "video") {

                holder.receiverMessageTextView.visibility = View.GONE
                holder.senderMessageTextView.visibility = View.GONE


                holder.messageSentTimeTextView.visibility = View.GONE
                holder.messageReceivedTimeTextView.visibility = View.GONE

//                holder.receiverMessageGeneralLayout.layoutParams = LinearLayout.LayoutParams(512,512)
//                holder.senderMessageGeneralLayout.layoutParams = LinearLayout.LayoutParams(512,512)

                if (fromUserId == messageSenderId) {
                    holder.senderMessageTimeTextView.visibility = View.VISIBLE
                    holder.senderMessageTimeTextView.text = currentMessage.time
                    holder.receiverMessageLayout.visibility = View.GONE


                    holder.senderMessageImageView.visibility = View.VISIBLE
                    holder.receiverMessageImageView.visibility = View.GONE


                    //   holder.senderMessageImageView.setImageResource(R.drawable.ic_video)
                    holder.receiverMessageImageView.visibility = View.GONE
                    holder.receiverMessageGeneralLayout.visibility = View.GONE

                    //Chosen frame interval
                    val interval: Long = 1* 1000
                    val options: RequestOptions = RequestOptions().frame(interval)

                    Glide.with(this@PrivateChatActivity)
                        .asBitmap().load(currentMessage.message).apply(options).into(holder.senderMessageImageView)

                    holder.senderMessageImageView.setOnClickListener {

                        val videoIntent = Intent(
                            this@PrivateChatActivity,
                            VideoPlayerActivity::class.java
                        )
                        videoIntent.putExtra(VIDEO_URL, currentMessage.message)
                        startActivity(videoIntent)

                        // showVideoPlayerDialog(myMessages.message)


                    }

                }

                else{

                    holder.receiverMessageTimeTextView.visibility = View.VISIBLE
                    holder.receiverMessageTimeTextView.text = currentMessage.time
                    holder.senderMessageLayout.visibility = View.GONE
                    holder.senderMessageGeneralLayout.visibility = View.GONE
                    holder.senderNameTextView.visibility = View.GONE

                    holder.receiverMessageImageView.visibility = View.VISIBLE
                    holder.senderMessageImageView.visibility = View.GONE

                    holder.receiverMessageGeneralLayout.visibility = View.VISIBLE

                    //
                    //  holder.receiverMessageImageView.setImageResource(R.drawable.ic_video)


                    val interval: Long = 1* 1000
                    val options: RequestOptions = RequestOptions().frame(interval)

                    Glide.with(this@PrivateChatActivity)
                        .asBitmap().load(currentMessage.message).apply(options).into(holder.receiverMessageImageView)



                    holder.receiverMessageImageView.setOnClickListener {
                        val videoIntent = Intent(
                            this@PrivateChatActivity,
                            VideoPlayerActivity::class.java
                        )
                        videoIntent.putExtra(VIDEO_URL, currentMessage.message)
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
                    holder.senderMessageTimeTextView.text = currentMessage.time
                    holder.receiverMessageLayout.visibility = View.GONE


                    holder.senderMessageImageView.visibility = View.VISIBLE
                    holder.receiverMessageImageView.visibility = View.GONE


                    holder.receiverNameTextView.visibility = View.GONE

                    holder.messageSentTimeTextView.visibility = View.VISIBLE

                    holder.messageSentTimeTextView.text = currentMessage.messageTime

                    holder.messageReceivedTimeTextView.visibility = View.INVISIBLE



                    holder.senderMessageImageView.setImageResource(R.drawable.ic_mic)
                    holder.receiverMessageImageView.visibility = View.GONE

                    holder.receiverMessageGeneralLayout.visibility = View.GONE


                    holder.senderMessageImageView.setOnClickListener {

                        val videoIntent = Intent(
                            this@PrivateChatActivity,
                            VideoPlayerActivity::class.java
                        )
                        videoIntent.putExtra(VIDEO_URL, currentMessage.message)
                        startActivity(videoIntent)

                        // showVideoPlayerDialog(myMessages.message)


                    }

                }

                else{

                    holder.receiverMessageTimeTextView.visibility = View.VISIBLE
                    holder.receiverMessageTimeTextView.text = currentMessage.time
                    holder.senderMessageLayout.visibility = View.GONE

                    holder.receiverMessageImageView.visibility = View.VISIBLE
                    holder.senderMessageImageView.visibility = View.GONE
                    holder.senderMessageGeneralLayout.visibility = View.GONE
                    holder.senderNameTextView.visibility = View.GONE

                    holder.receiverMessageGeneralLayout.visibility = View.VISIBLE

                    holder.messageSentTimeTextView.visibility = View.INVISIBLE
                    holder.messageReceivedTimeTextView.visibility = View.VISIBLE

                    holder.messageReceivedTimeTextView.text = currentMessage.messageTime

                    holder.receiverMessageImageView.setImageResource(R.drawable.ic_mic)

                    holder.receiverMessageImageView.setOnClickListener {
                        val videoIntent = Intent(
                            this@PrivateChatActivity,
                            VideoPlayerActivity::class.java
                        )
                        videoIntent.putExtra(VIDEO_URL, currentMessage.message)
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


                holder.messageSentTimeTextView.visibility = View.GONE
                holder.messageReceivedTimeTextView.visibility = View.GONE

                if (fromUserId == messageSenderId) {
                    holder.senderMessageTimeTextView.visibility = View.VISIBLE
                    holder.senderMessageTimeTextView.text = currentMessage.time
                    holder.receiverMessageLayout.visibility = View.GONE


                    holder.senderMessageImageView.visibility = View.VISIBLE
                    holder.receiverMessageImageView.visibility = View.GONE

                    holder.senderMessageImageView.setImageResource(R.drawable.ic_file)
                    holder.receiverMessageImageView.visibility = View.GONE

                    holder.receiverMessageGeneralLayout.visibility = View.GONE



                }
                else{
                    holder.receiverMessageTimeTextView.visibility = View.VISIBLE
                    holder.receiverMessageTimeTextView.text = currentMessage.time
                    holder.senderMessageLayout.visibility = View.GONE
                    holder.senderMessageGeneralLayout.visibility = View.GONE
                    holder.senderNameTextView.visibility = View.GONE

                    holder.receiverMessageImageView.visibility = View.VISIBLE
                    holder.senderMessageImageView.visibility = View.GONE

                    holder.receiverMessageGeneralLayout.visibility = View.VISIBLE


                    holder.receiverMessageImageView.setImageResource(R.drawable.ic_file)

                }
            }

//            if (fromUserId == senderId) {
//                holder.itemView.setOnLongClickListener {
//
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
        myMenu = menu!!
        return true
    }

    private fun deleteSelectedMessages(positions:MutableList<Int>) {

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.establish_video_chat -> {
                if (!blocked) {
                    makeVideoCall()
                } else {
                    showUnBlockContactDialog()
                }
            }
            R.id.establish_audio_chat ->{
                if (!blocked) {
                    makeAudioCall()
                } else {
                    showUnBlockContactDialog()
                }
            }
            R.id.clear_chat -> showClearChatDialog()
            R.id.private_chat_block_user -> showBlockContactDialog()
            R.id.reply_message -> Toast.makeText(this, "Reply", Toast.LENGTH_SHORT).show()
            R.id.star_message -> Toast.makeText(this, "Star", Toast.LENGTH_SHORT).show()
            R.id.delete_message -> removeMessage()
            R.id.copy_text_message -> Toast.makeText(this, "Copy", Toast.LENGTH_SHORT).show()
            R.id.forward_message -> Toast.makeText(this, "Forward", Toast.LENGTH_SHORT).show()


        }
        return super.onOptionsItemSelected(item)
    }

    private fun showBlockContactDialog() {
        alertBuilder = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.clear_chat_dialog,null)
        alertBuilder.setView(dialogView)


        val groupDialog =  alertBuilder.create()

        dialogView.clear.text = "Block"
        dialogView.info_text_view.text = "Blocked contacts will no longer be able to call you or send you messages"

        groupDialog.show()

        dialogView.clear.setOnClickListener {
            blockContact()
            groupDialog.dismiss()
        }

        dialogView.cancel.setOnClickListener {
            groupDialog.dismiss()
        }

    }

    private fun showUnBlockContactDialog() {
        alertBuilder = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.clear_chat_dialog,null)
        alertBuilder.setView(dialogView)


        val groupDialog =  alertBuilder.create()

        dialogView.clear.text = "UnBlock"
        dialogView.info_text_view.text = "Unblock this contact?"

        groupDialog.show()

        dialogView.clear.setOnClickListener {
            unBlockContact()
            groupDialog.dismiss()
        }

        dialogView.cancel.setOnClickListener {
            groupDialog.dismiss()
        }

    }

    private fun blockContact() {
        val blockerMap = HashMap<String,Any>()
        blockerMap[receiverId] = ""

        val blockedMap = HashMap<String,Any>()
        blockedMap[currentUserId] = ""

        rootRef.child(USERS_CHILD).child(currentUserId).child("Blocked").updateChildren(blockerMap).addOnCompleteListener {
            if (it.isComplete){
                rootRef.child(USERS_CHILD).child(receiverId).child("Blocked").updateChildren(blockedMap).addOnCompleteListener {
                }
            }
        }

    }

    private fun unBlockContact() {

        rootRef.child(USERS_CHILD).child(currentUserId).child("Blocked").child(receiverId).removeValue().addOnCompleteListener {
            if (it.isComplete){
                rootRef.child(USERS_CHILD).child(receiverId).child("Blocked").child(currentUserId).removeValue().addOnCompleteListener {
                    if (it.isComplete){
                        blocked = false
                        activityPrivateChatBinding.blockInfoTextView.visibility = View.GONE
                    }
                }
            }
        }

    }

    private fun clearChat() {
       rootRef.child(MESSAGES_CHILD).child(currentUserId).removeValue().addOnCompleteListener {
           if (it.isComplete){
               rootRef.child(MESSAGES_CHILD).child(receiverId).removeValue().addOnCompleteListener {
                   Toast.makeText(this, "Chat cleared successfully ", Toast.LENGTH_SHORT).show()
               }
           }
       }
    }

    private fun removeMessage() {

        for (messageKey in keysSelectedOnLongClick) {
            rootRef.child(MESSAGES_CHILD).child(currentUserId).child(receiverId).child(messageKey.value).removeValue().addOnCompleteListener {
                if (it.isComplete){
                    rootRef.child(MESSAGES_CHILD).child(receiverId).child(currentUserId).child(messageKey.value).removeValue().addOnCompleteListener{
                        if (it.isComplete){
                            messagesAdapter.notifyDataSetChanged()
                            myItemView.setBackgroundResource(android.R.color.transparent)
                        }
                    }
                }
            }
        }

    }


    @SuppressLint("SimpleDateFormat")
    private fun makeVideoCall () {
//        val callingIntent = Intent(this, VideoChatActivity::class.java)
//        callingIntent.putExtra(RECEIVER_ID, receiverId)
//        startActivity(callingIntent)
//        pushVideoChatNotificationRequest()
        val callKeyRef = rootRef.child(USERS_CHILD).
        child(senderId).child("Calls").push()

        val messagePushId = callKeyRef.key.toString()
        val calender = Calendar.getInstance()
        //get date and time
        val dateFormat = SimpleDateFormat("MMM dd, yyyy")
        val timeFormat = SimpleDateFormat("hh:mm a")

        currentDate = dateFormat.format(calender.time)
        currentTime = timeFormat.format(calender.time)

        val callMap = HashMap<String, Any>()
        callMap["toid"] = receiverId
        callMap["time"] = currentTime
        callMap["date"] = currentDate
        callMap["type"] = "video"
        callMap["caller"] = senderId

        rootRef.child("Users").child(senderId).child("Calls").child(messagePushId).updateChildren(
            callMap
        )
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    rootRef.child("Users").child(receiverId).child("Calls").child(messagePushId).updateChildren(
                        callMap
                    )
                        .addOnCompleteListener {

                        }
                }

                else{
                    Toast.makeText(this, task.exception?.message, Toast.LENGTH_SHORT).show()
                }
            }

    }

    @SuppressLint("SimpleDateFormat")
    private fun makeAudioCall () {
//        val callingIntent = Intent(this, VoiceCallActivity::class.java)
//        callingIntent.putExtra(RECEIVER_ID, receiverId)
//        startActivity(callingIntent)
        //pushVideoChatNotificationRequest()
        val callKeyRef = rootRef.child(USERS_CHILD).
        child(senderId).child("Calls").push()

        val messagePushId = callKeyRef.key.toString()

        val calender = Calendar.getInstance()
        //get date and time
        val dateFormat = SimpleDateFormat("MMM dd, yyyy")
        val timeFormat = SimpleDateFormat("hh:mm a")

        currentDate = dateFormat.format(calender.time)
        currentTime = timeFormat.format(calender.time)

        val callMap = HashMap<String, Any>()
        callMap["toid"] = receiverId
        callMap["time"] = currentTime
        callMap["date"] = currentDate
        callMap["type"] = "audio"
        callMap["caller"] = senderId

        rootRef.child("Users").child(senderId).child("Calls").child(messagePushId).updateChildren(
            callMap
        )
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    rootRef.child("Users").child(receiverId).child("Calls").child(messagePushId).updateChildren(
                        callMap
                    )
                        .addOnCompleteListener {

                        }
                }

                else{
                    Toast.makeText(this, task.exception?.message, Toast.LENGTH_SHORT).show()
                }
            }
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

    fun showSentImage(imageUrl: String) {
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
                Log.e(TAG, "startRecording: prepare() failed")
            }

            start()
        }
    }

    private fun checkBlockedOrNot() {
        rootRef.child(USERS_CHILD).child(receiverId).child("Blocked").addValueEventListener(object :
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (child in snapshot.children) {
                    if (child.key.toString() == senderId) {
                        blocked = true
                        activityPrivateChatBinding.blockInfoTextView.visibility = View.VISIBLE
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
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
        val uploadTask = filePath.putFile(fileUri).addOnFailureListener{
            progressDialog.dismiss()
            Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
        }.addOnProgressListener {
            val progress = ((100.0 * it.bytesTransferred) / it.totalByteCount).toInt()
            progressDialog.show()
            progressDialog.setMessage("$progress % Uploading...")
        }



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
                messageImageBody["messageTime"] = messageTime

                val messageBodyDetails = HashMap<String, Any>()
                messageBodyDetails["$messageSenderRef/$messagePushId"] = messageImageBody
                messageBodyDetails["$messageReceiverRef/$messagePushId"] = messageImageBody

                rootRef.updateChildren(messageBodyDetails).addOnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        Toast.makeText(this, task.exception?.message, Toast.LENGTH_SHORT).show()

                    }
                }

            }
            progressDialog.dismiss()
        }
    }

    private fun soundOnStartVoiceMessage() {
        val mediaPlayer: MediaPlayer? = MediaPlayer.create(this, R.raw.whatsapp_voice_message_start)
        mediaPlayer?.start()
        // no need to call prepare(); create() does that for you
        mediaPlayer?.setOnCompletionListener {
            startRecording()
        }
    }

    private fun soundOnReleaseVoiceMessage() {
        val mediaPlayer: MediaPlayer? = MediaPlayer.create(
            this,
            R.raw.whatsapp_voice_message_release
        )
        stopRecording()
        mediaPlayer?.start() // no need to call prepare(); create() does that for you
        mediaPlayer?.setOnCompletionListener {
            showVoiceMessageDialog()
        }
    }

    private fun showVoiceMessageDialog(){
        alertBuilder = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.clear_chat_dialog,null)
        alertBuilder.setView(dialogView)

        dialogView.info_text_view.text = "Send this voice?"
        dialogView.clear.text = "Send"

        val groupDialog =  alertBuilder.create()
        groupDialog.show()

        dialogView.clear.setOnClickListener {
            sendVoiceMessage()
            groupDialog.dismiss()
        }

        dialogView.cancel.setOnClickListener {
            groupDialog.dismiss()
        }

    }


    private fun showClearChatDialog(){
        alertBuilder = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.clear_chat_dialog,null)
        alertBuilder.setView(dialogView)


       val groupDialog =  alertBuilder.create()
        groupDialog.show()

        dialogView.clear.setOnClickListener {
            clearChat()
            groupDialog.dismiss()
        }

        dialogView.cancel.setOnClickListener {
            groupDialog.dismiss()
        }

    }

    private fun retrieveMessages() {
        rootRef.child(MESSAGES_CHILD).child(currentUserId).child(receiverId).addValueEventListener(
            object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    messagesList.clear()
                    var messageTime = ""
                    for (message in snapshot.children) {

                        val date = message.child("date").value.toString()
                        val from = message.child("from").value.toString()
                        val messageBody = message.child("message").value.toString()
                        val messageKey = message.child("messageKey").value.toString()
                        val seen = message.child("seen").value.toString()
                        val time = message.child("time").value.toString()
                        val to = message.child("to").value.toString()
                        val type = message.child("type").value.toString()

                        if (message.hasChild("messageTime")) {
                            messageTime = message.child("messageTime").value.toString()
                        } else {
                            messageTime = ""
                        }

                        val currentMessage = PrivateMessageModel(
                            from,
                            messageBody,
                            type,
                            to,
                            seen,
                            messageKey,
                            date,
                            time,
                            messageTime,
                            ""
                        )

                        messagesList.add(currentMessage)
                    }

                    messagesAdapter = PrivateMessagesAdapter(messagesList)
                    activityPrivateChatBinding.privateChatRecyclerView.adapter = messagesAdapter
//                messagesAdapter.notifyDataSetChanged()
                    //to scroll to the bottom of recycler view
                    if (messagesList.isNotEmpty()) {
                        activityPrivateChatBinding.privateChatRecyclerView.smoothScrollToPosition(
                            messagesList.size - 1
                        )
                    }

                }

                override fun onCancelled(error: DatabaseError) {
                }
            })
    }

}