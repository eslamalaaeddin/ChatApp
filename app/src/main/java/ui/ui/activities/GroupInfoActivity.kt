package ui.ui.activities

import android.app.ActionBar
import android.app.Dialog
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.whatsapp.BottomSheetDialog
import com.example.whatsapp.ChangeIconBottomSheet
import com.example.whatsapp.R
import com.example.whatsapp.Utils
import com.example.whatsapp.Utils.USERS_CHILD
import com.example.whatsapp.databinding.ActivityGroupInfoBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.change_group_picture_layout.*
import kotlinx.android.synthetic.main.clear_chat_dialog.view.*
import kotlinx.android.synthetic.main.clicked_participant_dialog.view.*
import kotlinx.android.synthetic.main.description_group_add_layout.*
import kotlinx.android.synthetic.main.description_group_add_layout.cancel_adding_description
import kotlinx.android.synthetic.main.description_group_add_layout.submit_adding_description
import kotlinx.android.synthetic.main.new_subject_layout.*
import models.ContactsModel
import models.PrivateMessageModel
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

private const val USER_ID = "user id"
private const val GROUP_ID = "group id"
private const val TAG = "GroupInfoActivity"
private const val VIDEO_URL = "video url"
private const val REQUEST_NUM = 1
class GroupInfoActivity : AppCompatActivity(),ChangeIconBottomSheet.BottomSheetListener {
    private lateinit var groupBinding: ActivityGroupInfoBinding

    private lateinit var upButtonImageView: ImageView
    private lateinit var groupNameTextView: TextView
    private lateinit var groupStatusTextView: TextView


    private lateinit var alertBuilder: AlertDialog.Builder
    private lateinit var addNoteAlertDialog: AlertDialog
    private lateinit var view: View

    private lateinit var myNameTextView:TextView
    private lateinit var myStatusTextView:TextView
    private lateinit var myImage: ImageView

    private  var messagesAdapter = MediaAdapter(emptyList())
    private var messagesList = mutableListOf<PrivateMessageModel>()

    private  var participantsAdapter = ParticipantsAdapter(emptyList())
    private var participantsList = mutableListOf<ContactsModel>()

    private lateinit var groupName: String
    private lateinit var groupAdmin: String
    private lateinit var groupImageUrl: String
    private lateinit var groupStatus: String

    private lateinit var usersReference: DatabaseReference
    private lateinit var rootReference: DatabaseReference

    private var participantsIds = mutableListOf<String>()
    private var messagesIds = mutableListOf<String>()

    private lateinit var auth: FirebaseAuth

    private lateinit var currentUser: FirebaseUser
    private lateinit var currentUserId:String

    private lateinit var groupId:String

    private var participantToBeDeletedIndex:Int = -1

    private var idsToBeAdded = mutableListOf("HwjJm7TzEpa7zFCNyMUuJdp0eK02")

    private var groupDialog : Dialog? = null
    private var subjectDialog : Dialog? = null
    private var iconDialog : Dialog? = null
    private lateinit var bottomSheet: ChangeIconBottomSheet

    private var checker:String = ""
  //  private lateinit var progressDialog: ProgressDialog

    private var url:String = ""
    private lateinit var fileUri: Uri

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
       groupBinding = DataBindingUtil.setContentView(this,R.layout.activity_group_info)

        auth = FirebaseAuth.getInstance()
        currentUser = auth.currentUser!!
        currentUserId = currentUser.uid

        usersReference = FirebaseDatabase.getInstance().reference.child("Users")
        rootReference = FirebaseDatabase.getInstance().reference

        groupId = intent.getStringExtra(GROUP_ID).toString()
        //groupId = "8a722852-4609-43c5-87a3-5c9f9c054de8"

