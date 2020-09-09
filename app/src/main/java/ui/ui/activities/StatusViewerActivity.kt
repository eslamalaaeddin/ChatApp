package ui.ui.activities

import android.annotation.SuppressLint
import android.content.res.TypedArray
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.MotionEvent
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.viewpager.widget.ViewPager
import com.example.whatsapp.BannerPagerAdapter
import com.example.whatsapp.R
import com.example.whatsapp.Utils
import com.example.whatsapp.databinding.ActivityStatusViewerBinding
import com.example.whatsapp.databinding.FragmentStatusBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import models.StatusModel
import java.util.*

class StatusViewerActivity : AppCompatActivity() {
    private lateinit var statusViewerBinding: ActivityStatusViewerBinding
    private lateinit var contactsReference: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var currentUser: FirebaseUser
    private lateinit var currentUserId: String
    private lateinit var usersReference: DatabaseReference
    private lateinit var rootReference: DatabaseReference
    private var statusList = mutableListOf<StatusModel>()
    private var bannerPagerAdapter: BannerPagerAdapter? = null

    private lateinit var handler : Handler
    private lateinit var updateRunnable :Runnable

    //private lateinit var bannerArray: TypedArray
    private var numberOfBannerImage = 0
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN)

        statusViewerBinding = DataBindingUtil.setContentView(this, R.layout.activity_status_viewer)

        auth = FirebaseAuth.getInstance()
        currentUser = auth.currentUser!!

        rootReference = FirebaseDatabase.getInstance().reference
        currentUserId = currentUser.uid
        usersReference = FirebaseDatabase.getInstance().reference.child("Users")
        contactsReference = FirebaseDatabase.getInstance().reference.child("Contacts").child(currentUser.uid)

        retrieveStatuses()

        statusViewerBinding.viewPager.setOnTouchListener { p0, motionEvent ->
            if (motionEvent?.action == MotionEvent.ACTION_DOWN) {
                Toast.makeText(this, "Stop Swiping", Toast.LENGTH_SHORT).show()

            } else if (motionEvent?.action == MotionEvent.ACTION_UP) {
                Toast.makeText(this, "Resume Swiping", Toast.LENGTH_SHORT).show()

            }
            true
        }


    }


    private fun autoSwipeBanner() {
         handler = Handler()
         updateRunnable = Runnable {
            var currentPage: Int = statusViewerBinding.viewPager.currentItem
            if (currentPage == numberOfBannerImage - 1) {
                currentPage = -1
            }
            statusViewerBinding.viewPager.setCurrentItem(currentPage + 1, true)
        }
        val swipeTimer = Timer()
        swipeTimer.schedule(object : TimerTask() {
            override fun run() {
                handler.post(updateRunnable)
            }
        }, 1500, 1500)
    }

    private fun retrieveStatuses() {
        rootReference.child(Utils.USERS_CHILD).child(currentUserId).child("Status").addValueEventListener(
            object :
                ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    statusList.clear()
                    for (status in snapshot.children) {
                        val by = status.child("by").value.toString()
                        val text = status.child("text").value.toString()
                        val color = status.child("color").value.toString()
                        val viewersId = status.child("viewersid").value.toString()
                        val viewsCount = status.child("viewscount").value.toString()
                        val statusId = status.child("statusid").value.toString()
                        val date = status.child("date").value.toString()
                        val time = status.child("time").value.toString()

                        val currentStatus = StatusModel(
                            by,
                            text,
                            color,
                            viewersId,
                            viewsCount,
                            statusId,
                            date,
                            time
                        )

                        statusList.add( currentStatus)
                    }


                    //  bannerArray = resources.obtainTypedArray(R.array.banner_img_array)
                    numberOfBannerImage = statusList.size

                    bannerPagerAdapter = BannerPagerAdapter(this@StatusViewerActivity, statusList)
                    statusViewerBinding.viewPager.adapter = bannerPagerAdapter




                    autoSwipeBanner()
                    statusViewerBinding.viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
                        @SuppressLint("ResourceType")
                        override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                        }
                        override fun onPageSelected(position: Int) {
                        }

                        override fun onPageScrollStateChanged(state: Int) {}
                    })
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@StatusViewerActivity, error.message, Toast.LENGTH_SHORT).show()
                }
            })
    }

}