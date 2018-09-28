/*
 * *
 *  * Created by Youssef Assad on 6/2/18 11:22 AM
 *  * Copyright (c) 2018 . All rights reserved.
 *  * Last modified 6/2/18 11:04 AM
 *
 */

package com.ringtone.maker.Views

import android.content.Context
import android.support.v7.widget.AppCompatImageView
import android.util.AttributeSet
import com.ringtone.maker.R


/**
 * Created by Joseph27 on 12/10/17.
 */


class CustomImageView : AppCompatImageView {

    private var heightRatio: Float? = null

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(attrs)
    }

    private fun init(attrs: AttributeSet?) {
        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.CustomCardView)
            heightRatio = a.getFloat(R.styleable.CustomCardView_HeightRatio, 1f)
            a.recycle()
        }
    }


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val width = measuredWidth
        val height = (width * heightRatio!!).toInt()
        setMeasuredDimension(width, height)
    }
}
