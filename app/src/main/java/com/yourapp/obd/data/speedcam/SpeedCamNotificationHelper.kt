package com.yourapp.obd.data.speedcam

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.yourapp.obd.R
import com.yourapp.obd.ui.MainActivity

object SpeedCamNotificationHelper {

    private const val CHANNEL_ID = "speedcam_update"
    private const val NOTIFICATION_ID_UPDATE_SUCCESS = 2001
    private const val NOTIFICATION_ID_UPDATE_ERROR = 2002

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Обновление базы камер",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Уведомления о статусе обновления базы камер скорости"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun notifyUpdateSuccess(context: Context, summary: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("База камер обновлена")
            .setContentText(summary)
            .setStyle(NotificationCompat.BigTextStyle().bigText(summary))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_UPDATE_SUCCESS, notification)
        } catch (_: SecurityException) { }
    }

    fun notifyUpdateError(context: Context, errorMessage: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("open_settings", "speedcam")
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 1, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Ошибка обновления базы камер")
            .setContentText(errorMessage)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$errorMessage\nНажмите, чтобы открыть настройки и повторить вручную."))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_UPDATE_ERROR, notification)
        } catch (_: SecurityException) { }
    }
}
