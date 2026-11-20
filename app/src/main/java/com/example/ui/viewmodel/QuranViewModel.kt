package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.NoorSakinahApp
import com.example.audio.AudioPlaybackState
import com.example.audio.QuranAudioDownloadManager.DownloadedSurahAudioInfo
import com.example.audio.SurahDownloadState
import com.example.data.local.BookmarkEntity
import com.example.data.model.AyahItem
import com.example.data.model.JuzInfo
import com.example.data.model.ReciterInfo
import com.example.data.model.RecitersCatalog
import com.example.data.model.SurahContent
import com.example.data.model.SurahInfo
import com.example.data.repository.FullQuranDownloadState
import com.example.data.repository.QuranSearchMatch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class QuranViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as NoorSakinahApp
    private val quranRepo = app.quranRepository
    private val prefsRepo = app.preferencesRepository
    private val audioController = app.audioController
    private val downloadManager = app.audioDownloadManager

    val allSurahs: List<SurahInfo> = quranRepo.getAllSurahs()
    val allJuz: List<JuzInfo> = quranRepo.getAllJuz()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow(allSurahs.map { QuranSearchMatch(it) })
    val searchResults: StateFlow<List<QuranSearchMatch>> = _searchResults.asStateFlow()

    private val _readerContent = MutableStateFlow<SurahContent?>(null)
    val readerContent: StateFlow<SurahContent?> = _readerContent.asStateFlow()

    val bookmarks: StateFlow<List<BookmarkEntity>> = quranRepo.bookmarksFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val offlineSurahIds: StateFlow<Set<Int>> = quranRepo.observeOfflineSurahIds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val fullDownloadState: StateFlow<FullQuranDownloadState> = quranRepo.fullDownloadState

    val audioState: StateFlow<AudioPlaybackState> = audioController.state
    val downloadsState: StateFlow<Map<String, SurahDownloadState>> = downloadManager.downloads

    private val _audioInventory = MutableStateFlow<List<DownloadedSurahAudioInfo>>(emptyList())
    val audioInventory: StateFlow<List<DownloadedSurahAudioInfo>> = _audioInventory.asStateFlow()

    private val _totalAudioBytes = MutableStateFlow(0L)
    val totalAudioBytes: StateFlow<Long> = _totalAudioBytes.asStateFlow()

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            val saved = prefsRepo.settingsFlow.first()
            audioController.setReciter(RecitersCatalog.byId(saved.selectedReciterId))
            audioController.setPlaybackSpeed(saved.audioPlaybackSpeed)
            audioController.setAutoAdvanceNextSurah(saved.audioAutoAdvanceNextSurah)
        }
        viewModelScope.launch {
            downloadsState.collect {
                refreshAudioInventoryInternal()
            }
        }
        refreshAudioInventory()
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _searchResults.value = quranRepo.searchAsync(query)
        }
    }

    fun openSurah(surahId: Int) {
        val info = quranRepo.getSurahById(surahId)
        _readerContent.value = SurahContent(
            surahInfo = info,
            ayahs = emptyList(),
            isComplete = false,
            isLoading = true
        )
        viewModelScope.launch {
            val loaded = quranRepo.loadSurahContent(surahId, attemptNetworkFetch = true)
            _readerContent.value = loaded
        }
    }

    fun retryLoadSurah(surahId: Int) {
        openSurah(surahId)
    }

    fun saveLastRead(surah: SurahInfo, ayahNumber: Int) {
        viewModelScope.launch {
            prefsRepo.saveLastRead(surah.id, surah.name, ayahNumber)
        }
    }

    fun toggleBookmark(surah: SurahInfo, ayah: AyahItem) {
        viewModelScope.launch {
            val key = "${surah.id}:${ayah.number}"
            val exists = bookmarks.value.any { it.key == key }
            if (exists) {
                quranRepo.removeBookmark(surah.id, ayah.number)
            } else {
                quranRepo.addBookmark(surah, ayah)
            }
        }
    }

    fun removeBookmark(surahId: Int, ayahNumber: Int) {
        viewModelScope.launch {
            quranRepo.removeBookmark(surahId, ayahNumber)
        }
    }

    fun setMushafFlowMode(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.setMushafFlowMode(enabled)
        }
    }

    fun setMushafViewMode(mode: String) {
        viewModelScope.launch {
            prefsRepo.setMushafViewMode(mode)
        }
    }

    fun adjustFontSize(delta: Float) {
        viewModelScope.launch {
            val current = prefsRepo.settingsFlow.first().quranFontSizeSp
            prefsRepo.setQuranFontSize(current + delta)
        }
    }

    // Full Quran Offline Downloader Controls
    fun startFullQuranDownload() {
        quranRepo.startFullQuranOfflineDownload()
    }

    fun cancelFullQuranDownload() {
        quranRepo.cancelFullQuranDownload()
    }

    fun clearCachedQuran() {
        viewModelScope.launch {
            quranRepo.clearCachedQuran()
            if (_searchQuery.value.isNotBlank()) {
                _searchResults.value = quranRepo.searchAsync(_searchQuery.value)
            }
        }
    }

    // Audio Controls
    fun playAyah(surah: SurahInfo, ayahNumber: Int) {
        audioController.playAyah(
            surahId = surah.id,
            surahName = surah.name,
            totalAyahs = surah.ayahCount,
            ayahNumber = ayahNumber
        )
    }

    fun togglePlayPause() = audioController.togglePlayPause()

    fun nextAyah() = audioController.nextAyah()

    fun previousAyah() = audioController.previousAyah()

    fun stopAudio() = audioController.stop()

    fun selectReciter(reciter: ReciterInfo) {
        audioController.setReciter(reciter)
        viewModelScope.launch {
            prefsRepo.setSelectedReciter(reciter.id)
        }
    }

    fun configureRangeRepeat(enabled: Boolean, startAyah: Int, endAyah: Int, repeatTarget: Int) {
        audioController.configureRangeRepeat(enabled, startAyah, endAyah, repeatTarget)
    }

    fun setPlaybackSpeed(speed: Float) {
        audioController.setPlaybackSpeed(speed)
        viewModelScope.launch { prefsRepo.setAudioPlaybackSpeed(speed) }
    }

    fun setAutoAdvanceNextSurah(enabled: Boolean) {
        audioController.setAutoAdvanceNextSurah(enabled)
        viewModelScope.launch { prefsRepo.setAudioAutoAdvanceNextSurah(enabled) }
    }

    fun setSleepTimer(minutes: Int) {
        audioController.setSleepTimer(minutes)
    }

    fun getDownloadState(reciterId: String, surah: SurahInfo): SurahDownloadState {
        return downloadManager.getStateFor(reciterId, surah)
    }

    fun downloadSurahAudio(reciter: ReciterInfo, surah: SurahInfo) {
        downloadManager.downloadSurah(reciter, surah)
    }

    fun downloadFullQuranAudio(reciter: ReciterInfo) {
        downloadManager.downloadFullQuran(reciter, allSurahs)
    }

    fun getFullQuranDownloadState(reciterId: String): SurahDownloadState {
        return downloadManager.getFullQuranDownloadState(reciterId)
    }

    fun cancelFullQuranDownload(reciterId: String) {
        downloadManager.cancelFullQuranDownload(reciterId)
    }

    fun cancelSurahDownload(reciterId: String, surahId: Int) {
        downloadManager.cancelDownload(reciterId, surahId)
    }

    fun deleteSurahAudio(reciterId: String, surahId: Int) {
        downloadManager.deleteSurahAudio(reciterId, surahId)
        refreshAudioInventory()
    }

    fun refreshAudioInventory() {
        viewModelScope.launch {
            refreshAudioInventoryInternal()
        }
    }

    fun clearAllDownloadedAudio() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                downloadManager.clearAllDownloadedAudio()
            }
            refreshAudioInventoryInternal()
        }
    }

    private suspend fun refreshAudioInventoryInternal() {
        val (inv, total) = withContext(Dispatchers.IO) {
            downloadManager.getDownloadedAudioInventory() to downloadManager.getTotalDownloadedBytes()
        }
        _audioInventory.value = inv
        _totalAudioBytes.value = total
    }
}
