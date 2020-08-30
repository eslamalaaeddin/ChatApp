package fragments

import activities.PrivateChatActivity
import activities.ProfileActivity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.whatsapp.Callback
import com.example.whatsapp.ContactsModel

import com.example.whatsapp.R
import com.example.whatsapp.databinding.FragmentChatsBinding
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.squareup.picasso.Picasso
import kotlin.math.log

private const val TAG = "ChatsFragment"

class ChatsFragment : Fragment() {
    private lateinit var fragmentChatsBinding :FragmentChatsBinding
    private lateinit var contactsReference: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var currentUser: FirebaseUser

    private lateinit var currentUserId: String
    private lateinit var usersReference: DatabaseReference
    private lateinit var contactsAdapter: FirebaseRecyclerAdapter<ContactsModel, ContactsViewHolder>

    private lateinit var rootReference: DatabaseReference

    private lateinit var messagesReference:DatabaseReference

    private  var usersIdsList =  mutableListOf<String>()
    private  var usersNamesList =  mutableListOf<String>()
    private  var usersImagesList =  mutableListOf<String>()

    private lateinit var callback: Callback

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
    }

    override fun onStart() {
        super.onStart()
        
        val options  =
            FirebaseRecyclerOptions.Builder<ContactsModel>().setQuery(contactsReference,
                ContactsModel::class.java).build()

        contactsAdapter = object :
            FirebaseRecyclerAdapter<ContactsModel, ContactsViewHolder>(options) {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactsViewHolder {
                val view =
                    LayoutInflater.from(parent.context).inflate(R.layout.user_item_layout
                        , parent
                        , false)

                return ContactsViewHolder(view)
            }

          

            override fun onBindViewHolder(holder: ContactsViewHolder, position: Int, model: ContactsModel) {
               val userIds = getRef(position).key.toString()
                //to get current user data
              rootReference.child("Users").child(userIds).addValueEventListener(object : ValueEventListener {
                   override fun onDataChange(snapshot: DataSnapshot) {
                       //snap shot == { key = BGOgKddJIVWRH60cKLoIlepQVet1, value = {image=....}

                       Log.i(TAG, "OOO onDataChange: $snapshot")
                       Log.i(TAG, "OOO onDataChange: ${snapshot.key}")
                       Log.i(TAG, "OOO onDataChange: ${snapshot.value}")
                       holder.bind(snapshot)
                       usersIdsList.add(snapshot.child("uid").value.toString())
                       usersNamesList.add(snapshot.child("name").value.toString())
                       usersImagesList.add(snapshot.child("image").value.toString())

                   }

                   override fun onCancelled(error: DatabaseError) {

                   }
               })


            }

        }

        //attaching recyclerView to the adapter
        fragmentChatsBinding.chatsRecyclerView.adapter = contactsAdapter
        fragmentChatsBinding.chatsRecyclerView.layoutManager = LinearLayoutManager(context)

        contactsAdapter.startListening()

    }


    inner class ContactsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        private val userNameTextView : TextView = itemView.findViewById(R.id.user_name_text_view)
        private val userLastSeenTextView: TextView =  itemView.findViewById(R.id.user_last_seen_text_view)
       // private val userLastMessageTextView : TextView = itemView.findViewById(R.id.last_message_text_view)
      //  private val userLastChatTimeTextView : TextView = itemView.findViewById(R.id.last_chat_time_text_view)
        private val userImageView : ImageView = itemView.findViewById(R.id.user_image_view)

        fun bind(snapshot: DataSnapshot) {

            val time = snapshot.child("state").child("time").value.toString()
            val date = snapshot.child("state").child("date").value.toString()
            val state = snapshot.child("state").child("state").value.toString()

            if (state == "offline") {

                userLastSeenTextView.text =  "Last seen: $time, $date"
            }
            else if (state == "online") {
                userLastSeenTextView.apply {
                    text = state
                    //setBackgroundResource(R.color.colorPrimary)
                }
            }

            userNameTextView.text = snapshot.child("name").value.toString()


            val imageUrl = snapshot.child("image").value.toString()
            if (imageUrl.isNotEmpty()) {
                Picasso.get()
                    .load(imageUrl)
                    .placeholder(R.drawable.dummy_avatar)
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


            callback.onUserChatClicked(userName,userId,userImage)
        }
    }

    override fun onStop() {
        super.onStop()
        contactsAdapter.stopListening()
    }
}