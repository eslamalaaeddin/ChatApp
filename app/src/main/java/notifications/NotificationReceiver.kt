package notifications

import activities.PrivateChatActivity
import android.app.Activity.RESULT_OK
import android.app.Notification
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat

private const val TAG = "NotificationReceiver"
class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.i(TAG, "received result: $resultCode")
        if (resultCode != RESULT_OK) {
            // A foreground activity canceled the broadcast
            return
        }

        val requestCode = intent?.getIntExtra(PrivateChatActivity.REQUEST_CODE, 0)
        val notification: Notification =
            intent?.getParcelableExtra(PrivateChatActivity.NOTIFICATION)!!
        val notificationManager = NotificationManagerCompat.from(context!!)
        notificationManager.notify(requestCode!!, notification)
    }

}