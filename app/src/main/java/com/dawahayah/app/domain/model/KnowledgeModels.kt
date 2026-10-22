package com.dawahayah.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class TerminologyLabelMode {
    ARABIC,
    ENGLISH,
    LATIN_TA2,
    TRILINGUAL
}

@Serializable
data class BodySystem(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val latinName: String? = null,
    val descriptionAr: String,
    val descriptionEn: String,
    val sourceIds: List<String>,
    val lastReviewedAt: String? = null,
    val hasDetailedPathway: Boolean = true,
    val interSystemRelationsAr: String = "",
    val interSystemRelationsEn: String = "",
    val relatedDrugClasses: List<String> = emptyList()
)

@Serializable
data class AnatomicalStructure(
    val id: String,
    val organId: String,
    val meshNodeName: String? = null,
    val nameAr: String,
    val nameEn: String,
    val latinName: String,
    val ta2Code: String,
    val structureType: String,
    val functionAr: String,
    val functionEn: String,
    val clinicalRelevanceAr: String,
    val clinicalRelevanceEn: String,
    val sourceId: String
)

@Serializable
data class AnatomyLabel(
    val id: String,
    val structureId: String,
    val organId: String,
    val labelAr: String,
    val labelEn: String,
    val labelLatin: String,
    val ta2Code: String
)

@Serializable
data class OrganSection(
    val id: String,
    val organId: String,
    val sectionKey: String,
    val titleAr: String,
    val titleEn: String,
    val bodyAr: String,
    val bodyEn: String,
    val orderIndex: Int
)

@Serializable
data class FigureAnnotation(
    val markerNumber: Int,
    val labelAr: String,
    val labelEn: String,
    val labelLatin: String,
    val descriptionAr: String,
    val descriptionEn: String
)

@Serializable
data class ScienceFigure(
    val id: String,
    val organId: String,
    val titleAr: String,
    val titleEn: String,
    val provenanceType: String, // "VERIFIED_SCIENTIFIC_DIAGRAM" | "SCHEMATIC_EDUCATIONAL"
    val license: String,
    val attribution: String,
    val annotations: List<FigureAnnotation> = emptyList()
)

@Serializable
data class Organ(
    val id: String,
    val systemId: String,
    val nameAr: String,
    val nameEn: String,
    val latinName: String? = null,
    val summaryAr: String,
    val summaryEn: String,
    val sourceIds: List<String>,
    val locationAr: String = "",
    val locationEn: String = "",
    val has3dAsset: Boolean = false,
    val asset3dId: String? = null,
    val relatedDiseaseIds: List<String> = emptyList(),
    val relatedDrugClasses: List<String> = emptyList(),
    val sections: List<OrganSection> = emptyList(),
    val structures: List<AnatomicalStructure> = emptyList(),
    val figures: List<ScienceFigure> = emptyList()
)

@Serializable
data class DiseaseSection(
    val id: String,
    val diseaseId: String,
    val sectionCode: String, // A_OVERVIEW .. J_TERMINOLOGY_AND_LINKS
    val titleAr: String,
    val titleEn: String,
    val contentAr: String,
    val contentEn: String,
    val informationTier: String = "GENERAL_EDUCATIONAL", // "GENERAL_EDUCATIONAL" | "CLINICAL_GUIDELINE_REF" | "UNDER_REVIEW_SAFEGUARD"
    val orderIndex: Int = 0
)

@Serializable
data class Disease(
    val id: String,
    val primarySystemId: String,
    val nameAr: String,
    val nameEn: String,
    val introductionAr: String,
    val introductionEn: String,
    val citationIds: List<String>,
    val reviewStatus: String,
    val icd11Code: String = "",
    val secondarySystemIds: List<String> = emptyList(),
    val affectedOrganIds: List<String> = emptyList(),
    val relatedDrugClasses: List<String> = emptyList(),
    val importedAt: String = "2026-10-22",
    val humanReviewedAt: String? = "2026-10-22",
    val sections: List<DiseaseSection> = emptyList()
)

@Serializable
data class ActiveIngredient(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val className: String,
    val generalDescriptionAr: String,
    val generalDescriptionEn: String,
    val safetyNote: String,
    val citationIds: List<String>,
    val reviewStatus: String
)

@Serializable
data class MedicalSource(
    val id: String,
    val title: String,
    val url: String,
    val publisher: String,
    val publicationDate: String? = null,
    val accessedAt: String? = null,
    val license: String,
    val attribution: String
)

@Serializable
data class LocalContent(
    val id: String,
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
    val bodyEn: String
)

data class Bookmark(
    val itemId: String,
    val itemType: String,
    val titleAr: String,
    val titleEn: String,
    val subtitle: String,
    val savedAtEpochMillis: Long
)

data class ReadingProgress(
    val itemId: String,
    val itemType: String,
    val titleAr: String,
    val titleEn: String,
    val progressPercent: Int,
    val updatedAtEpochMillis: Long
)

data class SearchHit(
    val itemId: String,
    val category: String,
    val titleAr: String,
    val titleEn: String,
    val latinSubtitle: String?,
    val snippetAr: String,
    val snippetEn: String
)
