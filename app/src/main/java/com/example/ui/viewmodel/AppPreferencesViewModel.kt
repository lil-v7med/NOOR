package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.NoorSakinahApp
import com.example.data.backup.BackupRestoreSummary
import com.example.data.local.UserSettings
import com.example.data.model.AyahItem
import com.example.data.model.SurahInfo
import com.example.khatma.KhatmaPlanner
import com.example.khatma.KhatmaSummary
import com.example.notifications.AdhanAudioCatalog
import com.example.notifications.AdhanAudioManager
import com.example.notifications.AzkarReminderScheduler
import com.example.notifications.PrayerAlarmScheduler
import com.example.prayer.DailyPrayerSchedule
import com.example.prayer.PrayerTimesCalculator
import com.example.widget.NoorWidgetUpdater
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class MuezzinDownloadUiState(
    val downloadedIds: Set<String> = emptySet(),
    val downloadingId: String? = null,
    val statusMessage: String? = null
)

class AppPreferencesViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as NoorSakinahApp
    private val prefsRepo = app.preferencesRepository
    private val quranRepo = app.quranRepository
    private val hadithRepo = app.hadithRepository
    private val backupManager = app.backupManager

    private val minuteTick = MutableStateFlow(System.currentTimeMillis())

    val settings: StateFlow<UserSettings> = prefsRepo.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())

    val dailyVerse: Pair<SurahInfo, AyahItem> by lazy { quranRepo.getDailyVerse() }

    private val _backupStatusMessage = MutableStateFlow<String?>(null)
    val backupStatusMessage: StateFlow<String?> = _backupStatusMessage.asStateFlow()

    private val _muezzinState = MutableStateFlow(MuezzinDownloadUiState())
    val muezzinState: StateFlow<MuezzinDownloadUiState> = _muezzinState.asStateFlow()

    val prayerSchedule: StateFlow<DailyPrayerSchedule> = combine(
        prefsRepo.settingsFlow,
        minuteTick
    ) { s, _ ->
        val city = PrayerTimesCalculator.cityById(s.prayerCityId)
        val method = PrayerTimesCalculator.methodById(s.prayerMethodId)
        PrayerTimesCalculator.computeSchedule(city, method, Calendar.getInstance())
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        PrayerTimesCalculator.computeSchedule(
            PrayerTimesCalculator.cityById("cairo"),
            PrayerTimesCalculator.methodById("egypt")
        )
    )

    val khatmaSummary: StateFlow<KhatmaSummary> = combine(
        prefsRepo.settingsFlow,
        minuteTick
    ) { s, _ ->
        val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        KhatmaPlanner.computeSummary(
            durationDays = s.khatmaDurationDays,
            currentPage = s.khatmaCurrentPage,
            todayPagesRead = s.khatmaTodayPagesRead,
            lastLoggedDate = s.khatmaLastLoggedDate,
            todayDateKey = todayKey,
            streakDays = s.khatmaStreakDays,
            surahs = quranRepo.getAllSurahs()
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        KhatmaPlanner.computeSummary(
            durationDays = 30,
            currentPage = 1,
            todayPagesRead = 0,
            lastLoggedDate = "",
            todayDateKey = "",
            streakDays = 0,
            surahs = quranRepo.getAllSurahs()
        )
    )

    init {
        viewModelScope.launch {
            while (isActive) {
                delay(30_000L)
                minuteTick.value = System.currentTimeMillis()
            }
        }
        // Gentle first-run background preload for Nawawi 42 + Full Quran/Tafsir when online on device
        viewModelScope.launch {
            refreshDownloadedMuezzins()
            hadithRepo.ensureNawawiSeeded()
            if (!hadithRepo.hasSyncedFullNawawi()) {
                hadithRepo.syncNawawiFullTexts()
            }
            if (isDeviceOnlineAndInteractive()) {
                val cachedCount = quranRepo.observeOfflineSurahIds().first().size
                if (cachedCount < 114) {
                    quranRepo.startFullQuranOfflineDownload()
                }
            }
            val currentSettings = prefsRepo.settingsFlow.first()
            if (currentSettings.prayerAlertsEnabled) {
                PrayerAlarmScheduler.rescheduleAll(app, currentSettings)
            }
            NoorWidgetUpdater.updateAllWidgets(app)
        }
    }

    private fun isDeviceOnlineAndInteractive(): Boolean {
        if (Build.FINGERPRINT.equals("robolectric", ignoreCase = true)) return false
        return runCatching {
            val cm = app.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return@runCatching false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        }.getOrDefault(false)
    }

    fun refreshDownloadedMuezzins() {
        val ids = AdhanAudioCatalog.muezzins
            .filter { AdhanAudioManager.isDownloaded(app, it.id) }
            .map { it.id }
            .toSet()
        _muezzinState.value = _muezzinState.value.copy(downloadedIds = ids)
    }

    fun selectMuezzin(muezzinId: String) {
        viewModelScope.launch {
            prefsRepo.setSelectedMuezzinId(muezzinId)
            val updated = prefsRepo.settingsFlow.first()
            PrayerAlarmScheduler.rescheduleAll(app, updated)
        }
    }

    fun downloadMuezzinAdhan(muezzinId: String) {
        if (_muezzinState.value.downloadingId != null) return
        viewModelScope.launch {
            _muezzinState.value = _muezzinState.value.copy(
                downloadingId = muezzinId,
                statusMessage = null
            )
            val ok = AdhanAudioManager.downloadMuezzin(app, muezzinId)
            refreshDownloadedMuezzins()
            if (ok) {
                prefsRepo.setSelectedMuezzinId(muezzinId)
                prefsRepo.setPrayerSoundMode(com.example.data.local.PrayerSoundModes.ADHAN)
                val updated = prefsRepo.settingsFlow.first()
                PrayerAlarmScheduler.rescheduleAll(app, updated)
            }
            _muezzinState.value = _muezzinState.value.copy(
                downloadingId = null,
                statusMessage = if (ok) "تم تحميل صوت الأذان وتعيينه أوفلاين ✓" else "تعذر تحميل ملف الأذان — تأكد من الاتصال"
            )
        }
    }

    fun deleteMuezzinAdhan(muezzinId: String) {
        AdhanAudioManager.deleteDownloaded(app, muezzinId)
        refreshDownloadedMuezzins()
    }

    fun toggleDarkMode() {
        viewModelScope.launch {
            prefsRepo.setDarkMode(!settings.value.isDarkMode)
        }
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setDarkMode(enabled) }
    }

    fun setQuranFontSize(sizeSp: Float) {
        viewModelScope.launch { prefsRepo.setQuranFontSize(sizeSp) }
    }

    fun updateMorningReminder(enabled: Boolean, hour: Int, minute: Int) {
        viewModelScope.launch {
            prefsRepo.updateMorningReminder(enabled, hour, minute)
            AzkarReminderScheduler.scheduleDaily(
                app,
                AzkarReminderScheduler.TYPE_MORNING,
                enabled,
                hour,
                minute
            )
        }
    }

    fun updateEveningReminder(enabled: Boolean, hour: Int, minute: Int) {
        viewModelScope.launch {
            prefsRepo.updateEveningReminder(enabled, hour, minute)
            AzkarReminderScheduler.scheduleDaily(
                app,
                AzkarReminderScheduler.TYPE_EVENING,
                enabled,
                hour,
                minute
            )
        }
    }

    fun setPrayerCityAndMethod(cityId: String, methodId: String) {
        viewModelScope.launch {
            prefsRepo.setPrayerCityAndMethod(cityId, methodId)
            minuteTick.value = System.currentTimeMillis()
            val updated = prefsRepo.settingsFlow.first()
            PrayerAlarmScheduler.rescheduleAll(app, updated)
            NoorWidgetUpdater.updateAllWidgets(app)
        }
    }

    fun setPrayerAlertsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.setPrayerAlertsEnabled(enabled)
            val updated = prefsRepo.settingsFlow.first()
            PrayerAlarmScheduler.rescheduleAll(app, updated)
        }
    }

    fun setPrayerSoundMode(mode: String) {
        viewModelScope.launch {
            prefsRepo.setPrayerSoundMode(mode)
            val updated = prefsRepo.settingsFlow.first()
            PrayerAlarmScheduler.rescheduleAll(app, updated)
        }
    }

    fun setPrayerPreAlertMinutes(minutes: Int) {
        viewModelScope.launch {
            prefsRepo.setPrayerPreAlertMinutes(minutes)
            val updated = prefsRepo.settingsFlow.first()
            PrayerAlarmScheduler.rescheduleAll(app, updated)
        }
    }

    fun togglePrayerTypeAlert(prayerTypeName: String) {
        viewModelScope.launch {
            prefsRepo.togglePrayerTypeAlert(prayerTypeName)
            val updated = prefsRepo.settingsFlow.first()
            PrayerAlarmScheduler.rescheduleAll(app, updated)
        }
    }

    fun setKhatmaDuration(days: Int) {
        viewModelScope.launch {
            prefsRepo.setKhatmaDuration(days)
        }
    }

    fun logKhatmaToday() {
        viewModelScope.launch {
            val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val summary = khatmaSummary.value
            val remainingForToday = (summary.dailyTargetPages - summary.todayPagesRead)
                .coerceAtLeast(1)
            prefsRepo.logKhatmaPages(
                pagesReadNow = remainingForToday,
                dailyTarget = summary.dailyTargetPages,
                todayDateKey = todayKey
            )
            NoorWidgetUpdater.updateAllWidgets(app)
        }
    }

    fun resetKhatma() {
        viewModelScope.launch {
            prefsRepo.resetKhatma()
            NoorWidgetUpdater.updateAllWidgets(app)
        }
    }

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            val ok = backupManager.exportToUri(app, uri)
            _backupStatusMessage.value = if (ok) {
                "تم تصدير النسخة الاحتياطية بنجاح ✓"
            } else {
                "تعذر حفظ النسخة الاحتياطية — حاول مرة أخرى"
            }
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            val summary: BackupRestoreSummary? = backupManager.importFromUri(app, uri)
            _backupStatusMessage.value = if (summary != null) {
                "تم الاسترجاع: ${summary.bookmarksRestored} علامة، ${summary.customAzkarRestored} ذكر، ${summary.favoriteHadithsRestored} حديث مفضل ✓"
            } else {
                "ملف النسخة الاحتياطية غير صالح أو تعذرت قراءته"
            }
            NoorWidgetUpdater.updateAllWidgets(app)
        }
    }

    fun clearBackupStatusMessage() {
        _backupStatusMessage.value = null
    }
}
