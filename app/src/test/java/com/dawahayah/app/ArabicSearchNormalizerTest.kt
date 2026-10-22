package com.dawahayah.app

import com.dawahayah.app.core.locale.ArabicSearchNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArabicSearchNormalizerTest {

    @Test
    fun `normalize unifies hamzas, taa marbuta, and alef maqsura`() {
        val raw = "الأَوعِيَةُ الدَّمَوِيَّةُ وَالإِطْرَاحُ الكُلَوِيُّ فِي المَعِدَةِ"
        val normalized = ArabicSearchNormalizer.normalize(raw)
        assertEquals("الاوعيه الدمويه والاطراح الكلوي في المعده", normalized)
    }

    @Test
    fun `normalize matches omeprazole with or without hamza`() {
        val indexed = ArabicSearchNormalizer.normalize("أوميبرازول")
        val queryWithoutHamza = ArabicSearchNormalizer.normalize("اوميبرازول")
        assertEquals(indexed, queryWithoutHamza)
    }

    @Test
    fun `buildIndexableTokens includes Latin and Arabic synonyms for Cor`() {
        val tokens = ArabicSearchNormalizer.buildIndexableTokens("القلب", "Heart", "Cor")
        assertTrue(tokens.contains("القلب"))
        assertTrue(tokens.contains("cor"))
        assertTrue(tokens.contains("فؤاد"))
        assertTrue(tokens.contains("cardiac"))
    }
}
