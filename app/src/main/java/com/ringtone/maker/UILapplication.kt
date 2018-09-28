/*
 * *
 *  * Created by Youssef Assad on 6/2/18 11:22 AM
 *  * Copyright (c) 2018 . All rights reserved.
 *  * Last modified 6/2/18 11:04 AM
 *
 */

package com.ringtone.maker

import android.app.Application
import android.os.Environment

import com.birbit.android.jobqueue.JobManager
import com.birbit.android.jobqueue.config.Configuration
import com.ringtone.maker.Utils.ExternalStorageUtil


/**
 * Created by Joseph27 on 5/21/18.
 */

class UILapplication : Application() {

    private var jobManager: JobManager? = null


    init {
        instance = this
    }

    override fun onCreate() {
        super.onCreate()
        getJobManager()// ensure it is created


    }

    private fun configureJobManager() {
        val builder = Configuration.Builder(this)
                .minConsumerCount(1)//always keep at least one consumer alive
                .maxConsumerCount(1)//up to 3 consumers at a time
                .loadFactor(1)//3 jobs per consumer
                .consumerKeepAlive(120)//wait 2 minute


        jobManager = JobManager(builder.build())
    }

    @Synchronized
    fun getJobManager(): JobManager? {
        if (jobManager == null) {
            configureJobManager()
        }
        return jobManager
    }





    companion object {
        lateinit var instance: UILapplication



        fun get(): UILapplication {
            return instance
        }




        fun getMusicCache(): String {

            return Environment.getExternalStorageDirectory().toString() +  "/" + UILapplication.get().getString(R.string.app_name) + "/Cache/Music/"
        }

        // Get public external storage base directory.
        fun getPublicExternalStorageBaseDir(): String {
            var ret = ""
            if (ExternalStorageUtil.isExternalStorageMounted()) {
                val file = Environment.getExternalStorageDirectory()
                ret = file.absolutePath
            }
            return ret
        }

    }
}
