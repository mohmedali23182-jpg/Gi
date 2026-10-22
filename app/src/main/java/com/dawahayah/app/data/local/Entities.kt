package com.dawahayah.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "body_systems",
    indices = [Index(value = ["normalizedSearchText"])]
)
data class BodySystemEntity(
    @PrimaryKey val id: String,
    val nameAr: String,
    val nameEn: String,
    val latinName: String?,
    val descriptionAr: String,
    val descriptionEn: String,
    val sourceIdsCsv: String,
    val lastReviewedAt: String?,
    val normalizedSearchText: String,
    val hasDetailedPathway: Boolean = true,
    val interSystemRelationsAr: String = "",
    val interSystemRelationsEn: String = "",
    val relatedDrugClassesCsv: String = ""
)

@Entity(
    tableName = "organs",
    indices = [
        Index(value = ["systemId"]),
        Index(value = ["normalizedSearchText"])
    ]
)
data class OrganEntity(
    @PrimaryKey val id: String,
    val systemId: String,
    val nameAr: String,
    val nameEn: String,
    val latinName: String?,
    val summaryAr: String,
    val summaryEn: String,
    val sourceIdsCsv: String,
    val normalizedSearchText: String,
    val locationAr: String = "",
    val locationEn: String = "",
    val has3dAsset: Boolean = false,
    val asset3dId: String? = null,
    val relatedDiseaseIdsCsv: String = "",
    val relatedDrugClassesCsv: String = ""
)

@Entity(
    tableName = "anatomical_structures",
    indices = [
        Index(value = ["organId"]),
        Index(value = ["normalizedSearchText"])
    ]
)
data class AnatomicalStructureEntity(
    @PrimaryKey val id: String,
    val organId: String,
    val meshNodeName: String?,
    val nameAr: String,
    val nameEn: String,
    val latinName: String,
    val ta2Code: String,
    val structureType: String,
    val functionAr: String,
    val functionEn: String,
    val clinicalRelevanceAr: String,
    val clinicalRelevanceEn: String,
    val sourceId: String,
    val normalizedSearchText: String
)

@Entity(
    tableName = "anatomy_labels",
    indices = [Index(value = ["structureId"]), Index(value = ["organId"])]
)
data class AnatomyLabelEntity(
    @PrimaryKey val id: String,
    val structureId: String,
    val organId: String,
    val labelAr: String,
    val labelEn: String,
    val labelLatin: String,
    val ta2Code: String
)

@Entity(
    tableName = "organ_sections",
    indices = [Index(value = ["organId"])]
)
data class OrganSectionEntity(
    @PrimaryKey val id: String,
    val organId: String,
    val sectionKey: String,
    val titleAr: String,
    val titleEn: String,
    val bodyAr: String,
    val bodyEn: String,
    val orderIndex: Int
)

@Entity(
    tableName = "science_figures",
    indices = [Index(value = ["organId"])]
)
data class ScienceFigureEntity(
    @PrimaryKey val id: String,
    val organId: String,
    val titleAr: String,
    val titleEn: String,
    val provenanceType: String,
    val license: String,
    val attribution: String,
    val annotationsJson: String
)

@Entity(
    tableName = "anatomy_3d_assets",
    indices = [Index(value = ["organId"]), Index(value = ["systemId"])]
)
data class Anatomy3DAssetEntity(
    @PrimaryKey val assetId: String,
    val organId: String,
    val systemId: String,
    val anatomicalLabelAr: String,
    val anatomicalLabelEn: String,
    val latinTerminology: String,
    val objectKey: String,
    val localRelativePath: String?,
    val qualityLevel: String,
    val triangleBudgetMax: Int,
    val sizeBytesBudgetMax: Long,
    val actualSizeBytes: Long,
    val sha256Checksum: String,
    val license: String,
    val attribution: String,
    val sourceUrl: String,
    val isOfflineBundled: Boolean,
    val provenanceCategory: String // "VERIFIED_SCIENTIFIC_MESH" | "SCHEMATIC_EDUCATIONAL_MESH" | "DEMO_PLACEHOLDER"
)

