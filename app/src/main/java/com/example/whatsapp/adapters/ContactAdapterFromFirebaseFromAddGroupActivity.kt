package com.example.whatsapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.whatsapp.R
import com.example.whatsapp.models.ContactsModel
import com.squareup.picasso.Picasso

// to return contacts from firebase db
class ContactAdapterFromFirebaseFromAddGroupActivity(
    items: MutableList<ContactsModel>,
    private var contactsClickedMap:HashMap<Int,String>,
    private var addedContacts : MutableList<ContactsModel>
)
    : RecyclerView.Adapter<ContactAdapterFromFirebaseFromAddGroupActivity.MyViewHolder>() ,
    Filterable {

    private var list = items


    val contactsListFull = mutableListOf<ContactsModel>()

    init {
        contactsListFull.addAll(list)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(
        holder: MyViewHolder,
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


    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener{
        private val nameTextView : TextView = itemView.findViewById(R.id.user_name_text_view)
        private val numberTextView : TextView = itemView.findViewById(R.id.user_status_text_view)
        private val userImageView : ImageView = itemView.findViewById(R.id.user_image_view)
        private val checkedImageView: ImageView = itemView.findViewById(R.id.contact_checked_image_view)

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

//            if (addedContacts.isNotEmpty()){
//                addGroupBinding.addedParticipantsRecyclerView.visibility = View.VISIBLE
//                addedContactsAdapter = AddedContactsAdapter(addedContacts, addedContacts, contactsClickedMap)
//                addGroupBinding.addedParticipantsRecyclerView.adapter = addedContactsAdapter
//                addParticipantsTextView.text = "${addedContacts.size} of 256 selected"
//
//            }
//            else{
//                addGroupBinding.addedParticipantsRecyclerView.visibility = View.GONE
//                addParticipantsTextView.text = "Add participants"
//            }
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