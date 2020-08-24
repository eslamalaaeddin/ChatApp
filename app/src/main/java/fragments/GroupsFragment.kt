package fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.whatsapp.R
import com.example.whatsapp.databinding.FragmentGroupsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.fragment_groups.view.*
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "GroupsFragment"
class GroupsFragment : Fragment() {

    private lateinit var fragmentGroupsBinding: FragmentGroupsBinding
    private lateinit var generalView: View
    private lateinit var groupsRecyclerView: RecyclerView
    private lateinit var rootRef: DatabaseReference
    private var groupsAdapter = GroupsAdapter(emptyList())
    private lateinit var database: FirebaseDatabase

    private var groupsList = mutableListOf<String>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)



        database = FirebaseDatabase.getInstance()

        rootRef = database.reference


        retrieveGroups()
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        fragmentGroupsBinding = DataBindingUtil.inflate(inflater , R.layout.fragment_groups , container,false)
        return fragmentGroupsBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        groupsRecyclerView = view.groups_recycler_view
        groupsRecyclerView.layoutManager = LinearLayoutManager(context)

    }

    private fun retrieveGroups() {
        rootRef.child("Groups").addValueEventListener(object :
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                groupsList.clear()
                for (group in snapshot.children) {
                    val currentGroup = group.key.toString()
                    groupsList.add(0, currentGroup)
                }
                groupsAdapter = GroupsAdapter(groupsList)
                groupsRecyclerView.adapter = groupsAdapter
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, error.message, Toast.LENGTH_SHORT).show()
            }
        })
    }


    inner class GroupsAdapter (private var list:List<String>) : RecyclerView.Adapter<GroupsAdapter.GroupsHolder>() {

        inner class GroupsHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener, View.OnLongClickListener {
           private val groupNameTextView : TextView = itemView.findViewById(R.id.group_name_text_view)

            init {
                itemView.setOnClickListener(this)
                itemView.setOnLongClickListener(this)
            }



            fun bind (groupName : String) {
               groupNameTextView.text = groupName
            }

            override fun onClick(item: View?) {

            }

            override fun onLongClick(item: View?): Boolean {
                return true
            }



        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupsHolder {
            val cardView = LayoutInflater.from(parent.context).inflate(R.layout.group_item_layout,parent,false ) as CardView

            return GroupsHolder(cardView)
        }

        override fun getItemCount(): Int {
            return list.size
        }

        override fun onBindViewHolder(holder: GroupsHolder, position: Int) {
            val groupName = list[holder.adapterPosition]
            holder.bind(groupName)
        }
    }


}