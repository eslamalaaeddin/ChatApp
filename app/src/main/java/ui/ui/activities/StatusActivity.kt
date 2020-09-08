package ui.ui.activities

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.example.whatsapp.R
import com.example.whatsapp.Utils
import com.example.whatsapp.databinding.ActivityStatusBinding


class StatusActivity : AppCompatActivity() {
    private lateinit var statusBinding: ActivityStatusBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        statusBinding = DataBindingUtil.setContentView(this, R.layout.activity_status)

        statusBinding.paletteImageView.setOnClickListener {
            val random = (0..7).random()
            statusBinding.statusEditText.setBackgroundColor(resources.getColor(Utils.COLORS[random]))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val window: Window = window
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                window.statusBarColor = resources.getColor(Utils.COLORS[random])
            }
        }

    }
}