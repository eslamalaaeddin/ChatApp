package ui.ui.activities

import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.databinding.DataBindingUtil
import com.example.whatsapp.Callback
import com.example.whatsapp.R
import com.example.whatsapp.TabsAdapter
import com.example.whatsapp.Utils
import com.example.whatsapp.Utils.STATE_CHILD
import com.example.whatsapp.Utils.USERS_CHILD
import com.example.whatsapp.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.iid.FirebaseInstanceId
import kotlinx.android.synthetic.main.create_group_dialog.view.*
import models.GroupModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

private const val USER_ID = "user id"
private const val USER_NAME = "user name"
private const val USER_IMAGE = "user image"
private const val GROUP_ID = "group id"
private const val PHONE_NUMBER = "phone number"
private const val TAG = "MainActivity"
private const val STATUS_IDENTIFIER = "STATUS_IDENTIFIER"
class MainActivity : AppCompatActivity() , Callback {

    private lateinit var tabsAdapter: TabsAdapter

    private lateinit var activityMainBinding : ActivityMainBinding

    //firebase authentication instance
    private lateinit var auth: FirebaseAuth

    //Database reference
    private lateinit var rootRef: DatabaseReference

    private  var currentUser:FirebaseUser? = null

    private lateinit var currentUserId : String

    private lateinit var usersRef :DatabaseReference


    private lateinit var alertBuilder: AlertDialog.Builder
    private lateinit var groupDialog: AlertDialog

    private var privateChatIntent : Intent? = null

    private lateinit var phoneNumber:String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activityMainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        phoneNumber = intent.getStringExtra(PHONE_NUMBER).toString()

        //Setting the toolbar title and its color
        setUpToolbar()

        //Setting the tabs
        setUpTabs()

        //get firebase auth
        auth = FirebaseAuth.getInstance()

        usersRef = FirebaseDatabase.getInstance().reference.child(USERS_CHILD)

        //get current user
        currentUser = auth.currentUser

         currentUserId = currentUser?.uid.toString()

