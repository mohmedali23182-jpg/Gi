package com.dawahayah.app.data.local

import android.content.Context
import com.dawahayah.app.core.contentpacks.Stage01AnatomyAssetPlayer
import com.dawahayah.app.core.locale.ArabicSearchNormalizer
import com.dawahayah.app.domain.model.ActiveIngredient
import com.dawahayah.app.domain.model.BodySystem
import com.dawahayah.app.domain.model.Disease
import com.dawahayah.app.domain.model.MedicalSource
import com.dawahayah.app.domain.model.Organ
import com.dawahayah.app.domain.model.PackInstallState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class SeedBundleDto(
    val schemaVersion: Int,
    val seedRevision: String,
    val systems: List<BodySystem>,
    val organs: List<Organ>,
    val diseases: List<Disease>,
    val activeIngredients: List<ActiveIngredient>,
    val sources: List<MedicalSource>
)

/**
 * Ensures idempotent offline seeding from assets/seed/base_seed_v1.json for Stage 01 & Stage 02.
 * Upgrades existing Stage 01 installations smoothly to Stage 02 (13 systems, organs with TA2 structures,
 * 16+ diseases with A-J sections, 3D assets, and granular offline content packs) without duplicating records.
 */
class SeedDatabaseInitializer(
    private val context: Context,
    private val dao: KnowledgeDao,
    private val json: Json = Json { ignoreUnknownKeys = true }
) {

    suspend fun ensureSeededIfEmpty() = withContext(Dispatchers.IO) {
        val currentSystemCount = dao.getBodySystemsCount()
        val currentStructureCount = dao.getAnatomicalStructuresCount()
        if (currentSystemCount >= 13 && currentStructureCount > 0) {
            return@withContext
        }

        val rawJson = context.assets.open("seed/base_seed_v1.json")
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }

        val seedBundle = json.decodeFromString<SeedBundleDto>(rawJson)

        val systemEntities = seedBundle.systems.map { sys ->
            BodySystemEntity(
                id = sys.id,
                nameAr = sys.nameAr,
                nameEn = sys.nameEn,
                latinName = sys.latinName,
                descriptionAr = sys.descriptionAr,
                descriptionEn = sys.descriptionEn,
                sourceIdsCsv = sys.sourceIds.joinToString(","),
                lastReviewedAt = sys.lastReviewedAt,
                normalizedSearchText = ArabicSearchNormalizer.buildIndexableTokens(
                    sys.nameAr,
                    sys.nameEn,
                    sys.latinName,
                    sys.descriptionAr,
                    sys.descriptionEn
                ),
                hasDetailedPathway = sys.hasDetailedPathway,
                interSystemRelationsAr = sys.interSystemRelationsAr,
                interSystemRelationsEn = sys.interSystemRelationsEn,
                relatedDrugClassesCsv = sys.relatedDrugClasses.joinToString(",")
            )
        }

        val organEntities = mutableListOf<OrganEntity>()
        val structureEntities = mutableListOf<AnatomicalStructureEntity>()
        val labelEntities = mutableListOf<AnatomyLabelEntity>()
        val organSectionEntities = mutableListOf<OrganSectionEntity>()
        val scienceFigureEntities = mutableListOf<ScienceFigureEntity>()

        seedBundle.organs.forEach { org ->
            organEntities.add(
                OrganEntity(
                    id = org.id,
                    systemId = org.systemId,
                    nameAr = org.nameAr,
                    nameEn = org.nameEn,
                    latinName = org.latinName,
                    summaryAr = org.summaryAr,
                    summaryEn = org.summaryEn,
                    sourceIdsCsv = org.sourceIds.joinToString(","),
                    normalizedSearchText = ArabicSearchNormalizer.buildIndexableTokens(
                        org.nameAr,
                        org.nameEn,
                        org.latinName,
                        org.summaryAr,
                        org.summaryEn,
                        org.locationAr,
                        org.locationEn
                    ),
                    locationAr = org.locationAr,
                    locationEn = org.locationEn,
                    has3dAsset = org.has3dAsset,
                    asset3dId = org.asset3dId,
                    relatedDiseaseIdsCsv = org.relatedDiseaseIds.joinToString(","),
                    relatedDrugClassesCsv = org.relatedDrugClasses.joinToString(",")
                )
            )

            org.structures.forEach { st ->
                structureEntities.add(
                    AnatomicalStructureEntity(
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
                        sourceId = st.sourceId,
                        normalizedSearchText = ArabicSearchNormalizer.buildIndexableTokens(
                            st.nameAr,
                            st.nameEn,
                            st.latinName,
                            st.ta2Code,
                            st.functionAr,
                            st.functionEn
                        )
                    )
                )
                labelEntities.add(
                    AnatomyLabelEntity(
                        id = "lbl-${st.id}",
                        structureId = st.id,
                        organId = st.organId,
                        labelAr = st.nameAr,
                        labelEn = st.nameEn,
                        labelLatin = st.latinName,
                        ta2Code = st.ta2Code
                    )
                )
            }

            org.sections.forEach { sec ->
                organSectionEntities.add(
                    OrganSectionEntity(
                        id = sec.id,
                        organId = sec.organId,
                        sectionKey = sec.sectionKey,
                        titleAr = sec.titleAr,
                        titleEn = sec.titleEn,
                        bodyAr = sec.bodyAr,
                        bodyEn = sec.bodyEn,
                        orderIndex = sec.orderIndex
                    )
                )
            }

            org.figures.forEach { fig ->
                scienceFigureEntities.add(
                    ScienceFigureEntity(
                        id = fig.id,
                        organId = fig.organId,
                        titleAr = fig.titleAr,
                        titleEn = fig.titleEn,
                        provenanceType = fig.provenanceType,
                        license = fig.license,
                        attribution = fig.attribution,
                        annotationsJson = json.encodeToString(fig.annotations)
                    )
                )
            }
        }

        val asset3dEntities = Stage01AnatomyAssetPlayer.STAGE_01_REGISTERED_3D_CONTRACTS.map { a ->
            Anatomy3DAssetEntity(
                assetId = a.assetId,
                organId = a.organId,
                systemId = a.systemId,
                anatomicalLabelAr = a.anatomicalLabelAr,
                anatomicalLabelEn = a.anatomicalLabelEn,
                latinTerminology = a.latinTerminology,
                objectKey = a.objectKey,
                localRelativePath = a.localRelativePath,
                qualityLevel = a.qualityLevel.name,
                triangleBudgetMax = a.triangleBudgetMax,
                sizeBytesBudgetMax = a.sizeBytesBudgetMax,
                actualSizeBytes = a.actualSizeBytes,
                sha256Checksum = a.sha256Checksum,
                license = a.license,
                attribution = a.attribution,
                sourceUrl = a.sourceUrl,
                isOfflineBundled = a.isAvailableInStage01,
                provenanceCategory = a.provenanceCategory
            )
        }

        val diseaseEntities = mutableListOf<DiseaseEntity>()
        val diseaseSectionEntities = mutableListOf<DiseaseSectionEntity>()
        val diseaseSystemLinks = mutableListOf<DiseaseSystemLinkEntity>()
        val diseaseOrganLinks = mutableListOf<DiseaseOrganLinkEntity>()
        val sourceAttributions = mutableListOf<SourceAttributionEntity>()

        seedBundle.diseases.forEach { dis ->
            val sectionSearchCorpusAr = dis.sections.joinToString(" ") { it.contentAr }
            val sectionSearchCorpusEn = dis.sections.joinToString(" ") { it.contentEn }

            diseaseEntities.add(
                DiseaseEntity(
                    id = dis.id,
                    primarySystemId = dis.primarySystemId,
                    nameAr = dis.nameAr,
                    nameEn = dis.nameEn,
                    introductionAr = dis.introductionAr,
                    introductionEn = dis.introductionEn,
                    citationIdsCsv = dis.citationIds.joinToString(","),
                    reviewStatus = dis.reviewStatus,
                    normalizedSearchText = ArabicSearchNormalizer.buildIndexableTokens(
                        dis.nameAr,
                        dis.nameEn,
                        dis.icd11Code,
                        dis.introductionAr,
                        dis.introductionEn,
                        sectionSearchCorpusAr,
                        sectionSearchCorpusEn
                    ),
                    icd11Code = dis.icd11Code,
                    secondarySystemIdsCsv = dis.secondarySystemIds.joinToString(","),
                    affectedOrganIdsCsv = dis.affectedOrganIds.joinToString(","),
                    relatedDrugClassesCsv = dis.relatedDrugClasses.joinToString(","),
                    importedAt = dis.importedAt,
                    humanReviewedAt = dis.humanReviewedAt
                )
            )

            diseaseSystemLinks.add(
                DiseaseSystemLinkEntity(
                    diseaseId = dis.id,
                    systemId = dis.primarySystemId,
                    isPrimary = true
                )
            )
            dis.secondarySystemIds.forEach { secSys ->
                diseaseSystemLinks.add(
                    DiseaseSystemLinkEntity(
                        diseaseId = dis.id,
                        systemId = secSys,
                        isPrimary = false
                    )
                )
            }
            dis.affectedOrganIds.forEach { orgId ->
                diseaseOrganLinks.add(
                    DiseaseOrganLinkEntity(
                        diseaseId = dis.id,
                        organId = orgId
                    )
                )
            }
            dis.citationIds.forEach { srcId ->
                sourceAttributions.add(
                    SourceAttributionEntity(
                        id = "attr-${dis.id}-$srcId",
                        entityId = dis.id,
                        entityType = "DISEASE",
                        sourceId = srcId,
                        importedAt = dis.importedAt,
                        humanReviewedAt = dis.humanReviewedAt,
                        reviewStatus = dis.reviewStatus
                    )
                )
            }

            dis.sections.forEach { sec ->
                diseaseSectionEntities.add(
                    DiseaseSectionEntity(
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
                )
            }
        }

        val ingredientEntities = seedBundle.activeIngredients.map { ing ->
            ActiveIngredientEntity(
                id = ing.id,
                nameAr = ing.nameAr,
                nameEn = ing.nameEn,
                className = ing.className,
                generalDescriptionAr = ing.generalDescriptionAr,
                generalDescriptionEn = ing.generalDescriptionEn,
                safetyNote = ing.safetyNote,
                citationIdsCsv = ing.citationIds.joinToString(","),
                reviewStatus = ing.reviewStatus,
                normalizedSearchText = ArabicSearchNormalizer.buildIndexableTokens(
                    ing.nameAr,
                    ing.nameEn,
                    ing.className,
                    ing.generalDescriptionAr,
                    ing.generalDescriptionEn
                )
            )
        }

        val sourceEntities = seedBundle.sources.map { src ->
            MedicalSourceEntity(
                id = src.id,
                title = src.title,
                url = src.url,
                publisher = src.publisher,
                publicationDate = src.publicationDate,
                accessedAt = src.accessedAt,
                license = src.license,
                attribution = src.attribution
            )
        }

        val initialPacks = listOf(
            PackRecordEntity(
                id = "pack-cardio-histology-ar-v1",
                version = "1.0.0",
                titleAr = "حزمة فسيولوجيا وتشريح نظام التوصيل القلبي (Cor Text & Vector)",
                titleEn = "Cardiac Conduction System Histology & Physiology Pack (Cor)",
                installState = PackInstallState.NOT_DOWNLOADED.name,
                installedAt = null,
                sizeBytes = 3_917L,
                downloadedBytes = 0L,
                locale = "ar",
                scopeType = "organ",
                scopeId = "organ-heart",
                assetQuality = "standard",
                license = "CC-BY-4.0 (Dawa & Hayah Original Educational Pack)",
                attribution = "دواء وحياة — الدكتور الصيدلاني معتز العلقمي — 2026 م.",
                isBundledOfflineDemoAvailable = true,
                companionDependencyNotice = null,
                lastErrorMessage = null
            ),
            PackRecordEntity(
                id = "heart-model-low-v1",
                version = "1.0.0",
                titleAr = "حزمة المجسم التشريحي ثلاثي الأبعاد للقلب — دقة الجوال (Cor 3D GLB Low LOD)",
                titleEn = "Heart 3D Anatomical Model Pack — Mobile Low LOD (Cor GLB)",
                installState = PackInstallState.NOT_DOWNLOADED.name,
                installedAt = null,
                sizeBytes = 50_576L,
                downloadedBytes = 0L,
                locale = "ar",
                scopeType = "model3d",
                scopeId = "organ-heart",
                assetQuality = "low_mobile",
                license = "CC-BY-4.0 (Dawa & Hayah Educational Multi-Mesh GLB — TA2 Labeled)",
                attribution = "دواء وحياة — الدكتور الصيدلاني معتز العلقمي — 2026 م.",
                isBundledOfflineDemoAvailable = true,
                companionDependencyNotice = "حزمة GLB ثلاثية الأبعاد مدمجة محلياً (6 أجزاء تشريحية TA2 قابلة للتحديد والشفافية).",
                lastErrorMessage = null
            ),
            PackRecordEntity(
                id = "respiratory-diseases-ar-v1",
                version = "1.0.0",
                titleAr = "حزمة أمراض الجهاز التنفسي وتبادل الغازات السنخي (Respiratory Pack)",
                titleEn = "Respiratory System Diseases & Alveolar Physiology Pack",
                installState = PackInstallState.NOT_DOWNLOADED.name,
                installedAt = null,
                sizeBytes = 3_126L,
                downloadedBytes = 0L,
                locale = "ar",
                scopeType = "system",
                scopeId = "sys-respiratory",
                assetQuality = "standard",
                license = "CC-BY-4.0 (Dawa & Hayah Original Educational Pack)",
                attribution = "دواء وحياة — الدكتور الصيدلاني معتز العلقمي — 2026 م.",
                isBundledOfflineDemoAvailable = true,
                companionDependencyNotice = null,
                lastErrorMessage = null
            ),
            PackRecordEntity(
                id = "heart-model-high-v1",
                version = "1.0.0",
                titleAr = "حزمة أطلس القلب ثلاثي الأبعاد عالي الدقة (Cor 3D GLB High LOD)",
                titleEn = "Heart High-Detail 3D Atlas Pack (Cor GLB High LOD)",
                installState = PackInstallState.NOT_DOWNLOADED.name,
                installedAt = null,
                sizeBytes = 6_420_000L,
                downloadedBytes = 0L,
                locale = "ar",
                scopeType = "model3d",
                scopeId = "organ-heart",
                assetQuality = "high_atlas",
                license = "CC-BY-SA-4.0 (Z-Anatomy / BodyParts3D Cloud Pack)",
                attribution = "Z-Anatomy / BodyParts3D — يتطلب اتصال Neon Cloud Object Storage",
                isBundledOfflineDemoAvailable = false,
                companionDependencyNotice = "حزمة اختيارية عالية الدقة (6.4 ميجابايت)؛ تعمل النسخة القياسية المنخفضة (heart-model-low-v1) محلياً دون الحاجة إليها.",
                lastErrorMessage = null
            ),
            PackRecordEntity(
                id = "pack-renal-pharmacokinetics-ar-v1",
                version = "1.0.0",
                titleAr = "حزمة الحرائك الدوائية والتصفية الكلوية (Renal Clearance)",
                titleEn = "Renal Clearance & Glomerular Pharmacokinetics Pack",
                installState = PackInstallState.NOT_DOWNLOADED.name,
                installedAt = null,
                sizeBytes = 148_400L,
                downloadedBytes = 0L,
                locale = "ar",
                scopeType = "system",
                scopeId = "sys-renal",
                assetQuality = "standard",
                license = "CC-BY-4.0 (Scheduled Cloud Pack on Neon Object Storage)",
                attribution = "دواء وحياة — يتطلب اتصال Neon Cloud API",
                isBundledOfflineDemoAvailable = false,
                companionDependencyNotice = "تحتاج حزمة مصاحبة للرسوم التوضيحية بحجم 64 كيلوبايت عند توفر السحابة.",
                lastErrorMessage = null
            )
        )

        dao.seedStage02KnowledgeTransaction(
            systems = systemEntities,
            organs = organEntities,
            structures = structureEntities,
            labels = labelEntities,
            organSections = organSectionEntities,
            figures = scienceFigureEntities,
            assets3d = asset3dEntities,
            diseases = diseaseEntities,
            diseaseSections = diseaseSectionEntities,
            systemLinks = diseaseSystemLinks,
            organLinks = diseaseOrganLinks,
            attributions = sourceAttributions,
            ingredients = ingredientEntities,
            sources = sourceEntities,
            initialPacks = initialPacks
        )
    }
}
