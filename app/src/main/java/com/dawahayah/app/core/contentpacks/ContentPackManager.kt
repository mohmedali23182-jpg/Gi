package com.dawahayah.app.core.contentpacks

import android.content.Context
import com.dawahayah.app.core.errors.AppError
import com.dawahayah.app.core.errors.AppResult
import com.dawahayah.app.core.locale.ArabicSearchNormalizer
import com.dawahayah.app.core.security.IntegrityVerifier
import com.dawahayah.app.data.local.DownloadTaskEntity
import com.dawahayah.app.data.local.KnowledgeDao
import com.dawahayah.app.data.local.LocalAssetEntity
import com.dawahayah.app.data.local.LocalContentEntity
import com.dawahayah.app.data.local.PackRecordEntity
import com.dawahayah.app.data.remote.RemoteContentDataSource
import com.dawahayah.app.domain.model.ContentPackManifest
import com.dawahayah.app.domain.model.PackInstallState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.net.URL
import javax.net.ssl.HttpsURLConnection

@Serializable
data class BundledPackLessonSectionDto(
    val heading: String,
    val body: String
)

@Serializable
data class BundledPackLessonDto(
    val canonicalId: String,
    val packId: String,
    val organId: String,
    val systemId: String,
    val revision: Int,
    val titleAr: String,
    val titleEn: String,
    val reviewStatus: String,
    val importedAt: String,
    val humanReviewedAt: String? = null,
    val license: String,
    val attribution: String,
    val sectionsAr: List<BundledPackLessonSectionDto>,
    val sectionsEn: List<BundledPackLessonSectionDto>,
    val sourceIds: List<String>
)

/**
 * Unified ContentPackManager for Dawa & Hayah.
 * Handles:
 * 1. Staging downloads in app-private staging directory (filesDir/staging/{packId})
 * 2. Cryptographic SHA-256 verification, byte size verification, and path traversal checks
 * 3. Atomic installation into persistent storage (filesDir/content_packs/{packId}/{version})
 * 4. Indexing installed lessons & assets in Room within a single transaction
 * 5. Deleting installed packs cleanly without removing BaseSeed records
 */
