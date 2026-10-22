package com.dawahayah.app.core.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "dawa_hayah_settings")

enum class AppLanguage(val code: String, val isRtl: Boolean) {
    ARABIC("ar", true),
    ENGLISH("en", false)
}

enum class AppThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

data class UserSettings(
    val language: AppLanguage = AppLanguage.ARABIC,
    val themeMode: AppThemeMode = AppThemeMode.LIGHT,
    val fontScale: Float = 1.0f,
    val wifiOnlyDownloads: Boolean = true
)

class UserPreferencesDataStore(private val context: Context) {

    private object Keys {
        val LANGUAGE = stringPreferencesKey("app_language")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val FONT_SCALE = floatPreferencesKey("font_scale")
        val WIFI_ONLY = booleanPreferencesKey("wifi_only_downloads")
    }

    val settingsFlow: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        val lang = when (prefs[Keys.LANGUAGE]) {
            "en" -> AppLanguage.ENGLISH
            else -> AppLanguage.ARABIC
        }
        val theme = when (prefs[Keys.THEME_MODE]) {
            "DARK" -> AppThemeMode.DARK
            "SYSTEM" -> AppThemeMode.SYSTEM
            else -> AppThemeMode.LIGHT
        }
        val scale = (prefs[Keys.FONT_SCALE] ?: 1.0f).coerceIn(0.85f, 1.40f)
        val wifiOnly = prefs[Keys.WIFI_ONLY] ?: true
        UserSettings(
            language = lang,
            themeMode = theme,
            fontScale = scale,
            wifiOnlyDownloads = wifiOnly
        )
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { it[Keys.LANGUAGE] = language.code }
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setFontScale(scale: Float) {
        context.dataStore.edit { it[Keys.FONT_SCALE] = scale.coerceIn(0.85f, 1.40f) }
    }

    suspend fun setWifiOnlyDownloads(wifiOnly: Boolean) {
        context.dataStore.edit { it[Keys.WIFI_ONLY] = wifiOnly }
    }
}
