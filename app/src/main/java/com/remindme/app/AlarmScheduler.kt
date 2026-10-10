package com.remindme.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object AlarmScheduler {
    const val EXTRA_ID = "reminder_id"

    fun schedule(c: Context, r: Reminder) {
        val am = c.getSystemService(AlarmManager::class.java)
        val pi = pendingIntent(c, r.id)
        if (!r.enabled) {
            am.cancel(pi)
            return
        }
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        if (canExact) {
            // Alarm-clock alarms are the ones OEM battery savers (Xiaomi, Oppo, Vivo...) are least likely to delay.
            val showApp = PendingIntent.getActivity(
                c, 0, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
            )
            am.setAlarmClock(AlarmManager.AlarmClockInfo(r.timeMillis, showApp), pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, r.timeMillis, pi)
        }
    }

    fun cancel(c: Context, id: Int) {
        c.getSystemService(AlarmManager::class.java).cancel(pendingIntent(c, id))
    }

    private fun pendingIntent(c: Context, id: Int): PendingIntent = PendingIntent.getBroadcast(
        c,
        id,
        Intent(c, ReminderReceiver::class.java).putExtra(EXTRA_ID, id),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /** First occurrence after [now], stepping from [time] by the repeat interval. */
    fun nextOccurrence(time: Long, repeat: Repeat, now: Long = System.currentTimeMillis()): Long {
        val field = when (repeat) {
            Repeat.ONCE -> return time
            Repeat.DAILY -> Calendar.DAY_OF_YEAR
            Repeat.WEEKLY -> Calendar.WEEK_OF_YEAR
            Repeat.MONTHLY -> Calendar.MONTH
        }
        val cal = Calendar.getInstance().apply { timeInMillis = time }
        while (cal.timeInMillis <= now) cal.add(field, 1)
        return cal.timeInMillis
    }

    /** Re-register every alarm (after reboot, app update or clock change). */
    fun rescheduleAll(c: Context) {
        val now = System.currentTimeMillis()
        for (r in ReminderStore.all(c)) {
            if (!r.enabled) continue
            val fixed = when {
                r.timeMillis > now -> r
                // A one-time reminder missed while the phone was off: fire it shortly.
                r.repeat == Repeat.ONCE -> r.copy(timeMillis = now + 5_000)
                else -> r.copy(timeMillis = nextOccurrence(r.timeMillis, r.repeat, now))
            }
            if (fixed != r) ReminderStore.save(c, fixed)
            schedule(c, fixed)
        }
    }
}
