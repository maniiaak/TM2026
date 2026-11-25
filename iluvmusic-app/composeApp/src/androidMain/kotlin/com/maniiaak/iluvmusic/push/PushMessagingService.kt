package com.maniiaak.iluvmusic.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.maniiaak.iluvmusic.MainActivity
import com.maniiaak.iluvmusic.R
import com.maniiaak.iluvmusic.data.MuseumApi
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class PushMessagingService : FirebaseMessagingService(), KoinComponent {
    private val api: MuseumApi by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        scope.launch { api.registerPushToken(token) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        createChannel()
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val title = message.notification?.title ?: message.data["title"] ?: "iluvmusic"
        val body = message.notification?.body ?: message.data["body"] ?: "Someone you follow posted a new review"
        val albumId = message.data["album_id"]

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (albumId != null) putExtra("album_id", albumId)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            albumId?.hashCode() ?: 0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val profileUrl = message.data["profile_url"]
        val avatar = loadCircularBitmap(profileUrl)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setColor(ContextCompat.getColor(this, R.color.notification_accent)) // optional
            .setLargeIcon(avatar)            // null is fine, it's just skipped
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .setOnlyAlertOnce(false)
            .build()

        NotificationManagerCompat.from(this).notify(albumId?.hashCode() ?: System.currentTimeMillis().toInt(), notification)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val oldChannel = getSystemService(NotificationManager::class.java)
            oldChannel.deleteNotificationChannel("friend_reviews")
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Friend reviews",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for new album reviews from users you follow"
                enableVibration(true)
                enableLights(true)
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PRIVATE
            }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    private fun loadCircularBitmap(url: String?): Bitmap? {
        if (url.isNullOrBlank()) return null
        return try {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 5_000
                readTimeout = 5_000
            }
            conn.inputStream.use { BitmapFactory.decodeStream(it) }?.let { toCircle(it) }
        } catch (e: Exception) {
            null
        }
    }

    private fun toCircle(src: Bitmap): Bitmap {
        val size = 192
        val scaled = Bitmap.createScaledBitmap(src, size, size, true)
        val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(scaled, 0f, 0f, paint)
        return out
    }

    companion object {
        private const val CHANNEL_ID = "friend_reviews_v2"
    }
}
