package com.dawahayah.app.data.remote

import com.dawahayah.app.core.errors.AppError
import com.dawahayah.app.core.errors.AppResult
import com.dawahayah.app.core.security.IntegrityVerifier
import com.dawahayah.app.domain.model.ContentPackManifest
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * The selected Neon URL ends in /appdb/rest/v1: it is PostgREST, not a Neon
 * Functions origin. Use safe SECURITY DEFINER RPCs exposed exclusively for
 * published educational content. Never send PostgreSQL or Neon S3 credentials.
 */
interface DawaNeonDataApiService {
    @GET("rpc/dawa_public_catalog")
    suspend fun publicCatalog(@Query("p_locale") locale: String): NeonCatalogResponse

    @GET("rpc/dawa_public_pack_manifest")
    suspend fun publicPackManifest(@Query("p_pack_id") packId: String): ContentPackManifest

    @GET("rpc/dawa_public_asset_access")
    suspend fun publicAssetAccess(@Query("p_asset_id") assetId: String): NeonAssetAccessResponse
}

class NeonDataApiRemoteDataSource(
    private val baseUrl: String,
    private val service: DawaNeonDataApiService
) : RemoteContentDataSource {

    override val backendState: CloudBackendState = CloudBackendState.Configured(baseUrl)

    override suspend fun checkHealth(): AppResult<NeonHealthResponse> =
        when (fetchCatalog("ar")) {
            is AppResult.Success -> AppResult.Success(
                NeonHealthResponse(
                    status = "ok",
                    region = "neon-data-api",
                    postgresConnected = true,
                    storageBucketReady = false, // Catalog success does not prove storage availability.
                    apiVersion = "v1"
                )
            )
            is AppResult.Error -> AppResult.Error(AppError.GeneralFailure(
                reasonAr = "تعذر الوصول إلى Neon Data API.",
                reasonEn = "Neon Data API health check failed."
            ))
        }

    override suspend fun fetchCatalog(locale: String): AppResult<NeonCatalogResponse> = secure {
        service.publicCatalog(if (locale == "en") "en" else "ar")
    }

    override suspend fun fetchPackManifest(packId: String): AppResult<ContentPackManifest> =
        secure { service.publicPackManifest(packId) }

    override suspend fun fetchAssetAccess(assetId: String): AppResult<NeonAssetAccessResponse> =
        secure { service.publicAssetAccess(assetId) }

    private suspend fun <T> secure(block: suspend () -> T): AppResult<T> {
        if (!IntegrityVerifier.isSecureHttpsEndpoint(baseUrl)) {
            return AppResult.Error(AppError.GeneralFailure(
                reasonAr = "عنوان خدمة Neon يجب أن يستخدم HTTPS.",
                reasonEn = "The Neon API URL must use HTTPS."
            ))
        }
        return try {
            AppResult.Success(block())
        } catch (e: Exception) {
            AppResult.Error(AppError.GeneralFailure(
                reasonAr = "تعذر جلب المعلومات السحابية.",
                reasonEn = "Could not retrieve published cloud content."
            ))
        }
    }
}
