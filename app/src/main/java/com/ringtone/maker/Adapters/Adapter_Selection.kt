/*
 * *
 *  * Created by Youssef Assad on 6/2/18 11:22 AM
 *  * Copyright (c) 2018 . All rights reserved.
 *  * Last modified 6/2/18 11:04 AM
 *
 */

package com.ringtone.maker.Adapters


import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Message
import android.provider.MediaStore
import android.provider.Settings
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import com.ringtone.maker.Activties.Activity_ContactsChoice
import com.ringtone.maker.Activties.Activity_Editor
import com.ringtone.maker.Database.DBHelper
import com.ringtone.maker.R
import com.ringtone.maker.UILapplication
import com.ringtone.maker.ViewHolders.Viewholder_SelectionItem
import com.ringtone.maker.Entities.WaveFormJob
import com.ringtone.maker.Models.MusicFile
import com.ringtone.maker.Models.MusicType
import com.ringtone.maker.Utils.PermissionManger
import com.ringtone.maker.Utils.Pixels
import com.squareup.picasso.NetworkPolicy
import com.squareup.picasso.Picasso
import jp.wasabeef.picasso.transformations.BlurTransformation
import jp.wasabeef.picasso.transformations.ColorFilterTransformation
import org.jetbrains.anko.*
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.*


class Adapter_Selection : RecyclerView.Adapter<Viewholder_SelectionItem>(), android.os.Handler.Callback {

    //  private List<Record> data = new ArrayList<>();
    private var mSongs = ArrayList<MusicFile>()
    internal var activity: Activity? = null
    internal var WaveWidth: Int = 0
    private var uiUpdateHandler: android.os.Handler? = null
    private var playingHolder: Viewholder_SelectionItem? = null
    private var playingPosition: Int = 0
    private var mediaPlayer: MediaPlayer? = null
    var JobManager = UILapplication.instance.getJobManager()
    var handler: Handler? = null


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Viewholder_SelectionItem = Viewholder_SelectionItem(LayoutInflater.from(parent.context).inflate(R.layout.item_selection, parent, false))


    override fun onViewRecycled(holder: Viewholder_SelectionItem?) {
        super.onViewRecycled(holder)
        // view holder displaying playing audio cell is being recycled
        // change its state to non-playing
        if (playingPosition == holder!!.adapterPosition) {
            updateNonPlayingView(playingHolder)
            playingHolder = null
        }
    }

    fun OnPause() {
        if (mediaPlayer != null) {
            mediaPlayer!!.pause()
            if (playingHolder != null) {
                playingHolder!!.playButton?.setImageResource(R.drawable.thumb_ic_play)
            }
        }

    }

    override fun onBindViewHolder(holder: Viewholder_SelectionItem, position: Int) {

        if (position == playingPosition) {
            playingHolder = holder
            // this view holder corresponds to the currently playing audio cell
            // update its view to show playing progress
            updatePlayingView()
        } else {
            // and this one corresponds to non playing
            updateNonPlayingView(holder)
        }


        holder.bind(mSongs[position])
        Picasso.with(activity).load(mSongs[position].musicAlbumArt)
                .placeholder(R.drawable.default_album_art)
                .networkPolicy(NetworkPolicy.OFFLINE)
                .resize(1080, 400)
                .centerCrop()
                .onlyScaleDown()
                .transform(ColorFilterTransformation(ContextCompat.getColor(activity, R.color.song_background_blur_color)))
                .transform(BlurTransformation(activity, 5, 2))
                .into(holder.Album_art)



        holder.playButton?.setOnClickListener {
            if (holder.adapterPosition == playingPosition) {
                // toggle between play/pause of audio
                if (mediaPlayer!!.isPlaying) {
                    mediaPlayer!!.pause()
                } else {
                    mediaPlayer!!.start()
                }
            } else {
                // start another audio playback
                playingPosition = holder.adapterPosition
                if (mediaPlayer != null) {
                    if (null != playingHolder) {
                        updateNonPlayingView(playingHolder)
                    }
                    mediaPlayer!!.release()
                }
                playingHolder = holder
                startMediaPlayer(mSongs[playingPosition].musicPath)
            }
            updatePlayingView()
        }


        holder.wave?.setOnClickListener {
            StartEditorActivity(position)

        }
        holder.mPopUpMenu?.setOnClickListener {
            onPopUpMenuClickListener(holder.mPopUpMenu!!, position)
        }


        val WaveFormFile = File(UILapplication.instance.cacheDir, mSongs[position].musicPath?.hashCode().toString())


        if (WaveFormFile.exists() || WaveFormFile.length() > 0) {
            holder.DisplayWaveForm(mSongs[position].musicPath!!.hashCode(), WaveWidth)
        } else {
            JobManager!!.addJobInBackground(WaveFormJob(
                    mSongs[position].musicPath.toString()), {

            })
        }

    }


