/*
 * *
 *  * Created by Youssef Assad on 6/2/18 11:17 AM
 *  * Copyright (c) 2018 . All rights reserved.
 *  * Last modified 6/2/18 11:04 AM
 *
 */

package com.ringtone.maker.Views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.PathMeasure;
import android.os.Handler;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.util.Log;

import com.ringtone.maker.R;


/**
 * Created by Joseph27 on 4/21/16.
 */
public class CustomMarkView extends AppCompatImageView {


    Paint paint;
    Path mPath;
    final Handler h = new Handler();

    boolean ClearCanvas = true;

    float length;

    float phase;

    public CustomMarkView(Context context) {
        super(context);

    }

    public CustomMarkView(Context context, AttributeSet attrs) {
        super(context, attrs);

    }

    public CustomMarkView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

    }


    public void setupAnimations() {

        int left =0;
        int bottom = getMeasuredHeight();
        mPath = new Path();
        paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(getResources().getColor(R.color.blueNavy));
        paint.setStrokeWidth(8);

        mPath.moveTo(left + (bottom * 3) / 12, (bottom * 6.5f) / 12);
        mPath.lineTo(left + (bottom * 5) / 12, (bottom * 8) / 12);
        mPath.lineTo(left + (bottom * 8) / 12, (bottom * 4) / 12);

        PathMeasure measure = new PathMeasure(mPath, false);
        length = measure.getLength();

        float[] intervals = new float[]{length, length};

        ObjectAnimator animator = ObjectAnimator.ofFloat(this, "phase", 1.0f, 0.0f);
        animator.setDuration(1500);
        animator.start();
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                mPath.close();


            }
        });
//        h.postDelayed(r2, 800); // 5 second delay

        ClearCanvas =false;
    }




    //is called by animtor object
    public void setPhase(float phase)
    {
        this.phase  = phase;
        paint.setPathEffect(createPathEffect(length, phase));
         invalidate();

    }

    private static PathEffect createPathEffect(float pathLength, float phase)
    {
        return new DashPathEffect(new float[] { pathLength, pathLength },
                Math.max(phase * pathLength, 0.0f));
    }



    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);



        if (ClearCanvas){


            Path mPath = new Path();
            Paint paint = new Paint();
            mPath.moveTo(0 ,0);
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(Color.TRANSPARENT);
            paint.setStrokeWidth(8);

            //   canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        }else{
            canvas.drawPath(mPath, paint); // draw on canvas

        }

    }

    @Override
    public void setAlpha(int alpha) {

    }

    @Override
    public void setColorFilter(ColorFilter cf) {

    }




}