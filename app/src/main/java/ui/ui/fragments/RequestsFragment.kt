package ui.ui.fragments

import ui.ui.activities.ProfileActivity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.whatsapp.BaseApplication
import models.ContactsModel
import com.example.whatsapp.R
import com.example.whatsapp.Utils
import com.example.whatsapp.databinding.FragmentRequestsBinding
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.squareup.picasso.Picasso

private const val TAG = "RequestsFragment"
private const val USER_ID = "user id"

class RequestsFragment : Fragment() {
    private lateinit var fragmentRequestsBinding: FragmentRequestsBinding

    private lateinit var contactsReference: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var rootReference: DatabaseReference
    private lateinit var currentUser: FirebaseUser
    private lateinit var usersReference: DatabaseReference
    private lateinit var contactsAdapter: FirebaseRecyclerAdapter<ContactsModel, ContactsViewHolder>

    private  var usersIdsList =  mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = (activity?.application as BaseApplication).getFirebaseAuthenticationReference()
        currentUser = auth.currentUser!!
        rootReference = (activity?.application as BaseApplication).getDatabaseRootReference()
        usersReference = rootReference.child(Utils.USERS_CHILD)
        contactsReference = rootReference.child(Utils.CHAT_REQUESTS_CHILD).child(currentUser.uid)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        fragmentRequestsBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_requests,container,false)
        return fragmentRequestsBinding.root
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
                usersReference.child(userIds).addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        holder.bind(snapshot)
                        usersIdsList.add(snapshot.child(Utils.USER_ID_CHILD).value.toString())
                    }

                    override fun onCancelled(error: DatabaseError) {

                    }
                })
            }
        }

        //attaching recyclerView to the adapter
        fragmentRequestsBinding.requestsRecyclerView.apply {
            adapter = contactsAdapter
            layoutManager = LinearLayoutManager(context)
            addItemDecoration( DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }

        contactsAdapter.startListening()

        Log.i(TAG, "onStart: ${contactsAdapter.snapshots.size}")
    }


    inner class ContactsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        private val userNameTextView : TextView = itemView.findViewById(R.id.user_name_text_view)
      //  private val userStatusTextView : TextView = itemView.findViewById(R.id.user_status_text_view)
        private val userImageView : ImageView = itemView.findViewById(R.id.user_image_view)

        fun bind(snapshot: DataSnapshot) {

            userNameTextView.text = snapshot.child(Utils.NAME_CHILD).value.toString()
          //  userStatusTextView.text =  snapshot.child(Utils.STATUS_CHILD).value.toString()

            val imageUrl = snapshot.child(Utils.IMAGE_CHILD).value.toString()
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
            val profileIntent = Intent(context, ProfileActivity::class.java)
            profileIntent.putExtra(USER_ID,userId)
            startActivity(profileIntent)
        }
    }

    override fun onStop() {
        super.onStop()
        contactsAdapter.stopListening()
    }
}