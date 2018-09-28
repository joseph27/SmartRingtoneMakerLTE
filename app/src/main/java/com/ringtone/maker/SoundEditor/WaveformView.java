/*
 * *
 *  * Created by Youssef Assad on 6/2/18 11:17 AM
 *  * Copyright (c) 2018 . All rights reserved.
 *  * Last modified 6/2/18 11:04 AM
 *
 */

package com.ringtone.maker.SoundEditor;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import com.ringtone.maker.R;
import com.ringtone.maker.Models.SelectionPoint;

import java.util.ArrayList;

/**
 * WaveformView is an Android view that displays a visual representation
 * of an audio waveform.  It retrieves the frame gains from a CheapSoundFile
 * object and recomputes the shape contour at several zoom levels.
 * <p/>
 * This class doesn't handle selection or any of the touch interactions
 * directly, so it exposes a listener interface.  The class that embeds
 * this view should add itself as a listener and make the view scroll
 * and respond to other events appropriately.
 * <p/>
 * WaveformView doesn't actually handle selection, but it will just display
 * the selected part of the waveform in a different color.
 *
 * Modified by Youssef Assad <joseph.as3d@gmail.com>
 */
public class WaveformView extends View {

    private static final String TAG = "WaveformView";

    public interface WaveformListener {
        void waveformTouchStart(float x);
        void waveformTouchMove(float x);
        void waveformTouchEnd();
        void waveformFling(float x);
        void waveformDraw();
        void waveformZoomIn();
        void waveformZoomOut();
        void CreateSelection(double start, double end);
    }

    // Colors
    private final Paint mGridPaint;
    private final Paint mSelectedLinePaint;
    private final Paint mUnselectedLinePaint;
    private final Paint mUnselectedBkgndLinePaint;
    private final Paint mBorderLinePaint;
    private final Paint mPlaybackLinePaint;
    private final Paint mTimecodePaint;
    private final Paint mSelectedRectangle;

    private CheapSoundFile mSoundFile;
    private int[] mLenByZoomLevel;
    private float[] mZoomFactorByZoomLevel;
    private int mZoomLevel;
    private int mNumZoomLevels;
    private int mSampleRate;
    private int mSamplesPerFrame;
    private int mOffset;
    private int mSelectionStart;
    private int mSelectionEnd;
    private int mPlaybackPos;
    private float mDensity;
    private float mInitialScaleSpan;
    private WaveformListener mListener;
    private final GestureDetector mGestureDetector;
    private final ScaleGestureDetector mScaleGestureDetector;
    private boolean mInitialized;

    private float range;
    private float scaleFactor;
    private float minGain;
    private ArrayList<SelectionPoint> dataPointsArrayList;
    private int maxheight = 1;

    private final Context context;
    private int mSamplingRate = 15;
    private final int minHeight40;
    private final int minHeight19;
    private final int minHeight4;
    private final int minHeight6;
    private int mSoundDuration;

    private int HeightCounter = 0;
    private int ZeroCounter = 0 ;

