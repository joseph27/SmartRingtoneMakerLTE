/*
 * *
 *  * Created by Youssef Assad on 6/2/18 11:22 AM
 *  * Copyright (c) 2018 . All rights reserved.
 *  * Last modified 6/2/18 11:04 AM
 *
 */

package com.ringtone.maker.Utils

import android.view.MotionEvent
import android.view.View

/**
 * Created by youssefassad on 12/8/17.
 */

object ViewUtil {

    fun SetOntouchListener(view: View) {
         view.setOnTouchListener(View.OnTouchListener { view1, motionEvent ->
             val action = motionEvent.action
             if (action == MotionEvent.ACTION_DOWN) {
                 view1.animate().scaleXBy(-0.05f).setDuration(100).start()
                 view1.animate().scaleYBy(-0.05f).setDuration(100).start()
                 return@OnTouchListener false
             } else if (action == MotionEvent.ACTION_UP) {
                 view1.animate().cancel()
                 view1.animate().scaleX(1f).setDuration(100).start()
                 view1.animate().scaleY(1f).setDuration(100).start()
                 return@OnTouchListener false
             }
             false
         })
     }





}
