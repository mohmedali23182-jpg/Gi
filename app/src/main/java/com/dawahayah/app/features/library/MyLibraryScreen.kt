package com.dawahayah.app.features.library

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dawahayah.app.core.storage.AppLanguage
import com.dawahayah.app.features.settings.MainUiState
import com.dawahayah.app.ui.components.UnboxedMetadataLine
import com.dawahayah.app.ui.navigation.AuxiliaryRoutes

@Composable
fun MyLibraryScreen(
    state: MainUiState,
    onRemoveBookmark: (String, String, String, String, String) -> Unit,
    onNavigateAuxiliary: (String) -> Unit,
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
                text = if (isAr) "مكتبتي المحلية والمحفوظات" else "My Local Library & Saved Items",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onNavigateAuxiliary(AuxiliaryRoutes.PACK_DOWNLOADS) }) {
                    Text(if (isAr) "إدارة الحزم غير المتصلة" else "Manage Offline Packs")
                }
                OutlinedButton(onClick = { onNavigateAuxiliary(AuxiliaryRoutes.SOURCES_AND_LICENSES) }) {
                    Text(if (isAr) "سجل المصادر والتراخيص" else "Sources & Licenses")
                }
            }
        }

        // Installed Pack Lessons Section
        item {
            Text(
                text = if (isAr) {
                    "المقالات المثبتة من الحزم الدائمة (${state.installedLessons.size})"
                } else {
                    "Lessons Installed from Persistent Packs (${state.installedLessons.size})"
                },
                style = MaterialTheme.typography.titleLarge
            )
        }

        if (state.installedLessons.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (isAr) {
                                "لا توجد دروس إضافية مثبتة بعد. افتح شاشة التنزيلات لتثبيت «حزمة فسيولوجيا وتشريح نظام التوصيل القلبي (Cor)» والتحقق من بصمتها SHA-256."
                            } else {
                                "No pack lessons installed yet. Open Downloads to install the Cardiac Conduction System pack and verify its SHA-256 checksum."
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        } else {
            items(state.installedLessons, key = { it.id }) { lesson ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = if (isAr) lesson.titleAr else lesson.titleEn,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isAr) lesson.bodyAr else lesson.bodyEn,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        UnboxedMetadataLine(
                            items = listOf(
                                "Pack Origin: ${lesson.origin}",
                                "Revision: v${lesson.revision}",
                                lesson.reviewStatus
                            )
                        )
                    }
                }
            }
        }

        // Bookmarks Section
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (isAr) "المفضلة المحفوظة (${state.bookmarks.size})" else "Saved Bookmarks (${state.bookmarks.size})",
                style = MaterialTheme.typography.titleLarge
            )
        }

        if (state.bookmarks.isEmpty()) {
            item {
                Text(
                    text = if (isAr) "اضغط على أيقونة الحفظ في أي جهاز أو عضو أو مرض أو مادة فعالة لإضافتها هنا." else "Tap the bookmark icon on any topic card to save it here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(state.bookmarks, key = { it.itemId }) { b ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (isAr) b.titleAr else b.titleEn,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        UnboxedMetadataLine(items = listOf(b.itemType, b.subtitle))
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                onRemoveBookmark(b.itemId, b.itemType, b.titleAr, b.titleEn, b.subtitle)
                            }
                        ) {
                            Text(if (isAr) "إزالة من المفضلة" else "Remove Bookmark")
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}