@Entity(
    tableName = "diseases",
    indices = [
        Index(value = ["primarySystemId"]),
        Index(value = ["normalizedSearchText"])
    ]
)
data class DiseaseEntity(
    @PrimaryKey val id: String,
    val primarySystemId: String,
    val nameAr: String,
    val nameEn: String,
    val introductionAr: String,
    val introductionEn: String,
    val citationIdsCsv: String,
    val reviewStatus: String,
    val normalizedSearchText: String,
    val icd11Code: String = "",
    val secondarySystemIdsCsv: String = "",
    val affectedOrganIdsCsv: String = "",
    val relatedDrugClassesCsv: String = "",
    val importedAt: String = "2026-10-22",
    val humanReviewedAt: String? = "2026-10-22"
)

@Entity(
    tableName = "disease_sections",
    indices = [Index(value = ["diseaseId"])]
)
data class DiseaseSectionEntity(
    @PrimaryKey val id: String,
    val diseaseId: String,
    val sectionCode: String,
    val titleAr: String,
    val titleEn: String,
    val contentAr: String,
    val contentEn: String,
    val informationTier: String,
    val orderIndex: Int
)

@Entity(
    tableName = "disease_system_links",
    primaryKeys = ["diseaseId", "systemId"],
    indices = [Index(value = ["systemId"])]
)
data class DiseaseSystemLinkEntity(
    val diseaseId: String,
    val systemId: String,
    val isPrimary: Boolean
)

@Entity(
    tableName = "disease_organ_links",
    primaryKeys = ["diseaseId", "organId"],
    indices = [Index(value = ["organId"])]
)
data class DiseaseOrganLinkEntity(
    val diseaseId: String,
    val organId: String
)

@Entity(
    tableName = "source_attributions",
    indices = [Index(value = ["entityId"]), Index(value = ["sourceId"])]
)
data class SourceAttributionEntity(
    @PrimaryKey val id: String,
    val entityId: String,
    val entityType: String,
    val sourceId: String,
    val importedAt: String,
    val humanReviewedAt: String?,
    val reviewStatus: String
)

@Entity(
    tableName = "active_ingredients",
    indices = [Index(value = ["normalizedSearchText"])]
)
data class ActiveIngredientEntity(
    @PrimaryKey val id: String,
    val nameAr: String,
    val nameEn: String,
    val className: String,
    val generalDescriptionAr: String,
    val generalDescriptionEn: String,
    val safetyNote: String,
    val citationIdsCsv: String,
    val reviewStatus: String,
    val normalizedSearchText: String
)

@Entity(tableName = "medical_sources")
data class MedicalSourceEntity(
    @PrimaryKey val id: String,
    val title: String,
    val url: String,
    val publisher: String,
    val publicationDate: String?,
    val accessedAt: String?,
    val license: String,
    val attribution: String
)

@Entity(
    tableName = "local_contents",
    indices = [Index(value = ["canonicalId"]), Index(value = ["normalizedSearchText"])]
)
data class LocalContentEntity(
    @PrimaryKey val id: String,
    val canonicalId: String,
    val revision: Int,
    val contentType: String,
    val locale: String,
    val sourceId: String,
    val origin: String,
    val publishedAt: String,
    val reviewStatus: String,
    val titleAr: String,
    val titleEn: String,
    val bodyAr: String,
    val bodyEn: String,
    val normalizedSearchText: String
)

@Entity(tableName = "pack_records")
data class PackRecordEntity(
    @PrimaryKey val id: String,
    val version: String,
    val titleAr: String,
    val titleEn: String,
    val installState: String,
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
    val companionDependencyNotice: String?,
    val lastErrorMessage: String?
)

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey val itemId: String,
    val itemType: String,
    val titleAr: String,
    val titleEn: String,
    val subtitle: String,
    val savedAtEpochMillis: Long
)

@Entity(tableName = "reading_progress")
data class ReadingProgressEntity(
    @PrimaryKey val itemId: String,
    val itemType: String,
    val titleAr: String,
    val titleEn: String,
    val progressPercent: Int,
    val updatedAtEpochMillis: Long
)

@Entity(tableName = "download_tasks")
data class DownloadTaskEntity(
    @PrimaryKey val taskId: String,
    val packId: String,
    val state: String,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val currentFilePath: String?,
    val errorReason: String?,
    val updatedAtEpochMillis: Long
)

@Entity(
    tableName = "local_assets",
    indices = [Index(value = ["packId"])]
)
data class LocalAssetEntity(
    @PrimaryKey val assetId: String,
    val packId: String,
    val relativePath: String,
    val absolutePersistentPath: String,
    val contentType: String,
    val sizeBytes: Long,
    val sha256: String,
    val verifiedAtEpochMillis: Long
)
