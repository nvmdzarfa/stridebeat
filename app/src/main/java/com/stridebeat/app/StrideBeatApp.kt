package com.stridebeat.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class StrideBeatApp : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
