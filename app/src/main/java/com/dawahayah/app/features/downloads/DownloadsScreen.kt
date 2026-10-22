package com.dawahayah.app.features.downloads

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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dawahayah.app.core.storage.AppLanguage
import com.dawahayah.app.domain.model.PackInstallState
import com.dawahayah.app.features.settings.MainUiState
import com.dawahayah.app.ui.components.UnboxedMetadataLine

@Composable
fun DownloadsScreen(
    state: MainUiState,
    onInstallPack: (String, Boolean) -> Unit,
    onDeletePack: (String) -> Unit,
    onRefreshCloudCatalog: () -> Unit,
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
                text = if (isAr) "مدير الحزم غير المتصلة (ContentPackManager)" else "Offline Content Pack Manager",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (isAr) {
                    "يتم تنزيل الحزم إلى مجلد staging مؤقت، وفحص بصمة SHA-256 والحجم ومسارات الملفات، ثم تثبيتها ذرياً في التخزين الدائم للتطبيق (filesDir/content_packs)."
                } else {
                    "Packs are staged in a temporary directory, verified against SHA-256 checksums and path traversal rules, then atomically promoted to persistent storage."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            OutlinedButton(onClick = onRefreshCloudCatalog) {
                Text(if (isAr) "استعراض حزم Neon عبر الإنترنت (دون تنزيل)" else "Browse Neon packs online (no download)")
            }
        }

        items(state.packs, key = { it.id }) { pack ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = if (isAr) pack.titleAr else pack.titleEn,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    UnboxedMetadataLine(
                        items = listOf(
                            "ID: ${pack.id}",
                            "v${pack.version}",
                            "Scope: ${pack.scopeType}/${pack.scopeId}",
                            "Size: ${pack.sizeBytes} Bytes"
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    UnboxedMetadataLine(
                        items = listOf(
                            pack.license,
                            "الحالة: ${pack.installState.name}"
                        )
                    )

                    if (!pack.companionDependencyNotice.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = pack.companionDependencyNotice,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (!pack.lastErrorMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = pack.lastErrorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    if (pack.installState == PackInstallState.RUNNING || pack.downloadedBytes > 0L) {
                        Spacer(modifier = Modifier.height(10.dp))
                        val progress = if (pack.sizeBytes > 0) {
                            (pack.downloadedBytes.toFloat() / pack.sizeBytes.toFloat()).coerceIn(0f, 1f)
                        } else 0f
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${pack.downloadedBytes} / ${pack.sizeBytes} Bytes",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (pack.installState != PackInstallState.COMPLETED) {
                            Button(
                                onClick = { onInstallPack(pack.id, false) }
                            ) {
                                Text(
                                    if (pack.isBundledOfflineDemoAvailable) {
                                        if (isAr) "تثبيت الحزمة وفحص SHA-256" else "Install & Verify SHA-256"
                                    } else {
                                        if (isAr) "طلب من سحابة Neon" else "Fetch from Neon Cloud"
                                    }
                                )
                            }
                            if (pack.isBundledOfflineDemoAvailable) {
                                OutlinedButton(
                                    onClick = { onInstallPack(pack.id, true) }
                                ) {
                                    Text(
                                        if (isAr) "اختبار رفض بصمة تالفة" else "Test Corrupted Hash Reject"
                                    )
                                }
                            }
                        } else {
                            OutlinedButton(
                                onClick = { onDeletePack(pack.id) }
                            ) {
                                Text(
                                    if (isAr) "حذف الحزمة من التخزين الدائم" else "Delete Pack from Persistent Storage"
                                )
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}
