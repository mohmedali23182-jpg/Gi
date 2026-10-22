package com.dawahayah.app.data.repository

import com.dawahayah.app.core.contentpacks.ContentPackManager
import com.dawahayah.app.core.locale.ArabicSearchNormalizer
import com.dawahayah.app.data.local.BookmarkEntity
import com.dawahayah.app.data.local.KnowledgeDao
import com.dawahayah.app.data.local.ReadingProgressEntity
import com.dawahayah.app.data.remote.CloudBackendState
import com.dawahayah.app.data.remote.RemoteContentDataSource
import com.dawahayah.app.domain.model.ActiveIngredient
import com.dawahayah.app.domain.model.AnatomicalStructure
import com.dawahayah.app.domain.model.BodySystem
import com.dawahayah.app.domain.model.Bookmark
import com.dawahayah.app.domain.model.Disease
import com.dawahayah.app.domain.model.DiseaseSection
import com.dawahayah.app.domain.model.FigureAnnotation
import com.dawahayah.app.domain.model.LocalContent
import com.dawahayah.app.domain.model.MedicalSource
import com.dawahayah.app.domain.model.Organ
import com.dawahayah.app.domain.model.OrganSection
import com.dawahayah.app.domain.model.PackInstallState
import com.dawahayah.app.domain.model.PackRecord
import com.dawahayah.app.domain.model.ReadingProgress
import com.dawahayah.app.domain.model.ScienceFigure
import com.dawahayah.app.domain.model.SearchHit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Single Local-First Repository for Dawa & Hayah (Stage 01 + Stage 02).
 * Reads foundational and pack-installed knowledge directly from Room/SQLite without requiring network.
 */
