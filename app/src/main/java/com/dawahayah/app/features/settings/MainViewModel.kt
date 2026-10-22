package com.dawahayah.app.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dawahayah.app.AppContainer
import com.dawahayah.app.core.contentpacks.Anatomy3DAsset
import com.dawahayah.app.core.contentpacks.Stage01AnatomyAssetPlayer
import com.dawahayah.app.core.errors.AppResult
import com.dawahayah.app.core.network.NetworkStatus
import com.dawahayah.app.core.storage.AppLanguage
import com.dawahayah.app.core.storage.AppThemeMode
import com.dawahayah.app.core.storage.UserSettings
import com.dawahayah.app.data.remote.CloudBackendState
import com.dawahayah.app.domain.model.ActiveIngredient
import com.dawahayah.app.domain.model.BodySystem
import com.dawahayah.app.domain.model.Bookmark
import com.dawahayah.app.domain.model.Disease
import com.dawahayah.app.domain.model.LocalContent
import com.dawahayah.app.domain.model.MedicalSource
import com.dawahayah.app.domain.model.Organ
import com.dawahayah.app.domain.model.PackRecord
import com.dawahayah.app.domain.model.ReadingProgress
import com.dawahayah.app.domain.model.SearchHit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MainUiState(
    val isSeedInitializing: Boolean = true,
    val settings: UserSettings = UserSettings(),
    val networkStatus: NetworkStatus = NetworkStatus(isConnected = false, isUnmeteredWifi = false),
    val cloudBackendState: CloudBackendState = CloudBackendState.DisabledInStage01,
    val systems: List<BodySystem> = emptyList(),
    val organs: List<Organ> = emptyList(),
    val diseases: List<Disease> = emptyList(),
    val activeIngredients: List<ActiveIngredient> = emptyList(),
    val sources: List<MedicalSource> = emptyList(),
    val installedLessons: List<LocalContent> = emptyList(),
    val packs: List<PackRecord> = emptyList(),
    val bookmarks: List<Bookmark> = emptyList(),
    val readingProgress: List<ReadingProgress> = emptyList(),
    val anatomy3DContracts: List<Anatomy3DAsset> = Stage01AnatomyAssetPlayer.STAGE_01_REGISTERED_3D_CONTRACTS,
    val searchQuery: String = "",
    val searchResults: List<SearchHit> = emptyList(),
    val transientBannerMessage: String? = null
)

class MainViewModel(
    private val container: AppContainer
) : ViewModel() {

    private val _isSeedInitializing = MutableStateFlow(true)
    private val _searchQuery = MutableStateFlow("")
    private val _searchResults = MutableStateFlow<List<SearchHit>>(emptyList())
    private val _transientBannerMessage = MutableStateFlow<String?>(null)

    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    val searchResults: StateFlow<List<SearchHit>> = _searchResults.asStateFlow()

    init {
        viewModelScope.launch {
            container.seedInitializer.ensureSeededIfEmpty()
            _isSeedInitializing.value = false
        }
    }

    val uiState: StateFlow<MainUiState> = combine(
        _isSeedInitializing,
        container.preferencesDataStore.settingsFlow,
        container.connectivityObserver.statusFlow,
        container.knowledgeRepository.bodySystemsFlow,
        container.knowledgeRepository.organsFlow
    ) { isSeeding, settings, net, systems, organs ->
        MainUiState(
            isSeedInitializing = isSeeding,
            settings = settings,
            networkStatus = net,
            cloudBackendState = container.knowledgeRepository.cloudBackendState,
            systems = systems,
            organs = organs
        )
    }.combine(
        combine(
            container.knowledgeRepository.diseasesFlow,
            container.knowledgeRepository.activeIngredientsFlow,
            container.knowledgeRepository.medicalSourcesFlow,
            container.knowledgeRepository.installedLessonsFlow,
            container.knowledgeRepository.packRecordsFlow
        ) { diseases, ingredients, sources, lessons, packs ->
             Quintuple(diseases, ingredients, sources, lessons, packs)
        }
    ) { partial, q ->
        partial.copy(
            diseases = q.first,
            activeIngredients = q.second,
            sources = q.third,
            installedLessons = q.fourth,
            packs = q.fifth
        )
    }.combine(
        combine(
            container.knowledgeRepository.bookmarksFlow,
            container.knowledgeRepository.readingProgressFlow,
            _searchQuery,
            _searchResults,
            _transientBannerMessage
        ) { bookmarks, progress, query, hits, banner ->
            Quintuple(bookmarks, progress, query, hits, banner)
        }
    ) { partial, q2 ->
        partial.copy(
            bookmarks = q2.first,
            readingProgress = q2.second,
            searchQuery = q2.third,
            searchResults = q2.fourth,
            transientBannerMessage = q2.fifth
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MainUiState()
    )

    fun updateSearchQuery(newQuery: String) {
        _searchQuery.value = newQuery
        viewModelScope.launch {
            _searchResults.value = container.knowledgeRepository.searchLocalKnowledge(newQuery)
        }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            container.preferencesDataStore.setLanguage(language)
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch {
            container.preferencesDataStore.setThemeMode(mode)
        }
    }

    fun setFontScale(scale: Float) {
        viewModelScope.launch {
            container.preferencesDataStore.setFontScale(scale)
        }
    }

    fun setWifiOnlyDownloads(wifiOnly: Boolean) {
        viewModelScope.launch {
            container.preferencesDataStore.setWifiOnlyDownloads(wifiOnly)
        }
    }

    fun toggleBookmark(
        itemId: String,
        itemType: String,
        titleAr: String,
        titleEn: String,
        subtitle: String,
        currentlyBookmarked: Boolean
    ) {
        viewModelScope.launch {
            container.knowledgeRepository.toggleBookmark(
                itemId = itemId,
                itemType = itemType,
                titleAr = titleAr,
                titleEn = titleEn,
                subtitle = subtitle,
                currentlyBookmarked = currentlyBookmarked
            )
        }
    }

    fun recordReading(
        itemId: String,
        itemType: String,
        titleAr: String,
        titleEn: String
    ) {
        viewModelScope.launch {
            container.knowledgeRepository.recordReadingProgress(
                itemId = itemId,
                itemType = itemType,
                titleAr = titleAr,
                titleEn = titleEn
            )
        }
    }

    fun installContentPack(packId: String, simulateCorruptedHash: Boolean = false) {
        viewModelScope.launch {
            when (val res = container.knowledgeRepository.installPack(packId, simulateCorruptedHash)) {
                is AppResult.Success -> {
                    _transientBannerMessage.value =
                        "تم التحقق من بصمة SHA-256 وتثبيت الحزمة (${res.data.titleAr}) في التخزين الدائم بنجاح."
                }
                is AppResult.Error -> {
                    _transientBannerMessage.value = res.error.messageAr
                }
            }
        }
    }

    fun deleteContentPack(packId: String) {
        viewModelScope.launch {
            when (val res = container.knowledgeRepository.deletePack(packId)) {
                is AppResult.Success -> {
                    _transientBannerMessage.value =
                        "تم حذف ملفات الحزمة من التخزين الدائم مع الحفاظ الكامل على قاعدة المعرفة الأساسية."
                }
                is AppResult.Error -> {
                    _transientBannerMessage.value = res.error.messageAr
                }
            }
        }
    }

    fun dismissBanner() {
        _transientBannerMessage.value = null
    }

    private data class Quintuple<A, B, C, D, E>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D,
        val fifth: E
    )

    companion object {
        fun provideFactory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return MainViewModel(container) as T
                }
            }
    }
}
