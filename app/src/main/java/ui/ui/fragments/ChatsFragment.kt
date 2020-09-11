package ui.ui.fragments

import android.annotation.SuppressLint
import ui.ui.activities.CallingActivity
import ui.ui.activities.FindFriendsActivity

import android.content.Context
import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.whatsapp.Callback
import models.ContactsModel

import com.example.whatsapp.R
import com.example.whatsapp.Utils
import com.example.whatsapp.Utils.USERS_CHILD
import models.UserStateModel
import com.example.whatsapp.databinding.FragmentChatsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.squareup.picasso.Picasso
import models.GroupModel

private const val TAG = "ChatsFragment"
private const val CALLER_ID = "receiver id"

class ChatsFragment : Fragment() {
    private lateinit var fragmentChatsBinding :FragmentChatsBinding
    private lateinit var contactsReference: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var currentUser: FirebaseUser

    private lateinit var currentUserId: String
    private lateinit var usersReference: DatabaseReference
//    private lateinit var contactsAdapter: FirebaseRecyclerAdapter<ContactsModel, ContactsViewHolder>

    private lateinit var rootReference: DatabaseReference
    private  var phoneContactsAdapter =  ContactAdapterFromFirebase (emptyList(), emptyList())
    val listFromFirebaseDb = mutableListOf<ContactsModel>()
    private lateinit var messagesReference:DatabaseReference

    private lateinit var callBy:String

    private  var usersIdsList =  mutableListOf<String>()
    private  var usersNamesList =  mutableListOf<String>()
    private  var usersImagesList =  mutableListOf<String>()
    private  var messagesKeysList =  mutableListOf<String>()

    private lateinit var callback: Callback

    private var stateList = mutableListOf<UserStateModel>()

    private lateinit var mySnapshot: DataSnapshot

    private var groupsAdapter = GroupsAdapter(emptyList())
    private var groupsList = mutableListOf<GroupModel>()
    override fun onAttach(context: Context) {
        super.onAttach(context)
        callback = context as Callback
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        currentUser = auth.currentUser!!

        rootReference = FirebaseDatabase.getInstance().reference
        currentUserId = currentUser.uid
        usersReference = FirebaseDatabase.getInstance().reference.child("Users")
        messagesReference = FirebaseDatabase.getInstance().reference.child("Messages")
        contactsReference = FirebaseDatabase.getInstance().reference.child("Contacts").child(currentUser.uid)


        Log.i(TAG, "TTTT onCreate: ")

    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        fragmentChatsBinding =
            DataBindingUtil.inflate(inflater , R.layout.fragment_chats , container,false)

        return fragmentChatsBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fragmentChatsBinding.chatsRecyclerView.apply {
            addItemDecoration( DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            layoutManager = LinearLayoutManager(context)
        }

        fragmentChatsBinding.groupsRecyclerView.apply {
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            layoutManager = LinearLayoutManager(context)
        }


            fragmentChatsBinding.fab.setOnClickListener {
            sendUserToFindFriendsActivity()
        }


    }



    override fun onStart() {
        super.onStart()
        checkForReceivingCalls()
        usersReference.addListenerForSingleValueEvent(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {
                stateList.clear()
                listFromFirebaseDb.clear()
                for (phoneSnapshot in snapshot.children) {
                    val id = phoneSnapshot.child("uid").value.toString()
                    if (id != currentUserId && phoneSnapshot.key != "Groups") {
                        val name = phoneSnapshot.child("name").value.toString()
                        val image = phoneSnapshot.child("image").value.toString()
                        val status = phoneSnapshot.child("status").value.toString()
                        val currentPhoneNumber = phoneSnapshot.child("phoneNumber").value.toString()

                        val date = phoneSnapshot.child("state").child("date").value.toString()
                        val time = phoneSnapshot.child("state").child("time").value.toString()
                        val state = phoneSnapshot.child("state").child("state").value.toString()

                        usersIdsList.add(id)
                        usersNamesList.add(name)
                        usersImagesList.add(image)


                        stateList.add(
                            UserStateModel(
                                date, state, time
                            )
                        )

                        listFromFirebaseDb.add(
                            ContactsModel(
                                name,
                                image,
                                status,
                                id,
                                currentPhoneNumber
                            )
                        )
                        Log.i(TAG, "QQQQ onDataChange: $name   $currentPhoneNumber")
                        Log.i(TAG, "QQQQ onDataChange: $date   $state   $time")
                    }
                }

                phoneContactsAdapter = ContactAdapterFromFirebase(listFromFirebaseDb,stateList)
                fragmentChatsBinding.chatsRecyclerView.adapter = phoneContactsAdapter
            }

            override fun onCancelled(error: DatabaseError) {

            }

        })
        retrieveGroups()
        Log.i(TAG, "TTTT onStart: ")
    }

