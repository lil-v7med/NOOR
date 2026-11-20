package com.example.data.repository

import android.content.Context
import com.example.data.local.FavoriteHadithDao
import com.example.data.local.FavoriteHadithEntity
import com.example.data.local.NawawiHadithDao
import com.example.data.local.NawawiHadithEntity
import com.example.data.model.HadithItem
import com.example.data.model.SourcesCatalog
import com.example.util.ArabicTextUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class HadithRepository(
    private val context: Context,
    private val nawawiHadithDao: NawawiHadithDao,
    private val favoriteHadithDao: FavoriteHadithDao
) {
    private val seedMutex = Mutex()
    private val nawawiCacheFile: File
        get() = File(context.filesDir, "nawawi40_cached.json")

    private val curatedHadiths: List<HadithItem> by lazy { loadCuratedCollection() }
    private val bundledNawawiSeed: List<NawawiHadithEntity> by lazy { loadNawawiSeedEntities() }

    suspend fun ensureNawawiSeeded() = withContext(Dispatchers.IO) {
        if (nawawiHadithDao.getCount() >= 42) return@withContext
        seedMutex.withLock {
            if (nawawiHadithDao.getCount() >= 42) return@withLock
            val cachedTexts = loadLegacyCachedNawawiTexts()
            val legacyFavIds = favoriteHadithDao.observeFavoriteIds().first().toSet()
            val entities = bundledNawawiSeed.map { base ->
                val isLegacyFav = legacyFavIds.contains("nawawi_${base.number}")
                base.copy(
                    text = cachedTexts[base.number].orEmpty(),
                    isFavorite = isLegacyFav,
                    favoritedAt = if (isLegacyFav) System.currentTimeMillis() else null
                )
            }
            nawawiHadithDao.insertAllIgnore(entities)
        }
    }

    fun observeAllHadiths(): Flow<List<HadithItem>> {
        return nawawiHadithDao.observeAllHadiths().map { entities ->
            val nawawiItems = if (entities.isEmpty()) {
                bundledNawawiSeed.map { it.toHadithItem() }
            } else {
                entities.map { it.toHadithItem() }
            }
            nawawiItems + curatedHadiths
        }
    }

    fun observeFavoriteIds(): Flow<Set<String>> {
        return combine(
            nawawiHadithDao.observeFavoriteNumbers(),
            favoriteHadithDao.observeFavoriteIds()
        ) { nawawiNums, generalIds ->
            val nawawiIds = nawawiNums.map { "nawawi_$it" }.toSet()
            nawawiIds + generalIds.toSet()
        }
    }

    fun observeFavoriteNawawiHadiths(): Flow<List<HadithItem>> {
        return nawawiHadithDao.observeFavoriteHadiths().map { list ->
            list.map { it.toHadithItem() }
        }
    }

    fun observeIsNawawiSynced(): Flow<Boolean> {
        return nawawiHadithDao.observeSyncedCount().map { it >= 42 }
    }

    suspend fun hasSyncedFullNawawi(): Boolean = withContext(Dispatchers.IO) {
        nawawiHadithDao.getSyncedCountOnce() >= 42
    }

    fun getAllHadiths(): List<HadithItem> {
        val legacyCache = loadLegacyCachedNawawiTexts()
        val nawawi = bundledNawawiSeed.map { it.copy(text = legacyCache[it.number].orEmpty()).toHadithItem() }
        return nawawi + curatedHadiths
    }

    fun getCategories(): List<String> {
        val base = listOf("الكل", "الأربعون النووية", "المفضلة", "الأخلاق والآداب", "العبادات", "العقيدة", "الأذكار والأدعية", "الأسرة والمجتمع")
        val curatedCats = curatedHadiths.map { it.category }.distinct()
        return (base + curatedCats).distinct()
    }

    suspend fun toggleFavorite(hadithId: String) = withContext(Dispatchers.IO) {
        ensureNawawiSeeded()
        if (hadithId.startsWith("nawawi_")) {
            val num = hadithId.removePrefix("nawawi_").toIntOrNull()
            if (num != null) {
                val currentlyFav = nawawiHadithDao.isFavorite(num) ?: false
                val nextFav = !currentlyFav
                nawawiHadithDao.setFavorite(
                    number = num,
                    isFavorite = nextFav,
                    favoritedAt = if (nextFav) System.currentTimeMillis() else null
                )
                if (nextFav) {
                    favoriteHadithDao.addFavorite(FavoriteHadithEntity(hadithId = hadithId))
                } else {
                    favoriteHadithDao.removeFavorite(hadithId)
                }
                return@withContext
            }
        }
        if (favoriteHadithDao.isFavorite(hadithId)) {
            favoriteHadithDao.removeFavorite(hadithId)
        } else {
            favoriteHadithDao.addFavorite(FavoriteHadithEntity(hadithId = hadithId))
        }
    }

    fun filterHadiths(
        query: String,
        category: String,
        favoriteIds: Set<String>,
        sourceItems: List<HadithItem> = getAllHadiths()
    ): List<HadithItem> {
        val normQuery = ArabicTextUtils.normalizeForSearch(query)
        return sourceItems.filter { item ->
            val matchesCat = when (category) {
                "الكل" -> true
                "الأربعون النووية" -> item.collectionTag == "الأربعون النووية"
                "المفضلة" -> favoriteIds.contains(item.id)
                else -> item.category == category
            }
            if (!matchesCat) return@filter false
            if (normQuery.isBlank()) return@filter true

            ArabicTextUtils.normalizeForSearch(item.title).contains(normQuery) ||
                ArabicTextUtils.normalizeForSearch(item.text).contains(normQuery) ||
                ArabicTextUtils.normalizeForSearch(item.explanation).contains(normQuery) ||
                ArabicTextUtils.normalizeForSearch(item.narrator).contains(normQuery) ||
                ArabicTextUtils.normalizeForSearch(item.source).contains(normQuery)
        }
    }

    suspend fun syncNawawiFullTexts(): Boolean = withContext(Dispatchers.IO) {
        ensureNawawiSeeded()
        try {
            val url = URL("https://cdn.jsdelivr.net/gh/fawazahmed0/hadith-api@1/editions/ara-nawawi.min.json")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 12000
            }
            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                conn.disconnect()
                return@withContext false
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()

            val root = JSONObject(body)
            val arr = root.optJSONArray("hadiths") ?: return@withContext false
            var updatedCount = 0

            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                val num = item.optInt("hadithnumber", 0)
                val text = item.optString("text", "").trim()
                if (num in 1..42 && text.isNotBlank()) {
                    nawawiHadithDao.updateFullText(num, text)
                    updatedCount++
                }
            }

            updatedCount > 0
        } catch (_: Exception) {
            false
        }
    }

    private fun loadLegacyCachedNawawiTexts(): Map<Int, String> {
        return try {
            if (!nawawiCacheFile.exists()) return emptyMap()
            val obj = JSONObject(nawawiCacheFile.readText())
            val map = mutableMapOf<Int, String>()
            obj.keys().forEach { key ->
                val num = key.toIntOrNull()
                val text = obj.optString(key, "")
                if (num != null && text.isNotBlank()) {
                    map[num] = text
                }
            }
            map
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun loadNawawiSeedEntities(): List<NawawiHadithEntity> {
        val json = context.assets.open("nawawi40_index.json").bufferedReader().use { it.readText() }
        val arr = JSONArray(json)
        return List(arr.length()) { i ->
            val obj = arr.getJSONObject(i)
            val num = obj.getInt("number")
            NawawiHadithEntity(
                number = num,
                title = obj.getString("title"),
                narrator = "عن ${obj.getString("narrator")}",
                text = "",
                summary = obj.getString("summary"),
                source = obj.getString("source"),
                grade = obj.getString("grade"),
                sourceUrl = SourcesCatalog.nawawiHadithUrl(num),
                isFavorite = false,
                favoritedAt = null
            )
        }
    }

    private fun NawawiHadithEntity.toHadithItem(): HadithItem {
        return HadithItem(
            id = "nawawi_$number",
            collectionTag = "الأربعون النووية",
            hadithNumber = number,
            category = "الأربعون النووية",
            title = title,
            narrator = narrator,
            text = text,
            explanation = summary,
            source = source,
            grade = grade,
            sourceUrl = sourceUrl
        )
    }

    private fun loadCuratedCollection(): List<HadithItem> {
        val json = context.assets.open("hadith_collection.json").bufferedReader().use { it.readText() }
        val arr = JSONArray(json)
        return List(arr.length()) { i ->
            val obj = arr.getJSONObject(i)
            HadithItem(
                id = obj.getString("id"),
                collectionTag = "مختارات الصحيحين",
                hadithNumber = null,
                category = obj.getString("category"),
                title = obj.getString("title"),
                narrator = obj.getString("narrator"),
                text = obj.getString("text"),
                explanation = obj.getString("explanation"),
                source = obj.getString("source"),
                grade = obj.getString("grade"),
                sourceUrl = obj.getString("sourceUrl")
            )
        }
    }
}
