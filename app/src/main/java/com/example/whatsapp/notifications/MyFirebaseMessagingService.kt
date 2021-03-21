package com.example.whatsapp.notifications

import com.example.whatsapp.ui.ui.activities.PrivateChatActivity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.example.whatsapp.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.example.whatsapp.ui.activities.VideoChatActivity
import kotlin.random.Random

private const val USER_ID = "user id"
private const val CHANNEL_ID = "my_channel"
private const val TAG = "MyFirebaseMessagingServ"
private const val DISMISS_ID = "dismiss id"

class MyFirebaseMessagingService : FirebaseMessagingService() {


    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)



        //video notification
        if (message.data["message"] == "I Want to make a video chat with you") {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notificationID = Random.nextInt()


            //accept inent
            val acceptVideoChatIntent = Intent(this, VideoChatActivity::class.java)
            acceptVideoChatIntent.putExtra(USER_ID,message.data["uid"])

            //dismiss intent
            val dismissVideoChatIntent = Intent(this, NotificationReceiver::class.java)
            dismissVideoChatIntent.apply {
                putExtra(DISMISS_ID,notificationID)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }



            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createNotificationChannel(notificationManager)
            }

            acceptVideoChatIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            dismissVideoChatIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            val acceptPendingIntent =
                PendingIntent.getActivity(this, 0, acceptVideoChatIntent, PendingIntent.FLAG_UPDATE_CURRENT)

            val dismissPendingIntent =
                PendingIntent.getBroadcast(this, 0, dismissVideoChatIntent, PendingIntent.FLAG_UPDATE_CURRENT)

            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(message.data["title"])
                .setContentText(message.data["message"])
                .setSmallIcon(R.drawable.whatsapp)
                .setAutoCancel(true)
                .setContentIntent(acceptPendingIntent)
                .setContentIntent(dismissPendingIntent)
                .addAction(R.drawable.tick,"Accept",acceptPendingIntent)
                .addAction(R.drawable.cross,"Dismiss",dismissPendingIntent)
                .build()

            notificationManager.notify(notificationID, notification)
        }
        //text Message notification
        else if (message.data["messageType"] == "audio" ||
            message.data["messageType"] == "video" ||
            message.data["messageType"] == "image" ||message.data["messageType"] == "captured image"){
            val intent = Intent(this, PrivateChatActivity::class.java)
            intent.putExtra(USER_ID,message.data["uid"])
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notificationID = Random.nextInt()

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createNotificationChannel(notificationManager)
            }

            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(message.data["messageType"])
                .setContentText(message.data["messageKey"])
                .setSmallIcon(R.drawable.whatsapp)
                .setAutoCancel(true)
                .setSound(Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE+ "://" +this.packageName +"/"+R.raw.new_message_sound))
                .setContentIntent(pendingIntent)
                .build()

            notificationManager.notify(notificationID, notification)
        }
        //text message
        else{
            val intent = Intent(this, PrivateChatActivity::class.java)
            intent.putExtra(USER_ID,message.data["uid"])
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notificationID = Random.nextInt()

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createNotificationChannel(notificationManager)
            }

            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(message.data["title"])
                .setContentText(message.data["message"])
                .setSmallIcon(R.drawable.whatsapp)
                .setAutoCancel(true)
                .setSound(Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE+ "://" +this.packageName +"/"+R.raw.new_message_sound))
                .setContentIntent(pendingIntent)
                .build()

            notificationManager.notify(notificationID, notification)
        }



//        showBackgroundNotification(0,notification)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(notificationManager: NotificationManager) {
        val channelName = "channelName"
        val channel = NotificationChannel(CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_HIGH).apply {
            description = "My channel description"
            enableLights(true)
            lightColor = Color.GREEN
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun showBackgroundNotification(
        requestCode: Int,
        notification: Notification
    ) {
        val intent = Intent(PrivateChatActivity.ACTION_SHOW_NOTIFICATION).apply {
            putExtra(PrivateChatActivity.REQUEST_CODE, requestCode)
            putExtra(PrivateChatActivity.NOTIFICATION, notification)
        }
        sendOrderedBroadcast(intent, PrivateChatActivity.PERM_PRIVATE)
    }

}