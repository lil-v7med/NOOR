package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ShaarawyPlayerState(
    val isActive: Boolean = false,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val lectureTitle: String = "",
    val lectureSource: String = "",
    val durationMs: Long = 0L,
    val errorMessage: String? = null
)

class ShaarawyAudioPlayer(private val appContext: Context) {
    private val _state = MutableStateFlow(ShaarawyPlayerState())
    val state: StateFlow<ShaarawyPlayerState> = _state.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null

    fun playLecture(title: String, source: String, url: String) {
        releasePlayer()
        _state.update {
            it.copy(
                isActive = true,
                isLoading = true,
                isPlaying = false,
                lectureTitle = title,
                lectureSource = source,
                errorMessage = null
            )
        }

        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(url)
                setOnPreparedListener { mp ->
                    mp.start()
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isPlaying = true,
                            durationMs = mp.duration.toLong()
                        )
                    }
                }
                setOnCompletionListener {
                    _state.update { it.copy(isPlaying = false) }
                }
                setOnErrorListener { _, _, _ ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isPlaying = false,
                            errorMessage = "تعذر تشغيل التسجيل الصوتي"
                        )
                    }
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
                    errorMessage = "تعذر تشغيل التسجيل الصوتي"
                )
            }
        }
    }

    fun togglePlayPause() {
        val mp = mediaPlayer ?: return
        if (mp.isPlaying) {
            mp.pause()
            _state.update { it.copy(isPlaying = false) }
        } else {
            mp.start()
            _state.update { it.copy(isPlaying = true) }
        }
    }

    fun stop() {
        releasePlayer()
        _state.value = ShaarawyPlayerState()
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
}
