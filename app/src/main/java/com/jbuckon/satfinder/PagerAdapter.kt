package com.jbuckon.satfinder;

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter;

class PagerAdapter(fragmentManager: FragmentManager, private val pages: ArrayList<Fragment>): FragmentStatePagerAdapter(fragmentManager) {
    override fun getCount(): Int {
        return pages.count()
    }

    override fun getItem(p0: Int): Fragment {
        return pages[p0]
    }

}
