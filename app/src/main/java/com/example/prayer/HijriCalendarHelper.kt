package com.example.prayer

import android.icu.util.IslamicCalendar
import android.icu.util.ULocale
import com.example.util.ArabicTextUtils
import java.util.Calendar
import java.util.Date

data class HijriDayEntry(
    val hijriDay: Int,
    val hijriMonth: Int, // 1..12
    val hijriYear: Int,
    val gregorianYear: Int,
    val gregorianMonth: Int, // 1..12
    val gregorianDay: Int,
    val dayOfWeek: Int, // Calendar.SUNDAY..SATURDAY (1..7)
    val isToday: Boolean,
    val isWhiteDay: Boolean,
    val isMonOrThu: Boolean,
    val occasionLabel: String?,
    val schedule: DailyPrayerSchedule
) {
    val gregorianLabelArabic: String
        get() = "${ArabicTextUtils.toEasternArabicNumerals(gregorianDay)} ${HijriCalendarHelper.gregorianMonthName(gregorianMonth)}"
}

data class HijriMonthSummary(
    val hijriYear: Int,
    val hijriMonth: Int, // 1..12
    val hijriMonthName: String,
    val firstDayOfWeekOffset: Int, // 0 for Saturday .. 6 for Friday (Arabic RTL calendar header Sat..Fri)
    val days: List<HijriDayEntry>,
    val city: PrayerCity,
    val method: PrayerCalculationMethod
)

object HijriCalendarHelper {

    private val hijriMonths = listOf(
        "محرم", "صفر", "ربيع الأول", "ربيع الآخر",
        "جمادى الأولى", "جمادى الآخرة", "رجب", "شعبان",
        "رمضان", "شوال", "ذو القعدة", "ذو الحجة"
    )

    private val gregorianMonths = listOf(
        "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
        "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
    )

    val rtlWeekdaysHeader = listOf("سبت", "أحد", "إثنين", "ثلاثاء", "أربعاء", "خميس", "جمعة")

    fun hijriMonthName(month1Based: Int): String {
        return hijriMonths.getOrElse(month1Based - 1) { "محرم" }
    }

    fun gregorianMonthName(month1Based: Int): String {
        return gregorianMonths.getOrElse(month1Based - 1) { "يناير" }
    }

    fun occasionFor(hijriMonth1Based: Int, hijriDay: Int): String? {
        return when (hijriMonth1Based to hijriDay) {
            1 to 1 -> "رأس السنة الهجرية"
            1 to 9 -> "تاسوعاء"
            1 to 10 -> "يوم عاشوراء"
            3 to 12 -> "ذكرى المولد النبوي الشريف"
            7 to 27 -> "ليلة الإسراء والمعراج"
            8 to 15 -> "ليلة النصف من شعبان"
            9 to 1 -> "غرة شهر رمضان المبارك"
            9 to 17 -> "غزوة بدر الكبرى"
            9 to 27 -> "ليلة السابع والعشرين من رمضان"
            10 to 1 -> "عيد الفطر المبارك"
            12 to 8 -> "يوم التروية"
            12 to 9 -> "يوم عرفة"
            12 to 10 -> "عيد الأضحى المبارك"
            12 to 11, 12 to 12, 12 to 13 -> "أيام التشريق"
            else -> null
        }
    }

