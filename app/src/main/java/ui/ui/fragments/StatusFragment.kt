package ui.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.example.whatsapp.R
import com.example.whatsapp.Utils
import com.example.whatsapp.databinding.FragmentChatsBinding
import com.example.whatsapp.databinding.FragmentStatusBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import models.CallModel
import models.StatusModel
import ui.ui.activities.StatusActivity

class StatusFragment : Fragment() {
    private lateinit var fragmentStatusBinding : FragmentStatusBinding
    private lateinit var contactsReference: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var currentUser: FirebaseUser
    private lateinit var currentUserId: String
    private lateinit var usersReference: DatabaseReference
    private lateinit var rootReference: DatabaseReference
    private var statusList = mutableListOf<StatusModel>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        currentUser = auth.currentUser!!

        rootReference = FirebaseDatabase.getInstance().reference
        currentUserId = currentUser.uid
        usersReference = FirebaseDatabase.getInstance().reference.child("Users")
        contactsReference = FirebaseDatabase.getInstance().reference.child("Contacts").child(currentUser.uid)

    }

    override fun onStart() {
        super.onStart()
        retrieveStatuses()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        fragmentStatusBinding =
            DataBindingUtil.inflate(inflater , R.layout.fragment_status , container,false)

        return fragmentStatusBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fragmentStatusBinding.fab.setOnClickListener {
            startActivity(Intent(context,StatusActivity::class.java))
        }

    }

    private fun retrieveStatuses() {
        rootReference.child(Utils.USERS_CHILD).child(currentUserId).child("Status").addValueEventListener(object :
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                statusList.clear()
                for (status in snapshot.children) {

                    val text = status.child("text").value.toString()
                    val color = status.child("color").value.toString()
                    val viewersId = status.child("viewersid").value.toString()
                    val viewsCount = status.child("viewscount").value.toString()
                    val statusId = status.child("statusid").value.toString()
                    val date = status.child("date").value.toString()
                    val time = status.child("time").value.toString()

                    val currentStatus = StatusModel(text,color,viewersId,viewsCount,statusId,date,time)

                    statusList.add(0, currentStatus)
                }
//                callsAdapter = CallsAdapter(statusList)
//                fragmentCallsBinding.callsRecyclerView.adapter = callsAdapter
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, error.message, Toast.LENGTH_SHORT).show()
            }
        })
    }
}