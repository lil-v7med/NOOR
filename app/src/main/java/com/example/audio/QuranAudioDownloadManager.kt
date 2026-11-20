package com.example.audio

import android.content.Context
import com.example.data.model.ReciterInfo
import com.example.data.model.RecitersCatalog
import com.example.data.model.SurahInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

enum class DownloadStatus {
    IDLE,
    DOWNLOADING,
    COMPLETED,
    ERROR
}

data class SurahDownloadState(
    val status: DownloadStatus = DownloadStatus.IDLE,
    val completedAyahs: Int = 0,
    val totalAyahs: Int = 0,
    val errorMessage: String? = null
)

class QuranAudioDownloadManager(private val appContext: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeJobs = mutableMapOf<String, Job>()

    private val _downloads = MutableStateFlow<Map<String, SurahDownloadState>>(emptyMap())
    val downloads: StateFlow<Map<String, SurahDownloadState>> = _downloads.asStateFlow()

    private fun key(reciterId: String, surahId: Int): String = "$reciterId:$surahId"

    private fun surahDir(reciterId: String, surahId: Int): File {
        val s3 = surahId.toString().padStart(3, '0')
        return File(appContext.filesDir, "recitations/$reciterId/$s3")
    }

    private fun ayahFile(reciterId: String, surahId: Int, ayahNumber: Int): File {
        val a3 = ayahNumber.toString().padStart(3, '0')
        return File(surahDir(reciterId, surahId), "$a3.mp3")
    }

    fun getLocalAyahFile(reciterId: String, surahId: Int, ayahNumber: Int): File? {
        val f = ayahFile(reciterId, surahId, ayahNumber)
        return if (f.exists() && f.length() > 100L) f else null
    }

    fun isSurahDownloaded(reciterId: String, surahId: Int, ayahCount: Int): Boolean {
        val dir = surahDir(reciterId, surahId)
        if (!dir.exists()) return false
        for (ayah in 1..ayahCount) {
            val f = ayahFile(reciterId, surahId, ayah)
            if (!f.exists() || f.length() <= 100L) return false
        }
        return true
    }

    fun getStateFor(reciterId: String, surah: SurahInfo): SurahDownloadState {
        val k = key(reciterId, surah.id)
        val live = _downloads.value[k]
        if (live != null && live.status == DownloadStatus.DOWNLOADING) return live
        if (isSurahDownloaded(reciterId, surah.id, surah.ayahCount)) {
            return SurahDownloadState(
                status = DownloadStatus.COMPLETED,
                completedAyahs = surah.ayahCount,
                totalAyahs = surah.ayahCount
            )
        }
        return live ?: SurahDownloadState(
            status = DownloadStatus.IDLE,
            completedAyahs = 0,
            totalAyahs = surah.ayahCount
        )
    }

    fun downloadSurah(reciter: ReciterInfo, surah: SurahInfo) {
        val k = key(reciter.id, surah.id)
        if (activeJobs[k]?.isActive == true) return

        val dir = surahDir(reciter.id, surah.id)
        if (!dir.exists()) dir.mkdirs()

        _downloads.update {
            it + (k to SurahDownloadState(
                status = DownloadStatus.DOWNLOADING,
                completedAyahs = 0,
                totalAyahs = surah.ayahCount
            ))
        }

        val job = scope.launch {
            try {
                for (ayah in 1..surah.ayahCount) {
                    if (!isActive) return@launch
                    val dest = ayahFile(reciter.id, surah.id, ayah)
                    if (!dest.exists() || dest.length() <= 100L) {
                        val url = URL(RecitersCatalog.buildAyahUrl(reciter, surah.id, ayah))
                        val conn = (url.openConnection() as HttpURLConnection).apply {
                            connectTimeout = 8000
                            readTimeout = 15000
                            requestMethod = "GET"
                        }
                        if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                            conn.disconnect()
                            throw IllegalStateException("HTTP ${conn.responseCode}")
                        }
                        val tempFile = File(dir, "$ayah.tmp")
                        conn.inputStream.use { input ->
                            FileOutputStream(tempFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                        conn.disconnect()
                        if (!tempFile.renameTo(dest)) {
                            tempFile.copyTo(dest, overwrite = true)
                            tempFile.delete()
                        }
                    }

                    _downloads.update {
                        it + (k to SurahDownloadState(
                            status = DownloadStatus.DOWNLOADING,
                            completedAyahs = ayah,
                            totalAyahs = surah.ayahCount
                        ))
                    }
                }

                _downloads.update {
                    it + (k to SurahDownloadState(
                        status = DownloadStatus.COMPLETED,
                        completedAyahs = surah.ayahCount,
                        totalAyahs = surah.ayahCount
                    ))
                }
            } catch (_: Exception) {
                _downloads.update {
                    it + (k to SurahDownloadState(
                        status = DownloadStatus.ERROR,
                        completedAyahs = 0,
                        totalAyahs = surah.ayahCount,
                        errorMessage = "تعذر تحميل التلاوة — تأكد من الاتصال بالإنترنت"
                    ))
                }
            } finally {
                activeJobs.remove(k)
            }
        }
        activeJobs[k] = job
    }

    fun cancelDownload(reciterId: String, surahId: Int) {
        val k = key(reciterId, surahId)
        activeJobs.remove(k)?.cancel()
        _downloads.update { it - k }
    }

    fun deleteSurahAudio(reciterId: String, surahId: Int) {
        val k = key(reciterId, surahId)
        activeJobs.remove(k)?.cancel()
        surahDir(reciterId, surahId).deleteRecursively()
        _downloads.update { it - k }
    }

    data class DownloadedSurahAudioInfo(
        val reciter: ReciterInfo,
        val surahId: Int,
        val fileCount: Int,
        val sizeBytes: Long
    )

    fun getDownloadedAudioInventory(): List<DownloadedSurahAudioInfo> {
        val root = File(appContext.filesDir, "recitations")
        if (!root.exists()) return emptyList()
        val entries = mutableListOf<DownloadedSurahAudioInfo>()
        root.listFiles()?.forEach { reciterDir ->
            if (!reciterDir.isDirectory) return@forEach
            val reciter = RecitersCatalog.byId(reciterDir.name)
            reciterDir.listFiles()?.forEach { surahDir ->
                if (!surahDir.isDirectory) return@forEach
                val surahId = surahDir.name.toIntOrNull() ?: return@forEach
                val mp3s = surahDir.listFiles()?.filter { it.isFile && it.name.endsWith(".mp3") } ?: emptyList()
                if (mp3s.isNotEmpty()) {
                    entries.add(
                        DownloadedSurahAudioInfo(
                            reciter = reciter,
                            surahId = surahId,
                            fileCount = mp3s.size,
                            sizeBytes = mp3s.sumOf { it.length() }
                        )
                    )
                }
            }
        }
        return entries.sortedWith(compareBy({ it.reciter.nameArabic }, { it.surahId }))
    }

    fun getTotalDownloadedBytes(): Long {
        return getDownloadedAudioInventory().sumOf { it.sizeBytes }
    }

    fun clearAllDownloadedAudio() {
        activeJobs.values.forEach { it.cancel() }
        activeJobs.clear()
        File(appContext.filesDir, "recitations").deleteRecursively()
        _downloads.value = emptyMap()
    }

    fun downloadFullQuran(reciter: ReciterInfo, allSurahs: List<SurahInfo>) {
        val k = "full_quran_${reciter.id}"
        if (activeJobs[k]?.isActive == true) return

        _downloads.update {
            it + (k to SurahDownloadState(
                status = DownloadStatus.DOWNLOADING,
                completedAyahs = 0,
                totalAyahs = 6236
            ))
        }

        val job = scope.launch {
            try {
                var downloadedCount = 0
                val totalAyahsCount = 6236
                for (surah in allSurahs) {
                    val dir = surahDir(reciter.id, surah.id)
                    if (!dir.exists()) dir.mkdirs()
                    for (ayah in 1..surah.ayahCount) {
                        if (!isActive) return@launch
                        val dest = ayahFile(reciter.id, surah.id, ayah)
                        if (!dest.exists() || dest.length() <= 100L) {
                            val url = URL(RecitersCatalog.buildAyahUrl(reciter, surah.id, ayah))
                            val conn = (url.openConnection() as HttpURLConnection).apply {
                                connectTimeout = 8000
                                readTimeout = 15000
                                requestMethod = "GET"
                            }
                            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                                val tempFile = File(dir, "$ayah.tmp")
                                conn.inputStream.use { input ->
                                    FileOutputStream(tempFile).use { output ->
                                        input.copyTo(output)
                                    }
                                }
                                conn.disconnect()
                                if (!tempFile.renameTo(dest)) {
                                    tempFile.copyTo(dest, overwrite = true)
                                    tempFile.delete()
                                }
                            } else {
                                conn.disconnect()
                            }
                        }
                        downloadedCount++
                        _downloads.update {
                            it + (k to SurahDownloadState(
                                status = DownloadStatus.DOWNLOADING,
                                completedAyahs = downloadedCount,
                                totalAyahs = totalAyahsCount
                            ))
                        }
                    }
                }

                _downloads.update {
                    it + (k to SurahDownloadState(
                        status = DownloadStatus.COMPLETED,
                        completedAyahs = totalAyahsCount,
                        totalAyahs = totalAyahsCount
                    ))
                }
            } catch (_: Exception) {
                _downloads.update {
                    it + (k to SurahDownloadState(
                        status = DownloadStatus.ERROR,
                        completedAyahs = 0,
                        totalAyahs = 6236,
                        errorMessage = "تعذر إكمال تحميل المصحف — تحقق من الاتصال بالإنترنت"
                    ))
                }
            } finally {
                activeJobs.remove(k)
            }
        }
        activeJobs[k] = job
    }

    fun getFullQuranDownloadState(reciterId: String): SurahDownloadState {
        val k = "full_quran_$reciterId"
        return _downloads.value[k] ?: SurahDownloadState(status = DownloadStatus.IDLE)
    }

    fun cancelFullQuranDownload(reciterId: String) {
        val k = "full_quran_$reciterId"
        activeJobs.remove(k)?.cancel()
        _downloads.update { it - k }
    }
}
