package fragments

import activities.ProfileActivity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.whatsapp.ContactsModel

import com.example.whatsapp.R
import com.example.whatsapp.databinding.FragmentChatsBinding
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.squareup.picasso.Picasso

private const val TAG = "ChatsFragment"
private const val USER_ID = "user id"
class ChatsFragment : Fragment() {
    private lateinit var fragmentChatsBinding :FragmentChatsBinding
    private lateinit var contactsReference: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var currentUser: FirebaseUser
    private lateinit var usersReference: DatabaseReference
    private lateinit var contactsAdapter: FirebaseRecyclerAdapter<ContactsModel, ContactsViewHolder>

    private  var usersIdsList =  mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        currentUser = auth.currentUser!!
        usersReference = FirebaseDatabase.getInstance().reference.child("Users")
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

        fragmentChatsBinding.chatsRecyclerView.layoutManager = LinearLayoutManager(context)
    }

    override fun onStart() {
        super.onStart()
        
        val options  =
            FirebaseRecyclerOptions.Builder<ContactsModel>().setQuery(contactsReference,
                ContactsModel::class.java).build()

        contactsAdapter = object :
            FirebaseRecyclerAdapter<ContactsModel, ContactsViewHolder>(options) {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactsViewHolder {
                val cardView =
                    LayoutInflater.from(parent.context).inflate(R.layout.user_item_layout
                        , parent
                        , false) as CardView

                return ContactsViewHolder(cardView)
            }

          

            override fun onBindViewHolder(holder: ContactsViewHolder, position: Int, model: ContactsModel) {
               val userIds = getRef(position).key.toString()
                //to get current user data
               usersReference.child(userIds).addValueEventListener(object :ValueEventListener{
                   override fun onDataChange(snapshot: DataSnapshot) {
                        holder.bind(snapshot)
                        usersIdsList.add(snapshot.child("uid").value.toString())
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

        Log.i(TAG, "onStart: ${contactsAdapter.snapshots.size}")
    }


    inner class ContactsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        private val userNameTextView : TextView = itemView.findViewById(R.id.user_name_text_view)
        private val userStatusTextView : TextView = itemView.findViewById(R.id.user_status_text_view)
        private val userImageView : ImageView = itemView.findViewById(R.id.user_image_view)



        fun bind(snapshot: DataSnapshot) {

            userNameTextView.text = snapshot.child("name").value.toString()
            userStatusTextView.text =  snapshot.child("status").value.toString()

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
//            val userId = usersIdsList[adapterPosition]
//            val profileIntent = Intent(context, ProfileActivity::class.java)
//            profileIntent.putExtra(USER_ID,userId)
//            startActivity(profileIntent)
//

        }
    }

    override fun onStop() {
        super.onStop()
        contactsAdapter.stopListening()
    }
}