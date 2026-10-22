package com.dawahayah.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Biotech
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.ui.graphics.vector.ImageVector

enum class PrimaryBottomTab(
    val route: String,
    val labelAr: String,
    val labelEn: String,
    val icon: ImageVector
) {
    HOME("home", "الرئيسية", "Home", Icons.Outlined.Home),
    BODY_SYSTEMS("body_systems", "جسم الإنسان", "Body Systems", Icons.Outlined.FavoriteBorder),
    DISEASES("diseases", "الأمراض", "Diseases", Icons.Outlined.Biotech),
    MEDICINES("medicines", "الأدوية", "Medicines", Icons.Outlined.Medication),
    MY_LIBRARY("my_library", "مكتبتي", "My Library", Icons.Outlined.AutoStories)
}

object AuxiliaryRoutes {
    const val GLOBAL_SEARCH = "global_search"
    const val PACK_DOWNLOADS = "pack_downloads"
    const val ANATOMY_SCENES = "anatomy_scenes"
    const val SOURCES_AND_LICENSES = "sources_and_licenses"
    const val SETTINGS_AND_ABOUT = "settings_and_about"
}
