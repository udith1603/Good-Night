package com.yourname.gnight

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import java.util.Calendar
import java.util.TimeZone

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra("id") ?: return
        val phone = intent.getStringExtra("phone") ?: return
        val message = intent.getStringExtra("message") ?: ""
        val repeat = intent.getBooleanExtra("repeat", false)
        val hour = intent.getIntExtra("hour", 0)
        val minute = intent.getIntExtra("minute", 0)

        // Try opening WhatsApp directly - works if the screen is already on.
        try {
            openWhatsApp(context, phone, message)
        } catch (e: Exception) {
            // ignored - the full-screen notification below is the real
            // guarantee for a locked/sleeping phone
        }

        // Guaranteed path: a high-priority, full-screen notification. This is
        // the same mechanism alarm-clock and incoming-call apps use to force
        // themselves open even on a locked screen - it bypasses the
        // background-activity-start restrictions a plain startActivity()
        // call runs into when the phone is asleep.
        showFullScreenNotification(context, phone, message, id)

        if (repeat) {
            scheduleAt(context, Schedule(id, phone, message, hour, minute, true, true))
        }
    }

    companion object {
        private const val CHANNEL_ID = "goodnight_alarm"

        fun openWhatsApp(context: Context, phone: String, message: String) {
            val uri = Uri.parse("https://wa.me/$phone?text=${Uri.encode(message)}")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }

        private fun waPendingActivity(context: Context, phone: String, message: String, id: String): PendingIntent {
            val uri = Uri.parse("https://wa.me/$phone?text=${Uri.encode(message)}")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            return PendingIntent.getActivity(
                context, id.hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                    val channel = NotificationChannel(
                        CHANNEL_ID,
                        "Goodnight message alarm",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "Wakes the phone to send the scheduled goodnight message"
                        enableVibration(true)
                    }
                    manager.createNotificationChannel(channel)
                }
            }
        }

        private fun showFullScreenNotification(context: Context, phone: String, message: String, id: String) {
            ensureChannel(context)
            val pendingIntent = waPendingActivity(context, phone, message, id)

            val notification = Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_email)
                .setContentTitle("Goodnight time 💌")
                .setContentText("Tap to send your message")
                .setCategory(Notification.CATEGORY_ALARM)
                .setPriority(Notification.PRIORITY_MAX)
                .setFullScreenIntent(pendingIntent, true)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(id.hashCode(), notification)
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
            val istZone = TimeZone.getTimeZone("Asia/Kolkata")
            val now = Calendar.getInstance(istZone)
            val target = Calendar.getInstance(istZone)
            target.set(Calendar.HOUR_OF_DAY, schedule.hour)
            target.set(Calendar.MINUTE, schedule.minute)
            target.set(Calendar.SECOND, 0)
            target.set(Calendar.MILLISECOND, 0)
            if (!target.after(now)) {
                target.add(Calendar.DAY_OF_YEAR, 1)
            }
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val pendingIntent = pendingIntentFor(context, schedule)
            val showIntent = PendingIntent.getActivity(
                context, 0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            // setAlarmClock is the mechanism real alarm-clock apps use: it is
            // exempt from Doze/App Standby and from most OEM battery
            // managers, because Android treats it as a user-visible alarm
            // (you'll see the small alarm-clock icon in the status bar).
            am.setAlarmClock(AlarmManager.AlarmClockInfo(target.timeInMillis, showIntent), pendingIntent)
        }

        fun cancel(context: Context, schedule: Schedule) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            am.cancel(pendingIntentFor(context, schedule))
        }
    }
}
