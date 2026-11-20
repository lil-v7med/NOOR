package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.NoorSakinahApp
import com.example.R
import com.example.prayer.PrayerTimesCalculator
import com.example.util.ArabicTextUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

object NoorWidgetUpdater {

    fun updateAllWidgets(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, NoorHomeWidgetProvider::class.java))
        if (ids.isEmpty()) return

        val app = context.applicationContext as? NoorSakinahApp
        CoroutineScope(Dispatchers.IO).launch {
            val settings = app?.preferencesRepository?.settingsFlow?.first()
            val city = PrayerTimesCalculator.cityById(settings?.prayerCityId ?: "cairo")
            val method = PrayerTimesCalculator.methodById(settings?.prayerMethodId ?: "egypt")
            val schedule = PrayerTimesCalculator.computeSchedule(city, method, Calendar.getInstance())
            val nextSlot = schedule.slots.first { it.type == schedule.nextPrayer }

            val dailyPair = app?.quranRepository?.getDailyVerse()
            val verseSnippet = if (dailyPair != null) {
                "«${dailyPair.second.text}» — ${dailyPair.first.name}"
            } else {
                "«فَإِنَّ مَعَ الْعُسْرِ يُسْرًا» — الشرح"
            }

            val timeStr = formatSlotArabic(nextSlot.hour24, nextSlot.minute)
            val remainingStr = formatRemainingArabic(schedule.nextPrayerMinutesRemaining)

            val launchIntent = PendingIntent.getActivity(
                context,
                701,
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            ids.forEach { appWidgetId ->
                val views = RemoteViews(context.packageName, R.layout.noor_home_widget).apply {
                    setTextViewText(R.id.widget_hijri_date, schedule.hijriFormatted)
                    setTextViewText(
                        R.id.widget_next_prayer,
                        "${schedule.nextPrayer.labelArabic} • $timeStr"
                    )
                    setTextViewText(R.id.widget_prayer_remaining, "متبقي $remainingStr")
                    setTextViewText(
                        R.id.widget_city_method,
                        "${city.nameArabic} • ورد ص ${ArabicTextUtils.toEasternArabicNumerals(settings?.khatmaCurrentPage ?: 1)}"
                    )
                    setTextViewText(R.id.widget_daily_verse, verseSnippet)
                    setOnClickPendingIntent(R.id.widget_root, launchIntent)
                }
                manager.updateAppWidget(appWidgetId, views)
            }
        }
    }

    private fun formatSlotArabic(hour24: Int, minute: Int): String {
        val displayHour = if (hour24 > 12) hour24 - 12 else if (hour24 == 0) 12 else hour24
        val hStr = ArabicTextUtils.toEasternArabicNumerals(displayHour)
        val mStr = ArabicTextUtils.toEasternArabicNumerals(minute).padStart(2, '٠')
        val suffix = if (hour24 < 12) "ص" else "م"
        return "$hStr:$mStr $suffix"
    }

    private fun formatRemainingArabic(totalMinutes: Int): String {
        val hours = totalMinutes / 60
        val mins = totalMinutes % 60
        return if (hours > 0) {
            "${ArabicTextUtils.toEasternArabicNumerals(hours)}س ${ArabicTextUtils.toEasternArabicNumerals(mins)}د"
        } else {
            "${ArabicTextUtils.toEasternArabicNumerals(mins)} د"
        }
    }
}

class NoorHomeWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        NoorWidgetUpdater.updateAllWidgets(context)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        NoorWidgetUpdater.updateAllWidgets(context)
    }
}
