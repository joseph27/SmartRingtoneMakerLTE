/*
 * *
 *  * Created by Youssef Assad on 6/2/18 11:20 AM
 *  * Copyright (c) 2018 . All rights reserved.
 *  * Last modified 6/2/18 11:04 AM
 *
 */

package com.ringtone.maker.Adapters


import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.provider.ContactsContract
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.ringtone.maker.Activties.Activity_ContactsChoice
import com.ringtone.maker.R
import com.ringtone.maker.Adapters.Adapter_Contacts.ItemHolder
import kotlinx.android.synthetic.main.item_contacts.view.*
import java.util.*


class Adapter_Contacts(private val mActivityContactsChoice: Activity_ContactsChoice, private var mData: ArrayList<com.ringtone.maker.Models.Contacts>?) : RecyclerView.Adapter<ItemHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemHolder {
        return ItemHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_contacts, parent, false))
    }

    override fun onBindViewHolder(holder: ItemHolder, position: Int) {
        holder.ContactName.text = mData!![position].mName
        if (mData!![position].mPhotoID != 0)
            holder.ContactImg.setImageBitmap(queryContactImage(mData!![position].mPhotoID!!))
         else
            holder.ContactImg.setImageResource(R.drawable.ic_comment_avatar)
    }


    private fun queryContactImage(imageDataRow: Int): Bitmap? {
        val c = mActivityContactsChoice.contentResolver.query(ContactsContract.Data.CONTENT_URI, arrayOf(ContactsContract.CommonDataKinds.Photo.PHOTO), ContactsContract.Data._ID + "=?", arrayOf(Integer.toString(imageDataRow)), null)
        var imageBytes: ByteArray? = null
        if (c != null) {
            if (c.moveToFirst()) {
                imageBytes = c.getBlob(0)
            }
            c.close()
        }

        return if (imageBytes != null)
            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
         else null
    }

    override fun getItemCount(): Int { return if (mData == null) 0 else mData!!.size}


    fun updateData(data: ArrayList<com.ringtone.maker.Models.Contacts>) {
        this.mData = data
        notifyDataSetChanged()
    }

    inner class ItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
         val ContactImg: ImageView
         val ContactName: TextView

        init {
            ContactImg = itemView.imageViewUserPic
            ContactName = itemView.textViewUser
            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View) = mActivityContactsChoice.onItemClicked(adapterPosition)

    }
}
