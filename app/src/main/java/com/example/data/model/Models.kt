package com.example.data.model

data class SurahInfo(
    val id: Int,
    val name: String,
    val englishName: String,
    val revelationType: String,
    val ayahCount: Int,
    val juz: Int,
    val page: Int
)

data class JuzInfo(
    val id: Int,
    val name: String,
    val surahId: Int,
    val surahName: String,
    val startAyah: Int
)

data class AyahItem(
    val surahId: Int,
    val number: Int,
    val text: String,
    val tafsir: String
)

data class SurahContent(
    val surahInfo: SurahInfo,
    val ayahs: List<AyahItem>,
    val isComplete: Boolean,
    val isLoading: Boolean = false,
    val offlineNotice: Boolean = false
)

data class ZikrItem(
    val id: String,
    val text: String,
    val count: Int,
    val fadl: String,
    val source: String = "",
    val sourceUrl: String = "",
    val isCustom: Boolean = false,
    val customDbId: Long = 0L
)

data class AzkarCategory(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: String,
    val items: List<ZikrItem>
)

data class HadithItem(
    val id: String,
    val collectionTag: String = "مختارات الصحيحين",
    val hadithNumber: Int? = null,
    val category: String,
    val title: String,
    val narrator: String,
    val text: String,
    val explanation: String,
    val source: String,
    val grade: String,
    val sourceUrl: String
)

data class AppSourceReference(
    val title: String,
    val publisher: String,
    val description: String,
    val url: String
)

object SourcesCatalog {
    val references = listOf(
        AppSourceReference(
            title = "النص القرآني بالرسم العثماني",
            publisher = "مجمع الملك فهد لطباعة المصحف الشريف / AlQuran Cloud",
            description = "نص القرآن الكريم بالرسم العثماني المعتمد مع إمكانية تنزيل الـ ١١٤ سورة للاستخدام بدون إنترنت.",
            url = "https://qurancomplex.gov.sa/"
        ),
        AppSourceReference(
            title = "التفسير الميسر",
            publisher = "مجمع الملك فهد لطباعة المصحف الشريف",
            description = "تفسير موجز وميسر لمعاني آيات القرآن الكريم معتمد رسمياً.",
            url = "https://quran.ksu.edu.sa/tafseer/moyassar/sura1-aya1.html"
        ),
        AppSourceReference(
            title = "التلاوات الصوتية (MP3)",
            publisher = "مشروع EveryAyah",
            description = "تسجيلات تلاوة القرآن الكريم آيةً بآية بصوت نخبة من كبار القراء مع إدارة التنزيلات بدون إنترنت.",
            url = "https://everyayah.com/"
        ),
        AppSourceReference(
            title = "أذكار حصن المسلم",
            publisher = "الشيخ سعيد بن علي بن وهف القحطاني رحمه الله / Sunnah.com",
            description = "الأذكار اليومية والأدعية المأثورة من الكتاب والسنة مع تخريج كل ذكر برقم الحديث.",
            url = "https://sunnah.com/hisn"
        ),
        AppSourceReference(
            title = "الأربعون النووية والأحاديث الصحيحة",
            publisher = "صحيح البخاري ومسلم والأربعون النووية (Sunnah.com)",
            description = "أحاديث نبوية صحيحة والأربعون النووية كاملة مخرّجة وموثقة بروابط مباشرة للتحقق.",
            url = "https://sunnah.com/nawawi40"
        ),
        AppSourceReference(
            title = "الحساب الفلكي لمواقيت الصلاة والتقويم الهجري",
            publisher = "الهيئة المصرية العامة للمساحة / أم القرى / PrayTimes",
            description = "معادلات الحساب الفلكي لزاوية الشمس وتقويم أم القرى الهجري لحساب المواقيت الخمسة حسب المدينة المختارة.",
            url = "https://praytimes.org/calculation"
        ),
        AppSourceReference(
            title = "حساب اتجاه القبلة والمجال المغناطيسي",
            publisher = "نموذج الحساب الجيوديسي الدائري ومستشعرات Android",
            description = "حساب السمت الدائري العظيم نحو إحداثيات الكعبة المشرفة (21.4225° N, 39.8262° E) مع تصحيح الانحراف المغناطيسي.",
            url = "https://www.ngdc.noaa.gov/geomag/WMM/"
        )
    )

    fun quranAyahReferenceUrl(surahId: Int, ayahNumber: Int): String {
        return "https://quran.com/$surahId/$ayahNumber"
    }

    fun muyassarTafsirUrl(surahId: Int, ayahNumber: Int): String {
        return "https://quran.ksu.edu.sa/tafseer/moyassar/sura$surahId-aya$ayahNumber.html"
    }

    fun nawawiHadithUrl(hadithNumber: Int): String {
        return "https://sunnah.com/nawawi40:$hadithNumber"
    }
}

data class ReciterInfo(
    val id: String,
    val nameArabic: String,
    val style: String,
    val everyAyahFolder: String
)

object RecitersCatalog {
    val reciters = listOf(
        ReciterInfo(
            id = "alafasy",
            nameArabic = "مشاري بن راشد العفاسي",
            style = "مرتل",
            everyAyahFolder = "Alafasy_128kbps"
        ),
        ReciterInfo(
            id = "abdul_basit",
            nameArabic = "عبد الباسط عبد الصمد",
            style = "مرتل",
            everyAyahFolder = "Abdul_Basit_Murattal_192kbps"
        ),
        ReciterInfo(
            id = "husary",
            nameArabic = "محمود خليل الحصري",
            style = "مرتل",
            everyAyahFolder = "Husary_128kbps"
        ),
        ReciterInfo(
            id = "minshawy",
            nameArabic = "محمد صديق المنشاوي",
            style = "مرتل",
            everyAyahFolder = "Minshawy_Murattal_128kbps"
        ),
        ReciterInfo(
            id = "maher",
            nameArabic = "ماهر المعيقلي",
            style = "مرتل",
            everyAyahFolder = "MaherAlMuaiqly128kbps"
        ),
        ReciterInfo(
            id = "yasser",
            nameArabic = "ياسر الدوسري",
            style = "مرتل",
            everyAyahFolder = "Yasser_Al-Dosari_128kbps"
        )
    )

    fun byId(id: String): ReciterInfo = reciters.firstOrNull { it.id == id } ?: reciters.first()

    fun buildAyahUrl(reciter: ReciterInfo, surahId: Int, ayahNumber: Int): String {
        val s = surahId.toString().padStart(3, '0')
        val a = ayahNumber.toString().padStart(3, '0')
        return "https://everyayah.com/data/${reciter.everyAyahFolder}/$s$a.mp3"
    }
}

data class QuizQuestionItem(
    val id: String,
    val category: String,
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

data class GoodDeedItem(
    val id: String,
    val title: String,
    val description: String,
    val category: String
)

data class ShaarawyLectureItem(
    val id: String,
    val title: String,
    val category: String,
    val surahName: String,
    val duration: String,
    val speaker: String,
    val source: String,
    val audioUrl: String
)
