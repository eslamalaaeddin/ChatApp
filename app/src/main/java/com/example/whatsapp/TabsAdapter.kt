package com.example.whatsapp

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import ui.ui.fragments.CallsFragment
import ui.ui.fragments.ChatsFragment
import ui.ui.fragments.StatusFragment

class TabsAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {



    override fun getItem(position: Int): Fragment {
        when(position) {
            0 -> return ChatsFragment()
            1 -> return StatusFragment()
            else -> return CallsFragment()
        }

    }

    override fun getCount(): Int = 3

    override fun getPageTitle(position: Int): CharSequence? {
        when(position) {
            0 -> return "CHATS"
            1 -> return "STATUS"
            else-> return "CALLS"
        }
    }
}