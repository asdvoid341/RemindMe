package com.remindme.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object Notifications {
    private const val CHANNEL_ID = "reminders"

    fun createChannel(c: Context) {
        val channel = NotificationChannel(CHANNEL_ID, "Reminders", NotificationManager.IMPORTANCE_HIGH)
            .apply { description = "Reminder alerts" }
        c.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun show(c: Context, r: Reminder) {
        createChannel(c)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val openApp = PendingIntent.getActivity(
            c, r.id,
            Intent(c, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            flags,
        )

        val body = r.message.ifBlank { "Reminder" }
        val builder = NotificationCompat.Builder(c, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(r.title)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openApp)

        if (r.phone.isNotBlank()) {
            val sendWhatsApp = PendingIntent.getActivity(
                c, r.id, WhatsAppActivity.intent(c, r.phone, r.whatsappText, r.id), flags,
            )
            val text = "$body\nTap to send on WhatsApp to +${r.phone}"
            builder.setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setContentIntent(sendWhatsApp)
                .addAction(R.drawable.ic_send, "Send on WhatsApp", sendWhatsApp)
        } else {
            builder.setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
        }

        NotificationManagerCompat.from(c).notify(r.id, builder.build())
    }
}
