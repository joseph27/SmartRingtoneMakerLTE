/*
 * *
 *  * Created by Youssef Assad on 6/2/18 11:22 AM
 *  * Copyright (c) 2018 . All rights reserved.
 *  * Last modified 6/2/18 11:04 AM
 *
 */

package com.ringtone.maker.Adapters

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import com.ringtone.maker.Models.IntroItem
import com.ringtone.maker.Fragments.Fragment_Intro


class Adapter_SlidePager(fm: FragmentManager, private var introItems: MutableList<IntroItem>) : FragmentStatePagerAdapter(fm) {

    override fun getItem(position: Int): Fragment? {
        return Fragment_Intro.newInstance(introItems.get(position))
    }

    override fun getCount(): Int {
        return introItems.size
    }
}