class LocalFirstKnowledgeRepository(
    private val dao: KnowledgeDao,
    private val contentPackManager: ContentPackManager,
    private val remoteDataSource: RemoteContentDataSource,
    private val json: Json = Json { ignoreUnknownKeys = true }
) {

    val cloudBackendState: CloudBackendState
        get() = remoteDataSource.backendState

    val bodySystemsFlow: Flow<List<BodySystem>> = dao.observeBodySystems().map { list ->
        list.map {
            BodySystem(
                id = it.id,
                nameAr = it.nameAr,
                nameEn = it.nameEn,
                latinName = it.latinName,
                descriptionAr = it.descriptionAr,
                descriptionEn = it.descriptionEn,
                sourceIds = it.sourceIdsCsv.split(",").filter { s -> s.isNotBlank() },
                lastReviewedAt = it.lastReviewedAt,
                hasDetailedPathway = it.hasDetailedPathway,
                interSystemRelationsAr = it.interSystemRelationsAr,
                interSystemRelationsEn = it.interSystemRelationsEn,
                relatedDrugClasses = it.relatedDrugClassesCsv.split(",").filter { s -> s.isNotBlank() }
            )
        }
    }

    val organsFlow: Flow<List<Organ>> = combine(
        dao.observeAllOrgans(),
        dao.observeAllAnatomicalStructures(),
        dao.observeAllOrganSections(),
        dao.observeAllScienceFigures()
    ) { organList, structureList, sectionList, figureList ->
        val structuresByOrgan = structureList.groupBy { it.organId }
        val sectionsByOrgan = sectionList.groupBy { it.organId }
        val figuresByOrgan = figureList.groupBy { it.organId }

        organList.map { org ->
            val mappedStructures = structuresByOrgan[org.id].orEmpty().map { st ->
                AnatomicalStructure(
                    id = st.id,
                    organId = st.organId,
                    meshNodeName = st.meshNodeName,
                    nameAr = st.nameAr,
                    nameEn = st.nameEn,
                    latinName = st.latinName,
                    ta2Code = st.ta2Code,
                    structureType = st.structureType,
                    functionAr = st.functionAr,
                    functionEn = st.functionEn,
                    clinicalRelevanceAr = st.clinicalRelevanceAr,
                    clinicalRelevanceEn = st.clinicalRelevanceEn,
                    sourceId = st.sourceId
                )
            }
            val mappedSections = sectionsByOrgan[org.id].orEmpty().map { sec ->
                OrganSection(
                    id = sec.id,
                    organId = sec.organId,
                    sectionKey = sec.sectionKey,
                    titleAr = sec.titleAr,
                    titleEn = sec.titleEn,
                    bodyAr = sec.bodyAr,
                    bodyEn = sec.bodyEn,
                    orderIndex = sec.orderIndex
                )
            }
            val mappedFigures = figuresByOrgan[org.id].orEmpty().map { fig ->
                val parsedAnnotations = runCatching {
                    json.decodeFromString<List<FigureAnnotation>>(fig.annotationsJson)
                }.getOrDefault(emptyList())
                ScienceFigure(
                    id = fig.id,
                    organId = fig.organId,
                    titleAr = fig.titleAr,
                    titleEn = fig.titleEn,
                    provenanceType = fig.provenanceType,
                    license = fig.license,
                    attribution = fig.attribution,
                    annotations = parsedAnnotations
                )
            }
            Organ(
                id = org.id,
                systemId = org.systemId,
                nameAr = org.nameAr,
                nameEn = org.nameEn,
                latinName = org.latinName,
                summaryAr = org.summaryAr,
                summaryEn = org.summaryEn,
                sourceIds = org.sourceIdsCsv.split(",").filter { s -> s.isNotBlank() },
                locationAr = org.locationAr,
                locationEn = org.locationEn,
                has3dAsset = org.has3dAsset,
                asset3dId = org.asset3dId,
                relatedDiseaseIds = org.relatedDiseaseIdsCsv.split(",").filter { s -> s.isNotBlank() },
                relatedDrugClasses = org.relatedDrugClassesCsv.split(",").filter { s -> s.isNotBlank() },
                sections = mappedSections,
                structures = mappedStructures,
                figures = mappedFigures
            )
        }
    }

    val diseasesFlow: Flow<List<Disease>> = combine(
        dao.observeAllDiseases(),
        dao.observeAllDiseaseSections()
    ) { diseaseList, sectionList ->
        val sectionsByDisease = sectionList.groupBy { it.diseaseId }
        diseaseList.map { dis ->
            val mappedSections = sectionsByDisease[dis.id].orEmpty().map { sec ->
                DiseaseSection(
                    id = sec.id,
                    diseaseId = sec.diseaseId,
                    sectionCode = sec.sectionCode,
                    titleAr = sec.titleAr,
                    titleEn = sec.titleEn,
                    contentAr = sec.contentAr,
                    contentEn = sec.contentEn,
                    informationTier = sec.informationTier,
                    orderIndex = sec.orderIndex
                )
            }
            Disease(
                id = dis.id,
                primarySystemId = dis.primarySystemId,
                nameAr = dis.nameAr,
                nameEn = dis.nameEn,
                introductionAr = dis.introductionAr,
                introductionEn = dis.introductionEn,
                citationIds = dis.citationIdsCsv.split(",").filter { s -> s.isNotBlank() },
                reviewStatus = dis.reviewStatus,
                icd11Code = dis.icd11Code,
                secondarySystemIds = dis.secondarySystemIdsCsv.split(",").filter { s -> s.isNotBlank() },
                affectedOrganIds = dis.affectedOrganIdsCsv.split(",").filter { s -> s.isNotBlank() },
                relatedDrugClasses = dis.relatedDrugClassesCsv.split(",").filter { s -> s.isNotBlank() },
                importedAt = dis.importedAt,
                humanReviewedAt = dis.humanReviewedAt,
                sections = mappedSections
            )
        }
    }

    val activeIngredientsFlow: Flow<List<ActiveIngredient>> = dao.observeAllActiveIngredients().map { list ->
        list.map {
            ActiveIngredient(
                id = it.id,
                nameAr = it.nameAr,
                nameEn = it.nameEn,
                className = it.className,
                generalDescriptionAr = it.generalDescriptionAr,
                generalDescriptionEn = it.generalDescriptionEn,
                safetyNote = it.safetyNote,
                citationIds = it.citationIdsCsv.split(",").filter { s -> s.isNotBlank() },
                reviewStatus = it.reviewStatus
            )
        }
    }

    val medicalSourcesFlow: Flow<List<MedicalSource>> = dao.observeAllSources().map { list ->
        list.map {
            MedicalSource(
                id = it.id,
                title = it.title,
                url = it.url,
                publisher = it.publisher,
                publicationDate = it.publicationDate,
                accessedAt = it.accessedAt,
                license = it.license,
                attribution = it.attribution
            )
        }
    }

    val installedLessonsFlow: Flow<List<LocalContent>> = dao.observeLocalContents().map { list ->
        list.map {
            LocalContent(
                id = it.id,
                canonicalId = it.canonicalId,
                revision = it.revision,
                contentType = it.contentType,
                locale = it.locale,
                sourceId = it.sourceId,
                origin = it.origin,
                publishedAt = it.publishedAt,
                reviewStatus = it.reviewStatus,
                titleAr = it.titleAr,
                titleEn = it.titleEn,
                bodyAr = it.bodyAr,
                bodyEn = it.bodyEn
            )
        }
    }

    val packRecordsFlow: Flow<List<PackRecord>> = dao.observePackRecords().map { list ->
        list.map {
            PackRecord(
                id = it.id,
                version = it.version,
                titleAr = it.titleAr,
                titleEn = it.titleEn,
                installState = runCatching { PackInstallState.valueOf(it.installState) }
                    .getOrDefault(PackInstallState.NOT_DOWNLOADED),
                installedAt = it.installedAt,
                sizeBytes = it.sizeBytes,
                downloadedBytes = it.downloadedBytes,
                locale = it.locale,
                scopeType = it.scopeType,
                scopeId = it.scopeId,
                assetQuality = it.assetQuality,
                license = it.license,
                attribution = it.attribution,
                isBundledOfflineDemoAvailable = it.isBundledOfflineDemoAvailable,
                companionDependencyNotice = it.companionDependencyNotice,
                lastErrorMessage = it.lastErrorMessage
            )
        }
    }

    val bookmarksFlow: Flow<List<Bookmark>> = dao.observeBookmarks().map { list ->
        list.map {
            Bookmark(
                itemId = it.itemId,
                itemType = it.itemType,
                titleAr = it.titleAr,
                titleEn = it.titleEn,
                subtitle = it.subtitle,
                savedAtEpochMillis = it.savedAtEpochMillis
            )
        }
    }

    val readingProgressFlow: Flow<List<ReadingProgress>> = dao.observeReadingProgress().map { list ->
        list.map {
            ReadingProgress(
                itemId = it.itemId,
                itemType = it.itemType,
                titleAr = it.titleAr,
                titleEn = it.titleEn,
                progressPercent = it.progressPercent,
                updatedAtEpochMillis = it.updatedAtEpochMillis
            )
        }
    }

    suspend fun searchLocalKnowledge(rawQuery: String): List<SearchHit> =
        withContext(Dispatchers.IO) {
            val normalized = ArabicSearchNormalizer.normalize(rawQuery)
            if (normalized.isBlank()) return@withContext emptyList()

            val hits = mutableListOf<SearchHit>()

            dao.searchBodySystems(normalized).forEach { sys ->
                hits.add(
                    SearchHit(
                        itemId = sys.id,
                        category = "BODY_SYSTEM",
                        titleAr = sys.nameAr,
                        titleEn = sys.nameEn,
                        latinSubtitle = sys.latinName,
                        snippetAr = sys.descriptionAr,
                        snippetEn = sys.descriptionEn
                    )
                )
            }

            dao.searchOrgans(normalized).forEach { org ->
                hits.add(
                    SearchHit(
                        itemId = org.id,
                        category = "ORGAN",
                        titleAr = org.nameAr,
                        titleEn = org.nameEn,
                        latinSubtitle = org.latinName,
                        snippetAr = org.summaryAr,
                        snippetEn = org.summaryEn
                    )
                )
            }

            dao.searchAnatomicalStructures(normalized).forEach { st ->
                hits.add(
                    SearchHit(
                        itemId = st.id,
                        category = "ANATOMICAL_STRUCTURE",
                        titleAr = st.nameAr,
                        titleEn = st.nameEn,
                        latinSubtitle = "${st.latinName} (${st.ta2Code})",
                        snippetAr = st.functionAr,
                        snippetEn = st.functionEn
                    )
                )
            }

            dao.searchDiseases(normalized).forEach { dis ->
                hits.add(
                    SearchHit(
                        itemId = dis.id,
                        category = "DISEASE",
                        titleAr = dis.nameAr,
                        titleEn = dis.nameEn,
                        latinSubtitle = "ICD-11: ${dis.icd11Code}",
                        snippetAr = dis.introductionAr,
                        snippetEn = dis.introductionEn
                    )
                )
            }

            dao.searchActiveIngredients(normalized).forEach { ing ->
                hits.add(
                    SearchHit(
                        itemId = ing.id,
                        category = "MEDICINE",
                        titleAr = ing.nameAr,
                        titleEn = ing.nameEn,
                        latinSubtitle = ing.className,
                        snippetAr = ing.generalDescriptionAr,
                        snippetEn = ing.generalDescriptionEn
                    )
                )
            }

            dao.searchLocalContents(normalized).forEach { content ->
                hits.add(
                    SearchHit(
                        itemId = content.id,
                        category = "INSTALLED_PACK_LESSON",
                        titleAr = content.titleAr,
                        titleEn = content.titleEn,
                        latinSubtitle = content.origin,
                        snippetAr = content.bodyAr,
                        snippetEn = content.bodyEn
                    )
                )
            }

            hits
        }

    suspend fun toggleBookmark(
        itemId: String,
        itemType: String,
        titleAr: String,
        titleEn: String,
        subtitle: String,
        currentlyBookmarked: Boolean
    ) {
        if (currentlyBookmarked) {
            dao.removeBookmark(itemId)
        } else {
            dao.upsertBookmark(
                BookmarkEntity(
                    itemId = itemId,
                    itemType = itemType,
                    titleAr = titleAr,
                    titleEn = titleEn,
                    subtitle = subtitle,
                    savedAtEpochMillis = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun recordReadingProgress(
        itemId: String,
        itemType: String,
        titleAr: String,
        titleEn: String,
        progressPercent: Int = 100
    ) {
        dao.upsertReadingProgress(
            ReadingProgressEntity(
                itemId = itemId,
                itemType = itemType,
                titleAr = titleAr,
                titleEn = titleEn,
                progressPercent = progressPercent,
                updatedAtEpochMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun installPack(packId: String, simulateCorruptedHash: Boolean = false) =
        contentPackManager.installPack(packId, simulateCorruptedHash)

    suspend fun deletePack(packId: String) =
        contentPackManager.deleteInstalledPack(packId)

    suspend fun checkCloudStatus() = remoteDataSource.checkHealth()
}
