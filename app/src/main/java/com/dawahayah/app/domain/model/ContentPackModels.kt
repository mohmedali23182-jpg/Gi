package com.dawahayah.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class PackInstallState {
    NOT_DOWNLOADED,
    QUEUED,
    RUNNING,
    PAUSED,
    FAILED,
    COMPLETED,
    UPDATE_AVAILABLE
}

@Serializable
data class PackFileEntry(
    val path: String,
    val contentType: String,
    val sizeBytes: Long,
    val sha256: String,
    val assetId: String
)

@Serializable
data class ContentPackManifest(
    val packId: String,
    val version: String,
    val scopeType: String,
    val scopeId: String,
    val titleAr: String,
    val titleEn: String,
    val language: String,
    val contentKind: String,
    val quality: String,
    val schemaVersion: Int,
    val minAppVersion: String,
    val sizeBytes: Long,
    val files: List<PackFileEntry>,
    val dependencies: List<String> = emptyList(),
    val sources: List<String> = emptyList(),
    val license: String,
    val attribution: String,
    val publishedAt: String
)

data class PackRecord(
    val id: String,
    val version: String,
    val titleAr: String,
    val titleEn: String,
    val installState: PackInstallState,
    val installedAt: Long?,
    val sizeBytes: Long,
    val downloadedBytes: Long,
    val locale: String,
    val scopeType: String,
    val scopeId: String,
    val assetQuality: String,
    val license: String,
    val attribution: String,
    val isBundledOfflineDemoAvailable: Boolean,
    val companionDependencyNotice: String? = null,
    val lastErrorMessage: String? = null
)

data class DownloadTask(
    val taskId: String,
    val packId: String,
    val state: PackInstallState,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val currentFilePath: String?,
    val errorReason: String?,
    val updatedAtEpochMillis: Long
)

data class LocalAsset(
    val assetId: String,
    val packId: String,
    val relativePath: String,
    val absolutePersistentPath: String,
    val contentType: String,
    val sizeBytes: Long,
    val sha256: String,
    val verifiedAtEpochMillis: Long
)
