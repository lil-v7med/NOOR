package com.example.prayer

import android.icu.util.IslamicCalendar
import android.icu.util.ULocale
import java.util.Calendar
import java.util.Date
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tan

data class PrayerCity(
    val id: String,
    val nameArabic: String,
    val countryArabic: String,
    val latitude: Double,
    val longitude: Double,
    val timeZoneHours: Double,
    val defaultMethodId: String
)

data class PrayerCalculationMethod(
    val id: String,
    val nameArabic: String,
    val fajrAngle: Double,
    val ishaAngle: Double,
    val ishaIntervalMinutes: Int? = null
)

enum class PrayerType(val labelArabic: String) {
    FAJR("الفجر"),
    SUNRISE("الشروق"),
    DHUHR("الظهر"),
    ASR("العصر"),
    MAGHRIB("المغرب"),
    ISHA("العشاء")
}

data class PrayerTimeSlot(
    val type: PrayerType,
    val hour24: Int,
    val minute: Int
) {
    val totalMinutes: Int get() = hour24 * 60 + minute
}

data class DailyPrayerSchedule(
    val city: PrayerCity,
    val method: PrayerCalculationMethod,
    val slots: List<PrayerTimeSlot>,
    val nextPrayer: PrayerType,
    val nextPrayerMinutesRemaining: Int,
    val hijriFormatted: String
)

object PrayerTimesCalculator {

    val methods = listOf(
        PrayerCalculationMethod(
            id = "egypt",
            nameArabic = "الهيئة المصرية العامة للمساحة",
            fajrAngle = 19.5,
            ishaAngle = 17.5
        ),
        PrayerCalculationMethod(
            id = "makkah",
            nameArabic = "أم القرى — مكة المكرمة",
            fajrAngle = 18.5,
            ishaAngle = 0.0,
            ishaIntervalMinutes = 90
        ),
        PrayerCalculationMethod(
            id = "mwl",
            nameArabic = "رابطة العالم الإسلامي",
            fajrAngle = 18.0,
            ishaAngle = 17.0
        ),
        PrayerCalculationMethod(
            id = "karachi",
            nameArabic = "جامعة العلوم الإسلامية بكراتشي",
            fajrAngle = 18.0,
            ishaAngle = 18.0
        ),
        PrayerCalculationMethod(
            id = "isna",
            nameArabic = "الجمعية الإسلامية لأمريكا الشمالية",
            fajrAngle = 15.0,
            ishaAngle = 15.0
        )
    )

    val cities = listOf(
        PrayerCity("cairo", "القاهرة", "مصر", 30.0444, 31.2357, 2.0, "egypt"),
        PrayerCity("alexandria", "الإسكندرية", "مصر", 31.2001, 29.9187, 2.0, "egypt"),
        PrayerCity("makkah", "مكة المكرمة", "السعودية", 21.4225, 39.8262, 3.0, "makkah"),
        PrayerCity("madinah", "المدينة المنورة", "السعودية", 24.4672, 39.6111, 3.0, "makkah"),
        PrayerCity("riyadh", "الرياض", "السعودية", 24.7136, 46.6753, 3.0, "makkah"),
        PrayerCity("jeddah", "جدة", "السعودية", 21.4858, 39.1925, 3.0, "makkah"),
        PrayerCity("dubai", "دبي", "الإمارات", 25.2048, 55.2708, 4.0, "makkah"),
        PrayerCity("doha", "الدوحة", "قطر", 25.2854, 51.5310, 3.0, "makkah"),
        PrayerCity("kuwait", "الكويت", "الكويت", 29.3759, 47.9774, 3.0, "makkah"),
        PrayerCity("amman", "عمّان", "الأردن", 31.9454, 35.9284, 3.0, "mwl"),
        PrayerCity("baghdad", "بغداد", "العراق", 33.3152, 44.3661, 3.0, "egypt"),
        PrayerCity("casablanca", "الدار البيضاء", "المغرب", 33.5731, -7.5898, 1.0, "mwl"),
        PrayerCity("algiers", "الجزائر", "الجزائر", 36.7538, 3.0588, 1.0, "egypt"),
        PrayerCity("tunis", "تونس", "تونس", 36.8065, 10.1815, 1.0, "egypt"),
        PrayerCity("istanbul", "إسطنبول", "تركيا", 41.0082, 28.9784, 3.0, "mwl"),
        PrayerCity("kuala_lumpur", "كوالالمبور", "ماليزيا", 3.1390, 101.6869, 8.0, "mwl"),
        PrayerCity("jakarta", "جاكرتا", "إندونيسيا", -6.2088, 106.8456, 7.0, "mwl"),
        PrayerCity("london", "لندن", "المملكة المتحدة", 51.5074, -0.1278, 0.0, "mwl"),
        PrayerCity("new_york", "نيويورك", "الولايات المتحدة", 40.7128, -74.0060, -5.0, "isna")
    )

    fun cityById(id: String): PrayerCity = cities.firstOrNull { it.id == id } ?: cities.first()

    fun methodById(id: String): PrayerCalculationMethod = methods.firstOrNull { it.id == id } ?: methods.first()

    private val hijriMonthsArabic = listOf(
        "محرم", "صفر", "ربيع الأول", "ربيع الآخر",
        "جمادى الأولى", "جمادى الآخرة", "رجب", "شعبان",
        "رمضان", "شوال", "ذو القعدة", "ذو الحجة"
    )

