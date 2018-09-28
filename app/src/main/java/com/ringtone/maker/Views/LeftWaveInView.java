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
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

/**
 * Created by Joseph27 on 5/30/18.
 */

public class LeftWaveInView  extends View
{

    private Bitmap bitmap;
    private float drawPadding;
    private final float[] drawingVerts;
    private TimeInterpolator interpolator;
    private boolean isListenerAdded;
    private int startDelay;
    private final float[] staticVerts;

    public LeftWaveInView(final Context context) {
        super(context);
        this.drawingVerts = new float[192];
        this.staticVerts = new float[192];
        this.isListenerAdded = false;
    }

    public LeftWaveInView(final Context context, final AttributeSet set) {
        super(context, set);
        this.drawingVerts = new float[192];
        this.staticVerts = new float[192];
        this.isListenerAdded = false;
    }

    public LeftWaveInView(final Context context, final AttributeSet set, final int n) {
        super(context, set, n);
        this.drawingVerts = new float[192];
        this.staticVerts = new float[192];
        this.isListenerAdded = false;
    }

    private static void adToParentLayout(final View view, final float drawPadding, final LeftWaveInView leftWaveInView) {
        ((ViewGroup)view.getParent()).addView(leftWaveInView, new ViewGroup.LayoutParams((int)(view.getWidth() + drawPadding * 2.0f), (int)(drawPadding * 2.0f + view.getHeight())));
        leftWaveInView.setX(view.getLeft() - drawPadding);
        leftWaveInView.setY(view.getTop() - drawPadding);
        leftWaveInView.setDrawPadding(drawPadding);
    }

    private static void addToLayoutRoot(final View view, final float drawPadding, final LeftWaveInView leftWaveInView) {
        ((ViewGroup)view.getRootView()).addView(leftWaveInView, new ViewGroup.LayoutParams((int) (view.getWidth() + drawPadding * 2.0f), (int) (drawPadding * 2.0f + view.getHeight())));
        final int[] array = new int[2];
        view.getLocationInWindow(array);
        leftWaveInView.setX(array[0] - drawPadding);
        leftWaveInView.setY(array[1] - drawPadding);
        leftWaveInView.setDrawPadding(drawPadding);
    }

    private static void addView(final TimeInterpolator interpolator, final View view, final int startDelay, final boolean b) {
        final float n = 50.0f * view.getResources().getDisplayMetrics().density;
        final LeftWaveInView leftWaveInView = new LeftWaveInView(view.getContext());
        leftWaveInView.setInterpolator(interpolator);
        leftWaveInView.setStartDelay(startDelay);
        if (b) {
            adToParentLayout(view, n, leftWaveInView);
        }
        else {
            addToLayoutRoot(view, n, leftWaveInView);
        }
        leftWaveInView.createBitmap(view);
        view.setVisibility(INVISIBLE);
        leftWaveInView.animateIn(view);
    }

    private void animateIn(final View view) {
        for (int i = this.drawingVerts.length / 2 - 1; i >= 0; --i) {
            final float n = this.drawingVerts[i * 2 + 1];
            final float n2 = this.drawingVerts[i * 2];
            this.animateXCoords(view, n, n2, i, this.animateYCoords(view, n, n2, i));
        }
    }

    private void animateXCoords(final View view, float n, final float n2, final int n3, final float n4) {
        n = Math.max(0.001f, n) / view.getHeight();
        final ValueAnimator setDuration = ValueAnimator.ofFloat(new float[] { n2 - view.getHeight() / 2.0f * (1.0f - n), n2 }).setDuration(500L);
        setDuration.addUpdateListener(valueAnimator -> LeftWaveInView.this.setXA(LeftWaveInView.this.drawingVerts, n3, (float)valueAnimator.getAnimatedValue()));
        setDuration.setInterpolator(this.interpolator);
        setDuration.setStartDelay(this.startDelay + (long) (245.0f * n4));
        setDuration.start();
    }

