/*
 * *
 *  * Created by Youssef Assad on 6/2/18 11:17 AM
 *  * Copyright (c) 2018 . All rights reserved.
 *  * Last modified 6/2/18 11:04 AM
 *
 */

package com.ringtone.maker.Database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.ringtone.maker.R
import com.ringtone.maker.Models.MusicFile
import com.ringtone.maker.Utils.SharedPref
import org.jetbrains.anko.db.*
import java.io.File
import java.util.*



/**
 * Created by Joseph27 on 2/28/16.
 */
class DBHelper(internal var context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, 1) {

    internal var TAG = "SQLiteOpenHelper"
    internal var sharedPref: SharedPref

    init {
        sharedPref = SharedPref(context)
    }


    val musicFromDB: ArrayList<MusicFile>
        get() {
            val db = this.writableDatabase
            val array_list = ArrayList<MusicFile>()
            db.select(MUSIC_TABLE_NAME).orderBy(MUSIC_COLUMN_Date_Added,SqlOrderDirection.DESC).exec {
                  moveToFirst()
                    while (!isAfterLast) { val file = File(getString(getColumnIndex(MUSIC_COLUMN_path)))
                        if (file.exists()) {
                            if (!file.absolutePath.contains(context.getString(R.string.app_name))) array_list.add(GetsongFromCurser(this))
                        } else db.delete(MUSIC_TABLE_NAME, MUSIC_COLUMN_path + "=?", arrayOf(getString(getColumnIndex(MUSIC_COLUMN_path))))
                        moveToNext()
                    }
                close()
            }
            return array_list
        }

    val historyRecords: ArrayList<MusicFile>
        get() {
            val db = this.writableDatabase
            val array_list = ArrayList<MusicFile>()
            db.select("$Music_Table_Records").whereArgs("($MUSIC_Record_Selected = {selection})",
                            "selection" to "1").orderBy(MUSIC_Record_Date,SqlOrderDirection.DESC).exec {
              moveToFirst()
                while (isAfterLast == false) {
                    val file = File(getString(getColumnIndex(MUSIC_Record_path)))
                    if (file.exists()) {
                        val nSong = GetsongFromHistoryCurser(this)
                        array_list.add(nSong)
                    } else {
                        db.delete(Music_Table_Records, MUSIC_Record_path + "=?", arrayOf(getString(getColumnIndex(MUSIC_Record_path))))
                    }
                    moveToNext()
                }
                close()

            }
            return array_list

        }


    override fun onCreate(db: SQLiteDatabase) {

        db.createTable(Music_Table_Records, true,
                MUSIC_Record_ID to INTEGER + PRIMARY_KEY + UNIQUE,
                MUSIC_Record_title to TEXT,
                MUSIC_Record_path to TEXT,
                MUSIC_Record_Type to TEXT,
                MUSIC_Record_Date to TEXT,
                MUSIC_Record_Selected to INTEGER,
                MUSIC_Record_Suggested to INTEGER,
                MUSIC_Record_BlackList to INTEGER,
                MUSIC_Record_Duration to INTEGER

        )

        db.createTable(MUSIC_TABLE_NAME, true,
                MUSIC_COLUMN_ID to INTEGER + PRIMARY_KEY + UNIQUE,
                MUSIC_COLUMN_title to TEXT,
                MUSIC_COLUMN_path to TEXT,
                MUSIC_COLUMN_albumart to TEXT,
                MUSIC_COLUMN_Notification_Displayed to INTEGER,
                MUSIC_COLUMN_Ringtone_Displayed to INTEGER,
                MUSIC_COLUMN_Alarm_Displayed to INTEGER,
                MUSIC_COLUMN_Duration to INTEGER,
                MUSIC_COLUMN_BlackList to INTEGER,
                MUSIC_COLUMN_SIZE to INTEGER,
                MUSIC_COLUMN_playcount to INTEGER,
                MUSIC_COLUMN_COMPOSER to TEXT,
                MUSIC_COLUMN_Year to INTEGER,
                MUSIC_COLUMN_Album to TEXT,
                MUSIC_COLUMN_Artist to TEXT,
                MUSIC_COLUMN_Type to TEXT,
                MUSIC_COLUMN_block_suggestion to INTEGER,
                MUSIC_COLUMN_Date_Added to INTEGER
        )
    }

    override fun onUpgrade(sqLiteDatabase: SQLiteDatabase, i: Int, i1: Int) {
        sqLiteDatabase.dropTable("$MUSIC_TABLE_NAME", true)
        sqLiteDatabase.dropTable("$Music_Table_Records", true)
        onCreate(sqLiteDatabase)
    }

