package com.example.whatsapp

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.widget.Toolbar


class MainActivity : AppCompatActivity() {
    //creating main toolbar
    private lateinit var mainToolbar: Toolbar
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Setting the toolbar title and its color
        setUpToolbar()
    }

    private fun setUpToolbar() {
        mainToolbar = findViewById(R.id.main_toolbar)
        setSupportActionBar(mainToolbar)
        mainToolbar.title = "WhatsApp"
        mainToolbar.setTitleTextColor(Color.WHITE)
    }
}