package com.yourname.gnight

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            ScheduleStore.load(context)
                .filter { it.enabled }
                .forEach { AlarmReceiver.scheduleAt(context, it) }
        }
    }
}