        groupBinding.mediaRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL,false)
        groupBinding.participantsRecyclerView.layoutManager = LinearLayoutManager(this)

        groupBinding.addGroupDescription.setOnClickListener {
            showPickDescriptionDialog()
        }

        groupBinding.exitGroupLayout.setOnClickListener {
            showExitGroupDialog()
        }

        groupBinding.mainToolbar.setOnClickListener {
            showChangeGroupIconDialog()
        }


    }

    override fun onStart() {
        super.onStart()
        retrieveMessages()
        retrieveMyInfo()
        setUpToolbar()
        //retrieveParticipants()
        retrieveGroupDescriptionValue()
    }

    private fun setUpToolbar() {
        val toolbarView = LayoutInflater.from(this).inflate(R.layout.custom_toolbar, null)
        setSupportActionBar(groupBinding.mainToolbar)
        //supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowCustomEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.subtitle= "tap here for group info"

        supportActionBar?.customView = toolbarView

        toolbarView.setBackgroundResource(android.R.color.transparent)

       val groupImageView: ImageView = findViewById(R.id.user_image_view_custom)
        groupNameTextView = findViewById(R.id.user_name_text_view_custom)
        groupStatusTextView = findViewById(R.id.user_last_seen_custom)
      val  upButtonImageView: ImageView  = findViewById(R.id.up_button_custom)

        groupImageView.visibility = View.GONE
        groupNameTextView.visibility = View.GONE
        groupStatusTextView.visibility = View.GONE


       // upButtonImageView.setBackgroundResource(android.R.color.transparent)

        upButtonImageView.setOnClickListener {
            //temp
            finish()
        }

        //Fetch group data
        rootReference.child("Groups").child(groupId).addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                participantsIds.clear()
                messagesIds.clear()
                 groupName = snapshot.child("name").value.toString()
                 groupAdmin = snapshot.child("admin").value.toString()
                 groupImageUrl = snapshot.child("image").value.toString()
                 groupStatus = snapshot.child("status").value.toString()

                for (participant in snapshot.child("participants").children){
                    participantsIds.add(participant.key.toString())
                }

                for (message in snapshot.child("Messages").children){
                    messagesIds.add(message.key.toString())
                }

                retrieveParticipants()

                groupBinding.mainToolbar.title = groupName

                if (groupImageUrl.isNotEmpty()){
                    Picasso.get().load(groupImageUrl).into(groupBinding.groupImageView)
                }
                else{
                    groupBinding.groupImageView.setImageResource(R.drawable.ic_group)
                }

            }

            override fun onCancelled(error: DatabaseError) {
            }
        })

        groupBinding.groupImageView.setOnClickListener {
            onStart()
        }


