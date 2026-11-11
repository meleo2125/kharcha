package com.mukeshprajapat.kharcha

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class KharchaApp : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
