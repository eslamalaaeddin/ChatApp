package ui.ui.fragments

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.whatsapp.R
import com.example.whatsapp.Utils
import com.example.whatsapp.databinding.FragmentStatusBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.squareup.picasso.Picasso
import models.CallModel
import models.StatusModel
import ui.ui.activities.StatusActivity
import ui.ui.activities.StatusViewerActivity

private const val TAG = "StatusFragment"
class StatusFragment : Fragment() {
    private lateinit var fragmentStatusBinding : FragmentStatusBinding
    private lateinit var contactsReference: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var currentUser: FirebaseUser
    private lateinit var currentUserId: String
    private lateinit var usersReference: DatabaseReference
    private lateinit var rootReference: DatabaseReference
    private var statusList = mutableListOf<StatusModel>()
    private var othersStatusList = mutableListOf<StatusModel>()
    private var contactsNames = mutableListOf<String>()
    private var contactsStatusAdapter = ContactsStatusAdapter(emptyList())
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        currentUser = auth.currentUser!!

        rootReference = FirebaseDatabase.getInstance().reference
        currentUserId = currentUser.uid
        usersReference = FirebaseDatabase.getInstance().reference.child("Users")
        contactsReference = FirebaseDatabase.getInstance().reference.child("Contacts").child(
            currentUser.uid
        )

    }

    override fun onStart() {
        super.onStart()
        retrieveMyStatuses()
        retrieveOthersStatuses()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        fragmentStatusBinding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_status, container, false)

        return fragmentStatusBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fragmentStatusBinding.fab.setOnClickListener {
            startActivity(Intent(context, StatusActivity::class.java))
        }

        fragmentStatusBinding.contactsStatusRecyclerView.apply {
            addItemDecoration( DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            layoutManager = LinearLayoutManager(context)
        }

    }

    private fun retrieveMyStatuses() {
        rootReference.child(Utils.USERS_CHILD).child(currentUserId).child("Status").addValueEventListener(
            object :
                ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    statusList.clear()
                    for (status in snapshot.children) {
                        val by = status.child("by").value.toString()
                        val text = status.child("text").value.toString()
                        val color = status.child("color").value.toString()
                        val viewersId = status.child("viewersid").value.toString()
                        val viewsCount = status.child("viewscount").value.toString()
                        val statusId = status.child("statusid").value.toString()
                        val date = status.child("date").value.toString()
                        val time = status.child("time").value.toString()

                        val currentStatus = StatusModel(
                            by,
                            text,
                            color,
                            viewersId,
                            viewsCount,
                            statusId,
                            date,
                            time
                        )

                        statusList.add( currentStatus)
                    }
                    checkStatusesSize()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(context, error.message, Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun retrieveOthersStatuses() {
        rootReference.child(Utils.USERS_CHILD).
        addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                for (child in snapshot.children) {
                    if (child.hasChild("Status")) {

                        rootReference.child(Utils.USERS_CHILD).child(child.key.toString()).child("Status")
                            .addValueEventListener(object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    for (snap in snapshot.children) {
                                        val by = snap.child("by").value.toString()
                                        Log.i(TAG, "ZZZ onDataChange: by $by")
                                        Log.i(TAG, "ZZZ onDataChange: current $currentUserId")
                                        if (by != currentUserId) {
                                            rootReference.child(Utils.USERS_CHILD)
                                                .child(child.key.toString()).child("Status")
                                                .addValueEventListener(object : ValueEventListener {
                                                    override fun onDataChange(snapshot: DataSnapshot) {
                                                        othersStatusList.clear()
                                                        for (status in snapshot.children) {
                                                            val by =
                                                                status.child("by").value.toString()
                                                            val text =
                                                                status.child("text").value.toString()
                                                            val color =
                                                                status.child("color").value.toString()
                                                            val viewersId =
                                                                status.child("viewersid").value.toString()
                                                            val viewsCount =
                                                                status.child("viewscount").value.toString()
                                                            val statusId =
                                                                status.child("statusid").value.toString()
                                                            val date =
                                                                status.child("date").value.toString()
                                                            val time =
                                                                status.child("time").value.toString()

                                                            val currentStatus = StatusModel(
                                                                by,
                                                                text,
                                                                color,
                                                                viewersId,
                                                                viewsCount,
                                                                statusId,
                                                                date,
                                                                time
                                                            )

                                                            othersStatusList.add(0, currentStatus)
                                                        }
                                                        contactsStatusAdapter =
                                                            ContactsStatusAdapter(othersStatusList)
                                                        fragmentStatusBinding.contactsStatusRecyclerView.adapter =
                                                            contactsStatusAdapter
                                                    }

                                                    override fun onCancelled(error: DatabaseError) {
                                                        Toast.makeText(
                                                            context,
                                                            error.message,
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                })
                                        }
                                    }
                                }
                                override fun onCancelled(error: DatabaseError) {
                                }
                            })


                    }
                }

            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, error.message, Toast.LENGTH_SHORT).show()
            }
        })
    }

    @SuppressLint("SetTextI18n", "UseCompatLoadingForDrawables")
    private fun checkStatusesSize(){

        if (statusList.isNotEmpty()) {
            val status = statusList[statusList.size - 1]
            fragmentStatusBinding.statusCountTextView.text = statusList.size.toString()

            fragmentStatusBinding.tabAddStatusTextView.text = "${status.date} ${status.time}"
            fragmentStatusBinding.statusImageView.visibility = View.INVISIBLE
            fragmentStatusBinding.statusTextView.visibility = View.VISIBLE

            fragmentStatusBinding.statusTextView.text = status.text

            val color = Color.parseColor("#${Integer.toHexString(status.color.toInt())}")

            val drawable = resources.getDrawable(R.drawable.circle)
            drawable.mutate().setColorFilter(color, PorterDuff.Mode.SRC_IN)
            fragmentStatusBinding.statusTextView.background = drawable


            fragmentStatusBinding.statusTextView.setOnClickListener {
                startActivity(Intent(context,StatusViewerActivity::class.java))
            }
        }
        //empty
        else{
            fragmentStatusBinding.statusCountTextView.text = "+"
            fragmentStatusBinding.tabAddStatusTextView.text = "Tab to add status update"
            fragmentStatusBinding.statusImageView.visibility = View.VISIBLE
            fragmentStatusBinding.statusTextView.visibility = View.INVISIBLE
            fragmentStatusBinding.statusImageView.setOnClickListener {
                startActivity(Intent(context, StatusActivity::class.java))
            }
        }
    }

    inner class ContactsStatusAdapter (private var list:List<StatusModel>) : RecyclerView.Adapter<ContactsStatusAdapter.ContactsStatusHolder>() {

        inner class ContactsStatusHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener, View.OnLongClickListener {
            private val statusTextView : TextView = itemView.findViewById(R.id.status_text_view)
            private val statusCountTextView: TextView =  itemView.findViewById(R.id.status_count_text_view)
            private val contactNameTextView : TextView = itemView.findViewById(R.id.contact_name_text_view)
            private val statusTimeTextView : TextView = itemView.findViewById(R.id.status_time_text_view)


            init {
                itemView.setOnClickListener(this)
                itemView.setOnLongClickListener(this)
            }

            @SuppressLint("SetTextI18n", "UseCompatLoadingForDrawables")
            fun bind (statusModel: StatusModel) {

                val color = Color.parseColor("#${Integer.toHexString(statusModel.color.toInt())}")
                val drawable = resources.getDrawable(R.drawable.circle)
                drawable.mutate().setColorFilter(color, PorterDuff.Mode.SRC_IN)

                statusTextView.background = drawable
                statusTextView.text = statusModel.text
                statusCountTextView.visibility = View.INVISIBLE

                rootReference.child(Utils.USERS_CHILD).child(statusModel.by).
                child("name").addValueEventListener(object : ValueEventListener{
                    override fun onDataChange(snapshot: DataSnapshot) {
                        contactNameTextView.text = snapshot.value.toString()
                    }

                    override fun onCancelled(error: DatabaseError) {
                    }
                })

                statusTimeTextView.text = "${statusModel.date} ${statusModel.time}"
            }

            override fun onClick(item: View?) {
//                val group = list[adapterPosition]
//                callback.onGroupClicked(group)
            }

            override fun onLongClick(item: View?): Boolean {
                return true
            }

        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactsStatusHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.contact_status_item_layout,parent,false )

            return ContactsStatusHolder(view)
        }

        override fun getItemCount(): Int {
            return list.size
        }

        override fun onBindViewHolder(holder: ContactsStatusHolder, position: Int) {
            val status = list[holder.adapterPosition]
            holder.bind(status)
        }
    }


}