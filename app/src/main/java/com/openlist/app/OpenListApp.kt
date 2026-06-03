package com.openlist.app

import android.app.Application
import com.openlist.app.data.local.AppDatabase
import com.openlist.app.data.local.AppPreferences

class OpenListApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val preferences: AppPreferences by lazy { AppPreferences(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: OpenListApp
            private set
    }
}
