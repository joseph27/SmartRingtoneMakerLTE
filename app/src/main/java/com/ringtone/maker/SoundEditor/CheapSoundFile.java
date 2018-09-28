/*
 * *
 *  * Created by Youssef Assad on 6/2/18 11:17 AM
 *  * Copyright (c) 2018 . All rights reserved.
 *  * Last modified 6/2/18 11:04 AM
 *
 */

package com.ringtone.maker.SoundEditor;


import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.ringtone.maker.UILapplication;
import com.ringtone.maker.Models.SelectionPoint;
import com.ringtone.maker.Utils.ExternalStorageUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * CheapSoundFile is the parent class of several subclasses that each
 * do a "cheap" scan of various sound file formats, parsing as little
 * as possible in order to understand the high-level frame structure
 * and get a rough estimate of the volume level of each frame.  Each
 * subclass is able to:
 * - open a sound file
 * - return the sample rate and number of frames
 * - return an approximation of the volume level of each frame
 * <p/>
 * A frame should represent no less than 1 ms and no more than 100 ms of
 * audio.  This is compatible with the native frame sizes of most audio
 * file formats already, but if notification_ic, this class should expose virtual
 * frames in that size range.
 * <p/>
 * Modified by Anna Stępień <anna.stepien@semantive.com>
 */
public class CheapSoundFile {


    protected float range;
    protected float scaleFactor;
    protected float minGain;

    public int[] mLenByZoomLevel;
    public float[] mZoomFactorByZoomLevel;
    public int mZoomLevel;
    protected boolean mInitialized;
    protected int mNumZoomLevels;
    protected  boolean CacheSaved = false;
    private final int measuredWidth = 540;
    public ArrayList<SelectionPoint> dataPointsArrayList;
    int filterPercentage = 85;
    int maxheight = 1;


    public interface ProgressListener {
        /**
         * Will be called by the CheapSoundFile subclass periodically
         * with values between 0.0 and 1.0.  Return true to continue
         * loading the file, and false to cancel.
         */
        boolean reportProgress(double fractionComplete);
    }

    public interface Factory {
        CheapSoundFile create();

        String[] getSupportedExtensions();
    }

    private static final Factory[] sSubclassFactories = new Factory[]{
            com.ringtone.maker.SoundEditor.CheapAAC.getFactory(),
            com.ringtone.maker.SoundEditor.CheapAMR.getFactory(),
            com.ringtone.maker.SoundEditor.CheapMP3.getFactory(),
            com.ringtone.maker.SoundEditor.CheapWAV.getFactory(),
    };

    private static final ArrayList<String> sSupportedExtensions = new ArrayList<>();
    private static final HashMap<String, Factory> sExtensionMap =
            new HashMap<>();

    static {
        for (Factory f : sSubclassFactories) {
            for (String extension : f.getSupportedExtensions()) {
                sSupportedExtensions.add(extension);
                sExtensionMap.put(extension, f);
            }
        }
    }

    /**
     * Static method to create the appropriate CheapSoundFile subclass
     * given a filename.
     * <p/>
     * TODO: make this more modular rather than hardcoding the logic
     */
    public static CheapSoundFile create(String fileName,
                                        ProgressListener progressListener)
            throws
            IOException {
        File f = new File(fileName);
        if (!f.exists()) {
            //Log.e("Filename",fileName);
            throw new java.io.FileNotFoundException(fileName);
        }
        String name = f.getName().toLowerCase();
        String[] components = name.split("\\.");
     //   Log.v("Extensions",components + "");


        for (String monthName : components) {
            //Log.v("Extensions",monthName + "");
        }

        if (components.length < 2) {
            return null;
        }
        Factory factory = sExtensionMap.get(components[components.length - 1]);
        if (factory == null) {
            return null;
        }

        for (String monthName : components) {

                CheapSoundFile soundFile = factory.create();
            if (progressListener != null){
                soundFile.setProgressListener(progressListener);
            }

                soundFile.ReadFile(f);
                return soundFile;

        }

        return  null;
    }

    public static boolean isFilenameSupported(String filename) {
        String[] components = filename.toLowerCase().split("\\.");
        return components.length >= 2 && sExtensionMap.containsKey(components[components.length - 1]);
    }

    /**
     * Return the filename extensions that are recognized by one of
     * our subclasses.
     */
    public static String[] getSupportedExtensions() {
        return sSupportedExtensions.toArray(
                new String[sSupportedExtensions.size()]);
    }

    ProgressListener mProgressListener = null;
    File mInputFile = null;

    CheapSoundFile() {
    }

    void ReadFile(File inputFile)
            throws
            IOException {
        mInputFile = inputFile;
    }

    private void setProgressListener(ProgressListener progressListener) {
        mProgressListener = progressListener;
    }

    public int getNumFrames() {
        return 0;
    }

    public int getSamplesPerFrame() {
        return 0;
    }

