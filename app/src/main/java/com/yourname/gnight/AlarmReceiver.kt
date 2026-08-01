package com.yourname.gnight

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import java.util.Calendar

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra("id") ?: return
        val phone = intent.getStringExtra("phone") ?: return
        val message = intent.getStringExtra("message") ?: ""
        val repeat = intent.getBooleanExtra("repeat", false)
        val hour = intent.getIntExtra("hour", 0)
        val minute = intent.getIntExtra("minute", 0)

        openWhatsApp(context, phone, message)

        if (repeat) {
            scheduleAt(context, Schedule(id, phone, message, hour, minute, true, true))
        }
    }

    companion object {

        fun openWhatsApp(context: Context, phone: String, message: String) {
            val uri = Uri.parse("https://wa.me/$phone?text=${Uri.encode(message)}")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }

        private fun pendingIntentFor(context: Context, schedule: Schedule): PendingIntent {
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra("id", schedule.id)
                putExtra("phone", schedule.phone)
                putExtra("message", schedule.message)
                putExtra("repeat", schedule.repeat)
                putExtra("hour", schedule.hour)
                putExtra("minute", schedule.minute)
            }
            val requestCode = schedule.id.hashCode()
            return PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        fun scheduleAt(context: Context, schedule: Schedule) {
            val now = Calendar.getInstance()
            val target = Calendar.getInstance()
            target.set(Calendar.HOUR_OF_DAY, schedule.hour)
            target.set(Calendar.MINUTE, schedule.minute)
            target.set(Calendar.SECOND, 0)
            target.set(Calendar.MILLISECOND, 0)
            if (!target.after(now)) {
                target.add(Calendar.DAY_OF_YEAR, 1)
            }
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            am.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                target.timeInMillis,
                pendingIntentFor(context, schedule)
            )
        }

        fun cancel(context: Context, schedule: Schedule) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            am.cancel(pendingIntentFor(context, schedule))
        }
    }
}
