package activities

import android.annotation.SuppressLint
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
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.whatsapp.PrivateMessageModel
import com.example.whatsapp.R
import com.example.whatsapp.Utils
import com.example.whatsapp.Utils.DEVICE_TOKEN_CHILD
import com.example.whatsapp.Utils.USERS_CHILD
import com.example.whatsapp.databinding.ActivityPrivateChatBinding
import com.example.whatsapp.databinding.PrivateMessageLayoutBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.squareup.picasso.Picasso

private const val USER_ID = "user id"
private const val USER_NAME = "user name"
private const val USER_IMAGE = "user image"
private const val TAG = "PrivateChatActivity"
class PrivateChatActivity : AppCompatActivity() {

    private lateinit var activityPrivateChatBinding: ActivityPrivateChatBinding

    private lateinit var privateMessageLayoutBinding: PrivateMessageLayoutBinding

    private lateinit var auth: FirebaseAuth
    private lateinit var senderId:String
    private lateinit var receiverId:String

    private lateinit var senderToken:String
    private lateinit var receiverToken:String

    private lateinit var rootRef: DatabaseReference

    private lateinit var usersRef: DatabaseReference

    private lateinit var messageSenderId : String
    private lateinit var messageReceiverId : String

    private lateinit var userImageView: ImageView
    private lateinit var userNameTextView: TextView
    private lateinit var lastSeenTextView: TextView

    private  var messagesAdapter = PrivateMessagesAdapter(emptyList())

    private var messagesList = mutableListOf<PrivateMessageModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityPrivateChatBinding =
            DataBindingUtil.setContentView(this,R.layout.activity_private_chat)
        setUpToolbar()

        auth = FirebaseAuth.getInstance()

        senderId = auth.currentUser?.uid.toString()
        receiverId = intent.getStringExtra(USER_ID).toString()

        rootRef = FirebaseDatabase.getInstance().reference

        usersRef = rootRef.child(USERS_CHILD)

        getTokens()

        activityPrivateChatBinding.sendMessageButton.setOnClickListener { sendMessage() }


        messagesAdapter = PrivateMessagesAdapter(messagesList)
        activityPrivateChatBinding.privateChatRecyclerView.adapter = messagesAdapter
        activityPrivateChatBinding.privateChatRecyclerView.layoutManager = LinearLayoutManager(this)


    }


    override fun onStart() {
        super.onStart()
        rootRef.child(Utils.MESSAGES_CHILD).child(senderId).child(receiverId).addChildEventListener(object : ChildEventListener{
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val ourMessages = snapshot.getValue(PrivateMessageModel::class.java)

                if (ourMessages != null) {
                    messagesList.add(ourMessages)
                    messagesAdapter.notifyDataSetChanged()

                    //to scroll to the bottom of recycler view
                    if (messagesList.isNotEmpty()){
                        activityPrivateChatBinding.privateChatRecyclerView.smoothScrollToPosition(messagesList.size-1)
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
    }

    private fun setUpToolbar() {

        val toolbarView = LayoutInflater.from(this).inflate(R.layout.custom_toolbar,null)

        setSupportActionBar(activityPrivateChatBinding.mainToolbar)
       // supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowCustomEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        supportActionBar?.customView = toolbarView


        userImageView = findViewById(R.id.user_image_view)
        userNameTextView = findViewById(R.id.user_name_text_view)
        lastSeenTextView = findViewById(R.id.user_last_seen)

        userNameTextView.text = intent.getStringExtra(USER_NAME)
        val imageUrl = intent.getStringExtra(USER_IMAGE).toString()

        if (imageUrl.isNotEmpty()) {
            Picasso.get()
                .load(imageUrl)
                .placeholder(R.drawable.dummy_avatar)
                .into(userImageView)
        }

//        activityPrivateChatBinding.mainToolbar.setTitleTextColor(Color.WHITE)
//        activityPrivateChatBinding.mainToolbar.overflowIcon?.setColorFilter(resources.getColor(R.color.white), PorterDuff.Mode.SRC_IN)
      //  activityPrivateChatBinding.mainToolbar.navigationIcon?.setColorFilter(resources.getColor(R.color.white), PorterDuff.Mode.SRC_IN)
    }

    private fun sendMessage() {
        val message = activityPrivateChatBinding.sendMessageEditText.editableText.toString()

        if(message.isNotEmpty()) {
            val messageSenderRef = "${Utils.MESSAGES_CHILD}/$senderId/$receiverId"
            val messageReceiverRef = "${Utils.MESSAGES_CHILD}/$receiverId/$senderId"

            val userMessageKeyRef = rootRef.child(Utils.MESSAGES_CHILD).
            child(senderId).child(receiverId).push()

            val messagePushId = userMessageKeyRef.key

            val messageTextBody = HashMap<String,Any>()
            messageTextBody.put("message",message)
            messageTextBody.put("type","text")
            messageTextBody.put("from",senderId)

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
                Toast.makeText(this@PrivateChatActivity, senderToken, Toast.LENGTH_SHORT).show()
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })
        usersRef.child(receiverId).child(DEVICE_TOKEN_CHILD).addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    receiverToken = snapshot.value.toString()
                    Toast.makeText(this@PrivateChatActivity, receiverToken, Toast.LENGTH_SHORT).show()
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }
            })
    }


   inner class PrivateMessagesAdapter (private val messages:List<PrivateMessageModel>) :
        RecyclerView.Adapter<PrivateMessagesAdapter.PopularMoviesViewHolder>() {

        inner class PopularMoviesViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
             val senderMessageTextView : TextView = itemView.findViewById(R.id.sender_message_text)
             val receiverMessageTextView : TextView = itemView.findViewById(R.id.receiver_message_text)

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
                holder.receiverMessageTextView.visibility = View.INVISIBLE

                holder.receiverMessageLayout.visibility = View.INVISIBLE

                if (fromUserId == messageSenderId) {
                    holder.senderMessageTextView.setBackgroundResource(R.drawable.sender_messages_background)
                    holder.senderMessageTextView.text = myMessages.message
                }

                else{
                    holder.senderMessageTextView.visibility = View.INVISIBLE
                    holder.receiverMessageTextView.visibility = View.VISIBLE

                    holder.senderMessageLayout.visibility = View.INVISIBLE
                    holder.receiverMessageLayout.visibility = View.VISIBLE


                    holder.receiverMessageTextView.setBackgroundResource(R.drawable.receiver_messages_background)
                    holder.receiverMessageTextView.text = myMessages.message
                }
            }
        }
    }


}