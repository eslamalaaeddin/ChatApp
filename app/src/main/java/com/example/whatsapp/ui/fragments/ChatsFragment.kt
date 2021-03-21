package com.example.whatsapp.ui.fragments

import android.annotation.SuppressLint
import com.example.whatsapp.ui.ui.activities.CallingActivity
import com.example.whatsapp.ui.ui.activities.FindFriendsActivity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.whatsapp.BaseApplication
import com.example.whatsapp.listeners.Callback
import com.example.whatsapp.models.ContactsModel

import com.example.whatsapp.R
import com.example.whatsapp.adapters.ContactAdapterFromFirebase
import com.example.whatsapp.adapters.GroupsAdapter
import com.example.whatsapp.helpers.Utils
import com.example.whatsapp.helpers.Utils.USERS_CHILD
import com.example.whatsapp.models.UserStateModel
import com.example.whatsapp.databinding.FragmentChatsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.example.whatsapp.models.GroupModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

private const val TAG = "ChatsFragment"
private const val CALLER_ID = "receiver id"

class ChatsFragment : Fragment() {
    private lateinit var fragmentChatsBinding: FragmentChatsBinding
    private lateinit var contactsReference: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var currentUser: FirebaseUser
    private lateinit var currentUserId: String
    private lateinit var usersReference: DatabaseReference
    private lateinit var rootReference: DatabaseReference
    private lateinit var phoneContactsAdapter: ContactAdapterFromFirebase
    val listFromFirebaseDb = mutableListOf<ContactsModel>()
    private lateinit var messagesReference: DatabaseReference
    private lateinit var callBy: String
    private var usersIdsList = mutableListOf<String>()
    private var usersNamesList = mutableListOf<String>()
    private var usersImagesList = mutableListOf<String>()
    private lateinit var callback: Callback
    private var stateList = mutableListOf<UserStateModel>()
    private lateinit var groupsAdapter: GroupsAdapter
    private var groupsList = mutableListOf<GroupModel>()


    override fun onAttach(context: Context) {
        super.onAttach(context)
        callback = context as Callback
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = BaseApplication.getAuth()

        currentUser = auth.currentUser!!
        currentUserId = currentUser.uid

        rootReference = BaseApplication.getRootReference()

        usersReference = FirebaseDatabase.getInstance().reference.child("Users")
        messagesReference = FirebaseDatabase.getInstance().reference.child("Messages")
        contactsReference =
            FirebaseDatabase.getInstance().reference.child("Contacts").child(currentUser.uid)


        Log.i(TAG, "TTTT onCreate: ")

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        fragmentChatsBinding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_chats, container, false)

        return fragmentChatsBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fragmentChatsBinding.chatsRecyclerView.apply {
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
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
                for (dataSnapshot in snapshot.children) {
                    val id = dataSnapshot.child("uid").value.toString()
                    if (id != currentUserId && dataSnapshot.key != "Groups") {
                        val name = dataSnapshot.child("name").value.toString()
                        val image = dataSnapshot.child("image").value.toString()
                        val status = dataSnapshot.child("status").value.toString()
                        val currentPhoneNumber = dataSnapshot.child("phoneNumber").value.toString()

                        val date = dataSnapshot.child("state").child("date").value.toString()
                        val time = dataSnapshot.child("state").child("time").value.toString()
                        val state = dataSnapshot.child("state").child("state").value.toString()

                        usersIdsList.add(id)
                        usersNamesList.add(name)
                        usersImagesList.add(image)


                        stateList.add(
                            UserStateModel(
                                date, state, time, "no", "no"
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

                phoneContactsAdapter = ContactAdapterFromFirebase(
                    listFromFirebaseDb,
                    stateList,
                    currentUserId,
                    rootReference,
                    usersIdsList,
                    usersNamesList,
                    usersImagesList,
                    callback
                )
                Utils.privateChatsAdapter = phoneContactsAdapter
                fragmentChatsBinding.chatsRecyclerView.adapter = phoneContactsAdapter
            }

            override fun onCancelled(error: DatabaseError) {

            }

        })
        retrieveGroups()
        //updateUserStatus("online")
        Log.i(TAG, "TTTT onStart: ")
    }

    @SuppressLint("SimpleDateFormat")
    private fun updateUserStatus(state: String) {
        var currentDate = ""
        var currentTime = ""

        val calender = Calendar.getInstance()
        //get date and time
        val dateFormat = SimpleDateFormat("MMM dd, yyyy")
        val timeFormat = SimpleDateFormat("hh:mm a")

        currentDate = dateFormat.format(calender.time)
        currentTime = timeFormat.format(calender.time)

        val userStateMap = HashMap<String, Any>()
        userStateMap["date"] = currentDate
        userStateMap["time"] = currentTime
        userStateMap["state"] = state

        rootReference.child(USERS_CHILD).child(currentUserId).child(Utils.STATE_CHILD)
            .updateChildren(
                userStateMap
            )


    }

    private fun checkForReceivingCalls() {
        usersReference.child(currentUserId).child("Ringing")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.hasChild("ringing")) {
                        //current caller Id
                        callBy = snapshot.child("ringing").value.toString()
                        //send user to calling activity
                        val callingIntent = Intent(context, CallingActivity::class.java)
                        callingIntent.putExtra(CALLER_ID, callBy)
                        startActivity(callingIntent)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })
    }

    private fun sendUserToFindFriendsActivity() {
        val findFriendsIntent = Intent(requireContext(), FindFriendsActivity::class.java)
        startActivity(findFriendsIntent)
    }

    //////////////////////////////////////////////////////Groups logic/////////////////////////////////////////////////
    private fun retrieveGroups() {
        rootReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.hasChild("Groups")) {
                    rootReference.child("Groups").addValueEventListener(object :
                        ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            groupsList.clear()
                            for (group in snapshot.children) {

                                val name = group.child("name").value.toString()
                                val image = group.child("image").value.toString()
                                val status = group.child("status").value.toString()
                                val groupId = group.child("gid").value.toString()

                                val currentGroup = GroupModel(name, image, status, groupId, "", "")

                                groupsList.add(0, currentGroup)
                            }
                            groupsAdapter = GroupsAdapter(
                                groupsList,
                                currentUserId,
                                usersReference,
                                rootReference,
                                callback
                            )
                            Utils.groupsChatAdapter = groupsAdapter
                            fragmentChatsBinding.groupsRecyclerView.adapter = groupsAdapter
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Toast.makeText(context, error.message, Toast.LENGTH_SHORT).show()
                        }
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })

    }


}