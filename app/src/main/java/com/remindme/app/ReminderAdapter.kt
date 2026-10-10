package com.remindme.app

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.materialswitch.MaterialSwitch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReminderAdapter(
    private val onClick: (Reminder) -> Unit,
    private val onToggle: (Reminder, Boolean) -> Unit,
) : RecyclerView.Adapter<ReminderAdapter.Holder>() {

    private var items: List<Reminder> = emptyList()

    @SuppressLint("NotifyDataSetChanged")
    fun submit(list: List<Reminder>) {
        items = list
        notifyDataSetChanged()
    }

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_reminder, parent, false)
    )

    override fun onBindViewHolder(h: Holder, position: Int) {
        val r = items[position]
        h.title.text = r.title
        h.time.text = "${formatTime(r.timeMillis)}  ·  ${r.repeat.label}"
        h.phone.isVisible = r.phone.isNotBlank()
        h.phone.text = "WhatsApp → +${r.phone}"
        h.itemView.alpha = if (r.enabled) 1f else 0.55f

        h.toggle.setOnCheckedChangeListener(null)
        h.toggle.isChecked = r.enabled
        h.toggle.setOnCheckedChangeListener { _, checked -> onToggle(r, checked) }
        h.itemView.setOnClickListener { onClick(r) }
    }

    class Holder(v: View) : RecyclerView.ViewHolder(v) {
        val title: TextView = v.findViewById(R.id.title)
        val time: TextView = v.findViewById(R.id.time)
        val phone: TextView = v.findViewById(R.id.phone)
        val toggle: MaterialSwitch = v.findViewById(R.id.toggle)
    }

    companion object {
        fun formatTime(millis: Long): String =
            SimpleDateFormat("EEE, d MMM yyyy, h:mm a", Locale.getDefault()).format(Date(millis))
    }
}
