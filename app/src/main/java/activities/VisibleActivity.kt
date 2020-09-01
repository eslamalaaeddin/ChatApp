package activities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContentProviderCompat.requireContext

private const val TAG = "VisibleActivity"
open class VisibleActivity : AppCompatActivity() {

    private val onShowNotification = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // If we receive this, we're visible, so cancel
            // the notification
            Log.i(TAG, "canceling notification")
            resultCode = RESULT_CANCELED
        }

    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(PrivateChatActivity.ACTION_SHOW_NOTIFICATION)
        registerReceiver(
            onShowNotification,
            filter,
            PrivateChatActivity.PERM_PRIVATE,
            null
        )
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(onShowNotification)
    }

}