    public WaveformView(Context context, AttributeSet attrs) {
        super(context, attrs);


        this.context = context;
        minHeight19 = pxtodp(19);
        minHeight4 = pxtodp(4);
        minHeight6 = pxtodp(6);
        minHeight40 = pxtodp(40);

        // We don't want keys, the markers get these
        setFocusable(false);

        mSelectedRectangle = new Paint();
        mSelectedRectangle.setAntiAlias(false);
        mSelectedRectangle.setStyle(Paint.Style.FILL_AND_STROKE);
        mSelectedRectangle.setColor(getResources().getColor(R.color.editor_selected_rectangle_color));


        mGridPaint = new Paint();
        mGridPaint.setAntiAlias(false);
        mGridPaint.setStrokeWidth(pxtodp(1));
        mGridPaint.setColor(getResources().getColor(R.color.editor_grind_paint_color));


        mSelectedLinePaint = new Paint();
        mSelectedLinePaint.setAntiAlias(false);
        mSelectedLinePaint.setStrokeWidth(pxtodp(2));
        mSelectedLinePaint.setStrokeCap(Paint.Cap.ROUND);
        mSelectedLinePaint.setColor(Color.parseColor(getContext().getString(R.string.editor_selected_line_paint_color)));

        mUnselectedLinePaint = new Paint();
        mUnselectedLinePaint.setAntiAlias(false);
        mUnselectedLinePaint.setStrokeWidth(pxtodp(2));
        mUnselectedLinePaint.setStrokeCap(Paint.Cap.ROUND);
        mUnselectedLinePaint.setColor(getResources().getColor(R.color.editor_mUnselectedline_color));

        mUnselectedBkgndLinePaint = new Paint();
        mUnselectedBkgndLinePaint.setAntiAlias(false);
        mUnselectedBkgndLinePaint.setColor(getResources().getColor(R.color.editor_waveform_bg_color));

        mBorderLinePaint = new Paint();
        mBorderLinePaint.setAntiAlias(true);
        mBorderLinePaint.setStrokeWidth(pxtodp(2));
        //    mBorderLinePaint.setPathEffect(new DashPathEffect(new float[]{3.0f, 2.0f}, 0.0f));
        mBorderLinePaint.setColor(getResources().getColor(R.color.editor_border_line_paint_color));

        //playbackline
        mPlaybackLinePaint = new Paint();
        mPlaybackLinePaint.setAntiAlias(false);
        mPlaybackLinePaint.setStrokeWidth(pxtodp(1));
        mPlaybackLinePaint.setColor(getResources().getColor(R.color.editor_play_back_line_paint_color));

        //time;ome
        mTimecodePaint = new Paint();
        mTimecodePaint.setTextSize(pxtodp(2));
        mTimecodePaint.setAntiAlias(true);
        mTimecodePaint.setColor(getResources().getColor(R.color.editor_border_line_paint_color));
     //   mTimecodePaint.setShadowLayer(2, 1, 1, getResources().getColor(R.color.white));

        mGestureDetector = new GestureDetector(
                context,
                new GestureDetector.SimpleOnGestureListener() {
                    public boolean onFling(
                            MotionEvent e1, MotionEvent e2, float vx, float vy) {
                        mListener.waveformFling(vx);
                        Log.d(this.getClass().getSimpleName(), "onFling: vx " + vx);
                        return true;
                    }
                });

        mScaleGestureDetector = new ScaleGestureDetector(
                context,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    public boolean onScaleBegin(ScaleGestureDetector d) {
                        mInitialScaleSpan = Math.abs(d.getCurrentSpanX());
                        return true;
                    }
                    public boolean onScale(ScaleGestureDetector d) {
                        float scale = Math.abs(d.getCurrentSpanX());
                        if (scale - mInitialScaleSpan > 40) {
                            mListener.waveformZoomIn();
                            mInitialScaleSpan = scale;
                        }
                        if (scale - mInitialScaleSpan < -40) {
                            mListener.waveformZoomOut();
                            mInitialScaleSpan = scale;
                        }
                        return true;
                    }
                });

        mSoundFile = null;
        mLenByZoomLevel = null;
        mOffset = 0;
        mPlaybackPos = -1;
        mSelectionStart = 0;
        mSelectionEnd = 0;
        mDensity = 1.0f;
        mInitialized = false;

