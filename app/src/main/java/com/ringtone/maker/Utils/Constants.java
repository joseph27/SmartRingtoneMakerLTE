/*
 * *
 *  * Created by Youssef Assad on 6/2/18 11:17 AM
 *  * Copyright (c) 2018 . All rights reserved.
 *  * Last modified 6/2/18 11:04 AM
 *
 */

package com.ringtone.maker.Utils;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.File;

/**
 * Created by Joseph27 on 3/4/16.
 */
public class Constants {


    //Intro
    public static String FIRST_TIME = "FIRST_TIME";


    // constants Object Data
    public static String NOTIFICATION_KEY = "NOTIFICATION";
    public static String RINGTONE_KEY = "RINGTONE";
    public static String ALARM_KEY = "ALARM";
    public static String Default_Shared_Value = "NONE";
    public static String KEY_REC_CHECK = "KEY_REC_CHECK";
    public static String KEY_SHOWADS = "KEY_SHOWADS";

    //Notification
    public static int NOTIFICATION_ID = 2727;

    //alertdialog keys
    public static String KEY_TONE_DURATION = "ToneDuration";
    public static String KEY_SAVED_RINGTONE_PATH = "RingtonePath";

    // AlarmSchedule
    public static String Displayed_Check_key = "Alert_Displayed";
    public static String MONTHLY_ALARM = "Monthly";
    public static String WEEKLY_ALARM = "Weekly";
    public static String BIWEEKLY_ALARM = "Biweekly";


    //firebase nodes
    public static String FIREBASE_MUISC_YEAR_NODE = "Year";
    public static String FIREBASE_MUISC_ARTIST_NODE = "ARTIST";
    public static String FIREBASE_MUISC_DURATION_NODE = "DURATION";
    public static String FIREBASE_MUISC_TITLE_NODE = "Title";
    public static String FIREBASE_MUISC_TYPE_NODE = "Type";
    public static String FIREBASE_MUISC_START_POINT_NODE = "startpoint";
    public static String FIREBASE_MUISC_END_POINT_NODE = "endpoint";
    public static String FIREBASE_MUISC_PATH_NODE = "Path";
    public static String FIREBASE_MUISC_ALBUM_NODE = "ALBUM";
    public static String FIREBASE_MUISC_DATA_NODE = "MusicData";
    public static String FIREBASE_MUISC_FEEDBACK_NODE = "Feedback";
    public static String FIREBASE_MUISC_FEEDBACK_TIMESTAMP_NODE = "timestamp";
    public static String FIREBASE_MUISC_FEEDBACK_FEEDBACK_ID_NODE = "feedback_id";
    public static String FIREBASE_MUISC_FEEDBACK_DESCRIPTION_NODE = "description";
    public static String FIREBASE_MUISC_FEEDBACK_TITLE_NODE = "title";







}
