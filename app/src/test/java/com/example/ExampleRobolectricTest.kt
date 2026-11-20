package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.audio.QuranAudioController
import com.example.audio.QuranAudioDownloadManager
import com.example.data.backup.BackupManager
import com.example.data.local.AppDatabase
import com.example.data.local.BookmarkEntity
import com.example.data.local.CustomZikrEntity
import com.example.data.local.DailyPrayerLogEntity
import com.example.data.local.UserPreferencesRepository
import com.example.data.model.MushafPageCatalog
import com.example.data.model.SourcesCatalog
import com.example.data.model.SurahInfo
import com.example.data.repository.AzkarRepository
import com.example.data.repository.HadithRepository
import com.example.data.repository.IslamicToolkitRepository
import com.example.data.repository.QuranRepository
import com.example.khatma.KhatmaPlanner
import com.example.prayer.HijriCalendarHelper
import com.example.prayer.PrayerTimesCalculator
import com.example.prayer.PrayerType
import com.example.qibla.QiblaCalculator
import com.example.util.ArabicTextUtils
import com.example.zakat.ZakatCalculator
import com.example.zakat.ZakatInput
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `app name matches Mihrab Al Noor`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("محراب النور", appName)
    }

    @Test
    fun `arabic normalization strips tashkeel and normalizes hamza`() {
        val raw = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ"
        val normalized = ArabicTextUtils.normalizeForSearch(raw)
        assertTrue(normalized.contains("بسم الله"))
        assertEquals("١٢٣", ArabicTextUtils.toEasternArabicNumerals(123))
    }

    @Test
    fun `offline quran and hisni datasets load cleanly`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getInstance(context)
        val quranRepo = QuranRepository(context, db.bookmarkDao(), db.cachedAyahDao())
        val azkarRepo = AzkarRepository(context, db.customZikrDao())

        assertEquals(114, quranRepo.getAllSurahs().size)
        assertEquals(30, quranRepo.getAllJuz().size)

        val fatihah = quranRepo.loadSurahContent(1, attemptNetworkFetch = false)
        assertEquals(7, fatihah.ayahs.size)
        assertTrue(fatihah.isComplete)

        val searchMatches = quranRepo.search("الفاتحه")
        assertTrue(searchMatches.any { it.surah.id == 1 })

        val categories = azkarRepo.getBuiltInCategories()
        assertEquals(8, categories.size)
        assertTrue(categories.any { it.id == "morning" && it.items.isNotEmpty() })
        assertTrue(
            "Every Zikr must include source and https sourceUrl",
            categories.flatMap { it.items }.all { it.source.isNotBlank() && it.sourceUrl.startsWith("https://") }
        )
    }

    @Test
    fun `hadith dataset seeds nawawi 42 into Room and supports favorites`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getInstance(context)
        val nawawiDao = db.nawawiHadithDao()
        val hadithRepo = HadithRepository(context, nawawiDao, db.favoriteHadithDao())

        hadithRepo.ensureNawawiSeeded()
        assertEquals(42, nawawiDao.getCount())

        val seededRows = nawawiDao.getAllHadithsOnce()
        assertEquals(42, seededRows.size)
        assertEquals(1, seededRows.first().number)

        val allHadiths = hadithRepo.observeAllHadiths().first()
        assertEquals(60, allHadiths.size)
        assertTrue(hadithRepo.getCategories().contains("الأربعون النووية"))
        assertTrue(hadithRepo.getCategories().contains("المفضلة"))
        assertTrue(
            "Every Hadith must include source, grade, and https sourceUrl",
            allHadiths.all { it.source.isNotBlank() && it.grade.isNotBlank() && it.sourceUrl.startsWith("https://") }
        )

        // Update text in Room and verify persistence
        nawawiDao.updateFullText(1, "إنما الأعمال بالنيات")
        val updatedRow = nawawiDao.getAllHadithsOnce().first { it.number == 1 }
        assertEquals("إنما الأعمال بالنيات", updatedRow.text)

        val searchResults = hadithRepo.filterHadiths("انما الاعمال", "الكل", emptySet(), allHadiths)
        assertTrue(searchResults.any { it.id == "h1" })

        val nawawiResults = hadithRepo.filterHadiths("", "الأربعون النووية", emptySet(), allHadiths)
        assertEquals(42, nawawiResults.size)

        // Room Favorite toggle test on nawawi_hadiths table
        nawawiDao.setFavorite(1, false, null)
        hadithRepo.toggleFavorite("nawawi_1")
        val favNawawiList = hadithRepo.observeFavoriteNawawiHadiths().first()
        assertTrue(favNawawiList.any { it.id == "nawawi_1" })
        val favsAfterAdd = hadithRepo.observeFavoriteIds().first()
        assertTrue(favsAfterAdd.contains("nawawi_1"))
        val favFiltered = hadithRepo.filterHadiths("", "المفضلة", favsAfterAdd, allHadiths)
        assertTrue(favFiltered.any { it.id == "nawawi_1" })

        hadithRepo.toggleFavorite("nawawi_1")
        val favsAfterRemove = hadithRepo.observeFavoriteIds().first()
        assertFalse(favsAfterRemove.contains("nawawi_1"))
        val favNawawiAfterRemove = hadithRepo.observeFavoriteNawawiHadiths().first()
        assertFalse(favNawawiAfterRemove.any { it.id == "nawawi_1" })
    }

    @Test
    fun `offline audio download manager tracks inventory size and deletes cleanly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = QuranAudioDownloadManager(context)
        manager.clearAllDownloadedAudio()

        val surah = SurahInfo(
            id = 114,
            name = "الناس",
            englishName = "An-Nas",
            revelationType = "مكية",
            ayahCount = 6,
            juz = 30,
            page = 604
        )
        val reciterId = "alafasy"
        assertFalse(manager.isSurahDownloaded(reciterId, surah.id, surah.ayahCount))

        val surahDir = File(context.filesDir, "recitations/$reciterId/114").apply { mkdirs() }
        for (a in 1..6) {
            val f = File(surahDir, "${a.toString().padStart(3, '0')}.mp3")
            f.writeBytes(ByteArray(256) { 1 })
        }

        assertTrue(manager.isSurahDownloaded(reciterId, surah.id, surah.ayahCount))
        assertTrue(manager.getLocalAyahFile(reciterId, surah.id, 1) != null)

        val inventory = manager.getDownloadedAudioInventory()
        assertEquals(1, inventory.size)
        assertEquals(6, inventory.first().fileCount)
        assertEquals(6L * 256L, manager.getTotalDownloadedBytes())

        manager.deleteSurahAudio(reciterId, surah.id)
        assertFalse(manager.isSurahDownloaded(reciterId, surah.id, surah.ayahCount))
        assertEquals(0L, manager.getTotalDownloadedBytes())
    }

    @Test
    fun `sources catalog generates valid verification links`() {
        assertTrue(SourcesCatalog.references.size >= 7)
        assertEquals("https://quran.com/2/255", SourcesCatalog.quranAyahReferenceUrl(2, 255))
        assertEquals(
            "https://quran.ksu.edu.sa/tafseer/moyassar/sura2-aya255.html",
            SourcesCatalog.muyassarTafsirUrl(2, 255)
        )
        assertEquals("https://sunnah.com/nawawi40:1", SourcesCatalog.nawawiHadithUrl(1))
    }

    @Test
    fun `prayer times calculator computes ordered daily schedule and hijri date`() {
        val cairo = PrayerTimesCalculator.cityById("cairo")
        val egyptMethod = PrayerTimesCalculator.methodById("egypt")
        val cal = Calendar.getInstance().apply {
            set(2025, Calendar.MARCH, 15, 10, 0, 0)
        }
        val schedule = PrayerTimesCalculator.computeSchedule(cairo, egyptMethod, cal)
        assertEquals(6, schedule.slots.size)
        assertTrue(schedule.hijriFormatted.contains("هـ"))

        val minutes = schedule.slots.map { it.totalMinutes }
        for (i in 0 until minutes.size - 1) {
            assertTrue("Prayer slots must be chronologically ordered: $minutes", minutes[i] < minutes[i + 1])
        }
        assertEquals(PrayerType.DHUHR, schedule.nextPrayer)
    }

    @Test
    fun `khatma planner computes daily target and juz mapping accurately`() {
        assertEquals(21, KhatmaPlanner.dailyPagesForDuration(30))
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getInstance(context)
        val quranRepo = QuranRepository(context, db.bookmarkDao(), db.cachedAyahDao())
        val surahs = quranRepo.getAllSurahs()

        val startSummary = KhatmaPlanner.computeSummary(
            durationDays = 30,
            currentPage = 1,
            todayPagesRead = 0,
            lastLoggedDate = "",
            todayDateKey = "2025-03-15",
            streakDays = 0,
            surahs = surahs
        )
        assertEquals(1, startSummary.currentJuz)
        assertEquals(1, startSummary.targetSurahId)
        assertFalse(startSummary.isTodayComplete)

        val endSummary = KhatmaPlanner.computeSummary(
            durationDays = 30,
            currentPage = 604,
            todayPagesRead = 21,
            lastLoggedDate = "2025-03-15",
            todayDateKey = "2025-03-15",
            streakDays = 5,
            surahs = surahs
        )
        assertEquals(30, endSummary.currentJuz)
        assertTrue(endSummary.isTodayComplete)
        assertEquals(5, endSummary.streakDays)
    }

    @Test
    fun `qibla calculator computes accurate bearing and distance`() {
        val cairoBearing = QiblaCalculator.calculateQiblaBearing(30.0444, 31.2357)
        val cairoDist = QiblaCalculator.calculateDistanceKm(30.0444, 31.2357)
        assertTrue("Expected Cairo bearing ~136, got $cairoBearing", cairoBearing in 132f..140f)
        assertTrue("Expected Cairo distance ~1285 km, got $cairoDist", cairoDist in 1240.0..1330.0)

        val riyadhBearing = QiblaCalculator.calculateQiblaBearing(24.7136, 46.6753)
        assertTrue("Expected Riyadh bearing ~244, got $riyadhBearing", riyadhBearing in 240f..249f)

        val wrapDiff = QiblaCalculator.shortestAngleDiff(358f, 2f)
        assertEquals(4f, wrapDiff, 0.01f)
    }

    @Test
    fun `zakat calculator verifies nisab thresholds and computes 2_5 percent accurately`() {
        val belowNisab = ZakatCalculator.calculate(
            ZakatInput(
                cashAndSavings = 50_000.0,
                gold24kPricePerGram = 3500.0,
                nisabBasisIsGold = true
            )
        )
        assertFalse(belowNisab.isZakatDue)
        assertEquals(0.0, belowNisab.zakatAmountDue, 0.01)

        // 100g 24k gold (= 350,000) + 100,000 cash - 50,000 debt = 400,000 net -> 2.5% = 10,000
        val dueResult = ZakatCalculator.calculate(
            ZakatInput(
                cashAndSavings = 100_000.0,
                gold24kGrams = 100.0,
                immediateDebts = 50_000.0,
                gold24kPricePerGram = 3500.0,
                nisabBasisIsGold = true
            )
        )
        assertTrue(dueResult.isZakatDue)
        assertEquals(400_000.0, dueResult.netZakatableWealth, 0.01)
        assertEquals(10_000.0, dueResult.zakatAmountDue, 0.01)
    }

    @Test
    fun `mushaf page catalog tracks 15 sajdah verses hizb quarters and page splits`() = runTest {
        assertEquals(15, MushafPageCatalog.sajdahVerses.size)
        assertTrue(MushafPageCatalog.isSajdahVerse(96, 19))
        assertFalse(MushafPageCatalog.isSajdahVerse(1, 1))
        assertEquals(60, MushafPageCatalog.hizbForPage(604))
        assertEquals(1, MushafPageCatalog.juzForPage(1))

        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getInstance(context)
        val quranRepo = QuranRepository(context, db.bookmarkDao(), db.cachedAyahDao())
        val surahs = quranRepo.getAllSurahs()
        val fatihah = quranRepo.loadSurahContent(1, attemptNetworkFetch = false)
        val pages = MushafPageCatalog.computePageGroupsForSurah(fatihah.surahInfo, fatihah.ayahs, surahs)
        assertTrue(pages.isNotEmpty())
        assertEquals(1, pages.first().pageNumber)
    }

    @Test
    fun `islamic toolkit repository loads 99 names and verified duas`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = IslamicToolkitRepository(context)

        val names = repo.getAllAsmaulHusna()
        assertTrue(names.size >= 99)
        assertTrue(repo.searchAsmaulHusna("الرحمن").any { it.number == 2 })

        val duas = repo.getAllDuas()
        assertTrue(duas.size >= 14)
        assertTrue(
            "Every Dua must include a verified source and https sourceUrl",
            duas.all { it.source.isNotBlank() && it.sourceUrl.startsWith("https://") }
        )
    }

    @Test
    fun `backup manager exports and restores bookmarks azkar favorites and settings`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getInstance(context)
        val prefs = UserPreferencesRepository(context)
        val hadithRepo = HadithRepository(context, db.nawawiHadithDao(), db.favoriteHadithDao())
        hadithRepo.ensureNawawiSeeded()

        db.bookmarkDao().insertBookmark(
            BookmarkEntity(
                key = "2:255",
                surahId = 2,
                surahName = "البقرة",
                ayahNumber = 255,
                ayahText = "اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ",
                createdAt = 1000L
            )
        )
        db.customZikrDao().insertCustomZikr(
            CustomZikrEntity(
                text = "سبحان الله وبحمده",
                repeatCount = 100,
                fadl = "غفرت خطاياه",
                createdAt = 1000L
            )
        )
        db.nawawiHadithDao().setFavorite(5, false, null)
        hadithRepo.toggleFavorite("nawawi_5")

        val backupManager = BackupManager(db, prefs)
        val json = backupManager.buildBackupJson()
        assertTrue(json.contains("2:255"))
        assertTrue(json.contains("nawawi_5"))

        // Clear tables then restore from JSON
        db.bookmarkDao().deleteBookmark("2:255")
        db.nawawiHadithDao().setFavorite(5, false, null)
        val summary = backupManager.restoreFromJson(json)
        assertTrue(summary.bookmarksRestored >= 1)
        assertTrue(summary.favoriteHadithsRestored >= 1)
        assertTrue(db.bookmarkDao().getAllBookmarksOnce().any { it.key == "2:255" })
        db.nawawiHadithDao().setFavorite(5, false, null)
    }

    @Test
    fun `audio controller transitions active playback and stop cleanly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val downloadManager = QuranAudioDownloadManager(context)
        val controller = QuranAudioController(context, downloadManager)

        assertFalse(controller.state.value.isActive)
        controller.configureRangeRepeat(enabled = true, startAyah = 2, endAyah = 5, repeatTarget = 3)
        assertTrue(controller.state.value.isRangeRepeatEnabled)
        assertEquals(2, controller.state.value.rangeStartAyah)
        assertEquals(5, controller.state.value.rangeEndAyah)

        controller.setPlaybackSpeed(0.85f)
        controller.setAutoAdvanceNextSurah(false)
        controller.setSleepTimer(15)
        assertEquals(0.85f, controller.state.value.playbackSpeed, 0.01f)
        assertFalse(controller.state.value.autoAdvanceNextSurah)
        assertEquals(900, controller.state.value.sleepTimerRemainingSec)

        controller.stop()
        assertFalse(controller.state.value.isActive)
        assertFalse(controller.state.value.isPlaying)
        assertEquals(0, controller.state.value.sleepTimerRemainingSec)
    }

    @Test
    fun `hijri calendar helper builds full month imsakiyah and two-way conversion`() {
        val cairo = PrayerTimesCalculator.cityById("cairo")
        val method = PrayerTimesCalculator.methodById("egypt")
        val summary = HijriCalendarHelper.getMonthSchedule(0, cairo, method)

        assertTrue(summary.days.size in 29..30)
        assertTrue(summary.days.all { it.schedule.slots.size == 6 })
        assertTrue(summary.days.any { it.isToday })
        assertTrue(summary.days.count { it.isWhiteDay } == 3)

        assertEquals("غرة شهر رمضان المبارك", HijriCalendarHelper.occasionFor(9, 1))
        assertEquals("يوم عرفة", HijriCalendarHelper.occasionFor(12, 9))

        val hijri = HijriCalendarHelper.convertGregorianToHijri(2025, 3, 1)
        assertTrue(hijri.contains("رمضان"))
        val greg = HijriCalendarHelper.convertHijriToGregorian(1446, 9, 1)
        assertTrue(greg.contains("مارس"))
    }

    @Test
    fun `prayer tracker dao persists fard and sunnah counts and backup restores them`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getInstance(context)
        val prefs = UserPreferencesRepository(context)
        val dao = db.prayerLogDao()

        val log = DailyPrayerLogEntity(
            dateKey = "2026-11-19",
            fajrDone = true,
            dhuhrDone = true,
            asrDone = true,
            maghribDone = false,
            ishaDone = true,
            rawatibDone = true,
            duhaDone = false,
            witrDone = true
        )
        dao.upsertLog(log)
        val loaded = dao.getLogForDateOnce("2026-11-19")
        assertEquals(4, loaded?.completedFardCount)
        assertEquals(2, loaded?.completedSunnahCount)

        val backupManager = BackupManager(db, prefs)
        val json = backupManager.buildBackupJson()
        assertTrue(json.contains("2026-11-19"))

        dao.upsertLog(DailyPrayerLogEntity(dateKey = "2026-11-19"))
        val summary = backupManager.restoreFromJson(json)
        assertTrue(summary.prayerLogsRestored >= 1)
        val restored = dao.getLogForDateOnce("2026-11-19")
        assertEquals(4, restored?.completedFardCount)
        assertEquals(2, restored?.completedSunnahCount)
    }
}
