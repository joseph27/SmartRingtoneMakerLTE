/*
 * *
 *  * Created by Youssef Assad on 6/2/18 11:22 AM
 *  * Copyright (c) 2018 . All rights reserved.
 *  * Last modified 6/2/18 11:04 AM
 *
 */

package com.ringtone.maker.Activties


import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.ContentUris
import android.content.ContentValues
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.provider.MediaStore
import android.provider.Settings
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.app.AppCompatDelegate
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.ringtone.maker.R
import com.ringtone.maker.SoundEditor.CheapSoundFile
import com.ringtone.maker.SoundEditor.MarkerView
import com.ringtone.maker.Models.MusicType
import com.ringtone.maker.Utils.*
import de.mateware.snacky.Snacky
import io.codetail.animation.SupportAnimator
import io.codetail.animation.ViewAnimationUtils
import kotlinx.android.synthetic.main.activity_editor.*
import org.jetbrains.anko.alert
import org.jetbrains.anko.yesButton
import java.io.*


class Activity_Editor : AppCompatActivity(), MarkerView.MarkerListener, com.ringtone.maker.SoundEditor.WaveformView.WaveformListener, View.OnClickListener {
    private var mSaveSoundFileThread: Thread? = null
    private val Supported_Format = arrayOf(".aac", ".AMR", ".mp3", ".wav",".m4a")
    private var mNewFileKind: Int = 0
    private var mMarkerLeftInset: Int = 0
    private var mMarkerRightInset: Int = 0
    private var mLoadingLastUpdateTime: Long = 0
    private var mLoadingKeepGoing: Boolean = false
    private var mProgressDialog: ProgressDialog? = null
    private var mSoundFile: com.ringtone.maker.SoundEditor.CheapSoundFile? = null
    private var mFile: File? = null
    private var mFilename: String? = null
    private var mWaveformView: com.ringtone.maker.SoundEditor.WaveformView? = null
    private var mStartMarker: MarkerView? = null
    private var mEndMarker: MarkerView? = null
    private var mStartText: TextView? = null
    private var mEndText: TextView? = null
    private var mKeyDown: Boolean = false
    private var mWidth: Int = 0
    private var mMaxPos: Int = 0
    private var mStartPos = -1
    private var mEndPos = -1
    private var mStartVisible: Boolean = false
    private var mEndVisible: Boolean = false
    private var mLastDisplayedStartPos: Int = 0
    private var mLastDisplayedEndPos: Int = 0
    private var mOffset: Int = 0
    private var mOffsetGoal: Int = 0
    private var mFlingVelocity: Int = 0
    private var mPlayStartMsec: Int = 0
    private var mPlayStartOffset: Int = 0
    private var mPlayEndMsec: Int = 0
    private var mHandler: Handler? = null
    private var mIsPlaying: Boolean = false
    private var mPlayer: MediaPlayer? = null
    private var mTouchDragging: Boolean = false
    private var mTouchStart: Float = 0.toFloat()
    private var mTouchInitialOffset: Int = 0
    private var mTouchInitialStartPos: Int = 0
    private var mTouchInitialEndPos: Int = 0
    private var mWaveformTouchStartMsec: Long = 0
    private var mDensity: Float = 0.toFloat()
    private var outputFile: File? = null
    private var mSound_AlbumArt_Path: String? = null
    private var marginvalue: Int = 0
    private var EdgeReached = false
    private var mSoundDuration = 0
    private var Maskhidden = true
    private var mSharedPref: SharedPref? = null