    private fun StartEditorActivity(position: Int) {
        activity!!.startActivity(activity!!.intentFor<Activity_Editor>(
                Activity_Editor.KEY_SOUND_COLUMN_path to mSongs[position].musicPath,
                Activity_Editor.KEY_SOUND_COLUMN_title to mSongs[position].musicTitle).newTask())
        activity!!.overridePendingTransition(0, 0)
    }

    private fun onPopUpMenuClickListener(v: View, position: Int) {
        val menu = PopupMenu(activity, v)
        menu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.popup_song_edit -> StartEditorActivity(position)
                R.id.popup_song_delete -> confirmDelete(position)
                R.id.popup_song_assign_to_contact -> if (PermissionManger.checkAndRequestContactsPermissions(activity)) {
                    chooseContactForRingtone(position)
                }
                R.id.popup_song_set_default_ringtone -> setAsDefaultRingtoneOrNotification(position)
            }
            false
        }
        menu.inflate(R.menu.popup_song)

        when {
            mSongs.get(position).type.equals(MusicType.NOTIFICATION.toString()) -> {
                menu.menu.findItem(R.id.popup_song_set_default_ringtone).isVisible = false
                menu.menu.findItem(R.id.popup_song_assign_to_contact).isVisible = false
            }
        }
        menu.show()
    }

    private fun confirmDelete(position: Int) {
        OnPause()

        activity!!.alert("Remove " + mSongs[position].musicTitle + "?", "Delete") {
            yesButton {
                val file = File(mSongs[position].musicPath)
                val deleted = file.delete()
                if (deleted) {
                    mSongs.removeAt(position)
                    notifyDataSetChanged()
                }
            }
            noButton { }
        }.show()


    }

    private fun openAndroidPermissionsMenu() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
            intent.data = Uri.parse("package:" + activity!!.packageName)
            activity!!.startActivity(intent)
        }

    }

    private fun setAsDefaultRingtoneOrNotification(pos: Int) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(activity)) {
            openAndroidPermissionsMenu()
            return
        }

        val mimeType: String? = when {
            (mSongs[pos].musicPath)!!.endsWith(".m4a") -> "audio/mp4a-latm"
            (mSongs[pos].musicPath)!!.endsWith(".wav") -> "audio/wav"
            else -> // This should never happen.
                "audio/mpeg"
        }

        val values = ContentValues()
        values.put(MediaStore.MediaColumns.DATA, (mSongs[pos].musicPath))
        values.put(MediaStore.MediaColumns.TITLE, (mSongs[pos].musicTitle))
        values.put(MediaStore.MediaColumns.SIZE, (mSongs[pos].musicTitle))
        values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType)

        values.put(MediaStore.Audio.Media.ARTIST, (mSongs[pos].artist))
        values.put(MediaStore.Audio.Media.DURATION, (mSongs[pos].duration))
        val uri = MediaStore.Audio.Media.getContentUriForPath(mSongs[pos].musicPath)
        activity!!.contentResolver.delete(uri, MediaStore.MediaColumns.DATA + "=\"" + (mSongs[pos].musicPath).toString() + "\"", null)
        val newUri = activity!!.contentResolver.insert(uri, values)
        RingtoneManager.setActualDefaultRingtoneUri(activity, RingtoneManager.TYPE_RINGTONE, newUri)
        activity!!.toast(R.string.default_ringtone_success_message)


        DBHelper.getInstance(activity!!).MarkSongAsAlerted(mSongs[pos].musicTitle, MusicType.RINGTONE.toString(), mSongs[pos].duration, System.currentTimeMillis() / 1000, mSongs[pos].musicPath, true)
        DBHelper.getInstance(activity!!).MarkSongAsSelected(mSongs[pos].musicTitle, MusicType.RINGTONE.toString(), mSongs[pos].duration , System.currentTimeMillis() / 1000, mSongs[pos].musicPath, true)

    }


    private fun chooseContactForRingtone(pos: Int): Boolean {

        val intent = Intent(activity, Activity_ContactsChoice::class.java)
        intent.putExtra(Activity_ContactsChoice.FILE_NAME, mSongs[pos].musicPath)
        activity!!.startActivity(intent)

        return true
    }

    private fun updateNonPlayingView(holder: Viewholder_SelectionItem?) {
        if (holder == playingHolder) {
            uiUpdateHandler!!.removeMessages(MSG_UPDATE_SEEK_BAR)
        }
        if (holder != null) {
            holder.playButton?.setImageResource(R.drawable.thumb_ic_play)
            holder.wave?.progress = 0F

        }

    }

    private fun updatePlayingView() {

        val currentduration = (mediaPlayer!!.duration - mediaPlayer!!.currentPosition).toLong()
        playingHolder!!.updateDurationText(currentduration)
        if (mediaPlayer!!.currentPosition != 0) {

            val progress = mediaPlayer!!.currentPosition * 100 / mediaPlayer!!.duration
            playingHolder!!.wave?.progress = progress.toFloat()
        }


        if (mediaPlayer!!.isPlaying) {
            uiUpdateHandler!!.sendEmptyMessageDelayed(MSG_UPDATE_SEEK_BAR, 100)
            playingHolder!!.playButton?.setImageResource(R.drawable.ic_pause)
        } else {
            uiUpdateHandler!!.removeMessages(MSG_UPDATE_SEEK_BAR)
            playingHolder!!.playButton?.setImageResource(R.drawable.thumb_ic_play)
        }
    }

    private fun releaseMediaPlayer() {
        if (null != playingHolder) {
            updateNonPlayingView(playingHolder)
        }
        mediaPlayer!!.release()
        mediaPlayer = null
        playingPosition = -1
    }


    private fun startMediaPlayer(Path: String?) {

        mediaPlayer = MediaPlayer()

        try {
            mediaPlayer!!.reset()
            mediaPlayer!!.setAudioStreamType(AudioManager.STREAM_MUSIC)
            val subsetInputStream = FileInputStream(Path!!)
            mediaPlayer!!.setDataSource(subsetInputStream.fd)
            mediaPlayer!!.prepare()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        mediaPlayer!!.setOnCompletionListener { releaseMediaPlayer() }
        mediaPlayer!!.start()
    }


    fun updateData(mSongs: ArrayList<MusicFile>, activity: Activity) {
        //    this.data = nextData;
        handler = Handler(activity.mainLooper)
        this.mSongs = mSongs
        this.activity = activity
        notifyDataSetChanged()

        WaveWidth = (Pixels.getScreenWidth(activity) * 0.65).toInt()
        this.playingPosition = -1
        uiUpdateHandler = android.os.Handler(this)

    }

    fun ClearData() {
        OnPause()
        uiUpdateHandler!!.removeMessages(MSG_UPDATE_SEEK_BAR)
        //    data.clear();
        mSongs.clear()
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return this.mSongs.size
    }

    override fun handleMessage(message: Message): Boolean {
        when (message.what) {
            MSG_UPDATE_SEEK_BAR -> {
                if (mediaPlayer!!.currentPosition != 0) {
                    val progress = mediaPlayer!!.currentPosition * 100 / mediaPlayer!!.duration
                    playingHolder!!.wave?.progress = progress.toFloat()

                }
                val currentduration = (mediaPlayer!!.duration - mediaPlayer!!.currentPosition).toLong()
                playingHolder!!.updateDurationText(currentduration)

                uiUpdateHandler!!.sendEmptyMessageDelayed(MSG_UPDATE_SEEK_BAR, 100)
                return true
            }
        }
        return false
    }

    companion object {


        private val MSG_UPDATE_SEEK_BAR = 1845

    }
}
