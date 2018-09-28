/*
 * *
 *  * Created by Youssef Assad on 6/2/18 11:17 AM
 *  * Copyright (c) 2018 . All rights reserved.
 *  * Last modified 6/2/18 11:04 AM
 *
 */

package com.ringtone.maker.Utils

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore

import com.ringtone.maker.Database.DBHelper

/**
 * Created by Joseph27 on 5/6/16.
 */
class MediaStoreFetcher(internal var mContext: Context) {
    internal var supportedformats = arrayOf("aac", "m4a", "AMR", "mp3", "wav")


    init {
        getMusicData()
    }

    fun getMusicData() {
        val dbHelper = DBHelper.getInstance(mContext)
        var cursor: Cursor? = null
        try {

            val selection = MediaStore.Audio.Media.IS_MUSIC + " != 0"
            val orderby = MediaStore.Audio.Media.DATE_ADDED + " DESC"
            val projection = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.ALBUM_ID, MediaStore.Audio.Media.DISPLAY_NAME, MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.DATE_ADDED, MediaStore.Audio.Media.DATE_MODIFIED, MediaStore.MediaColumns.SIZE, MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.COMPOSER, MediaStore.Audio.Media.YEAR)
            cursor = mContext.contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, null, orderby)

            if (cursor != null && cursor.count > 0) {
                while (cursor.moveToNext()) {
                    val sArtworkUri = Uri.parse("content://media/external/audio/albumart")
                    val uri = ContentUris.withAppendedId(sArtworkUri, cursor.getLong(4))
                    if (checkifFileissuppported(cursor.getString(3)) && cursor.getString(6) != null && Integer.parseInt(cursor.getString(6)) / 1000 >= 30) {
                        dbHelper.checkandinsert(
                                cursor.getString(2), // title
                                cursor.getString(3), // path
                                uri.toString(), //albumart
                                cursor.getLong(7), //dateadded
                                cursor.getInt(6), //duration
                                cursor.getLong(8), // size
                                cursor.getInt(13), // year
                                cursor.getString(12), // composer
                                cursor.getString(10), //album
                                cursor.getString(1)//artist
                        ) //size
                    }

                }
            }

        } finally {
            DBHelper.CloseCursor(cursor)
        }

    }


    fun checkifFileissuppported(filename: String): Boolean {
        var i = 0
        while (i < supportedformats.size) {
            if (filename.endsWith(supportedformats[i])) {
                return true
            }
            i++

        }
        return false
    }


}
