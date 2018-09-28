/*
 * *
 *  * Created by Youssef Assad on 6/2/18 11:22 AM
 *  * Copyright (c) 2018 . All rights reserved.
 *  * Last modified 6/2/18 11:04 AM
 *
 */

package com.ringtone.maker.Views

import android.animation.Animator
import android.animation.AnimatorSet
import android.annotation.TargetApi
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.os.Build
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.util.Property
import android.view.View
import android.view.ViewOutlineProvider
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout

import com.ringtone.maker.R


class PlayPauseView(context: Context, attrs: AttributeSet) : FrameLayout(context, attrs) {

    private val mDrawable: PlayPauseDrawable
    private val mPaint = Paint()


    private var mAnimatorSet: AnimatorSet? = null
    private var mBackgroundColor: Int = 0
    private var mArrowColor: Int = 0
    private var mWidth: Int = 0
    private var mHeight: Int = 0
    private var isPlaying: Boolean = false
    private val isInteractive: Boolean = false
    private var width: Float = 0.toFloat()
    private var height: Float = 0.toFloat()
    private var distance: Float = 0.toFloat()

    private var color: Int
        get() = mBackgroundColor
        set(color) {
            mBackgroundColor = color
            invalidate()
        }


    init {

        val typedArray = context.theme.obtainStyledAttributes(
                attrs,
                R.styleable.PlayPauseView,
                0, 0)
        //Reading values from the XML layout
        try {
            mBackgroundColor = typedArray.getColor(R.styleable.PlayPauseView_backgroundcolor, resources.getColor(R.color.colorPrimary))
            mArrowColor = typedArray.getColor(R.styleable.PlayPauseView_PlayPauseArrow_color, ContextCompat.getColor(context,R.color.colorPrimary))
            width = typedArray.getDimension(R.styleable.PlayPauseView_PlayPauseView_width, 0f)
            height = typedArray.getDimension(R.styleable.PlayPauseView_PlayPauseView_height, 0f)
            distance = typedArray.getDimension(R.styleable.PlayPauseView_PlayPauseView_distance, 0f)
        } finally {
            typedArray.recycle()
        }

        setWillNotDraw(false)
        mPaint.isAntiAlias = true
        mPaint.style = Paint.Style.FILL
        mDrawable = PlayPauseDrawable(context, width, height, distance , mArrowColor)
        mDrawable.callback = this
    }


    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mDrawable.setBounds(0, 0, w, h)
        mWidth = w
        mHeight = h

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            outlineProvider = object : ViewOutlineProvider() {
                @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                override fun getOutline(view: View, outline: Outline) {
                    outline.setOval(0, 0, view.width, view.height)
                }
            }
            clipToOutline = true
        }
    }

    override fun verifyDrawable(who: Drawable): Boolean {
        return who === mDrawable || super.verifyDrawable(who)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        mPaint.color = mBackgroundColor
        val radius = Math.min(mWidth, mHeight) / 2f
        canvas.drawCircle(mWidth / 2f, mHeight / 2f, radius, mPaint)
        mDrawable.draw(canvas)
    }


    fun toggle() {
        if (mAnimatorSet != null) {
            mAnimatorSet!!.cancel()
        }




        mAnimatorSet = AnimatorSet()
        //  final ObjectAnimator colorAnim = ObjectAnimator.ofInt(this, COLOR, isPlay ? mPauseBackgroundColor : mPlayBackgroundColor);
        //  colorAnim.setEvaluator(new ArgbEvaluator());
        val pausePlayAnim = mDrawable.pausePlayAnimator
        mAnimatorSet!!.interpolator = DecelerateInterpolator()
        mAnimatorSet!!.duration = PLAY_PAUSE_ANIMATION_DURATION
        mAnimatorSet!!.play(pausePlayAnim)
        mAnimatorSet!!.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animator: Animator) {
                isEnabled = false
            }

            override fun onAnimationEnd(animator: Animator) {
                isEnabled = true
            }

            override fun onAnimationCancel(animator: Animator) {

            }

            override fun onAnimationRepeat(animator: Animator) {

            }
        })
        mAnimatorSet!!.start()
    }


    fun setPlaying(isPlaying: Boolean) {
        this.isPlaying = isPlaying
        toggle()
    }

    fun isPlaying(): Boolean {
        isPlaying = mDrawable.isPlay
        return isPlaying
    }

    companion object {

        private val COLOR = object : Property<PlayPauseView, Int>(Int::class.java, "color") {
            override fun get(v: PlayPauseView): Int {
                return v.color
            }

            override fun set(v: PlayPauseView, value: Int?) {
                v.color = value!!
            }
        }

        private val PLAY_PAUSE_ANIMATION_DURATION: Long = 400
    }


}
