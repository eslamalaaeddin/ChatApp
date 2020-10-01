package ui.ui.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
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
import com.example.whatsapp.BaseApplication
import com.example.whatsapp.Callback
import com.example.whatsapp.R
import com.example.whatsapp.Utils.USERS_CHILD
import com.example.whatsapp.databinding.FragmentCallsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.squareup.picasso.Picasso
import models.CallModel

class CallsFragment : Fragment() {
    private lateinit var fragmentCallsBinding : FragmentCallsBinding
    private lateinit var contactsReference: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var currentUser: FirebaseUser

    private lateinit var currentUserId: String
    private lateinit var usersReference: DatabaseReference
//    private lateinit var contactsAdapter: FirebaseRecyclerAdapter<ContactsModel, ContactsViewHolder>

    private lateinit var rootReference: DatabaseReference

    private var callsAdapter = CallsAdapter(emptyList())
    private var callsList = mutableListOf<CallModel>()

    private lateinit var callback: Callback

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callback = context as Callback
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = (activity?.application as BaseApplication).getFirebaseAuthenticationReference()
        currentUser = auth.currentUser!!

        rootReference = (activity?.application as BaseApplication).getDatabaseRootReference()
        currentUserId = currentUser.uid
        usersReference = FirebaseDatabase.getInstance().reference.child("Users")


    }



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        fragmentCallsBinding =
            DataBindingUtil.inflate(inflater , R.layout.fragment_calls , container,false)

        return fragmentCallsBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fragmentCallsBinding.callsRecyclerView.apply {
            addItemDecoration( DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            layoutManager = LinearLayoutManager(context)
        }
    }

    override fun onStart() {
        super.onStart()
        retrieveCalls()
    }

    private fun retrieveCalls() {
        rootReference.child(USERS_CHILD).child(currentUserId).child("Calls").addValueEventListener(object :
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                callsList.clear()
                for (call in snapshot.children) {

                    val toId = call.child("toid").value.toString()
                    val image = call.child("image").value.toString()
                    val time = call.child("time").value.toString()
                    val date = call.child("date").value.toString()
                    val type = call.child("type").value.toString()
                    val caller = call.child("caller").value.toString()

                    val currentCall = CallModel(toId,image,time,date,type,caller)

                    callsList.add(0, currentCall)
                }
                callsAdapter = CallsAdapter(callsList)
                fragmentCallsBinding.callsRecyclerView.adapter = callsAdapter
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, error.message, Toast.LENGTH_SHORT).show()
            }
        })
    }

    inner class CallsAdapter (private var list:List<CallModel>) : RecyclerView.Adapter<CallsAdapter.CallsHolder>() {

        inner class CallsHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener, View.OnLongClickListener {
            private val userCalledNameTextView : TextView = itemView.findViewById(R.id.user_name_text_view)
            private val callTimeTextView: TextView =  itemView.findViewById(R.id.call_time_text_view)
            private val userCalledImageView : ImageView = itemView.findViewById(R.id.user_image_view)
            private val videoCallTypeImageView : ImageView = itemView.findViewById(R.id.video_call_type_image_view)
            private val voiceCallTypeImageView : ImageView = itemView.findViewById(R.id._voice_call_type_image_view)

            private val callMadeImageView : ImageView = itemView.findViewById(R.id.call_made_image_view)
            private val callReceivedImageView : ImageView = itemView.findViewById(R.id.call_received_image_view)

            init {
                itemView.setOnClickListener(this)
                itemView.setOnLongClickListener(this)
            }

            @SuppressLint("SetTextI18n")
            fun bind (call : CallModel) {

                callTimeTextView.text = "${call.time}, ${call.date}"

                if (call.type == "video") {
                    videoCallTypeImageView.visibility = View.VISIBLE
                    voiceCallTypeImageView.visibility = View.INVISIBLE
                }else{
                    videoCallTypeImageView.visibility = View.INVISIBLE
                    voiceCallTypeImageView.visibility = View.VISIBLE
                }

                //call made
                if (call.caller == currentUserId) {
                    callReceivedImageView.visibility = View.INVISIBLE
                    callMadeImageView.visibility = View.VISIBLE
                }
                else{
                    callReceivedImageView.visibility = View.VISIBLE
                    callMadeImageView.visibility = View.INVISIBLE
                }

                if (call.toid == currentUserId) {
                    rootReference.child(USERS_CHILD).child(currentUserId).child("Calls")
                        .child(call.caller).addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val id = snapshot.key.toString()
                            rootReference.child(USERS_CHILD).child(id)
                                .addValueEventListener(object : ValueEventListener {
                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        val name = snapshot.child("name").value.toString()
                                        val imageUrl = snapshot.child("image").value.toString()

                                        userCalledNameTextView.text = name

                                        if (imageUrl.isNotEmpty()) {
                                            Picasso.get()
                                                .load(imageUrl)
                                                .placeholder(R.drawable.ic_person)
                                                .into(userCalledImageView)
                                        }

                                    }

                                    override fun onCancelled(error: DatabaseError) {

                                    }
                                })
                        }

                        override fun onCancelled(error: DatabaseError) {
                            TODO("Not yet implemented")
                        }
                    })
                }
                else{
                    rootReference.child(USERS_CHILD).child(currentUserId).child("Calls")
                        .child(call.toid).addValueEventListener(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                val id = snapshot.key.toString()
                                rootReference.child(USERS_CHILD).child(id)
                                    .addValueEventListener(object : ValueEventListener {
                                        override fun onDataChange(snapshot: DataSnapshot) {
                                            val name = snapshot.child("name").value.toString()
                                            val imageUrl = snapshot.child("image").value.toString()

                                            userCalledNameTextView.text = name

                                            if (imageUrl.isNotEmpty()) {
                                                Picasso.get()
                                                    .load(imageUrl)
                                                    .placeholder(R.drawable.ic_person)
                                                    .into(userCalledImageView)
                                            }

                                        }

                                        override fun onCancelled(error: DatabaseError) {

                                        }
                                    })
                            }

                            override fun onCancelled(error: DatabaseError) {
                            }
                        })
                }

            }

            override fun onClick(item: View?) {
                val callerId = list[adapterPosition].caller
                if (callerId == currentUserId){
                    callback.onCallClicked(list[adapterPosition].toid)
                }
                else{
                    callback.onCallClicked(callerId)

                }

            }

            override fun onLongClick(item: View?): Boolean {
                return true
            }

        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CallsHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.call_item_layout,parent,false )

            return CallsHolder(view)
        }

        override fun getItemCount(): Int {
            return list.size
        }

        override fun onBindViewHolder(holder: CallsHolder, position: Int) {
            val call = list[holder.adapterPosition]
            holder.bind(call)
        }
    }

}