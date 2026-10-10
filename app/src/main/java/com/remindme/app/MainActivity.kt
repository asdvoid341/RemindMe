package com.remindme.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton

class MainActivity : AppCompatActivity() {
    private lateinit var adapter: ReminderAdapter
    private lateinit var empty: TextView
    private lateinit var warningCard: MaterialCardView
    private lateinit var warningText: TextView
    private lateinit var warningFix: Button

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            // If the user said no (or "don't ask again"), send them to the settings screen instead.
            if (!granted) openNotificationSettings()
            checkSetup()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Notifications.createChannel(this)

        empty = findViewById(R.id.empty)
        warningCard = findViewById(R.id.warningCard)
        warningText = findViewById(R.id.warningText)
        warningFix = findViewById(R.id.warningFix)

        adapter = ReminderAdapter(
            onClick = { startActivity(EditReminderActivity.intent(this, it.id)) },
            onToggle = ::setEnabled,
        )
        findViewById<RecyclerView>(R.id.list).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }
        findViewById<ExtendedFloatingActionButton>(R.id.add).setOnClickListener {
            startActivity(EditReminderActivity.intent(this, null))
        }

        if (needsNotificationPermission()) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-register alarms in case a permission just changed or the system dropped them.
        AlarmScheduler.rescheduleAll(this)
        refresh()
        checkSetup()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.test_now -> {
            val test = Reminder(TEST_NOTIFICATION_ID, "Test notification", "Notifications are working 🎉", 0, Repeat.ONCE, "")
            if (!Notifications.show(this, test)) {
                Toast.makeText(this, "Notifications are blocked. Tap \"Fix now\" above.", Toast.LENGTH_LONG).show()
                checkSetup()
            }
            true
        }
        R.id.test_in_minute -> {
            val r = Reminder(
                id = ReminderStore.newId(this),
                title = "Test reminder",
                message = "Alarms are working 🎉",
                timeMillis = System.currentTimeMillis() + 60_000,
                repeat = Repeat.ONCE,
                phone = "",
            )
            ReminderStore.save(this, r)
            AlarmScheduler.schedule(this, r)
            refresh()
            Toast.makeText(this, "Test reminder in 1 minute. Lock the phone and wait.", Toast.LENGTH_LONG).show()
            true
        }
        R.id.app_settings -> {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun refresh() {
        val items = ReminderStore.all(this)
        adapter.submit(items)
        empty.isVisible = items.isEmpty()
    }

    /** Shows the first setting that would stop reminders from appearing, with a button to fix it. */
    private fun checkSetup() {
        val am = getSystemService(AlarmManager::class.java)
        val pm = getSystemService(PowerManager::class.java)
        val channel = getSystemService(NotificationManager::class.java).getNotificationChannel("reminders")

        when {
            !NotificationManagerCompat.from(this).areNotificationsEnabled() ||
                channel?.importance == NotificationManager.IMPORTANCE_NONE ->
                showWarning("Notifications are turned off for RemindMe, so reminders can't appear.") {
                    if (needsNotificationPermission()) {
                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        openNotificationSettings()
                    }
                }

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms() ->
                showWarning("\"Alarms & reminders\" permission is off, so reminders may arrive late or not at all.") {
                    openSettings(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName")))
                }

            !pm.isIgnoringBatteryOptimizations(packageName) ->
                showWarning("Battery optimisation can stop reminders when the phone is idle. Allow RemindMe to run unrestricted.") {
                    requestBatteryExemption()
                }

            else -> warningCard.isVisible = false
        }
    }

    private fun showWarning(text: String, fix: () -> Unit) {
        warningText.text = text
        warningFix.setOnClickListener { fix() }
        warningCard.isVisible = true
    }

    private fun needsNotificationPermission() =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED

    private fun openNotificationSettings() {
        openSettings(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
    }

    @SuppressLint("BatteryLife")
    private fun requestBatteryExemption() {
        openSettings(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName")))
    }

    /** Opens a settings screen, falling back to the app's info page if this phone doesn't have it. */
    private fun openSettings(intent: Intent) {
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        }
    }

    private fun setEnabled(r: Reminder, on: Boolean) {
        val now = System.currentTimeMillis()
        if (on && r.repeat == Repeat.ONCE && r.timeMillis <= now) {
            Toast.makeText(this, "That time has passed. Open the reminder to pick a new time.", Toast.LENGTH_LONG).show()
            refresh()
            return
        }
        val updated = r.copy(
            enabled = on,
            timeMillis = if (on) AlarmScheduler.nextOccurrence(r.timeMillis, r.repeat, now) else r.timeMillis,
        )
        ReminderStore.save(this, updated)
        AlarmScheduler.schedule(this, updated)
        refresh()
    }

    companion object {
        private const val TEST_NOTIFICATION_ID = 999_999
    }
}
