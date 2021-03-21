package com.example.whatsapp.ui.ui.activities

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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.whatsapp.BaseApplication
import com.example.whatsapp.R
import com.example.whatsapp.adapters.ContactAdapterFromFirebaseFindFriendsActivity
import com.example.whatsapp.databinding.ActivityFindFriendsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.squareup.picasso.Picasso
import com.example.whatsapp.models.ContactsModel
import com.example.whatsapp.models.PhoneContactModel
import pub.devrel.easypermissions.EasyPermissions

private const val TAG = "FindFriendsActivity"
private const val USER_ID = "user id"
class FindFriendsActivity : AppCompatActivity() {
    private lateinit var activityFindFriendsBinding: ActivityFindFriendsBinding

    private lateinit var usersReference: DatabaseReference
    private lateinit var rootReference: DatabaseReference

    private var contactsNumbersList = mutableListOf<String>()

    //firebase authentication instance
    private lateinit var auth: FirebaseAuth

    private lateinit var currentUser: FirebaseUser
    private lateinit var currentUserId: String

    private lateinit var phoneContactsAdapter : ContactAdapterFromFirebaseFindFriendsActivity

    private val contactList : MutableList<PhoneContactModel> = mutableListOf()
    private var distinctContactList : List<PhoneContactModel> = listOf()

    val listFromFirebaseDb = mutableListOf<ContactsModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activityFindFriendsBinding = DataBindingUtil.setContentView(
            this,
            R.layout.activity_find_friends
        )
        auth = BaseApplication.getAuth()
        rootReference = BaseApplication.getRootReference()
        currentUser = auth.currentUser!!
        currentUserId = auth.currentUser!!.uid
        usersReference = rootReference.child("Users")
        setUpToolbar()

        requestPermissions()

        activityFindFriendsBinding.findFriendsRecyclerView.layoutManager = LinearLayoutManager(this@FindFriendsActivity)
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
                                    if (  id == currentUserId  ) {

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

                phoneContactsAdapter = ContactAdapterFromFirebaseFindFriendsActivity(listFromFirebaseDb)
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