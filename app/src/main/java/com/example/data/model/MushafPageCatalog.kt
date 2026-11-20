package com.example.data.model

import com.example.util.ArabicTextUtils
import kotlin.math.ceil
import kotlin.math.floor

data class MushafPageGroup(
    val pageNumber: Int,
    val juzNumber: Int,
    val hizbNumber: Int,
    val quarterLabel: String,
    val ayahs: List<AyahItem>,
    val containsSajdah: Boolean
)

object MushafPageCatalog {
    const val TOTAL_PAGES = 604

    // All 15 Sajdah Al-Tilawah positions (Surah ID to Ayah Number)
    val sajdahVerses: Set<Pair<Int, Int>> = setOf(
        7 to 206,
        13 to 15,
        16 to 50,
        17 to 109,
        19 to 58,
        22 to 18,
        22 to 77,
        25 to 60,
        27 to 26,
        32 to 15,
        38 to 24,
        41 to 38,
        53 to 62,
        84 to 21,
        96 to 19
    )

    fun isSajdahVerse(surahId: Int, ayahNumber: Int): Boolean {
        return sajdahVerses.contains(surahId to ayahNumber)
    }

    fun juzForPage(pageNumber: Int): Int {
        val safePage = pageNumber.coerceIn(1, TOTAL_PAGES)
        return (((safePage - 1) / 20) + 1).coerceIn(1, 30)
    }

    fun hizbForPage(pageNumber: Int): Int {
        val safePage = pageNumber.coerceIn(1, TOTAL_PAGES)
        return (((safePage - 1) / 10) + 1).coerceIn(1, 60)
    }

    fun quarterLabelForPage(pageNumber: Int): String {
        val safePage = pageNumber.coerceIn(1, TOTAL_PAGES)
        val posInHizb = (safePage - 1) % 10
        return when {
            posInHizb < 2 -> "بداية الحزب"
            posInHizb < 5 -> "ربع الحزب"
            posInHizb < 7 -> "نصف الحزب"
            else -> "ثلاثة أرباع الحزب"
        }
    }

    fun hizbHeaderText(pageNumber: Int): String {
        val juz = ArabicTextUtils.toEasternArabicNumerals(juzForPage(pageNumber))
        val hizb = ArabicTextUtils.toEasternArabicNumerals(hizbForPage(pageNumber))
        val quarter = quarterLabelForPage(pageNumber)
        return "الجزء $juz • الحزب $hizb ($quarter)"
    }

    fun computePageGroupsForSurah(
        surah: SurahInfo,
        ayahs: List<AyahItem>,
        allSurahs: List<SurahInfo>
    ): List<MushafPageGroup> {
        if (ayahs.isEmpty()) return emptyList()
        val startPage = surah.page.coerceIn(1, TOTAL_PAGES)
        val nextSurah = allSurahs.firstOrNull { it.id == surah.id + 1 }
        val endPage = when {
            nextSurah != null -> (nextSurah.page).coerceAtLeast(startPage)
            else -> TOTAL_PAGES
        }
        val pageSpan = (endPage - startPage + 1).coerceIn(1, 49)

        if (pageSpan == 1 || ayahs.size <= 3) {
            return listOf(
                MushafPageGroup(
                    pageNumber = startPage,
                    juzNumber = juzForPage(startPage),
                    hizbNumber = hizbForPage(startPage),
                    quarterLabel = quarterLabelForPage(startPage),
                    ayahs = ayahs,
                    containsSajdah = ayahs.any { isSajdahVerse(surah.id, it.number) }
                )
            )
        }

        val chunkSize = ceil(ayahs.size.toDouble() / pageSpan.toDouble()).toInt().coerceAtLeast(1)
        val chunked = ayahs.chunked(chunkSize)
        return chunked.mapIndexed { idx, pageAyahs ->
            val pageNum = (startPage + idx).coerceAtMost(TOTAL_PAGES)
            MushafPageGroup(
                pageNumber = pageNum,
                juzNumber = juzForPage(pageNum),
                hizbNumber = hizbForPage(pageNum),
                quarterLabel = quarterLabelForPage(pageNum),
                ayahs = pageAyahs,
                containsSajdah = pageAyahs.any { isSajdahVerse(surah.id, it.number) }
            )
        }
    }

    fun findPageIndexForAyah(groups: List<MushafPageGroup>, targetAyah: Int): Int {
        if (groups.isEmpty()) return 0
        val idx = groups.indexOfFirst { g -> g.ayahs.any { it.number >= targetAyah } }
        return if (idx >= 0) idx else floor((groups.size - 1).toDouble()).toInt().coerceAtLeast(0)
    }
}
