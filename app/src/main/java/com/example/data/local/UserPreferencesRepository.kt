package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "noor_sakinah_prefs")

object MushafViewModes {
    const val LIST = "list"
    const val FLOW = "flow"
    const val PAGES = "pages"
}

object PrayerSoundModes {
    const val ADHAN = "adhan"
    const val TAKBIR = "takbir"
    const val SILENT = "silent"
}

data class UserSettings(
    val isDarkMode: Boolean = false,
    val quranFontSizeSp: Float = 24f,
    val isMushafFlowMode: Boolean = false,
    val mushafViewMode: String = MushafViewModes.LIST,
    val showTashkeel: Boolean = true,
    val showFadl: Boolean = true,
    val lastReadSurahId: Int = 1,
    val lastReadSurahName: String = "الفاتحة",
    val lastReadAyahNumber: Int = 1,
    val selectedReciterId: String = "alafasy",
    val lifetimeTasbeeh: Long = 0L,
    val tasbeehCurrentCount: Int = 0,
    val tasbeehTarget: Int = 33, // 0 means Open
    val tasbeehPhrase: String = "سبحان الله",
    val tasbeehHapticsEnabled: Boolean = true,
    val morningReminderEnabled: Boolean = false,
    val morningReminderHour: Int = 6,
    val morningReminderMinute: Int = 0,
    val eveningReminderEnabled: Boolean = false,
    val eveningReminderHour: Int = 17,
    val eveningReminderMinute: Int = 0,
    val completedAzkarCategories: Set<String> = emptySet(),
    val completedAzkarDate: String = "",
    val prayerCityId: String = "cairo",
    val prayerMethodId: String = "egypt",
    val prayerAlertsEnabled: Boolean = false,
    val prayerSoundMode: String = PrayerSoundModes.TAKBIR,
    val selectedMuezzinId: String = "makkah",
    val prayerPreAlertMinutes: Int = 0, // 0 or 10
    val enabledPrayerTypes: Set<String> = setOf("FAJR", "DHUHR", "ASR", "MAGHRIB", "ISHA"),
    val audioPlaybackSpeed: Float = 1.0f,
    val audioAutoAdvanceNextSurah: Boolean = true,
    val khatmaDurationDays: Int = 30,
    val khatmaCurrentPage: Int = 1,
    val khatmaTodayPagesRead: Int = 0,
    val khatmaLastLoggedDate: String = "",
    val khatmaStreakDays: Int = 0
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        val QURAN_FONT_SIZE = floatPreferencesKey("quran_font_size")
        val IS_MUSHAF_FLOW = booleanPreferencesKey("is_mushaf_flow")
        val MUSHAF_VIEW_MODE = stringPreferencesKey("mushaf_view_mode")
        val SHOW_TASHKEEL = booleanPreferencesKey("show_tashkeel")
        val SHOW_FADL = booleanPreferencesKey("show_fadl")
        val LAST_READ_SURAH_ID = intPreferencesKey("last_read_surah_id")
        val LAST_READ_SURAH_NAME = stringPreferencesKey("last_read_surah_name")
        val LAST_READ_AYAH = intPreferencesKey("last_read_ayah")
        val SELECTED_RECITER = stringPreferencesKey("selected_reciter")
        val LIFETIME_TASBEEH = longPreferencesKey("lifetime_tasbeeh")
        val TASBEEH_CURRENT = intPreferencesKey("tasbeeh_current")
        val TASBEEH_TARGET = intPreferencesKey("tasbeeh_target")
        val TASBEEH_PHRASE = stringPreferencesKey("tasbeeh_phrase")
        val TASBEEH_HAPTICS = booleanPreferencesKey("tasbeeh_haptics")
        val MORNING_REMINDER_ENABLED = booleanPreferencesKey("morning_reminder_enabled")
        val MORNING_REMINDER_HOUR = intPreferencesKey("morning_reminder_hour")
        val MORNING_REMINDER_MINUTE = intPreferencesKey("morning_reminder_minute")
        val EVENING_REMINDER_ENABLED = booleanPreferencesKey("evening_reminder_enabled")
        val EVENING_REMINDER_HOUR = intPreferencesKey("evening_reminder_hour")
        val EVENING_REMINDER_MINUTE = intPreferencesKey("evening_reminder_minute")
        val COMPLETED_AZKAR_CATEGORIES = stringSetPreferencesKey("completed_azkar_categories")
        val COMPLETED_AZKAR_DATE = stringPreferencesKey("completed_azkar_date")
        val PRAYER_CITY_ID = stringPreferencesKey("prayer_city_id")
        val PRAYER_METHOD_ID = stringPreferencesKey("prayer_method_id")
        val PRAYER_ALERTS_ENABLED = booleanPreferencesKey("prayer_alerts_enabled")
        val PRAYER_SOUND_MODE = stringPreferencesKey("prayer_sound_mode")
        val SELECTED_MUEZZIN_ID = stringPreferencesKey("selected_muezzin_id")
        val PRAYER_PRE_ALERT_MIN = intPreferencesKey("prayer_pre_alert_min")
        val ENABLED_PRAYER_TYPES = stringSetPreferencesKey("enabled_prayer_types")
        val AUDIO_PLAYBACK_SPEED = floatPreferencesKey("audio_playback_speed")
        val AUDIO_AUTO_ADVANCE = booleanPreferencesKey("audio_auto_advance")
        val KHATMA_DURATION_DAYS = intPreferencesKey("khatma_duration_days")
        val KHATMA_CURRENT_PAGE = intPreferencesKey("khatma_current_page")
        val KHATMA_TODAY_PAGES_READ = intPreferencesKey("khatma_today_pages_read")
        val KHATMA_LAST_LOGGED_DATE = stringPreferencesKey("khatma_last_logged_date")
        val KHATMA_STREAK_DAYS = intPreferencesKey("khatma_streak_days")
    }

    val settingsFlow: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        val legacyFlow = prefs[Keys.IS_MUSHAF_FLOW] ?: false
        val savedViewMode = prefs[Keys.MUSHAF_VIEW_MODE]
            ?: if (legacyFlow) MushafViewModes.FLOW else MushafViewModes.LIST
        UserSettings(
            isDarkMode = prefs[Keys.IS_DARK_MODE] ?: false,
            quranFontSizeSp = prefs[Keys.QURAN_FONT_SIZE] ?: 24f,
            isMushafFlowMode = savedViewMode == MushafViewModes.FLOW,
            mushafViewMode = savedViewMode,
            showTashkeel = prefs[Keys.SHOW_TASHKEEL] ?: true,
            showFadl = prefs[Keys.SHOW_FADL] ?: true,
            lastReadSurahId = prefs[Keys.LAST_READ_SURAH_ID] ?: 1,
            lastReadSurahName = prefs[Keys.LAST_READ_SURAH_NAME] ?: "الفاتحة",
            lastReadAyahNumber = prefs[Keys.LAST_READ_AYAH] ?: 1,
            selectedReciterId = prefs[Keys.SELECTED_RECITER] ?: "alafasy",
            lifetimeTasbeeh = prefs[Keys.LIFETIME_TASBEEH] ?: 0L,
            tasbeehCurrentCount = prefs[Keys.TASBEEH_CURRENT] ?: 0,
            tasbeehTarget = prefs[Keys.TASBEEH_TARGET] ?: 33,
            tasbeehPhrase = prefs[Keys.TASBEEH_PHRASE] ?: "سبحان الله",
            tasbeehHapticsEnabled = prefs[Keys.TASBEEH_HAPTICS] ?: true,
            morningReminderEnabled = prefs[Keys.MORNING_REMINDER_ENABLED] ?: false,
            morningReminderHour = prefs[Keys.MORNING_REMINDER_HOUR] ?: 6,
            morningReminderMinute = prefs[Keys.MORNING_REMINDER_MINUTE] ?: 0,
            eveningReminderEnabled = prefs[Keys.EVENING_REMINDER_ENABLED] ?: false,
            eveningReminderHour = prefs[Keys.EVENING_REMINDER_HOUR] ?: 17,
            eveningReminderMinute = prefs[Keys.EVENING_REMINDER_MINUTE] ?: 0,
            completedAzkarCategories = prefs[Keys.COMPLETED_AZKAR_CATEGORIES] ?: emptySet(),
            completedAzkarDate = prefs[Keys.COMPLETED_AZKAR_DATE] ?: "",
            prayerCityId = prefs[Keys.PRAYER_CITY_ID] ?: "cairo",
            prayerMethodId = prefs[Keys.PRAYER_METHOD_ID] ?: "egypt",
            prayerAlertsEnabled = prefs[Keys.PRAYER_ALERTS_ENABLED] ?: false,
            prayerSoundMode = prefs[Keys.PRAYER_SOUND_MODE] ?: PrayerSoundModes.TAKBIR,
            selectedMuezzinId = prefs[Keys.SELECTED_MUEZZIN_ID] ?: "makkah",
            prayerPreAlertMinutes = prefs[Keys.PRAYER_PRE_ALERT_MIN] ?: 0,
            enabledPrayerTypes = prefs[Keys.ENABLED_PRAYER_TYPES]
                ?: setOf("FAJR", "DHUHR", "ASR", "MAGHRIB", "ISHA"),
            audioPlaybackSpeed = prefs[Keys.AUDIO_PLAYBACK_SPEED] ?: 1.0f,
            audioAutoAdvanceNextSurah = prefs[Keys.AUDIO_AUTO_ADVANCE] ?: true,
            khatmaDurationDays = prefs[Keys.KHATMA_DURATION_DAYS] ?: 30,
            khatmaCurrentPage = prefs[Keys.KHATMA_CURRENT_PAGE] ?: 1,
            khatmaTodayPagesRead = prefs[Keys.KHATMA_TODAY_PAGES_READ] ?: 0,
            khatmaLastLoggedDate = prefs[Keys.KHATMA_LAST_LOGGED_DATE] ?: "",
            khatmaStreakDays = prefs[Keys.KHATMA_STREAK_DAYS] ?: 0
        )
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.IS_DARK_MODE] = enabled }
    }

    suspend fun setQuranFontSize(sizeSp: Float) {
        context.dataStore.edit { it[Keys.QURAN_FONT_SIZE] = sizeSp.coerceIn(18f, 36f) }
    }

    suspend fun setMushafFlowMode(enabled: Boolean) {
        val mode = if (enabled) MushafViewModes.FLOW else MushafViewModes.LIST
        setMushafViewMode(mode)
    }

    suspend fun setMushafViewMode(mode: String) {
        context.dataStore.edit {
            it[Keys.MUSHAF_VIEW_MODE] = mode
            it[Keys.IS_MUSHAF_FLOW] = (mode == MushafViewModes.FLOW)
        }
    }

    suspend fun setShowTashkeel(show: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_TASHKEEL] = show }
    }

    suspend fun setShowFadl(show: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_FADL] = show }
    }

    suspend fun saveLastRead(surahId: Int, surahName: String, ayahNumber: Int) {
        context.dataStore.edit {
            it[Keys.LAST_READ_SURAH_ID] = surahId
            it[Keys.LAST_READ_SURAH_NAME] = surahName
            it[Keys.LAST_READ_AYAH] = ayahNumber
        }
    }

    suspend fun setSelectedReciter(reciterId: String) {
        context.dataStore.edit { it[Keys.SELECTED_RECITER] = reciterId }
    }

    suspend fun incrementTasbeeh() {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.TASBEEH_CURRENT] ?: 0
            val lifetime = prefs[Keys.LIFETIME_TASBEEH] ?: 0L
            prefs[Keys.TASBEEH_CURRENT] = current + 1
            prefs[Keys.LIFETIME_TASBEEH] = lifetime + 1L
        }
    }

    suspend fun resetTasbeehCycle() {
        context.dataStore.edit { it[Keys.TASBEEH_CURRENT] = 0 }
    }

    suspend fun setTasbeehTarget(target: Int) {
        context.dataStore.edit {
            it[Keys.TASBEEH_TARGET] = target
            it[Keys.TASBEEH_CURRENT] = 0
        }
    }

    suspend fun setTasbeehPhrase(phrase: String) {
        context.dataStore.edit { it[Keys.TASBEEH_PHRASE] = phrase }
    }

    suspend fun setTasbeehHaptics(enabled: Boolean) {
        context.dataStore.edit { it[Keys.TASBEEH_HAPTICS] = enabled }
    }

    suspend fun updateMorningReminder(enabled: Boolean, hour: Int, minute: Int) {
        context.dataStore.edit {
            it[Keys.MORNING_REMINDER_ENABLED] = enabled
            it[Keys.MORNING_REMINDER_HOUR] = hour
            it[Keys.MORNING_REMINDER_MINUTE] = minute
        }
    }

    suspend fun updateEveningReminder(enabled: Boolean, hour: Int, minute: Int) {
        context.dataStore.edit {
            it[Keys.EVENING_REMINDER_ENABLED] = enabled
            it[Keys.EVENING_REMINDER_HOUR] = hour
            it[Keys.EVENING_REMINDER_MINUTE] = minute
        }
    }

    suspend fun markAzkarCategoryCompleted(categoryId: String, todayDateKey: String) {
        context.dataStore.edit { prefs ->
            val savedDate = prefs[Keys.COMPLETED_AZKAR_DATE] ?: ""
            val currentSet = if (savedDate == todayDateKey) {
                prefs[Keys.COMPLETED_AZKAR_CATEGORIES] ?: emptySet()
            } else {
                emptySet()
            }
            prefs[Keys.COMPLETED_AZKAR_DATE] = todayDateKey
            prefs[Keys.COMPLETED_AZKAR_CATEGORIES] = currentSet + categoryId
        }
    }

    suspend fun setPrayerCityAndMethod(cityId: String, methodId: String) {
        context.dataStore.edit {
            it[Keys.PRAYER_CITY_ID] = cityId
            it[Keys.PRAYER_METHOD_ID] = methodId
        }
    }

    suspend fun setPrayerAlertsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.PRAYER_ALERTS_ENABLED] = enabled }
    }

    suspend fun setPrayerSoundMode(mode: String) {
        context.dataStore.edit { it[Keys.PRAYER_SOUND_MODE] = mode }
    }

    suspend fun setSelectedMuezzinId(muezzinId: String) {
        context.dataStore.edit { it[Keys.SELECTED_MUEZZIN_ID] = muezzinId }
    }

    suspend fun setAudioPlaybackSpeed(speed: Float) {
        context.dataStore.edit { it[Keys.AUDIO_PLAYBACK_SPEED] = speed.coerceIn(0.75f, 1.0f) }
    }

    suspend fun setAudioAutoAdvanceNextSurah(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUDIO_AUTO_ADVANCE] = enabled }
    }

    suspend fun setPrayerPreAlertMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.PRAYER_PRE_ALERT_MIN] = minutes }
    }

    suspend fun togglePrayerTypeAlert(prayerTypeName: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.ENABLED_PRAYER_TYPES]
                ?: setOf("FAJR", "DHUHR", "ASR", "MAGHRIB", "ISHA")
            prefs[Keys.ENABLED_PRAYER_TYPES] = if (current.contains(prayerTypeName)) {
                current - prayerTypeName
            } else {
                current + prayerTypeName
            }
        }
    }

    suspend fun setKhatmaDuration(days: Int) {
        context.dataStore.edit {
            it[Keys.KHATMA_DURATION_DAYS] = days.coerceIn(7, 365)
        }
    }

    suspend fun logKhatmaPages(pagesReadNow: Int, dailyTarget: Int, todayDateKey: String) {
        context.dataStore.edit { prefs ->
            val currentPage = prefs[Keys.KHATMA_CURRENT_PAGE] ?: 1
            val savedDate = prefs[Keys.KHATMA_LAST_LOGGED_DATE] ?: ""
            val prevToday = if (savedDate == todayDateKey) (prefs[Keys.KHATMA_TODAY_PAGES_READ] ?: 0) else 0
            val newToday = prevToday + pagesReadNow
            val wasCompletedToday = prevToday >= dailyTarget
            val nowCompletedToday = newToday >= dailyTarget

            val nextPage = (currentPage + pagesReadNow).let { p ->
                if (p > 604) 1 else p
            }
            val currentStreak = prefs[Keys.KHATMA_STREAK_DAYS] ?: 0
            val updatedStreak = if (!wasCompletedToday && nowCompletedToday) currentStreak + 1 else currentStreak

            prefs[Keys.KHATMA_CURRENT_PAGE] = nextPage
            prefs[Keys.KHATMA_TODAY_PAGES_READ] = newToday
            prefs[Keys.KHATMA_LAST_LOGGED_DATE] = todayDateKey
            prefs[Keys.KHATMA_STREAK_DAYS] = updatedStreak
        }
    }

    suspend fun resetKhatma() {
        context.dataStore.edit {
            it[Keys.KHATMA_CURRENT_PAGE] = 1
            it[Keys.KHATMA_TODAY_PAGES_READ] = 0
            it[Keys.KHATMA_LAST_LOGGED_DATE] = ""
        }
    }

    suspend fun restoreFromBackup(
        isDarkMode: Boolean,
        quranFontSizeSp: Float,
        prayerCityId: String,
        prayerMethodId: String,
        khatmaDurationDays: Int,
        khatmaCurrentPage: Int,
        khatmaStreakDays: Int,
        lifetimeTasbeeh: Long,
        lastReadSurahId: Int,
        lastReadSurahName: String,
        lastReadAyahNumber: Int
    ) {
        context.dataStore.edit {
            it[Keys.IS_DARK_MODE] = isDarkMode
            it[Keys.QURAN_FONT_SIZE] = quranFontSizeSp.coerceIn(18f, 36f)
            it[Keys.PRAYER_CITY_ID] = prayerCityId
            it[Keys.PRAYER_METHOD_ID] = prayerMethodId
            it[Keys.KHATMA_DURATION_DAYS] = khatmaDurationDays.coerceIn(7, 365)
            it[Keys.KHATMA_CURRENT_PAGE] = khatmaCurrentPage.coerceIn(1, 604)
            it[Keys.KHATMA_STREAK_DAYS] = khatmaStreakDays.coerceAtLeast(0)
            it[Keys.LIFETIME_TASBEEH] = lifetimeTasbeeh.coerceAtLeast(0L)
            it[Keys.LAST_READ_SURAH_ID] = lastReadSurahId.coerceIn(1, 114)
            it[Keys.LAST_READ_SURAH_NAME] = lastReadSurahName
            it[Keys.LAST_READ_AYAH] = lastReadAyahNumber.coerceAtLeast(1)
        }
    }
}
