package com.example.whatsapp.ui.activities

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
import android.widget.*
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.whatsapp.BaseApplication
import com.example.whatsapp.R
import com.example.whatsapp.adapters.AddedContactsAdapter
import com.example.whatsapp.adapters.ContactAdapterFromFirebaseFromAddGroupActivity
import com.example.whatsapp.helpers.Utils
import com.example.whatsapp.databinding.ActivityAddGroupBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.example.whatsapp.models.ContactsModel
import com.example.whatsapp.models.PhoneContactModel
import pub.devrel.easypermissions.EasyPermissions

private const val TAG = "AddGroupActivity"
class AddGroupActivity : AppCompatActivity() {
    private lateinit var addGroupBinding: ActivityAddGroupBinding
    private lateinit var toolbarView: View
    private lateinit var newGroupTextView: TextView
    private lateinit var addParticipantsTextView: TextView

    private lateinit var usersReference: DatabaseReference
    private lateinit var rootReference: DatabaseReference

    private var contactsNumbersList = mutableListOf<String>()

    //firebase authentication instance
    private lateinit var auth: FirebaseAuth

    private lateinit var currentUser: FirebaseUser

    private lateinit var phoneContactsAdapter : ContactAdapterFromFirebaseFromAddGroupActivity
    private lateinit var addedContactsAdapter : AddedContactsAdapter
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

        auth = BaseApplication.getAuth()
        rootReference = BaseApplication.getRootReference()
        currentUser = auth.currentUser!!
        usersReference = rootReference.child("Users")
        setUpToolbar()

        requestPermissions()

        addGroupBinding.findFriendsRecyclerView.layoutManager = LinearLayoutManager(this)
        usersReference.addValueEventListener(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {
                listFromFirebaseDb.clear()
                for ((k,user) in snapshot.children.withIndex()) {
                    var tempId = ""
                    if (user.hasChild("phoneNumber")) {
                        val currentPhoneNumber =
                            user.child("phoneNumber").value.toString()

                        tempId = user.child("uid").value.toString()
                        if (user.child("phoneNumber").value != null) {
                            for ((i, item) in distinctContactList.withIndex()) {
                                if (item.number == currentPhoneNumber || currentPhoneNumber.contains(
                                        item.number
                                    )
                                ) {
                                    val name = user.child("name").value.toString()
                                    val image = user.child("image").value.toString()
                                    val status = user.child("status").value.toString()
                                    val id = user.child("uid").value.toString()

                                    Log.i(TAG, "onDataChange: ISLAM  $i")
                                    Log.i(TAG, "onDataChange: ISLAM  $k")

                                    //to ignore my profile
                                    if (  id == currentUser.uid  ) {

                                    } else {
                                        listFromFirebaseDb.add(
                                            ContactsModel(
                                                name,
                                                image,
                                                status,
                                                id,
                                                currentPhoneNumber
                                            )
                                        )
                                    }

                                    if (k>0 && listFromFirebaseDb.size > k && listFromFirebaseDb[k].uid == listFromFirebaseDb[k-1].uid ){
                                        listFromFirebaseDb.removeAt(k)
                                    }

                                }
                            }
                        }

                    }
                }

                phoneContactsAdapter = ContactAdapterFromFirebaseFromAddGroupActivity(listFromFirebaseDb, contactsClickedMap, addedContacts)
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
               startActivity(Intent(this, GroupCreationActivity::class.java))
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