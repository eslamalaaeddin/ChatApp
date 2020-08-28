package activities

import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.whatsapp.ContactsModel
import com.example.whatsapp.R
import com.example.whatsapp.databinding.ActivityFindFriendsBinding
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.squareup.picasso.Picasso

private const val TAG = "FindFriendsActivity"
private const val USER_ID = "user id"
class FindFriendsActivity : AppCompatActivity() {
    private lateinit var activityFindFriendsBinding: ActivityFindFriendsBinding

    private lateinit var usersReference: DatabaseReference

    //firebase authentication instance
    private lateinit var auth: FirebaseAuth

    private lateinit var currentUser: FirebaseUser

    private lateinit var contactsAdapter: FirebaseRecyclerAdapter<ContactsModel, ContactsViewHolder>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activityFindFriendsBinding = DataBindingUtil.setContentView(this,R.layout.activity_find_friends)
        auth = FirebaseAuth.getInstance()
        currentUser = auth.currentUser!!
        usersReference = FirebaseDatabase.getInstance().reference.child("Users")
        setUpToolbar()

    }

    override fun onStart() {
        super.onStart()

        val options  =
            FirebaseRecyclerOptions.Builder<ContactsModel>().setQuery(usersReference,ContactsModel::class.java).build()

         contactsAdapter = object :
            FirebaseRecyclerAdapter<ContactsModel, ContactsViewHolder>(options) {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactsViewHolder {
                val cardView =
                    LayoutInflater.from(parent.context).inflate(R.layout.user_item_layout
                        , parent
                        , false)

                return ContactsViewHolder(cardView)
            }

            override fun onBindViewHolder(holder: ContactsViewHolder, position: Int, model: ContactsModel) {

                    holder.bind(model)


                Log.i(TAG, "onBindViewHolder: $model")
            }

        }


        //attaching recyclerView to the adapter
        activityFindFriendsBinding.findFriendsRecyclerView.adapter = contactsAdapter
        activityFindFriendsBinding.findFriendsRecyclerView.layoutManager = LinearLayoutManager(this)

        contactsAdapter.startListening()

    }

   inner class ContactsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
            private val userNameTextView :TextView = itemView.findViewById(R.id.user_name_text_view)
            private val userStatusTextView :TextView = itemView.findViewById(R.id.user_status_text_view)
            private val userImageView :ImageView = itemView.findViewById(R.id.user_image_view)



        fun bind(contactModel: ContactsModel) {

            userNameTextView.text = contactModel.name
            userStatusTextView.text = contactModel.status

            if (contactModel.image.isNotEmpty()) {
                Picasso.get()
                    .load(contactModel.image)
                    .placeholder(R.drawable.dummy_avatar)
                    .into(userImageView)
            }
        }

       init {
                itemView.setOnClickListener(this)
       }

       override fun onClick(itemView: View?) {
           val userId = contactsAdapter.getItem(adapterPosition).uid
           val profileIntent = Intent(this@FindFriendsActivity, ProfileActivity::class.java)
           profileIntent.putExtra(USER_ID,userId)
           startActivity(profileIntent)
       }
   }

    override fun onStop() {
        super.onStop()
        contactsAdapter.stopListening()
    }

    private fun setUpToolbar() {
        setSupportActionBar(activityFindFriendsBinding.mainToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Find Friends"
        activityFindFriendsBinding.mainToolbar.setTitleTextColor(Color.WHITE)
        activityFindFriendsBinding.mainToolbar.overflowIcon?.setColorFilter(resources.getColor(R.color.white), PorterDuff.Mode.SRC_IN)
        activityFindFriendsBinding.mainToolbar.navigationIcon?.setColorFilter(resources.getColor(R.color.white), PorterDuff.Mode.SRC_IN)
    }
}