package com.houssein.sezaia.network

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.houssein.sezaia.R
import com.houssein.sezaia.ui.screen.MainActivity

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("FCM", "Message reçu: ${remoteMessage.data} / ${remoteMessage.notification}")
        // Gère la notification
        // Récupère title et body depuis data
        val title = remoteMessage.data["title"] ?: "Notification"
        val body = remoteMessage.data["body"] ?: ""

        sendNotification(title, body)
    }

    private fun sendNotification(title: String, message: String) {
        // Crée un intent pour ouvrir MainActivity
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        // Crée le PendingIntent avec le flag approprié
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "default_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Crée le canal de notification si nécessaire (Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (notificationManager.getNotificationChannel(channelId) == null) {
                val channel = NotificationChannel(
                    channelId,
                    "Default Channel",
                    NotificationManager.IMPORTANCE_HIGH
                )
                channel.enableLights(true)
                channel.enableVibration(true)
                notificationManager.createNotificationChannel(channel)
            }
        }

        // Vérifie la permission POST_NOTIFICATIONS pour Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                // La permission n'est pas accordée, on ne peut pas afficher la notification
                return
            }
        }

        // Construit la notification
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.sezaia_logo1) // Assure-toi que c'est une icône monochrome
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        // Affiche la notification
        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}
