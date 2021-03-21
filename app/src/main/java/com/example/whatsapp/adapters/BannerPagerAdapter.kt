package com.example.whatsapp.adapters


import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.*
import android.widget.TextView
import androidx.viewpager.widget.PagerAdapter
import com.example.whatsapp.R
import kotlinx.android.synthetic.main.banner_fragment_layout.view.*
import com.example.whatsapp.models.StatusModel

class BannerPagerAdapter(private val context: Context, var bannerArray: MutableList<StatusModel>) : PagerAdapter() {

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object`
    }

    override fun getCount(): Int {
        return bannerArray.size
    }


    @SuppressLint("UseCompatLoadingForDrawables")
    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val inflater = LayoutInflater.from(context)
        val bannerLayout =
            inflater.inflate(R.layout.banner_fragment_layout, container, false) as TextView
        bannerLayout.text_view.text = bannerArray[position].text

        val color = Color.parseColor("#${Integer.toHexString(bannerArray[position].color.toInt())}")


        bannerLayout.text_view.text = bannerArray[position].text
        bannerLayout.text_view.setBackgroundColor(color)

        container.addView(bannerLayout)
        return bannerLayout
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }

}