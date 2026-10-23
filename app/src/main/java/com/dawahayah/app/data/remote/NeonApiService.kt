package com.dawahayah.app.data.remote

import com.dawahayah.app.core.contentpacks.Anatomy3DAsset
import com.dawahayah.app.core.errors.AppError
import com.dawahayah.app.core.errors.AppResult
import com.dawahayah.app.core.security.IntegrityVerifier
import com.dawahayah.app.domain.model.BodySystem
import com.dawahayah.app.domain.model.ContentPackManifest
import com.dawahayah.app.domain.model.Disease
import com.dawahayah.app.domain.model.LocalContent
import com.dawahayah.app.domain.model.MedicalSource
import com.dawahayah.app.domain.model.Organ
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

@Serializable
data class NeonHealthResponse(
    val status: String,
    val region: String = "aws-eu-central-1",
    val postgresConnected: Boolean = false,
    val storageBucketReady: Boolean = false,
    val apiVersion: String = "v1"
)

@Serializable
data class NeonCatalogResponse(
    val revision: Int,
    val locale: String,
    val totalPublishedItems: Int,
    val availablePacks: List<ContentPackManifest>
)

@Serializable
data class NeonAssetAccessResponse(
    val assetId: String,
    val accessMode: String, // "public_read" or "presigned_url"
    val downloadUrl: String,
    val expiresAt: String?,
    val sha256: String,
    val sizeBytes: Long,
    val license: String
)

@Serializable
data class NeonUpdatesResponse(
    val fromRevision: Int,
    val latestRevision: Int,
    val updatedContentIds: List<String>,
    val updatedPackIds: List<String>
)

/**
 * Official Neon Compute Functions HTTPS v1 API Contract (Stage 01 + Stage 02 Expansion).
 * Android NEVER connects directly to PostgreSQL or S3 with secret credentials.
 */
interface NeonApiService {

    @GET("health")
    suspend fun getHealth(): NeonHealthResponse

    @GET("v1/catalog")
    suspend fun getCatalog(@Query("locale") locale: String = "ar"): NeonCatalogResponse

    @GET("v1/systems")
    suspend fun getSystems(@Query("locale") locale: String = "ar"): List<BodySystem>

    @GET("v1/body-systems")
    suspend fun getBodySystems(@Query("locale") locale: String = "ar"): List<BodySystem>

    @GET("v1/organs/{id}")
    suspend fun getOrganById(
        @Path("id") organId: String,
        @Query("locale") locale: String = "ar"
    ): Organ

    @GET("v1/anatomy/models")
    suspend fun getAnatomyModels(
        @Query("organId") organId: String? = null,
        @Query("quality") quality: String? = null
    ): List<Anatomy3DAsset>

    @GET("v1/diseases")
    suspend fun getDiseases(
        @Query("systemId") systemId: String? = null,
        @Query("locale") locale: String = "ar"
    ): List<Disease>

    @GET("v1/diseases/{id}")
    suspend fun getDiseaseById(
        @Path("id") diseaseId: String,
        @Query("locale") locale: String = "ar"
    ): Disease

    @GET("v1/content-packs")
    suspend fun getContentPacks(
        @Query("scope") scopeType: String? = null,
        @Query("scopeId") scopeId: String? = null
    ): List<ContentPackManifest>

    @GET("v1/sources/{id}")
    suspend fun getSourceById(@Path("id") sourceId: String): MedicalSource

    @GET("v1/content/{id}")
    suspend fun getContentById(
        @Path("id") canonicalId: String,
        @Query("locale") locale: String = "ar"
    ): LocalContent

    @GET("v1/packs")
    suspend fun getPacksByScope(
        @Query("scope") scopeType: String,
        @Query("scopeId") scopeId: String
    ): List<ContentPackManifest>

    @GET("v1/packs/{id}/manifest")
    suspend fun getPackManifest(@Path("id") packId: String): ContentPackManifest

    @GET("v1/assets/{id}/access")
    suspend fun getAssetAccess(@Path("id") assetId: String): NeonAssetAccessResponse

    @GET("v1/updates")
    suspend fun getUpdates(@Query("fromRevision") fromRevision: Int): NeonUpdatesResponse
}

sealed interface CloudBackendState {
    data object DisabledInStage01 : CloudBackendState
    data class Configured(val endpointBaseUrl: String) : CloudBackendState
}

interface RemoteContentDataSource {
    val backendState: CloudBackendState
    suspend fun checkHealth(): AppResult<NeonHealthResponse>
    suspend fun fetchCatalog(locale: String): AppResult<NeonCatalogResponse>
    suspend fun fetchPackManifest(packId: String): AppResult<ContentPackManifest>
    suspend fun fetchAssetAccess(assetId: String): AppResult<NeonAssetAccessResponse>
}

/**
 * Default Offline-First RemoteDataSource when Neon Functions are not yet deployed externally.
 * Explicitly reports CloudBackendNotConfigured instead of faking HTTP success.
 */
class DisabledNeonDataSource : RemoteContentDataSource {
    override val backendState: CloudBackendState = CloudBackendState.DisabledInStage01

    override suspend fun checkHealth(): AppResult<NeonHealthResponse> =
        AppResult.Error(AppError.CloudBackendNotConfigured)

    override suspend fun fetchCatalog(locale: String): AppResult<NeonCatalogResponse> =
        AppResult.Error(AppError.CloudBackendNotConfigured)

    override suspend fun fetchPackManifest(packId: String): AppResult<ContentPackManifest> =
        AppResult.Error(AppError.CloudBackendNotConfigured)

    override suspend fun fetchAssetAccess(assetId: String): AppResult<NeonAssetAccessResponse> =
        AppResult.Error(AppError.CloudBackendNotConfigured)
}

/**
 * Production-ready HTTPS RemoteDataSource backed by Neon Compute Functions.
 */
class RetrofitNeonDataSource(
    private val baseUrl: String,
    private val apiService: NeonApiService
) : RemoteContentDataSource {

    override val backendState: CloudBackendState = CloudBackendState.Configured(baseUrl)

    override suspend fun checkHealth(): AppResult<NeonHealthResponse> = safeCall {
        apiService.getHealth()
    }

    override suspend fun fetchCatalog(locale: String): AppResult<NeonCatalogResponse> = safeCall {
        apiService.getCatalog(locale)
    }

    override suspend fun fetchPackManifest(packId: String): AppResult<ContentPackManifest> = safeCall {
        apiService.getPackManifest(packId)
    }

    override suspend fun fetchAssetAccess(assetId: String): AppResult<NeonAssetAccessResponse> = safeCall {
        apiService.getAssetAccess(assetId)
    }

    private suspend inline fun <T> safeCall(block: () -> T): AppResult<T> {
        if (!IntegrityVerifier.isSecureHttpsEndpoint(baseUrl)) {
            return AppResult.Error(
                AppError.GeneralFailure(
                    reasonAr = "تم رفض الاتصال: يجب أن يكون عنوان Neon API عبر بروتوكول HTTPS حصرياً.",
                    reasonEn = "Connection rejected: Neon API base URL must use HTTPS exclusively."
                )
            )
        }
        return try {
            AppResult.Success(block())
        } catch (e: Exception) {
            AppResult.Error(
                AppError.GeneralFailure(
                    reasonAr = "تعذر الوصول إلى خادم Neon السحابي: ${e.localizedMessage ?: "خطأ شبكة"}",
                    reasonEn = "Failed to reach Neon Cloud API: ${e.localizedMessage ?: "Network error"}"
                )
            )
        }
    }
}
