package com.dawahayah.app.features.medicines

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.foundation.text.selection.SelectionContainer
import com.dawahayah.app.core.locale.ArabicSearchNormalizer
import com.dawahayah.app.domain.model.ActiveIngredient
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
    var selectedIngredient by rememberSaveable { mutableStateOf<String?>(null) }
    var searchTerm by rememberSaveable { mutableStateOf("") }
    val query = ArabicSearchNormalizer.normalize(searchTerm)
    val filtered = state.activeIngredients.filter { item ->
        query.isBlank() || ArabicSearchNormalizer.buildIndexableTokens(item.nameAr, item.nameEn, item.className).contains(query)
    }

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

        item {
            OutlinedTextField(
                value = searchTerm,
                onValueChange = { searchTerm = it },
                label = { Text(if (isAr) "بحث بالاسم العلمي أو الفئة" else "Search generic name or class") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        items(filtered, key = { it.id }) { ingredient ->
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
                    selectedIngredient = ingredient.id
                }
            )
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
    val detail = state.activeIngredients.firstOrNull { it.id == selectedIngredient }
    if (detail != null) {
        MedicineDetailDialog(
            item = detail,
            isAr = isAr,
            onDismiss = { selectedIngredient = null },
            sources = state.sources.associateBy { it.id }
        )
    }

}


/**
 * Descriptive information only: dosing/interaction assessment remains gated
 * until a qualified scientific reviewer verifies the actual source and region.
 */
@Composable
private fun MedicineDetailDialog(
    item: ActiveIngredient,
    isAr: Boolean,
    onDismiss: () -> Unit,
    sources: Map<String, com.dawahayah.app.domain.model.MedicalSource>
) {
    val clipboard = LocalClipboardManager.current
    val uri = LocalUriHandler.current
    var tab by rememberSaveable(item.id) { mutableStateOf(0) }
    val names = if (isAr) listOf("التعريف", "السلامة", "المصادر", "الجرعات والأصناف")
                else listOf("Overview", "Safety", "References", "Doses & Brands")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isAr) item.nameAr else item.nameEn) },
        text = {
            Column(
                modifier = Modifier.height(380.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                names.forEachIndexed { i, label ->
                    TextButton(onClick = { tab = i }) {
                        Text((if (tab == i) "● " else "○ ") + label)
                    }
                }
                when (tab) {
                    0 -> {
                        Text(item.className, style = MaterialTheme.typography.titleMedium)
                        SelectionContainer {
                            Text(if (isAr) item.generalDescriptionAr else item.generalDescriptionEn)
                        }
                    }
                    1 -> SelectionContainer { Text(item.safetyNote) }
                    2 -> {
                        item.citationIds.forEach { id ->
                            val src = sources[id]
                            Text(src?.title ?: id)
                            if (src != null && src.url.startsWith("https://")) {
                                TextButton(onClick = { uri.openUri(src.url) }) {
                                    Text(if (isAr) "فتح المرجع الأصلي" else "Open source")
                                }
                            }
                        }
                    }
                    else -> Text(
                        if (isAr) "الجرعات الفردية والتداخلات والأسماء التجارية المحلية قيد التحقق العلمي والتنظيمي، ولا تُعرض كتوصيات قبل اعتماد المصدر والمراجع المختص."
                        else "Patient-specific doses, interactions, and local brands require source verification and professional review before publication."
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = {
                clipboard.setText(AnnotatedString(
                    item.nameAr + " / " + item.nameEn + "\n" +
                    item.className + "\n" + item.generalDescriptionAr + "\n" +
                    item.generalDescriptionEn + "\n" + item.citationIds.joinToString(", ")
                ))
            }) { Text(if (isAr) "نسخ المعلومات" else "Copy") }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(if (isAr) "إغلاق" else "Close") }
        }
    )
}
