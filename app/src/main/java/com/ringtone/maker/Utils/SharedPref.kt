package com.ringtone.maker.Utils

import android.content.Context
import android.preference.PreferenceManager

class SharedPref(internal var context: Context?) {

    fun SaveString(key: String, value: String?) =  PreferenceManager.getDefaultSharedPreferences(context).edit().putString(key, value!!).apply()
    fun SaveBoolean(key: String, value: Boolean?) =  PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(key, value!!).apply()
    fun SaveInt(key: String, value: Int) = PreferenceManager.getDefaultSharedPreferences(context).edit().putInt(key, value).apply()
    fun LoadInt(key: String, DefaultValue: Int): Int  = PreferenceManager.getDefaultSharedPreferences(context).getInt(key, DefaultValue)
    fun LoadLong(key: String): Long = PreferenceManager.getDefaultSharedPreferences(context).getLong(key, 0)
    fun LoadBoolean(key: String, Default: Boolean?): Boolean?  = PreferenceManager .getDefaultSharedPreferences(context).getBoolean(key, Default!!)
    fun LoadString(key: String, DefaultValue: String?): String  = PreferenceManager.getDefaultSharedPreferences(context).getString(key, DefaultValue)
    fun SaveLong(key: String, value: Long)  = PreferenceManager.getDefaultSharedPreferences(context).edit().putLong(key, value).apply()
    fun RemoveValue(key: String )  = PreferenceManager.getDefaultSharedPreferences(context).edit().remove(key).apply()

}
