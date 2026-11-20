package com.example.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.NoorSakinahApp
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

object AzkarReminderScheduler {
    const val CHANNEL_ID = "azkar_reminders_channel"
    const val EXTRA_TYPE = "reminder_type"
    const val TYPE_MORNING = "morning"
    const val TYPE_EVENING = "evening"
    private const val REQ_MORNING = 2001
    private const val REQ_EVENING = 2002

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "تذكير الأذكار اليومية",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "تنبيهات يومية لقراءة أذكار الصباح والمساء"
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    fun scheduleDaily(context: Context, type: String, enabled: Boolean, hour: Int, minute: Int) {
        ensureChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val requestCode = if (type == TYPE_MORNING) REQ_MORNING else REQ_EVENING
        val intent = Intent(context, AzkarReminderReceiver::class.java).apply {
            action = "com.example.AZKAR_REMINDER"
            putExtra(EXTRA_TYPE, type)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (!enabled) {
            alarmManager.cancel(pendingIntent)
            return
        }

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            cal.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }
}

class AzkarReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val app = context.applicationContext as? NoorSakinahApp ?: return
            CoroutineScope(Dispatchers.IO).launch {
                val settings = app.preferencesRepository.settingsFlow.first()
                AzkarReminderScheduler.scheduleDaily(
                    context,
                    AzkarReminderScheduler.TYPE_MORNING,
                    settings.morningReminderEnabled,
                    settings.morningReminderHour,
                    settings.morningReminderMinute
                )
                AzkarReminderScheduler.scheduleDaily(
                    context,
                    AzkarReminderScheduler.TYPE_EVENING,
                    settings.eveningReminderEnabled,
                    settings.eveningReminderHour,
                    settings.eveningReminderMinute
                )
            }
            return
        }

        AzkarReminderScheduler.ensureChannel(context)
        val type = intent.getStringExtra(AzkarReminderScheduler.EXTRA_TYPE) ?: AzkarReminderScheduler.TYPE_MORNING
        val isMorning = type == AzkarReminderScheduler.TYPE_MORNING
        val title = if (isMorning) "موعد أذكار الصباح" else "موعد أذكار المساء"
        val body = if (isMorning) {
            "ابدأ يومك بحصن المسلم وذكر الله تعالى لتنال البركة والسكينة."
        } else {
            "حصّن نفسك بأذكار المساء واختم يومك بذكر الله."
        }

        val openIntent = PendingIntent.getActivity(
            context,
            if (isMorning) 301 else 302,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("open_azkar_category", type)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, AzkarReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openIntent)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(if (isMorning) 201 else 202, notification)
    }
}
