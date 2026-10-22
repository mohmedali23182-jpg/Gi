package com.dawahayah.app.features.search

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dawahayah.app.core.storage.AppLanguage
import com.dawahayah.app.features.settings.MainUiState
import com.dawahayah.app.ui.components.UnboxedMetadataLine

@Composable
fun GlobalSearchScreen(
    state: MainUiState,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isAr = state.settings.language == AppLanguage.ARABIC

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isAr) "البحث المحلي المطبع (العربية · الإنجليزية · اللاتينية)" else "Normalized Local Search (Arabic · English · Latin)",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (isAr) {
                    "يدعم تطبيع الهمزات (أ/إ/آ ← ا) والتاء المربوطة (ة ← ه) والمصطلحات المرادفة دون المساس بالنص العلمي الأصلي."
                } else {
                    "Supports Arabic hamza and taa marbuta normalization plus Latin anatomical synonyms completely offline."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onQueryChange,
                label = {
                    Text(
                        if (isAr) "ابحث عن عضو، جهاز، مرض، أو مادة فعالة (مثال: القلب، أوميبرازول، Cor)"
                        else "Search organ, system, disease, or active ingredient..."
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onQueryChange("القلب") }) {
                    Text("القلب / Cor")
                }
                OutlinedButton(onClick = { onQueryChange("اوميبرازول") }) {
                    Text("اوميبرازول (بدون همزة)")
                }
                OutlinedButton(onClick = { onQueryChange("معده") }) {
                    Text("معده (هاء بدل ة)")
                }
            }
        }

        if (state.searchQuery.isNotBlank() && state.searchResults.isEmpty()) {
            item {
                Text(
                    text = if (isAr) "لا توجد نتائج مطابقة في قاعدة المعرفة المحلية." else "No matching records in local knowledge base.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(state.searchResults, key = { "${it.category}_${it.itemId}" }) { hit ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isAr) hit.titleAr else hit.titleEn,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    UnboxedMetadataLine(
                        items = listOfNotNull(
                            hit.category,
                            if (isAr) hit.titleEn else hit.titleAr,
                            hit.latinSubtitle
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isAr) hit.snippetAr else hit.snippetEn,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}
