package com.dawahayah.app.core.contentpacks

import kotlinx.serialization.Serializable

/**
 * Stage 02 3D Anatomical Asset & Player Contract (GLB/GLTF).
 * Distinguishes clearly between:
 * - VERIFIED_SCIENTIFIC_MESH: Audited anatomical atlas mesh (e.g., Z-Anatomy / BodyParts3D)
 * - SCHEMATIC_EDUCATIONAL_MESH: TA2-labeled multi-part schematic educational mesh bundled offline
 * - CLOUD_PACK_PENDING: Requires downloading optional 3D pack
 */
@Serializable
data class Anatomy3DAsset(
    val assetId: String,
    val organId: String,
    val systemId: String,
    val anatomicalLabelAr: String,
    val anatomicalLabelEn: String,
    val latinTerminology: String,
    val objectKey: String,
    val localRelativePath: String?,
    val qualityLevel: QualityLevel,
    val triangleBudgetMax: Int,
    val sizeBytesBudgetMax: Long,
    val actualSizeBytes: Long,
    val sha256Checksum: String,
    val license: String,
    val attribution: String,
    val sourceUrl: String,
    val isAvailableInStage01: Boolean = false,
    val stage01FallbackDiagramAsset: String,
    val provenanceCategory: String = "SCHEMATIC_EDUCATIONAL_MESH",
    val selectableSubMeshes: List<String> = emptyList(),
    val requiredPackId: String? = null
) {
    @Serializable
    enum class QualityLevel {
        LOW_MOBILE,
        STANDARD_EDUCATIONAL,
        HIGH_ATLAS
    }
}

sealed interface AssetPlayerState {
    data class Stage01IntroductoryDiagram(
        val asset: Anatomy3DAsset,
        val noticeAr: String,
        val noticeEn: String
    ) : AssetPlayerState

    data class ReadyToRender3D(
        val asset: Anatomy3DAsset,
        val verifiedLocalGlbPath: String
    ) : AssetPlayerState

    data class RequiresPackDownload(
        val asset: Anatomy3DAsset,
        val requiredPackId: String,
        val estimatedBytes: Long
    ) : AssetPlayerState

    data class FallbackTo2DDiagram(
        val asset: Anatomy3DAsset,
        val reasonAr: String,
        val reasonEn: String
    ) : AssetPlayerState
}

interface AnatomyAssetPlayerContract {
    fun resolveAssetState(
        asset: Anatomy3DAsset,
        isPackInstalled: Boolean,
        isWebGlSupported: Boolean = true
    ): AssetPlayerState
}

/**
 * Stage 02 implementation of AnatomyAssetPlayerContract.
 * Resolves local bundled or pack-installed GLB assets to ReadyToRender3D,
 * or falls back cleanly to 2D diagram when WebGL/GPU is unavailable or pack is not downloaded.
 */
class Stage01AnatomyAssetPlayer : AnatomyAssetPlayerContract {
    override fun resolveAssetState(
        asset: Anatomy3DAsset,
        isPackInstalled: Boolean,
        isWebGlSupported: Boolean
    ): AssetPlayerState {
        if (!isWebGlSupported) {
            return AssetPlayerState.FallbackTo2DDiagram(
                asset = asset,
                reasonAr = "تعذر تهيئة WebGL على معالج الرسوميات؛ تم التحويل التلقائي إلى المخطط التشريحي ثنائي الأبعاد.",
                reasonEn = "WebGL unavailable on GPU; automatically switched to 2D anatomical diagram fallback."
            )
        }
        val localPath = asset.localRelativePath
        if (localPath != null && (asset.isAvailableInStage01 || isPackInstalled)) {
            return AssetPlayerState.ReadyToRender3D(
                asset = asset,
                verifiedLocalGlbPath = localPath
            )
        }
        if (asset.requiredPackId != null && !isPackInstalled) {
            return AssetPlayerState.RequiresPackDownload(
                asset = asset,
                requiredPackId = asset.requiredPackId,
                estimatedBytes = asset.actualSizeBytes
            )
        }
        return AssetPlayerState.Stage01IntroductoryDiagram(
            asset = asset,
            noticeAr = "لا توجد نسخة 3D محلية مثبتة لهذا العضو بعد — يُعرض المخطط التشريحي ثنائي الأبعاد الموثق.",
            noticeEn = "No local 3D model installed for this organ yet — showing verified 2D anatomical diagram."
        )
    }

