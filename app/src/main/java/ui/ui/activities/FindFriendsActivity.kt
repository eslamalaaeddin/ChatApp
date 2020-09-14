package ui.ui.activities

import android.Manifest
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.whatsapp.R
import com.example.whatsapp.Utils
import com.example.whatsapp.Utils.USERS_CHILD
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

    private  var phoneContactsAdapter =  ContactAdapterFromFirebase(mutableListOf())

    private val contactList : MutableList<PhoneContactModel> = mutableListOf()
    private var distinctContactList : List<PhoneContactModel> = listOf()

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
        usersReference.addValueEventListener(object : ValueEventListener {

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

            }

            override fun onCancelled(error: DatabaseError) {

            }

        })

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.find_friends_activity_menu,menu)
        val searchItem = menu?.findItem(R.id.action_search)
        val searchView = searchItem?.actionView as SearchView

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                phoneContactsAdapter.filter.filter(newText)
                return true
            }
        })


        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.open_contacts -> openContacts()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun openContacts() {
        val contactIntent =
            Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
            startActivity(contactIntent)
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


    // to return contacts from firebase db
    inner class ContactAdapterFromFirebase(items: MutableList<ContactsModel>)
        : RecyclerView.Adapter<ContactAdapterFromFirebase.MyViewHolder>() , Filterable{

        private var list = items


        val contactsListFull = mutableListOf<ContactsModel>()

        init {
            contactsListFull.addAll(list)
        }

        override fun getItemCount(): Int {
            return list.size
        }

        override fun onBindViewHolder(
            holder: ContactAdapterFromFirebase.MyViewHolder,
            position: Int
        ) {
            if (position>0 && list[position].phoneNumber == list[position-1].phoneNumber) {
                holder.itemView.visibility = View.GONE
            }
            else{
                holder.bind()
            }



                usersReference.child(currentUser.uid)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val currentPhoneNumber = snapshot.child("phoneNumber").value.toString()
                        Log.i(TAG, "HHHHHHHHHHHHHHHHHHHHHHHHHHHH: $currentPhoneNumber")
                        if (list[position].phoneNumber == currentPhoneNumber) {
                            holder.itemView.visibility = View.GONE
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                    }
                })


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
            private val nameTextView : TextView = itemView.findViewById(R.id.user_name_text_view)
            private val numberTextView :TextView = itemView.findViewById(R.id.user_status_text_view)
            private val userImageView : ImageView = itemView.findViewById(R.id.user_image_view)
            private val messagesCountTextView : TextView = itemView.findViewById(R.id.messages_count_text_view)
            private val messageCheckedImageView : ImageView = itemView.findViewById(R.id.message_checked_image_view)



            init {
                itemView.setOnClickListener(this)
            }

            fun bind() {

                messageCheckedImageView.visibility = View.GONE
                messagesCountTextView.visibility = View.GONE

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

        override fun getFilter(): Filter {
            return object : Filter() {
                //Background thread
                override fun performFiltering(constraint: CharSequence?): FilterResults {
                    val filteredListOfContactsModel = mutableListOf<ContactsModel>()
                    if (constraint == null || constraint.isEmpty()) {
                        filteredListOfContactsModel.addAll(contactsListFull)
                    } else {
                        val filterPattern = constraint.toString().toLowerCase().trim()
                        for (item in contactsListFull) {
                            if (item.name.toLowerCase().contains(filterPattern)) {
                                filteredListOfContactsModel.add(item)
                            }
                        }
                    }
                    val results = FilterResults()
                    results.values = filteredListOfContactsModel
                    return results
                }

                //Main thread
                override fun publishResults(p0: CharSequence?, filterResults: FilterResults?) {
                    list.clear()
                    list.addAll(filterResults?.values as List<ContactsModel>)
                    notifyDataSetChanged()
                }
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