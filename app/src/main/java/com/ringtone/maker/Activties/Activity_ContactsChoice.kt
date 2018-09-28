

/*
 * *
 *  * Created by Youssef Assad on 6/2/18 11:20 AM
 *  * Copyright (c) 2018 . All rights reserved.
 *  * Last modified 6/2/18 11:04 AM
 *
 */

package com.ringtone.maker.Activties


import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.support.v4.view.MenuItemCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SearchView
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.ringtone.maker.R
import com.ringtone.maker.Adapters.Adapter_Contacts
import com.ringtone.maker.Utils.PermissionManger
import java.util.*


/**
 * After a ringtone has been saved, this activity lets you pick a contact
 * and assign the ringtone to that contact.
 */
class Activity_ContactsChoice : AppCompatActivity(), SearchView.OnQueryTextListener {
    private var mRingtoneUri: Uri? = null

    /**
     * Called when the activity is first created.
     */
    private var mToolbar: Toolbar? = null
    private var mSearchView: SearchView? = null
    private var mRecyclerView: RecyclerView? = null
    private var mAdapterContacts: Adapter_Contacts? = null
    private var mData: ArrayList<com.ringtone.maker.Models.Contacts>? = null

    public override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)

        mRingtoneUri = Uri.parse(intent.extras!!.getString(FILE_NAME))
        setContentView(R.layout.activity_choose_contact)
        mData = ArrayList()
        mToolbar = findViewById<View>(R.id.toolbar) as Toolbar?
        setSupportActionBar(mToolbar)
        supportActionBar!!.setTitle(R.string.contacts)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)


        mRecyclerView = findViewById<View>(R.id.recycler_view) as RecyclerView?
        mRecyclerView!!.layoutManager = LinearLayoutManager(applicationContext)
        mData = getContacts(this, "")
        mAdapterContacts = Adapter_Contacts(this, mData)
        mRecyclerView!!.adapter = mAdapterContacts
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_search, menu)
        mSearchView = MenuItemCompat.getActionView(menu.findItem(R.id.menu_search)) as SearchView
        mSearchView!!.setOnQueryTextListener(this)
        mSearchView!!.isIconified = false
        MenuItemCompat.setOnActionExpandListener(menu.findItem(R.id.menu_search), object : MenuItemCompat.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                finish()
                return false
            }
        })

        menu.findItem(R.id.menu_search).expandActionView()
        return super.onCreateOptionsMenu(menu)
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        return false
    }

    override fun onQueryTextChange(newText: String): Boolean {
        mData = getContacts(this, newText)
        mAdapterContacts!!.updateData(mData!!)
        return false
    }



    fun onItemClicked(adapterPosition: Int) {
        if (PermissionManger.checkAndRequestContactsPermissions(this)){
            val contactsModel = mData!![adapterPosition]
            val uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contactsModel.mContactId)
            val values = ContentValues()
            values.put(ContactsContract.Contacts.CUSTOM_RINGTONE, mRingtoneUri!!.toString())
            contentResolver.update(uri, values, null, null)
            val message = resources.getText(R.string.Edit_Done_Toast).toString() +
                    " " +
                    contactsModel.mName

            Toast.makeText(this, message, Toast.LENGTH_SHORT)
                    .show()
            finish()
        }


    }
    companion object {
        var FILE_NAME = "FILE_NAME"

        @SuppressLint("Recycle")
        fun getContacts(context: Context, searchQuery: String): ArrayList<com.ringtone.maker.Models.Contacts> {

            val selection = "(DISPLAY_NAME LIKE \"%$searchQuery%\")"

            val contactsModels = ArrayList<com.ringtone.maker.Models.Contacts>()

            val cursor = context.contentResolver.query(
                    ContactsContract.Contacts.CONTENT_URI,
                    arrayOf(ContactsContract.Contacts._ID, ContactsContract.Contacts.CUSTOM_RINGTONE, ContactsContract.Contacts.DISPLAY_NAME, ContactsContract.Contacts.LAST_TIME_CONTACTED, ContactsContract.Contacts.STARRED, ContactsContract.Contacts.TIMES_CONTACTED, ContactsContract.Contacts.PHOTO_ID),
                    selection, null,
                    "STARRED DESC, TIMES_CONTACTED DESC, LAST_TIME_CONTACTED DESC, DISPLAY_NAME ASC")

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    val contactsModel = com.ringtone.maker.Models.Contacts(cursor.getString(2),
                            cursor.getString(0), cursor.getInt(6))
                    contactsModels.add(contactsModel)
                } while (cursor.moveToNext())
            }

            return contactsModels
        }
    }
}
