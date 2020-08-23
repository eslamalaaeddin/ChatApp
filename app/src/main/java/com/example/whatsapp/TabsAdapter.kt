package com.example.whatsapp

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import fragments.CallsFragment
import fragments.ChatsFragment
import fragments.StatusFragment

class TabsAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {



    override fun getItem(position: Int): Fragment {
        when(position) {
            0 -> return ChatsFragment()
            1 -> return StatusFragment()
            2 -> return CallsFragment()

            else -> return Fragment()
        }

    }

    override fun getCount(): Int = 3

    override fun getPageTitle(position: Int): CharSequence? {
        when(position) {
            0 -> return "CHATS"
            1 -> return "STATUS"
            2 -> return "CALLS"

            else -> return null
        }
    }
}