    private float animateYCoords(final View view, float n, final float n2, final int n3) {
        final ValueAnimator setDuration = ValueAnimator.ofFloat(new float[] { view.getHeight(), n }).setDuration(500L);
        n = Math.max(0.001f, n2) / view.getWidth();
        this.setXY(this.drawingVerts, n3, n2, view.getHeight());
        setDuration.addUpdateListener(valueAnimator -> LeftWaveInView.this.setXY(LeftWaveInView.this.drawingVerts, n3, n2, (float)valueAnimator.getAnimatedValue()));
        setDuration.setInterpolator(this.interpolator);
        setDuration.setStartDelay(this.startDelay + (long)(245.0f * n));
        setDuration.start();
        if (n == 1.0f && !this.isListenerAdded) {
            setDuration.addListener(this.getWaveFinishedListener(view));
            this.isListenerAdded = true;
        }
        return n;
    }

    private void createBitmap(final View view) {
        this.bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        view.draw(new Canvas(this.bitmap));
        this.createVerts();
    }

    private void createVerts() {
        final float n = this.bitmap.getWidth();
        final float n2 = this.bitmap.getHeight();
        int n3 = 0;
        for (int i = 0; i <= 5; ++i) {
            final float n4 = i * n2 / 5.0f;
            for (int j = 0; j <= 15; ++j) {
                final float n5 = j * n / 15.0f;
                this.setXY(this.drawingVerts, n3, n5, n4);
                this.setXY(this.staticVerts, n3, n5, n4);
                ++n3;
            }
        }
    }

    public static void doWaveInAnimForView(final TimeInterpolator timeInterpolator, final int n, final View view, final boolean b) {
        if (view.getWidth() == 0) {
            view.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                public boolean onPreDraw() {
                    view.getViewTreeObserver().removeOnPreDrawListener(this);
                    if (view.getWidth() != 0) {
                        addView(timeInterpolator, view, n, b);
                    }
                    return true;
                }
            });
            return;
        }
        addView(timeInterpolator, view, n, b);
    }

    private AnimatorListenerAdapter getWaveFinishedListener(final View view) {
        return new AnimatorListenerAdapter() {
            public void onAnimationEnd(final Animator animator) {
                super.onAnimationEnd(animator);
                view.setVisibility(VISIBLE);
                ((ViewGroup)LeftWaveInView.this.getParent()).removeView(LeftWaveInView.this);
                LeftWaveInView.this.bitmap.recycle();
            }
        };
    }

    public float getDrawPadding() {
        return this.drawPadding;
    }

    public TimeInterpolator getInterpolator() {
        return this.interpolator;
    }

    public int getStartDelay() {
        return this.startDelay;
    }

    protected void onDraw(final Canvas canvas) {
        super.onDraw(canvas);
        if (this.bitmap != null) {
            canvas.save();
            canvas.translate(this.drawPadding, this.drawPadding);
            canvas.drawBitmapMesh(this.bitmap, 15, 5, this.drawingVerts, 0, null, 0, null);
            canvas.restore();
        }
        this.invalidate();
    }

    private void setDrawPadding(final float drawPadding) {
        this.drawPadding = drawPadding;
    }

    private void setInterpolator(final TimeInterpolator interpolator) {
        this.interpolator = interpolator;
    }

    private void setStartDelay(final int startDelay) {
        this.startDelay = startDelay;
    }

    private void setXA(final float[] array, final int n, final float n2) {
        array[n * 2 + 0] = n2;
    }

    private void setXY(final float[] array, final int n, final float n2, final float n3) {
        array[n * 2 + 0] = n2;
        array[n * 2 + 1] = n3;
    }

    public void setYA(final float[] array, final int n, final float n2) {
        array[n * 2 + 1] = this.staticVerts[n * 2 + 1] + n2;
    }
}
