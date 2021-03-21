package com.example.whatsapp.adapters

import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.whatsapp.R
import com.example.whatsapp.listeners.Callback
import com.example.whatsapp.models.GroupModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso

class GroupsAdapter(
    private var list: MutableList<GroupModel>,
    private var currentUserId: String,
    private var usersReference: DatabaseReference,
    private var rootReference: DatabaseReference,
    private var callback: Callback
) :
    RecyclerView.Adapter<GroupsAdapter.GroupsHolder>(), Filterable {

    val groupsListFull = mutableListOf<GroupModel>()

    init {
        groupsListFull.addAll(list)
    }

    inner class GroupsHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener, View.OnLongClickListener {
        private val groupNameTextView: TextView =
            itemView.findViewById(R.id.group_name_text_view)
        private val lastMessageTextView: TextView =
            itemView.findViewById(R.id.group_last_message_text_view)
        private val groupImageView: ImageView = itemView.findViewById(R.id.group_image_view)

        private val lastMessageTimeTextView: TextView = itemView.findViewById(R.id.date_text_view)
        private val lastMessageSenderNameTextView: TextView =
            itemView.findViewById(R.id.last_message_sender_name_text_view)

        private val checkedMessageImageView: ImageView =
            itemView.findViewById(R.id.message_checked_image_view)

        init {
            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener(this)
        }

        fun bind(group: GroupModel) {
            groupNameTextView.text = group.name
            // groupLastSeenTextView.text = group.status

            val imageUrl = group.image
            if (imageUrl.isNotEmpty()) {
                Picasso.get()
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_group)
                    .into(groupImageView)
            }

            rootReference.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.hasChild("Groups")) {
                        rootReference.child("Groups").child(group.gid)
                            .addValueEventListener(object :
                                ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    if (snapshot.hasChild("Messages")) {
                                        usersReference.child(currentUserId).child("Groups")
                                            .child(group.gid).child("Messages")
                                            .addValueEventListener(object :
                                                ValueEventListener {
                                                override fun onDataChange(snapshot: DataSnapshot) {
                                                    if (snapshot.hasChildren()) {
                                                        val lastMessage = snapshot.children.last()
                                                        val lastMessageTime =
                                                            "${lastMessage.child("date").value.toString()} ${
                                                                lastMessage.child("time").value.toString()
                                                            }"

                                                        lastMessageTimeTextView.text =
                                                            lastMessageTime
                                                    }
                                                }

                                                override fun onCancelled(error: DatabaseError) {
                                                }
                                            })
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {
                                }
                            })
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })


            // Users messages
            rootReference.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.hasChild("Groups")) {
                        rootReference.child("Groups").addValueEventListener(object :
                            ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                for (group in snapshot.children) {
                                    if (group.hasChild("Messages")) {
                                        //if last message is from me
                                        val lastMessageSenderId =
                                            group.child("Messages").children.last()
                                                .child("from").value.toString()
                                        val lastMessage = group.child("Messages").children.last()
                                            .child("message").value.toString()
                                        if (lastMessageSenderId == currentUserId) {
                                            lastMessageSenderNameTextView.visibility = View.GONE
                                            val messageType =
                                                group.child("Messages").children.last()
                                                    .child("type").value.toString()

                                            if (messageType == "text") {
                                                lastMessageTextView.text = lastMessage
                                            } else if (messageType == "audio") {
                                                lastMessageTextView.text = "Audio"
                                            } else if (messageType == "image" || messageType == "captured image") {
                                                lastMessageTextView.text = "Photo"
                                            } else if (messageType == "video") {
                                                lastMessageTextView.text = "Video"
                                            } else if (messageType == "pdf" || messageType == "docx") {
                                                lastMessageTextView.text = "Document"
                                            }
                                        }
                                        //if last message is not from me
                                        else {
                                            lastMessageSenderNameTextView.visibility = View.VISIBLE
                                            val messageType =
                                                group.child("Messages").children.last()
                                                    .child("type").value.toString()
                                            usersReference.child(lastMessageSenderId)
                                                .addValueEventListener(object :
                                                    ValueEventListener {
                                                    override fun onDataChange(snapshot: DataSnapshot) {

                                                        lastMessageSenderNameTextView.text =
                                                            "${snapshot.child("name").value.toString()}:"
                                                    }

                                                    override fun onCancelled(error: DatabaseError) {
                                                    }
                                                })

                                            if (messageType == "text") {
                                                checkedMessageImageView.visibility = View.GONE
                                                lastMessageTextView.text = lastMessage
                                            } else if (messageType == "audio") {
                                                lastMessageTextView.text = "audio time"
                                                checkedMessageImageView.visibility = View.VISIBLE
                                                checkedMessageImageView.setImageResource(R.drawable.ic_mic)
                                                checkedMessageImageView.setColorFilter(
                                                    itemView.context.resources.getColor(
                                                        R.color.green
                                                    ), PorterDuff.Mode.SRC_IN
                                                )
                                            } else if (messageType == "image" || messageType == "captured image") {
                                                lastMessageTextView.text = "Photo"
                                                checkedMessageImageView.visibility = View.VISIBLE
                                                checkedMessageImageView.setImageResource(R.drawable.ic_image)
                                            } else if (messageType == "video") {
                                                lastMessageTextView.text = "Video"
                                                checkedMessageImageView.visibility = View.VISIBLE
                                                checkedMessageImageView.setImageResource(R.drawable.ic_video_call)
                                                checkedMessageImageView.setColorFilter(
                                                    itemView.context.resources.getColor(
                                                        R.color.light_gray
                                                    ), PorterDuff.Mode.SRC_IN
                                                )
                                            } else if (messageType == "pdf" || messageType == "docx") {
                                                lastMessageTextView.text = "Document"
                                                checkedMessageImageView.visibility = View.VISIBLE
                                                checkedMessageImageView.setImageResource(R.drawable.ic_file)
                                                checkedMessageImageView.setColorFilter(
                                                    itemView.context.resources.getColor(
                                                        R.color.light_gray
                                                    ), PorterDuff.Mode.SRC_IN
                                                )
                                            }

                                        }
                                    }
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                            }
                        })
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })


        }

        override fun onClick(item: View?) {
            val groupId = list[adapterPosition].gid
            callback.onGroupClicked(groupId)
        }

        override fun onLongClick(item: View?): Boolean {
            return true
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupsHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.group_item_layout, parent, false)

        return GroupsHolder(view)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: GroupsHolder, position: Int) {
        val group = list[holder.adapterPosition]
        holder.bind(group)
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            //Background thread
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val filteredListOfContactsModel = mutableListOf<GroupModel>()
                if (constraint == null || constraint.isEmpty()) {
                    filteredListOfContactsModel.addAll(groupsListFull)
                } else {
                    val filterPattern = constraint.toString().toLowerCase().trim()
                    for (item in groupsListFull) {
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
                list.addAll(filterResults?.values as List<GroupModel>)
                notifyDataSetChanged()
            }
        }
    }
}