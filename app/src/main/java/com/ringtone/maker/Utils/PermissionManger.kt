package com.ringtone.maker.Utils

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import com.ringtone.maker.R


/**
 * Created by Joseph27 on 5/13/16.
 */
object PermissionManger {


    val REQUEST_ID_READ_CONTACTS_PERMISSION = 47


     fun checkPermission(activity: Activity, Permission: String): Boolean {
        val result = ContextCompat.checkSelfPermission(activity, Permission)
        return result == PackageManager.PERMISSION_GRANTED
    }

    fun requestPermission(activity: Activity, Permission: String) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Permission)) {
            showStorageDialog(activity)
        } else {
            ActivityCompat.requestPermissions(activity, arrayOf(Permission), 1)
        }
    }

    fun showStorageDialog(mactivity: Activity) {
        val builder = AlertDialog.Builder(mactivity)
        builder.setTitle(mactivity.getString(R.string.application_permission))
        builder.setMessage(mactivity.getString(R.string.permission_description))
        val positiveText = "Ok"
        builder.setPositiveButton(positiveText
        ) { dialog, which ->
            // positive button logic
            goToSettings(mactivity)
        }

        val negativeText = "Cancel"
        builder.setNegativeButton(negativeText
        ) { dialog, which ->
            //
            mactivity.finish()
            // negative button logic
        }

        val dialog = builder.create()
        // display dialog
        dialog.show()
    }

    private fun goToSettings(activity: Activity) {
        val myAppSettings = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + activity.packageName))
        myAppSettings.addCategory(Intent.CATEGORY_DEFAULT)
        myAppSettings.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        activity.startActivityForResult(myAppSettings, 1)
    }

    fun checkAndRequestContactsPermissions(activity: Activity?): Boolean {
        val modifyAudioPermission = ContextCompat.checkSelfPermission(activity!!, Manifest.permission.READ_CONTACTS)
        if (modifyAudioPermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.READ_CONTACTS),
                    REQUEST_ID_READ_CONTACTS_PERMISSION)
            return false
        }
        return true
    }




}
