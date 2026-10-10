package com.remindme.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat

/** No UI. Launched from the notification; opens WhatsApp then finishes. */
class WhatsAppActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val phone = intent.getStringExtra(EXTRA_PHONE).orEmpty()
        val text = intent.getStringExtra(EXTRA_TEXT).orEmpty()
        NotificationManagerCompat.from(this).cancel(intent.getIntExtra(EXTRA_NOTIF_ID, -1))
        if (!WhatsApp.open(this, phone, text)) {
            Toast.makeText(this, "WhatsApp is not installed", Toast.LENGTH_LONG).show()
        }
        finish()
    }

    companion object {
        private const val EXTRA_PHONE = "phone"
        private const val EXTRA_TEXT = "text"
        private const val EXTRA_NOTIF_ID = "notif_id"

        fun intent(c: Context, phone: String, text: String, notificationId: Int): Intent =
            Intent(c, WhatsAppActivity::class.java)
                .putExtra(EXTRA_PHONE, phone)
                .putExtra(EXTRA_TEXT, text)
                .putExtra(EXTRA_NOTIF_ID, notificationId)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