    private val mTimerRunnable = object : Runnable {
        override fun run() {
            // Updating an EditText is slow on Android.  Make sure
            // we only do the update if the text has actually changed.
            if (mStartPos != mLastDisplayedStartPos && !mStartText!!.hasFocus()) {
                mStartText!!.text = getTimeFormat(formatTime(mStartPos))
                mLastDisplayedStartPos = mStartPos
            }

            if (mEndPos != mLastDisplayedEndPos && !mEndText!!.hasFocus()) {
                mEndText!!.text = getTimeFormat(formatTime(mEndPos))
                mLastDisplayedEndPos = mEndPos
            }

            mHandler!!.postDelayed(this, 100)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.window.decorView.setBackgroundColor(ContextCompat.getColor(this, R.color.app_decorview_color))
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        setContentView(R.layout.activity_editor)

        zoom_in!!.setOnClickListener(this)
        zoom_out!!.setOnClickListener(this)
        Button_Done!!.setOnClickListener(this)
        image_Cancel!!.setOnClickListener(this)
        Editor_Save!!.setOnClickListener(this)
        Editor_Notification!!.setOnClickListener(this)
        Editor_Alarm!!.setOnClickListener(this)
        Editor_Ringtone!!.setOnClickListener(this)
        Editor_Contacts!!.setOnClickListener(this)

        ViewUtil.SetOntouchListener(Button_Done!!)
        ViewUtil.SetOntouchListener(Editor_Save!!)
        ViewUtil.SetOntouchListener(Editor_Notification!!)
        ViewUtil.SetOntouchListener(Editor_Alarm!!)
        ViewUtil.SetOntouchListener(Editor_Ringtone!!)
        ViewUtil.SetOntouchListener(Editor_Contacts!!)

        Play_Pause_View!!.visibility = View.INVISIBLE
        Play_Pause_View!!.setPlaying(!mIsPlaying)
        Play_Pause_View!!.setOnClickListener(this)

        // temporary solution to fix the delay between initial pause to play animation
        Play_Pause_View!!.postDelayed({ runOnUiThread { Play_Pause_View!!.visibility = View.VISIBLE } }, 400)

        marginvalue = Pixels.pxtodp(this, 12)
        mPlayer = null
        mIsPlaying = false
        mSoundFile = null
        mKeyDown = false
        mHandler = Handler()
        mHandler!!.postDelayed(mTimerRunnable, 100)

        val extras = intent.extras
        val path = extras?.getString(KEY_SOUND_COLUMN_path, null)
        val title = extras?.getString(KEY_SOUND_COLUMN_title, null)

        if (path == null) {
            pickFile()
        } else {
            // remove mp3 part
            val newtitle: String
            if (title!!.contains(EXTENSION_MP3)) newtitle = title.replace(EXTENSION_MP3, "") else newtitle = title.toString()
            Editor_song_title!!.text = newtitle
            mFilename = path

            if (mSoundFile == null) loadFromFile() else mHandler!!.post { this.finishOpeningSoundFile() }
        }

        loadGui()

        mSharedPref = SharedPref(this)

    }


    private fun pickFile() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!PermissionManger.checkPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)) PermissionManger.requestPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            else StartMediaPickerActivity()
        } else StartMediaPickerActivity()
    }

    private fun StartMediaPickerActivity() = startActivityForResult(Intent(Intent.ACTION_PICK, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI), 200)


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == AppCompatActivity.RESULT_OK) {
            var mSoundTitle: String
            val dataUri = data.data
            val proj = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DATA, MediaStore.Audio.Albums.ALBUM_ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Artists.ARTIST)
            val tempCursor = managedQuery(dataUri, proj, null, null, null)
            tempCursor.moveToFirst() //reset the cursor
            var col_index: Int
            var AlbumID_index: Int
            do {
                col_index = tempCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                mSoundTitle = tempCursor.getString(col_index)
                AlbumID_index = tempCursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM_ID)
                val albumid = tempCursor.getLong(AlbumID_index)
                val sArtworkUri = Uri.parse("content://media/external/audio/albumart")
                val uri = ContentUris.withAppendedId(sArtworkUri, albumid)
                mSound_AlbumArt_Path = uri.toString()
            } while (tempCursor.moveToNext())
            try {
                assert(dataUri != null)
                var path: String? = dataUri!!.path

                if (!path!!.startsWith("/storage/")) {
                    path = MediaStoreHelper.getRealPathFromURI(applicationContext, data.data!!)
                }
                assert(path != null)
                val file = File(path!!)
                var mNewTitle = mSoundTitle
                if (mSoundTitle.contains(EXTENSION_MP3)) {
                    mNewTitle = file.name.replace(EXTENSION_MP3, "")
                }

                Editor_song_title!!.text = mNewTitle
                mFilename = file.absolutePath

                if (mSoundFile == null) loadFromFile() else mHandler!!.post { this.finishOpeningSoundFile() }
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }

    }


    override fun onBackPressed() {
        if (!Maskhidden) runanimation() else if (mFilename != null && !mFilename!!.isEmpty()) showExitOptionsDialog() else finish()
    }


    private fun showExitOptionsDialog() {

        val colors = arrayOf<CharSequence>(getString(R.string.editor_back_dialog_discard), getString(R.string.editor_back_dialog_cancel))
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.editor_back_dialog_title)
        builder.setItems(colors) { dialog, which ->
            when (which) {
                0 -> FinishActivity()
            }
        }
        builder.show()
    }

    private fun FinishActivity() {
        if (mPlayer != null && mPlayer!!.isPlaying) {
            mPlayer!!.pause()
        }
        mWaveformView!!.setPlayback(-1)
        mIsPlaying = false
        finish()
    }


    override fun onDestroy() {
        super.onDestroy()

        if (mPlayer != null && mPlayer!!.isPlaying) {
            mPlayer!!.stop()
            mPlayer!!.release()
            mPlayer = null
        }
        mProgressDialog = null

        finish()

        mSoundFile = null
        mWaveformView = null


    }


    override fun waveformDraw() {
        if (mWaveformView != null) {
            mWidth = mWaveformView!!.measuredWidth
        }
        if (mOffsetGoal != mOffset && !mKeyDown && !EdgeReached) {
            updateDisplay()
        } else if (mIsPlaying) {

            updateDisplay()
        } else if (mFlingVelocity != 0) {

            updateDisplay()
        }
    }

    override fun waveformTouchStart(x: Float) {

        mTouchDragging = true
        mTouchStart = x
        mTouchInitialOffset = mOffset
        mFlingVelocity = 0
        mWaveformTouchStartMsec = System.currentTimeMillis()
    }

    override fun waveformTouchMove(x: Float) {
        mOffset = trap((mTouchInitialOffset + (mTouchStart - x)).toInt())
        updateDisplay()
    }

    override fun waveformTouchEnd() {
        mTouchDragging = false
        mOffsetGoal = mOffset

        val elapsedMsec = System.currentTimeMillis() - mWaveformTouchStartMsec
        if (elapsedMsec < 300) {
            if (mIsPlaying) {
                val seekMsec = mWaveformView!!.pixelsToMillisecs((mTouchStart + mOffset).toInt())
                if (seekMsec >= mPlayStartMsec && seekMsec < mPlayEndMsec) {
                    mPlayer!!.seekTo(seekMsec - mPlayStartOffset)
                } else {
                    handlePause()
                }
            } else {
                onPlay((mTouchStart + mOffset).toInt())
            }
        }
    }

    override fun waveformFling(vx: Float) {
        mTouchDragging = false
        mOffsetGoal = mOffset
        mFlingVelocity = (-vx).toInt()
        updateDisplay()
    }


    override fun waveformZoomIn() {

        if (mWaveformView!!.canZoomOut()) {

            marginvalue = Pixels.pxtodp(this, 12)
            val params = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT)
            params.setMargins(Pixels.pxtodp(this@Activity_Editor, 12), 0, Pixels.pxtodp(this@Activity_Editor, 12), Pixels.pxtodp(this@Activity_Editor, 20))
            mWaveformView!!.layoutParams = params

        }
        mWaveformView!!.zoomIn()

        mStartPos = mWaveformView!!.start
        mEndPos = mWaveformView!!.end
        mMaxPos = mWaveformView!!.maxPos()
        mOffset = mWaveformView!!.offset
        mOffsetGoal = mOffset
        updateDisplay()


    }

    override fun waveformZoomOut() {

        if (!mWaveformView!!.canZoomOut()) {
            marginvalue = Pixels.pxtodp(this, 12)
            val params = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT)
            params.setMargins(Pixels.pxtodp(this@Activity_Editor, 12), 0, Pixels.pxtodp(this@Activity_Editor, 12), Pixels.pxtodp(this@Activity_Editor, 20))
            mWaveformView!!.layoutParams = params

        }
        mWaveformView!!.zoomOut()
        mStartPos = mWaveformView!!.start
        mEndPos = mWaveformView!!.end
        mMaxPos = mWaveformView!!.maxPos()
        mOffset = mWaveformView!!.offset
        mOffsetGoal = mOffset
        updateDisplay()
    }


    //
    // MarkerListener
    //

    override fun markerDraw() {}

    override fun markerTouchStart(marker: MarkerView, pos: Float) {
        mTouchDragging = true
        mTouchStart = pos
        mTouchInitialStartPos = mStartPos
        mTouchInitialEndPos = mEndPos
    }

    override fun markerTouchMove(marker: MarkerView, pos: Float) {
        val delta: Float = pos - mTouchStart

        if (marker == mStartMarker) {
            mStartPos = trap((mTouchInitialStartPos + delta).toInt())
            if (mStartPos + mStartMarker!!.width >= mEndPos) {
                mStartPos = mEndPos - mStartMarker!!.width
            }

        } else {
            mEndPos = trap((mTouchInitialEndPos + delta).toInt())
            if (mEndPos < mStartPos + mStartMarker!!.width)
                mEndPos = mStartPos + mStartMarker!!.width
        }


        updateDisplay()
    }

    override fun markerTouchEnd(marker: MarkerView) {
        mTouchDragging = false
        if (marker == mStartMarker) {
            setOffsetGoalStart()
        } else {
            setOffsetGoalEnd()
        }
    }

    override fun markerLeft(marker: MarkerView, velocity: Int) {
        mKeyDown = true

        if (marker == mStartMarker) {
            val saveStart = mStartPos
            mStartPos = trap(mStartPos - velocity)
            mEndPos = trap(mEndPos - (saveStart - mStartPos))
            setOffsetGoalStart()
        }

        if (marker == mEndMarker) {
            if (mEndPos == mStartPos) {
                mStartPos = trap(mStartPos - velocity)
                mEndPos = mStartPos
            } else {
                mEndPos = trap(mEndPos - velocity)
            }

            setOffsetGoalEnd()
        }

        updateDisplay()
    }

    override fun markerRight(marker: MarkerView, velocity: Int) {
        mKeyDown = true

        if (marker == mStartMarker) {
            val saveStart = mStartPos
            mStartPos += velocity
            if (mStartPos > mMaxPos)
                mStartPos = mMaxPos
            mEndPos += mStartPos - saveStart
            if (mEndPos > mMaxPos)
                mEndPos = mMaxPos

            setOffsetGoalStart()
        }

        if (marker == mEndMarker) {
            mEndPos += velocity
            if (mEndPos > mMaxPos)
                mEndPos = mMaxPos

            setOffsetGoalEnd()
        }

        updateDisplay()
    }

    override fun markerEnter(marker: MarkerView) {}

    override fun markerKeyUp() {
        mKeyDown = false
        updateDisplay()
    }

    override fun markerFocus(marker: MarkerView) {
        mKeyDown = false
        if (marker == mStartMarker) {
            setOffsetGoalStartNoUpdate()
        } else {
            setOffsetGoalEndNoUpdate()
        }

        // Delay updaing the display because if this focus was in
        // response to a touch event, we want to receive the touch
        // event too before updating the display.
        mHandler!!.postDelayed({ this.updateDisplay() }, 100)
    }


    //
    // Internal methods
    //

    private fun loadGui() {

        val metrics = DisplayMetrics()
        this.windowManager.defaultDisplay.getMetrics(metrics)
        mDensity = metrics.density

        mMarkerLeftInset = (13 * mDensity).toInt()
        mMarkerRightInset = (13 * mDensity).toInt()


        mStartText = findViewById(R.id.starttext)
        mEndText = findViewById(R.id.endtext)
        mark_start.setOnClickListener(this)
        mark_end.setOnClickListener(this)

        enableDisableButtons()

        mWaveformView = findViewById(R.id.waveform)
        mWaveformView!!.setListener(this)
        mMaxPos = 0
        mLastDisplayedStartPos = -1
        mLastDisplayedEndPos = -1

        if (mSoundFile != null && !mWaveformView!!.hasSoundFile()) {
            mWaveformView!!.setSoundFile(mSoundFile)
            mWaveformView!!.recomputeHeights(mDensity)
            mMaxPos = mWaveformView!!.maxPos()
        }

        mStartMarker = findViewById(R.id.startmarker)
        mStartMarker!!.setListener(this)
        mStartMarker!!.alpha = 1f
        mStartMarker!!.isFocusable = true
        mStartMarker!!.isFocusableInTouchMode = true
        mStartVisible = true

        mEndMarker = findViewById(R.id.endmarker)
        mEndMarker!!.setListener(this)
        mEndMarker!!.alpha = 1f
        mEndMarker!!.isFocusable = true
        mEndMarker!!.isFocusableInTouchMode = true
        mEndVisible = true

        updateDisplay()


    }


    private fun loadFromFile() {


        mFile = File(mFilename!!)
        val mFileName = mFile!!.name

        var FileSupported = false
        for (aSupported_Format in Supported_Format) if (mFileName.contains(aSupported_Format)) {
            FileSupported = true
            break
        }

        if (!FileSupported) {
            alert("Unsupported Format") {
                yesButton {
                    finish()
                }
            }.show()

            return
        }



        mLoadingLastUpdateTime = System.currentTimeMillis()
        mLoadingKeepGoing = true


        mProgressDialog = ProgressDialog(this)
        mProgressDialog!!.setCancelable(false)
        mProgressDialog!!.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
        mProgressDialog!!.setTitle(getString(R.string.edit_loading_text))
        mProgressDialog!!.setOnCancelListener { mLoadingKeepGoing = false }
        mProgressDialog!!.show()


        val listener = CheapSoundFile.ProgressListener { fractionComplete ->
            val now = System.currentTimeMillis()
            if (now - mLoadingLastUpdateTime > 100) {
                mProgressDialog!!.progress = (mProgressDialog!!.max * fractionComplete).toInt()
                mLoadingLastUpdateTime = now
            }
            mLoadingKeepGoing
        }

        mProgressDialog!!.setOnDismissListener {

            Log.e(TAG, "loadFromFile: setOnDismissListener ")
            mEndMarker!!.visibility = View.VISIBLE
            mStartMarker!!.visibility = View.VISIBLE

        }


        // Create the MediaPlayer in a background thread
        object : Thread() {
            override fun run() {
                try {
                    mPlayer = MediaPlayer()
                    mPlayer?.setDataSource(this@Activity_Editor, Uri.fromFile(mFile))
                    mPlayer?.setAudioStreamType(AudioManager.STREAM_MUSIC)
                    mPlayer?.prepare()

                } catch (ignored: IOException) {
                    runOnUiThread {
                        Toast.makeText(this@Activity_Editor, "Please try to change file name", Toast.LENGTH_LONG).show()
                        AlertDialog.Builder(this@Activity_Editor)
                                .setTitle(R.string.editor_error)
                                .setMessage(R.string.editor_error_msg.toString() + " File name contains Special Characters Please change file name and try again.")
                                .setPositiveButton(android.R.string.yes) { dialog, which -> pickFile() }
                                .show()
                    }

                    try {
                        val filePath = mFile!!.absolutePath
                        val file = File(filePath)
                        val inputStream = FileInputStream(file)

                        mPlayer = MediaPlayer()
                        mPlayer?.setDataSource(inputStream.fd)
                        mPlayer?.setAudioStreamType(AudioManager.STREAM_MUSIC)
                        mPlayer?.prepare()

                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }

            }
        }.start()

        // Load the sound file in a background thread
        object : Thread() {
            override fun run() {
                try {
                    mSoundFile = com.ringtone.maker.SoundEditor.CheapSoundFile.create(mFile!!.absolutePath, listener)
                } catch (e: Exception) {
                    //  Log.e(TAG, "Error while loading sound file" + e);
                    mProgressDialog!!.dismiss()
                    return
                }

                if (mLoadingKeepGoing) {
                    mHandler!!.post {
                        if (mSoundFile != null) {
                            finishOpeningSoundFile()
                        } else {
                            //Log.e(TAG, "run: editor_error" );
                            mProgressDialog!!.dismiss()
                            AlertDialog.Builder(this@Activity_Editor)
                                    .setTitle(R.string.editor_error)
                                    .setMessage(R.string.editor_error_msg)
                                    .setPositiveButton(android.R.string.yes) { dialog, which -> pickFile() }
                                    .show()

                        }

                    }
                }
            }
        }.start()
    }

    private fun finishOpeningSoundFile() {

        mWaveformView!!.setSoundFile(mSoundFile)
        mWaveformView!!.recomputeHeights(mDensity)

        mMaxPos = mWaveformView!!.maxPos()
        mLastDisplayedStartPos = -1
        mLastDisplayedEndPos = -1

        mTouchDragging = false

        mOffset = 0
        mOffsetGoal = 0
        mFlingVelocity = 0
        resetPositions()

        mProgressDialog!!.dismiss()
        updateDisplay()
    }

    @SuppressLint("NewApi")
    @Synchronized private fun updateDisplay() {
        if (mPlayer != null) {
            mSoundDuration = mPlayer!!.duration / 1000

        }


        if (mIsPlaying) {


            var now = 0f
            if (mPlayer != null) {
                now = (mPlayer!!.currentPosition + mPlayStartOffset).toFloat()
            }

            // check if the user has modified the limits
            val frames = mWaveformView!!.millisecsToPixels(now.toInt())
            mWaveformView!!.setPlayback(frames)
            setOffsetGoalNoUpdate(frames - mWidth / 2)




            if (now >= mPlayEndMsec) {
                handlePause()


            }
        }

        if (!mTouchDragging) {
            var offsetDelta: Int

            if (mFlingVelocity != 0) {

                offsetDelta = mFlingVelocity / 30
                if (mFlingVelocity > 80) {
                    mFlingVelocity -= 80
                } else if (mFlingVelocity < -80) {
                    mFlingVelocity += 80
                } else {
                    mFlingVelocity = 0
                }

                mOffset += offsetDelta

                if (mOffset + mWidth / 2 > mMaxPos) {
                    mOffset = mMaxPos - mWidth / 2
                    mFlingVelocity = 0
                }
                if (mOffset < 0) {
                    mOffset = 0
                    mFlingVelocity = 0
                }
                mOffsetGoal = mOffset
            } else {
                offsetDelta = mOffsetGoal - mOffset

                if (offsetDelta > 10)
                    offsetDelta = offsetDelta / 10
                else if (offsetDelta > 0)
                    offsetDelta = 1
                else if (offsetDelta < -10)
                    offsetDelta = offsetDelta / 10
                else if (offsetDelta < 0)
                    offsetDelta = -1
                else
                    offsetDelta = 0

                mOffset += offsetDelta
            }
        }



        if (mWaveformView != null) {
            if (mWaveformView!!.getcurrentmLevel() != 0) {
                if (mWaveformView!!.measuredWidth + mOffset >= mWaveformView!!.getcurrentmLevel()) {
                    mOffset = mWaveformView!!.getcurrentmLevel() - mWaveformView!!.measuredWidth
                    EdgeReached = true
                } else {
                    EdgeReached = false
                }
            }

        }
        mWaveformView!!.setParameters(mStartPos, mEndPos, mOffset, mSoundDuration)
        mWaveformView!!.invalidate()

        var startX = mStartPos - mOffset - mMarkerLeftInset
        if (startX + mStartMarker!!.width >= 0) {
            if (!mStartVisible) {
                // Delay this to avoid flicker
                mHandler!!.postDelayed({
                    mStartVisible = true
                    mStartMarker!!.alpha = 1f

                    mStartMarker!!.alpha = 1f
                }, 0)
            }
        } else {
            if (mStartVisible) {

                mStartMarker!!.alpha = 0f
                mStartVisible = false
            }
            startX = 0
        }

        var endX = mEndPos - mOffset - mEndMarker!!.width + mMarkerRightInset
        if (endX + mEndMarker!!.width >= 0) {
            if (!mEndVisible) {
                // Delay this to avoid flicker
                mHandler!!.postDelayed({
                    mEndVisible = true
                    mEndMarker!!.alpha = 1f

                }, 0)
            }
        } else {
            if (mEndVisible) {
                mEndMarker!!.alpha = 0f

                mEndVisible = false
            }
            endX = 0
        }


        val layoutParamsStart = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT)
        layoutParamsStart.setMargins(startX + marginvalue, mWaveformView!!.measuredHeight, 0, 0)

        mStartMarker!!.layoutParams = layoutParamsStart
        val layoutParamsEnd = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT)

        // if the marker does notification_ic reach the end  margin endx + value
        if (endX + marginvalue <= mWaveformView!!.measuredWidth) {
            layoutParamsEnd.setMargins(endX + marginvalue, mWaveformView!!.measuredHeight, 0, 0)
        } else {
            // if endx is less or equal the maxmium width we fix the margin at wave width
            if (endX <= mWaveformView!!.measuredWidth) {
                layoutParamsEnd.setMargins(mWaveformView!!.measuredWidth, mWaveformView!!.measuredHeight, 0, 0)
                // else we use the same endx as value for margin so it will disappear
            } else {
                layoutParamsEnd.setMargins(endX, mWaveformView!!.measuredHeight, 0, 0)
            }
        }

        mEndMarker!!.layoutParams = layoutParamsEnd


        mEndMarker!!.alpha
    }

    private fun enableDisableButtons() {
        runOnUiThread {
            if (mIsPlaying) {
                Play_Pause_View!!.toggle()
            } else {
                Play_Pause_View!!.toggle()
            }

        }

    }

    private fun resetPositions() {
        mStartPos = 0
        mEndPos = mMaxPos
    }

    private fun trap(pos: Int): Int {
        if (pos < 0)
            return 0
        return if (pos > mMaxPos) mMaxPos else pos
    }

    private fun setOffsetGoalStart() = setOffsetGoal(mStartPos - mWidth / 2)
    private fun setOffsetGoalStartNoUpdate() = setOffsetGoalNoUpdate(mStartPos - mWidth / 2)
    private fun setOffsetGoalEnd() = setOffsetGoal(mEndPos - mWidth / 2)
    private fun setOffsetGoalEndNoUpdate() = setOffsetGoalNoUpdate(mEndPos - mWidth / 2)

    private fun setOffsetGoal(offset: Int) {
        setOffsetGoalNoUpdate(offset)
        updateDisplay()
    }

    private fun setOffsetGoalNoUpdate(offset: Int) {
        if (mTouchDragging) {
            return
        }

        mOffsetGoal = offset
        if (mOffsetGoal + mWidth / 2 > mMaxPos)
            mOffsetGoal = mMaxPos - mWidth / 2
        if (mOffsetGoal < 0)
            mOffsetGoal = 0
    }

    private fun formatTime(pixels: Int): String {
        return if (mWaveformView != null && mWaveformView!!.isInitialized) {
            formatDecimal(mWaveformView!!.pixelsToSeconds(pixels))
        } else {
            ""
        }
    }

    private fun formatDecimal(x: Double): String {
        var xWhole = x.toInt()
        var xFrac = (100 * (x - xWhole) + 0.5).toInt()

        if (xFrac >= 100) {
            xWhole++ //Round up
            xFrac -= 100 //Now we need the remainder after the round up
            if (xFrac < 10) {
                xFrac *= 10 //we need a fraction that is 2 digits long
            }
        }

        return if (xFrac < 10)
            xWhole.toString() + ".0" + xFrac
        else
            xWhole.toString() + "." + xFrac
    }

    @Synchronized private fun handlePause() {
        if (mPlayer != null && mPlayer!!.isPlaying) {
            mPlayer!!.pause()
        }
        mWaveformView!!.setPlayback(-1)
        mIsPlaying = false
        enableDisableButtons()

    }


    @Synchronized private fun onPlay(startPosition: Int) {
        if (mIsPlaying) {
            handlePause()
            return
        }

        if (mPlayer == null) {
            // Not initialized yet
            return
        }

        try {
            mPlayStartMsec = mWaveformView!!.pixelsToMillisecs(startPosition)
            if (startPosition < mStartPos) {
                mPlayEndMsec = mWaveformView!!.pixelsToMillisecs(mStartPos)
            } else if (startPosition > mEndPos) {
                mPlayEndMsec = mWaveformView!!.pixelsToMillisecs(mMaxPos)
            } else {
                mPlayEndMsec = mWaveformView!!.pixelsToMillisecs(mEndPos)
            }

            mPlayStartOffset = 0

            val startFrame = mWaveformView!!.secondsToFrames(mPlayStartMsec * 0.001)
            val endFrame = mWaveformView!!.secondsToFrames(mPlayEndMsec * 0.001)

            val startByte = mSoundFile!!.getSeekableFrameOffset(startFrame)
            val endByte = mSoundFile!!.getSeekableFrameOffset(endFrame)
            if (startByte >= 0 && endByte >= 0) {
                try {
                    mPlayer!!.reset()
                    mPlayer!!.setAudioStreamType(AudioManager.STREAM_MUSIC)
                    val subsetInputStream = FileInputStream(mFile!!.absolutePath)
                    mPlayer!!.setDataSource(subsetInputStream.fd, startByte.toLong(), (endByte - startByte).toLong())
                    mPlayer!!.prepare()
                    mPlayStartOffset = mPlayStartMsec
                } catch (e: Exception) {
                    Log.e(TAG, "Exception trying to play file subset" + e)
                    mPlayer!!.reset()
                    mPlayer!!.setAudioStreamType(AudioManager.STREAM_MUSIC)
                    mPlayer!!.setDataSource(mFile!!.absolutePath)
                    mPlayer!!.prepare()
                    mPlayStartOffset = 0
                }

            }

            mPlayer!!.setOnCompletionListener(object : MediaPlayer.OnCompletionListener {
                override fun onCompletion(mediaPlayer: MediaPlayer) {
                    handlePause()
                    Log.d(this.javaClass.simpleName, "onCompletion: Completed")

                    //progressBarPlay.setProgress(0);
                }
            })
            // mPlayer.setOnCompletionListener((MediaPlayer mediaPlayer) -> handlePause());
            mIsPlaying = true

            if (mPlayStartOffset == 0) {
                mPlayer!!.seekTo(mPlayStartMsec)
            }
            mPlayer!!.start()
            updateDisplay()
            enableDisableButtons()
        } catch (e: Exception) {
            //Log.e(TAG, "Exception while playing file" + e);
        }

    }


    @SuppressLint("SetTextI18n")
    override fun CreateSelection(startpoint: Double, endpoint: Double) {

        if (mEndPos != -1 || mStartPos != -1) {

            val endpointbefore = java.lang.Float.valueOf(mWaveformView!!.pixelsToSeconds(mEndPos).toString())!!
            val endpointafter = java.lang.Float.valueOf(endpoint.toString())!!
            val propertyValuesHolder = PropertyValuesHolder.ofFloat("phase", endpointbefore, endpointafter)


            val startpointBefore = java.lang.Float.valueOf(mWaveformView!!.pixelsToSeconds(mStartPos).toString())!!
            val startpointAFter = java.lang.Float.valueOf(startpoint.toString())!!
            val propertyValuesHolder2 = PropertyValuesHolder.ofFloat("phase2", startpointBefore, startpointAFter)

            val mObjectAnimator = ObjectAnimator.ofPropertyValuesHolder(this, propertyValuesHolder, propertyValuesHolder2)

            mObjectAnimator.addUpdateListener { valueAnimator ->
                val newEndpos = java.lang.Float.valueOf(valueAnimator.getAnimatedValue(propertyValuesHolder.propertyName).toString())
                mEndPos = mWaveformView!!.secondsToPixels(newEndpos!!.toDouble())
                val NewStartpos = java.lang.Float.valueOf(valueAnimator.getAnimatedValue(propertyValuesHolder2.propertyName).toString())

                mStartPos = mWaveformView!!.secondsToPixels(NewStartpos!!.toDouble())
                mStartText!!.text = (newEndpos % 3600 / 60).toInt().toString() + ":" + (newEndpos % 60).toInt().toString()
                mEndText!!.text = (NewStartpos % 3600 / 60).toInt().toString() + ":" + (NewStartpos % 60).toInt().toString()
                updateDisplay()

            }

            mObjectAnimator.start()

            mStartText!!.text = (startpoint % 3600 / 60).toInt().toString() + ":" + (startpoint % 60).toInt().toString()
            mEndText!!.text = (endpoint % 3600 / 60).toInt().toString() + ":" + (endpoint % 60).toInt().toString()
            mEndPos = mWaveformView!!.secondsToPixels(endpoint)
            mStartPos = mWaveformView!!.secondsToPixels(startpoint)
            updateDisplay()


        }


    }


    fun setPhase(phase: Float) { }
    fun setPhase2(phase2: Float) { }


    override fun onClick(view: View) {
        when {
            view === zoom_in -> waveformZoomIn()
            view === zoom_out -> waveformZoomOut()
            view === Button_Done -> HandleCutRequest()
            view === image_Cancel -> runanimation()
            view == Play_Pause_View -> onPlay(mStartPos)
            view == mark_start -> if (mIsPlaying) {
                mStartPos = mWaveformView!!.millisecsToPixels(mPlayer!!.currentPosition + mPlayStartOffset)
                updateDisplay()
            }
            view == mark_end -> if (mIsPlaying) {
                mEndPos = mWaveformView!!.millisecsToPixels(mPlayer!!.currentPosition + mPlayStartOffset)
                updateDisplay()
                handlePause()
            }
            else -> Cutselection(view.id)
        }

    }

    private fun HandleCutRequest() {

        if (mPlayer!!.isPlaying) handlePause()


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.System.canWrite(this@Activity_Editor)) {
                //                com.zatrek.zatrekcut.Pixels.Log.d(TAG, "onClick: Permission Done");
                runanimation()
            } else {
                openAndroidPermissionsMenu()
            }
        } else {
            runanimation()
        }
    }

    private fun openAndroidPermissionsMenu() {
        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
        intent.data = Uri.parse("package:" + this@Activity_Editor.packageName)
        startActivity(intent)
    }


    fun runanimation() {

        val optionsContainer = findViewById<View>(R.id.options_container) as LinearLayout
        val array = IntArray(2)
        Button_Done!!.getLocationOnScreen(array)
        optionsContainer.post {
            val cx = Pixels.getScreenWidth(this@Activity_Editor) / 2
            val cy = array[1]
            val radius = Math.max(optionsContainer.width, optionsContainer.height)

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {


                val animator = ViewAnimationUtils.createCircularReveal(optionsContainer, cx, cy, 0f, radius.toFloat())
                animator.setInterpolator(AccelerateDecelerateInterpolator())
                val animator_reverse = animator.reverse()

                if (Maskhidden) {
                    optionsContainer.visibility = View.VISIBLE
                    animator.start()
                    Maskhidden = false
                    editor_container!!.visibility = View.INVISIBLE
                } else {
                    animator_reverse.addListener(object : SupportAnimator.AnimatorListener {
                        override fun onAnimationStart() { }
                        override fun onAnimationCancel() { }
                        override fun onAnimationRepeat() {}
                        override fun onAnimationEnd() {
                            optionsContainer.visibility = View.INVISIBLE
                            editor_container!!.visibility = View.VISIBLE
                            Maskhidden = true
                        }

                    })
                    animator_reverse.start()
                }


            } else {

                if (Maskhidden) {
                    optionsContainer.visibility = View.VISIBLE
                    optionsContainer.requestLayout()
                    android.view.ViewAnimationUtils.createCircularReveal(optionsContainer, cx, cy, 0f, radius.toFloat()).start()
                    Maskhidden = false
                    editor_container!!.visibility = View.INVISIBLE
                } else {
                    val anim = android.view.ViewAnimationUtils.createCircularReveal(optionsContainer, cx, cy, radius.toFloat(), 0f)
                    anim.addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            super.onAnimationEnd(animation)
                            optionsContainer.visibility = View.INVISIBLE
                            Maskhidden = true
                            editor_container!!.visibility = View.VISIBLE
                        }
                    })
                    anim.start()

                }


            }
        }

    }

    fun Cutselection(which: Int) {

        when (which) {
            R.id.Editor_Ringtone -> {
                SaveRingTone()
                mNewFileKind = FILE_KIND_RINGTONE
            }
            R.id.Editor_Notification -> {
                SaveRingTone()
                mNewFileKind = FILE_KIND_NOTIFICATION
            }
            R.id.Editor_Save -> {
                SaveRingTone()
                mNewFileKind = FILE_KIND_Save
            }
            R.id.Editor_Alarm -> {
                SaveRingTone()
                mNewFileKind = FILE_KIND_ALARM
            }
            R.id.Editor_Contacts -> {
                if (PermissionManger.checkAndRequestContactsPermissions(this)) {
                    chooseContactForRingtone()
                }
            }
        }


    }

    private fun chooseContactForRingtone() {
        val intent = Intent(this, Activity_ContactsChoice::class.java)
        intent.putExtra(Activity_ContactsChoice.FILE_NAME, mFilename)
        this.startActivity(intent)
    }


    private fun SaveRingTone() {

        val startTime = mWaveformView!!.pixelsToSeconds(mStartPos)
        val endTime = mWaveformView!!.pixelsToSeconds(mEndPos)

        val mStartPosMilliS = mWaveformView!!.pixelsToMillisecs(mStartPos)
        val mEndPosMilliS = mWaveformView!!.pixelsToMillisecs(mEndPos)

        val startFrame = mWaveformView!!.secondsToFrames(mStartPosMilliS * 0.001)
        val numFrames = mWaveformView!!.secondsToFrames(mEndPosMilliS * 0.001) - startFrame

        val fadeTime = mWaveformView!!.secondsToFrames(5.0)
        val duration = (endTime - startTime + 0.5).toInt()


        // Create an indeterminate progress dialog
        mProgressDialog = ProgressDialog(this)
        mProgressDialog!!.setProgressStyle(ProgressDialog.STYLE_SPINNER)
        mProgressDialog!!.setTitle(getString(R.string.dialog_saving))
        mProgressDialog!!.isIndeterminate = true
        mProgressDialog!!.setCancelable(false)
        mProgressDialog!!.show()

        mSaveSoundFileThread = object : Thread() {
            override fun run() {

                var outPath: String? = makeRingtoneFilename(Editor_song_title!!.text.toString(), ".mp3") ?: return
                outputFile = File(outPath)
                var fallbackToWAV: Boolean? = false
                try {
                    // Write the new file
                    mSoundFile!!.WriteFile(outputFile, startFrame, numFrames, false, false, fadeTime)

                } catch (e: Exception) {
                    // log the error and try to create a .wav file instead
                    if (outputFile!!.exists()) {
                        outputFile!!.delete()
                    }
                    val writer = StringWriter()
                    e.printStackTrace(PrintWriter(writer))
                    fallbackToWAV = true
                }

                if (fallbackToWAV!!) {
                    outPath = makeRingtoneFilename(Editor_song_title!!.text.toString(), ".wav")
                    if (outPath == null) {
                        val runnable = Runnable {
                        }
                        mHandler!!.post(runnable)
                        return
                    }
                    outputFile = File(outPath)
                    try {
                        mSoundFile!!.writewavfile(outputFile, startFrame, numFrames, false, false, fadeTime)


                    } catch (e: Exception) {
                        // Creating the .wav file also failed. Stop the progress dialog, show an
                        // error message and exit.
                        mProgressDialog!!.dismiss()
                        if (outputFile!!.exists()) {
                            outputFile!!.delete()
                        }

                        return
                    }

                }

                val finalOutPath = outPath
                val runnable = Runnable {
                    afterSavingRingtone(Editor_song_title!!.text.toString(),
                            finalOutPath,
                            duration, endTime)
                }
                mHandler!!.post(runnable)
                mProgressDialog!!.dismiss()
            }
        }


        mSaveSoundFileThread!!.start()

    }

    private fun afterSavingRingtone(title: CharSequence, outPath: String?, duration: Int, endpoint: Double) {


        val dbHelper = com.ringtone.maker.Database.DBHelper.getInstance(this)
        val outFile = File(outPath!!)
        val fileSize = outFile.length()
        if (fileSize <= 512) {
            outFile.delete()
            AlertDialog.Builder(this)
                    .setTitle("Failure")
                    .setMessage("File is too Small")
                    .setPositiveButton("Ok", null)
                    .setCancelable(false)
                    .show()
            return
        }

        // Create the database record, pointing to the existing file path
        val mimeType: String
        when {
            outPath.endsWith(".m4a") -> mimeType = "audio/mp4a-latm"
            outPath.endsWith(".wav") -> mimeType = "audio/wav"
            else ->  mimeType = "audio/mpeg"
        }

        val artist = "Zatrek Ringtone Cutter"

        val values = ContentValues()
        values.put(MediaStore.MediaColumns.DATA, outPath)
        values.put(MediaStore.MediaColumns.TITLE, title.toString())
        values.put(MediaStore.MediaColumns.SIZE, fileSize)
        values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
        values.put(MediaStore.Audio.Media.ARTIST, artist)
        values.put(MediaStore.Audio.Media.DURATION, duration)
        values.put(MediaStore.Audio.Media.IS_RINGTONE, 1)
        values.put(MediaStore.Audio.Media.IS_NOTIFICATION, 0)

        val uri = MediaStore.Audio.Media.getContentUriForPath(outPath)
        val newUri = contentResolver.insert(uri, values)
        setResult(Activity.RESULT_OK, Intent().setData(newUri))

        var Songtype: String? = null

        val time = System.currentTimeMillis() / 1000

        when (mNewFileKind) {
            FILE_KIND_NOTIFICATION -> {
                RingtoneManager.setActualDefaultRingtoneUri(this@Activity_Editor, RingtoneManager.TYPE_NOTIFICATION, newUri)
                Songtype = com.ringtone.maker.Utils.Constants.NOTIFICATION_KEY
                dbHelper.MarkSongAsSelected(title.toString(), MusicType.NOTIFICATION.toString(), duration * 1000, time, outPath, true)
                dbHelper.MarkSongAsAlerted(title.toString(), MusicType.NOTIFICATION.toString(), duration * 1000, time, outPath, false)
            }
            FILE_KIND_RINGTONE -> {
                RingtoneManager.setActualDefaultRingtoneUri(this@Activity_Editor, RingtoneManager.TYPE_RINGTONE, newUri)
                Songtype = com.ringtone.maker.Utils.Constants.RINGTONE_KEY
                dbHelper.MarkSongAsSelected(title.toString(), MusicType.RINGTONE.toString(), duration * 1000, time, outPath, true)
                dbHelper.MarkSongAsAlerted(title.toString(), MusicType.RINGTONE.toString(), duration * 1000, time, outPath, false)
            }
            FILE_KIND_ALARM -> {
                RingtoneManager.setActualDefaultRingtoneUri(this@Activity_Editor, RingtoneManager.TYPE_ALARM, newUri)
                Songtype = com.ringtone.maker.Utils.Constants.ALARM_KEY
                dbHelper.MarkSongAsSelected(title.toString(), MusicType.ALARM.toString(), duration * 1000, time / 1000, outPath, true)
                dbHelper.MarkSongAsAlerted(title.toString(), MusicType.ALARM.toString(), duration * 1000, time, outPath, false)
            }
            FILE_KIND_Save -> {
                dbHelper.MarkSongAsAlerted(title.toString(), MusicType.RINGTONE.toString(), duration * 1000, time, outPath, true)
                dbHelper.MarkSongAsSelected(title.toString(), MusicType.RINGTONE.toString(), duration * 1000, time, outPath, true)
            }
        }

        val warningSnackBar = Snacky.builder()
                .setActivty(this)
                .setBackgroundColor(ContextCompat.getColor(this,R.color.editor_toast_color))
                .setText(this.getString(R.string.Edit_Done_Toast))
                .setDuration(Snacky.LENGTH_LONG)

        warningSnackBar.success().show()




    }

    private fun makeRingtoneFilename(title: CharSequence, extension: String): String? {

        var subdir: String? = null

        when (mNewFileKind) {
            0 -> subdir = "Notification/"
            1 -> subdir = "RingTone/"
            2 -> subdir = "Saved/"
            3 -> subdir = "Alarm/"
        }

        val extr = Environment.getExternalStorageDirectory().toString()
        var externalRootDir = extr + "/" + resources.getString(R.string.app_name) + "/" + subdir
        if (!externalRootDir.endsWith("/")) {
            externalRootDir += "/"
        }

        var parentdir = externalRootDir
        // Create the parent directory
        val parentDirFile = File(parentdir)
        parentDirFile.mkdirs()

        // If we can't write to that special path, try just writing
        // directly to the sdcard
        if (!parentDirFile.isDirectory) {
            parentdir = externalRootDir
        }

        // Turn the title into a filename
        var filename = ""
        for (i in 0 until title.length) {
            if (Character.isLetterOrDigit(title[i])) {
                filename += title[i]
            }
        }

        // Try to make the filename unique
        var path: String? = null
        for (i in 0..99) {
            val testPath: String
            if (i > 0)
                testPath = parentdir + filename + i + extension
            else
                testPath = parentdir + filename + extension

            try {
                val f = RandomAccessFile(File(testPath), "r")
                f.close()
            } catch (e: Exception) {
                // Good, the file didn't exist
                path = testPath
                break
            }

        }

        return path
    }


    companion object {
        private val TAG = Activity_Editor::class.java.simpleName
        private val EXTENSION_MP3 = ".mp3"
        val KEY_SOUND_COLUMN_title = "title"
        val KEY_SOUND_COLUMN_path = "path"
        val FILE_KIND_NOTIFICATION = 0
        val FILE_KIND_RINGTONE = 1
        val FILE_KIND_Save = 2
        val FILE_KIND_ALARM = 3

        private fun getTimeFormat(time: String): String {
            if (!time.isEmpty()) {
                val Displayedmins: String
                val DisplayedSecs: String
                val mins = java.lang.Double.parseDouble(time) % 3600 / 60
                if (mins < 10) Displayedmins = "0" + mins.toInt().toString() else Displayedmins = mins.toInt().toString()
                val secs = java.lang.Double.parseDouble(time) % 60
                if (secs < 10) DisplayedSecs = "0" + secs.toInt().toString() else DisplayedSecs = secs.toInt().toString()
                return Displayedmins + ":" + DisplayedSecs
            } else return ""
        }
    }


}