        GetSamplingRate();

    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mScaleGestureDetector.onTouchEvent(event);
        if (mGestureDetector.onTouchEvent(event)) {
            return true;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mListener.waveformTouchStart(event.getX());
                break;
            case MotionEvent.ACTION_MOVE:
                mListener.waveformTouchMove(event.getX());
                 break;
            case MotionEvent.ACTION_UP:
                mListener.waveformTouchEnd();

                break;
        }
        return true;
    }

    public boolean hasSoundFile() {
        return mSoundFile != null;
    }

    public void setSoundFile(CheapSoundFile soundFile) {
        mSoundFile = soundFile;
        mSampleRate = mSoundFile.getSampleRate();
        mSamplesPerFrame = mSoundFile.getSamplesPerFrame();
        computeDoublesForAllZoomLevels();


          if (mSoundDuration < 480){

        this.postDelayed(() -> {

             FilterData();

            dataPointsArrayList.size();

           //counter -1 for increment
            int j =-1;

            ArrayList<SelectionPoint> temparray = new ArrayList<>();

            for (int x = 0; x <  dataPointsArrayList.size()-1; x++) {

               double z = (dataPointsArrayList.get(x +1).getStartsec());
               double w = (dataPointsArrayList.get(x ).getStartsec());

                //10 secs is the max difference between 2 Amplitudes
                if ( z - w <= 2500){

                    // increase the counter
                    j++;
                    // check if the list is completed
                    if(dataPointsArrayList.size() == x+2 ){
                        temparray.add(new SelectionPoint(x+1 , j, dataPointsArrayList.get(x -j).getStartsec() ,dataPointsArrayList.get(x).getStartsec()));
                    }

                }else {

                    // get the difference between the first point and last point with counter difference
                    temparray.add(new SelectionPoint(x+1 , j, dataPointsArrayList.get(x -j).getStartsec() ,dataPointsArrayList.get(x).getStartsec()));
                    //reset the counter
                    j = -1;

                }

            }

            temparray.size();


            double startpoint = 0;
            double endpoint = 0;
            int Maxprobablity =0;

            for (int i = 0; i < temparray.size(); i++) {
               int h = temparray.get(i).getYposition();

                if (h > Maxprobablity) {
                    Maxprobablity = h;
                    startpoint = temparray.get(i).getStartsec();
                    endpoint = temparray.get(i).getEndsec();
                }

            }
         // add the difference to the endpoint if the duration is less than 30 secs
            if (endpoint - startpoint < 40000){double  temp = endpoint + ( 40000 - (endpoint - startpoint));

                if ( temp < pixelsToMillisecs(mLenByZoomLevel[mZoomLevel] -1)){
                    endpoint = temp;
                }

            }

            if (startpoint/1000 < 1){
                startpoint = 1000;
            }
           mListener.CreateSelection(startpoint/1000 ,endpoint/1000);



        },750);

          }

    }




    private void GetMaxHeight() {



        // neglecting the first and last frames
        int toleranceRat = (0);
        for (int i = toleranceRat; i < mLenByZoomLevel[mZoomLevel] - toleranceRat; i++) {
            int h = (int) (getScaledHeight(mZoomFactorByZoomLevel[mZoomLevel], i) * getMeasuredHeight() / 2);

            if (h > maxheight && h < getMeasuredHeight() / 2) {
                maxheight = h;
            }
           // Log.v("maxheight", maxheight + "");
        }


    }

    private void FilterData(){

        GetMaxHeight();
        dataPointsArrayList = new ArrayList<>();

        // neglecting the first and last frames
        int toleranceRat = (mLenByZoomLevel[mZoomLevel] * 8 / 100);

        int j = 0;
        while (dataPointsArrayList.size() < toleranceRat) {

            j++;

            dataPointsArrayList.clear();
            for (int i = 0; i < mLenByZoomLevel[mZoomLevel]; i++) {
                int h = (int) (getScaledHeight(mZoomFactorByZoomLevel[mZoomLevel], i) * getMeasuredHeight() / 2);

                //get the amplitude that above the percentage of the max height
                // make sure the divided value is bigger than 0
                int filterPercentage = 85;
                int maxheightFilterPercentage = (maxheight * (filterPercentage - j) );
                if (maxheightFilterPercentage != 0){
                    int divider = (maxheight * (filterPercentage - j));
                    if (divider != 0){
                        if (((h / (divider / 100)) > 0) && (h < getMeasuredHeight() / 2)) {
                            SelectionPoint data = new SelectionPoint(i, h, pixelsToMillisecs(i), 0);
                            dataPointsArrayList.add(data);
                            //    Log.v("Height", h + "");

                        }
                    }

                }



            }
        }

    }

    public boolean isInitialized() {
        return mInitialized;
    }

    private int getZoomLevel() {
        return mZoomLevel;
    }

    private boolean canZoomIn() {
        return (mZoomLevel < mNumZoomLevels - 1);
    }

    public void zoomIn() {


        if (canZoomIn()) {
            mZoomLevel++;
            float factor = mLenByZoomLevel[mZoomLevel] / (float) mLenByZoomLevel[mZoomLevel - 1];
            mSelectionStart *= factor;
            mSelectionEnd *= factor;
            int offsetCenter = mOffset + (int) (getMeasuredWidth() / factor);
            offsetCenter *= factor;
            mOffset = offsetCenter - (int) (getMeasuredWidth() / factor);
            if (mOffset < 0)
                mOffset = 0;
            invalidate();


        }
    }

    public boolean canZoomOut() {
        return (mZoomLevel > 0);
    }

    public void zoomOut() {
        if (canZoomOut()) {
            mZoomLevel--;
            float factor = mLenByZoomLevel[mZoomLevel + 1] / (float) mLenByZoomLevel[mZoomLevel];
            mSelectionStart /= factor;
            mSelectionEnd /= factor;
            int offsetCenter = (int) (mOffset + getMeasuredWidth() / factor);
            offsetCenter /= factor;
            mOffset = offsetCenter - (int) (getMeasuredWidth() / factor);
            if (mOffset < 0)
                mOffset = 0;
            invalidate();
        }
    }

    public int maxPos() {
        return mLenByZoomLevel[mZoomLevel];
    }

    public int secondsToFrames(double seconds) {
        return (int) (1.0 * seconds * mSampleRate / mSamplesPerFrame + 0.5);
    }

    public int secondsToPixels(double seconds) {
        double z = mZoomFactorByZoomLevel[mZoomLevel];
        return (int) (z * seconds * mSampleRate / mSamplesPerFrame + 0.5);
    }

    public double pixelsToSeconds(int pixels) {
        double z = mZoomFactorByZoomLevel[mZoomLevel];
        return (pixels * (double) mSamplesPerFrame / (mSampleRate * z));
    }

    public int millisecsToPixels(int msecs) {
        double z = mZoomFactorByZoomLevel[mZoomLevel];
        return (int) ((msecs * 1.0 * mSampleRate * z) / (1000.0 * mSamplesPerFrame) + 0.5);
    }

    public int pixelsToMillisecs(int pixels) {
        double z = mZoomFactorByZoomLevel[mZoomLevel];
        return (int) (pixels * (1000.0 * mSamplesPerFrame) / (mSampleRate * z) + 0.5);
    }

    public void setParameters(int start, int end, int offset, int duration) {
        mSelectionStart = start;
        mSelectionEnd = end;
        mOffset = offset;
        this.mSoundDuration = duration;

    }

    public int getStart() {
        return mSelectionStart;
    }

    public int getEnd() {
        return mSelectionEnd;
    }

    public int getOffset() {
        return mOffset;
    }

    public void setPlayback(int pos) {
        mPlaybackPos = pos;
    }

    public void setListener(WaveformListener listener) {
        mListener = listener;
    }



    public void recomputeHeights(float density) {
        mDensity = density;
        mTimecodePaint.setTextSize((int) (12 * density));

        invalidate();
    }

    private void drawWaveformLine(Canvas canvas, int x, int y0, int y1, Paint paint) {

        canvas.drawLine(x, y0, x, y1, paint);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //if no file don't draw
        if (mSoundFile == null)
            return;


        int measuredWidth = getMeasuredWidth();
        int measuredHeight = getMeasuredHeight();
        int start = mOffset;
        int width = mLenByZoomLevel[mZoomLevel] - start;

        //drawing center point = view height/2
        int ctr = measuredHeight / 2;

        if (width > measuredWidth)
            width = measuredWidth;

        // when the offset and width = zoom length fix the start .
        if (getMeasuredWidth() + mOffset >= mLenByZoomLevel[mZoomLevel]) {
            //   start =  mOffset = mLenByZoomLevel[mZoomLevel] - getMeasuredWidth();
            width = getMeasuredWidth();
        }


        int i = 0;

        while (i < width) {
               drawWaveformUnPlayed(canvas, i, start, ctr +  pxtodp(12), selectWaveformPaint(i, start));
            i++;
        }

        canvas.drawLine(
                mSelectionStart - mOffset + 0.5f, minHeight40,
                mSelectionStart - mOffset + 0.5f, measuredHeight ,
                mBorderLinePaint);

        canvas.drawLine(
                mSelectionEnd - mOffset + 0.5f, minHeight40,
                mSelectionEnd - mOffset + 0.5f, measuredHeight ,
                mBorderLinePaint);

        canvas.drawRect(mSelectionStart - mOffset + 0.5f, minHeight40, mSelectionEnd - mOffset + 0.5f, measuredHeight , mSelectedRectangle);


        // Draw timecode

        int pixelsdiveration;

        // check if the song duration is too high then the timeline diveration will be divided by 10 mins
        if (getZoomLevel() == 0){
            if (mSoundDuration > 360){
                pixelsdiveration = 10000;
            }else if (mSoundDuration < 230){
                // by 5 ses
                pixelsdiveration = 50;
            }else {
                // default by 10 secs
                pixelsdiveration = 100;
            }

        }else {
            // if otherzoom level than 0 then by 5 secs
            pixelsdiveration = 50;
        }

        double onePixelInSecs = pixelsToSeconds(1);
        boolean onlyEveryFiveSecs = (onePixelInSecs > 1.0 / pixelsdiveration);
        double fractionalSecs = mOffset * onePixelInSecs;
        int integerSecs = (int) fractionalSecs;

        canvas.drawRect(0,0,getMeasuredWidth(),pxtodp(1),mGridPaint);

        i = 0;
        while (i < width) {
            i++;
           // canvas.drawLine(i, 0, i, 5, mGridPaint);
           fractionalSecs += onePixelInSecs;
            int integerSecsNew = (int) fractionalSecs;
            if (integerSecsNew != integerSecs) {
               integerSecs = integerSecsNew;
                if (!onlyEveryFiveSecs || 0 == (integerSecs % (pixelsdiveration/10))) {
                   canvas.drawLine(i, 0, i, pxtodp(3), mGridPaint);
                }
          }
        }

         fractionalSecs = mOffset * onePixelInSecs;

        double timecodeIntervalSecs = 1.0;

        if (getZoomLevel() == 2){
             timecodeIntervalSecs = 3.0;
        }else if (getZoomLevel() == 3){
            timecodeIntervalSecs = 2.0;
        }else if (getZoomLevel() == 1){
            timecodeIntervalSecs = 5.0;
        }

        if ((timecodeIntervalSecs / onePixelInSecs) < pixelsdiveration ) {


            if (mSoundDuration > 360){
                timecodeIntervalSecs = mSoundDuration /8;
            }else if (mSoundDuration < 230){

                timecodeIntervalSecs = mSoundDuration /7;
            }else {
                timecodeIntervalSecs = 30;
            }


            if (getZoomLevel() == 1){
                timecodeIntervalSecs = 15.0;
            }
        }

        // Draw grid

        int integerTimecode = (int) (fractionalSecs / timecodeIntervalSecs);


        i = 0;
        while (i < width) {
            drawWaveformPlayed(canvas, i, start, measuredHeight);
            i++;

            // add pixelinsec  to the intitial fractionalsecs  each time the loop occers
             fractionalSecs += onePixelInSecs;

            int integerSecs2 = (int) fractionalSecs;

            // fractional secs / time interval
            int integerTimecodeNew = (int) (fractionalSecs / timecodeIntervalSecs);

           if (integerTimecodeNew != integerTimecode) {
                integerTimecode = integerTimecodeNew;

                // Turn, e.g. 67 seconds into "1:07"
                String timecodeMinutes = "" + (integerSecs2 / 60);
                String timecodeSeconds = "" + (integerSecs2 % 60);
                if ((integerSecs2 % 60) < 10) {
                    timecodeSeconds = "0" + timecodeSeconds;
                }

                String timecodeStr = timecodeMinutes + ":" + timecodeSeconds;


                float offset = (float) (0.4 *  mTimecodePaint.measureText(timecodeStr));

                if (i < (getMeasuredWidth() - getMeasuredWidth()/14) &&getZoomLevel() == 0){
                    canvas.drawText(timecodeStr, i - offset, (int) (16 * mDensity), mTimecodePaint);
                }else if (getZoomLevel() != 0){
                    canvas.drawText(timecodeStr, i - offset, (int) (16 * mDensity), mTimecodePaint);
                }

            }
        }

        if (mListener != null) {
            mListener.waveformDraw();
        }
    }



    private void drawWaveformPlayed(final Canvas canvas, final int i, final int start, final int measuredHeight) {
        if (i +  start == mPlaybackPos  && mPlaybackPos != -1) {
            Path path = new Path();
            path.moveTo(i, getMeasuredHeight() - minHeight6);
            path.lineTo(i + minHeight4, getMeasuredHeight());
            path.lineTo(i - minHeight4, getMeasuredHeight()  );
            path.close();
            Paint p = new Paint();
            p.setColor(getResources().getColor(R.color.white));
            canvas.drawPath(path, p);
           canvas.drawLine(i,  0 , i, measuredHeight , mPlaybackLinePaint);

        }

    }


    private void drawWaveformUnPlayed(final Canvas canvas, final int i, final int start, final int ctr, final Paint paint) {

        int h = (int) (getScaledHeight(mZoomFactorByZoomLevel[mZoomLevel], start + i) * getMeasuredHeight() / 2);
        HeightCounter += h;


        if (h == 0 && ZeroCounter < mSamplingRate -1){
            ZeroCounter ++;
        }

        if ((((start + i))% mSamplingRate) == 0) {

            h = HeightCounter /(mSamplingRate -ZeroCounter);

            HeightCounter = 0;
            ZeroCounter = 0;

            int positivey;
            int negativey;

            //top
            if (h <=  minHeight19 ){
                positivey = ctr - h  ;
            }else{
                positivey = ctr - h  + minHeight40 ;
            }

            //bottom
            if (h <= minHeight19){
                negativey = ctr + 1 + h   ;
            }else{
                negativey = ctr + 1 + h - minHeight40 ;
            }
                drawWaveformLine(
                        canvas, i,
                        positivey,
                        negativey,
                        paint);
    }


    }

    private Paint selectWaveformPaint(final int i, final int start) {
        Paint paint;
        if (i + start >= mSelectionStart && i + start < mSelectionEnd) {
            paint = mSelectedLinePaint;
        } else {
            paint = mUnselectedLinePaint;
        }


        return paint;
    }

    private float getGain(int i, int numFrames, int[] frameGains) {
        int x = Math.min(i, numFrames - 1);
        if (numFrames < 2) {
            return frameGains[x];
        } else {
            if (x == 0) {
                return (frameGains[0] / 2.0f) + (frameGains[1] / 2.0f);
            } else if (x == numFrames - 1) {
                return (frameGains[numFrames - 2] / 2.0f) + (frameGains[numFrames - 1] / 2.0f);
            } else {

                // to avoid out of index error
                if (x < 0){
                    return  0;
                }
                else {

                    return (frameGains[x - 1] / 3.0f) + (frameGains[x] / 3.0f) + (frameGains[x + 1] / 3.0f);
                }


            }
        }
    }

    private float getHeight(int i, int numFrames, int[] frameGains, float scaleFactor, float minGain, float range) {
        float value = (getGain(i, numFrames, frameGains) * scaleFactor - minGain) / range;
        if (value < 0.0)
            value = 0.0f;
        if (value > 1.0)
            value = 1.0f;

        return value;
    }

    /**
     * Called once when a new sound file is added
     */
    private void computeDoublesForAllZoomLevels() {
        int numFrames = mSoundFile.getNumFrames();

        // Make sure the range is no more than 0 - 255

        float maxGain = 1.0f;
        for (int i = 0; i < numFrames; i++) {
            float gain = getGain(i, numFrames, mSoundFile.getFrameGains());
            if (gain > maxGain) {
                maxGain = gain;
            }
        }

        scaleFactor = 1.0f;
        if (maxGain > 255.0) {
            scaleFactor = 255 / maxGain;
        }

        // Build histogram of 256 bins and figure out the new scaled max
        maxGain = 0;
        int gainHist[] = new int[256];
        for (int i = 0; i < numFrames; i++) {
            int smoothedGain = (int) (getGain(i, numFrames, mSoundFile.getFrameGains()) * scaleFactor);
            if (smoothedGain < 0)
                smoothedGain = 0;
            if (smoothedGain > 255)
                smoothedGain = 255;

            if (smoothedGain > maxGain)
                maxGain = smoothedGain;

            gainHist[smoothedGain]++;
        }

        // Re-calibrate the min to be 5%
        minGain = 0;
        int sum = 0;
        while (minGain < 255 && sum < numFrames / 20) {
            sum += gainHist[(int) minGain];
            minGain++;
        }

        // Re-calibrate the max to be 99%
        sum = 0;
        while (maxGain > 2 && sum < numFrames / 100) {
            sum += gainHist[(int) maxGain];
            maxGain--;
        }

        range = maxGain - minGain;

        mNumZoomLevels = 6;
        mLenByZoomLevel = new int[6];
        mZoomFactorByZoomLevel = new float[7];

        float ratio = getMeasuredWidth() / (float) numFrames;

        if (ratio < 1) {
            mLenByZoomLevel[0] = Math.round(numFrames * ratio);
            mZoomFactorByZoomLevel[0] = ratio;

            mLenByZoomLevel[1] = numFrames;
            mZoomFactorByZoomLevel[1] = 1.0f;

            mLenByZoomLevel[2] = numFrames * 2;
            mZoomFactorByZoomLevel[2] = 2.0f;

            mLenByZoomLevel[3] = numFrames * 3;
            mZoomFactorByZoomLevel[3] = 3.0f;

            mLenByZoomLevel[4] = numFrames * 4;
            mZoomFactorByZoomLevel[4] = 4.0f;

            mLenByZoomLevel[5] = numFrames * 5;
            mZoomFactorByZoomLevel[5] = 5.0f;

            mZoomLevel = 0;
        } else {
            mLenByZoomLevel[0] = numFrames;
            mZoomFactorByZoomLevel[0] = 1.0f;

            mLenByZoomLevel[1] = numFrames * 2;
            mZoomFactorByZoomLevel[1] = 2f;

            mLenByZoomLevel[2] = numFrames * 3;
            mZoomFactorByZoomLevel[2] = 3.0f;

            mLenByZoomLevel[3] = numFrames * 4;
            mZoomFactorByZoomLevel[3] = 4.0f;

            mLenByZoomLevel[4] = numFrames * 5;
            mZoomFactorByZoomLevel[5] = 5.0f;

            mLenByZoomLevel[5] = numFrames * 6;
            mZoomFactorByZoomLevel[6] = 6.0f;

            mZoomLevel = 0;
            for (int i = 0; i < 5; i++) {
                if (mLenByZoomLevel[mZoomLevel] - getMeasuredWidth() > 0) {
                    break;
                } else {
                    mZoomLevel = i;
                }
            }
        }

        mInitialized = true;
    }

    private float getZoomedInHeight(float zoomLevel, int i) {
        int f = (int) zoomLevel;
        if (i == 0) {
            return 0.5f * getHeight(0, mSoundFile.getNumFrames(), mSoundFile.getFrameGains(), scaleFactor, minGain, range);
        }
        if (i == 1) {
            return getHeight(0, mSoundFile.getNumFrames(), mSoundFile.getFrameGains(), scaleFactor, minGain, range);
        }
        if (i % f == 0) {
            float x1 = getHeight(i / f - 1, mSoundFile.getNumFrames(), mSoundFile.getFrameGains(), scaleFactor, minGain, range);
            float x2 = getHeight(i / f, mSoundFile.getNumFrames(), mSoundFile.getFrameGains(), scaleFactor, minGain, range);
            return 0.5f * (x1 + x2);
        } else if ((i - 1) % f == 0) {
            return getHeight((i - 1) / f, mSoundFile.getNumFrames(), mSoundFile.getFrameGains(), scaleFactor, minGain, range);
        }
        return 0;
    }

    private float getZoomedOutHeight(float zoomLevel, int i) {
        int f = (int) (i / zoomLevel);
        float x1 = getHeight(f, mSoundFile.getNumFrames(), mSoundFile.getFrameGains(), scaleFactor, minGain, range);
        float x2 = getHeight(f + 1, mSoundFile.getNumFrames(), mSoundFile.getFrameGains(), scaleFactor, minGain, range);
        return 0.5f * (x1 + x2);
    }

    private float getNormalHeight(int i) {
        return getHeight(i, mSoundFile.getNumFrames(), mSoundFile.getFrameGains(), scaleFactor, minGain, range);
    }

    private float getScaledHeight(float zoomLevel, int i) {
      //  Log.d(this.getClass().getSimpleName(), "getScaledHeight: zoomLevel" + zoomLevel + " I " + i );
        if (zoomLevel == 1.0) {
            return getNormalHeight(i);
        } else if (zoomLevel < 1.0) {
            return getZoomedOutHeight(zoomLevel, i);
        }
        return getZoomedInHeight(zoomLevel, i);
    }



    public int getcurrentmLevel(){
        if (mLenByZoomLevel != null){
            return mLenByZoomLevel[mZoomLevel];
        }else {
            return  0;
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        GetSamplingRate();
    }


    /**
     * Diver Rate = Gap Distance = Width of Line + Gap Space between line
     * @return
     */

    private void GetSamplingRate(){
        mSamplingRate = pxtodp(2) + pxtodp(1);

    }

    private int pxtodp(int value){
        Resources r = context.getResources();
        int px = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                r.getDisplayMetrics()
        );
        return  px;
    }

}

