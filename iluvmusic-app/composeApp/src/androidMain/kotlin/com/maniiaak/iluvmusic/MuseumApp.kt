package com.maniiaak.iluvmusic

import android.app.Application
import com.maniiaak.iluvmusic.di.initKoin
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.maniiaak.iluvmusic.data.MuseumApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MuseumApp : Application(), KoinComponent {
    private val api: MuseumApi by inject()
    override fun onCreate() {
        super.onCreate()
        // Pass the Application instance to the Android-specific initKoin
        try {
            initKoin(this)
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                    api.registerPushToken(token)
                }
            }
        } catch (e: Exception) {
            // Log initialization errors to help diagnose DI/startup failures
            Log.e("MuseumApp", "Koin initialization failed", e)
            throw e
        }
    }
}
