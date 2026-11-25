package com.maniiaak.iluvmusic.push

import com.google.firebase.messaging.FirebaseMessaging
import com.maniiaak.iluvmusic.data.MuseumApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

actual fun registerPushNotifications(api: MuseumApi) {
    FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            api.registerPushToken(token)
        }
    }
}
