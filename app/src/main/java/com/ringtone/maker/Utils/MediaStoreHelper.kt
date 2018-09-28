package com.ringtone.maker.Utils

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.support.v4.content.CursorLoader

/**
 * Created by Joseph27 on 5/21/18.
 */

object MediaStoreHelper {

    fun getRealPathFromURI(context: Context, contentUri: Uri): String? {
        // TODO
        val proj = arrayOf(MediaStore.Audio.Media.DATA)
        val loader = CursorLoader(context, contentUri, proj, null, null, null)
        val cursor = loader.loadInBackground()
        val column_index = cursor!!.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
        if (cursor != null && cursor.count > 0) {
            cursor.moveToFirst()
            return cursor.getString(column_index)
        } else return null


    }


}
