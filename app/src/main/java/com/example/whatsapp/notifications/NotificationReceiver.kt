package com.example.whatsapp.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat
private const val DISMISS_ID = "dismiss id"
private const val TAG = "NotificationReceiver"
class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.i(TAG, "received result: $resultCode")
//        if (resultCode != RESULT_OK) {
//            // A foreground activity canceled the broadcast
//            return
//        }

        val notId = intent?.getIntExtra(DISMISS_ID,-1)
        if (notId!= null ) {
            val notificationManager = NotificationManagerCompat.from(context!!)
            notificationManager.cancel(notId)
        }

//        val requestCode = intent?.getIntExtra(PrivateChatActivity.REQUEST_CODE, 0)
//        val notification: Notification =
//            intent?.getParcelableExtra(PrivateChatActivity.NOTIFICATION)!!
//        val notificationManager = NotificationManagerCompat.from(context!!)
//        notificationManager.notify(requestCode!!, notification)
    }

}