package com.example.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey val key: String, // "surahId:ayahNumber"
    val surahId: Int,
    val surahName: String,
    val ayahNumber: Int,
    val ayahText: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "custom_azkar")
data class CustomZikrEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val text: String,
    val repeatCount: Int,
    val fadl: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "cached_ayahs", primaryKeys = ["surahId", "ayahNumber"])
data class CachedAyahEntity(
    val surahId: Int,
    val ayahNumber: Int,
    val text: String,
    val tafsir: String
)

@Entity(tableName = "nawawi_hadiths")
data class NawawiHadithEntity(
    @PrimaryKey val number: Int, // 1..42
    val title: String,
    val narrator: String,
    val text: String,
    val summary: String,
    val source: String,
    val grade: String,
    val sourceUrl: String,
    val isFavorite: Boolean = false,
    val favoritedAt: Long? = null
)

@Entity(tableName = "favorite_hadiths")
data class FavoriteHadithEntity(
    @PrimaryKey val hadithId: String,
    val savedAt: Long = System.currentTimeMillis()
)

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    suspend fun getAllBookmarksOnce(): List<BookmarkEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE key = :key)")
    fun isBookmarked(key: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE key = :key")
    suspend fun deleteBookmark(key: String)
}

@Dao
interface CustomZikrDao {
    @Query("SELECT * FROM custom_azkar ORDER BY createdAt DESC")
    fun getAllCustomAzkar(): Flow<List<CustomZikrEntity>>

    @Query("SELECT * FROM custom_azkar ORDER BY createdAt DESC")
    suspend fun getAllCustomAzkarOnce(): List<CustomZikrEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomZikr(item: CustomZikrEntity): Long

    @Update
    suspend fun updateCustomZikr(item: CustomZikrEntity)

    @Query("DELETE FROM custom_azkar WHERE id = :id")
    suspend fun deleteCustomZikr(id: Long)
}

data class SurahCachedCount(
    val surahId: Int,
    val ayahCount: Int
)

@Dao
interface CachedAyahDao {
    @Query("SELECT * FROM cached_ayahs WHERE surahId = :surahId ORDER BY ayahNumber ASC")
    suspend fun getAyahsForSurah(surahId: Int): List<CachedAyahEntity>

    @Query("SELECT surahId, COUNT(*) as ayahCount FROM cached_ayahs GROUP BY surahId")
    fun observeCachedSurahCounts(): Flow<List<SurahCachedCount>>

    @Query("SELECT surahId, COUNT(*) as ayahCount FROM cached_ayahs GROUP BY surahId")
    suspend fun getCachedSurahCounts(): List<SurahCachedCount>

    @Query("SELECT * FROM cached_ayahs ORDER BY surahId ASC, ayahNumber ASC")
    suspend fun getAllCachedAyahs(): List<CachedAyahEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAyahs(ayahs: List<CachedAyahEntity>)

    @Query("DELETE FROM cached_ayahs")
    suspend fun clearAllCachedAyahs()
}

@Dao
interface NawawiHadithDao {
    @Query("SELECT * FROM nawawi_hadiths ORDER BY number ASC")
    fun observeAllHadiths(): Flow<List<NawawiHadithEntity>>

    @Query("SELECT * FROM nawawi_hadiths ORDER BY number ASC")
    suspend fun getAllHadithsOnce(): List<NawawiHadithEntity>

    @Query("SELECT * FROM nawawi_hadiths WHERE isFavorite = 1 ORDER BY favoritedAt DESC, number ASC")
    fun observeFavoriteHadiths(): Flow<List<NawawiHadithEntity>>

    @Query("SELECT number FROM nawawi_hadiths WHERE isFavorite = 1 ORDER BY favoritedAt DESC")
    fun observeFavoriteNumbers(): Flow<List<Int>>

    @Query("SELECT COUNT(*) FROM nawawi_hadiths")
    suspend fun getCount(): Int

    @Query("SELECT COUNT(*) FROM nawawi_hadiths WHERE text != ''")
    fun observeSyncedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM nawawi_hadiths WHERE text != ''")
    suspend fun getSyncedCountOnce(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(items: List<NawawiHadithEntity>)

    @Query("UPDATE nawawi_hadiths SET text = :text WHERE number = :number")
    suspend fun updateFullText(number: Int, text: String)

    @Query("UPDATE nawawi_hadiths SET isFavorite = :isFavorite, favoritedAt = :favoritedAt WHERE number = :number")
    suspend fun setFavorite(number: Int, isFavorite: Boolean, favoritedAt: Long?)

    @Query("SELECT isFavorite FROM nawawi_hadiths WHERE number = :number")
    suspend fun isFavorite(number: Int): Boolean?
}

@Dao
interface FavoriteHadithDao {
    @Query("SELECT hadithId FROM favorite_hadiths ORDER BY savedAt DESC")
    fun observeFavoriteIds(): Flow<List<String>>

    @Query("SELECT hadithId FROM favorite_hadiths ORDER BY savedAt DESC")
    suspend fun getAllFavoriteIdsOnce(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(item: FavoriteHadithEntity)

    @Query("DELETE FROM favorite_hadiths WHERE hadithId = :hadithId")
    suspend fun removeFavorite(hadithId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_hadiths WHERE hadithId = :hadithId)")
    suspend fun isFavorite(hadithId: String): Boolean
}

@Entity(tableName = "daily_prayer_logs")
data class DailyPrayerLogEntity(
    @PrimaryKey val dateKey: String, // yyyy-MM-dd
    val fajrDone: Boolean = false,
    val dhuhrDone: Boolean = false,
    val asrDone: Boolean = false,
    val maghribDone: Boolean = false,
    val ishaDone: Boolean = false,
    val rawatibDone: Boolean = false,
    val duhaDone: Boolean = false,
    val witrDone: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
) {
    val completedFardCount: Int
        get() = listOf(fajrDone, dhuhrDone, asrDone, maghribDone, ishaDone).count { it }

    val completedSunnahCount: Int
        get() = listOf(rawatibDone, duhaDone, witrDone).count { it }
}

@Dao
interface PrayerLogDao {
    @Query("SELECT * FROM daily_prayer_logs WHERE dateKey = :dateKey LIMIT 1")
    fun observeLogForDate(dateKey: String): Flow<DailyPrayerLogEntity?>

    @Query("SELECT * FROM daily_prayer_logs WHERE dateKey = :dateKey LIMIT 1")
    suspend fun getLogForDateOnce(dateKey: String): DailyPrayerLogEntity?

    @Query("SELECT * FROM daily_prayer_logs ORDER BY dateKey DESC LIMIT :limit")
    fun observeRecentLogs(limit: Int = 14): Flow<List<DailyPrayerLogEntity>>

    @Query("SELECT * FROM daily_prayer_logs ORDER BY dateKey DESC")
    suspend fun getAllLogsOnce(): List<DailyPrayerLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLog(log: DailyPrayerLogEntity)
}
