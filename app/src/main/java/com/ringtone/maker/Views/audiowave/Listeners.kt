package com.ringtone.maker.Views.audiowave

/**
 * Created by Youssef Assad
 */

interface OnSamplingListener {
  fun onComplete()
}

interface OnProgressListener {
  fun onStartTracking(progress: Float)
  fun onStopTracking(progress: Float)
  fun onProgressChanged(progress: Float, byUser: Boolean)
}