class ContentPackManager(
    private val context: Context,
    private val dao: KnowledgeDao,
    private val remoteDataSource: RemoteContentDataSource,
    private val json: Json = Json { ignoreUnknownKeys = true }
) {

    private val stagingRootDir: File
        get() = File(context.filesDir, "staging").apply { mkdirs() }

    private val persistentPacksRootDir: File
        get() = File(context.filesDir, "content_packs").apply { mkdirs() }

    /**
     * Installs or downloads a content pack.
     * For the Stage 01 bundled demonstration pack ("pack-cardio-histology-ar-v1"),
     * runs the exact staging -> SHA-256 verification -> persistent install -> Room transaction pipeline
     * so offline pack persistence works even in Airplane mode.
     * For cloud-only packs, queries RemoteContentDataSource and honestly reports DisabledBackend if Neon is not yet deployed.
     */
    suspend fun installPack(
        packId: String,
        simulateCorruptedHash: Boolean = false
    ): AppResult<PackRecordEntity> = withContext(Dispatchers.IO) {
        val currentRecord = dao.getPackRecordById(packId)
            ?: return@withContext AppResult.Error(
                AppError.GeneralFailure(
                    reasonAr = "لم يتم العثور على سجل الحزمة المطلوبة: $packId",
                    reasonEn = "Requested pack record not found: $packId"
                )
            )

        // One install pipeline for bundled and optional cloud packs.
        val remoteManifest = if (!currentRecord.isBundledOfflineDemoAvailable) {
            when (val result = remoteDataSource.fetchPackManifest(packId)) {
                is AppResult.Success -> result.data
                is AppResult.Error -> {
                    dao.upsertPackRecord(currentRecord.copy(
                        installState = PackInstallState.FAILED.name,
                        lastErrorMessage = result.error.messageAr
                    ))
                    return@withContext result
                }
            }
        } else null

        // 1. Mark QUEUED -> RUNNING
        val runningRecord = currentRecord.copy(
            installState = PackInstallState.RUNNING.name,
            downloadedBytes = 0L,
            lastErrorMessage = null
        )
        dao.upsertPackRecord(runningRecord)

        val manifest = if (remoteManifest != null) {
            remoteManifest
        } else {
            val manifestPath = "packs/$packId/manifest.json"
            val manifestText = try {
                context.assets.open(manifestPath).bufferedReader(Charsets.UTF_8).use { it.readText() }
            } catch (e: Exception) {
                return@withContext failPack(currentRecord, AppError.MissingPackFiles(listOf(manifestPath)))
            }
            json.decodeFromString<ContentPackManifest>(manifestText)
        }

        if (!packId.matches(Regex("[a-zA-Z0-9_-]{1,128}")) ||
            manifest.packId != packId || manifest.files.isEmpty() ||
            manifest.files.size > 300 || manifest.sizeBytes <= 0L ||
            manifest.files.any { it.sizeBytes <= 0L } ||
            manifest.files.sumOf { it.sizeBytes } != manifest.sizeBytes ||
            manifest.sizeBytes > 250L * 1024L * 1024L ||
            manifest.schemaVersion != 1
        ) {
            return@withContext failPack(currentRecord, AppError.GeneralFailure(
                reasonAr = "حزمة غير متوافقة أو كبيرة جدًا أو لا تطابق معرفها.",
                reasonEn = "Incompatible, oversized, or mismatched content pack."
            ))
        }

        // Check available storage
        val usableSpace = context.filesDir.usableSpace
        if (usableSpace < manifest.sizeBytes * 2) {
            return@withContext failPack(
                currentRecord,
                AppError.InsufficientStorage(manifest.sizeBytes, usableSpace)
            )
        }

        val packStagingDir = File(stagingRootDir, "${manifest.packId}_${manifest.version}")
        if (packStagingDir.exists()) {
            packStagingDir.deleteRecursively()
        }
        packStagingDir.mkdirs()

        var cumulativeBytes = 0L
        val stagedFiles = mutableListOf<Pair<com.dawahayah.app.domain.model.PackFileEntry, File>>()

        // 2. Stage each file & verify Path Traversal + Size + SHA-256
        for (fileEntry in manifest.files) {
            if (!IntegrityVerifier.isSafeRelativePath(fileEntry.path, packStagingDir)) {
                packStagingDir.deleteRecursively()
                return@withContext failPack(
                    currentRecord,
                    AppError.PathTraversalDetected(fileEntry.path)
                )
            }

            // Stream to app-private staging instead of buffering a 250 MB model in RAM.
            // Reject untrusted paths, invalid asset metadata and oversized replies.
            val stagedFile = File(packStagingDir, fileEntry.path)
            try {
                stagedFile.parentFile?.mkdirs()
                if (!fileEntry.sha256.matches(Regex("[0-9a-fA-F]{64}")) ||
                    fileEntry.sizeBytes <= 0L || fileEntry.sizeBytes > 250L * 1024L * 1024L
                ) throw IOException("Invalid expected asset length or checksum")

                if (remoteManifest == null) {
                    context.assets.open("packs/$packId/${fileEntry.path}").use { input ->
                        stagedFile.outputStream().buffered().use { output -> input.copyTo(output) }
                    }
                } else {
                    val access = when (val result = remoteDataSource.fetchAssetAccess(fileEntry.assetId)) {
                        is AppResult.Success -> result.data
                        is AppResult.Error -> throw IOException(result.error.messageEn)
                    }
                    if (!access.sha256.equals(fileEntry.sha256, ignoreCase = true) ||
                        access.sizeBytes != fileEntry.sizeBytes ||
                        !IntegrityVerifier.isSecureHttpsEndpoint(access.downloadUrl)
                    ) throw IOException("Asset metadata mismatch or URL is not HTTPS")

                    val connection = URL(access.downloadUrl).openConnection() as HttpsURLConnection
                    connection.connectTimeout = 15_000
                    connection.readTimeout = 60_000
                    connection.instanceFollowRedirects = false
                    try {
                        if (connection.responseCode != HttpsURLConnection.HTTP_OK) {
                            throw IOException("Asset response HTTP ${connection.responseCode}")
                        }
                        var received = 0L
                        connection.inputStream.use { input ->
                            stagedFile.outputStream().buffered().use { output ->
                                val buffer = ByteArray(32 * 1024)
                                while (true) {
                                    val n = input.read(buffer)
                                    if (n < 0) break
                                    if (n == 0) continue
                                    received += n
                                    if (received > fileEntry.sizeBytes) {
                                        throw IOException("Asset exceeds declared byte length")
                                    }
                                    output.write(buffer, 0, n)
                                }
                            }
                        }
                    } finally {
                        connection.disconnect()
                    }
                }
                if (simulateCorruptedHash) stagedFile.appendBytes(byteArrayOf(0x00))
            } catch (e: Exception) {
                packStagingDir.deleteRecursively()
                return@withContext failPack(currentRecord, AppError.GeneralFailure(
                    reasonAr = "تعذر تنزيل ملف الحزمة أو التحقق منه: ${fileEntry.path}",
                    reasonEn = "Failed to stream or verify content pack asset: ${fileEntry.path}"
                ))
            }

            val actualHash = IntegrityVerifier.computeFileSha256(stagedFile)
            if (!actualHash.equals(fileEntry.sha256, ignoreCase = true) ||
                stagedFile.length() != fileEntry.sizeBytes
            ) {
                packStagingDir.deleteRecursively()
                return@withContext failPack(
                    currentRecord,
                    AppError.HashMismatch(
                        filePath = fileEntry.path,
                        expectedSha256 = fileEntry.sha256,
                        actualSha256 = actualHash
                    )
                )
            }

            cumulativeBytes += stagedFile.length()
            dao.upsertPackRecord(
                runningRecord.copy(downloadedBytes = cumulativeBytes)
            )
            stagedFiles.add(fileEntry to stagedFile)
        }

        // 3. Atomic Promotion from Staging -> Persistent Directory
        val persistentPackDir = File(persistentPacksRootDir, "${manifest.packId}/${manifest.version}")
        if (persistentPackDir.exists()) {
            persistentPackDir.deleteRecursively()
        }
        persistentPackDir.parentFile?.mkdirs()
        val moved = packStagingDir.renameTo(persistentPackDir)
        if (!moved) {
            packStagingDir.copyRecursively(persistentPackDir, overwrite = true)
            packStagingDir.deleteRecursively()
        }

        val now = System.currentTimeMillis()
        val localAssets = manifest.files.map { entry ->
            val finalFile = File(persistentPackDir, entry.path)
            LocalAssetEntity(
                assetId = entry.assetId,
                packId = manifest.packId,
                relativePath = entry.path,
                absolutePersistentPath = finalFile.absolutePath,
                contentType = entry.contentType,
                sizeBytes = entry.sizeBytes,
                sha256 = entry.sha256,
                verifiedAtEpochMillis = now
            )
        }

        // Parse lesson JSON from persistent storage to index in Room LocalContent
        val lessonFile = manifest.files
            .firstOrNull { it.path.startsWith("content/") && it.path.endsWith(".json") }
            ?.let { File(persistentPackDir, it.path) }
        val installedContentEntity = if (lessonFile?.exists() == true) {
            val lessonDto = runCatching { json.decodeFromString<BundledPackLessonDto>(
                lessonFile.readText(Charsets.UTF_8)
            ) }.getOrNull()
            if (lessonDto == null) null else {
            val fullBodyAr = lessonDto.sectionsAr.joinToString("\n\n") { "${it.heading}\n${it.body}" }
            val fullBodyEn = lessonDto.sectionsEn.joinToString("\n\n") { "${it.heading}\n${it.body}" }
            LocalContentEntity(
                id = "${lessonDto.canonicalId}-${manifest.language}",
                canonicalId = lessonDto.canonicalId,
                revision = lessonDto.revision,
                contentType = "PACK_LESSON",
                locale = manifest.language,
                sourceId = lessonDto.sourceIds.firstOrNull() ?: "src-ta2-fipat",
                origin = manifest.packId,
                publishedAt = manifest.publishedAt,
                reviewStatus = lessonDto.reviewStatus,
                titleAr = lessonDto.titleAr,
                titleEn = lessonDto.titleEn,
                bodyAr = fullBodyAr,
                bodyEn = fullBodyEn,
                normalizedSearchText = ArabicSearchNormalizer.buildIndexableTokens(
                    lessonDto.titleAr,
                    lessonDto.titleEn,
                    fullBodyAr,
                    fullBodyEn
                )
            )
            }
        } else {
            null
        }

        val completedRecord = currentRecord.copy(
            installState = PackInstallState.COMPLETED.name,
            installedAt = now,
            downloadedBytes = manifest.sizeBytes,
            lastErrorMessage = null
        )

        val completedTask = DownloadTaskEntity(
            taskId = "task-${manifest.packId}",
            packId = manifest.packId,
            state = PackInstallState.COMPLETED.name,
            downloadedBytes = manifest.sizeBytes,
            totalBytes = manifest.sizeBytes,
            currentFilePath = null,
            errorReason = null,
            updatedAtEpochMillis = now
        )

        dao.commitPackInstallationTransaction(
            updatedPack = completedRecord,
            task = completedTask,
            assets = localAssets,
            installedContent = installedContentEntity
        )

        AppResult.Success(completedRecord)
    }

    /**
     * Deletes an installed pack's persistent files and Room LocalContent without touching BaseSeed.
     */
    suspend fun deleteInstalledPack(packId: String): AppResult<PackRecordEntity> =
        withContext(Dispatchers.IO) {
            val currentRecord = dao.getPackRecordById(packId)
                ?: return@withContext AppResult.Error(
                    AppError.GeneralFailure(
                        reasonAr = "الحزمة غير موجودة: $packId",
                        reasonEn = "Pack not found: $packId"
                    )
                )

            val persistentDir = File(persistentPacksRootDir, packId)
            if (persistentDir.exists()) {
                persistentDir.deleteRecursively()
            }

            val resetRecord = currentRecord.copy(
                installState = PackInstallState.NOT_DOWNLOADED.name,
                installedAt = null,
                downloadedBytes = 0L,
                lastErrorMessage = null
            )
            dao.removeInstalledPackTransaction(resetRecord)
            AppResult.Success(resetRecord)
        }


    /**
     * Discover cloud metadata on demand; do not download any payload automatically.
     * Persist only the pack listing in Room so users can choose granular downloads.
     */
    suspend fun discoverCloudPacks(locale: String): AppResult<Int> = withContext(Dispatchers.IO) {
        when (val catalog = remoteDataSource.fetchCatalog(locale)) {
            is AppResult.Error -> catalog
            is AppResult.Success -> {
                var added = 0
                for (pack in catalog.data.availablePacks) {
                    if (pack.packId.isBlank() || pack.files.isEmpty() || pack.sizeBytes <= 0L) continue
                    val current = dao.getPackRecordById(pack.packId)
                    if (current?.isBundledOfflineDemoAvailable == true) continue
                    dao.upsertPackRecord(PackRecordEntity(
                        id = pack.packId,
                        version = pack.version,
                        titleAr = pack.titleAr,
                        titleEn = pack.titleEn,
                        installState = if (current?.installState == PackInstallState.COMPLETED.name
                            && current.version == pack.version) PackInstallState.COMPLETED.name
                            else PackInstallState.NOT_DOWNLOADED.name,
                        installedAt = if (current?.version == pack.version) current.installedAt else null,
                        sizeBytes = pack.sizeBytes,
                        downloadedBytes = if (current?.version == pack.version) current.downloadedBytes else 0L,
                        locale = pack.language,
                        scopeType = pack.scopeType,
                        scopeId = pack.scopeId,
                        assetQuality = pack.quality,
                        license = pack.license,
                        attribution = pack.attribution,
                        isBundledOfflineDemoAvailable = false,
                        companionDependencyNotice = pack.dependencies.joinToString(", ").ifEmpty { null },
                        lastErrorMessage = null
                    ))
                    added++
                }
                AppResult.Success(added)
            }
        }
    }

    private suspend fun failPack(
        record: PackRecordEntity,
        error: AppError
    ): AppResult<Nothing> {
        val failed = record.copy(
            installState = PackInstallState.FAILED.name,
            lastErrorMessage = error.messageAr
        )
        dao.upsertPackRecord(failed)
        return AppResult.Error(error)
    }
}
