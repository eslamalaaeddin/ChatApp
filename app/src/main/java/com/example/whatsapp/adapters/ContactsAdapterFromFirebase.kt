package com.example.whatsapp.adapters

import android.annotation.SuppressLint
import android.graphics.PorterDuff
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.whatsapp.R
import com.example.whatsapp.listeners.Callback
import com.example.whatsapp.models.ContactsModel
import com.example.whatsapp.models.UserStateModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso

class ContactAdapterFromFirebase(
    private val contactsModel: MutableList<ContactsModel>,
    private val userSatesModel: List<UserStateModel>,
    private var currentUserId: String,
    private var rootReference: DatabaseReference,
    private var usersIdsList: MutableList<String>,
    private var usersNamesList: MutableList<String>,
    private var usersImagesList: MutableList<String>,
    private var callback: Callback

) : RecyclerView.Adapter<ContactAdapterFromFirebase.ContactsViewHolder>(), Filterable {

    val contactsListFull = mutableListOf<ContactsModel>()

    init {
        contactsListFull.addAll(contactsModel)
    }

    override fun getItemCount(): Int {
        return contactsModel.size
    }

    override fun onBindViewHolder(holder: ContactsViewHolder, position: Int) {
        val userStateModel = userSatesModel[holder.adapterPosition]
        val contactModel = contactsModel[holder.adapterPosition]
        holder.bind(userStateModel, contactModel)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactsViewHolder {
        return ContactsViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.friend_item_layout, parent, false)
        )
    }

    inner class ContactsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        private val userNameTextView: TextView = itemView.findViewById(R.id.user_name_text_view)
        private val userLastSeenTextView: TextView =
            itemView.findViewById(R.id.user_status_text_view)
        private val lastMessageDateTextView: TextView =
            itemView.findViewById(R.id.date_text_view)
        private val checkedMessageImageView: ImageView =
            itemView.findViewById(R.id.message_checked_image_view)
        private val userImageView: ImageView = itemView.findViewById(R.id.user_image_view)
        private val messagesCountTextView: TextView =
            itemView.findViewById(R.id.messages_count_text_view)

        fun bind(userStateModel: UserStateModel, contactModel: ContactsModel) {


            //to show last message in user's item
            if (currentUserId != contactModel.uid) {
                rootReference.child("Messages").child(currentUserId).child(contactModel.uid)
                    .addValueEventListener(object : ValueEventListener {
                        @SuppressLint("SetTextI18n")
                        override fun onDataChange(snapshot: DataSnapshot) {

                            //there is a messages
                            if (snapshot.value != null && snapshot.hasChildren()) {
                                val fromWhom =
                                    snapshot.children.last().child("from").value.toString()

                                val messageState =
                                    snapshot.children.last().child("seen").value.toString()

                                //sent from me
                                if (fromWhom == currentUserId) {
                                    val currentMessage = snapshot.children.last()
                                    val currentMessageType =
                                        snapshot.children.last().child("type").value.toString()
                                    //message time
                                    lastMessageDateTextView.text =
                                        "${currentMessage.child("date").value.toString()} ${
                                            currentMessage.child(
                                                "time"
                                            ).value.toString()
                                        }"

                                    lastMessageDateTextView.setTextColor(
                                        ContextCompat.getColor(
                                            itemView.context,
                                            R.color.light_gray
                                        )
                                    )
                                    messagesCountTextView.visibility = View.INVISIBLE
                                    //message is text
                                    if (currentMessageType == "text") {
                                        val lastMessage =
                                            currentMessage.child("message").value.toString()
                                        userLastSeenTextView.text = lastMessage

                                    }
                                    //Documents
                                    else if (currentMessageType == "docx" || currentMessageType == "pdf") {
                                        userLastSeenTextView.text = "Document"
                                    }

                                    //images
                                    else if (currentMessageType == "image" || currentMessageType == "captured image") {
                                        userLastSeenTextView.text = "Photo"

                                    }

                                    //audio
                                    else if (currentMessageType == "audio") {
                                        userLastSeenTextView.text = "Voice"

                                    }

                                    //Videos
                                    else if (currentMessageType == "video") {
                                        userLastSeenTextView.text = "Video"

                                    }

                                    //check to see if it is seen
                                    if (messageState == "yes") {
                                        checkedMessageImageView.visibility = View.VISIBLE
                                        checkedMessageImageView.setImageResource(R.drawable.ic_check)
                                        checkedMessageImageView.setColorFilter(
                                            itemView.context.resources.getColor(
                                                R.color.blue
                                            ), PorterDuff.Mode.SRC_IN
                                        )
                                    } else {
                                        checkedMessageImageView.visibility = View.VISIBLE
                                        checkedMessageImageView.setImageResource(R.drawable.ic_check)
                                        checkedMessageImageView.setColorFilter(
                                            itemView.context.resources.getColor(
                                                R.color.light_gray
                                            ), PorterDuff.Mode.SRC_IN
                                        )
                                    }

                                }
                                //shown to me
                                else {
                                    val currentMessage = snapshot.children.last()
                                    val currentMessageType =
                                        snapshot.children.last().child("type").value.toString()

                                    val currentMessageState =
                                        snapshot.children.last().child("seen").value.toString()

                                    lastMessageDateTextView.text =
                                        "${currentMessage.child("date").value.toString()} ${
                                            currentMessage.child(
                                                "time"
                                            ).value.toString()
                                        }"

                                    if (currentMessageState == "yes") {

                                        lastMessageDateTextView.setTextColor(
                                            ContextCompat.getColor(
                                                itemView.context,
                                                R.color.light_gray
                                            )
                                        )

                                        messagesCountTextView.visibility = View.GONE

                                    } else {
                                        lastMessageDateTextView.text =
                                            "${currentMessage.child("date").value.toString()} ${
                                                currentMessage.child(
                                                    "time"
                                                ).value.toString()
                                            }"

                                        lastMessageDateTextView.setTextColor(
                                            ContextCompat.getColor(
                                                itemView.context,
                                                R.color.green
                                            )
                                        )

                                        messagesCountTextView.visibility = View.VISIBLE

                                        messagesCountTextView.text =
                                            snapshot.childrenCount.toString()
                                    }

                                    //message is text
                                    if (currentMessageType == "text") {
                                        val lastMessage =
                                            currentMessage.child("message").value.toString()
                                        userLastSeenTextView.text = lastMessage

                                        checkedMessageImageView.visibility = View.GONE
                                    }
                                    //Documents
                                    else if (currentMessageType == "docx" || currentMessageType == "pdf") {
                                        userLastSeenTextView.text = "Document"

                                        checkedMessageImageView.visibility = View.VISIBLE
                                        checkedMessageImageView.setImageResource(R.drawable.ic_file)
                                        checkedMessageImageView.setColorFilter(
                                            itemView.context.resources.getColor(
                                                R.color.light_gray
                                            ), PorterDuff.Mode.SRC_IN
                                        )
                                    }

                                    //images
                                    else if (currentMessageType == "image" || currentMessageType == "captured image") {
                                        userLastSeenTextView.text = "Photo"
                                        checkedMessageImageView.visibility = View.VISIBLE
                                        checkedMessageImageView.setImageResource(R.drawable.ic_image)
                                        checkedMessageImageView.setColorFilter(
                                            itemView.context.resources.getColor(
                                                R.color.light_gray
                                            ), PorterDuff.Mode.SRC_IN
                                        )

                                    }

                                    //audio
                                    else if (currentMessageType == "audio") {
                                        userLastSeenTextView.text = "Voice"

                                        checkedMessageImageView.visibility = View.VISIBLE
                                        checkedMessageImageView.setImageResource(R.drawable.ic_audio)
                                        checkedMessageImageView.setColorFilter(
                                            itemView.context.resources.getColor(
                                                R.color.green
                                            ), PorterDuff.Mode.SRC_IN
                                        )

                                    }

                                    //Videos
                                    else if (currentMessageType == "video") {
                                        userLastSeenTextView.text = "Video"

                                        checkedMessageImageView.visibility = View.VISIBLE
                                        checkedMessageImageView.setImageResource(R.drawable.ic_video)
                                        checkedMessageImageView.setColorFilter(
                                            itemView.context.resources.getColor(
                                                R.color.light_gray
                                            ), PorterDuff.Mode.SRC_IN
                                        )

                                    }
                                }
                            } else {
                                userLastSeenTextView.text = "No messages yet"
                                checkedMessageImageView.visibility = View.GONE
                                messagesCountTextView.visibility = View.GONE
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                        }
                    })
            }

            userNameTextView.text = contactModel.name


            val imageUrl = contactModel.image
            if (imageUrl.isNotEmpty()) {
                Picasso.get()
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_person)
                    .into(userImageView)
            }
        }

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(itemView: View?) {
            val userId = usersIdsList[adapterPosition]
            val userName = usersNamesList[adapterPosition]
            val userImage = usersImagesList[adapterPosition]

            callback.onUserChatClicked(userName, userId, userImage)
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
                contactsModel.clear()
                contactsModel.addAll(filterResults?.values as List<ContactsModel>)
                notifyDataSetChanged()
            }
        }
    }

}
