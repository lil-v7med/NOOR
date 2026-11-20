package com.example.notifications

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.RingtoneManager
import com.example.data.local.PrayerSoundModes
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

object PrayerToneGenerator {

    @Volatile
    private var activeMediaPlayer: MediaPlayer? = null

    fun stop() {
        val mp = activeMediaPlayer
        activeMediaPlayer = null
        mp?.let {
            runCatching {
                if (it.isPlaying) it.stop()
                it.reset()
                it.release()
            }
        }
    }

    fun playPreview(context: Context, mode: String, muezzinId: String = "makkah") {
        stop()
        when (mode) {
            PrayerSoundModes.SILENT -> Unit
            PrayerSoundModes.TAKBIR -> playHarmonicChime(
                frequencies = doubleArrayOf(392.0, 493.88, 587.33),
                noteDurationMs = 260
            )
            PrayerSoundModes.ADHAN -> {
                val localAdhan = AdhanAudioManager.getLocalAdhanFile(context, muezzinId)
                if (localAdhan != null && playLocalMp3(localAdhan.absolutePath)) {
                    return
                }
                val playedHarmonic = playHarmonicChime(
                    frequencies = doubleArrayOf(293.66, 349.23, 392.0, 440.0, 392.0, 349.23, 293.66),
                    noteDurationMs = 340
                )
                if (!playedHarmonic) {
                    try {
                        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                        RingtoneManager.getRingtone(context, uri)?.play()
                    } catch (_: Exception) {
                    }
                }
            }
        }
    }

    private fun playLocalMp3(filePath: String): Boolean {
        return try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                setDataSource(filePath)
                setOnCompletionListener { mp ->
                    runCatching { mp.release() }
                    if (activeMediaPlayer === mp) activeMediaPlayer = null
                }
                setOnErrorListener { mp, _, _ ->
                    runCatching { mp.release() }
                    if (activeMediaPlayer === mp) activeMediaPlayer = null
                    true
                }
                prepare()
                start()
            }
            activeMediaPlayer = player
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun playHarmonicChime(
        frequencies: DoubleArray,
        noteDurationMs: Int
    ): Boolean {
        return try {
            thread(isDaemon = true, name = "prayer-tone-player") {
                val sampleRate = 22050
                val samplesPerNote = (sampleRate * noteDurationMs) / 1000
                val totalSamples = samplesPerNote * frequencies.size
                val pcm = ShortArray(totalSamples)

                for (nIdx in frequencies.indices) {
                    val freq = frequencies[nIdx]
                    val offset = nIdx * samplesPerNote
                    for (i in 0 until samplesPerNote) {
                        val t = i.toDouble() / sampleRate.toDouble()
                        val env = sin(PI * i.toDouble() / samplesPerNote.toDouble()) * exp(-2.2 * t)
                        val wave = 0.65 * sin(2.0 * PI * freq * t) + 0.25 * sin(4.0 * PI * freq * t)
                        pcm[offset + i] = (wave * env * Short.MAX_VALUE * 0.55).toInt()
                            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                            .toShort()
                    }
                }

                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(pcm.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(pcm, 0, pcm.size)
                track.play()
                Thread.sleep((noteDurationMs * frequencies.size + 150).toLong())
                track.stop()
                track.release()
            }
            true
        } catch (_: Exception) {
            false
        }
    }
}
