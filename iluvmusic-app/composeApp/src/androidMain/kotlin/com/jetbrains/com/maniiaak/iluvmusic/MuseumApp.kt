package com.maniiaak.iluvmusic

import android.app.Application
import com.maniaak.iluvmusic.di.initKoin
import android.util.Log

class MuseumApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Pass the Application instance to the Android-specific initKoin
        try {
            initKoin(this)
        } catch (e: Exception) {
            // Log initialization errors to help diagnose DI/startup failures
            Log.e("MuseumApp", "Koin initialization failed", e)
            throw e
        }
    }
}
