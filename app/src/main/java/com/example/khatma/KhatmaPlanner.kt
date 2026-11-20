package com.example.khatma

import com.example.data.model.SurahInfo
import kotlin.math.ceil

data class KhatmaSummary(
    val durationDays: Int,
    val currentPage: Int,
    val totalPages: Int,
    val dailyTargetPages: Int,
    val todayPagesRead: Int,
    val progressFraction: Float,
    val currentJuz: Int,
    val targetSurahId: Int,
    val targetSurahName: String,
    val streakDays: Int,
    val isTodayComplete: Boolean
)

object KhatmaPlanner {
    const val TOTAL_MUSHAF_PAGES = 604

    val durationPresets = listOf(15, 30, 60, 90)

    fun dailyPagesForDuration(durationDays: Int): Int {
        val safeDays = durationDays.coerceIn(7, 365)
        return ceil(TOTAL_MUSHAF_PAGES.toDouble() / safeDays.toDouble()).toInt().coerceAtLeast(2)
    }

    fun computeSummary(
        durationDays: Int,
        currentPage: Int,
        todayPagesRead: Int,
        lastLoggedDate: String,
        todayDateKey: String,
        streakDays: Int,
        surahs: List<SurahInfo>
    ): KhatmaSummary {
        val safePage = currentPage.coerceIn(1, TOTAL_MUSHAF_PAGES)
        val dailyTarget = dailyPagesForDuration(durationDays)
        val effectiveTodayRead = if (lastLoggedDate == todayDateKey) todayPagesRead else 0
        val currentJuz = (((safePage - 1) / 20) + 1).coerceIn(1, 30)

        val matchedSurah = surahs.lastOrNull { it.page <= safePage } ?: surahs.firstOrNull()
        val progress = ((safePage - 1).toFloat() / (TOTAL_MUSHAF_PAGES - 1).toFloat()).coerceIn(0f, 1f)

        return KhatmaSummary(
            durationDays = durationDays,
            currentPage = safePage,
            totalPages = TOTAL_MUSHAF_PAGES,
            dailyTargetPages = dailyTarget,
            todayPagesRead = effectiveTodayRead,
            progressFraction = progress,
            currentJuz = currentJuz,
            targetSurahId = matchedSurah?.id ?: 1,
            targetSurahName = matchedSurah?.name ?: "الفاتحة",
            streakDays = streakDays,
            isTodayComplete = effectiveTodayRead >= dailyTarget
        )
    }
}
