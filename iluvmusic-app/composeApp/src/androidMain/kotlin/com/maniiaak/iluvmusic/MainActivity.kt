package com.maniiaak.iluvmusic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.content.Intent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.runtime.mutableStateOf
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

class MainActivity : ComponentActivity() {
    private val notificationAlbumId = mutableStateOf<Int?>(null)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        createFriendReviewChannel()

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
        }
        notificationAlbumId.value = intent.getStringExtra("album_id")?.toIntOrNull()
            ?: intent.data?.lastPathSegment?.toIntOrNull()

        setContent {
            App(initialAlbumId = notificationAlbumId.value)
        }
    }

    private fun createFriendReviewChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.deleteNotificationChannel("friend_reviews")
            val channel = NotificationChannel(
                "friend_reviews_v2",
                "Friend reviews",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Pop-up notifications for album reviews from users you follow"
                enableVibration(true)
                enableLights(true)
                setShowBadge(true)
            }
            manager.createNotificationChannel(channel)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        notificationAlbumId.value = intent.getStringExtra("album_id")?.toIntOrNull()
            ?: intent.data?.lastPathSegment?.toIntOrNull()
    }
}