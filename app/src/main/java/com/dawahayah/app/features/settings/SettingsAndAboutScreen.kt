package com.dawahayah.app.features.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dawahayah.app.core.storage.AppLanguage
import com.dawahayah.app.core.storage.AppThemeMode
import com.dawahayah.app.ui.components.UnboxedMetadataLine

@Composable
fun SettingsAndAboutScreen(
    state: MainUiState,
    onLanguageChange: (AppLanguage) -> Unit,
    onThemeModeChange: (AppThemeMode) -> Unit,
    onFontScaleChange: (Float) -> Unit,
    onWifiOnlyChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val isAr = state.settings.language == AppLanguage.ARABIC

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isAr) "الإعدادات، المصادر، وحول التطبيق" else "Settings, Sources & About Application",
                style = MaterialTheme.typography.headlineMedium
            )
        }

        // Language, Theme & Accessibility Settings
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = if (isAr) "اللغة والاتجاه والوصول (DataStore Persistence)" else "Language, Direction & Accessibility",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (isAr) {
                            Button(onClick = { onLanguageChange(AppLanguage.ARABIC) }) {
                                Text("العربية (RTL افتراضي)")
                            }
                            OutlinedButton(onClick = { onLanguageChange(AppLanguage.ENGLISH) }) {
                                Text("English (LTR)")
                            }
                        } else {
                            OutlinedButton(onClick = { onLanguageChange(AppLanguage.ARABIC) }) {
                                Text("العربية (RTL)")
                            }
                            Button(onClick = { onLanguageChange(AppLanguage.ENGLISH) }) {
                                Text("English (LTR Active)")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (isAr) "الوضع اللوني (المظهر)" else "Color Theme Mode",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onThemeModeChange(AppThemeMode.LIGHT) }) {
                            Text(if (isAr) "فاتح (Ivory)" else "Light")
                        }
                        OutlinedButton(onClick = { onThemeModeChange(AppThemeMode.DARK) }) {
                            Text(if (isAr) "داكن (Navy)" else "Dark")
                        }
                        OutlinedButton(onClick = { onThemeModeChange(AppThemeMode.SYSTEM) }) {
                            Text(if (isAr) "تلقائي" else "System")
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = if (isAr) {
                            "تكبير الخط للقراءة المريحة: ${(state.settings.fontScale * 100).toInt()}%"
                        } else {
                            "Font Scale for Accessibility: ${(state.settings.fontScale * 100).toInt()}%"
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                    Slider(
                        value = state.settings.fontScale,
                        onValueChange = onFontScaleChange,
                        valueRange = 0.85f..1.35f,
                        steps = 4
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isAr) "تنزيل الحزم عبر شبكة Wi-Fi فقط" else "Download Packs on Wi-Fi Only",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Switch(
                            checked = state.settings.wifiOnlyDownloads,
                            onCheckedChange = onWifiOnlyChange
                        )
                    }
                }
            }
        }

        // Official Attribution Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "دواء وحياة — Dawa & Hayah",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "الشعار: علم الدواء لفهم الحياة",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "من تطوير وبرمجة الدكتور الصيدلاني معتز العلقمي — اليمن، تعز — 2026 م.",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "المرحلة الأولى (Prompt 01 of 05) · Android minSdk=26 · Room SQLite + Neon Contract · إصدار 22 أكتوبر 2026",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                    )
                }
            }
        }

        // Verified Medical Sources & Provenance Registry
        item {
            Text(
                text = if (isAr) "سجل المصادر العلمية والترخيص (Provenance Registry)" else "Scientific Sources & Licensing Registry",
                style = MaterialTheme.typography.titleLarge
            )
        }

        items(state.sources, key = { it.id }) { src ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = src.title,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = src.publisher,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    UnboxedMetadataLine(
                        items = listOfNotNull(
                            src.license,
                            src.accessedAt?.let { "تاريخ الاسترجاع: $it" },
                            src.url
                        )
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}