        rootRef = FirebaseDatabase.getInstance().reference
        Log.i(TAG, "onCreate: MAIN")

    }

    override fun onStart() {
        super.onStart()
        Log.i(TAG, "onStart: MAIN")
       // activityMainBinding.mainViewPager.currentItem = 0
        if(currentUser == null) {
            sendUserToPhoneLogInActivity()
        }

        else{
            verifyUserExistence()

            updateUserStatus("online")
        }
    }

    override fun onStop() {
        super.onStop()
        if(privateChatIntent == null) {
            updateUserStatus("offline")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if(currentUser != null) {
            updateUserStatus("offline")
        }
    }


    private fun sendUserToPhoneLogInActivity() {
        val loginIntent = Intent(this , PhoneLogInActivity::class.java)
        loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(loginIntent)
        finish()
    }

    private fun sendUserToFindFriendsActivity() {
        val findFriendsIntent = Intent(this , FindFriendsActivity::class.java)
        startActivity(findFriendsIntent)
    }

    private fun sendUserToSettingsActivity() {
        val settingIntent = Intent(this , SettingsActivity::class.java)
        settingIntent.putExtra(PHONE_NUMBER,phoneNumber)
        startActivity(settingIntent)

    }

    private fun verifyUserExistence() {
        val currentUserId = currentUser?.uid.toString()
        val deviceToken = FirebaseInstanceId.getInstance().token
        usersRef.child(currentUserId).child(Utils.DEVICE_TOKEN_CHILD).setValue(deviceToken)

        rootRef.child("Users").child(currentUserId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                //if user name does exist
                if (snapshot.child("name").exists()) {

                }

                else{
                    sendUserToSettingsActivity()
                    //Snackbar.make(activityMainBinding.mainToolbar,"HI",Snackbar.LENGTH_INDEFINITE).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    private fun setUpToolbar() {
        setSupportActionBar(activityMainBinding.mainToolbar)
        activityMainBinding.mainToolbar.title = "WhatsApp"
        activityMainBinding.mainToolbar.setTitleTextColor(Color.WHITE)
        activityMainBinding.mainToolbar.overflowIcon?.setColorFilter(resources.getColor(R.color.white), PorterDuff.Mode.SRC_IN)
    }

    private fun setUpTabs() {
        tabsAdapter = TabsAdapter(supportFragmentManager)
        //attach the view pager to the adapter
        activityMainBinding.mainViewPager.adapter = tabsAdapter
        //link the ViewPager and TabLayout together so that changes in one are automatically reflected in the other.
        activityMainBinding.mainTabLayout.setupWithViewPager(activityMainBinding.mainViewPager)

        activityMainBinding.mainTabLayout.getTabAt(3)?.setIcon(R.drawable.ic_camera_tab)

    }


    private fun showNewGroupDialog(){
        alertBuilder = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.create_group_dialog,null)
        alertBuilder.setView(dialogView)

        groupDialog =  alertBuilder.create()
        groupDialog.show()

        dialogView.create_button.setOnClickListener {
            val groupName = dialogView.group_name_edit_text.editableText.toString()

            if (groupName.isNotEmpty()) {
                //Should be take from user input
                createNewGroup(GroupModel(groupName,"","Hello I am $groupName","","",""))
                groupDialog.dismiss()
            }

            else{
                Toast.makeText(this, "Enter a valid group name", Toast.LENGTH_SHORT).show()
            }
        }

        dialogView.cancel_button.setOnClickListener {
            groupDialog.dismiss()
        }



    }

    private fun createNewGroup(group:GroupModel){

        val gid = UUID.randomUUID().toString()
        val groupMap = HashMap<String, Any>()
        groupMap.put("name", group.name)
        groupMap.put("status", group.status)
        groupMap.put("image", group.image)
        groupMap.put("gid", gid)

        rootRef.child("Users").child("Groups").child(gid).updateChildren(groupMap)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "${group.name} created successfully", Toast.LENGTH_SHORT).show()
                }

                else{
                    Toast.makeText(this, task.exception?.message, Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)

        val privateChatSearchItem = menu?.findItem(R.id.action_search_private)
        val groupsChatSearchItem = menu?.findItem(R.id.action_search_group)

        val privateChatSearchView = privateChatSearchItem?.actionView as SearchView
        val groupsChatSearchView = groupsChatSearchItem?.actionView as SearchView

        privateChatSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
               return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                Utils.privateChatsAdapter?.filter?.filter(newText)
                return true
            }
        })

        groupsChatSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                Utils.groupsChatAdapter?.filter?.filter(newText)
                return true
            }
        })


        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {

                R.id.find_friends_item -> sendUserToFindFriendsActivity()
//                R.id.create_new_group -> showNewGroupDialog()
//                R.id.create_new_group -> showNewGroupDialog()
                R.id.create_new_group -> startActivity(Intent(this,AddGroupActivity::class.java))
                R.id.settings_item -> sendUserToSettingsActivity()
                R.id.log_out_item -> {
                    auth.signOut()
                    sendUserToPhoneLogInActivity()
                }
        }
        return super.onOptionsItemSelected(item)
    }



    private fun sendUserToGroupChatActivity(groupId: String) {
        val groupChatIntent = Intent(this , GroupsChatActivity::class.java)
        groupChatIntent.putExtra(GROUP_ID,groupId)
        startActivity(groupChatIntent)
    }

    override fun onGroupClicked(groupId: String) {
        sendUserToGroupChatActivity(groupId)
    }

    override fun onUserChatClicked(userName: String, userId: String, userImage:String) {
         privateChatIntent = Intent(this, PrivateChatActivity::class.java)
        privateChatIntent?.putExtra(USER_NAME,userName)
        privateChatIntent?.putExtra(USER_ID,userId)
        privateChatIntent?.putExtra(USER_IMAGE,userImage)
        startActivity(privateChatIntent)


    }

    override fun onStatusClicked(id: String) {
        val userStatusIntent = Intent(this,StatusViewerActivity::class.java)
        userStatusIntent.putExtra(STATUS_IDENTIFIER,id)
        startActivity(userStatusIntent)
    }

    override fun onCallClicked(callerId: String) {
        privateChatIntent = Intent(this, PrivateChatActivity::class.java)
        privateChatIntent?.putExtra(USER_ID,callerId)
        startActivity(privateChatIntent)
    }

    private fun updateUserStatus (state :String) {
        var currentDate = ""
        var currentTime = ""

        val calender = Calendar.getInstance()
        //get date and time
        val dateFormat = SimpleDateFormat("MMM dd, yyyy")
        val timeFormat = SimpleDateFormat("hh:mm a")

        currentDate = dateFormat.format(calender.time)
        currentTime = timeFormat.format(calender.time)

        val userStateMap = HashMap<String,Any> ()
        userStateMap.put("date",currentDate)
        userStateMap.put("time",currentTime)
        userStateMap.put("state",state)


        rootRef.child(USERS_CHILD).child(currentUserId).child(STATE_CHILD).updateChildren(userStateMap)

    }
}