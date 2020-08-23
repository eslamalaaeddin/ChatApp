package com.example.whatsapp

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout


class MainActivity : AppCompatActivity() {
    //creating main toolbar
    private lateinit var mainToolbar: Toolbar
    private lateinit var viewPager: ViewPager
    private lateinit var tabLayout:TabLayout
    private lateinit var tabsAdapter: TabsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Setting the toolbar title and its color
        setUpToolbar()

        //Setting the tabs
        setUpTabs()
    }

    private fun setUpToolbar() {
        mainToolbar = findViewById(R.id.main_toolbar)
        setSupportActionBar(mainToolbar)
        mainToolbar.title = "WhatsApp"
        mainToolbar.setTitleTextColor(Color.WHITE)
    }

    private fun setUpTabs() {
        viewPager = findViewById(R.id.main_view_pager)
        tabLayout = findViewById(R.id.main_tab_layout)

        tabsAdapter = TabsAdapter(supportFragmentManager)
        //attach the view pager to the adapter
        viewPager.adapter = tabsAdapter
        //link the ViewPager and TabLayout together so that changes in one are automatically reflected in the other.
        tabLayout.setupWithViewPager(viewPager)
    }
}