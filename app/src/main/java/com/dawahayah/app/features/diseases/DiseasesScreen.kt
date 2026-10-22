package com.dawahayah.app.features.diseases

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dawahayah.app.core.storage.AppLanguage
import com.dawahayah.app.domain.model.Disease
import com.dawahayah.app.features.settings.MainUiState
import com.dawahayah.app.ui.components.KnowledgeTopicCard
import com.dawahayah.app.ui.components.UnboxedMetadataLine

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiseasesScreen(
    state: MainUiState,
    onBookmarkToggle: (String, String, String, String, String, Boolean) -> Unit,
    onRecordReading: (String, String, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isAr = state.settings.language == AppLanguage.ARABIC
    var selectedSystemFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var expandedDiseaseId by rememberSaveable { mutableStateOf<String?>("dis-hypertension") }
    var activeSectionFilter by rememberSaveable { mutableStateOf("ALL") }
    val bookmarkedIds = state.bookmarks.map { it.itemId }.toSet()

    val filteredDiseases = if (selectedSystemFilter == null) {
        state.diseases
    } else {
        state.diseases.filter {
            it.primarySystemId == selectedSystemFilter || it.secondarySystemIds.contains(selectedSystemFilter)
        }
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
                text = if (isAr) {
                    "موسوعة الأمراض والفيزيولوجيا المرضية (${state.diseases.size} مرضاً موثقاً)"
                } else {
                    "Diseases & Pathophysiology Encyclopedia (${state.diseases.size} Diseases)"
                },
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isAr) {
                    "أقسام معيارية منظمة (أ إلى ي: التعريف، الانتشار، الفيزيولوجيا المرضية، العلامات التحذيرية Red Flags، التشخيص، التدبير غير الدوائي، الفئات الدوائية التعليمية، المضاعفات، والمصادر) مرتبطة بأجهزة الجسم وتصنيف WHO ICD-11."
                } else {
                    "Structured A–J clinical-educational monographs (Overview, Epidemiology, Pathophysiology, Red Flags, Diagnosis, Non-Pharmacological, Educational Drug Classes, Complications, and Provenance) linked to body systems and WHO ICD-11."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // System Filter Row
        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedSystemFilter == null,
                    onClick = { selectedSystemFilter = null },
                    label = { Text(if (isAr) "جميع الأمراض (${state.diseases.size})" else "All (${state.diseases.size})") }
                )
                state.systems.filter { sys ->
                    state.diseases.any { it.primarySystemId == sys.id || it.secondarySystemIds.contains(sys.id) }
                }.forEach { sys ->
                    FilterChip(
                        selected = selectedSystemFilter == sys.id,
                        onClick = {
                            selectedSystemFilter = if (selectedSystemFilter == sys.id) null else sys.id
                        },
                        label = { Text(if (isAr) sys.nameAr else sys.nameEn) }
                    )
                }
            }
        }

        items(filteredDiseases, key = { it.id }) { disease ->
            val isBookmarked = bookmarkedIds.contains(disease.id)
            val isExpanded = expandedDiseaseId == disease.id

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                KnowledgeTopicCard(
                    title = if (isAr) disease.nameAr else disease.nameEn,
                    secondaryTitle = if (isAr) disease.nameEn else disease.nameAr,
                    latinTerm = if (disease.icd11Code.isNotBlank()) "WHO ICD-11: ${disease.icd11Code}" else null,
                    bodyText = if (isAr) disease.introductionAr else disease.introductionEn,
                    metadataItems = listOf(
                        "System: ${disease.primarySystemId}",
                        "الأقسام: ${disease.sections.size} (A–J)",
                        "حالة المراجعة: ${disease.reviewStatus}"
                    ),
                    isBookmarked = isBookmarked,
                    onBookmarkToggle = {
                        onBookmarkToggle(
                            disease.id,
                            "DISEASE",
                            disease.nameAr,
                            disease.nameEn,
                            disease.primarySystemId,
                            isBookmarked
                        )
                    },
                    onCardClick = {
                        expandedDiseaseId = if (expandedDiseaseId == disease.id) null else disease.id
                        onRecordReading(disease.id, "DISEASE", disease.nameAr, disease.nameEn)
                    }
                )

                if (isExpanded && disease.sections.isNotEmpty()) {
                    DiseaseStructuredDetailCard(
                        disease = disease,
                        isAr = isAr,
                        activeSectionFilter = activeSectionFilter,
                        onSectionFilterChange = { activeSectionFilter = it }
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DiseaseStructuredDetailCard(
    disease: Disease,
    isAr: Boolean,
    activeSectionFilter: String,
    onSectionFilterChange: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (isAr) "الملف العلمي المهيكل (A–J): ${disease.nameAr}" else "Structured Monograph (A–J): ${disease.nameEn}",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )

            UnboxedMetadataLine(
                items = listOf(
                    "WHO ICD-11: ${disease.icd11Code}",
                    if (isAr) "تاريخ الاستيراد: ${disease.importedAt}" else "Imported: ${disease.importedAt}",
                    if (isAr) "المراجعة البشرية: ${disease.humanReviewedAt ?: "قيد المراجعة"}" else "Human Reviewed: ${disease.humanReviewedAt ?: "Pending"}"
                )
            )

            if (disease.relatedDrugClasses.isNotEmpty()) {
                Text(
                    text = if (isAr) {
                        "الفئات الدوائية المرتبطة (تمهيداً للمرحلة الثالثة): ${disease.relatedDrugClasses.joinToString(" · ")}"
                    } else {
                        "Linked Pharmacotherapy Classes (Stage 03 Bridge): ${disease.relatedDrugClasses.joinToString(" · ")}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            // Section Filter Chips
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = activeSectionFilter == "ALL",
                    onClick = { onSectionFilterChange("ALL") },
                    label = { Text(if (isAr) "كل الأقسام (A–J)" else "All Sections (A–J)") }
                )
                disease.sections.forEach { sec ->
                    val shortCode = sec.sectionCode.substringBefore("_")
                    FilterChip(
                        selected = activeSectionFilter == sec.sectionCode,
                        onClick = { onSectionFilterChange(sec.sectionCode) },
                        label = { Text(shortCode) }
                    )
                }
            }

            val visibleSections = if (activeSectionFilter == "ALL") {
                disease.sections
            } else {
                disease.sections.filter { it.sectionCode == activeSectionFilter }
            }

            visibleSections.forEach { sec ->
                HorizontalDivider()
                Text(
                    text = if (isAr) sec.titleAr else sec.titleEn,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                val tierLabel = when (sec.informationTier) {
                    "CLINICAL_GUIDELINE_REF" -> if (isAr) "تصنيف المعلومة: مرجع إرشادي سريري وعلامات تحذيرية" else "Tier: Clinical Guideline Reference & Red Flags"
                    "UNDER_REVIEW_SAFEGUARD" -> if (isAr) "تصنيف المعلومة: فئات دوائية تعليمية — الجرعات الفردية محجوبة وتستلزم إشراف الطبيب" else "Tier: Educational Drug Classes — Individual Dosing Omitted"
                    else -> if (isAr) "تصنيف المعلومة: شرح علمي وفسيولوجي مرضي عام" else "Tier: General Pathophysiological & Academic Explanation"
                }
                Text(
                    text = tierLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (isAr) sec.contentAr else sec.contentEn,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
