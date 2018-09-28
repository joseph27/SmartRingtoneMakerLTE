/*
 * *
 *  * Created by Youssef Assad on 6/2/18 11:22 AM
 *  * Copyright (c) 2018 . All rights reserved.
 *  * Last modified 6/2/18 11:04 AM
 *
 */

package com.ringtone.maker.Fragments

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ringtone.maker.R
import com.ringtone.maker.Models.IntroItem
import kotlinx.android.synthetic.main.fragment_intropage.*


class Fragment_Intro : Fragment() {


    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? =
         inflater?.inflate(R.layout.fragment_intropage, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        frag_title.text = arguments.get(IntroTitle).toString()
        frag_desc.text = arguments.get(IntroDesc).toString()
         frag_img.setImageResource(arguments.getInt(IntroImg))
    }

    companion object {
        val IntroTitle = "IntroTitle"
        val IntroDesc = "IntroDesc"
        val IntroImg = "IntroImg"

        fun newInstance(introItem: IntroItem) = Fragment_Intro().apply {
            arguments = Bundle(3).apply {
             putString(IntroTitle, introItem.Title)
             putString(IntroDesc, introItem.Desc)
             putInt(IntroImg, introItem.Img)
            }
        }
    }

}