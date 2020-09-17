package ui.ui.activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.whatsapp.R
import com.example.whatsapp.Utils
import com.example.whatsapp.databinding.ActivityAddGroupBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.squareup.picasso.Picasso
import models.ContactsModel
import models.PhoneContactModel
import pub.devrel.easypermissions.EasyPermissions

private const val TAG = "AddGroupActivity"
class AddGroupActivity : AppCompatActivity() {
    private lateinit var addGroupBinding: ActivityAddGroupBinding
    private lateinit var toolbarView: View
    private lateinit var newGroupTextView: TextView
    private lateinit var addParticipantsTextView: TextView

    private lateinit var usersReference: DatabaseReference

    private var contactsNumbersList = mutableListOf<String>()

    //firebase authentication instance
    private lateinit var auth: FirebaseAuth

    private lateinit var currentUser: FirebaseUser

    private  var phoneContactsAdapter =  ContactAdapterFromFirebase(mutableListOf())
    private  var addedContactsAdapter =  AddedContactsAdapter(mutableListOf())
    private val contactList : MutableList<PhoneContactModel> = mutableListOf()
    private var distinctContactList : List<PhoneContactModel> = listOf()

    val listFromFirebaseDb = mutableListOf<ContactsModel>()

    private var contactsClickedMap:HashMap<Int,String> = HashMap()
    private var addedContacts = mutableListOf<ContactsModel>()
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
         addGroupBinding = DataBindingUtil.setContentView(this,R.layout.activity_add_group)

        setUpToolbar()

        auth = FirebaseAuth.getInstance()
        currentUser = auth.currentUser!!
        usersReference = FirebaseDatabase.getInstance().reference.child("Users")
        setUpToolbar()

        requestPermissions()