    fun formatHijriDate(date: Date = Date()): String {
        return try {
            val islamic = IslamicCalendar(ULocale("ar_SA@calendar=islamic-umalqura"))
            islamic.time = date
            val day = islamic.get(IslamicCalendar.DAY_OF_MONTH)
            val monthIndex = islamic.get(IslamicCalendar.MONTH).coerceIn(0, 11)
            val year = islamic.get(IslamicCalendar.YEAR)
            "$day ${hijriMonthsArabic[monthIndex]} $year هـ"
        } catch (_: Exception) {
            "التقويم الهجري"
        }
    }

    fun computeSchedule(
        city: PrayerCity,
        method: PrayerCalculationMethod,
        calendar: Calendar = Calendar.getInstance()
    ): DailyPrayerSchedule {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val slots = calculateDaySlots(
            year = year,
            month = month,
            day = day,
            latitude = city.latitude,
            longitude = city.longitude,
            timeZoneHours = city.timeZoneHours,
            method = method
        )

        val nowMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        val upcoming = slots.firstOrNull { it.type != PrayerType.SUNRISE && it.totalMinutes > nowMinutes }
        val (nextType, remainingMin) = if (upcoming != null) {
            upcoming.type to (upcoming.totalMinutes - nowMinutes)
        } else {
            val fajrSlot = slots.first { it.type == PrayerType.FAJR }
            PrayerType.FAJR to ((24 * 60 - nowMinutes) + fajrSlot.totalMinutes)
        }

        return DailyPrayerSchedule(
            city = city,
            method = method,
            slots = slots,
            nextPrayer = nextType,
            nextPrayerMinutesRemaining = remainingMin.coerceAtLeast(1),
            hijriFormatted = formatHijriDate(calendar.time)
        )
    }

    fun calculateDaySlots(
        year: Int,
        month: Int,
        day: Int,
        latitude: Double,
        longitude: Double,
        timeZoneHours: Double,
        method: PrayerCalculationMethod
    ): List<PrayerTimeSlot> {
        val jd = julianDate(year, month, day) - longitude / (15.0 * 24.0)
        val (declination, eqTime) = sunPosition(jd)

        val noonHours = 12.0 + timeZoneHours - longitude / 15.0 - eqTime
        val sunriseAngle = 0.833
        val sunRiseDiff = hourAngle(latitude, declination, sunriseAngle)
        val sunriseHours = noonHours - sunRiseDiff
        val sunsetHours = noonHours + sunRiseDiff

        val fajrDiff = hourAngle(latitude, declination, method.fajrAngle)
        val fajrHours = noonHours - fajrDiff

        // Shafi'i / Standard Asr shadow factor = 1.0
        val asrAltitudeRad = atan(1.0 / (1.0 + tan(Math.toRadians(kotlin.math.abs(latitude - Math.toDegrees(declination))))))
        val asrAngle = -Math.toDegrees(asrAltitudeRad)
        val asrDiff = hourAngle(latitude, declination, asrAngle)
        val asrHours = noonHours + asrDiff

        val maghribHours = sunsetHours
        val ishaHours = if (method.ishaIntervalMinutes != null) {
            maghribHours + method.ishaIntervalMinutes / 60.0
        } else {
            maghribHours + (hourAngle(latitude, declination, method.ishaAngle) - sunRiseDiff)
        }

        return listOf(
            hoursToSlot(PrayerType.FAJR, fajrHours),
            hoursToSlot(PrayerType.SUNRISE, sunriseHours),
            hoursToSlot(PrayerType.DHUHR, noonHours + 2.0 / 60.0), // 2 min ihtiyat
            hoursToSlot(PrayerType.ASR, asrHours),
            hoursToSlot(PrayerType.MAGHRIB, maghribHours),
            hoursToSlot(PrayerType.ISHA, ishaHours)
        )
    }

    private fun julianDate(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    private fun sunPosition(jd: Double): Pair<Double, Double> {
        val d = jd - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * sinDeg(g) + 0.020 * sinDeg(2 * g))

        val e = 23.439 - 0.00000036 * d
        val ra = Math.toDegrees(atan2(cosDeg(e) * sinDeg(l), cosDeg(l))) / 15.0
        val eqt = q / 15.0 - fixHour(ra)
        val decl = asin(sinDeg(e) * sinDeg(l)) // in radians
        return decl to eqt
    }

    private fun hourAngle(latitudeDeg: Double, declinationRad: Double, angleDeg: Double): Double {
        val latRad = Math.toRadians(latitudeDeg)
        val zenithRad = Math.toRadians(90.0 + angleDeg)
        val numerator = cos(zenithRad) - sin(latRad) * sin(declinationRad)
        val denominator = cos(latRad) * cos(declinationRad)
        val cosVal = (numerator / denominator).coerceIn(-1.0, 1.0)
        return Math.toDegrees(acos(cosVal)) / 15.0
    }

    private fun hoursToSlot(type: PrayerType, rawHours: Double): PrayerTimeSlot {
        val normalized = fixHour(rawHours + 0.5 / 60.0)
        val hour = floor(normalized).toInt().coerceIn(0, 23)
        val minute = floor((normalized - hour) * 60.0).toInt().coerceIn(0, 59)
        return PrayerTimeSlot(type = type, hour24 = hour, minute = minute)
    }

    private fun sinDeg(deg: Double): Double = sin(Math.toRadians(deg))
    private fun cosDeg(deg: Double): Double = cos(Math.toRadians(deg))

    private fun fixAngle(a: Double): Double {
        val r = a - 360.0 * floor(a / 360.0)
        return if (r < 0) r + 360.0 else r
    }

    private fun fixHour(h: Double): Double {
        val r = h - 24.0 * floor(h / 24.0)
        return if (r < 0) r + 24.0 else r
    }
}