    // to return contacts from firebase db
    inner class ContactAdapterFromFirebase(private val contactsModel : List<ContactsModel>,
                                           private val userSatesModel:List<UserStateModel>)
        : RecyclerView.Adapter<ContactAdapterFromFirebase.ContactsViewHolder>(){

        override fun getItemCount(): Int {
            return contactsModel.size
        }

        override fun onBindViewHolder(holder: ContactsViewHolder, position: Int) {
            val userStateModel = userSatesModel[holder.adapterPosition]
            val contactModel = contactsModel[holder.adapterPosition]
            holder.bind(userStateModel,contactModel)

        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactsViewHolder {
            return ContactsViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.friend_item_layout,parent,false))
        }

        inner class ContactsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
            private val userNameTextView : TextView = itemView.findViewById(R.id.user_name_text_view)
            private val userLastSeenTextView: TextView =  itemView.findViewById(R.id.user_status_text_view)
             private val lastMessageDateTextView : TextView = itemView.findViewById(R.id.date_text_view)
              private val checkedMessageImageView : ImageView = itemView.findViewById(R.id.message_checked_image_view)
            private val userImageView : ImageView = itemView.findViewById(R.id.user_image_view)
            private val messagesCountTextView : TextView = itemView.findViewById(R.id.messages_count_text_view)