    public int[] getFrameGains() {
        return null;
    }

    public int getFileSizeBytes() {
        return 0;
    }

    public int getAvgBitrateKbps() {
        return 0;
    }

    public int getSampleRate() {
        return 0;
    }

    public int getChannels() {
        return 0;
    }

    public String getFiletype() {
        return "Unknown";
    }

    /**
     * If and only if this particular file format supports seeking
     * directly into the middle of the file without reading the rest of
     * the header, this returns the byte offset of the given frame,
     * otherwise returns -1.
     */
    public int getSeekableFrameOffset(int frame) {
        return -1;
    }

    private static final char[] HEX_CHARS = {
            '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    public static String bytesToHex(byte hash[]) {
        char buf[] = new char[hash.length * 2];
        for (int i = 0, x = 0; i < hash.length; i++) {
            buf[x++] = HEX_CHARS[(hash[i] >>> 4) & 0xf];
            buf[x++] = HEX_CHARS[hash[i] & 0xf];
        }
        return new String(buf);
    }

    public void writewavfile(File outputFile, int startFrame, int numFrames, boolean fadeIn, boolean fadeOut, int fadeTime)
            throws IOException {

        com.ringtone.maker.SoundEditor.CheapAAC cheap = (com.ringtone.maker.SoundEditor.CheapAAC) this;
        cheap.WriteFile(outputFile, startFrame, numFrames, fadeIn, fadeOut, fadeTime);

    }

    public void WriteFile(File outputFile, int startFrame, int numFrames, boolean fadeIn, boolean fadeOut, int fadeTime)
            throws IOException {

        if (this instanceof com.ringtone.maker.SoundEditor.CheapAAC) {
            com.ringtone.maker.SoundEditor.CheapAAC cheap = (com.ringtone.maker.SoundEditor.CheapAAC) this;
            cheap.WriteFile(outputFile, startFrame, numFrames, fadeIn, fadeOut, fadeTime);
        } else if (this instanceof com.ringtone.maker.SoundEditor.CheapAMR) {
            com.ringtone.maker.SoundEditor.CheapAMR cheap = (CheapAMR) this;
            cheap.WriteFile(outputFile, startFrame, numFrames, fadeIn, fadeOut, fadeTime);
        } else if (this instanceof com.ringtone.maker.SoundEditor.CheapMP3) {
            com.ringtone.maker.SoundEditor.CheapMP3 cheap = (CheapMP3) this;
            cheap.WriteFile(outputFile, startFrame, numFrames, fadeIn, fadeOut, fadeTime);
        } else if (this instanceof com.ringtone.maker.SoundEditor.CheapWAV) {
            com.ringtone.maker.SoundEditor.CheapWAV cheap = (CheapWAV) this;
            cheap.WriteFile(outputFile, startFrame, numFrames, fadeIn, fadeOut, fadeTime);
        }
    }





    public void SaveCache(String filePath ) throws IOException {

        File WaveFormFile = new File(UILapplication.instance.getCacheDir(), filePath.hashCode()+"");
        if (WaveFormFile.exists()){
            WaveFormFile.delete();
        }

        boolean newFile = WaveFormFile.createNewFile();

        try {
            PrintWriter pw = new PrintWriter(new FileOutputStream(WaveFormFile.getAbsolutePath()) );
            int i = 0;
            while (i < measuredWidth) {
                i++;
                float h = (getScaledHeight(mZoomFactorByZoomLevel[0], i));
                pw.println(h);
            }
            pw.close();
            CacheSaved = true;
        } catch (IOException e) {
            e.printStackTrace();
        }



    }


    protected float getGain(int i, int numFrames, int[] frameGains) {
        int x = Math.min(i, numFrames - 1);
        if (numFrames < 2) {
            return frameGains[x];
        } else {
            if (x == 0) {
                return (frameGains[0] / 2.0f) + (frameGains[1] / 2.0f);
            } else if (x == numFrames - 1) {
                return (frameGains[numFrames - 2] / 2.0f) + (frameGains[numFrames - 1] / 2.0f);
            } else {


                if (x < 0){
                    //  SendErrorReport();
                    return 0;
                }
                else {
                    return (frameGains[x - 1] / 3.0f) + (frameGains[x] / 3.0f) + (frameGains[x + 1] / 3.0f);
                }

            }
        }
    }

