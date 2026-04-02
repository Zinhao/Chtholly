package com.zinhao.chtholly.view.adapter


import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.zinhao.chtholly.view.SetupServerFragment
import com.zinhao.chtholly.view.SetupSoulFragment
import com.zinhao.chtholly.view.SetupSuccessFragment
import com.zinhao.chtholly.view.SetupTtsFragment

class SetupPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 4

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> SetupServerFragment()
            1 -> SetupSoulFragment()
            2 -> SetupTtsFragment()
            3 -> SetupSuccessFragment()
            else -> throw IllegalArgumentException("Invalid position $position")
        }
    }
}