package com.example.whatsapp


import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Build
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.viewpager.widget.PagerAdapter
import kotlinx.android.synthetic.main.banner_fragment_layout.view.*
import models.StatusModel

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