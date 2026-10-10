package com.remindme.app

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.ContactsContract
import android.text.format.DateFormat
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.isVisible
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class EditReminderActivity : AppCompatActivity() {
    private var existing: Reminder? = null
    private val cal: Calendar = Calendar.getInstance()

    private lateinit var titleLayout: TextInputLayout
    private lateinit var titleInput: TextInputEditText
    private lateinit var messageInput: TextInputEditText
    private lateinit var phoneLayout: TextInputLayout
    private lateinit var phoneInput: TextInputEditText
    private lateinit var dateButton: Button
    private lateinit var timeButton: Button
    private lateinit var repeatSpinner: Spinner

    // Picking from the phone-number list grants temporary read access, so no READ_CONTACTS needed.
    private val pickContact =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val uri = result.data?.data ?: return@registerForActivityResult
            contentResolver.query(uri, arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER), null, null, null)
                ?.use { if (it.moveToFirst()) phoneInput.setText(it.getString(0)) }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        titleLayout = findViewById(R.id.titleLayout)
        titleInput = findViewById(R.id.titleInput)
        messageInput = findViewById(R.id.messageInput)
        phoneLayout = findViewById(R.id.phoneLayout)
        phoneInput = findViewById(R.id.phoneInput)
        dateButton = findViewById(R.id.dateButton)
        timeButton = findViewById(R.id.timeButton)
        repeatSpinner = findViewById(R.id.repeatSpinner)

        repeatSpinner.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, Repeat.entries.map { it.label },
        )

        val id = intent.getIntExtra(EXTRA_ID, -1)
        existing = if (id > 0) ReminderStore.get(this, id) else null
        val r = existing
        if (r != null) {
            title = "Edit reminder"
            titleInput.setText(r.title)
            messageInput.setText(r.message)
            phoneInput.setText(if (r.phone.isBlank()) "" else "+${r.phone}")
            repeatSpinner.setSelection(r.repeat.ordinal)
            cal.timeInMillis = r.timeMillis
        } else {
            title = "New reminder"
            cal.add(Calendar.HOUR_OF_DAY, 1)
            cal.set(Calendar.MINUTE, 0)
        }
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        updateDateTimeLabels()

        dateButton.setOnClickListener {
            DatePickerDialog(this, { _, y, m, d ->
                cal.set(y, m, d)
                updateDateTimeLabels()
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).apply {
                datePicker.minDate = System.currentTimeMillis() - 1000
            }.show()
        }
        timeButton.setOnClickListener {
            TimePickerDialog(this, { _, h, min ->
                cal.set(Calendar.HOUR_OF_DAY, h)
                cal.set(Calendar.MINUTE, min)
                updateDateTimeLabels()
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), DateFormat.is24HourFormat(this)).show()
        }
        findViewById<Button>(R.id.contactButton).setOnClickListener {
            try {
                pickContact.launch(Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI))
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(this, "No contacts app found", Toast.LENGTH_SHORT).show()
            }
        }
        findViewById<Button>(R.id.testButton).setOnClickListener { testWhatsApp() }
        findViewById<Button>(R.id.saveButton).setOnClickListener { save() }
        findViewById<Button>(R.id.deleteButton).apply {
            isVisible = existing != null
            setOnClickListener { confirmDelete() }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun updateDateTimeLabels() {
        dateButton.text = SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()).format(cal.time)
        timeButton.text = DateFormat.getTimeFormat(this).format(cal.time)
    }

    /** Returns the normalized phone, "" if none was entered, or null (with an error shown) if invalid. */
    private fun readPhone(): String? {
        phoneLayout.error = null
        val raw = phoneInput.text?.toString().orEmpty().trim()
        if (raw.isEmpty()) return ""
        val digits = WhatsApp.normalize(raw)
        return when {
            digits.length < 8 -> {
                phoneLayout.error = "Enter a valid phone number"
                null
            }
            !raw.startsWith("+") && digits.length == 10 -> {
                phoneLayout.error = "Add the country code, e.g. +91 98765 43210"
                null
            }
            else -> digits
        }
    }

    private fun testWhatsApp() {
        val phone = readPhone() ?: return
        if (phone.isEmpty()) {
            phoneLayout.error = "Enter a number to test"
            return
        }
        val text = messageInput.text?.toString().orEmpty().ifBlank { titleInput.text?.toString().orEmpty() }
        if (!WhatsApp.open(this, phone, text)) {
            Toast.makeText(this, "WhatsApp is not installed", Toast.LENGTH_LONG).show()
        }
    }

    private fun save() {
        titleLayout.error = null
        val title = titleInput.text?.toString().orEmpty().trim()
        if (title.isEmpty()) {
            titleLayout.error = "Enter a task"
            return
        }
        val phone = readPhone() ?: return
        val repeat = Repeat.entries[repeatSpinner.selectedItemPosition]

        val now = System.currentTimeMillis()
        var time = cal.timeInMillis
        if (time <= now) {
            if (repeat == Repeat.ONCE) {
                Toast.makeText(this, "Pick a time in the future", Toast.LENGTH_SHORT).show()
                return
            }
            time = AlarmScheduler.nextOccurrence(time, repeat, now)
        }

        val reminder = Reminder(
            id = existing?.id ?: ReminderStore.newId(this),
            title = title,
            message = messageInput.text?.toString().orEmpty().trim(),
            timeMillis = time,
            repeat = repeat,
            phone = phone,
            enabled = true,
        )
        ReminderStore.save(this, reminder)
        AlarmScheduler.schedule(this, reminder)
        Toast.makeText(this, "Reminder set for ${ReminderAdapter.formatTime(time)}", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun confirmDelete() {
        val r = existing ?: return
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete reminder?")
            .setMessage(r.title)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                AlarmScheduler.cancel(this, r.id)
                ReminderStore.delete(this, r.id)
                NotificationManagerCompat.from(this).cancel(r.id)
                finish()
            }
            .show()
    }

    companion object {
        private const val EXTRA_ID = "id"

        fun intent(c: Context, id: Int?): Intent =
            Intent(c, EditReminderActivity::class.java).apply { if (id != null) putExtra(EXTRA_ID, id) }
    }
}
