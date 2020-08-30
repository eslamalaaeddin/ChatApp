package activities

import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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
import fragments.GroupsFragment
import kotlinx.android.synthetic.main.create_group_dialog.view.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

private const val USER_ID = "user id"
private const val USER_NAME = "user name"
private const val USER_IMAGE = "user image"
private const val GROUP_NAME = "group name"
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



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activityMainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)

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


    }

    override fun onStart() {
        super.onStart()
        if(currentUser == null) {
            sendUserToLogInActivity()
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
    

    private fun sendUserToLogInActivity() {
        val loginIntent = Intent(this , LogInActivity::class.java)
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
        startActivity(settingIntent)

    }

    private fun verifyUserExistence() {
        val currentUserId = currentUser?.uid.toString()
        val deviceToken = FirebaseInstanceId.getInstance().getToken()
        usersRef.child(currentUserId).child(Utils.DEVICE_TOKEN_CHILD).setValue(deviceToken)

        rootRef.child("Users").child(currentUserId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                //if user name does exist
                if (snapshot.child("name").exists()) {

                }

                else{
                    sendUserToSettingsActivity()
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
                createNewGroup(groupName)
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

    private fun createNewGroup(groupName:String){
        rootRef.child("Groups").child(groupName).setValue("")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "$groupName created successfully", Toast.LENGTH_SHORT).show()
                }

                else{
                    Toast.makeText(this, task.exception?.message, Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {

                R.id.find_friends_item -> sendUserToFindFriendsActivity()
                R.id.create_new_group -> showNewGroupDialog()
                R.id.settings_item -> sendUserToSettingsActivity()
                R.id.log_out_item -> {
                    auth.signOut()
                    sendUserToLogInActivity()
                }
        }
        return super.onOptionsItemSelected(item)
    }



    private fun sendUserToGroupChatActivity(groupName: String) {
        val groupChatIntent = Intent(this , GroupChatActivity::class.java)
        groupChatIntent.putExtra(GROUP_NAME,groupName)
        startActivity(groupChatIntent)
    }

    override fun onGroupClicked(groupName: String) {
        sendUserToGroupChatActivity(groupName)
    }

    override fun onUserChatClicked(userName: String, userId: String, userImage:String) {
         privateChatIntent = Intent(this, PrivateChatActivity::class.java)
        privateChatIntent?.putExtra(USER_NAME,userName)
        privateChatIntent?.putExtra(USER_ID,userId)
        privateChatIntent?.putExtra(USER_IMAGE,userImage)
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