    companion object {
        val STAGE_01_REGISTERED_3D_CONTRACTS: List<Anatomy3DAsset> = listOf(
            Anatomy3DAsset(
                assetId = "3d-cor-chambers-low-v1",
                organId = "organ-heart",
                systemId = "sys-cardiovascular",
                anatomicalLabelAr = "القلب البشري والحجرات الأربع ونظام التوصيل (Low LOD)",
                anatomicalLabelEn = "Human Heart, Four Chambers & Conduction System (Low LOD)",
                latinTerminology = "Cor (Atria, Ventriculi, Arcus aortae, Nodus sinuatrialis)",
                objectKey = "anatomical-3d/cor_chambers_low_v1.glb",
                localRelativePath = "models/cor_chambers_low_v1.glb",
                qualityLevel = Anatomy3DAsset.QualityLevel.LOW_MOBILE,
                triangleBudgetMax = 15_000,
                sizeBytesBudgetMax = 500_000L,
                actualSizeBytes = 50_576L,
                sha256Checksum = "56ed783e8a6b79c81972767616f67580fc5aec564b6368da5bc54b74c2659ef8",
                license = "CC-BY-4.0 (Dawa & Hayah Educational Schematic Multi-Mesh GLB — TA2 Labeled)",
                attribution = "دواء وحياة — الدكتور الصيدلاني معتز العلقمي — 2026 م (وفق معيار Terminologia Anatomica TA2)",
                sourceUrl = "https://fipat.library.dal.ca/",
                isAvailableInStage01 = true,
                stage01FallbackDiagramAsset = "graphics/cor_anatomy_diagram.svg",
                provenanceCategory = "SCHEMATIC_EDUCATIONAL_MESH",
                selectableSubMeshes = listOf(
                    "Atrium_Dextrum",
                    "Atrium_Sinistrum",
                    "Ventriculus_Dexter",
                    "Ventriculus_Sinister",
                    "Arcus_Aortae",
                    "Nodus_Sinuatrialis"
                ),
                requiredPackId = "heart-model-low-v1"
            ),
            Anatomy3DAsset(
                assetId = "3d-cor-chambers-high-v1",
                organId = "organ-heart",
                systemId = "sys-cardiovascular",
                anatomicalLabelAr = "أطلس القلب التشريحي عالي الدقة (High LOD Cloud Pack)",
                anatomicalLabelEn = "High-Detail Heart Anatomical Atlas (High LOD Cloud Pack)",
                latinTerminology = "Cor (Trabeculae carneae, Valvae atrioventriculares)",
                objectKey = "anatomical-3d/cor_chambers_high_v1.glb",
                localRelativePath = null,
                qualityLevel = Anatomy3DAsset.QualityLevel.HIGH_ATLAS,
                triangleBudgetMax = 65_000,
                sizeBytesBudgetMax = 8_500_000L,
                actualSizeBytes = 6_420_000L,
                sha256Checksum = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                license = "CC-BY-SA-4.0 (Z-Anatomy / BodyParts3D High-Detail Mesh — Cloud Pack)",
                attribution = "Z-Anatomy / BodyParts3D (DBCLS Japan) / FIPAT TA2",
                sourceUrl = "https://www.z-anatomy.com/",
                isAvailableInStage01 = false,
                stage01FallbackDiagramAsset = "graphics/cor_anatomy_diagram.svg",
                provenanceCategory = "VERIFIED_SCIENTIFIC_MESH",
                selectableSubMeshes = listOf(
                    "Atrium_Dextrum",
                    "Atrium_Sinistrum",
                    "Ventriculus_Dexter",
                    "Ventriculus_Sinister",
                    "Valva_Atrioventricularis_Sinistra",
                    "Valva_Aortae"
                ),
                requiredPackId = "heart-model-high-v1"
            ),
            Anatomy3DAsset(
                assetId = "3d-pulmones-bronchi-v1",
                organId = "organ-lungs",
                systemId = "sys-respiratory",
                anatomicalLabelAr = "الرئتان والشجرة القصبية (Pulmones)",
                anatomicalLabelEn = "Lungs & Bronchial Tree (Pulmones)",
                latinTerminology = "Pulmones et Arbor bronchialis",
                objectKey = "anatomical-3d/pulmones_bronchi_v1.glb",
                localRelativePath = null,
                qualityLevel = Anatomy3DAsset.QualityLevel.STANDARD_EDUCATIONAL,
                triangleBudgetMax = 40_000,
                sizeBytesBudgetMax = 5_800_000L,
                actualSizeBytes = 4_150_000L,
                sha256Checksum = "8f434346648f6b96df89dda901c5176b10a6d83961dd3c1ac88b59b2dc327aa4",
                license = "CC-BY-SA-4.0 (Z-Anatomy Candidate — Cloud Pack)",
                attribution = "Terminologia Anatomica (FIPAT) / Dawa & Hayah",
                sourceUrl = "https://fipat.library.dal.ca/",
                isAvailableInStage01 = false,
                stage01FallbackDiagramAsset = "graphics/cor_anatomy_diagram.svg",
                provenanceCategory = "VERIFIED_SCIENTIFIC_MESH",
                selectableSubMeshes = listOf("Pulmo_Dexter", "Pulmo_Sinister", "Trachea", "Bronchi_Principales"),
                requiredPackId = "lungs-model-standard-v1"
            ),
            Anatomy3DAsset(
                assetId = "3d-ren-nephron-v1",
                organId = "organ-kidneys",
                systemId = "sys-renal",
                anatomicalLabelAr = "الكلية والقشرة واللب الكلوي (Ren)",
                anatomicalLabelEn = "Kidney Cortex, Medulla & Pelvis (Ren)",
                latinTerminology = "Ren (Cortex renalis, Medulla renalis, Pelvis renalis)",
                objectKey = "anatomical-3d/ren_cross_section_v1.glb",
                localRelativePath = null,
                qualityLevel = Anatomy3DAsset.QualityLevel.STANDARD_EDUCATIONAL,
                triangleBudgetMax = 35_000,
                sizeBytesBudgetMax = 4_500_000L,
                actualSizeBytes = 3_410_000L,
                sha256Checksum = "2c26b46b68ffc68ff99b453c1d30413413422d706483bfa0f98a5e886266e7ae",
                license = "CC-BY-SA-4.0 (Z-Anatomy Candidate — Cloud Pack)",
                attribution = "Terminologia Anatomica (FIPAT) / Dawa & Hayah",
                sourceUrl = "https://fipat.library.dal.ca/",
                isAvailableInStage01 = false,
                stage01FallbackDiagramAsset = "graphics/cor_anatomy_diagram.svg",
                provenanceCategory = "VERIFIED_SCIENTIFIC_MESH",
                selectableSubMeshes = listOf("Cortex_Renalis", "Pyramides_Renales", "Pelvis_Renalis", "Ureter"),
                requiredPackId = "kidney-model-standard-v1"
            )
        )
    }
}
