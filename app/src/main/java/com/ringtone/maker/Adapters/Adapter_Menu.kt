/*
 * *
 *  * Created by Youssef Assad on 6/2/18 11:22 AM
 *  * Copyright (c) 2018 . All rights reserved.
 *  * Last modified 6/2/18 11:04 AM
 *
 */

package com.ringtone.maker.Adapters

import android.app.Activity
import android.content.res.TypedArray
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.ringtone.maker.R
import kotlinx.android.synthetic.main.menu_item.view.*


class Adapter_Menu(activity: Activity, internal var MenuItemClickListener: ListenerOnMenuItemClick) : RecyclerView.Adapter<Adapter_Menu.ViewHolder_Menu>() {

    private val img: TypedArray = activity.resources.obtainTypedArray(R.array.menu_array)
    private val titles: TypedArray = activity.resources.obtainTypedArray(R.array.MenuTitles)
    private val space = 2
    private val item = 3


    override fun getItemViewType(position: Int): Int {
        return if (titles.getString(position) == KEY_SPACE)  space
        else   item
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder_Menu {
        var v: View? = null
        if (viewType == space) {
            v = LayoutInflater.from(parent.context)
                    .inflate(R.layout.menu_space, parent, false)

        } else if (viewType == item) {
            v = LayoutInflater.from(parent.context)
                    .inflate(R.layout.menu_item, parent, false)
        }
        return ViewHolder_Menu(v!!)
    }

    override fun onBindViewHolder(holder: ViewHolder_Menu, position: Int) {
        if (getItemViewType(position) != space) {
            holder.SetIcon()
            if (holder.im_icon != null) holder.im_icon!!.setImageResource(img.getResourceId(position, 0))
            if (holder.im_title != null) holder.im_title!!.text = titles.getString(position)
            holder.mView.setOnClickListener { MenuItemClickListener.Item(img.getResourceId(position, 0) )} }
    }

    override fun getItemCount(): Int = img.length()

    inner class ViewHolder_Menu(internal val mView: View) : RecyclerView.ViewHolder(mView) {
        internal var im_icon: ImageView? = null
        internal var im_title: TextView? = null
        fun SetIcon() {
            im_icon = itemView.iv_icon
            im_title = itemView.tv_title
        }
    }

    interface ListenerOnMenuItemClick { fun Item(Title: Int)}

    companion object { private val KEY_SPACE = "SPACE" }

}

