package ui.ui.activities

import android.Manifest
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.whatsapp.R
import com.example.whatsapp.databinding.ActivityFindFriendsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.squareup.picasso.Picasso
import models.ContactsModel
import models.PhoneContactModel
import pub.devrel.easypermissions.EasyPermissions

private const val TAG = "FindFriendsActivity"
private const val USER_ID = "user id"
class FindFriendsActivity : AppCompatActivity() {
    private lateinit var activityFindFriendsBinding: ActivityFindFriendsBinding

    private lateinit var usersReference: DatabaseReference

    private var contactsNumbersList = mutableListOf<String>()

    //firebase authentication instance
    private lateinit var auth: FirebaseAuth

    private lateinit var currentUser: FirebaseUser

    private  var phoneContactsAdapter =  ContactAdapterFromFirebase(emptyList())

    private val contactList : MutableList<PhoneContactModel> = mutableListOf()
    private var distinctContactList : List<PhoneContactModel> = listOf()

    private val contactsFromFirebaseDb : MutableList<ContactsModel> = mutableListOf()
    val uniqueIds = mutableListOf<String>()
    val listFromFirebaseDb = mutableListOf<ContactsModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activityFindFriendsBinding = DataBindingUtil.setContentView(
            this,
            R.layout.activity_find_friends
        )
        auth = FirebaseAuth.getInstance()
        currentUser = auth.currentUser!!
        usersReference = FirebaseDatabase.getInstance().reference.child("Users")
        setUpToolbar()

        requestPermissions()