    fun insertSong(title: String?, path: String?, albumart: String?, DateAdded: Long?, duration: Int?, size: Long?, year: Int?, composer: String?, album: String?, artist: String?): Boolean {
        val db = this.writableDatabase
        db.insert(MUSIC_TABLE_NAME,
                MUSIC_COLUMN_title to title,
                MUSIC_COLUMN_path to path,
                MUSIC_COLUMN_albumart to albumart,
                MUSIC_COLUMN_Date_Added to DateAdded,
                MUSIC_COLUMN_Notification_Displayed to 0,
                MUSIC_COLUMN_Ringtone_Displayed to 0,
                MUSIC_COLUMN_Alarm_Displayed to 0,
                MUSIC_COLUMN_Duration to duration,
                MUSIC_COLUMN_SIZE to size,
                MUSIC_COLUMN_Artist to artist,
                MUSIC_COLUMN_COMPOSER to composer,
                MUSIC_COLUMN_Album to album,
                MUSIC_COLUMN_Year to year,
                MUSIC_COLUMN_block_suggestion to 0
        )

        return true
    }


    // Getting single contact
    fun checkandinsert(title: String?, path: String?, albumart: String?, dateadded: Long?, duration: Int?, size: Long?, year: Int?, composer: String?, album: String?, artist: String?): Boolean {
        val db = this.readableDatabase
        val Query = "Select * from $MUSIC_TABLE_NAME where $MUSIC_COLUMN_path = ?;"
        var cursor: Cursor? = null
        try {
            cursor = db.rawQuery(Query, arrayOf(path))
            if (cursor!!.count <= 0) {
                insertSong(title, path, albumart, dateadded, duration, size, year, composer, album, artist)
                return false
            }
        } finally {
            cursor?.close()
        }
        return true
    }

    //increase play count

    fun IncreasePlayCount(Path: String) {
        val database = this.writableDatabase
        var playcount = getMusicplaycount(Path)
        val playcountincreament = ++playcount
        val values = ContentValues()
        values.put(MUSIC_COLUMN_playcount, playcountincreament)
        database.update(MUSIC_TABLE_NAME, values, MUSIC_COLUMN_path + " = ?", arrayOf(Path))
        database.close()
    }

    private fun getMusicplaycount(Path: String): Int {
        val db = this.readableDatabase
        val Query = "Select * from $MUSIC_TABLE_NAME where $MUSIC_COLUMN_path = ?;"
        val cursor = db.rawQuery(Query, arrayOf(Path))
        if (cursor.count > 0) {
            cursor.moveToFirst()
            val count = cursor.getInt(cursor.getColumnIndex(MUSIC_COLUMN_playcount))
            return if (count != 0) {
                count
            } else {
                0
            }

        }

        DBHelper.CloseCursor(cursor)


        return 0
    }


    /**
     *
     * @param Date
     * Filter the Music Database by Date such as 30 days ago
     * and with Seleciton of Displayed count
     *
     * @return
     * return the first song in the cursor selection , Null if no music
     */
    fun FilterMusicByDate(Date: Long?, SongType: String): MusicFile? {
        val date = System.currentTimeMillis() / 1000 - 30 * 24 * 60 * 60
        var song: MusicFile? = null
        var cursor: Cursor? = null
        var RecordCursor: Cursor? = null
        var DateCursor: Cursor? = null

        try {
            val db = this.readableDatabase
            val Query = "Select * from $MUSIC_TABLE_NAME where $MUSIC_COLUMN_Date_Added >?  AND $MUSIC_COLUMN_Duration >?  AND $MUSIC_COLUMN_Duration <?  ORDER BY $MUSIC_COLUMN_playcount DESC"
            cursor = db.rawQuery(Query, arrayOf(Date!!.toString(), "30000", "600000"))
            cursor!!.moveToFirst()
            while (!cursor.isAfterLast) {

                // check if the song is already selected in the device
                if (sharedPref.LoadString(SongType, "") == cursor.getString(cursor.getColumnIndex(MUSIC_COLUMN_title))) {
                    cursor.moveToNext()
                } else {

                    try {
                        // check if the select song is in the Record table
                        val RecordQuery = "Select * from $Music_Table_Records where $MUSIC_Record_path = ? ;"
                        RecordCursor = db.rawQuery(RecordQuery, arrayOf(cursor.getString(cursor.getColumnIndex(MUSIC_COLUMN_path))))
                        if (RecordCursor!!.count == 0) {
                            song = GetsongFromCurser(cursor)
                            DBHelper.CloseCursor(RecordCursor)
                            return song

                        } else {
                            val DateQuery = "Select * from $Music_Table_Records where $MUSIC_Record_path = ? AND $MUSIC_Record_Date <= $date AND $MUSIC_Record_Suggested = 0"
                            DateCursor = db.rawQuery(DateQuery, arrayOf(cursor.getString(cursor.getColumnIndex(MUSIC_COLUMN_path))))
                            if (DateCursor!!.count == 0) {
                                cursor.moveToNext()
                            } else {
                                song = GetsongFromCurser(cursor)
                                DBHelper.CloseCursor(RecordCursor)
                                return song
                            }

                        }

                    } finally {
                        CloseCursor(RecordCursor)
                        DBHelper.CloseCursor(DateCursor)
                    }


                }
            }

            DBHelper.CloseCursor(cursor)


        } finally {
            cursor?.close()
        }


        return song

    }


