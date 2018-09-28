/*
 * *
 *  * Created by Youssef Assad on 6/2/18 11:17 AM
 *  * Copyright (c) 2018 . All rights reserved.
 *  * Last modified 6/2/18 11:04 AM
 *
 */

package com.ringtone.maker.Utils;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.animation.LinearInterpolator;


public class AnimationLibs {



    public static AnimatorSet StartSlideinWithFade(View view , int Duration){
        ObjectAnimator animator1 = ObjectAnimator.ofFloat(view, "translationY", -view.getMeasuredHeight()/2 , 0);
        animator1.setDuration(Duration);
        animator1.setRepeatCount(0);
        ObjectAnimator alphaAnimation = ObjectAnimator.ofFloat(view, View.ALPHA, 0, 1);
        alphaAnimation.setRepeatCount(0);
        alphaAnimation.setDuration(Duration);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(animator1, alphaAnimation);
        animatorSet.start();
        return animatorSet;
    }




    public static void StartScaleoutinFadeAnimation(View view , int Duration){
        ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(view, "scaleX", 1.0f,0.5f);
        ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(view, "scaleY", 1.0f,0.5f);
        ObjectAnimator alphaAnimation = ObjectAnimator.ofFloat(view, View.ALPHA, 1, 0);
        alphaAnimation.setRepeatCount(0);
        alphaAnimation.setDuration(Duration);
        scaleDownX.setDuration(Duration);
        scaleDownY.setDuration(Duration);
        AnimatorSet scaleDown = new AnimatorSet();
        scaleDown.play(scaleDownX).with(scaleDownY).with(alphaAnimation);
        scaleDown.start();
    }


    public static void shakewithrotate(View view  , int repeatcount ){

        ObjectAnimator rotate = ObjectAnimator.ofFloat(view,"rotation",-5 , 5);
        rotate.setDuration(70);
        rotate.setRepeatCount(repeatcount);
        rotate.setInterpolator(new LinearInterpolator());
        rotate.setRepeatMode(ValueAnimator.REVERSE);
        ObjectAnimator shake =  ObjectAnimator.ofFloat(view, "translationX", -10, 10);
        shake.setRepeatMode(ValueAnimator.REVERSE);
        shake.setRepeatCount(repeatcount);
        shake.setInterpolator(new LinearInterpolator());
        shake.setDuration(70);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.play(shake).with(rotate);
        animatorSet.start();
    }




    public static void fadeObjectsinSeries( View view1, View view2, View view3) {

        if (view1 != null){
            animateView(view1,(100));
        }

        if (view2 != null){
            animateView(view2,(200));
        }
          if (view3 != null){
              animateView(view3,(300));
          }




    }


    private static void animateView(View paramView, int paramInt)    {

        ObjectAnimator paramView2 = ObjectAnimator.ofFloat(paramView, View.ALPHA, new float[] { 1.0F, 0.3F }).setDuration(500);
        paramView2.setStartDelay(paramInt);
        paramView2.start();
    }








}
