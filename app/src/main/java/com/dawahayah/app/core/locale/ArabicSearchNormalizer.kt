package com.dawahayah.app.core.locale

/**
 * Arabic & English Search Normalizer for Dawa & Hayah.
 * Normalizes hamzas (أ/إ/آ/ٱ -> ا), taa marbuta (ة -> ه), alef maqsura (ى -> ي),
 * removes Arabic diacritics (tashkeel) and tatweel, and normalizes case/whitespace
 * for fast offline indexing in Room without altering the displayed scientific text.
 */
object ArabicSearchNormalizer {

    private val TASHKEEL_REGEX = Regex("[\\u064B-\\u065F\\u0670\\u06D6-\\u06ED]")
    private val MULTISPACE_REGEX = Regex("\\s+")

    fun normalize(rawText: String): String {
        if (rawText.isBlank()) return ""
        val sb = StringBuilder(rawText.length)
        val stripped = rawText.replace(TASHKEEL_REGEX, "")
        for (ch in stripped) {
            when (ch) {
                'أ', 'إ', 'آ', 'ٱ' -> sb.append('ا')
                'ة' -> sb.append('ه')
                'ى' -> sb.append('ي')
                'ؤ' -> sb.append('و')
                'ئ' -> sb.append('ي')
                '\u0640' -> { /* strip tatweel */ }
                else -> sb.append(ch.lowercaseChar())
            }
        }
        return sb.toString()
            .replace(MULTISPACE_REGEX, " ")
            .trim()
    }

    /**
     * Expands common medical/anatomical search synonyms in Arabic so searching
     * for colloquial or alternative academic terms matches canonical entries.
     */
    fun buildIndexableTokens(vararg fields: String?): String {
        val joined = fields.filterNotNull().joinToString(" ")
        val normalizedBase = normalize(joined)
        val synonyms = buildList {
            if (normalizedBase.contains("القلب") || normalizedBase.contains("cor")) {
                add("فؤاد")
                add("قلبيه")
                add("cardiac")
            }
            if (normalizedBase.contains("الكليتان") || normalizedBase.contains("ren")) {
                add("كليه")
                add("كلوي")
                add("كلى")
                add("renal")
            }
            if (normalizedBase.contains("الرئتان") || normalizedBase.contains("pulmo")) {
                add("رئه")
                add("تنفس")
                add("pulmonary")
            }
            if (normalizedBase.contains("فرط ضغط الدم") || normalizedBase.contains("hypertension")) {
                add("ارتفاع ضغط الدم")
                add("ضغط الدم المرتفع")
            }
        }
        return if (synonyms.isEmpty()) {
            normalizedBase
        } else {
            "$normalizedBase ${synonyms.joinToString(" ") { normalize(it) }}"
        }
    }
}
