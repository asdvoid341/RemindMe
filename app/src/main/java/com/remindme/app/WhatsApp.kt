package com.remindme.app

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

object WhatsApp {
    /** "+91 98765-43210" -> "919876543210" */
    fun normalize(raw: String): String = raw.filter { it.isDigit() }.trimStart('0')

    /** Opens the WhatsApp chat with [text] pre-filled. Returns false if nothing could handle it. */
    fun open(c: Context, phone: String, text: String): Boolean {
        val uri = Uri.parse(
            "https://api.whatsapp.com/send?phone=${normalize(phone)}&text=${Uri.encode(text)}"
        )
        // Prefer regular WhatsApp, then WhatsApp Business, then whatever handles the link.
        for (pkg in listOf("com.whatsapp", "com.whatsapp.w4b", null)) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (pkg != null) intent.setPackage(pkg)
                c.startActivity(intent)
                return true
            } catch (e: ActivityNotFoundException) {
                // try the next option
            }
        }
        return false
    }
}
