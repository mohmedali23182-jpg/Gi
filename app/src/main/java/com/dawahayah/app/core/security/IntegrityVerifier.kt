package com.dawahayah.app.core.security

import java.io.File
import java.io.InputStream
import java.security.MessageDigest

/**
 * Cryptographic SHA-256 & Path Traversal Verifier for Dawa & Hayah Content Packs.
 */
object IntegrityVerifier {

    fun computeSha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(bytes)
        return hash.joinToString("") { "%02x".format(it) }
    }

    fun computeSha256(inputStream: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)
        var read = inputStream.read(buffer)
        while (read > 0) {
            digest.update(buffer, 0, read)
            read = inputStream.read(buffer)
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun computeFileSha256(file: File): String {
        return file.inputStream().buffered().use { computeSha256(it) }
    }

    fun verifySha256(actualBytes: ByteArray, expectedHex: String): Boolean {
        val actualHex = computeSha256(actualBytes)
        return actualHex.equals(expectedHex.trim(), ignoreCase = true)
    }

    /**
     * Prevents zip-slip / directory traversal attacks in pack file paths.
     */
    fun isSafeRelativePath(relativePath: String, targetRootDir: File): Boolean {
        if (relativePath.isBlank()) return false
        if (relativePath.startsWith("/") || relativePath.startsWith("\\")) return false
        if (relativePath.contains("..")) return false
        val resolvedFile = File(targetRootDir, relativePath).canonicalFile
        val canonicalRoot = targetRootDir.canonicalFile
        return resolvedFile.path.startsWith(canonicalRoot.path + File.separator)
    }

    /**
     * Enforces HTTPS-only URLs for Neon Functions and Object Storage endpoints.
     */
    fun isSecureHttpsEndpoint(url: String): Boolean {
        val trimmed = url.trim()
        return trimmed.startsWith("https://", ignoreCase = true) &&
            !trimmed.contains("postgresql://", ignoreCase = true) &&
            !trimmed.contains("AWS_SECRET_ACCESS_KEY", ignoreCase = true)
    }
}
