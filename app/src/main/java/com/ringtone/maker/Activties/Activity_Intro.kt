/*
 * *
 *  * Created by Youssef Assad on 6/2/18 11:22 AM
 *  * Copyright (c) 2018 . All rights reserved.
 *  * Last modified 6/2/18 11:04 AM
 *
 */

package com.ringtone.maker.Activties

import android.Manifest
import android.animation.ArgbEvaluator
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.view.WindowManager
import com.ringtone.maker.R
import com.ringtone.maker.Models.IntroItem
import com.ringtone.maker.Utils.Constants
import com.ringtone.maker.Adapters.Adapter_SlidePager
import kotlinx.android.synthetic.main.activity_intro.*
import com.ringtone.maker.Utils.PermissionManger
import com.ringtone.maker.Utils.SharedPref


class Activity_Intro : AppCompatActivity(), ViewPager.OnPageChangeListener, View.OnClickListener {


    internal val ev = ArgbEvaluator()
    val IntroItems: MutableList<IntroItem> = ArrayList()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val window = window
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)  window.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        val FirstTime = SharedPref(this).LoadBoolean(Constants.FIRST_TIME,true)
        if (FirstTime == false){
            StartMainActivity()
            finish()
        }
        setContentView(R.layout.activity_intro)
        SetupItemsArray()
        val  pagerAdapter = Adapter_SlidePager(supportFragmentManager,IntroItems)
        pager.offscreenPageLimit = IntroItems.size
        pager.adapter = pagerAdapter
        mi_pager_indicator.setViewPager(pager)
        pager.addOnPageChangeListener(this)
        btn_skip.setOnClickListener(this)
        btn_next.setOnClickListener(this)





    }

        override fun onBackPressed() {
            when {
                pager.currentItem === 0 -> super.onBackPressed()
                else -> pager.currentItem = pager.currentItem - 1
            }
    }


    private fun NextButtonController(PagePosition: Int){

        when (PagePosition) {
            IntroItems.size -1 -> btn_next.text = getString(R.string.btn_getstarted)
            else -> btn_next.text = getString(R.string.btn_next)
        }
    }

    private fun SkipButtonController(PagePosition: Int){
        when (PagePosition) {
            IntroItems.size -1 -> btn_skip.visibility = View.INVISIBLE
            else -> btn_skip.visibility = View.VISIBLE
        }

    }


    override fun onClick(button: View?) {
        when (button) {
            btn_skip -> pager.setCurrentItem(IntroItems.size -1,true)
            btn_next -> when {
                btn_next.text == getString(R.string.btn_next) -> pager.setCurrentItem(pager.currentItem + 1 , true)
                else -> PermissionCheck()
            }
        }

    }

     private fun PermissionCheck(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (!PermissionManger.checkPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                PermissionManger.requestPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                return
            }else{
                if (!PermissionManger.checkPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    PermissionManger.requestPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    return
                }else{
                    StartMainActivity()
                }
            }
        }else{
            StartMainActivity()
        }

    }

    private fun StartMainActivity()  =   startActivity(Intent(this, Activity_Main::class.java))


    override fun onPageScrollStateChanged(state: Int) {
    }


    override fun onPageSelected(position: Int) {
        SkipButtonController(position)
         NextButtonController (position)
    }


    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
        if (position < 3 - 1 && position < 3 - 1)   pager.setBackgroundColor(ev.evaluate(positionOffset, IntroItems[position].Color, IntroItems[position + 1].Color) as Int)
        else pager.setBackgroundColor(IntroItems[3 - 1].Color)
    }


    private fun SetupItemsArray() {
        val resources = resources
        val titles = resources.obtainTypedArray(R.array.intro_titles)
        val descs = resources.obtainTypedArray(R.array.intro_descs)
        val imgs = resources.obtainTypedArray(R.array.intro_imgs)
        val colors = resources.obtainTypedArray(R.array.intro_colors)
        for (i in 0..titles.length()-1)  IntroItems.add(IntroItem(colors.getInt(i, 0), titles.getString(i), descs.getString(i), imgs.getResourceId(i, -1)))
        titles.recycle()
        descs.recycle()
        imgs.recycle()
        colors.recycle()
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        for (i in 0..permissions.size-1){
            val permission = permissions[i]
            val grantResult = grantResults[i]
            Log.e("Permission " + permission , "Value" + grantResult);
        }
    }


}
