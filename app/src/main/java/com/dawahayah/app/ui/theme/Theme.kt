package com.dawahayah.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import com.dawahayah.app.core.storage.AppLanguage
import com.dawahayah.app.core.storage.AppThemeMode

private val LightColors = lightColorScheme(
    primary = MedicalDeepTeal,
    onPrimary = MedicalIvory,
    primaryContainer = MedicalTurquoise,
    onPrimaryContainer = LightSurfacePure,
    secondary = AcademicGold,
    onSecondary = AcademicNavy,
    secondaryContainer = LightSurfaceMuted,
    onSecondaryContainer = MedicalDeepTeal,
    tertiary = AcademicNavy,
    background = MedicalIvory,
    onBackground = LightTextPrimary,
    surface = LightSurfacePure,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceMuted,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorderHairline
)

private val DarkColors = darkColorScheme(
    primary = MedicalTurquoise,
    onPrimary = DarkCanvasNavy,
    primaryContainer = MedicalDeepTeal,
    onPrimaryContainer = MedicalIvory,
    secondary = AcademicGold,
    onSecondary = DarkCanvasNavy,
    secondaryContainer = DarkSurfaceCard,
    onSecondaryContainer = AcademicGold,
    tertiary = AcademicGold,
    background = DarkCanvasNavy,
    onBackground = DarkTextPrimary,
    surface = DarkSurfaceElevated,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceCard,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorderHairline
)

val DawaHayahTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 36.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 21.sp,
        lineHeight = 30.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 25.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )
)

@Composable
fun DawaHayahTheme(
    language: AppLanguage = AppLanguage.ARABIC,
    themeMode: AppThemeMode = AppThemeMode.LIGHT,
    fontScale: Float = 1.0f,
    content: @Composable () -> Unit
) {
    val useDark = when (themeMode) {
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colorScheme = if (useDark) DarkColors else LightColors
    val layoutDirection = if (language.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    val currentDensity = LocalDensity.current

    CompositionLocalProvider(
        LocalLayoutDirection provides layoutDirection,
        LocalDensity provides Density(
            density = currentDensity.density,
            fontScale = currentDensity.fontScale * fontScale.coerceIn(0.85f, 1.40f)
        )
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = DawaHayahTypography,
            content = content
        )
    }
}
