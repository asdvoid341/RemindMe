package com.remindme.app

import org.json.JSONObject

enum class Repeat(val label: String) {
    ONCE("One time"),
    DAILY("Every day"),
    WEEKLY("Every week"),
    MONTHLY("Every month"),
}

data class Reminder(
    val id: Int,
    val title: String,
    /** Text sent on WhatsApp. Falls back to [title] when blank. */
    val message: String,
    /** Next time this reminder fires. */
    val timeMillis: Long,
    val repeat: Repeat,
    /** WhatsApp number with country code, digits only. Blank = no WhatsApp. */
    val phone: String,
    val enabled: Boolean = true,
) {
    val whatsappText: String get() = message.ifBlank { title }

    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("title", title)
        .put("message", message)
        .put("time", timeMillis)
        .put("repeat", repeat.name)
        .put("phone", phone)
        .put("enabled", enabled)

    companion object {
        fun fromJson(o: JSONObject) = Reminder(
            id = o.getInt("id"),
            title = o.getString("title"),
            message = o.optString("message"),
            timeMillis = o.getLong("time"),
            repeat = Repeat.valueOf(o.getString("repeat")),
            phone = o.optString("phone"),
            enabled = o.optBoolean("enabled", true),
        )
    }
}
