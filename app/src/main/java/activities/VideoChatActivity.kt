package activities

import android.media.MediaPlayer
import android.opengl.GLSurfaceView
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.databinding.DataBindingUtil
import com.example.whatsapp.R
import com.example.whatsapp.Utils.USERS_CHILD
import com.example.whatsapp.databinding.ActivityVideoChatBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.opentok.android.*
import kotlinx.android.synthetic.main.activity_log_in.*
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions
import java.util.jar.Manifest

private const val API_KEY = "46905204"
private const val SESSION_ID = "2_MX40NjkwNTIwNH5-MTU5ODk4OTMzODg2N34vdi9OZkZIcURkTUc2Nk84a2Zhc0xnQUh-fg"
private const val TOKEN = "T1==cGFydG5lcl9pZD00NjkwNTIwNCZzaWc9MDU0M2FlYzMwZjhlN2Y4MjYyODkyM2M1NzM5YmJiMTdmMjU4MWZhMTpzZXNzaW9uX2lkPTJfTVg0ME5qa3dOVEl3Tkg1LU1UVTVPRGs0T1RNek9EZzJOMzR2ZGk5T1prWkljVVJrVFVjMk5rODRhMlpoYzB4blFVaC1mZyZjcmVhdGVfdGltZT0xNTk4OTg5NDM0Jm5vbmNlPTAuMDk0NTIzNDE4ODU0NzE1MDUmcm9sZT1wdWJsaXNoZXImZXhwaXJlX3RpbWU9MTYwMTU4MTQzMiZpbml0aWFsX2xheW91dF9jbGFzc19saXN0PQ=="
private const val TAG = "VideoChatActivity"
private const val VIDEO_APP_PERMISSION = 123


class VideoChatActivity : AppCompatActivity(), Session.SessionListener, PublisherKit.PublisherListener {
    private lateinit var activityVideoChatBinding: ActivityVideoChatBinding
    private val usersRef = FirebaseDatabase.getInstance().reference.child(USERS_CHILD)
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid.toString()

    private lateinit var session:Session
    private var publisher: Publisher? = null
    private var subscriber: Subscriber? = null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityVideoChatBinding = DataBindingUtil.setContentView(this,R.layout.activity_video_chat)

        activityVideoChatBinding.cancelCallButton.setOnClickListener {
            usersRef.addValueEventListener(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.child(currentUserId).hasChild("Ringing")){
                        usersRef.child(currentUserId).child("Ringing").removeValue()

                        if (publisher != null) {
                            publisher?.destroy()
                        }

                        if (subscriber != null) {
                            subscriber?.destroy()
                        }
                    }

                    if (snapshot.child(currentUserId).hasChild("Calling")){
                        usersRef.child(currentUserId).child("Calling").removeValue()
                        if (publisher != null) {
                            publisher?.destroy()
                        }

                        if (subscriber != null) {
                            subscriber?.destroy()
                        }
                    }


                    finish()

                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }
            })
        }

        requestPermission()

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        EasyPermissions.onRequestPermissionsResult(requestCode,permissions,grantResults,this)
    }

    //check results of permissions
    @AfterPermissionGranted(VIDEO_APP_PERMISSION)
    private fun requestPermission () {
        val permissionsToCheck =
            arrayOf(android.Manifest.permission.INTERNET
            ,android.Manifest.permission.CAMERA,
            android.Manifest.permission.RECORD_AUDIO)

        for (perm in permissionsToCheck) {
            //if permission is granted
            if (EasyPermissions.hasPermissions(this, perm)) {
                //1//initialize and connect to session
                session = Session.Builder(this, API_KEY, SESSION_ID).build()
                session.setSessionListener(this)
                session.connect(TOKEN)
            }
            //if not granted
            else{
                EasyPermissions.requestPermissions(this,"This App need Camera, and Mic Permissions",VIDEO_APP_PERMISSION)
            }

        }
    }

    override fun onConnected(session: Session?) {
       //2//Publishing stream to the session
        Log.i(TAG, "onConnected: Session Connected")
        publisher = Publisher.Builder(this).build()
        publisher?.setPublisherListener(this)

        activityVideoChatBinding.publisherView.addView(publisher?.view)


        if (publisher?.view is GLSurfaceView) {
           (publisher?.view as GLSurfaceView).setZOrderOnTop(true)
        }
        session?.publish(publisher)
    }

    override fun onDisconnected(p0: Session?) {
        Log.i(TAG, "onDisconnected: Stream Disconnected")
    }

    override fun onStreamReceived(session: Session?, stream: Stream?) {
        //3// subscribing to the stream
        Log.i(TAG, "onStreamReceived: Stream Received")
        if (subscriber == null) {
            subscriber = Subscriber.Builder(this,stream).build()
            session?.subscribe(subscriber)
            activityVideoChatBinding.subscriberView.addView(subscriber?.view)
        }
    }

    override fun onStreamDropped(session: Session?, stream: Stream?) {
        Log.i(TAG, "onStreamDropped: Session Dropped")
        if (subscriber != null) {
            subscriber = null
            activityVideoChatBinding.subscriberView.removeAllViews()
        }
    }

    override fun onError(p0: Session?, p1: OpentokError?) {
        TODO("Not yet implemented")
    }
//////////////////////////////////////////////////////////////////////////
    override fun onStreamCreated(p0: PublisherKit?, p1: Stream?) {
        TODO("Not yet implemented")
    }

    override fun onStreamDestroyed(p0: PublisherKit?, p1: Stream?) {
        TODO("Not yet implemented")
    }

    override fun onError(p0: PublisherKit?, p1: OpentokError?) {
        TODO("Not yet implemented")
    }
}