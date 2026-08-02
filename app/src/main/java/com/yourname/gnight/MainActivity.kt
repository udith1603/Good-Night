package com.yourname.gnight

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ListView
import android.widget.TimePicker
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var phoneInput: EditText
    private lateinit var messageInput: EditText
    private lateinit var timePicker: TimePicker
    private lateinit var repeatCheck: CheckBox
    private lateinit var listView: ListView
    private lateinit var adapter: ArrayAdapter<String>
    private var schedules = mutableListOf<Schedule>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        phoneInput = findViewById(R.id.phoneInput)
        messageInput = findViewById(R.id.messageInput)
        timePicker = findViewById(R.id.timePicker)
        timePicker.setIs24HourView(false)
        repeatCheck = findViewById(R.id.repeatCheck)
        listView = findViewById(R.id.scheduleListView)

        schedules = ScheduleStore.load(this).toMutableList()
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        listView.adapter = adapter
        refreshList()

        findViewById<Button>(R.id.addButton).setOnClickListener { addSchedule() }

        listView.setOnItemLongClickListener { _, _, position, _ ->
            showActionsDialog(position)
            true
        }

        requestExactAlarmPermissionIfNeeded()
        requestNotificationPermissionIfNeeded()

        // Re-arm everything each time the app opens. Self-healing if an
        // alarm was ever dropped (e.g. before BootReceiver ran).
        schedules.filter { it.enabled }.forEach { AlarmReceiver.scheduleAt(this, it) }
    }

    private fun requestExactAlarmPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 31) {
            val am = getSystemService(ALARM_SERVICE) as AlarmManager
            if (!am.canScheduleExactAlarms()) {
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
            }
        }
    }

    private fun addSchedule() {
        val phone = phoneInput.text.toString().trim()
        val message = messageInput.text.toString().trim()
        if (phone.isEmpty() || message.isEmpty()) {
            Toast.makeText(this, "Enter phone and message", Toast.LENGTH_SHORT).show()
            return
        }
        val hour = timePicker.hour
        val minute = timePicker.minute
        val schedule = Schedule(
            id = System.currentTimeMillis().toString(),
            phone = phone,
            message = message,
            hour = hour,
            minute = minute,
            repeat = repeatCheck.isChecked,
            enabled = true
        )
        schedules.add(schedule)
        ScheduleStore.save(this, schedules)
        AlarmReceiver.scheduleAt(this, schedule)
        refreshList()
        phoneInput.text.clear()
        messageInput.text.clear()
        Toast.makeText(this, "Scheduled for %02d:%02d".format(hour, minute), Toast.LENGTH_SHORT).show()
    }

    private fun refreshList() {
        adapter.clear()
        schedules.forEach { s ->
            val rep = if (s.repeat) "daily" else "once"
            adapter.add("%02d:%02d  %s  [%s]\n-> %s".format(s.hour, s.minute, s.phone, rep, s.message))
        }
        adapter.notifyDataSetChanged()
    }

    private fun showActionsDialog(position: Int) {
        val s = schedules[position]
        AlertDialog.Builder(this)
            .setTitle("Schedule at %02d:%02d".format(s.hour, s.minute))
            .setItems(arrayOf("Test now (open WhatsApp)", "Delete")) { _, which ->
                when (which) {
                    0 -> AlarmReceiver.openWhatsApp(this, s.phone, s.message)
                    1 -> {
                        AlarmReceiver.cancel(this, s)
                        schedules.removeAt(position)
                        ScheduleStore.save(this, schedules)
                        refreshList()
                    }
                }
            }
            .show()
    }
}