    fun FilterMusicByDate(mlist: ArrayList<MusicFile>): ArrayList<MusicFile> {

        var song: MusicFile?
        var cursor: Cursor? = null
        var res: Cursor? = null
        val db = this.readableDatabase
        try {
            cursor = db.rawQuery("Select * from $MUSIC_TABLE_NAME where $MUSIC_COLUMN_block_suggestion = 0  ORDER BY $MUSIC_COLUMN_playcount DESC", null)
            // check if the list count is 0 and the orignal count is 0 to reset the suggestionblock
            if (cursor!!.count == 0) {
                res = db.rawQuery("select * from $MUSIC_TABLE_NAME  ORDER BY $MUSIC_COLUMN_Date_Added DESC", null)
                if (res!!.count != 0) {
                    ResetSuggestionBlock()
                    FilterMusicByDate(mlist)
                }

            }
            cursor.moveToFirst()

            while (!cursor.isAfterLast) {
                val file = File(cursor.getString(cursor.getColumnIndex(MUSIC_COLUMN_path)))
                if (file.exists()) {
                    song = GetsongFromCurser(cursor)
                    mlist.add(song)
                }
                cursor.moveToNext()
            }


        } finally {
            CloseCursor(res)
            CloseCursor(cursor)
        }

        return mlist

    }


    // general user activity count
    fun MarkSongAsAlerted(title: String?, Type: String?, duration: Int?, Date: Long?, Path: String?, Alerted: Boolean?) {
        val database = this.writableDatabase
        val values = ContentValues()
        values.put(MUSIC_Record_title, title)
        values.put(MUSIC_Record_Date, Date)
        values.put(MUSIC_Record_Duration, duration)
        values.put(MUSIC_Record_Type, Type)
        values.put(MUSIC_Record_path, Path)
        if (Alerted == true) {
            values.put(MUSIC_Record_Suggested, 1)
        } else {
            values.put(MUSIC_Record_Suggested, 0)
        }


        if (CheckIsDataAlreadyInDBorNot(Path)){
            database.update(Music_Table_Records,values,"$MUSIC_Record_path =  $Path", null)
        }else {
            database.insertWithOnConflict(Music_Table_Records, " $MUSIC_Record_path =  $Path", values, SQLiteDatabase.CONFLICT_REPLACE) as Long
        }

//
//        val id =  database.insertWithOnConflict(Music_Table_Records, "$MUSIC_Record_path =  $Path", values, SQLiteDatabase.CONFLICT_ABORT) as Long
//        if (id.toInt() == -1) database.update(Music_Table_Records,values,"$MUSIC_Record_path =  $Path", null)
////        database.updateWithOnConflict(Music_Table_Records, values, MUSIC_Record_path + " = ?", arrayOf(Path.toString()),SQLiteDatabase.CONFLICT_REPLACE)
//        Log.e(TAG, "MarkSongAsAlerted: duration" + duration!! + "  id " + id)

    }


    // general user activity count
    fun MarkSongAsSelected(title: String?, Type: String?, duration: Int?, Date: Long?, Path: String?, Selected: Boolean?) {
        val database = this.writableDatabase
        val values = ContentValues()
        values.put(MUSIC_Record_title, title)
        values.put(MUSIC_Record_Date, Date)
        values.put(MUSIC_Record_Duration, duration)
        values.put(MUSIC_Record_Type, Type)
        values.put(MUSIC_Record_path, Path)
        values.put(MUSIC_Record_Suggested, 1)
        if (Selected == true) {
            values.put(MUSIC_Record_Selected, 1)
        } else {
            values.put(MUSIC_Record_Selected, 0)
        }

        if (CheckIsDataAlreadyInDBorNot(Path)){
            database.update(Music_Table_Records,values,"$MUSIC_Record_path =  $Path", null)
        }else {
            database.insertWithOnConflict(Music_Table_Records, " $MUSIC_Record_path =  $Path", values, SQLiteDatabase.CONFLICT_REPLACE) as Long
        }

    }


    fun SearchSongByTitle(title: String, songArrayList: ArrayList<MusicFile>): ArrayList<MusicFile> {
        val db = this.readableDatabase
        val Query = "Select * from $MUSIC_TABLE_NAME where $MUSIC_COLUMN_title LIKE ? "
        val cursor = db.rawQuery(Query, arrayOf("%$title%"))
        cursor.moveToFirst()
        while (cursor.isAfterLast == false) {
            val song = GetsongFromCurser(cursor)
            songArrayList.add(song)
            cursor.moveToNext()
        }
        return songArrayList
    }


