/*
 * *
 *  * Created by Youssef Assad on 6/2/18 11:22 AM
 *  * Copyright (c) 2018 . All rights reserved.
 *  * Last modified 6/2/18 11:04 AM
 *
 */

package com.ringtone.maker.Receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore

/**
 * Created by Joseph27 on 2/28/16.
 */
class MusicActivityTracker : BroadcastReceiver() {


    override fun onReceive(context: Context, intent: Intent) {
        val track = intent.getStringExtra("track")
        val dbHelper = com.ringtone.maker.Database.DBHelper.getInstance(context)
        var path: String?
        if (track != null) {
            path = track.replace("'".toRegex(), "''")
            val c = query(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    arrayOf(MediaStore.Audio.Media.DATA), "${MediaStore.Audio.Media.TITLE}='$path'", null, null, 0)
            try {
                if (c == null || c.count == 0) return
                val size = c.count
                if (size != 1) return
                c.moveToNext()
                // Here's the song path
                val songPath = c.getString(0)
                // increase the record of playing for a certain music
                dbHelper.IncreasePlayCount(songPath)
            } finally {
                c?.close()
            }
        }

    }

    fun query(context: Context, uri: Uri, projection: Array<String>, selection: String, selectionArgs: Array<String>?, sortOrder: String?, limit: Int): Cursor? {
        var uri = uri
        try {
            val resolver = context.contentResolver ?: return null
            if (limit > 0) uri = uri.buildUpon().appendQueryParameter("limit", "" + limit).build()
            return resolver.query(uri, projection, selection, selectionArgs, sortOrder)
        } catch (ex: UnsupportedOperationException) {
            return null
        }

    }
}


