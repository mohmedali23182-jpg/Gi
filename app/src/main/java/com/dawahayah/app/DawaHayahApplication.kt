package com.dawahayah.app

import android.app.Application
import android.content.Context
import com.dawahayah.app.core.contentpacks.ContentPackManager
import com.dawahayah.app.core.contentpacks.Stage01AnatomyAssetPlayer
import com.dawahayah.app.core.network.NetworkConnectivityObserver
import com.dawahayah.app.core.storage.UserPreferencesDataStore
import com.dawahayah.app.data.local.DawaHayahDatabase
import com.dawahayah.app.data.local.SeedDatabaseInitializer
import com.dawahayah.app.data.remote.DisabledNeonDataSource
import com.dawahayah.app.data.remote.NeonApiService
import com.dawahayah.app.data.remote.RemoteContentDataSource
import com.dawahayah.app.data.remote.RetrofitNeonDataSource
import com.dawahayah.app.data.repository.LocalFirstKnowledgeRepository
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Lightweight, testable Dependency Container for the Single-Module Android Architecture.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    val database: DawaHayahDatabase by lazy {
        DawaHayahDatabase.getInstance(appContext)
    }

    val seedInitializer: SeedDatabaseInitializer by lazy {
        SeedDatabaseInitializer(appContext, database.knowledgeDao(), json)
    }

    val preferencesDataStore: UserPreferencesDataStore by lazy {
        UserPreferencesDataStore(appContext)
    }

    val connectivityObserver: NetworkConnectivityObserver by lazy {
        NetworkConnectivityObserver(appContext)
    }

    val remoteDataSource: RemoteContentDataSource by lazy {
        val configuredUrl = BuildConfig.NEON_PUBLIC_API_BASE_URL.trim()
        if (configuredUrl.startsWith("https://", ignoreCase = true)) {
            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build()
            val retrofit = Retrofit.Builder()
                .baseUrl(if (configuredUrl.endsWith("/")) configuredUrl else "$configuredUrl/")
                .client(client)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
            RetrofitNeonDataSource(configuredUrl, retrofit.create(NeonApiService::class.java))
        } else {
            DisabledNeonDataSource()
        }
    }

    val contentPackManager: ContentPackManager by lazy {
        ContentPackManager(
            context = appContext,
            dao = database.knowledgeDao(),
            remoteDataSource = remoteDataSource,
            json = json
        )
    }

    val knowledgeRepository: LocalFirstKnowledgeRepository by lazy {
        LocalFirstKnowledgeRepository(
            dao = database.knowledgeDao(),
            contentPackManager = contentPackManager,
            remoteDataSource = remoteDataSource
        )
    }

    val anatomyAssetPlayer: Stage01AnatomyAssetPlayer by lazy {
        Stage01AnatomyAssetPlayer()
    }
}

class DawaHayahApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