    fun CheckIsDataAlreadyInDBorNot(fieldValue: String?): Boolean {
        val db = this.writableDatabase
        var result = true;
        db.select("$Music_Table_Records").whereArgs("($MUSIC_Record_path = {path})",
                "path" to String).exec {
            if (getCount() <= 0) {
                close()
                result = false
            }
            close()


        }
        return result

    }
    private fun ResetSuggestionBlock() {
        val database = this.writableDatabase
        val cv = ContentValues()
        cv.put(MUSIC_COLUMN_block_suggestion, 0)
        database.update(MUSIC_TABLE_NAME, cv, null, null)
    }


    fun GetsongFromCurser(cursor: Cursor): MusicFile {
        return MusicFile(
                cursor.getString(cursor.getColumnIndex(MUSIC_COLUMN_albumart)),
                cursor.getString(cursor.getColumnIndex(MUSIC_COLUMN_title)),
                cursor.getString(cursor.getColumnIndex(MUSIC_COLUMN_path)),
                cursor.getLong(cursor.getColumnIndex(MUSIC_COLUMN_Date_Added)),
                cursor.getInt(cursor.getColumnIndex(MUSIC_COLUMN_Duration)),
                cursor.getLong(cursor.getColumnIndex(MUSIC_COLUMN_SIZE)),
                cursor.getInt(cursor.getColumnIndex(MUSIC_COLUMN_Year)),
                cursor.getString(cursor.getColumnIndex(MUSIC_COLUMN_COMPOSER)),
                cursor.getString(cursor.getColumnIndex(MUSIC_COLUMN_Album)),
                cursor.getString(cursor.getColumnIndex(MUSIC_COLUMN_Artist)),
                cursor.getString(cursor.getColumnIndex(MUSIC_COLUMN_Type)))
    }


    fun GetsongFromHistoryCurser(cursor: Cursor): MusicFile {
        return MusicFile(null,
                cursor.getString(cursor.getColumnIndex(MUSIC_Record_title)),
                cursor.getString(cursor.getColumnIndex(MUSIC_Record_path)),
                cursor.getLong(cursor.getColumnIndex(MUSIC_Record_Date)),
                cursor.getInt(cursor.getColumnIndex(MUSIC_Record_Duration)), null, null, null, null, null, cursor.getString(cursor.getColumnIndex(MUSIC_Record_Type)))
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var sInstance: DBHelper? = null

        // database name
        val DATABASE_NAME = "MusicDB.db"
        // database tablets
        val MUSIC_TABLE_NAME = "music"
        val Music_Table_Records = "music_records"

        /**
         * Table Contain all the Music Data and Play count of Each Music File
         */
        val MUSIC_COLUMN_ID = "_id"
        val MUSIC_COLUMN_title = "title"
        val MUSIC_COLUMN_path = "path"
        val MUSIC_COLUMN_SIZE = "size"
        val MUSIC_COLUMN_COMPOSER = "composer"
        val MUSIC_COLUMN_Year = "year"
        val MUSIC_COLUMN_Album = "album"
        val MUSIC_COLUMN_Artist = "artist"
        val MUSIC_COLUMN_albumart = "albumart"
        val MUSIC_COLUMN_playcount = "playcount"
        val MUSIC_COLUMN_Date_Added = "data_added"
        val MUSIC_COLUMN_Notification_Displayed = "notification_displayed"
        val MUSIC_COLUMN_Alarm_Displayed = "alarm_displayed"
        val MUSIC_COLUMN_Ringtone_Displayed = "ringtone_displayed"
        val MUSIC_COLUMN_BlackList = "blackList"
        val MUSIC_COLUMN_Duration = "duration"
        val MUSIC_COLUMN_Type = "type"
        val MUSIC_COLUMN_block_suggestion = "suggestion_block"


        /**
         * record history of all the user actions
         */
        val MUSIC_Record_ID = "_id"
        val MUSIC_Record_title = "title"
        val MUSIC_Record_path = "path"
        val MUSIC_Record_Type = "type"
        val MUSIC_Record_Date = "date"
        val MUSIC_Record_Suggested = "suggested"
        val MUSIC_Record_Selected = "selected"
        val MUSIC_Record_Duration = "duration"
        val MUSIC_Record_BlackList = "blackList"


        @Synchronized
        fun getInstance(context: Context): DBHelper {
            if (sInstance == null) {
                sInstance = DBHelper(context.applicationContext)
            }
            return sInstance!!
        }


        fun CloseCursor(cursor: Cursor?) {
            cursor?.close()
        }
    }


}