            fun bind(userStateModel: UserStateModel, contactModel: ContactsModel) {


                //to show last message in user's item
                if (currentUserId != contactModel.uid) {
                rootReference.child("Messages").child(currentUserId).child(contactModel.uid)
                    .addValueEventListener(object : ValueEventListener {
                        @SuppressLint("SetTextI18n")
                        override fun onDataChange(snapshot: DataSnapshot) {

                            //there is a messages
                            if (snapshot.value != null && snapshot.hasChildren()) {
                                val fromWhom =
                                    snapshot.children.last().child("from").value.toString()

                                val messageState =
                                    snapshot.children.last().child("seen").value.toString()

                                //sent from me
                                if (fromWhom == currentUserId) {
                                    val currentMessage = snapshot.children.last()
                                    val currentMessageType =
                                        snapshot.children.last().child("type").value.toString()
                                    //message time
                                    lastMessageDateTextView.text =
                                        "${currentMessage.child("date").value.toString()} ${currentMessage.child("time").value.toString()}"

                                    lastMessageDateTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.light_gray))
                                    messagesCountTextView.visibility = View.INVISIBLE
                                    //message is text
                                    if (currentMessageType == "text") {
                                        val lastMessage =
                                            currentMessage.child("message").value.toString()
                                        userLastSeenTextView.text = lastMessage

                                    }
                                    //Documents
                                    else if (currentMessageType == "docx" || currentMessageType == "pdf") {
                                        userLastSeenTextView.text = "Document"
                                    }

                                    //images
                                    else if (currentMessageType == "image" || currentMessageType == "captured image") {
                                        userLastSeenTextView.text = "Photo"

                                    }

                                    //audio
                                    else if (currentMessageType == "audio") {
                                        userLastSeenTextView.text = "Voice"

                                    }

                                    //Videos
                                    else if (currentMessageType == "video") {
                                        userLastSeenTextView.text = "Video"

                                    }

                                    //check to see if it is seen
                                    if (messageState == "yes") {
                                        checkedMessageImageView.visibility = View.VISIBLE
                                        checkedMessageImageView.setImageResource(R.drawable.ic_check)
                                        checkedMessageImageView.setColorFilter(resources.getColor(R.color.blue), PorterDuff.Mode.SRC_IN)
                                    }
                                    else{
                                        checkedMessageImageView.visibility = View.VISIBLE
                                        checkedMessageImageView.setImageResource(R.drawable.ic_check)
                                        checkedMessageImageView.setColorFilter(resources.getColor(R.color.light_gray), PorterDuff.Mode.SRC_IN)
                                    }

                                }
                                //shown to me
                                else {
                                    val currentMessage = snapshot.children.last()
                                    val currentMessageType =
                                        snapshot.children.last().child("type").value.toString()

                                    lastMessageDateTextView.text =
                                        "${currentMessage.child("date").value.toString()} ${currentMessage.child("time").value.toString()}"

                                    lastMessageDateTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.green))

                                    messagesCountTextView.visibility = View.VISIBLE

                                    messagesCountTextView.text = snapshot.childrenCount.toString()

                                    //message is text
                                    if (currentMessageType == "text") {
                                        val lastMessage =
                                            currentMessage.child("message").value.toString()
                                        userLastSeenTextView.text = lastMessage

                                        checkedMessageImageView.visibility = View.GONE
                                    }
                                    //Documents
                                    else if (currentMessageType == "docx" || currentMessageType == "pdf") {
                                        userLastSeenTextView.text = "Document"

                                        checkedMessageImageView.visibility = View.VISIBLE
                                        checkedMessageImageView.setImageResource(R.drawable.ic_file)
                                        checkedMessageImageView.setColorFilter(resources.getColor(R.color.light_gray), PorterDuff.Mode.SRC_IN)
                                    }

                                    //images
                                    else if (currentMessageType == "image" || currentMessageType == "captured image") {
                                        userLastSeenTextView.text = "Photo"
                                        checkedMessageImageView.visibility = View.VISIBLE
                                        checkedMessageImageView.setImageResource(R.drawable.ic_image)
                                        checkedMessageImageView.setColorFilter(resources.getColor(R.color.light_gray), PorterDuff.Mode.SRC_IN)

                                    }

                                    //audio
                                    else if (currentMessageType == "audio") {
                                        userLastSeenTextView.text = "Voice"

                                        checkedMessageImageView.visibility = View.VISIBLE
                                        checkedMessageImageView.setImageResource(R.drawable.ic_audio)
                                        checkedMessageImageView.setColorFilter(resources.getColor(R.color.green), PorterDuff.Mode.SRC_IN)

                                    }

                                    //Videos
                                    else if (currentMessageType == "video") {
                                        userLastSeenTextView.text = "Video"

                                        checkedMessageImageView.visibility = View.VISIBLE
                                        checkedMessageImageView.setImageResource(R.drawable.ic_video)
                                        checkedMessageImageView.setColorFilter(resources.getColor(R.color.light_gray), PorterDuff.Mode.SRC_IN)

                                    }
                                }
                            }

                            else{
                                userLastSeenTextView.text = "No messages yet"
                                checkedMessageImageView.visibility = View.GONE
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                        }
                    })
            }

                userNameTextView.text = contactModel.name


                val imageUrl = contactModel.image
                if (imageUrl.isNotEmpty()) {
                    Picasso.get()
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_person)
                        .into(userImageView)
                }
            }

            init {
                itemView.setOnClickListener(this)
            }

            override fun onClick(itemView: View?) {
                val userId = usersIdsList[adapterPosition]
                val userName = usersNamesList[adapterPosition]
                val userImage = usersImagesList[adapterPosition]

                Log.i(TAG, "CLICKED onClick: ")

//                rootReference.child(Utils.MESSAGES_CHILD).child(currentUserId).child(userId).addListenerForSingleValueEvent(object:ChildEventListener{
//                    override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
//                        //make messages seen
//                        rootReference.child("Messages").child(currentUserId).child(userId)
//                            .child(snapshot.key.toString())
//                            .child("seen").setValue("yes").addOnCompleteListener {
//                                rootReference.child("Messages").child(userId).child(currentUserId)
//                                    .child(snapshot.key.toString())
//                                    .child("seen").setValue("yes")
//                            }
//                    }
//
//                    override fun onChildChanged(
//                        snapshot: DataSnapshot,
//                        previousChildName: String?
//                    ) {
//                    }
//
//                    override fun onChildRemoved(snapshot: DataSnapshot) {
//                    }
//
//                    override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
//                    }
//
//                    override fun onCancelled(error: DatabaseError) {
//                    }
//                })



                callback.onUserChatClicked(userName,userId,userImage)
            }
        }

    }
    private fun checkForReceivingCalls() {
        usersReference.child(currentUserId).child("Ringing").addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.hasChild("ringing")){
                    //current caller Id
                    callBy = snapshot.child("ringing").value.toString()
                    //send user to calling activity
                    val callingIntent = Intent(context, CallingActivity::class.java)
                    callingIntent.putExtra(CALLER_ID,callBy)
                    startActivity(callingIntent)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })
    }

    private fun sendUserToFindFriendsActivity() {
        val findFriendsIntent = Intent(requireContext() , FindFriendsActivity::class.java)
        startActivity(findFriendsIntent)
    }

    //////////////////////////////////////////////////////Groups logic/////////////////////////////////////////////////
    private fun retrieveGroups() {
        rootReference.child(USERS_CHILD).child("Groups").addValueEventListener(object :
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                groupsList.clear()
                for (group in snapshot.children) {

                    val name = group.child("name").value.toString()
                    val image = group.child("image").value.toString()
                    val status = group.child("status").value.toString()
                    val groupId = group.child("gid").value.toString()

                    val currentGroup = GroupModel(name,image,status,groupId)

                    groupsList.add(0, currentGroup)
                }
                groupsAdapter = GroupsAdapter(groupsList)
                fragmentChatsBinding.groupsRecyclerView.adapter = groupsAdapter
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, error.message, Toast.LENGTH_SHORT).show()
            }
        })
    }

    //Adapter

    inner class GroupsAdapter (private var list:List<GroupModel>) : RecyclerView.Adapter<GroupsAdapter.GroupsHolder>() {

        inner class GroupsHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener, View.OnLongClickListener {
            private val groupNameTextView : TextView = itemView.findViewById(R.id.group_name_text_view)
            private val groupLastSeenTextView: TextView =  itemView.findViewById(R.id.group_status_text_view)
            private val groupImageView : ImageView = itemView.findViewById(R.id.group_image_view)

            init {
                itemView.setOnClickListener(this)
                itemView.setOnLongClickListener(this)
            }

            fun bind (group : GroupModel) {
                groupNameTextView.text = group.name
                groupLastSeenTextView.text = group.status

                val imageUrl = group.image
                if (imageUrl.isNotEmpty()) {
                    Picasso.get()
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_group)
                        .into(groupImageView)
                }
            }

            override fun onClick(item: View?) {
                val group = list[adapterPosition]
                callback.onGroupClicked(group)
            }

            override fun onLongClick(item: View?): Boolean {
                return true
            }

        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupsHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.group_item_layout,parent,false )

            return GroupsHolder(view)
        }

        override fun getItemCount(): Int {
            return list.size
        }

        override fun onBindViewHolder(holder: GroupsHolder, position: Int) {
            val group = list[holder.adapterPosition]
            holder.bind(group)
        }
    }

}