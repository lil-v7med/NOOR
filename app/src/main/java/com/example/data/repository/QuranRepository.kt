package com.example.data.repository

import android.content.Context
import com.example.data.local.BookmarkDao
import com.example.data.local.BookmarkEntity
import com.example.data.local.CachedAyahDao
import com.example.data.local.CachedAyahEntity
import com.example.data.model.AyahItem
import com.example.data.model.JuzInfo
import com.example.data.model.SurahContent
import com.example.data.model.SurahInfo
import com.example.util.ArabicTextUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class QuranSearchMatch(
    val surah: SurahInfo,
    val ayahNumber: Int? = null,
    val snippet: String? = null
)

data class FullQuranDownloadState(
    val isDownloading: Boolean = false,
    val currentSurahName: String = "",
    val downloadedSurahsCount: Int = 0,
    val totalSurahs: Int = 114,
    val errorMessage: String? = null
)

class QuranRepository(
    private val context: Context,
    private val bookmarkDao: BookmarkDao,
    private val cachedAyahDao: CachedAyahDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var fullDownloadJob: Job? = null

    private val surahListCache: List<SurahInfo> by lazy { loadSurahIndex() }
    private val juzListCache: List<JuzInfo> by lazy { loadJuzIndex() }
    private val bundledSurahs: Map<Int, Pair<Boolean, List<AyahItem>>> by lazy { loadBundledQuran() }

    private val _fullDownloadState = MutableStateFlow(FullQuranDownloadState())
    val fullDownloadState: StateFlow<FullQuranDownloadState> = _fullDownloadState.asStateFlow()

    val bookmarksFlow: Flow<List<BookmarkEntity>> = bookmarkDao.getAllBookmarks()

    fun observeOfflineSurahIds(): Flow<Set<Int>> {
        val completeBundledIds = bundledSurahs.filter { it.value.first }.keys
        return cachedAyahDao.observeCachedSurahCounts().map { counts ->
            val cachedComplete = counts.filter { count ->
                val expected = surahListCache.firstOrNull { it.id == count.surahId }?.ayahCount ?: Int.MAX_VALUE
                count.ayahCount >= expected
            }.map { it.surahId }.toSet()
            completeBundledIds + cachedComplete
        }
    }

    fun getAllSurahs(): List<SurahInfo> = surahListCache

    fun getAllJuz(): List<JuzInfo> = juzListCache

    fun getSurahById(surahId: Int): SurahInfo {
        return surahListCache.firstOrNull { it.id == surahId } ?: surahListCache.first()
    }

    fun getDailyVerse(): Pair<SurahInfo, AyahItem> {
        val candidates = listOf(
            2 to 255,
            94 to 5,
            93 to 5,
            18 to 10,
            103 to 3,
            59 to 22,
            1 to 5
        )
        val dayIndex = ((System.currentTimeMillis() / 86_400_000L) % candidates.size).toInt()
        val (sId, aNum) = candidates[dayIndex]
        val surah = getSurahById(sId)
        val ayah = bundledSurahs[sId]?.second?.firstOrNull { it.number == aNum }
            ?: AyahItem(1, 5, "إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ", "إنا نخصك وحدك بالعبادة، ونستعين بك وحدك في جميع أمورنا.")
        return surah to ayah
    }

    suspend fun searchAsync(query: String): List<QuranSearchMatch> = withContext(Dispatchers.IO) {
        val normQuery = ArabicTextUtils.normalizeForSearch(query)
        if (normQuery.isBlank()) {
            return@withContext surahListCache.map { QuranSearchMatch(it) }
        }

        val matches = mutableListOf<QuranSearchMatch>()
        for (surah in surahListCache) {
            val normName = ArabicTextUtils.normalizeForSearch(surah.name)
            val normEn = surah.englishName.lowercase()
            if (normName.contains(normQuery) || normEn.contains(normQuery) || surah.id.toString() == normQuery) {
                matches.add(QuranSearchMatch(surah))
            }
        }

        if (normQuery.length >= 2) {
            val seenAyahKeys = HashSet<String>()
            // Search bundled verses
            for ((surahId, pair) in bundledSurahs) {
                val surah = getSurahById(surahId)
                for (ayah in pair.second) {
                    if (ArabicTextUtils.normalizeForSearch(ayah.text).contains(normQuery)) {
                        val key = "$surahId:${ayah.number}"
                        if (seenAyahKeys.add(key)) {
                            matches.add(QuranSearchMatch(surah, ayah.number, ayah.text))
                        }
                    }
                }
            }
            // Search Room cached verses from downloaded Surahs
            val cachedAyahs = cachedAyahDao.getAllCachedAyahs()
            for (cached in cachedAyahs) {
                if (matches.size >= 150) break
                val key = "${cached.surahId}:${cached.ayahNumber}"
                if (seenAyahKeys.contains(key)) continue
                if (ArabicTextUtils.normalizeForSearch(cached.text).contains(normQuery)) {
                    seenAyahKeys.add(key)
                    matches.add(QuranSearchMatch(getSurahById(cached.surahId), cached.ayahNumber, cached.text))
                }
            }
        }

        matches
    }

    // Synchronous search for unit tests & instant lookup
    fun search(query: String): List<QuranSearchMatch> {
        val normQuery = ArabicTextUtils.normalizeForSearch(query)
        if (normQuery.isBlank()) {
            return surahListCache.map { QuranSearchMatch(it) }
        }

        val matches = mutableListOf<QuranSearchMatch>()
        for (surah in surahListCache) {
            val normName = ArabicTextUtils.normalizeForSearch(surah.name)
            val normEn = surah.englishName.lowercase()
            if (normName.contains(normQuery) || normEn.contains(normQuery) || surah.id.toString() == normQuery) {
                matches.add(QuranSearchMatch(surah))
            }
        }

        if (normQuery.length >= 2) {
            for ((surahId, pair) in bundledSurahs) {
                val surah = getSurahById(surahId)
                for (ayah in pair.second) {
                    val normAyah = ArabicTextUtils.normalizeForSearch(ayah.text)
                    if (normAyah.contains(normQuery)) {
                        matches.add(
                            QuranSearchMatch(
                                surah = surah,
                                ayahNumber = ayah.number,
                                snippet = ayah.text
                            )
                        )
                    }
                }
            }
        }

        return matches
    }

    fun startFullQuranOfflineDownload() {
        if (fullDownloadJob?.isActive == true) return
        fullDownloadJob = scope.launch {
            try {
                val completeBundledIds = bundledSurahs.filter { it.value.first }.keys
                val cachedCounts = cachedAyahDao.getCachedSurahCounts().associate { it.surahId to it.ayahCount }
                val alreadyComplete = completeBundledIds + cachedCounts.filter { (id, count) ->
                    val expected = surahListCache.firstOrNull { it.id == id }?.ayahCount ?: Int.MAX_VALUE
                    count >= expected
                }.keys

                var readyCount = alreadyComplete.size
                _fullDownloadState.update {
                    FullQuranDownloadState(
                        isDownloading = true,
                        currentSurahName = "",
                        downloadedSurahsCount = readyCount,
                        totalSurahs = surahListCache.size,
                        errorMessage = null
                    )
                }

                for (surah in surahListCache) {
                    if (!isActive) return@launch
                    if (alreadyComplete.contains(surah.id)) continue

                    _fullDownloadState.update {
                        it.copy(currentSurahName = surah.name)
                    }
                    val fetched = fetchSurahFromCloud(surah.id)
                    if (fetched.isEmpty()) {
                        _fullDownloadState.update {
                            it.copy(
                                isDownloading = false,
                                errorMessage = "توقف التحميل عند سورة ${surah.name} — تأكد من الاتصال بالإنترنت"
                            )
                        }
                        return@launch
                    }
                    cachedAyahDao.insertAyahs(
                        fetched.map { CachedAyahEntity(it.surahId, it.number, it.text, it.tafsir) }
                    )
                    readyCount++
                    _fullDownloadState.update {
                        it.copy(downloadedSurahsCount = readyCount)
                    }
                }

                _fullDownloadState.update {
                    FullQuranDownloadState(
                        isDownloading = false,
                        currentSurahName = "",
                        downloadedSurahsCount = surahListCache.size,
                        totalSurahs = surahListCache.size,
                        errorMessage = null
                    )
                }
            } catch (_: Exception) {
                _fullDownloadState.update {
                    it.copy(
                        isDownloading = false,
                        errorMessage = "تعذر إكمال تنزيل المصحف — حاول مرة أخرى"
                    )
                }
            }
        }
    }

    fun cancelFullQuranDownload() {
        fullDownloadJob?.cancel()
        fullDownloadJob = null
        _fullDownloadState.update { it.copy(isDownloading = false, currentSurahName = "") }
    }

    suspend fun clearCachedQuran() = withContext(Dispatchers.IO) {
        cancelFullQuranDownload()
        cachedAyahDao.clearAllCachedAyahs()
    }

    suspend fun loadSurahContent(surahId: Int, attemptNetworkFetch: Boolean = true): SurahContent =
        withContext(Dispatchers.IO) {
            val info = getSurahById(surahId)

            val cached = cachedAyahDao.getAyahsForSurah(surahId)
            if (cached.size >= info.ayahCount) {
                return@withContext SurahContent(
                    surahInfo = info,
                    ayahs = cached.map { AyahItem(it.surahId, it.ayahNumber, it.text, it.tafsir) },
                    isComplete = true,
                    offlineNotice = false
                )
            }

            val bundled = bundledSurahs[surahId]
            if (bundled != null && bundled.first) {
                return@withContext SurahContent(
                    surahInfo = info,
                    ayahs = bundled.second,
                    isComplete = true,
                    offlineNotice = false
                )
            }

            if (attemptNetworkFetch) {
                val fetched = fetchSurahFromCloud(surahId)
                if (fetched.isNotEmpty()) {
                    cachedAyahDao.insertAyahs(
                        fetched.map { CachedAyahEntity(it.surahId, it.number, it.text, it.tafsir) }
                    )
                    return@withContext SurahContent(
                        surahInfo = info,
                        ayahs = fetched,
                        isComplete = true,
                        offlineNotice = false
                    )
                }
            }

            SurahContent(
                surahInfo = info,
                ayahs = bundled?.second ?: emptyList(),
                isComplete = false,
                offlineNotice = true
            )
        }

    suspend fun addBookmark(surah: SurahInfo, ayah: AyahItem) {
        val key = "${surah.id}:${ayah.number}"
        bookmarkDao.insertBookmark(
            BookmarkEntity(
                key = key,
                surahId = surah.id,
                surahName = surah.name,
                ayahNumber = ayah.number,
                ayahText = ayah.text
            )
        )
    }

    suspend fun removeBookmark(surahId: Int, ayahNumber: Int) {
        bookmarkDao.deleteBookmark("$surahId:$ayahNumber")
    }

    private fun fetchSurahFromCloud(surahId: Int): List<AyahItem> {
        return try {
            val url = URL("https://api.alquran.cloud/v1/surah/$surahId/editions/quran-uthmani,ar.muyassar")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 8000
            }
            if (conn.responseCode != HttpURLConnection.HTTP_OK) return emptyList()
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()

            val root = JSONObject(body)
            val dataArray = root.optJSONArray("data") ?: return emptyList()
            if (dataArray.length() < 2) return emptyList()

            val uthmaniAyahs = dataArray.getJSONObject(0).getJSONArray("ayahs")
            val tafsirAyahs = dataArray.getJSONObject(1).getJSONArray("ayahs")
            val result = ArrayList<AyahItem>(uthmaniAyahs.length())

            for (i in 0 until uthmaniAyahs.length()) {
                val uObj = uthmaniAyahs.getJSONObject(i)
                val tObj = tafsirAyahs.optJSONObject(i)
                val num = uObj.getInt("numberInSurah")
                var text = uObj.getString("text")
                if (surahId != 1 && surahId != 9 && num == 1) {
                    val basmalah = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ "
                    val basmalah2 = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ "
                    text = text.removePrefix(basmalah).removePrefix(basmalah2).trim()
                }
                val tafsir = tObj?.optString("text", "") ?: ""
                result.add(AyahItem(surahId = surahId, number = num, text = text, tafsir = tafsir))
            }
            result
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun loadSurahIndex(): List<SurahInfo> {
        val json = context.assets.open("surah_index.json").bufferedReader().use { it.readText() }
        val arr = JSONArray(json)
        return List(arr.length()) { idx ->
            val obj = arr.getJSONObject(idx)
            SurahInfo(
                id = obj.getInt("id"),
                name = obj.getString("name"),
                englishName = obj.getString("englishName"),
                revelationType = obj.getString("revelationType"),
                ayahCount = obj.getInt("ayahCount"),
                juz = obj.getInt("juz"),
                page = obj.getInt("page")
            )
        }
    }

    private fun loadJuzIndex(): List<JuzInfo> {
        val json = context.assets.open("juz_index.json").bufferedReader().use { it.readText() }
        val arr = JSONArray(json)
        return List(arr.length()) { idx ->
            val obj = arr.getJSONObject(idx)
            JuzInfo(
                id = obj.getInt("id"),
                name = obj.getString("name"),
                surahId = obj.getInt("surahId"),
                surahName = obj.getString("surahName"),
                startAyah = obj.getInt("startAyah")
            )
        }
    }

    private fun loadBundledQuran(): Map<Int, Pair<Boolean, List<AyahItem>>> {
        val json = context.assets.open("quran_bundled.json").bufferedReader().use { it.readText() }
        val arr = JSONArray(json)
        val map = mutableMapOf<Int, Pair<Boolean, List<AyahItem>>>()
        for (i in 0 until arr.length()) {
            val sObj = arr.getJSONObject(i)
            val surahId = sObj.getInt("surahId")
            val isComplete = sObj.getBoolean("isComplete")
            val ayahsArr = sObj.getJSONArray("ayahs")
            val ayahs = List(ayahsArr.length()) { j ->
                val aObj = ayahsArr.getJSONObject(j)
                AyahItem(
                    surahId = surahId,
                    number = aObj.getInt("number"),
                    text = aObj.getString("text"),
                    tafsir = aObj.getString("tafsir")
                )
            }
            map[surahId] = isComplete to ayahs
        }
        return map
    }
}
