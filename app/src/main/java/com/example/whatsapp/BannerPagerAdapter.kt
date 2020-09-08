package com.example.whatsapp


import android.content.Context
import android.content.res.TypedArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.viewpager.widget.PagerAdapter


/**
 * Created by ajay singh dewari on 11/5/17.
 */
class BannerPagerAdapter(private val mContext: Context, var bannerArray: TypedArray) :

    PagerAdapter() {

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object`
    }

    override fun getCount(): Int {
        return bannerArray.length()
    }


    override fun instantiateItem(container: ViewGroup, position: Int): Any {
//        return super.instantiateItem(container, position);
        val inflater = LayoutInflater.from(mContext)
        val bannerLayout =
            inflater.inflate(R.layout.banner_fragment_layout, container, false) as ImageView
        //        bannerLayout.setBackgroundResource(bannerArray.getResourceId(position, 0));
        bannerLayout.setImageResource(bannerArray.getResourceId(position, 0))
        bannerLayout.setOnClickListener {
            Toast.makeText(
                mContext,
                "swipe clicked$position",
                Toast.LENGTH_LONG
            ).show()
        }
        container.addView(bannerLayout)
        return bannerLayout
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }

}