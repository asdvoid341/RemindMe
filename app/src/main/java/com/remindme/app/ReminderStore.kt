package com.remindme.app

import android.content.Context
import org.json.JSONArray

/** Keeps reminders as a JSON array in SharedPreferences. */
object ReminderStore {
    private const val PREFS = "reminders"
    private const val KEY_LIST = "list"
    private const val KEY_NEXT_ID = "next_id"

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    @Synchronized
    fun all(c: Context): List<Reminder> {
        val arr = JSONArray(prefs(c).getString(KEY_LIST, "[]"))
        return (0 until arr.length())
            .map { Reminder.fromJson(arr.getJSONObject(it)) }
            .sortedWith(compareByDescending<Reminder> { it.enabled }.thenBy { it.timeMillis })
    }

    @Synchronized
    fun get(c: Context, id: Int): Reminder? = all(c).firstOrNull { it.id == id }

    @Synchronized
    fun save(c: Context, reminder: Reminder) {
        write(c, all(c).filter { it.id != reminder.id } + reminder)
    }

    @Synchronized
    fun delete(c: Context, id: Int) {
        write(c, all(c).filter { it.id != id })
    }

    @Synchronized
    fun newId(c: Context): Int {
        val p = prefs(c)
        val id = p.getInt(KEY_NEXT_ID, 1)
        p.edit().putInt(KEY_NEXT_ID, id + 1).commit()
        return id
    }

    private fun write(c: Context, list: List<Reminder>) {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        prefs(c).edit().putString(KEY_LIST, arr.toString()).commit()
    }
}
