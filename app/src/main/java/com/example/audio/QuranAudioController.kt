package com.example.audio

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import com.example.data.model.ReciterInfo
import com.example.data.model.RecitersCatalog
import com.example.data.model.SurahInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class AudioPlaybackState(
    val isActive: Boolean = false,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val surahId: Int = 1,
    val surahName: String = "الفاتحة",
    val totalAyahsInSurah: Int = 7,
    val currentAyah: Int = 1,
    val durationMs: Long = 0L,
    val reciter: ReciterInfo = RecitersCatalog.reciters.first(),
    val isRangeRepeatEnabled: Boolean = false,
    val rangeStartAyah: Int = 1,
    val rangeEndAyah: Int = 1,
    val repeatCountTarget: Int = 3, // 0 = infinite
    val currentRepeatIteration: Int = 1,
    val isPlayingOffline: Boolean = false,
    val playbackSpeed: Float = 1.0f,
    val autoAdvanceNextSurah: Boolean = true,
    val sleepTimerMinutes: Int = 0,
    val sleepTimerRemainingSec: Int = 0,
    val errorMessage: String? = null
)

class QuranAudioController(
    private val appContext: Context,
    private val downloadManager: QuranAudioDownloadManager
) {

    private val controllerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var sleepTimerJob: Job? = null
    private var surahLookup: ((Int) -> SurahInfo?)? = null

    private val _state = MutableStateFlow(AudioPlaybackState())
    val state: StateFlow<AudioPlaybackState> = _state.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var focusRequest: AudioFocusRequest? = null

    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                if (_state.value.isPlaying) {
                    pauseInternal()
                }
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                mediaPlayer?.setVolume(1.0f, 1.0f)
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                mediaPlayer?.setVolume(0.3f, 0.3f)
            }
        }
    }

    fun setReciter(reciter: ReciterInfo) {
        val wasPlaying = _state.value.isPlaying
        val currentState = _state.value
        _state.update { it.copy(reciter = reciter) }
        if (currentState.isActive && wasPlaying) {
            playAyah(
                surahId = currentState.surahId,
                surahName = currentState.surahName,
                totalAyahs = currentState.totalAyahsInSurah,
                ayahNumber = currentState.currentAyah
            )
        }
    }

    fun configureRangeRepeat(
        enabled: Boolean,
        startAyah: Int,
        endAyah: Int,
        repeatTarget: Int
    ) {
        val safeStart = startAyah.coerceAtLeast(1)
        val safeEnd = endAyah.coerceAtLeast(safeStart)
        _state.update {
            it.copy(
                isRangeRepeatEnabled = enabled,
                rangeStartAyah = safeStart,
                rangeEndAyah = safeEnd,
                repeatCountTarget = repeatTarget,
                currentRepeatIteration = 1
            )
        }
    }

    fun setSurahLookup(lookup: (Int) -> SurahInfo?) {
        surahLookup = lookup
    }

    fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.75f, 1.0f)
        _state.update { it.copy(playbackSpeed = clamped) }
        mediaPlayer?.let { applyPlaybackSpeed(it, clamped) }
        updateServiceNotification()
    }

    fun setAutoAdvanceNextSurah(enabled: Boolean) {
        _state.update { it.copy(autoAdvanceNextSurah = enabled) }
    }

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        if (minutes <= 0) {
            _state.update { it.copy(sleepTimerMinutes = 0, sleepTimerRemainingSec = 0) }
            return
        }
        val totalSec = minutes * 60
        _state.update { it.copy(sleepTimerMinutes = minutes, sleepTimerRemainingSec = totalSec) }
        sleepTimerJob = controllerScope.launch {
            var remaining = totalSec
            while (remaining > 0 && isActive) {
                delay(1000L)
                remaining--
                _state.update { it.copy(sleepTimerRemainingSec = remaining) }
            }
            if (isActive && remaining == 0) {
                pause()
                _state.update { it.copy(sleepTimerMinutes = 0, sleepTimerRemainingSec = 0) }
            }
        }
    }

    private fun applyPlaybackSpeed(player: MediaPlayer, speed: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            runCatching {
                val params = player.playbackParams
                params.speed = speed
                player.playbackParams = params
            }
        }
    }

    fun playAyah(
        surahId: Int,
        surahName: String,
        totalAyahs: Int,
        ayahNumber: Int
    ) {
        val current = _state.value
        val startRange = if (current.isRangeRepeatEnabled && current.surahId == surahId) {
            current.rangeStartAyah.coerceIn(1, totalAyahs)
        } else {
            ayahNumber
        }
        val endRange = if (current.isRangeRepeatEnabled && current.surahId == surahId) {
            current.rangeEndAyah.coerceIn(startRange, totalAyahs)
        } else {
            totalAyahs
        }

        _state.update {
            it.copy(
                isActive = true,
                isPlaying = false,
                isLoading = true,
                surahId = surahId,
                surahName = surahName,
                totalAyahsInSurah = totalAyahs,
                currentAyah = ayahNumber,
                durationMs = 0L,
                rangeStartAyah = if (it.surahId != surahId) 1 else startRange,
                rangeEndAyah = if (it.surahId != surahId) totalAyahs else endRange,
                isRangeRepeatEnabled = if (it.surahId != surahId) false else it.isRangeRepeatEnabled,
                errorMessage = null
            )
        }

        startForegroundService()
        startStreamFor(surahId, ayahNumber, _state.value.reciter)
    }

    private fun requestFocus(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val req = focusRequest ?: AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                .setOnAudioFocusChangeListener(focusChangeListener)
                .build()
                .also { focusRequest = it }
            audioManager.requestAudioFocus(req) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                focusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun abandonFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(focusChangeListener)
        }
    }

    private fun startStreamFor(surahId: Int, ayahNumber: Int, reciter: ReciterInfo) {
        releasePlayer()
        requestFocus()
        val localFile = downloadManager.getLocalAyahFile(reciter.id, surahId, ayahNumber)
        val dataSource = localFile?.absolutePath ?: RecitersCatalog.buildAyahUrl(reciter, surahId, ayahNumber)
        val playingOffline = localFile != null
        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(dataSource)
                setOnPreparedListener { mp ->
                    val dur = runCatching { mp.duration.toLong() }.getOrDefault(0L)
                    applyPlaybackSpeed(mp, _state.value.playbackSpeed)
                    mp.start()
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isPlaying = true,
                            durationMs = dur,
                            isPlayingOffline = playingOffline,
                            errorMessage = null
                        )
                    }
                    updateServiceNotification()
                }
                setOnCompletionListener {
                    handleAyahCompleted()
                }
                setOnErrorListener { _, _, _ ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isPlaying = false,
                            errorMessage = "تعذر تشغيل التلاوة — تأكد من الاتصال بالإنترنت"
                        )
                    }
                    updateServiceNotification()
                    true
                }
                prepareAsync()
            }
            mediaPlayer = player
        } catch (_: Exception) {
            _state.update {
                it.copy(
                    isLoading = false,
                    isPlaying = false,
                    errorMessage = "تعذر تشغيل التلاوة — تأكد من الاتصال بالإنترنت"
                )
            }
        }
    }

    private fun handleAyahCompleted() {
        val s = _state.value
        if (s.isRangeRepeatEnabled) {
            if (s.currentAyah < s.rangeEndAyah) {
                val next = s.currentAyah + 1
                _state.update { it.copy(currentAyah = next, isLoading = true) }
                startStreamFor(s.surahId, next, s.reciter)
            } else {
                val infinite = s.repeatCountTarget == 0
                if (infinite || s.currentRepeatIteration < s.repeatCountTarget) {
                    _state.update {
                        it.copy(
                            currentAyah = s.rangeStartAyah,
                            currentRepeatIteration = it.currentRepeatIteration + 1,
                            isLoading = true
                        )
                    }
                    startStreamFor(s.surahId, s.rangeStartAyah, s.reciter)
                } else {
                    _state.update { it.copy(isPlaying = false, isLoading = false, currentRepeatIteration = 1) }
                    updateServiceNotification()
                }
            }
        } else {
            if (s.currentAyah < s.totalAyahsInSurah) {
                val next = s.currentAyah + 1
                _state.update { it.copy(currentAyah = next, isLoading = true) }
                startStreamFor(s.surahId, next, s.reciter)
            } else if (s.autoAdvanceNextSurah && s.surahId < 114) {
                val nextSurah = surahLookup?.invoke(s.surahId + 1)
                if (nextSurah != null) {
                    _state.update {
                        it.copy(
                            surahId = nextSurah.id,
                            surahName = nextSurah.name,
                            totalAyahsInSurah = nextSurah.ayahCount,
                            currentAyah = 1,
                            isLoading = true
                        )
                    }
                    startStreamFor(nextSurah.id, 1, s.reciter)
                } else {
                    _state.update { it.copy(isPlaying = false, isLoading = false) }
                    updateServiceNotification()
                }
            } else {
                _state.update { it.copy(isPlaying = false, isLoading = false) }
                updateServiceNotification()
            }
        }
    }

    private fun pauseInternal() {
        mediaPlayer?.let { mp ->
            if (mp.isPlaying) mp.pause()
        }
        _state.update { it.copy(isPlaying = false) }
        updateServiceNotification()
    }

    fun resumeOrPlay() {
        val s = _state.value
        val mp = mediaPlayer
        if (mp != null && !s.isPlaying) {
            requestFocus()
            mp.start()
            _state.update { it.copy(isPlaying = true, errorMessage = null) }
            updateServiceNotification()
        } else if (s.isActive && mp == null) {
            playAyah(s.surahId, s.surahName, s.totalAyahsInSurah, s.currentAyah)
        }
    }

    fun pause() {
        if (_state.value.isPlaying) {
            pauseInternal()
        }
    }

    fun togglePlayPause() {
        if (_state.value.isPlaying) {
            pause()
        } else {
            resumeOrPlay()
        }
    }

    fun nextAyah() {
        val s = _state.value
        val next = (s.currentAyah + 1).coerceAtMost(s.totalAyahsInSurah)
        if (next != s.currentAyah || s.isPlaying) {
            playAyah(s.surahId, s.surahName, s.totalAyahsInSurah, next)
        }
    }

    fun previousAyah() {
        val s = _state.value
        val prev = (s.currentAyah - 1).coerceAtLeast(1)
        playAyah(s.surahId, s.surahName, s.totalAyahsInSurah, prev)
    }

    fun currentPositionMs(): Long {
        return runCatching { mediaPlayer?.currentPosition?.toLong() ?: 0L }.getOrDefault(0L)
    }

    fun stop() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        releasePlayer()
        abandonFocus()
        _state.update {
            it.copy(
                isActive = false,
                isPlaying = false,
                isLoading = false,
                durationMs = 0L,
                sleepTimerMinutes = 0,
                sleepTimerRemainingSec = 0,
                errorMessage = null
            )
        }
        val intent = Intent(appContext, QuranAudioService::class.java).apply {
            action = QuranAudioService.ACTION_STOP
        }
        appContext.startService(intent)
    }

    private fun releasePlayer() {
        mediaPlayer?.let { mp ->
            try {
                if (mp.isPlaying) mp.stop()
                mp.reset()
                mp.release()
            } catch (_: Exception) {
            }
        }
        mediaPlayer = null
    }

    private fun startForegroundService() {
        val intent = Intent(appContext, QuranAudioService::class.java).apply {
            action = QuranAudioService.ACTION_START_OR_UPDATE
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            appContext.startForegroundService(intent)
        } else {
            appContext.startService(intent)
        }
    }

    private fun updateServiceNotification() {
        if (!_state.value.isActive) return
        val intent = Intent(appContext, QuranAudioService::class.java).apply {
            action = QuranAudioService.ACTION_START_OR_UPDATE
        }
        appContext.startService(intent)
    }
}
