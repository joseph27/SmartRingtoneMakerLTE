/*
 * *
 *  * Created by Youssef Assad on 6/2/18 11:17 AM
 *  * Copyright (c) 2018 . All rights reserved.
 *  * Last modified 6/2/18 11:04 AM
 *
 */

package com.ringtone.maker.Entities


import android.content.Context
import android.database.Cursor
import android.media.RingtoneManager
import android.provider.MediaStore
import com.ringtone.maker.Database.DBHelper
import com.ringtone.maker.Utils.Constants
import com.ringtone.maker.Utils.SharedPref

class ToneChangeTracker(internal var context: Context) {

    internal var dbHelper: DBHelper
    internal var sharedPref: SharedPref


    init {
        sharedPref = SharedPref(context)
        dbHelper = DBHelper.getInstance(context)
        val defaultRintoneUri = RingtoneManager.getActualDefaultRingtoneUri(context.applicationContext, RingtoneManager.TYPE_RINGTONE)
        val defaultNotificationUri = RingtoneManager.getActualDefaultRingtoneUri(context.applicationContext, RingtoneManager.TYPE_NOTIFICATION)
        val defaultAlarmUri = RingtoneManager.getActualDefaultRingtoneUri(context.applicationContext, RingtoneManager.TYPE_ALARM)

        val currentRingtonePath = sharedPref.LoadString(Constants.RINGTONE_KEY, Constants.Default_Shared_Value)
        val currentNotificationPath = sharedPref.LoadString(Constants.NOTIFICATION_KEY, Constants.Default_Shared_Value)
        val currentAlarmPath = sharedPref.LoadString(Constants.ALARM_KEY, Constants.Default_Shared_Value)

        val r = RingtoneManager.getRingtone(context, defaultRintoneUri)
        val ringToneName = r.getTitle(context)

        // check ringtone
            if (defaultRintoneUri?.toString() != currentRingtonePath && defaultRintoneUri?.toString() != Constants.Default_Shared_Value) FetchAndInsertdatafromDB(ringToneName, Constants.RINGTONE_KEY)


        // check notification

            val Notificaiton = RingtoneManager.getRingtone(context, defaultNotificationUri)
            val Notificaitonname = Notificaiton.getTitle(context)
            if (defaultNotificationUri?.toString() != currentNotificationPath && defaultNotificationUri?.toString() != Constants.Default_Shared_Value) FetchAndInsertdatafromDB(Notificaitonname, Constants.NOTIFICATION_KEY)


        // check alarm

            try {
                val Alarm = RingtoneManager.getRingtone(context, defaultAlarmUri)
                val AlarmName = Alarm.getTitle(context)
                if (defaultAlarmUri?.toString() != currentAlarmPath && defaultAlarmUri?.toString() != Constants.ALARM_KEY) FetchAndInsertdatafromDB(AlarmName, Constants.ALARM_KEY)
            } catch (e: Exception) {

            }




    }


    fun FetchAndInsertdatafromDB(title: String, Type: String) {
        var cursor: Cursor? = null
        val selection = MediaStore.Audio.Media.TITLE + "=?"
        val orderby = MediaStore.Audio.Media.DATE_ADDED + " DESC"
        val projection = arrayOf(MediaStore.Audio.Media.DATA)
        try {
            cursor = context.contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, arrayOf(title), orderby)
            if (cursor != null && cursor.count > 0) {
                while (cursor.moveToNext()) {
                    dbHelper.MarkSongAsAlerted(title, Type, 0,  System.currentTimeMillis() / 1000, cursor.getString(0), false)
                }
            }

        } finally {
            DBHelper.CloseCursor(cursor)
        }


    }
}
