package com.dawahayah.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dawahayah.app.core.storage.AppLanguage
import com.dawahayah.app.features.body.AnatomyScenesScreen
import com.dawahayah.app.features.body.BodySystemsScreen
import com.dawahayah.app.features.diseases.DiseasesScreen
import com.dawahayah.app.features.downloads.DownloadsScreen
import com.dawahayah.app.features.home.HomeScreen
import com.dawahayah.app.features.library.MyLibraryScreen
import com.dawahayah.app.features.medicines.MedicinesScreen
import com.dawahayah.app.features.search.GlobalSearchScreen
import com.dawahayah.app.features.settings.MainViewModel
import com.dawahayah.app.features.settings.SettingsAndAboutScreen
import com.dawahayah.app.ui.navigation.AuxiliaryRoutes
import com.dawahayah.app.ui.navigation.PrimaryBottomTab
import com.dawahayah.app.ui.theme.DawaHayahTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as DawaHayahApplication

        setContent {
            val mainViewModel: MainViewModel = viewModel(
                factory = MainViewModel.provideFactory(app.container)
            )
            val uiState by mainViewModel.uiState.collectAsState()

            DawaHayahTheme(
                language = uiState.settings.language,
                themeMode = uiState.settings.themeMode,
                fontScale = uiState.settings.fontScale
            ) {
                DawaHayahMainScaffold(viewModel = mainViewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DawaHayahMainScaffold(
    viewModel: MainViewModel
) {
    val state by viewModel.uiState.collectAsState()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: PrimaryBottomTab.HOME.route
    val isAr = state.settings.language == AppLanguage.ARABIC
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.transientBannerMessage) {
        val msg = state.transientBannerMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissBanner()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isAr) "دواء وحياة · علم الدواء لفهم الحياة" else "Dawa & Hayah",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    IconButton(onClick = { navController.navigate(AuxiliaryRoutes.GLOBAL_SEARCH) }) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = if (isAr) "البحث العام" else "Global Search"
                        )
                    }
                    IconButton(onClick = { navController.navigate(AuxiliaryRoutes.PACK_DOWNLOADS) }) {
                        Icon(
                            imageVector = Icons.Outlined.CloudDownload,
                            contentDescription = if (isAr) "التنزيلات والحزم" else "Downloads"
                        )
                    }
                    IconButton(onClick = { navController.navigate(AuxiliaryRoutes.SETTINGS_AND_ABOUT) }) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = if (isAr) "الإعدادات وحول التطبيق" else "Settings & About"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            NavigationBar {
                PrimaryBottomTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(PrimaryBottomTab.HOME.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = if (isAr) tab.labelAr else tab.labelEn
                            )
                        },
                        label = {
                            Text(if (isAr) tab.labelAr else tab.labelEn)
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = PrimaryBottomTab.HOME.route
            ) {
                composable(PrimaryBottomTab.HOME.route) {
                    HomeScreen(
                        state = state,
                        onNavigateTab = { tab -> navController.navigate(tab.route) },
                        onNavigateAuxiliary = { route -> navController.navigate(route) }
                    )
                }
                composable(PrimaryBottomTab.BODY_SYSTEMS.route) {
                    BodySystemsScreen(
                        state = state,
                        onBookmarkToggle = viewModel::toggleBookmark,
                        onRecordReading = viewModel::recordReading,
                        onInstallPack = { packId -> viewModel.installContentPack(packId) },
                        onOpenAnatomyScenes = { navController.navigate(AuxiliaryRoutes.ANATOMY_SCENES) }
                    )
                }
                composable(PrimaryBottomTab.DISEASES.route) {
                    DiseasesScreen(
                        state = state,
                        onBookmarkToggle = viewModel::toggleBookmark,
                        onRecordReading = viewModel::recordReading
                    )
                }
                composable(PrimaryBottomTab.MEDICINES.route) {
                    MedicinesScreen(
                        state = state,
                        onBookmarkToggle = viewModel::toggleBookmark,
                        onRecordReading = viewModel::recordReading
                    )
                }
                composable(PrimaryBottomTab.MY_LIBRARY.route) {
                    MyLibraryScreen(
                        state = state,
                        onRemoveBookmark = { id, type, ar, en, sub ->
                            viewModel.toggleBookmark(id, type, ar, en, sub, true)
                        },
                        onNavigateAuxiliary = { route -> navController.navigate(route) }
                    )
                }
                composable(AuxiliaryRoutes.GLOBAL_SEARCH) {
                    GlobalSearchScreen(
                        state = state,
                        onQueryChange = viewModel::updateSearchQuery
                    )
                }
                composable(AuxiliaryRoutes.PACK_DOWNLOADS) {
                    DownloadsScreen(
                        state = state,
                        onInstallPack = viewModel::installContentPack,
                        onDeletePack = viewModel::deleteContentPack,
                        onRefreshCloudCatalog = viewModel::browseCloudCatalog
                    )
                }
                composable(AuxiliaryRoutes.ANATOMY_SCENES) {
                    AnatomyScenesScreen(state = state)
                }
                composable(AuxiliaryRoutes.SOURCES_AND_LICENSES) {
                    SettingsAndAboutScreen(
                        state = state,
                        onLanguageChange = viewModel::setLanguage,
                        onThemeModeChange = viewModel::setThemeMode,
                        onFontScaleChange = viewModel::setFontScale,
                        onWifiOnlyChange = viewModel::setWifiOnlyDownloads
                    )
                }
                composable(AuxiliaryRoutes.SETTINGS_AND_ABOUT) {
                    SettingsAndAboutScreen(
                        state = state,
                        onLanguageChange = viewModel::setLanguage,
                        onThemeModeChange = viewModel::setThemeMode,
                        onFontScaleChange = viewModel::setFontScale,
                        onWifiOnlyChange = viewModel::setWifiOnlyDownloads
                    )
                }
            }
        }
    }
}