    protected float getHeight(int i, int numFrames, int[] frameGains, float scaleFactor, float minGain, float range) {
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
    public void computeDoublesForAllZoomLevels() {
        int numFrames = getNumFrames();

        // Make sure the range is no more than 0 - 255

        float maxGain = 1.0f;
        for (int i = 0; i < numFrames; i++) {
            float gain = getGain(i, numFrames, getFrameGains());
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
            int smoothedGain = (int) (getGain(i, numFrames, getFrameGains()) * scaleFactor);
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

        mNumZoomLevels = 4;
        mLenByZoomLevel = new int[4];
        mZoomFactorByZoomLevel = new float[4];

        float ratio = measuredWidth / (float) numFrames;

        if (ratio < 1) {
            mLenByZoomLevel[0] = Math.round(numFrames * ratio);
            mZoomFactorByZoomLevel[0] = ratio;

            mLenByZoomLevel[1] = numFrames;
            mZoomFactorByZoomLevel[1] = 1.0f;

            mLenByZoomLevel[2] = numFrames * 2;
            mZoomFactorByZoomLevel[2] = 2.0f;

            mLenByZoomLevel[3] = numFrames * 3;
            mZoomFactorByZoomLevel[3] = 3.0f;

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

            mZoomLevel = 0;
            for (int i = 0; i < 4; i++) {
                if (mLenByZoomLevel[mZoomLevel] - measuredWidth > 0) {
                    break;
                } else {
                    mZoomLevel = i;
                }
            }
        }

        mInitialized = true;
    }

    protected float getZoomedInHeight(float zoomLevel, int i) {
        int f = (int) zoomLevel;
        if (i == 0) {
            return 0.5f * getHeight(0, getNumFrames(), getFrameGains(), scaleFactor, minGain, range);
        }
        if (i == 1) {
            return getHeight(0, getNumFrames(), getFrameGains(), scaleFactor, minGain, range);
        }
        if (i % f == 0) {
            float x1 = getHeight(i / f - 1, getNumFrames(), getFrameGains(), scaleFactor, minGain, range);
            float x2 = getHeight(i / f, getNumFrames(), getFrameGains(), scaleFactor, minGain, range);
            return 0.5f * (x1 + x2);
        } else if ((i - 1) % f == 0) {
            return getHeight((i - 1) / f, getNumFrames(), getFrameGains(), scaleFactor, minGain, range);
        }
        return 0;
    }

    protected float getZoomedOutHeight(float zoomLevel, int i) {
        int f = (int) (i / zoomLevel);
        float x1 = getHeight(f, getNumFrames(), getFrameGains(), scaleFactor, minGain, range);
        float x2 = getHeight(f + 1, getNumFrames(), getFrameGains(), scaleFactor, minGain, range);
        return 0.5f * (x1 + x2);
    }

    protected float getNormalHeight(int i) {
        return getHeight(i, getNumFrames(), getFrameGains(), scaleFactor, minGain, range);
    }

    public float getScaledHeight(float zoomLevel, int i) {
        if (zoomLevel == 1.0) {
            return getNormalHeight(i);
        } else if (zoomLevel < 1.0) {
            return getZoomedOutHeight(zoomLevel, i);
        }
        return getZoomedInHeight(zoomLevel, i);
    }





    public void GetMaxHeight() {

        // neglecting the first and last frames
        int toleranceRat = (mLenByZoomLevel[mZoomLevel] * 0 / 100);

        for (int i = toleranceRat; i < mLenByZoomLevel[mZoomLevel] - toleranceRat; i++) {
            int h = (int) (getScaledHeight(mZoomFactorByZoomLevel[mZoomLevel], i) * 1200 / 2);

            if (h > maxheight && h < 1200 / 2) {
                maxheight = h;
            }

            //   Log.v("maxheight", maxheight + "");


        }


    }

    public int pixelsToMillisecs(int pixels) {
        double z = mZoomFactorByZoomLevel[mZoomLevel];
        return (int) (pixels * (1000.0 * getSamplesPerFrame()) / (getSampleRate() * z) + 0.5);
    }


    public int secondsToFrames(double seconds) {
        return (int) (1.0 * seconds * this.getSampleRate() / getSamplesPerFrame() + 0.5);
    }


    public void FilterData(){

        GetMaxHeight();
        dataPointsArrayList = new ArrayList<>();

        // neglecting the first and last frames
        int toleranceRat = (mLenByZoomLevel[mZoomLevel] * 8 / 100);
        //   Log.e("toleranceRat", toleranceRat + "");

        int j = 0;
        while (dataPointsArrayList.size() < toleranceRat) {

            j++;

            dataPointsArrayList.clear();
            for (int i = 0; i < mLenByZoomLevel[mZoomLevel]; i++) {
                int h = (int) (getScaledHeight(mZoomFactorByZoomLevel[mZoomLevel], i) * 1200 / 2);

                //get the amplitude that above the percentage of the max height
                int maxheightprecentage = maxheight * (filterPercentage - j);

                if ( h == 0 || maxheightprecentage == 0 ){
                    return;
                }

                //get the amplitude that above the percentage of the max height
                if (((h / (maxheightprecentage / 100)) > 0) && (h < 1200 / 2)) {
                    SelectionPoint data = new SelectionPoint(i, h, pixelsToMillisecs(i), 0);
                    dataPointsArrayList.add(data);
                    //    Log.v("Height", h + "");

                }


            }
        }

    }





}
