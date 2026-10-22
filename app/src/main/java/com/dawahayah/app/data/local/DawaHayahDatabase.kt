package com.dawahayah.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        BodySystemEntity::class,
        OrganEntity::class,
        AnatomicalStructureEntity::class,
        AnatomyLabelEntity::class,
        OrganSectionEntity::class,
        ScienceFigureEntity::class,
        Anatomy3DAssetEntity::class,
        DiseaseEntity::class,
        DiseaseSectionEntity::class,
        DiseaseSystemLinkEntity::class,
        DiseaseOrganLinkEntity::class,
        SourceAttributionEntity::class,
        ActiveIngredientEntity::class,
        MedicalSourceEntity::class,
        LocalContentEntity::class,
        PackRecordEntity::class,
        BookmarkEntity::class,
        ReadingProgressEntity::class,
        DownloadTaskEntity::class,
        LocalAssetEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class DawaHayahDatabase : RoomDatabase() {

    abstract fun knowledgeDao(): KnowledgeDao

    companion object {
        private const val DATABASE_NAME = "dawa_hayah_knowledge.db"

        /**
         * Explicit Room Migration from schema v1 to v2 ensuring user bookmarks,
         * reading progress, and installed pack metadata are preserved across upgrades.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE pack_records ADD COLUMN companionDependencyNotice TEXT DEFAULT NULL"
                )
            }
        }

        /**
         * Explicit Room Migration from schema v2 (Stage 01) to v3 (Stage 02: Anatomy, 3D Viewer & Diseases).
         * Preserves all existing bookmarks, reading_progress, pack_records, and local_assets while adding
         * anatomical_structures, anatomy_labels, organ_sections, science_figures, anatomy_3d_assets,
         * disease_sections, disease_system_links, disease_organ_links, and source_attributions.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Expand body_systems
                db.execSQL("ALTER TABLE body_systems ADD COLUMN hasDetailedPathway INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE body_systems ADD COLUMN interSystemRelationsAr TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE body_systems ADD COLUMN interSystemRelationsEn TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE body_systems ADD COLUMN relatedDrugClassesCsv TEXT NOT NULL DEFAULT ''")

                // Expand organs
                db.execSQL("ALTER TABLE organs ADD COLUMN locationAr TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE organs ADD COLUMN locationEn TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE organs ADD COLUMN has3dAsset INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE organs ADD COLUMN asset3dId TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE organs ADD COLUMN relatedDiseaseIdsCsv TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE organs ADD COLUMN relatedDrugClassesCsv TEXT NOT NULL DEFAULT ''")

                // Expand diseases
                db.execSQL("ALTER TABLE diseases ADD COLUMN icd11Code TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE diseases ADD COLUMN secondarySystemIdsCsv TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE diseases ADD COLUMN affectedOrganIdsCsv TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE diseases ADD COLUMN relatedDrugClassesCsv TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE diseases ADD COLUMN importedAt TEXT NOT NULL DEFAULT '2026-10-22'")
                db.execSQL("ALTER TABLE diseases ADD COLUMN humanReviewedAt TEXT DEFAULT '2026-10-22'")

                // New Stage 02 tables
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `anatomical_structures` (
                        `id` TEXT NOT NULL,
                        `organId` TEXT NOT NULL,
                        `meshNodeName` TEXT,
                        `nameAr` TEXT NOT NULL,
                        `nameEn` TEXT NOT NULL,
                        `latinName` TEXT NOT NULL,
                        `ta2Code` TEXT NOT NULL,
                        `structureType` TEXT NOT NULL,
                        `functionAr` TEXT NOT NULL,
                        `functionEn` TEXT NOT NULL,
                        `clinicalRelevanceAr` TEXT NOT NULL,
                        `clinicalRelevanceEn` TEXT NOT NULL,
                        `sourceId` TEXT NOT NULL,
                        `normalizedSearchText` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_anatomical_structures_organId` ON `anatomical_structures` (`organId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_anatomical_structures_normalizedSearchText` ON `anatomical_structures` (`normalizedSearchText`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `anatomy_labels` (
                        `id` TEXT NOT NULL,
                        `structureId` TEXT NOT NULL,
                        `organId` TEXT NOT NULL,
                        `labelAr` TEXT NOT NULL,
                        `labelEn` TEXT NOT NULL,
                        `labelLatin` TEXT NOT NULL,
                        `ta2Code` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_anatomy_labels_structureId` ON `anatomy_labels` (`structureId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_anatomy_labels_organId` ON `anatomy_labels` (`organId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `organ_sections` (
                        `id` TEXT NOT NULL,
                        `organId` TEXT NOT NULL,
                        `sectionKey` TEXT NOT NULL,
                        `titleAr` TEXT NOT NULL,
                        `titleEn` TEXT NOT NULL,
                        `bodyAr` TEXT NOT NULL,
                        `bodyEn` TEXT NOT NULL,
                        `orderIndex` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_organ_sections_organId` ON `organ_sections` (`organId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `science_figures` (
                        `id` TEXT NOT NULL,
                        `organId` TEXT NOT NULL,
                        `titleAr` TEXT NOT NULL,
                        `titleEn` TEXT NOT NULL,
                        `provenanceType` TEXT NOT NULL,
                        `license` TEXT NOT NULL,
                        `attribution` TEXT NOT NULL,
                        `annotationsJson` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_science_figures_organId` ON `science_figures` (`organId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `anatomy_3d_assets` (
                        `assetId` TEXT NOT NULL,
                        `organId` TEXT NOT NULL,
                        `systemId` TEXT NOT NULL,
                        `anatomicalLabelAr` TEXT NOT NULL,
                        `anatomicalLabelEn` TEXT NOT NULL,
                        `latinTerminology` TEXT NOT NULL,
                        `objectKey` TEXT NOT NULL,
                        `localRelativePath` TEXT,
                        `qualityLevel` TEXT NOT NULL,
                        `triangleBudgetMax` INTEGER NOT NULL,
                        `sizeBytesBudgetMax` INTEGER NOT NULL,
                        `actualSizeBytes` INTEGER NOT NULL,
                        `sha256Checksum` TEXT NOT NULL,
                        `license` TEXT NOT NULL,
                        `attribution` TEXT NOT NULL,
                        `sourceUrl` TEXT NOT NULL,
                        `isOfflineBundled` INTEGER NOT NULL,
                        `provenanceCategory` TEXT NOT NULL,
                        PRIMARY KEY(`assetId`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_anatomy_3d_assets_organId` ON `anatomy_3d_assets` (`organId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_anatomy_3d_assets_systemId` ON `anatomy_3d_assets` (`systemId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `disease_sections` (
                        `id` TEXT NOT NULL,
                        `diseaseId` TEXT NOT NULL,
                        `sectionCode` TEXT NOT NULL,
                        `titleAr` TEXT NOT NULL,
                        `titleEn` TEXT NOT NULL,
                        `contentAr` TEXT NOT NULL,
                        `contentEn` TEXT NOT NULL,
                        `informationTier` TEXT NOT NULL,
                        `orderIndex` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_disease_sections_diseaseId` ON `disease_sections` (`diseaseId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `disease_system_links` (
                        `diseaseId` TEXT NOT NULL,
                        `systemId` TEXT NOT NULL,
                        `isPrimary` INTEGER NOT NULL,
                        PRIMARY KEY(`diseaseId`, `systemId`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_disease_system_links_systemId` ON `disease_system_links` (`systemId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `disease_organ_links` (
                        `diseaseId` TEXT NOT NULL,
                        `organId` TEXT NOT NULL,
                        PRIMARY KEY(`diseaseId`, `organId`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_disease_organ_links_organId` ON `disease_organ_links` (`organId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `source_attributions` (
                        `id` TEXT NOT NULL,
                        `entityId` TEXT NOT NULL,
                        `entityType` TEXT NOT NULL,
                        `sourceId` TEXT NOT NULL,
                        `importedAt` TEXT NOT NULL,
                        `humanReviewedAt` TEXT,
                        `reviewStatus` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_source_attributions_entityId` ON `source_attributions` (`entityId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_source_attributions_sourceId` ON `source_attributions` (`sourceId`)")
            }
        }

        @Volatile
        private var INSTANCE: DawaHayahDatabase? = null

        fun getInstance(context: Context): DawaHayahDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    DawaHayahDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
