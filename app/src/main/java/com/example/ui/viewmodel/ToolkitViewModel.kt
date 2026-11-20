package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.NoorSakinahApp
import com.example.data.local.DailyPrayerLogEntity
import com.example.data.repository.AsmaulHusnaItem
import com.example.data.repository.DuaEntry
import com.example.prayer.HijriCalendarHelper
import com.example.prayer.HijriMonthSummary
import com.example.prayer.PrayerTimesCalculator
import com.example.zakat.ZakatCalculator
import com.example.zakat.ZakatInput
import com.example.zakat.ZakatResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ToolkitViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as NoorSakinahApp
    private val toolkitRepo = app.toolkitRepository
    private val prefsRepo = app.preferencesRepository
    private val prayerLogDao = app.database.prayerLogDao()

    private fun todayDateKey(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    // Asmaul Husna state
    private val _asmaQuery = MutableStateFlow("")
    val asmaQuery: StateFlow<String> = _asmaQuery.asStateFlow()

    private val _asmaResults = MutableStateFlow(toolkitRepo.getAllAsmaulHusna())
    val asmaResults: StateFlow<List<AsmaulHusnaItem>> = _asmaResults.asStateFlow()

    // Dua Encyclopedia state
    val duaCategories: List<String> = toolkitRepo.getDuaCategories()

    private val _duaQuery = MutableStateFlow("")
    val duaQuery: StateFlow<String> = _duaQuery.asStateFlow()

    private val _selectedDuaCategory = MutableStateFlow("الكل")
    val selectedDuaCategory: StateFlow<String> = _selectedDuaCategory.asStateFlow()

    private val _duaResults = MutableStateFlow(toolkitRepo.getAllDuas())
    val duaResults: StateFlow<List<DuaEntry>> = _duaResults.asStateFlow()

    // Zakat Calculator state
    private val _zakatInput = MutableStateFlow(ZakatInput())
    val zakatInput: StateFlow<ZakatInput> = _zakatInput.asStateFlow()

    private val _zakatResult = MutableStateFlow(ZakatCalculator.calculate(ZakatInput()))
    val zakatResult: StateFlow<ZakatResult> = _zakatResult.asStateFlow()

    // Hijri Calendar & Monthly Imsakiyah state
    private val _hijriMonthOffset = MutableStateFlow(0)
    val hijriMonthOffset: StateFlow<Int> = _hijriMonthOffset.asStateFlow()

    val hijriMonthSummary: StateFlow<HijriMonthSummary> = combine(
        _hijriMonthOffset,
        prefsRepo.settingsFlow
    ) { offset, settings ->
        val city = PrayerTimesCalculator.cityById(settings.prayerCityId)
        val method = PrayerTimesCalculator.methodById(settings.prayerMethodId)
        HijriCalendarHelper.getMonthSchedule(offset, city, method)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        HijriCalendarHelper.getMonthSchedule(
            0,
            PrayerTimesCalculator.cityById("cairo"),
            PrayerTimesCalculator.methodById("egypt")
        )
    )

    // Daily Prayer & Sunnah Tracker state
    val todayPrayerLog: StateFlow<DailyPrayerLogEntity> = prayerLogDao.observeLogForDate(todayDateKey())
        .map { it ?: DailyPrayerLogEntity(dateKey = todayDateKey()) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            DailyPrayerLogEntity(dateKey = todayDateKey())
        )

    val recentPrayerLogs: StateFlow<List<DailyPrayerLogEntity>> = prayerLogDao.observeRecentLogs(14)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateAsmaQuery(query: String) {
        _asmaQuery.value = query
        _asmaResults.value = toolkitRepo.searchAsmaulHusna(query)
    }

    fun updateDuaQuery(query: String) {
        _duaQuery.value = query
        _duaResults.value = toolkitRepo.filterDuas(query, _selectedDuaCategory.value)
    }

    fun selectDuaCategory(category: String) {
        _selectedDuaCategory.value = category
        _duaResults.value = toolkitRepo.filterDuas(_duaQuery.value, category)
    }

    fun updateZakatInput(transform: (ZakatInput) -> ZakatInput) {
        val next = transform(_zakatInput.value)
        _zakatInput.value = next
        _zakatResult.value = ZakatCalculator.calculate(next)
    }

    fun resetZakatInput() {
        val fresh = ZakatInput()
        _zakatInput.value = fresh
        _zakatResult.value = ZakatCalculator.calculate(fresh)
    }

    fun previousHijriMonth() {
        _hijriMonthOffset.value -= 1
    }

    fun nextHijriMonth() {
        _hijriMonthOffset.value += 1
    }

    fun resetHijriMonthToToday() {
        _hijriMonthOffset.value = 0
    }

    fun togglePrayerTrackerSlot(slotKey: String) {
        viewModelScope.launch {
            val key = todayDateKey()
            val current = prayerLogDao.getLogForDateOnce(key) ?: DailyPrayerLogEntity(dateKey = key)
            val updated = when (slotKey) {
                "fajr" -> current.copy(fajrDone = !current.fajrDone)
                "dhuhr" -> current.copy(dhuhrDone = !current.dhuhrDone)
                "asr" -> current.copy(asrDone = !current.asrDone)
                "maghrib" -> current.copy(maghribDone = !current.maghribDone)
                "isha" -> current.copy(ishaDone = !current.ishaDone)
                "rawatib" -> current.copy(rawatibDone = !current.rawatibDone)
                "duha" -> current.copy(duhaDone = !current.duhaDone)
                "witr" -> current.copy(witrDone = !current.witrDone)
                else -> current
            }.copy(updatedAt = System.currentTimeMillis())
            prayerLogDao.upsertLog(updated)
        }
    }
}
