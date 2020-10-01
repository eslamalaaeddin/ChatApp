package ui.ui.activities

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.example.whatsapp.BaseApplication
import com.example.whatsapp.R
import com.example.whatsapp.Utils
import com.example.whatsapp.Utils.USERS_CHILD
import com.example.whatsapp.databinding.ActivityStatusBinding
import com.example.whatsapp.databinding.FragmentStatusBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap


class StatusActivity : AppCompatActivity() {
    private lateinit var statusBinding: ActivityStatusBinding

    private lateinit var fragmentStatusBinding : FragmentStatusBinding
    private lateinit var contactsReference: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var currentUser: FirebaseUser
    private lateinit var currentUserId: String
    private lateinit var usersReference: DatabaseReference
    private lateinit var rootReference: DatabaseReference

    private var statusColor:Int = 0
    @SuppressLint("SimpleDateFormat", "ResourceAsColor")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = (application as BaseApplication).getFirebaseAuthenticationReference()
        currentUser = auth.currentUser!!

        rootReference = (application as BaseApplication).getDatabaseRootReference()
        currentUserId = currentUser.uid
        usersReference = FirebaseDatabase.getInstance().reference.child("Users")
        contactsReference = FirebaseDatabase.getInstance().reference.child("Contacts").child(currentUser.uid)

        statusBinding = DataBindingUtil.setContentView(this, R.layout.activity_status)

        statusColor = resources.getColor(R.color.colorPrimary)
        val calender = Calendar.getInstance()
        //get date and time
        val dateFormat = SimpleDateFormat("MMM dd, yyyy")
        val timeFormat = SimpleDateFormat("hh:mm a")

        statusBinding.paletteImageView.setOnClickListener {
            val random = (0..7).random()
            statusBinding.statusEditText.setBackgroundColor(resources.getColor(Utils.COLORS[random]))
            statusColor = resources.getColor(Utils.COLORS[random])
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val window: Window = window
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                window.statusBarColor = resources.getColor(Utils.COLORS[random])
            }
        }

        //handling the visibility of fab
        statusBinding.statusEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun afterTextChanged(text: Editable?) {
                if (text.toString().isEmpty()){
                    statusBinding.fab.visibility = View.INVISIBLE
                }else{
                    statusBinding.fab.visibility = View.VISIBLE
                }
            }
        })

        //adding status logic
        statusBinding.fab.setOnClickListener {
          val statusKey =  rootReference.child(USERS_CHILD).child("Status").push().key.toString()


           val currentDate = dateFormat.format(calender.time)
           val currentTime = timeFormat.format(calender.time)

            val statusText  = statusBinding.statusEditText.editableText.toString()

            val statusMap = HashMap<String, Any>()
            statusMap["by"] = currentUserId
            statusMap["text"] = statusText
            statusMap["color"] = statusColor.toString()
            statusMap["viewersid"] = "01021823908"
            statusMap["viewscount"] = "15"
            statusMap["statusid"] = statusKey
            statusMap["date"] = currentDate
            statusMap["time"] = currentTime
           // statusMap["timestamp"] = ServerValue.TIMESTAMP

            rootReference.child(USERS_CHILD).child(currentUserId).child("Status").child(statusKey).
            updateChildren(statusMap).addOnCompleteListener {
                Toast.makeText(this, "Status Added", Toast.LENGTH_SHORT).show()
            }



        }

    }
}