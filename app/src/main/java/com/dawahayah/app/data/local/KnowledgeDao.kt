package com.dawahayah.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface KnowledgeDao {

    @Query("SELECT COUNT(*) FROM body_systems")
    suspend fun getBodySystemsCount(): Int

    @Query("SELECT COUNT(*) FROM anatomical_structures")
    suspend fun getAnatomicalStructuresCount(): Int

    @Query("SELECT * FROM body_systems ORDER BY id ASC")
    fun observeBodySystems(): Flow<List<BodySystemEntity>>

    @Query("SELECT * FROM organs ORDER BY id ASC")
    fun observeAllOrgans(): Flow<List<OrganEntity>>

    @Query("SELECT * FROM organs WHERE systemId = :systemId ORDER BY id ASC")
    fun observeOrgansBySystem(systemId: String): Flow<List<OrganEntity>>

    @Query("SELECT * FROM anatomical_structures ORDER BY organId ASC, id ASC")
    fun observeAllAnatomicalStructures(): Flow<List<AnatomicalStructureEntity>>

    @Query("SELECT * FROM anatomical_structures WHERE organId = :organId ORDER BY id ASC")
    fun observeStructuresByOrgan(organId: String): Flow<List<AnatomicalStructureEntity>>

    @Query("SELECT * FROM anatomy_labels ORDER BY organId ASC, id ASC")
    fun observeAllAnatomyLabels(): Flow<List<AnatomyLabelEntity>>

    @Query("SELECT * FROM organ_sections ORDER BY organId ASC, orderIndex ASC")
    fun observeAllOrganSections(): Flow<List<OrganSectionEntity>>

    @Query("SELECT * FROM science_figures ORDER BY organId ASC, id ASC")
    fun observeAllScienceFigures(): Flow<List<ScienceFigureEntity>>

    @Query("SELECT * FROM anatomy_3d_assets ORDER BY assetId ASC")
    fun observeAllAnatomy3DAssets(): Flow<List<Anatomy3DAssetEntity>>

    @Query("SELECT * FROM diseases ORDER BY id ASC")
    fun observeAllDiseases(): Flow<List<DiseaseEntity>>

    @Query("SELECT * FROM disease_sections ORDER BY diseaseId ASC, orderIndex ASC")
    fun observeAllDiseaseSections(): Flow<List<DiseaseSectionEntity>>

    @Query("SELECT * FROM active_ingredients ORDER BY id ASC")
    fun observeAllActiveIngredients(): Flow<List<ActiveIngredientEntity>>

    @Query("SELECT * FROM medical_sources ORDER BY id ASC")
    fun observeAllSources(): Flow<List<MedicalSourceEntity>>

    @Query("SELECT * FROM local_contents ORDER BY publishedAt DESC")
    fun observeLocalContents(): Flow<List<LocalContentEntity>>

    @Query("SELECT * FROM pack_records ORDER BY id ASC")
    fun observePackRecords(): Flow<List<PackRecordEntity>>

    @Query("SELECT * FROM pack_records WHERE id = :packId LIMIT 1")
    suspend fun getPackRecordById(packId: String): PackRecordEntity?

    @Query("SELECT * FROM local_assets WHERE packId = :packId")
    fun observeAssetsForPack(packId: String): Flow<List<LocalAssetEntity>>

    @Query("SELECT * FROM bookmarks ORDER BY savedAtEpochMillis DESC")
    fun observeBookmarks(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM reading_progress ORDER BY updatedAtEpochMillis DESC")
    fun observeReadingProgress(): Flow<List<ReadingProgressEntity>>

    // Normalized Local Search Queries (Arabic, English, and Latin TA2)
    @Query("SELECT * FROM body_systems WHERE normalizedSearchText LIKE '%' || :normalizedQuery || '%'")
    suspend fun searchBodySystems(normalizedQuery: String): List<BodySystemEntity>

    @Query("SELECT * FROM organs WHERE normalizedSearchText LIKE '%' || :normalizedQuery || '%'")
    suspend fun searchOrgans(normalizedQuery: String): List<OrganEntity>

    @Query("SELECT * FROM anatomical_structures WHERE normalizedSearchText LIKE '%' || :normalizedQuery || '%'")
    suspend fun searchAnatomicalStructures(normalizedQuery: String): List<AnatomicalStructureEntity>

    @Query("SELECT * FROM diseases WHERE normalizedSearchText LIKE '%' || :normalizedQuery || '%'")
    suspend fun searchDiseases(normalizedQuery: String): List<DiseaseEntity>

    @Query("SELECT * FROM active_ingredients WHERE normalizedSearchText LIKE '%' || :normalizedQuery || '%'")
    suspend fun searchActiveIngredients(normalizedQuery: String): List<ActiveIngredientEntity>

    @Query("SELECT * FROM local_contents WHERE normalizedSearchText LIKE '%' || :normalizedQuery || '%'")
    suspend fun searchLocalContents(normalizedQuery: String): List<LocalContentEntity>

    // Inserts & Updates
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBodySystems(items: List<BodySystemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrgans(items: List<OrganEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnatomicalStructures(items: List<AnatomicalStructureEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnatomyLabels(items: List<AnatomyLabelEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrganSections(items: List<OrganSectionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScienceFigures(items: List<ScienceFigureEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnatomy3DAssets(items: List<Anatomy3DAssetEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiseases(items: List<DiseaseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiseaseSections(items: List<DiseaseSectionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiseaseSystemLinks(items: List<DiseaseSystemLinkEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiseaseOrganLinks(items: List<DiseaseOrganLinkEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSourceAttributions(items: List<SourceAttributionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActiveIngredients(items: List<ActiveIngredientEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicalSources(items: List<MedicalSourceEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocalContent(item: LocalContentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPackRecord(pack: PackRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDownloadTask(task: DownloadTaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocalAssets(assets: List<LocalAssetEntity>)

    @Query("DELETE FROM local_assets WHERE packId = :packId")
    suspend fun deleteAssetsByPackId(packId: String)

    @Query("DELETE FROM local_contents WHERE origin = :packId")
    suspend fun deleteLocalContentByPackOrigin(packId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE itemId = :itemId")
    suspend fun removeBookmark(itemId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReadingProgress(progress: ReadingProgressEntity)

    @Transaction
    suspend fun seedStage02KnowledgeTransaction(
        systems: List<BodySystemEntity>,
        organs: List<OrganEntity>,
        structures: List<AnatomicalStructureEntity>,
        labels: List<AnatomyLabelEntity>,
        organSections: List<OrganSectionEntity>,
        figures: List<ScienceFigureEntity>,
        assets3d: List<Anatomy3DAssetEntity>,
        diseases: List<DiseaseEntity>,
        diseaseSections: List<DiseaseSectionEntity>,
        systemLinks: List<DiseaseSystemLinkEntity>,
        organLinks: List<DiseaseOrganLinkEntity>,
        attributions: List<SourceAttributionEntity>,
        ingredients: List<ActiveIngredientEntity>,
        sources: List<MedicalSourceEntity>,
        initialPacks: List<PackRecordEntity>
    ) {
        insertBodySystems(systems)
        insertOrgans(organs)
        insertAnatomicalStructures(structures)
        insertAnatomyLabels(labels)
        insertOrganSections(organSections)
        insertScienceFigures(figures)
        insertAnatomy3DAssets(assets3d)
        insertDiseases(diseases)
        insertDiseaseSections(diseaseSections)
        insertDiseaseSystemLinks(systemLinks)
        insertDiseaseOrganLinks(organLinks)
        insertSourceAttributions(attributions)
        insertActiveIngredients(ingredients)
        insertMedicalSources(sources)
        initialPacks.forEach { existing ->
            val current = getPackRecordById(existing.id)
            if (current == null) {
                upsertPackRecord(existing)
            }
        }
    }

    @Transaction
    suspend fun commitPackInstallationTransaction(
        updatedPack: PackRecordEntity,
        task: DownloadTaskEntity,
        assets: List<LocalAssetEntity>,
        installedContent: LocalContentEntity?
    ) {
        upsertPackRecord(updatedPack)
        upsertDownloadTask(task)
        deleteAssetsByPackId(updatedPack.id)
        insertLocalAssets(assets)
        if (installedContent != null) {
            insertLocalContent(installedContent)
        }
    }

    @Transaction
    suspend fun removeInstalledPackTransaction(
        resetPack: PackRecordEntity
    ) {
        deleteAssetsByPackId(resetPack.id)
        deleteLocalContentByPackOrigin(resetPack.id)
        upsertPackRecord(resetPack)
    }
}
