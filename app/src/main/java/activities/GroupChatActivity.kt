package activities

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import models.MessageModel
import com.example.whatsapp.R
import com.example.whatsapp.databinding.ActivityGroupChatBinding
import com.example.whatsapp.databinding.SelfMessageItemBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

private const val GROUP_NAME = "group name"
private const val TAG = "GroupChatActivity"
class GroupChatActivity : AppCompatActivity() {
    private lateinit var activityGroupChatBinding: ActivityGroupChatBinding

    private lateinit var selfMessageItemBinding: SelfMessageItemBinding

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var usersReference: DatabaseReference

    private lateinit var currentGroupName : String
    private lateinit var currentUserName : String
    private lateinit var currentUserId : String
    private lateinit var currentDate : String
    private lateinit var currentTime : String
    private lateinit var groupMessageKeyReference: DatabaseReference
    private lateinit var groupNameReference: DatabaseReference

    private lateinit var messagesAdapter:MessagesAdapter

    private var retrievedMessagesModel = mutableListOf<MessageModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activityGroupChatBinding = DataBindingUtil.setContentView(this, R.layout.activity_group_chat)


        currentGroupName = intent.getStringExtra(GROUP_NAME).toString()
        setUpToolbar(currentGroupName)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        //current users node
        usersReference = database.reference.child("Users")
        currentUserId = auth.currentUser?.uid.toString()

        //current group node
        groupNameReference = database.reference.child("Groups").child(currentGroupName)

        //creating recycler view instance
        activityGroupChatBinding.groupChatRecyclerView.layoutManager = LinearLayoutManager(this)

        getUserInfoFromDb()

        activityGroupChatBinding.sendMessageButton.setOnClickListener {
            setMessageInfoToDb()
            activityGroupChatBinding.sendMessageEditText.text.clear()

        }
    }

    override fun onStart() {
        super.onStart()

        //replace it with add child even listener

//        database.reference.child("Groups").child(currentGroupName).addValueEventListener(object :
//            ValueEventListener {
//            override fun onDataChange(snapshot: DataSnapshot) {
//                displayMessages(snapshot)
//            }
//
//            override fun onCancelled(error: DatabaseError) {
//                Toast.makeText(this@GroupChatActivity, error.message, Toast.LENGTH_SHORT).show()
//            }
//        })
    }

    private fun setUpToolbar(groupName:String) {
        setSupportActionBar(activityGroupChatBinding.mainToolbar)
        supportActionBar?.title = groupName
        activityGroupChatBinding.mainToolbar.setTitleTextColor(Color.WHITE)
        activityGroupChatBinding.mainToolbar.overflowIcon?.setColorFilter(resources.getColor(R.color.white), PorterDuff.Mode.SRC_IN)
    }

    private fun getUserInfoFromDb () {
        usersReference.child(currentUserId).addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    currentUserName = snapshot.child("name").value.toString()
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    private fun setMessageInfoToDb () {
        val currentMessage = activityGroupChatBinding.sendMessageEditText.editableText.toString()
        //to get the message key
        val messageKey = groupNameReference.push().key.toString()
        if (currentMessage.isNotEmpty()) {

            val calender = Calendar.getInstance()
            //get date and time
            val dateFormat = SimpleDateFormat("MMM dd, yyyy")
            val timeFormat = SimpleDateFormat("hh:mm a")

            currentDate = dateFormat.format(calender.time)
            currentTime = timeFormat.format(calender.time)

//            val groupMessageKey = HashMap<String,Any>()
//            groupNameReference.updateChildren(groupMessageKey)

            //MFWUeUDlFP_-8pMvRbx like this --> it refers to the message but with its key
            groupMessageKeyReference = groupNameReference.child(messageKey)

            val messageInfoMap = HashMap<String,Any>()
            messageInfoMap.put("name",currentUserName)
            messageInfoMap.put("uid",currentUserId)
            messageInfoMap.put("message",currentMessage)
            messageInfoMap.put("date",currentDate)
            messageInfoMap.put("time",currentTime)

            groupMessageKeyReference.updateChildren(messageInfoMap)
        }
        else{
            Toast.makeText(this, "Enter valid message", Toast.LENGTH_SHORT).show()
        }
    }

    private fun displayMessages (snapshot: DataSnapshot) {
        retrievedMessagesModel.clear()
        messagesAdapter =  MessagesAdapter(retrievedMessagesModel)
        for (note in snapshot.children) {
            val currentMessageModel = note.getValue(MessageModel::class.java)
            retrievedMessagesModel.add(currentMessageModel!!)
            messagesAdapter.notifyDataSetChanged()
        }

        activityGroupChatBinding.groupChatRecyclerView.adapter = messagesAdapter
        //to scroll to the bottom of recycler view
        if (retrievedMessagesModel.isNotEmpty()){
            activityGroupChatBinding.groupChatRecyclerView.smoothScrollToPosition(retrievedMessagesModel.size-1)
        }
    }


    inner class MessagesAdapter (private var list:List<MessageModel>) : RecyclerView.Adapter<MessagesAdapter.MessagesHolder>() {

        inner class MessagesHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener, View.OnLongClickListener {
            private val messageBodyTextView : TextView = itemView.findViewById(R.id.message_body_text_view)
            private val messageTimeTextView : TextView = itemView.findViewById(R.id.message_time_text_view)
            private val messageSenderTextView : TextView = itemView.findViewById(R.id.message_text_view)


            init {
                itemView.setOnClickListener(this)
                itemView.setOnLongClickListener(this)
            }

            fun bind (messageModel : MessageModel) {

                messageBodyTextView.text = messageModel.message
                messageTimeTextView.text = messageModel.time
                messageSenderTextView.text = messageModel.name
            }



            override fun onClick(item: View?) {

            }

            override fun onLongClick(item: View?): Boolean {
                return true
            }

        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessagesHolder {


            if (getItemViewType(parent.childCount) ==1) {
               val cardView =
                    LayoutInflater.from(parent.context).inflate(
                        R.layout.self_message_item
                        ,parent,false )
                return MessagesHolder(cardView)
            }

            else {
                val cardView =
                    LayoutInflater.from(parent.context).inflate(
                        R.layout.others_message_item
                        ,parent,false )
                return MessagesHolder(cardView)
            }


        }

        override fun getItemCount(): Int {
            return list.size
        }

        override fun onBindViewHolder(holder: MessagesHolder, position: Int) {
            val messageModel = list[holder.adapterPosition]

                holder.bind(messageModel)
            if (list[position].name == currentUserName) {

            }

        }

        override fun getItemViewType(position: Int): Int {
            if (list[position].name == currentUserName) {
                //user message item
                return 1
            }
            else{
                //user message item
                return 2
            }

        }
    }
}