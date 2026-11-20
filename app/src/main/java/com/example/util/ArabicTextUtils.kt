package com.example.util

object ArabicTextUtils {

    private val DIACRITICS_REGEX = Regex("[\\u0610-\\u061A\\u064B-\\u065F\\u0670\\u06D6-\\u06DC\\u06DF-\\u06E8\\u06EA-\\u06ED\\u0640]")

    fun stripTashkeel(input: String): String {
        return DIACRITICS_REGEX.replace(input, "")
    }

    fun normalizeForSearch(input: String): String {
        val noDiacritics = stripTashkeel(input)
        return noDiacritics
            .replace('أ', 'ا')
            .replace('إ', 'ا')
            .replace('آ', 'ا')
            .replace('ٱ', 'ا')
            .replace('ى', 'ي')
            .replace('ة', 'ه')
            .replace('ؤ', 'و')
            .replace('ئ', 'ي')
            .trim()
            .lowercase()
    }

    private val EASTERN_DIGITS = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')

    fun toEasternArabicNumerals(number: Int): String {
        return toEasternArabicNumerals(number.toLong())
    }

    fun toEasternArabicNumerals(number: Long): String {
        val str = number.toString()
        val sb = StringBuilder(str.length)
        for (ch in str) {
            if (ch in '0'..'9') {
                sb.append(EASTERN_DIGITS[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun formatAyahEndMarker(ayahNumber: Int): String {
        return " ﴿${toEasternArabicNumerals(ayahNumber)}﴾"
    }
}