//        groupBinding.mainToolbar.subtitle = "tap here for group info"
        groupBinding.mainToolbar.setTitleTextColor(Color.WHITE)
        groupBinding.mainToolbar.setSubtitleTextColor(Color.WHITE)
        groupBinding.mainToolbar.overflowIcon?.setColorFilter(resources.getColor(R.color.white), PorterDuff.Mode.SRC_IN)
        groupBinding.mainToolbar.navigationIcon?.setColorFilter(resources.getColor(R.color.white), PorterDuff.Mode.SRC_IN)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.group_info_menu,menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.add_participant -> {
//                startActivity(Intent(this,AddGroupActivity::class.java))
                addParticipants(idsToBeAdded)
            }

            R.id.edit_name -> {
                showAddNewSubjectDialog()
                setUpToolbar()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun retrieveMessages() {
        rootReference.child("Groups").child(groupId).child(Utils.MESSAGES_CHILD).addValueEventListener(
            object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    messagesList.clear()
                    for (message in snapshot.children) {
                        val type = message.child("type").value.toString()
                        if (type == "video" || type == "image" || type == "captured image") {
                            val messageKey = message.child("messageKey").value.toString()
                            val message = message.child("message").value.toString()
                            val currentMessage = PrivateMessageModel(
                                "",
                                message,
                                type,
                                "",
                                "",
                                messageKey,
                                "",
                                "",
                                "",
                                ""
                            )

                            messagesList.add(0,currentMessage)
                        }
                        groupBinding.mediaCountTextView.text = "${messagesList.size} >"
                    }

                    messagesAdapter = MediaAdapter(messagesList)
                    groupBinding.mediaRecyclerView.adapter = messagesAdapter

//                messagesAdapter.notifyDataSetChanged()
                    //to scroll to the bottom of recycler view
                    if (messagesList.isNotEmpty()) {
                        groupBinding.mediaRecyclerView.smoothScrollToPosition(0)
                    }

                }

                override fun onCancelled(error: DatabaseError) {
                }
            })
    }

    private fun retrieveParticipants(){
        participantsList.clear()
       for (participant in participantsIds) {
           if (participant != currentUserId) {
               rootReference.child(USERS_CHILD).child(participant)
                   .addValueEventListener(object : ValueEventListener {
                       override fun onDataChange(snapshot: DataSnapshot) {
                           //participantsList.clear()
                           val name = snapshot.child("name").value.toString()
                           val imageUrl = snapshot.child("image").value.toString()
                           val status = snapshot.child("status").value.toString()
                           val uid = snapshot.child("uid").value.toString()

                           participantsList.add(ContactsModel(name, imageUrl, status, uid, ""))

                           participantsAdapter = ParticipantsAdapter(participantsList)
                           groupBinding.participantsRecyclerView.adapter = participantsAdapter
                       }


                       override fun onCancelled(error: DatabaseError) {
                       }
                   })
           }
       }

        groupBinding.participantsCountTextView.text = "${participantsIds.size} participants"

    }

    inner class MediaAdapter (private var list:List<PrivateMessageModel>) : RecyclerView.Adapter<MediaAdapter.MediaHolder>() {

        inner class MediaHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener, View.OnLongClickListener {
            private var mediaImageView:ImageView = itemView.findViewById(R.id.media_image_view)
            private var videoInfoLayout:LinearLayout = itemView.findViewById(R.id.video_info_layout)
            private var mediaDurationTextView:TextView = itemView.findViewById(R.id.media_duration_text_view)

            init {
                itemView.setOnClickListener(this)
                itemView.setOnLongClickListener(this)
            }


            fun bind (mediaMessage : PrivateMessageModel) {
                //media is images
                if (mediaMessage.type == "image" || mediaMessage.type == "captured image"){
                    Picasso.get().load(mediaMessage.message).into(mediaImageView)
                    videoInfoLayout.visibility = View.INVISIBLE
                }

                else if (mediaMessage.type == "video") {
                    videoInfoLayout.visibility = View.VISIBLE

                    val interval: Long = 1* 1000
                    val options: RequestOptions = RequestOptions().frame(interval)

                    Glide.with(this@GroupInfoActivity)
                        .asBitmap().load(mediaMessage.message).apply(options).into(mediaImageView)
                }
            }

            override fun onClick(item: View?) {
                val currentMedia = list[adapterPosition]
                val messageType = list[adapterPosition].type

                if (messageType== "image" ||messageType == "captured image"){
                    showSentImage(currentMedia.message)
                }

                else if (messageType== "video") {
                    val videoIntent = Intent(
                        this@GroupInfoActivity,
                        VideoPlayerActivity::class.java
                    )
                    videoIntent.putExtra(VIDEO_URL, currentMedia.message)
                    startActivity(videoIntent)
                }
            }

            override fun onLongClick(item: View?): Boolean {
                return true
            }

        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.media_item_layout,parent,false )

            return MediaHolder(view)
        }

        override fun getItemCount(): Int {
            return list.size
        }

        override fun onBindViewHolder(holder: MediaHolder, position: Int) {
            val media = list[holder.adapterPosition]
            holder.bind(media)
        }
    }

    inner class ParticipantsAdapter (private var list:List<ContactsModel>) : RecyclerView.Adapter<ParticipantsAdapter.ParticipantHolder>() {

        inner class ParticipantHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener, View.OnLongClickListener {
            private val participantNameTextView:TextView = itemView.findViewById(R.id.participant_name_text_view)
            private val participantStatusTextView:TextView = itemView.findViewById(R.id.participant_status_text_view)
           private val participantImageView:ImageView = itemView.findViewById(R.id.participant_image_view)

            init {
                itemView.setOnClickListener(this)
                itemView.setOnLongClickListener(this)
            }


            fun bind (participant : ContactsModel) {
                participantStatusTextView.text = participant.status

                Picasso.get().load(participant.image).into(participantImageView)
                if (currentUserId == participant.uid) {
                    participantNameTextView.text = "You"
                }
                else{
                    participantNameTextView.text = participant.name

                }

            }

            override fun onClick(item: View?) {
                val clickedParticipantId = list[adapterPosition].uid
                showClickedParticipantDialog(clickedParticipantId)
            }

            override fun onLongClick(item: View?): Boolean {
                return true
            }

        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParticipantHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.participant_item_layout,parent,false )

            return ParticipantHolder(view)
        }

        override fun getItemCount(): Int {
            return list.size
        }

        override fun onBindViewHolder(holder: ParticipantHolder, position: Int) {
            val call = list[holder.adapterPosition]
            holder.bind(call)
        }
    }

    private fun showSentImage(imageUrl: String) {
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

    private fun retrieveMyInfo() {
        rootReference.child(USERS_CHILD).child(currentUserId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    //participantsList.clear()

                    val name = snapshot.child("name").value.toString()
                    val imageUrl = snapshot.child("image").value.toString()
                    val status = snapshot.child("status").value.toString()
                    val uid = snapshot.child("uid").value.toString()

                    myNameTextView = findViewById(R.id.my_name_text_view)
                    myImage = findViewById(R.id.my_image_view)
                    myStatusTextView = findViewById(R.id.my_status_text_view)
                    groupBinding.adminBadge.text = "Admin"

                    Picasso.get().load(imageUrl).into(myImage)
                    myNameTextView.text = "You"
                    myStatusTextView.text = status

                }


                override fun onCancelled(error: DatabaseError) {
                }
            })
    }

    private fun showClickedParticipantDialog(clickedParticipantId:String){
        alertBuilder = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.clicked_participant_dialog,null)
        alertBuilder.setView(dialogView)


        val groupDialog =  alertBuilder.create()
        groupDialog.show()

        dialogView.message_participant_text_view.setOnClickListener {
            sendUserToPrivateChat(clickedParticipantId)
            groupDialog.dismiss()
        }

        dialogView.view_participant_text_view.setOnClickListener {
            sendUserToProfileActivity(clickedParticipantId)
            groupDialog.dismiss()
        }

        dialogView.make_participant_admin_text_view.setOnClickListener {
            makeParticipantAdmin(clickedParticipantId)
            groupDialog.dismiss()
        }

        dialogView.remove_participant_text_view.setOnClickListener {
            removeParticipantFromGroup(clickedParticipantId)
            groupDialog.dismiss()
        }


    }

    private fun sendUserToPrivateChat(clickedParticipantId:String) {
       val privateChatIntent = Intent(this, PrivateChatActivity::class.java)
        privateChatIntent.putExtra(USER_ID,clickedParticipantId)
        startActivity(privateChatIntent)
    }

    private fun sendUserToProfileActivity(clickedParticipantId:String) {
//        val privateChatIntent = Intent(this, ProfileActivity::class.java)
//        privateChatIntent.putExtra(USER_ID,clickedParticipantId)
//        startActivity(privateChatIntent)
        Toast.makeText(this, "To profile activity", Toast.LENGTH_SHORT).show()
    }

    private fun makeParticipantAdmin(clickedParticipantId:String){
        Toast.makeText(this, "Became admin", Toast.LENGTH_SHORT).show()
    }

    private fun removeParticipantFromGroup(clickedParticipantId:String){
        rootReference.child("Groups")
            .child(groupId).child("participants").child(clickedParticipantId).removeValue().addOnCompleteListener {
                rootReference.child(USERS_CHILD).child(clickedParticipantId).child("Groups").child(groupId).removeValue()
                }
        }


    //Will be updated
    private fun addParticipants(idsToBeAdded:List<String>) {

        for (id in idsToBeAdded){
            rootReference.child("Groups").child(groupId).
            child("participants").child(id).setValue("").addOnCompleteListener {
                if (it.isComplete){
                    participantsIds.add(id)
                    rootReference.child(USERS_CHILD).child(id).child("Groups").child(groupId).setValue("")
                }

            }
        }
        onStart()


    }

    private fun showPickDescriptionDialog () {

         groupDialog = Dialog(this, android.R.style.Theme_Light_NoTitleBar_Fullscreen)
        groupDialog?.setContentView(R.layout.description_group_add_layout)

        retrieveGroupDescriptionValue()

        groupDialog?.show()

        groupDialog?.submit_adding_description?.setOnClickListener {
            val description = groupDialog?.description_edit_text?.editableText.toString()

            if (description.isEmpty()) {
                removeGroupDescription()
                groupDialog?.dismiss()
                groupDialog = null
            }
            else {
                addGroupDescription(description)
                groupDialog?.dismiss()
                groupDialog = null
            }
        }

        groupDialog?.cancel_adding_description?.setOnClickListener {
            groupDialog?.dismiss()
            groupDialog = null
        }
    }

    private fun addGroupDescription(description: String) {
        rootReference.child("Groups").child(groupId).child("status").setValue(description)
    }

    private fun removeGroupDescription() {
        rootReference.child("Groups").child(groupId).child("status").setValue("")
    }

    private fun retrieveGroupDescriptionValue(){
        rootReference.child("Groups").child(groupId).
        addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.hasChild("status")){
                    rootReference.child("Groups").child(groupId).child("status").
                    addValueEventListener(object : ValueEventListener{
                        override fun onDataChange(snapshot: DataSnapshot) {
                            //Dialog is not shown
                            if (groupDialog == null) {
                                if (snapshot.value.toString() == "") {
                                    groupBinding.descriptionBadgeTextView.visibility = View.GONE
                                    groupBinding.addGroupDescription.text = "Add group description"

                                } else {
                                    groupBinding.descriptionBadgeTextView.visibility = View.VISIBLE
                                    groupBinding.addGroupDescription.text =
                                        snapshot.value.toString()
                                }
                            }

                            else{
                                if (snapshot.value.toString() == "") {
                                    groupDialog!!.description_edit_text.hint = "Add group description"
                                } else {
                                    groupDialog!!.description_edit_text.setText(snapshot.value.toString())
                                }
                            }
                    }
                        override fun onCancelled(error: DatabaseError) {
                        }
                    })
                }


            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    private fun showExitGroupDialog() {


        alertBuilder = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.clear_chat_dialog,null)
        alertBuilder.setView(dialogView)

        dialogView.info_text_view.text = "Exit $groupName?"
        dialogView.clear.text = "Exit"

        val groupDialog =  alertBuilder.create()
        groupDialog.show()


        dialogView.clear.setOnClickListener {
            removeParticipantFromGroup(currentUserId)
            groupDialog.dismiss()
        }

        dialogView.cancel.setOnClickListener {
            groupDialog.dismiss()
        }
    }

    private fun showAddNewSubjectDialog () {

        subjectDialog = Dialog(this, android.R.style.Theme_Light_NoTitleBar_Fullscreen)
        subjectDialog?.setContentView(R.layout.new_subject_layout)


        subjectDialog?.show()

        subjectDialog?.subject_edit_text?.hint = groupName

        subjectDialog?.submit_adding_name?.setOnClickListener {
            val name = subjectDialog?.subject_edit_text?.editableText.toString()

            if (name.isEmpty()) {
                Toast.makeText(this, "Subject can't be empty", Toast.LENGTH_SHORT).show()
                subjectDialog?.dismiss()

            }
            else {
                changeGroupName(name)
                subjectDialog?.dismiss()
            }
        }

        subjectDialog?.cancel_adding_name?.setOnClickListener {
            subjectDialog?.dismiss()
        }
    }

    private fun changeGroupName(name: String) {

        rootReference.child("Groups").child(groupId).child("name").setValue(name)
    }

    private fun showChangeGroupIconDialog () {

        iconDialog = Dialog(this, android.R.style.Theme_Light_NoTitleBar_Fullscreen)
        iconDialog?.setContentView(R.layout.change_group_picture_layout)


        iconDialog?.show()

        if (groupImageUrl.isNotEmpty()){
            Picasso.get().load(groupImageUrl).into(iconDialog?.icon_image_view)
        }

        iconDialog?.dismiss_group_icon_dialog?.setOnClickListener {
            iconDialog?.dismiss()
        }

        iconDialog?.edit_group_icon?.setOnClickListener {
            bottomSheet = ChangeIconBottomSheet()
            bottomSheet.show(supportFragmentManager, "exampleBottomSheet")
        }
    }

    override fun onFabClicked(textUnderFab: String?) {
        when(textUnderFab){
            "Remove icon" -> removeGroupIcon()

            "Gallery" -> sendMeToImagesStorage()

            "Camera" -> takePhoto()
        }
    }

    private fun removeGroupIcon() {
        rootReference.child("Groups").child(groupId).child("image").setValue("")
    }

    private fun takePhoto () {
        checker = "captured image"
        val captureImage = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(captureImage, REQUEST_NUM)
    }

    private fun sendMeToImagesStorage() {
        checker = "image"
        val imagesIntent = Intent(Intent.ACTION_GET_CONTENT)
        imagesIntent.type = "image/*"
        startActivityForResult(Intent.createChooser(imagesIntent, "Choose an image"), REQUEST_NUM)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_NUM && resultCode == RESULT_OK
            && checker!="captured image" && data != null && data.data != null) {

           // getLoadingDialog()

            if (checker =="image") {
                fileUri = data.data!!
                val storageRef = FirebaseStorage.getInstance().reference.child("Image files")

                val userMessageKeyRef = rootReference.child("Groups").child(groupId).child("image").push()

                val messagePushId = userMessageKeyRef.key.toString()

                val filePath = storageRef.child("$messagePushId.jpg")
                val uploadTask = filePath.putFile(fileUri).addOnFailureListener{
                    //progressDialog.dismiss()
                    Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
                }.addOnProgressListener {
                  //  val progress = ((100.0 * it.bytesTransferred) / it.totalByteCount).toInt()
                   // progressDialog.show()
                   // progressDialog.setMessage("$progress % Uploading...")
                }

                uploadTask.continueWithTask { task ->
                    if (!task.isSuccessful) {
                        throw task.exception!!
                    }
                    filePath.downloadUrl
                }.addOnCompleteListener {
                    if (it.isSuccessful) {
                        url = it.result.toString()
                        rootReference.child("Groups").child(groupId).
                        child("image").setValue(url)
                    }
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

            val userMessageKeyRef = rootReference.child("Groups").child(groupId).child("image").push()

            val messagePushId = userMessageKeyRef.key.toString()

            val filePath = storageRef.child("$messagePushId.jpg")
            val uploadTask = filePath.putBytes(b).addOnFailureListener{
              //  progressDialog.dismiss()
                Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            }.addOnProgressListener {
//                val progress = ((100.0 * it.bytesTransferred) / it.totalByteCount).toInt()
//                progressDialog.show()
//                progressDialog.setMessage("$progress % Uploading...")
            }

            uploadTask.continueWithTask { task ->
                if (!task.isSuccessful) {
                    throw task.exception!!
                }
                filePath.downloadUrl
            }.addOnCompleteListener {
                if (it.isSuccessful) {
                    url = it.result.toString()
                    rootReference.child("Groups").child(groupId).
                    child("image").setValue(url)
                }
            }

        }

        else{
            Toast.makeText(this, "Choose a valid attachment", Toast.LENGTH_SHORT).show()
        }
//        progressDialog.dismiss()
    }

   // private fun getLoadingDialog() {
//        progressDialog = ProgressDialog(this)
//            .also {
//                title = "Attachment is uploading"
//                it.setCanceledOnTouchOutside(false)
//                it.show()
//            }
//    }

}