    fun getMonthSchedule(
        monthOffset: Int,
        city: PrayerCity,
        method: PrayerCalculationMethod
    ): HijriMonthSummary {
        val uLocale = ULocale("ar@calendar=islamic-umalqura")
        val nowIslamic = IslamicCalendar(uLocale).apply { time = Date() }
        val todayHy = nowIslamic.get(IslamicCalendar.YEAR)
        val todayHm = nowIslamic.get(IslamicCalendar.MONTH)
        val todayHd = nowIslamic.get(IslamicCalendar.DAY_OF_MONTH)

        val monthCal = IslamicCalendar(uLocale).apply {
            set(todayHy, todayHm, 1)
            add(IslamicCalendar.MONTH, monthOffset)
        }
        val targetHy = monthCal.get(IslamicCalendar.YEAR)
        val targetHm0 = monthCal.get(IslamicCalendar.MONTH)
        val maxDays = monthCal.getActualMaximum(IslamicCalendar.DAY_OF_MONTH).coerceIn(29, 30)

        val firstGreg = Calendar.getInstance().apply { time = monthCal.time }
        val firstOffset = weekdayColumnIndex(firstGreg.get(Calendar.DAY_OF_WEEK))

        val days = (1..maxDays).map { hDay ->
            val dayIslamic = IslamicCalendar(uLocale).apply {
                set(targetHy, targetHm0, hDay)
            }
            val greg = Calendar.getInstance().apply { time = dayIslamic.time }
            val schedule = PrayerTimesCalculator.computeSchedule(city, method, greg)
            val dow = greg.get(Calendar.DAY_OF_WEEK)
            val hm1 = targetHm0 + 1

            HijriDayEntry(
                hijriDay = hDay,
                hijriMonth = hm1,
                hijriYear = targetHy,
                gregorianYear = greg.get(Calendar.YEAR),
                gregorianMonth = greg.get(Calendar.MONTH) + 1,
                gregorianDay = greg.get(Calendar.DAY_OF_MONTH),
                dayOfWeek = dow,
                isToday = (targetHy == todayHy && targetHm0 == todayHm && hDay == todayHd),
                isWhiteDay = hDay in 13..15,
                isMonOrThu = (dow == Calendar.MONDAY || dow == Calendar.THURSDAY),
                occasionLabel = occasionFor(hm1, hDay),
                schedule = schedule
            )
        }

        return HijriMonthSummary(
            hijriYear = targetHy,
            hijriMonth = targetHm0 + 1,
            hijriMonthName = hijriMonthName(targetHm0 + 1),
            firstDayOfWeekOffset = firstOffset,
            days = days,
            city = city,
            method = method
        )
    }

    private fun weekdayColumnIndex(calendarDayOfWeek: Int): Int {
        // Sat=0, Sun=1, Mon=2, Tue=3, Wed=4, Thu=5, Fri=6
        return when (calendarDayOfWeek) {
            Calendar.SATURDAY -> 0
            Calendar.SUNDAY -> 1
            Calendar.MONDAY -> 2
            Calendar.TUESDAY -> 3
            Calendar.WEDNESDAY -> 4
            Calendar.THURSDAY -> 5
            Calendar.FRIDAY -> 6
            else -> 0
        }
    }

    fun convertGregorianToHijri(year: Int, month1Based: Int, day: Int): String {
        val greg = Calendar.getInstance().apply {
            set(year, (month1Based - 1).coerceIn(0, 11), day.coerceIn(1, 31), 12, 0, 0)
        }
        val uLocale = ULocale("ar@calendar=islamic-umalqura")
        val hijri = IslamicCalendar(uLocale).apply { time = greg.time }
        val hDay = hijri.get(IslamicCalendar.DAY_OF_MONTH)
        val hMonth = hijri.get(IslamicCalendar.MONTH) + 1
        val hYear = hijri.get(IslamicCalendar.YEAR)
        return "${ArabicTextUtils.toEasternArabicNumerals(hDay)} ${hijriMonthName(hMonth)} ${ArabicTextUtils.toEasternArabicNumerals(hYear)} هـ"
    }

    fun convertHijriToGregorian(hijriYear: Int, hijriMonth1Based: Int, hijriDay: Int): String {
        val uLocale = ULocale("ar@calendar=islamic-umalqura")
        val hijri = IslamicCalendar(uLocale).apply {
            set(hijriYear, (hijriMonth1Based - 1).coerceIn(0, 11), hijriDay.coerceIn(1, 30))
        }
        val greg = Calendar.getInstance().apply { time = hijri.time }
        val gDay = greg.get(Calendar.DAY_OF_MONTH)
        val gMonth = greg.get(Calendar.MONTH) + 1
        val gYear = greg.get(Calendar.YEAR)
        return "${ArabicTextUtils.toEasternArabicNumerals(gDay)} ${gregorianMonthName(gMonth)} ${ArabicTextUtils.toEasternArabicNumerals(gYear)} م"
    }
}
