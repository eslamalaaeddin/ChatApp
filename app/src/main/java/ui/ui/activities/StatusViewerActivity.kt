package ui.ui.activities

import android.content.res.TypedArray
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.viewpager.widget.ViewPager
import com.example.whatsapp.BannerPagerAdapter
import com.example.whatsapp.R
import com.example.whatsapp.databinding.ActivityStatusViewerBinding
import java.util.*

class StatusViewerActivity : AppCompatActivity() {
    private lateinit var statusViewerBinding: ActivityStatusViewerBinding
    private var mBannerPagerAdapter: BannerPagerAdapter? = null
    private var mBannerArray: TypedArray? = null
    private var numberOfBannerImage = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        statusViewerBinding = DataBindingUtil.setContentView(this, R.layout.activity_status_viewer)


        mBannerArray = resources.obtainTypedArray(R.array.banner_img_array)
        numberOfBannerImage = mBannerArray!!.length()

        mBannerPagerAdapter = BannerPagerAdapter(this, mBannerArray!!)
        statusViewerBinding.viewPager.adapter = mBannerPagerAdapter


        AutoSwipeBanner()
        statusViewerBinding.viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
            override fun onPageSelected(position: Int) {
            }

            override fun onPageScrollStateChanged(state: Int) {}
        })

    }

    private fun AutoSwipeBanner() {
        val handler = Handler()
        val Update = Runnable {
            var currentPage: Int = statusViewerBinding.viewPager.currentItem
            if (currentPage == numberOfBannerImage - 1) {
                currentPage = -1
            }
            statusViewerBinding.viewPager.setCurrentItem(currentPage + 1, true)
        }
        val swipeTimer = Timer()
        swipeTimer.schedule(object : TimerTask() {
            override fun run() {
                handler.post(Update)
            }
        }, 500, 3000)
    }

}