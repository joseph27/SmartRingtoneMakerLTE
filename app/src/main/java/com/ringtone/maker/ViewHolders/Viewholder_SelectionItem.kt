/*
 * *
 *  * Created by Youssef Assad on 6/2/18 11:22 AM
 *  * Copyright (c) 2018 . All rights reserved.
 *  * Last modified 6/2/18 11:04 AM
 *
 */

package com.ringtone.maker.ViewHolders


import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.ringtone.maker.UILapplication
import com.ringtone.maker.Models.MusicFile
import kotlinx.android.synthetic.main.item_selection.view.*
import com.ringtone.maker.Views.audiowave.ThumbWaveView
import java.io.*
import java.util.*


/**
 * Created by alex
 */

class Viewholder_SelectionItem(itemView: View) : RecyclerView.ViewHolder(itemView) {

    var playButton: ImageView? = null
    var wave: ThumbWaveView? = null
    internal var song_title: TextView? = null
    internal var song_duration: TextView? = null
    var Album_art: ImageView? = null
    internal var song: MusicFile? = null
    var mPopUpMenu: ImageView? = null


    fun bind(song: MusicFile) {
        mPopUpMenu = itemView.overflow
        Album_art = itemView.Album_art_container
        song_duration = itemView.song_duration
        song_title = itemView.song_title
        wave = itemView.audio_wave
        playButton = itemView.audio_button_play
        song_title!!.text = song.musicTitle
        this.song = song
        wave!!.isTouched = false
        wave!!.scaledData = ByteArray(0)
        wave!!.progress = 0f
        updateDurationText(song.duration?.toLong())
    }


    fun updateDurationText(duration: Long?) {

        val minutes = (duration!! % (1000 * 60 * 60) / (1000 * 60)).toInt()
        val seconds = (duration % (1000 * 60 * 60) % (1000 * 60) / 1000).toInt()
        var divider = ":"
        // if less than 10 secs add 0
        if (seconds < 10) divider = ":0"
        song_duration!!.text = "$minutes$divider$seconds"

    }

    fun DisplayWaveForm(HashCode: Int, waveWidth: Int) {

        val ChuckWidth = wave!!.chunkSpacing + wave!!.chunkWidth
        val ChuckRatio = waveWidth / ChuckWidth
        val SamplingRate = (540 / ChuckRatio).toFloat()

        val WaveFormFile = File(UILapplication.instance.cacheDir, HashCode.toString())

        ReadWaveFormFile(WaveFormFile, wave!!, SamplingRate)
    }



    companion object {

        fun ReadWaveFormFile(WaveFormFile: File, wave: ThumbWaveView, samplingRate: Float) {
            var HeightCounter = 0f
            val bytes = ArrayList<Byte>()
            val `in`: InputStream?
            try {
                `in` = BufferedInputStream(FileInputStream(WaveFormFile))
                val sb = StringBuilder()
                var line: String? = null
                var linecount = 0
                try {
                    val reader = BufferedReader(InputStreamReader(`in`, "UTF-8"))
                    while ({ line = reader.readLine(); line }() != null) {
                        sb.append(line)
                        linecount++
                        HeightCounter += java.lang.Float.valueOf(line)!!
                        if (linecount % samplingRate == 0f) {
                            val avgheight = HeightCounter / samplingRate
                            val height = (java.lang.Float.valueOf(avgheight)!! * 127).toInt()
                            bytes.add(height.toByte())
                            HeightCounter = 0f
                        }
                    }
                    val data = ByteArray(bytes.size)
                    for (i in bytes.indices) {
                        val x = bytes[i]
                        data[i] = x
                    }
                    wave.scaledData = data

                } catch (e: UnsupportedEncodingException) {
                    e.printStackTrace()
                } catch (e: IOException) {
                    e.printStackTrace()
                } finally {
                    try {
                        `in`.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }

                }
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            }

        }
    }

}
