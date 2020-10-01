package ui.ui.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.media.Image
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.whatsapp.R
import com.example.whatsapp.Utils
import com.example.whatsapp.databinding.ActivityGroupCreationBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.squareup.picasso.Picasso
import models.ContactsModel
import models.GroupModel
import okhttp3.internal.Util
import java.util.*
import kotlin.collections.HashMap
private const val GROUP_ID = "group id"
class GroupCreationActivity : AppCompatActivity() {
    private lateinit var groupCreationBinding: ActivityGroupCreationBinding
    private  var addedContactsAdapter =  AddedContactsAdapter(mutableListOf())
    private var addedContacts = mutableListOf<ContactsModel>()
    //firebase authentication instance
    private lateinit var auth: FirebaseAuth

    //Database reference
    private lateinit var rootRef: DatabaseReference

    private  var currentUser: FirebaseUser? = null

    private lateinit var currentUserId : String

    private lateinit var usersRef : DatabaseReference

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //get firebase auth
        auth = FirebaseAuth.getInstance()

        usersRef = FirebaseDatabase.getInstance().reference.child(Utils.USERS_CHILD)

        //get current user
        currentUser = auth.currentUser

        currentUserId = currentUser?.uid.toString()

        rootRef = FirebaseDatabase.getInstance().reference

        groupCreationBinding = DataBindingUtil.setContentView(this,R.layout.activity_group_creation)
        setUpToolbar()
        addedContacts = Utils.dummyList
        addedContactsAdapter = AddedContactsAdapter(addedContacts)
        groupCreationBinding.participantsCountTextView.text = "Participants: ${addedContacts.size}"
        groupCreationBinding.participantsRecyclerView.adapter = addedContactsAdapter


        groupCreationBinding.submitParticipantsFab.setOnClickListener {
            val groupName = groupCreationBinding.groupNameEditText.editableText.toString()
            if (groupName.isNotEmpty()){
                createNewGroup(groupName)
            }
            else{
                Toast.makeText(this, "Provide a group subject and optional group icon", Toast.LENGTH_SHORT).show()
            }
        }



    }

    private fun setUpToolbar() {

       val toolbarView = LayoutInflater.from(this).inflate(R.layout.add_group_custom_toolbar, null)

        setSupportActionBar(groupCreationBinding.mainToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowCustomEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        supportActionBar?.customView = toolbarView



       val newGroupTextView: TextView = findViewById(R.id.new_group_text_view_custom)
       val  addSubjectTextView : TextView = findViewById(R.id.add_participants_text_view)

        newGroupTextView.text = "New group"

        addSubjectTextView.text = "Add subject"

        groupCreationBinding.mainToolbar.setTitleTextColor(Color.WHITE)
        groupCreationBinding.mainToolbar.navigationIcon?.setColorFilter(resources.getColor(R.color.white), PorterDuff.Mode.SRC_IN)
    }

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

                nameTextView.text = list[adapterPosition].name
                if (list[adapterPosition].image.isNotEmpty()) {
                    Picasso.get()
                        .load(list[adapterPosition].image)
                        .placeholder(R.drawable.ic_person)
                        .into(userImageView)
                }
                removeContactImageView.visibility = View.GONE
            }

            override fun onClick(itemView: View?) {

            }
        }

    }


    private fun createNewGroup(groupName:String){

        val gid = UUID.randomUUID().toString()
        val groupMap = HashMap<String, Any>()
        groupMap.put("name", groupName)
        groupMap.put("status", "")
        groupMap.put("image", "")
        groupMap.put("gid", gid)
        groupMap.put("participants", "")
        groupMap.put("admin", currentUserId)

        rootRef.child("Groups").child(gid).updateChildren(groupMap)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val map = HashMap<String,Any>()
                    map[currentUserId] = ""

                    Toast.makeText(this, "$groupName created successfully", Toast.LENGTH_SHORT).show()

                    rootRef.child("Groups").child(gid).
                    child("participants").updateChildren(map).addOnCompleteListener {
                        for (participant in addedContacts){
                            map[participant.uid] = ""
                            rootRef.child("Groups").child(gid).
                            child("participants").updateChildren(map).addOnCompleteListener {

                                rootRef.child("Users").child(participant.uid).
                                child("Groups").child(gid).setValue("").addOnCompleteListener {

                                            if (it.isComplete){
                                                sendUserToGroupChatActivity(gid)
                                                 finish()
                                            }
                                }
                            }
                        }
                    }

                }

                else{
                    Toast.makeText(this, task.exception?.message, Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun sendUserToGroupChatActivity(groupId: String) {
        val groupChatIntent = Intent(this , GroupsChatActivity::class.java)
        groupChatIntent.putExtra(GROUP_ID,groupId)
        groupChatIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(groupChatIntent)
    }

}