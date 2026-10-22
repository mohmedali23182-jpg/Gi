package com.dawahayah.app

import com.dawahayah.app.core.security.IntegrityVerifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ContentPackIntegrityTest {

    @Test
    fun `computeSha256 and verifySha256 pass on exact bytes and fail on corrupted bytes`() {
        val samplePayload = """{"canonicalId":"lesson-cardiac-conduction-physiology-01"}""".toByteArray(Charsets.UTF_8)
        val expectedHash = IntegrityVerifier.computeSha256(samplePayload)

        assertTrue(IntegrityVerifier.verifySha256(samplePayload, expectedHash))

        val corruptedPayload = samplePayload + byteArrayOf(0x01)
        assertFalse(IntegrityVerifier.verifySha256(corruptedPayload, expectedHash))
    }

    @Test
    fun `isSafeRelativePath rejects path traversal and absolute paths`() {
        val stagingRoot = File("/tmp/dawa_hayah_staging_test")
        assertTrue(IntegrityVerifier.isSafeRelativePath("content/cardiac_conduction_ar.json", stagingRoot))
        assertTrue(IntegrityVerifier.isSafeRelativePath("graphics/cor_conduction_vector.svg", stagingRoot))

        assertFalse(IntegrityVerifier.isSafeRelativePath("../secrets.json", stagingRoot))
        assertFalse(IntegrityVerifier.isSafeRelativePath("content/../../etc/passwd", stagingRoot))
        assertFalse(IntegrityVerifier.isSafeRelativePath("/absolute/path.json", stagingRoot))
    }

    @Test
    fun `isSecureHttpsEndpoint enforces HTTPS and rejects raw database URLs`() {
        assertTrue(IntegrityVerifier.isSecureHttpsEndpoint("https://api.dawahayah.example.neon.tech/v1"))
        assertFalse(IntegrityVerifier.isSecureHttpsEndpoint("http://insecure.example.com/v1"))
        assertFalse(IntegrityVerifier.isSecureHttpsEndpoint("postgresql://user:pass@ep-test.neon.tech/db"))
    }
}
