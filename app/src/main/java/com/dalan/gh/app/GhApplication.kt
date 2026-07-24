package com.dalan.gh.app

import android.app.Application
import android.content.Context

class GhApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
    }

    companion object {
        lateinit var appContext: Context
    }
}