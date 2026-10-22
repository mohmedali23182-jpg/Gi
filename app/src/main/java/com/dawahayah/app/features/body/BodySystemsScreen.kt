package com.dawahayah.app.features.body

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dawahayah.app.core.storage.AppLanguage
import com.dawahayah.app.domain.model.AnatomicalStructure
import com.dawahayah.app.domain.model.Organ
import com.dawahayah.app.domain.model.PackInstallState
import com.dawahayah.app.domain.model.TerminologyLabelMode
import com.dawahayah.app.features.settings.MainUiState
import com.dawahayah.app.ui.components.KnowledgeTopicCard
import com.dawahayah.app.ui.components.UnboxedMetadataLine

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BodySystemsScreen(
    state: MainUiState,
    onBookmarkToggle: (String, String, String, String, String, Boolean) -> Unit,
    onRecordReading: (String, String, String, String) -> Unit,
    onInstallPack: (String) -> Unit,
    onOpenAnatomyScenes: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isAr = state.settings.language == AppLanguage.ARABIC
    var selectedSystemId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedOrganId by rememberSaveable { mutableStateOf<String?>("organ-heart") }
    var labelMode by rememberSaveable { mutableStateOf(TerminologyLabelMode.TRILINGUAL) }
    var selectedAnnotationMarker by rememberSaveable { mutableStateOf<Int?>(1) }
    val bookmarkedIds = state.bookmarks.map { it.itemId }.toSet()

    val displayedOrgans = if (selectedSystemId == null) {
        state.organs
    } else {
        state.organs.filter { it.systemId == selectedSystemId }
    }

    val activeOrgan: Organ? = state.organs.firstOrNull { it.id == selectedOrganId }
        ?: displayedOrgans.firstOrNull()

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
                    "موسوعة أجهزة جسم الإنسان (${state.systems.size} جهازاً تشريحياً)"
                } else {
                    "Human Body Systems Encyclopedia (${state.systems.size} Systems)"
                },
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isAr) {
                    "المرحلة الثانية: تغطية شاملة للأجهزة الـ 13 مع مسارات تفصيلية للأعضاء والتراكيب التشريحية وفق معيار Terminologia Anatomica (TA2) وعارض 3D التفاعلي."
                } else {
                    "Stage 02: Full 13-system index with detailed organ & sub-structure pathways per Terminologia Anatomica (TA2) and interactive 3D GLB viewer."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onOpenAnatomyScenes) {
                    Text(if (isAr) "فتح عارض التشريح ثلاثي الأبعاد (3D GLB)" else "Open 3D Anatomy Viewer (GLB)")
                }
            }
        }

        // Independent Anatomical Terminology Language Switcher (AR / EN / Latin TA2)
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = if (isAr) {
                            "لغة اللافتات التشريحية (مستقلة عن لغة واجهة التطبيق)"
                        } else {
                            "Anatomical Label Language (Independent of App UI)"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = labelMode == TerminologyLabelMode.TRILINGUAL,
                            onClick = { labelMode = TerminologyLabelMode.TRILINGUAL },
                            label = { Text(if (isAr) "ثلاثي (عربي + EN + Latin)" else "Trilingual (AR + EN + Latin)") }
                        )
                        FilterChip(
                            selected = labelMode == TerminologyLabelMode.ARABIC,
                            onClick = { labelMode = TerminologyLabelMode.ARABIC },
                            label = { Text("العربية فقط") }
                        )
                        FilterChip(
                            selected = labelMode == TerminologyLabelMode.ENGLISH,
                            onClick = { labelMode = TerminologyLabelMode.ENGLISH },
                            label = { Text("English Only") }
                        )
                        FilterChip(
                            selected = labelMode == TerminologyLabelMode.LATIN_TA2,
                            onClick = { labelMode = TerminologyLabelMode.LATIN_TA2 },
                            label = { Text("Latin TA2 Only") }
                        )
                    }
                }
            }
        }

        // Filter Chips for the 13 Body Systems
        item {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedSystemId == null,
                    onClick = { selectedSystemId = null },
                    label = { Text(if (isAr) "كل الأجهزة (${state.systems.size})" else "All (${state.systems.size})") }
                )
                state.systems.forEach { sys ->
                    FilterChip(
                        selected = selectedSystemId == sys.id,
                        onClick = {
                            selectedSystemId = if (selectedSystemId == sys.id) null else sys.id
                        },
                        label = {
                            Text(
                                text = if (isAr) sys.nameAr else sys.nameEn
                            )
                        }
                    )
                }
            }
        }

        // Systems List
        items(
            items = if (selectedSystemId == null) state.systems else state.systems.filter { it.id == selectedSystemId },
            key = { it.id }
        ) { system ->
            val isBookmarked = bookmarkedIds.contains(system.id)
            val primaryHeading = when (labelMode) {
                TerminologyLabelMode.ARABIC -> system.nameAr
                TerminologyLabelMode.ENGLISH -> system.nameEn
                TerminologyLabelMode.LATIN_TA2 -> system.latinName ?: system.nameEn
                TerminologyLabelMode.TRILINGUAL -> if (isAr) system.nameAr else system.nameEn
            }
            val secondaryHeading = when (labelMode) {
                TerminologyLabelMode.TRILINGUAL -> if (isAr) system.nameEn else system.nameAr
                else -> null
            }

            KnowledgeTopicCard(
                title = primaryHeading,
                secondaryTitle = secondaryHeading,
                latinTerm = if (labelMode == TerminologyLabelMode.TRILINGUAL || labelMode == TerminologyLabelMode.LATIN_TA2) system.latinName else null,
                bodyText = buildString {
                    append(if (isAr) system.descriptionAr else system.descriptionEn)
                    if (system.interSystemRelationsAr.isNotBlank()) {
                        append("\n\n")
                        append(if (isAr) "التكامل الفسيولوجي بين الأجهزة: ${system.interSystemRelationsAr}" else "Inter-System Integration: ${system.interSystemRelationsEn}")
                    }
                },
                metadataItems = listOf(
                    if (system.hasDetailedPathway) {
                        if (isAr) "مسار تفصيلي كامل (المرحلة 02)" else "Full Detailed Pathway (Stage 02)"
                    } else {
                        if (isAr) "مفهرس موسوعياً" else "Indexed System"
                    },
                    if (system.relatedDrugClasses.isNotEmpty()) {
                        "الفئات الدوائية: ${system.relatedDrugClasses.take(3).joinToString(" · ")}"
                    } else {
                        "المصادر: ${system.sourceIds.joinToString(", ")}"
                    }
                ),
                isBookmarked = isBookmarked,
                onBookmarkToggle = {
                    onBookmarkToggle(
                        system.id,
                        "BODY_SYSTEM",
                        system.nameAr,
                        system.nameEn,
                        system.latinName ?: "",
                        isBookmarked
                    )
                },
                onCardClick = {
                    selectedSystemId = if (selectedSystemId == system.id) null else system.id
                    onRecordReading(system.id, "BODY_SYSTEM", system.nameAr, system.nameEn)
                }
            )
        }

        // Linked Organs Header
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (isAr) {
                    "الأعضاء التشريحية والتراكيب الدقيقة (${displayedOrgans.size})"
                } else {
                    "Anatomical Organs & Sub-Structures (${displayedOrgans.size})"
                },
                style = MaterialTheme.typography.titleLarge
            )
        }

        items(displayedOrgans, key = { it.id }) { organ ->
            val isBookmarked = bookmarkedIds.contains(organ.id)
            val isExpanded = activeOrgan?.id == organ.id
            val relatedPack = state.packs.firstOrNull { it.scopeId == organ.id && it.isBundledOfflineDemoAvailable }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                KnowledgeTopicCard(
                    title = formatTermByMode(organ.nameAr, organ.nameEn, organ.latinName ?: organ.nameEn, labelMode, isAr),
                    secondaryTitle = if (labelMode == TerminologyLabelMode.TRILINGUAL) (if (isAr) organ.nameEn else organ.nameAr) else null,
                    latinTerm = organ.latinName,
                    bodyText = buildString {
                        append(if (isAr) organ.summaryAr else organ.summaryEn)
                        if (organ.locationAr.isNotBlank()) {
                            append("\n")
                            append(if (isAr) "الموقع التشريحي: ${organ.locationAr}" else "Anatomical Location: ${organ.locationEn}")
                        }
                    },
                    metadataItems = listOf(
                        if (organ.has3dAsset) {
                            if (isAr) "يتوفر مجسم 3D GLB تفاعلي" else "Interactive 3D GLB Available"
                        } else {
                            if (isAr) "لا توجد نسخة 3D لهذا العضو بعد (مخطط 2D)" else "No 3D Model Yet (2D Diagram)"
                        },
                        "التراكيب الدقيقة: ${organ.structures.size}",
                        "الأمراض المرتبطة: ${organ.relatedDiseaseIds.size}"
                    ),
                    isBookmarked = isBookmarked,
                    onBookmarkToggle = {
                        onBookmarkToggle(
                            organ.id,
                            "ORGAN",
                            organ.nameAr,
                            organ.nameEn,
                            organ.latinName ?: "",
                            isBookmarked
                        )
                    },
                    onCardClick = {
                        selectedOrganId = if (selectedOrganId == organ.id) null else organ.id
                        onRecordReading(organ.id, "ORGAN", organ.nameAr, organ.nameEn)
                    }
                )

                if (isExpanded) {
                    OrganDetailedEncyclopediaCard(
                        organ = organ,
                        isAr = isAr,
                        labelMode = labelMode,
                        selectedAnnotationMarker = selectedAnnotationMarker,
                        onSelectAnnotationMarker = { selectedAnnotationMarker = it },
                        onOpenAnatomyScenes = onOpenAnatomyScenes
                    )
                }

                if (relatedPack != null) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = if (isAr) relatedPack.titleAr else relatedPack.titleEn,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            UnboxedMetadataLine(
                                items = listOf(
                                    "الحجم: ${relatedPack.sizeBytes} بايت",
                                    "الحالة: ${relatedPack.installState.name}"
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            if (relatedPack.installState != PackInstallState.COMPLETED) {
                                Button(onClick = { onInstallPack(relatedPack.id) }) {
                                    Text(
                                        if (isAr) "تنزيل الحزمة للعمل دون إنترنت (SHA-256)"
                                        else "Download Pack for Offline Use (SHA-256)"
                                    )
                                }
                            } else {
                                Text(
                                    text = if (isAr) "مثبتة في التخزين الدائم وجاهزة للقراءة في وضع الطيران." else "Installed in persistent storage for offline reading.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OrganDetailedEncyclopediaCard(
    organ: Organ,
    isAr: Boolean,
    labelMode: TerminologyLabelMode,
    selectedAnnotationMarker: Int?,
    onSelectAnnotationMarker: (Int) -> Unit,
    onOpenAnatomyScenes: () -> Unit
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
                text = if (isAr) "التفصيل التشريحي والفسيولوجي: ${organ.nameAr}" else "Anatomical & Physiological Detail: ${organ.nameEn}",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )

            // 3D Availability Notice or Launch Button
            if (organ.has3dAsset) {
                Button(onClick = onOpenAnatomyScenes) {
                    Text(
                        if (isAr) "استعراض المجسم ثلاثي الأبعاد (GLB) وتحديد الأجزاء التشريحية"
                        else "Inspect Interactive 3D GLB Mesh & Pick Structures"
                    )
                }
            } else {
                Text(
                    text = if (isAr) {
                        "حالة المجسم ثلاثي الأبعاد: لا توجد نسخة 3D محلية لهذا العضو بعد؛ يُعتمد على الشرح البنيوي والمخططات التشريحية ثنائية الأبعاد الموثقة."
                    } else {
                        "3D Status: No local 3D model for this organ yet; relying on verified structural text and 2D diagrams."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Organ Sections (Macro Anatomy, Histology, Physiology)
            organ.sections.forEach { sec ->
                HorizontalDivider()
                Text(
                    text = if (isAr) sec.titleAr else sec.titleEn,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (isAr) sec.bodyAr else sec.bodyEn,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Anatomical Sub-Structures (TA2)
            if (organ.structures.isNotEmpty()) {
                HorizontalDivider()
                Text(
                    text = if (isAr) {
                        "التراكيب التشريحية الفرعية ومعيار TA2 (${organ.structures.size})"
                    } else {
                        "Anatomical Sub-Structures & TA2 Terminology (${organ.structures.size})"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                organ.structures.forEach { st: AnatomicalStructure ->
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = formatTermByMode(st.nameAr, st.nameEn, st.latinName, labelMode, isAr),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${st.latinName} · ${st.ta2Code} · ${st.structureType}",
                                style = MaterialTheme.typography.labelMedium.copy(fontStyle = FontStyle.Italic),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (isAr) "الوظيفة: ${st.functionAr}" else "Function: ${st.functionEn}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = if (isAr) "الأهمية السريرية والدوائية: ${st.clinicalRelevanceAr}" else "Clinical & Drug Relevance: ${st.clinicalRelevanceEn}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Interactive Figure Annotations
            organ.figures.forEach { fig ->
                HorizontalDivider()
                Text(
                    text = if (isAr) fig.titleAr else fig.titleEn,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                UnboxedMetadataLine(
                    items = listOf(
                        fig.provenanceType,
                        fig.license
                    )
                )
                if (fig.annotations.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        fig.annotations.forEach { ann ->
                            FilterChip(
                                selected = selectedAnnotationMarker == ann.markerNumber,
                                onClick = { onSelectAnnotationMarker(ann.markerNumber) },
                                label = {
                                    Text(
                                        "#${ann.markerNumber} ${formatTermByMode(ann.labelAr, ann.labelEn, ann.labelLatin, labelMode, isAr)}"
                                    )
                                }
                            )
                        }
                    }
                    val activeAnn = fig.annotations.firstOrNull { it.markerNumber == selectedAnnotationMarker }
                        ?: fig.annotations.first()
                    Text(
                        text = "${activeAnn.labelAr} (${activeAnn.labelLatin}): ${if (isAr) activeAnn.descriptionAr else activeAnn.descriptionEn}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

private fun formatTermByMode(
    ar: String,
    en: String,
    latin: String,
    mode: TerminologyLabelMode,
    isArUi: Boolean
): String = when (mode) {
    TerminologyLabelMode.ARABIC -> ar
    TerminologyLabelMode.ENGLISH -> en
    TerminologyLabelMode.LATIN_TA2 -> latin
    TerminologyLabelMode.TRILINGUAL -> if (isArUi) "$ar — $en ($latin)" else "$en — $ar ($latin)"
}
