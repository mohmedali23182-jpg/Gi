package com.dawahayah.app.features.medicines

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dawahayah.app.core.storage.AppLanguage
import com.dawahayah.app.features.settings.MainUiState
import com.dawahayah.app.ui.components.KnowledgeTopicCard

@Composable
fun MedicinesScreen(
    state: MainUiState,
    onBookmarkToggle: (String, String, String, String, String, Boolean) -> Unit,
    onRecordReading: (String, String, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isAr = state.settings.language == AppLanguage.ARABIC
    val bookmarkedIds = state.bookmarks.map { it.itemId }.toSet()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isAr) "المواد الفعالة والتصنيف الدوائي" else "Active Ingredients & Pharmacological Classification",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isAr) {
                    "تأسيس علمي آمن يشرح التصنيف الدوائي وآليات العمل الجزيئية والمستقبلات دون تقديم جرعات أو بروتوكولات علاجية غير مراجعة."
                } else {
                    "Safe educational foundation covering pharmacological classes and molecular mechanisms without unreviewed clinical dosing."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(state.activeIngredients, key = { it.id }) { ingredient ->
            val isBookmarked = bookmarkedIds.contains(ingredient.id)
            KnowledgeTopicCard(
                title = if (isAr) ingredient.nameAr else ingredient.nameEn,
                secondaryTitle = if (isAr) ingredient.nameEn else ingredient.nameAr,
                latinTerm = ingredient.className,
                bodyText = if (isAr) ingredient.generalDescriptionAr else ingredient.generalDescriptionEn,
                safetyNotice = ingredient.safetyNote,
                metadataItems = listOf(
                    "حالة المراجعة: ${ingredient.reviewStatus}",
                    "المصادر: ${ingredient.citationIds.joinToString(", ")}"
                ),
                isBookmarked = isBookmarked,
                onBookmarkToggle = {
                    onBookmarkToggle(
                        ingredient.id,
                        "MEDICINE",
                        ingredient.nameAr,
                        ingredient.nameEn,
                        ingredient.className,
                        isBookmarked
                    )
                },
                onCardClick = {
                    onRecordReading(ingredient.id, "MEDICINE", ingredient.nameAr, ingredient.nameEn)
                }
            )
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}
