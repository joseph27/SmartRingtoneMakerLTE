/*
 * *
 *  * Created by Youssef Assad on 6/2/18 11:22 AM
 *  * Copyright (c) 2018 . All rights reserved.
 *  * Last modified 6/2/18 11:04 AM
 *
 */

package com.ringtone.maker.Fragments

import android.os.Bundle
import android.support.graphics.drawable.AnimatedVectorDrawableCompat
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.PopupMenu
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import com.ringtone.maker.R
import com.ringtone.maker.ViewHolders.Viewholder_SelectionItem
import com.ringtone.maker.Models.MusicFile
import kotlinx.android.synthetic.main.fragment_selection.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import com.ringtone.maker.Adapters.Adapter_Selection
import com.ringtone.maker.Utils.SharedPref
import java.util.*


/**
 * Created by Joseph27 on 5/24/18.
 */

class Fragment_Selection : Fragment(), PopupMenu.OnMenuItemClickListener {


    val History = "History"

    companion object {
        fun newInstance(history: Boolean?) = Fragment_Selection().apply {
            arguments = Bundle(1).apply {
                if (history != null) putBoolean(History, history)
            }
        }
    }


    private var mItems: ArrayList<MusicFile> = ArrayList()
    private var adapterSelection: Adapter_Selection? = null
    private var TotalMusicSize: Int = 0
    private var dbHelper: com.ringtone.maker.Database.DBHelper? = null
    private var mSharedPref: SharedPref? = null
    private var IsitHistory:Boolean? = null;

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? = inflater!!.inflate(R.layout.fragment_selection, container, false)


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        recycler_view.layoutManager = LinearLayoutManager(activity)
        mItems = ArrayList()
        adapterSelection = Adapter_Selection()
        recycler_view.adapter = adapterSelection
        dbHelper = com.ringtone.maker.Database.DBHelper.getInstance(activity)
        IsitHistory = arguments.getBoolean(History, false)
        if (IsitHistory == true) mItems = dbHelper!!.historyRecords
        else GetMusicDataFromDB()
        adapterSelection!!.updateData(mItems, activity)
        TotalMusicSize = mItems.size

        if (TotalMusicSize == 0) ShowEmptyStatus() else history_empty_state_Container.visibility = View.GONE





    }



    private fun ShowEmptyStatus() {

        history_empty_state_Container.visibility = View.VISIBLE
        history_empty_state.setImageDrawable(null)
        history_empty_state.setBackgroundDrawable(null)

        if (activity.isFinishing) {
            return
        }
        var drawable:AnimatedVectorDrawableCompat? = null;
        if (IsitHistory == true ){
             drawable = AnimatedVectorDrawableCompat.create(activity, R.drawable.history_empty_state_animation)
             no_data_text1.setText(activity.getString(R.string.your_records_are_clean))
             no_data_text2.setText(activity.getString(R.string.no_history_yet_download_contents_and_it_will_show_up_here))
        }else{
              drawable = AnimatedVectorDrawableCompat.create(activity, R.drawable.music_empty_state_animation)
            no_data_text1.setText(activity.getString(R.string.nothing_here_only_cool_ghosts))
        }



        history_empty_state.clearAnimation()
        drawable!!.setVisible(false, false)
        history_empty_state.setImageDrawable(drawable)

        drawable.start()

        val toolbaranimation = android.animation.AnimatorSet()
        val translateYArtistType = android.animation.ObjectAnimator.ofFloat(no_data_text1, "translationY", -no_data_text1.height.toFloat(), 0f)
        val alphaArtistType = android.animation.ObjectAnimator.ofFloat(no_data_text1, "alpha", 0f, 1f)
        toolbaranimation.playTogether(translateYArtistType, alphaArtistType)
        toolbaranimation.duration = 500
        toolbaranimation.startDelay = 1550
        toolbaranimation.start()
        com.ringtone.maker.Views.LeftWaveInView.doWaveInAnimForView(OvershootInterpolator(), 2050, no_data_text2, true)
    }



    private fun GetMusicDataFromDB(): ArrayList<MusicFile> {
        return dbHelper!!.FilterMusicByDate(mItems)
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        return false
    }

    override fun onPause() {
        super.onPause()

        adapterSelection!!.OnPause()

    }

    fun RestoreToDefault() {

        if (TotalMusicSize != mItems.size) {
            mItems.clear()
            adapterSelection!!.ClearData()
            adapterSelection = null
            adapterSelection = Adapter_Selection()
            recycler_view.adapter = adapterSelection
            GetMusicDataFromDB()
            adapterSelection!!.updateData(mItems, activity)
        }

    }


    fun SearchQuery(Keyword: String?) {

        mItems.clear()
        adapterSelection!!.ClearData()
        adapterSelection = null
        adapterSelection = Adapter_Selection()
        recycler_view.adapter = adapterSelection
        dbHelper!!.SearchSongByTitle(Keyword.toString(), mItems)
        adapterSelection!!.updateData(mItems, activity)

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: com.ringtone.maker.Entities.WaveFormCompletionEvent) {
        val layoutManager = recycler_view.layoutManager as LinearLayoutManager
        val firstVisiblePosition = layoutManager.findFirstVisibleItemPosition()
        val firstLastPosition = layoutManager.findLastVisibleItemPosition()

        for (x in firstVisiblePosition..firstLastPosition) {
            if (mItems.get(x).musicPath!!.equals(event.message)) {
                val Viewholder = recycler_view.findViewHolderForPosition(x) as Viewholder_SelectionItem
                Viewholder.DisplayWaveForm(mItems[x].musicPath!!.hashCode(), 540)
            }
        }

    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        EventBus.getDefault().unregister(this)
        super.onStop()
    }


}
