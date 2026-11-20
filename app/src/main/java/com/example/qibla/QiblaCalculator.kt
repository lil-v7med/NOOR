package com.example.qibla

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class CityPreset(
    val nameArabic: String,
    val countryArabic: String,
    val latitude: Double,
    val longitude: Double
)

object QiblaCalculator {
    // Al-Kaaba Al-Musharrafah exact coordinates
    const val KAABA_LAT = 21.422487
    const val KAABA_LON = 39.826206
    private const val EARTH_RADIUS_KM = 6371.0

    /**
     * Computes the great-circle forward azimuth from the user's location to the Kaaba,
     * measured clockwise in degrees from True North [0, 360).
     */
    fun calculateQiblaBearing(userLat: Double, userLon: Double): Float {
        val phi1 = Math.toRadians(userLat)
        val phi2 = Math.toRadians(KAABA_LAT)
        val deltaLambda = Math.toRadians(KAABA_LON - userLon)

        val y = sin(deltaLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)
        val bearingRad = atan2(y, x)
        val bearingDeg = Math.toDegrees(bearingRad)
        return ((bearingDeg + 360.0) % 360.0).toFloat()
    }

    /**
     * Computes great-circle Haversine distance in kilometers to the Kaaba.
     */
    fun calculateDistanceKm(userLat: Double, userLon: Double): Double {
        val dLat = Math.toRadians(KAABA_LAT - userLat)
        val dLon = Math.toRadians(KAABA_LON - userLon)
        val lat1 = Math.toRadians(userLat)
        val lat2 = Math.toRadians(KAABA_LAT)

        val a = sin(dLat / 2).pow(2.0) +
            cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2.0)
        val c = 2.0 * atan2(sqrt(a), sqrt(1.0 - a))
        return EARTH_RADIUS_KM * c
    }

    /**
     * Returns shortest signed angular difference from `fromDeg` to `toDeg` in [-180, 180].
     */
    fun shortestAngleDiff(fromDeg: Float, toDeg: Float): Float {
        val diff = ((toDeg - fromDeg + 540f) % 360f) - 180f
        return diff
    }

    fun cardinalDirectionArabic(bearingDeg: Float): String {
        val normalized = ((bearingDeg % 360f) + 360f) % 360f
        return when {
            normalized < 22.5f || normalized >= 337.5f -> "شمال"
            normalized < 67.5f -> "شمال شرق"
            normalized < 112.5f -> "شرق"
            normalized < 157.5f -> "جنوب شرق"
            normalized < 202.5f -> "جنوب"
            normalized < 247.5f -> "جنوب غرب"
            normalized < 292.5f -> "غرب"
            else -> "شمال غرب"
        }
    }

    fun isAlignedWithQibla(deviceAzimuthDeg: Float, qiblaBearingDeg: Float, toleranceDeg: Float = 3.5f): Boolean {
        return abs(shortestAngleDiff(deviceAzimuthDeg, qiblaBearingDeg)) <= toleranceDeg
    }

    val cityPresets = listOf(
        CityPreset("مكة المكرمة", "السعودية", 21.4225, 39.8262),
        CityPreset("المدينة المنورة", "السعودية", 24.4672, 39.6111),
        CityPreset("الرياض", "السعودية", 24.7136, 46.6753),
        CityPreset("جدة", "السعودية", 21.4858, 39.1925),
        CityPreset("القاهرة", "مصر", 30.0444, 31.2357),
        CityPreset("دبي", "الإمارات", 25.2048, 55.2708),
        CityPreset("الدوحة", "قطر", 25.2854, 51.5310),
        CityPreset("الكويت", "الكويت", 29.3759, 47.9774),
        CityPreset("عمّان", "الأردن", 31.9454, 35.9284),
        CityPreset("بغداد", "العراق", 33.3152, 44.3661),
        CityPreset("الدار البيضاء", "المغرب", 33.5731, -7.5898),
        CityPreset("إسطنبول", "تركيا", 41.0082, 28.9784),
        CityPreset("كوالالمبور", "ماليزيا", 3.1390, 101.6869),
        CityPreset("جاكرتا", "إندونيسيا", -6.2088, 106.8456),
        CityPreset("لندن", "المملكة المتحدة", 51.5074, -0.1278),
        CityPreset("نيويورك", "الولايات المتحدة", 40.7128, -74.0060)
    )
}
