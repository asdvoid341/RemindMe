package com.remindme.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Fired by AlarmManager when a reminder is due. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(AlarmScheduler.EXTRA_ID, -1)
        val r = ReminderStore.get(context, id) ?: return
        if (!r.enabled) return

        Notifications.show(context, r)

        if (r.repeat == Repeat.ONCE) {
            ReminderStore.save(context, r.copy(enabled = false))
        } else {
            val next = r.copy(timeMillis = AlarmScheduler.nextOccurrence(r.timeMillis, r.repeat))
            ReminderStore.save(context, next)
            AlarmScheduler.schedule(context, next)
        }
    }
}
