/*
 * *
 *  * Created by Youssef Assad on 6/2/18 11:22 AM
 *  * Copyright (c) 2018 . All rights reserved.
 *  * Last modified 6/2/18 11:04 AM
 *
 */

package com.ringtone.maker.Entities

import com.birbit.android.jobqueue.Job
import com.birbit.android.jobqueue.Params
import com.birbit.android.jobqueue.RetryConstraint
import com.ringtone.maker.UILapplication
import org.greenrobot.eventbus.EventBus
import java.io.File


/**
 * Created by Joseph27 on 5/28/18.
 */

class WaveFormJob(private val FilePath: String) : Job(Params(PRIORITY).setDelayMs(5000).persist()) {


    override fun onAdded() {
    }

    @Throws(Throwable::class)
    override fun onRun() {
        var mFile = File(FilePath)
        val externalRootDir = UILapplication.getMusicCache() + FilePath.hashCode()
        val file = File(externalRootDir)
        if (file.exists() || file.length() > 0) {
            return
        } else {
            try {
                var mSoundFile = com.ringtone.maker.SoundEditor.CheapSoundFile.create(mFile.absolutePath, null)
                if (mSoundFile != null) {
                    mSoundFile.computeDoublesForAllZoomLevels()
                    mSoundFile.SaveCache(FilePath)
                    EventBus.getDefault().post(com.ringtone.maker.Entities.WaveFormCompletionEvent(FilePath))
                }
            } catch (e: Exception) {
            }

        }



    }

    override fun onCancel(cancelReason: Int, throwable: Throwable?) {

    }

    override fun shouldReRunOnThrowable(throwable: Throwable, runCount: Int, maxRunCount: Int): RetryConstraint? {
        return RetryConstraint.createExponentialBackoff(runCount, 1000)
    }

    companion object {

        val PRIORITY = 1
    }
}
