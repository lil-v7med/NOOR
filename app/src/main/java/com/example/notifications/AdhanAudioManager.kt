package com.example.notifications

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class MuezzinOption(
    val id: String,
    val nameArabic: String,
    val locationArabic: String,
    val mp3Url: String,
    val sourceUrl: String = "https://www.islamcan.com/audio/adhan/azan"
)

object AdhanAudioCatalog {
    val muezzins = listOf(
        MuezzinOption(
            id = "makkah",
            nameArabic = "أذان المسجد الحرام",
            locationArabic = "مكة المكرمة",
            mp3Url = "https://www.islamcan.com/audio/adhan/azan1.mp3"
        ),
        MuezzinOption(
            id = "madinah",
            nameArabic = "أذان المسجد النبوي",
            locationArabic = "المدينة المنورة",
            mp3Url = "https://www.islamcan.com/audio/adhan/azan2.mp3"
        ),
        MuezzinOption(
            id = "aqsa",
            nameArabic = "أذان المسجد الأقصى",
            locationArabic = "القدس الشريف",
            mp3Url = "https://www.islamcan.com/audio/adhan/azan3.mp3"
        ),
        MuezzinOption(
            id = "cairo",
            nameArabic = "أذان جامع خاشع",
            locationArabic = "الأذان الكلاسيكي",
            mp3Url = "https://www.islamcan.com/audio/adhan/azan10.mp3"
        )
    )

    fun byId(id: String): MuezzinOption = muezzins.firstOrNull { it.id == id } ?: muezzins.first()
}

object AdhanAudioManager {

    private fun adhanDir(context: Context): File {
        return File(context.filesDir, "adhan").apply { if (!exists()) mkdirs() }
    }

    fun getLocalAdhanFile(context: Context, muezzinId: String): File? {
        val file = File(adhanDir(context), "$muezzinId.mp3")
        return if (file.exists() && file.length() > 10_000L) file else null
    }

    fun isDownloaded(context: Context, muezzinId: String): Boolean {
        return getLocalAdhanFile(context, muezzinId) != null
    }

    fun deleteDownloaded(context: Context, muezzinId: String) {
        val file = File(adhanDir(context), "$muezzinId.mp3")
        if (file.exists()) file.delete()
    }

    suspend fun downloadMuezzin(context: Context, muezzinId: String): Boolean = withContext(Dispatchers.IO) {
        val option = AdhanAudioCatalog.byId(muezzinId)
        val targetFile = File(adhanDir(context), "${option.id}.mp3")
        val tempFile = File(adhanDir(context), "${option.id}.mp3.tmp")
        try {
            val conn = (URL(option.mp3Url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 20000
                instanceFollowRedirects = true
            }
            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                conn.disconnect()
                return@withContext false
            }
            conn.inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buf = ByteArray(8192)
                    var read: Int
                    while (input.read(buf).also { read = it } != -1) {
                        output.write(buf, 0, read)
                    }
                    output.flush()
                }
            }
            conn.disconnect()
            if (tempFile.length() > 10_000L) {
                if (targetFile.exists()) targetFile.delete()
                tempFile.renameTo(targetFile)
                true
            } else {
                tempFile.delete()
                false
            }
        } catch (_: Exception) {
            if (tempFile.exists()) tempFile.delete()
            false
        }
    }
}