        activityFindFriendsBinding.findFriendsRecyclerView.layoutManager = LinearLayoutManager(this@FindFriendsActivity)
           usersReference.addListenerForSingleValueEvent(object : ValueEventListener {

               override fun onDataChange(snapshot: DataSnapshot) {
                    listFromFirebaseDb.clear()
                   for (phoneSnapshot in snapshot.children) {
                       if (phoneSnapshot.hasChild("phoneNumber")) {
                           val currentPhoneNumber =
                               phoneSnapshot.child("phoneNumber").value.toString()
                           if (phoneSnapshot.child("phoneNumber").value != null) {
                               for ((i, item) in distinctContactList.withIndex()) {
                                   if (item.number == currentPhoneNumber || currentPhoneNumber.contains(
                                           item.number
                                       )
                                   ) {
                                       val name = phoneSnapshot.child("name").value.toString()
                                       val image = phoneSnapshot.child("image").value.toString()
                                       val status = phoneSnapshot.child("status").value.toString()
                                       val id = phoneSnapshot.child("uid").value.toString()

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



                                   }
                               }
                           }

                       }
                   }

                   phoneContactsAdapter = ContactAdapterFromFirebase(listFromFirebaseDb)
                   activityFindFriendsBinding.findFriendsRecyclerView.adapter = phoneContactsAdapter
                   activityFindFriendsBinding.findFriendsRecyclerView.addItemDecoration(
                       DividerItemDecoration(
                           this@FindFriendsActivity,
                           DividerItemDecoration.VERTICAL
                       )
                   )

               }

               override fun onCancelled(error: DatabaseError) {

               }

           })



        }



    private fun setUpToolbar() {
        setSupportActionBar(activityFindFriendsBinding.mainToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Select contact"
        activityFindFriendsBinding.mainToolbar.setTitleTextColor(Color.WHITE)
        activityFindFriendsBinding.mainToolbar.overflowIcon?.setColorFilter(
            resources.getColor(R.color.white),
            PorterDuff.Mode.SRC_IN
        )
        activityFindFriendsBinding.mainToolbar.navigationIcon?.setColorFilter(
            resources.getColor(R.color.white),
            PorterDuff.Mode.SRC_IN
        )
    }

    private fun fetchContactsNumbersFromPhone () {

        val contacts = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null,
            null,
            null,
            null
        )

        if (contacts != null) {
            while (contacts.moveToNext()){
                val name = contacts.getString(contacts.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                val number = contacts.getString(contacts.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                val contact = PhoneContactModel()
                contact.name = name
                contact.number = number

                if (!number.contains(" ")){
                    contactList.add(contact)

                    contactsNumbersList.add(contact.number)
                    Log.i(TAG, "AAAA fetchContactsNumbersFromPhone: $number")
                }

            }
        }
        distinctContactList = contactList.distinct()
        contacts?.close()
    }

    // to return contacts from phone db

    inner class ContactAdapter(items: List<PhoneContactModel>)
        : RecyclerView.Adapter<ContactAdapter.ViewHolder>(){

        private var list = items

        override fun getItemCount(): Int {
            return list.size
        }

        override fun onBindViewHolder(holder: ContactAdapter.ViewHolder, position: Int) {
          holder.bind()

        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactAdapter.ViewHolder {
            return ViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.friend_item_layout,
                    parent,
                    false
                )
            )
        }


        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),View.OnClickListener{
            val nameTextView : TextView = itemView.findViewById(R.id.user_name_text_view)
            val numberTextView :TextView = itemView.findViewById(R.id.user_status_text_view)
            val userImageView : ImageView = itemView.findViewById(R.id.user_image_view)

            init {
                itemView.setOnClickListener(this)
            }

            fun bind() {
                nameTextView.text = list[adapterPosition].name
                numberTextView.text = list[adapterPosition].number
//                if (list[adapterPosition].image.isNotEmpty()) {
//                    Picasso.get()
//                        .load(list[adapterPosition].image)
//                        .placeholder(R.drawable.dummy_avatar)
//                        .into(userImageView)
//                }
            }

            override fun onClick(itemView: View?) {
//                val userId =list[adapterPosition].uid
//                val profileIntent = Intent(this@FindFriendsActivity, PrivateChatActivity::class.java)
//                profileIntent.putExtra(USER_ID,userId)
//                startActivity(profileIntent)
            }
        }
    }

    // to return contacts from firebase db
    inner class ContactAdapterFromFirebase(items: List<ContactsModel>)
        : RecyclerView.Adapter<ContactAdapterFromFirebase.MyViewHolder>(){

        private var list = items

        override fun getItemCount(): Int {
            return list.size
        }

        override fun onBindViewHolder(
            holder: ContactAdapterFromFirebase.MyViewHolder,
            position: Int
        ) {
            holder.bind()

        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            return MyViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.friend_item_layout,
                    parent,
                    false
                )
            )
        }


        inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),View.OnClickListener{
            val nameTextView : TextView = itemView.findViewById(R.id.user_name_text_view)
            val numberTextView :TextView = itemView.findViewById(R.id.user_status_text_view)
            val userImageView : ImageView = itemView.findViewById(R.id.user_image_view)

            init {
                itemView.setOnClickListener(this)
            }

            fun bind() {
                nameTextView.text = list[adapterPosition].name
                numberTextView.text = list[adapterPosition].status
                if (list[adapterPosition].image.isNotEmpty()) {
                    Picasso.get()
                        .load(list[adapterPosition].image)
                        .placeholder(R.drawable.ic_person)
                        .into(userImageView)
                }
            }

            override fun onClick(itemView: View?) {
                val userId =list[adapterPosition].uid
                val profileIntent = Intent(
                    this@FindFriendsActivity,
                    PrivateChatActivity::class.java
                )
                profileIntent.putExtra(USER_ID, userId)
                startActivity(profileIntent)
            }
        }
    }

    private fun requestPermissions() {
        val perms = arrayOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS
        )
        if (EasyPermissions.hasPermissions(this, *perms)) {
            fetchContactsNumbersFromPhone()
        } else {
            EasyPermissions.requestPermissions(
                this, getString(R.string.rationale_video_app), 124, *perms)
        }
    }
  
}