package com.example.data.backup

import android.content.Context
import android.net.Uri
import com.example.data.local.AppDatabase
import com.example.data.local.BookmarkEntity
import com.example.data.local.CustomZikrEntity
import com.example.data.local.DailyPrayerLogEntity
import com.example.data.local.FavoriteHadithEntity
import com.example.data.local.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class BackupRestoreSummary(
    val bookmarksRestored: Int,
    val customAzkarRestored: Int,
    val favoriteHadithsRestored: Int,
    val prayerLogsRestored: Int = 0
)

class BackupManager(
    private val database: AppDatabase,
    private val prefsRepo: UserPreferencesRepository
) {

    suspend fun buildBackupJson(): String = withContext(Dispatchers.IO) {
        val settings = prefsRepo.settingsFlow.first()
        val bookmarks = database.bookmarkDao().getAllBookmarksOnce()
        val customAzkar = database.customZikrDao().getAllCustomAzkarOnce()
        val nawawiFavs = database.nawawiHadithDao().observeFavoriteNumbers().first().map { "nawawi_$it" }
        val otherFavs = database.favoriteHadithDao().getAllFavoriteIdsOnce()
        val allFavIds = (nawawiFavs + otherFavs).distinct()
        val prayerLogs = database.prayerLogDao().getAllLogsOnce()

        val root = JSONObject().apply {
            put("app", "noor_sakinah")
            put("version", 2)
            put("exportedAt", System.currentTimeMillis())

            put("settings", JSONObject().apply {
                put("isDarkMode", settings.isDarkMode)
                put("quranFontSizeSp", settings.quranFontSizeSp.toDouble())
                put("prayerCityId", settings.prayerCityId)
                put("prayerMethodId", settings.prayerMethodId)
                put("khatmaDurationDays", settings.khatmaDurationDays)
                put("khatmaCurrentPage", settings.khatmaCurrentPage)
                put("khatmaStreakDays", settings.khatmaStreakDays)
                put("lifetimeTasbeeh", settings.lifetimeTasbeeh)
                put("lastReadSurahId", settings.lastReadSurahId)
                put("lastReadSurahName", settings.lastReadSurahName)
                put("lastReadAyahNumber", settings.lastReadAyahNumber)
            })

            put("bookmarks", JSONArray().apply {
                bookmarks.forEach { bm ->
                    put(JSONObject().apply {
                        put("key", bm.key)
                        put("surahId", bm.surahId)
                        put("surahName", bm.surahName)
                        put("ayahNumber", bm.ayahNumber)
                        put("ayahText", bm.ayahText)
                        put("createdAt", bm.createdAt)
                    })
                }
            })

            put("customAzkar", JSONArray().apply {
                customAzkar.forEach { item ->
                    put(JSONObject().apply {
                        put("text", item.text)
                        put("repeatCount", item.repeatCount)
                        put("fadl", item.fadl)
                        put("createdAt", item.createdAt)
                    })
                }
            })

            put("favoriteHadithIds", JSONArray().apply {
                allFavIds.forEach { put(it) }
            })

            put("prayerLogs", JSONArray().apply {
                prayerLogs.forEach { log ->
                    put(JSONObject().apply {
                        put("dateKey", log.dateKey)
                        put("fajrDone", log.fajrDone)
                        put("dhuhrDone", log.dhuhrDone)
                        put("asrDone", log.asrDone)
                        put("maghribDone", log.maghribDone)
                        put("ishaDone", log.ishaDone)
                        put("rawatibDone", log.rawatibDone)
                        put("duhaDone", log.duhaDone)
                        put("witrDone", log.witrDone)
                        put("updatedAt", log.updatedAt)
                    })
                }
            })
        }
        root.toString(2)
    }

    suspend fun exportToUri(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = buildBackupJson()
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(json.toByteArray(Charsets.UTF_8))
                stream.flush()
            } ?: return@withContext false
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun restoreFromJson(json: String): BackupRestoreSummary = withContext(Dispatchers.IO) {
        val root = JSONObject(json)
        require(root.optString("app") == "noor_sakinah") { "Invalid backup format" }

        // Restore Settings
        root.optJSONObject("settings")?.let { sObj ->
            prefsRepo.restoreFromBackup(
                isDarkMode = sObj.optBoolean("isDarkMode", false),
                quranFontSizeSp = sObj.optDouble("quranFontSizeSp", 24.0).toFloat(),
                prayerCityId = sObj.optString("prayerCityId", "cairo"),
                prayerMethodId = sObj.optString("prayerMethodId", "egypt"),
                khatmaDurationDays = sObj.optInt("khatmaDurationDays", 30),
                khatmaCurrentPage = sObj.optInt("khatmaCurrentPage", 1),
                khatmaStreakDays = sObj.optInt("khatmaStreakDays", 0),
                lifetimeTasbeeh = sObj.optLong("lifetimeTasbeeh", 0L),
                lastReadSurahId = sObj.optInt("lastReadSurahId", 1),
                lastReadSurahName = sObj.optString("lastReadSurahName", "الفاتحة"),
                lastReadAyahNumber = sObj.optInt("lastReadAyahNumber", 1)
            )
        }

        // Restore Bookmarks
        var bmCount = 0
        val bmArr = root.optJSONArray("bookmarks")
        if (bmArr != null) {
            val bookmarkDao = database.bookmarkDao()
            for (i in 0 until bmArr.length()) {
                val obj = bmArr.getJSONObject(i)
                bookmarkDao.insertBookmark(
                    BookmarkEntity(
                        key = obj.getString("key"),
                        surahId = obj.getInt("surahId"),
                        surahName = obj.getString("surahName"),
                        ayahNumber = obj.getInt("ayahNumber"),
                        ayahText = obj.getString("ayahText"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
                bmCount++
            }
        }

        // Restore Custom Azkar (avoid duplicates by text)
        var azkarCount = 0
        val azkarArr = root.optJSONArray("customAzkar")
        if (azkarArr != null) {
            val customZikrDao = database.customZikrDao()
            val existingTexts = customZikrDao.getAllCustomAzkarOnce().map { it.text }.toHashSet()
            for (i in 0 until azkarArr.length()) {
                val obj = azkarArr.getJSONObject(i)
                val text = obj.getString("text")
                if (!existingTexts.contains(text)) {
                    customZikrDao.insertCustomZikr(
                        CustomZikrEntity(
                            text = text,
                            repeatCount = obj.optInt("repeatCount", 1).coerceAtLeast(1),
                            fadl = obj.optString("fadl", ""),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                    existingTexts.add(text)
                    azkarCount++
                }
            }
        }

        // Restore Favorite Hadiths
        var favCount = 0
        val favArr = root.optJSONArray("favoriteHadithIds")
        if (favArr != null) {
            val nawawiDao = database.nawawiHadithDao()
            val favDao = database.favoriteHadithDao()
            for (i in 0 until favArr.length()) {
                val id = favArr.getString(i)
                if (id.startsWith("nawawi_")) {
                    val num = id.removePrefix("nawawi_").toIntOrNull()
                    if (num != null) {
                        nawawiDao.setFavorite(num, true, System.currentTimeMillis())
                    }
                }
                favDao.addFavorite(FavoriteHadithEntity(hadithId = id))
                favCount++
            }
        }

        // Restore Prayer Tracker Logs
        var logsCount = 0
        val logsArr = root.optJSONArray("prayerLogs")
        if (logsArr != null) {
            val prayerDao = database.prayerLogDao()
            for (i in 0 until logsArr.length()) {
                val obj = logsArr.getJSONObject(i)
                prayerDao.upsertLog(
                    DailyPrayerLogEntity(
                        dateKey = obj.getString("dateKey"),
                        fajrDone = obj.optBoolean("fajrDone", false),
                        dhuhrDone = obj.optBoolean("dhuhrDone", false),
                        asrDone = obj.optBoolean("asrDone", false),
                        maghribDone = obj.optBoolean("maghribDone", false),
                        ishaDone = obj.optBoolean("ishaDone", false),
                        rawatibDone = obj.optBoolean("rawatibDone", false),
                        duhaDone = obj.optBoolean("duhaDone", false),
                        witrDone = obj.optBoolean("witrDone", false),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
                logsCount++
            }
        }

        BackupRestoreSummary(
            bookmarksRestored = bmCount,
            customAzkarRestored = azkarCount,
            favoriteHadithsRestored = favCount,
            prayerLogsRestored = logsCount
        )
    }

    suspend fun importFromUri(context: Context, uri: Uri): BackupRestoreSummary? =
        withContext(Dispatchers.IO) {
            try {
                val json = context.contentResolver.openInputStream(uri)
                    ?.bufferedReader()
                    ?.use { it.readText() }
                    ?: return@withContext null
                restoreFromJson(json)
            } catch (_: Exception) {
                null
            }
        }
}