        addGroupBinding.findFriendsRecyclerView.layoutManager = LinearLayoutManager(this)
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
                addGroupBinding.findFriendsRecyclerView.adapter = phoneContactsAdapter

            }

            override fun onCancelled(error: DatabaseError) {

            }

        })

        //Horizontal recyclerView
        addGroupBinding.addedParticipantsRecyclerView.layoutManager = LinearLayoutManager(this,LinearLayoutManager.HORIZONTAL,false)


            if (addedContacts.isEmpty()){
                addParticipantsTextView.text = "Add participants"
            }
            else{
                addParticipantsTextView.text = "${addedContacts.size} of 256 selected"
            }


        addGroupBinding.fab.setOnClickListener {
            if (contactsClickedMap.isEmpty()){
                Toast.makeText(this, "At least 1 contact must be selected", Toast.LENGTH_SHORT).show()
            }
            else{
               startActivity(Intent(this,GroupCreationActivity::class.java))
                Utils.dummyList = addedContacts
            }
        }

    }

    private fun setUpToolbar() {

        toolbarView = LayoutInflater.from(this).inflate(R.layout.add_group_custom_toolbar, null)

        setSupportActionBar(addGroupBinding.mainToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowCustomEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        supportActionBar?.customView = toolbarView



        newGroupTextView = findViewById(R.id.new_group_text_view_custom)
        addParticipantsTextView = findViewById(R.id.add_participants_text_view)

        newGroupTextView.text = "New group"

        addParticipantsTextView.setOnClickListener {
            if (contactsClickedMap.isEmpty()){
                addParticipantsTextView.text = "Add participants"
            }
            else{
                addParticipantsTextView.text = "${addedContacts.size} of 256 selected"
            }
        }

        addGroupBinding.mainToolbar.setTitleTextColor(Color.WHITE)
        addGroupBinding.mainToolbar.overflowIcon?.setColorFilter(resources.getColor(R.color.white), PorterDuff.Mode.SRC_IN)
        addGroupBinding.mainToolbar.navigationIcon?.setColorFilter(resources.getColor(R.color.white), PorterDuff.Mode.SRC_IN)
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
        : RecyclerView.Adapter<ContactAdapterFromFirebase.MyViewHolder>() , Filterable {

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
//            if (position>0 && list[position].phoneNumber == list[position-1].phoneNumber) {
//                holder.itemView.visibility = View.GONE
//            }
//            else{
                holder.bind()
//            }



//            usersReference.child(currentUser.uid)
//                .addValueEventListener(object : ValueEventListener {
//                    override fun onDataChange(snapshot: DataSnapshot) {
//                        val currentPhoneNumber = snapshot.child("phoneNumber").value.toString()
//                        Log.i(TAG, "HHHHHHHHHHHHHHHHHHHHHHHHHHHH: $currentPhoneNumber")
//                        if (list[position].phoneNumber == currentPhoneNumber) {
                            //holder.itemView.visibility = View.GONE
//                        }
//                    }
//
//                    override fun onCancelled(error: DatabaseError) {
//                    }
//                })


        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            return MyViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.add_participant_item_layout,
                    parent,
                    false
                )
            )
        }


        inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),View.OnClickListener{
            private val nameTextView : TextView = itemView.findViewById(R.id.user_name_text_view)
            private val numberTextView :TextView = itemView.findViewById(R.id.user_status_text_view)
            private val userImageView : ImageView = itemView.findViewById(R.id.user_image_view)
            private val checkedImageView:ImageView = itemView.findViewById(R.id.contact_checked_image_view)



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
                if (!contactsClickedMap.containsKey(adapterPosition)){
                    contactsClickedMap[adapterPosition] = list[adapterPosition].uid

                    val name = list[adapterPosition].name
                    val image = list[adapterPosition].image
                    val uid = list[adapterPosition].uid
                    val status = list[adapterPosition].status

                    addedContacts.add(ContactsModel(name,image,status,uid,""))

                    checkedImageView.visibility = View.VISIBLE
                }
                else{
                    for (item in addedContacts){
                        if (item.uid == contactsClickedMap[adapterPosition]){
                            addedContacts.remove(item)
                        }
                    }

                    contactsClickedMap.remove(adapterPosition)

                    checkedImageView.visibility = View.GONE
                }

                if (addedContacts.isNotEmpty()){
                    addGroupBinding.addedParticipantsRecyclerView.visibility = View.VISIBLE
                    addedContactsAdapter = AddedContactsAdapter(addedContacts)
                    addGroupBinding.addedParticipantsRecyclerView.adapter = addedContactsAdapter
                    addParticipantsTextView.text = "${addedContacts.size} of 256 selected"





                }
                else{
                    addGroupBinding.addedParticipantsRecyclerView.visibility = View.GONE
                    addParticipantsTextView.text = "Add participants"
                }
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


    // to return contacts from firebase db
    inner class AddedContactsAdapter(items: MutableList<ContactsModel>)
        : RecyclerView.Adapter<AddedContactsAdapter.MyViewHolder>() {

        private var list = items


        override fun getItemCount(): Int {
            return list.size
        }



        override fun onBindViewHolder(
            holder: AddedContactsAdapter.MyViewHolder,
            position: Int
        ) {
            val contact = list[holder.adapterPosition]
            holder.bind(contact)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            return MyViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.added_participant_item_layout,
                    parent,
                    false
                )
            )
        }


        inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),View.OnClickListener{
            private val nameTextView : TextView = itemView.findViewById(R.id.user_name_text_view)
            private val userImageView : ImageView = itemView.findViewById(R.id.user_image_view)
            private val removeContactImageView:ImageView = itemView.findViewById(R.id.contact_removed_image_view)



            init {
                itemView.setOnClickListener(this)

            }

            fun bind(contactsModel: ContactsModel) {

                nameTextView.text = list[adapterPosition].name.substring(0,2)
                if (list[adapterPosition].image.isNotEmpty()) {
                    Picasso.get()
                        .load(list[adapterPosition].image)
                        .placeholder(R.drawable.ic_person)
                        .into(userImageView)
                }
            }

            override fun onClick(itemView: View?) {
                for (item in addedContacts){
                    if (item.uid == contactsClickedMap[adapterPosition]){
                        addedContacts.remove(item)
                        contactsClickedMap.remove(adapterPosition)

                    }
                }
                if (addedContacts.isEmpty()){
                    addGroupBinding.addedParticipantsRecyclerView.visibility = View.GONE
                }
//                removeContactImageView.setOnClickListener {
//                    contactsClickedMap.remove(adapterPosition)
//                }
            }
        }

    }





}