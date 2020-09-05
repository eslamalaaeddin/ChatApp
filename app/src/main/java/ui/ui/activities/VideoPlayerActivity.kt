package ui.ui.activities

import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import android.widget.MediaController
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.example.whatsapp.R
import com.example.whatsapp.databinding.ActivityVideoPlayerBinding

private const val VIDEO_URL = "video url"
class VideoPlayerActivity : AppCompatActivity() {
    private lateinit var activityVideoPlayerBinding: ActivityVideoPlayerBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //hide status bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN)

        activityVideoPlayerBinding = DataBindingUtil.setContentView(
            this,
            R.layout.activity_video_player
        )

        val videoUrl = intent.getStringExtra(VIDEO_URL)
        activityVideoPlayerBinding.videoView.setVideoPath(videoUrl)
        activityVideoPlayerBinding.videoView.start()

        val mediaController = MediaController(this)
        activityVideoPlayerBinding.videoView.setMediaController(mediaController)
        mediaController.setAnchorView(activityVideoPlayerBinding.videoView)

    }
}