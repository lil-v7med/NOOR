package com.example.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.NoorSakinahApp
import com.example.R
import com.example.data.local.PrayerSoundModes
import com.example.data.local.UserSettings
import com.example.prayer.PrayerTimesCalculator
import com.example.prayer.PrayerType
import com.example.util.ArabicTextUtils
import com.example.widget.NoorWidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

object PrayerAlarmScheduler {
    const val CHANNEL_ADHAN = "prayer_adhan_channel"
    const val CHANNEL_TAKBIR = "prayer_takbir_channel"
    const val CHANNEL_SILENT = "prayer_silent_channel"

    const val ACTION_PRAYER_ALARM = "com.example.PRAYER_ALARM"
    const val EXTRA_PRAYER_TYPE = "prayer_type"
    const val EXTRA_IS_PRE_ALERT = "is_pre_alert"
    const val EXTRA_SOUND_MODE = "sound_mode"
    const val EXTRA_MUEZZIN_ID = "muezzin_id"
    const val EXTRA_CITY_NAME = "city_name"

    private val alertablePrayers = listOf(
        PrayerType.FAJR,
        PrayerType.DHUHR,
        PrayerType.ASR,
        PrayerType.MAGHRIB,
        PrayerType.ISHA
    )

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val adhanChannel = NotificationChannel(
                CHANNEL_ADHAN,
                "تنبيهات الصلاة — الأذان",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "تنبيه دخول وقت الصلاة مع صوت تنبيه مرتفع"
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            }

            val takbirChannel = NotificationChannel(
                CHANNEL_TAKBIR,
                "تنبيهات الصلاة — تكبير خفيف",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "تنبيه دخول وقت الصلاة بنغمة هادئة"
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            }

            val silentChannel = NotificationChannel(
                CHANNEL_SILENT,
                "تنبيهات الصلاة — صامت",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "إشعار صامت لدخول وقت الصلاة"
                setSound(null, null)
            }

            nm.createNotificationChannels(listOf(adhanChannel, takbirChannel, silentChannel))
        }
    }

    fun channelForMode(mode: String): String {
        return when (mode) {
            PrayerSoundModes.ADHAN -> CHANNEL_ADHAN
            PrayerSoundModes.SILENT -> CHANNEL_SILENT
            else -> CHANNEL_TAKBIR
        }
    }

    fun rescheduleAll(context: Context, settings: UserSettings) {
        ensureChannels(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Always cancel existing slots first
        alertablePrayers.forEachIndexed { index, _ ->
            cancelAlarm(context, alarmManager, 4001 + index)
            cancelAlarm(context, alarmManager, 4101 + index)
        }

        if (!settings.prayerAlertsEnabled) return

        val city = PrayerTimesCalculator.cityById(settings.prayerCityId)
        val method = PrayerTimesCalculator.methodById(settings.prayerMethodId)
        val nowMs = System.currentTimeMillis()

        val todayCal = Calendar.getInstance()
        val tomorrowCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }

        val todaySchedule = PrayerTimesCalculator.computeSchedule(city, method, todayCal)
        val tomorrowSchedule = PrayerTimesCalculator.computeSchedule(city, method, tomorrowCal)

        alertablePrayers.forEachIndexed { index, prayerType ->
            if (!settings.enabledPrayerTypes.contains(prayerType.name)) return@forEachIndexed

            val todaySlot = todaySchedule.slots.first { it.type == prayerType }
            val tomorrowSlot = tomorrowSchedule.slots.first { it.type == prayerType }

            // 1. Main prayer time alarm
            val mainTriggerMs = computeNextTriggerMs(
                todayCal = todayCal,
                tomorrowCal = tomorrowCal,
                todayHour = todaySlot.hour24,
                todayMinute = todaySlot.minute,
                tomorrowHour = tomorrowSlot.hour24,
                tomorrowMinute = tomorrowSlot.minute,
                offsetMinutes = 0,
                nowMs = nowMs
            )
            scheduleOneShot(
                context = context,
                alarmManager = alarmManager,
                requestCode = 4001 + index,
                triggerAtMs = mainTriggerMs,
                prayerType = prayerType,
                isPreAlert = false,
                soundMode = settings.prayerSoundMode,
                muezzinId = settings.selectedMuezzinId,
                cityName = city.nameArabic
            )

            // 2. Optional 10-min pre-alert alarm
            if (settings.prayerPreAlertMinutes > 0) {
                val preTriggerMs = computeNextTriggerMs(
                    todayCal = todayCal,
                    tomorrowCal = tomorrowCal,
                    todayHour = todaySlot.hour24,
                    todayMinute = todaySlot.minute,
                    tomorrowHour = tomorrowSlot.hour24,
                    tomorrowMinute = tomorrowSlot.minute,
                    offsetMinutes = -settings.prayerPreAlertMinutes,
                    nowMs = nowMs
                )
                scheduleOneShot(
                    context = context,
                    alarmManager = alarmManager,
                    requestCode = 4101 + index,
                    triggerAtMs = preTriggerMs,
                    prayerType = prayerType,
                    isPreAlert = true,
                    soundMode = settings.prayerSoundMode,
                    muezzinId = settings.selectedMuezzinId,
                    cityName = city.nameArabic
                )
            }
        }
    }

    private fun computeNextTriggerMs(
        todayCal: Calendar,
        tomorrowCal: Calendar,
        todayHour: Int,
        todayMinute: Int,
        tomorrowHour: Int,
        tomorrowMinute: Int,
        offsetMinutes: Int,
        nowMs: Long
    ): Long {
        val candidateToday = (todayCal.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, todayHour)
            set(Calendar.MINUTE, todayMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MINUTE, offsetMinutes)
        }
        if (candidateToday.timeInMillis > nowMs + 5_000L) {
            return candidateToday.timeInMillis
        }
        val candidateTomorrow = (tomorrowCal.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, tomorrowHour)
            set(Calendar.MINUTE, tomorrowMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MINUTE, offsetMinutes)
        }
        return candidateTomorrow.timeInMillis
    }

    private fun scheduleOneShot(
        context: Context,
        alarmManager: AlarmManager,
        requestCode: Int,
        triggerAtMs: Long,
        prayerType: PrayerType,
        isPreAlert: Boolean,
        soundMode: String,
        muezzinId: String,
        cityName: String
    ) {
        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = ACTION_PRAYER_ALARM
            putExtra(EXTRA_PRAYER_TYPE, prayerType.name)
            putExtra(EXTRA_IS_PRE_ALERT, isPreAlert)
            putExtra(EXTRA_SOUND_MODE, soundMode)
            putExtra(EXTRA_MUEZZIN_ID, muezzinId)
            putExtra(EXTRA_CITY_NAME, cityName)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent)
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent)
        }
    }

    private fun cancelAlarm(context: Context, alarmManager: AlarmManager, requestCode: Int) {
        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = ACTION_PRAYER_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}

class PrayerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as? NoorSakinahApp
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            if (app != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    val settings = app.preferencesRepository.settingsFlow.first()
                    PrayerAlarmScheduler.rescheduleAll(context, settings)
                    NoorWidgetUpdater.updateAllWidgets(context)
                }
            }
            return
        }

        PrayerAlarmScheduler.ensureChannels(context)
        val typeName = intent.getStringExtra(PrayerAlarmScheduler.EXTRA_PRAYER_TYPE) ?: PrayerType.FAJR.name
        val prayerType = runCatching { PrayerType.valueOf(typeName) }.getOrDefault(PrayerType.FAJR)
        val isPreAlert = intent.getBooleanExtra(PrayerAlarmScheduler.EXTRA_IS_PRE_ALERT, false)
        val soundMode = intent.getStringExtra(PrayerAlarmScheduler.EXTRA_SOUND_MODE) ?: PrayerSoundModes.TAKBIR
        val muezzinId = intent.getStringExtra(PrayerAlarmScheduler.EXTRA_MUEZZIN_ID) ?: "makkah"
        val cityName = intent.getStringExtra(PrayerAlarmScheduler.EXTRA_CITY_NAME) ?: "مدينتك"

        val title = if (isPreAlert) {
            "اقترب موعد صلاة ${prayerType.labelArabic}"
        } else {
            "حان الآن موعد صلاة ${prayerType.labelArabic}"
        }
        val motivationalQuotes = listOf(
            "اترك هموم الدنيا وما يشغلك، وأقبل على ربك في صلاتك لتجد الراحة والسكينة والطمأنينة.",
            "أرحنا بها يا بلال.. الصلاة عماد الدين وقرة عيون المؤمنين.",
            "خذ زادك للآخرة، وأقبل بقلبك الخاشع إلى صلاتك.",
            "إن الصلاة كانت على المؤمنين كتاباً موقوتاً.. هلمّ إلى الفلاح والفوز برضوان الله.",
            "اقطع حبل انشغالك بملهيات الدنيا، واجعل سجودك ملجأً لقلبك وروحك."
        )
        val randomQuote = motivationalQuotes.random()

        val body = if (isPreAlert) {
            "تبقّى ${ArabicTextUtils.toEasternArabicNumerals(10)} دقائق على أذان ${prayerType.labelArabic} حسب توقيت $cityName — $randomQuote"
        } else {
            "حيّ على الصلاة، حيّ على الفلاح • توقيت $cityName — $randomQuote"
        }

        val openIntent = PendingIntent.getActivity(
            context,
            501,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = if (isPreAlert) {
            PrayerAlarmScheduler.channelForMode(PrayerSoundModes.TAKBIR)
        } else {
            PrayerAlarmScheduler.channelForMode(soundMode)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openIntent)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notifId = if (isPreAlert) 4200 + prayerType.ordinal else 4100 + prayerType.ordinal
        nm.notify(notifId, notification)

        if (!isPreAlert) {
            PrayerToneGenerator.playPreview(context, soundMode, muezzinId)
        }

        // Reschedule tomorrow's cycle & refresh Home Screen widget
        if (app != null) {
            CoroutineScope(Dispatchers.IO).launch {
                val settings = app.preferencesRepository.settingsFlow.first()
                PrayerAlarmScheduler.rescheduleAll(context, settings)
                NoorWidgetUpdater.updateAllWidgets(context)
            }
        }
    }
}
