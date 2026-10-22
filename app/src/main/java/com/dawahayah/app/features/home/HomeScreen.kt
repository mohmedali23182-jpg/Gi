package com.dawahayah.app.features.home

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dawahayah.app.core.storage.AppLanguage
import com.dawahayah.app.data.remote.CloudBackendState
import com.dawahayah.app.domain.model.PackInstallState
import com.dawahayah.app.features.settings.MainUiState
import com.dawahayah.app.ui.components.UnboxedMetadataLine
import com.dawahayah.app.ui.navigation.AuxiliaryRoutes
import com.dawahayah.app.ui.navigation.PrimaryBottomTab

@Composable
fun HomeScreen(
    state: MainUiState,
    onNavigateTab: (PrimaryBottomTab) -> Unit,
    onNavigateAuxiliary: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isAr = state.settings.language == AppLanguage.ARABIC
    val installedPacksCount = state.packs.count { it.installState == PackInstallState.COMPLETED }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            // Hero Identity Banner
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(22.dp)) {
                    Text(
                        text = if (isAr) "دواء وحياة · Dawa & Hayah" else "Dawa & Hayah · دواء وحياة",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isAr) {
                            "علم الدواء لفهم الحياة — موسوعة طبية وصيدلانية تأسيسية تعمل دون إنترنت"
                        } else {
                            "Pharmacology to Understand Life — Offline-First Medical & Pharmaceutical Foundation"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.92f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    val cloudLabel = when (state.cloudBackendState) {
                        is CloudBackendState.DisabledInStage01 ->
                            if (isAr) "البيانات السحابية (Neon): غير متصلة بعد — الوضع المحلي نشط"
                            else "Neon Cloud: Not connected yet — Local-First mode active"
                        is CloudBackendState.Configured ->
                            if (isAr) "البيانات السحابية (Neon): مهيأة عبر HTTPS"
                            else "Neon Cloud: Configured via HTTPS"
                    }
                    Text(
                        text = cloudLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        // Continue Reading Card
        item {
            val latestProgress = state.readingProgress.firstOrNull()
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = if (isAr) "متابعة القراءة" else "Continue Reading",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    if (latestProgress != null) {
                        Text(
                            text = if (isAr) latestProgress.titleAr else latestProgress.titleEn,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        UnboxedMetadataLine(
                            items = listOf(
                                latestProgress.itemType,
                                if (isAr) "محفوظ محلياً في Room" else "Persisted locally in Room"
                            )
                        )
                    } else {
                        Text(
                            text = if (isAr) {
                                "ابدأ باستكشاف الجهاز القلبي الوعائي (Systema cardiovasculare) أو آليات عمل حاصرات بيتا-1."
                            } else {
                                "Start exploring the Cardiovascular System (Systema cardiovasculare) or Beta-1 blocker pharmacology."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Verified Scientific Fact Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = if (isAr) "معلومة علمية موثقة" else "Verified Scientific Insight",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isAr) {
                            "تتراكم مثبطات مضخة البروتون (مثل أوميبرازول) انتقائياً في القنيات الإفرازية الحامضية للخلايا الجدارية المعدية بفضل طبيعتها القاعدية الضعيفة (Prodrug Trapping)، ثم تتحول داخل الوسط الحامضي إلى شكل السلفيناميد الفعال الذي يثبط مضخة H+/K+ ATPase."
                        } else {
                            "Proton pump inhibitors (such as Omeprazole) selectively concentrate in the acidic secretory canaliculi of gastric parietal cells as weak-base prodrugs before converting to active sulfenamide."
                        },
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    UnboxedMetadataLine(
                        items = listOf(
                            "المصدر: NCBI Bookshelf / NLM RxNorm",
                            "محتوى تعليمي تأسيسي (غير علاجي)"
                        )
                    )
                }
            }
        }

        // Sections Grid & Quick Access
        item {
            Text(
                text = if (isAr) "أقسام الموسوعة التأسيسية (المرحلة الأولى)" else "Foundational Encyclopedia Sections (Stage 01)",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SectionShortcutButton(
                        title = if (isAr) "جسم الإنسان (${state.systems.size} أجهزة)" else "Body Systems (${state.systems.size})",
                        subtitle = if (isAr) "${state.organs.size} أعضاء بالمصطلح اللاتيني" else "${state.organs.size} Organs with Latin TA2",
                        onClick = { onNavigateTab(PrimaryBottomTab.BODY_SYSTEMS) },
                        modifier = Modifier.weight(1f)
                    )
                    SectionShortcutButton(
                        title = if (isAr) "الأمراض (${state.diseases.size})" else "Diseases (${state.diseases.size})",
                        subtitle = if (isAr) "الفيزيولوجيا المرضية" else "Pathophysiology",
                        onClick = { onNavigateTab(PrimaryBottomTab.DISEASES) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SectionShortcutButton(
                        title = if (isAr) "المواد الفعالة (${state.activeIngredients.size})" else "Medicines (${state.activeIngredients.size})",
                        subtitle = if (isAr) "آليات العمل الجزيئية" else "Mechanisms of Action",
                        onClick = { onNavigateTab(PrimaryBottomTab.MEDICINES) },
                        modifier = Modifier.weight(1f)
                    )
                    SectionShortcutButton(
                        title = if (isAr) "الحزم والتنزيلات ($installedPacksCount/${state.packs.size})" else "Offline Packs ($installedPacksCount/${state.packs.size})",
                        subtitle = if (isAr) "تحقق SHA-256 وتخزين دائم" else "SHA-256 Verified Storage",
                        onClick = { onNavigateAuxiliary(AuxiliaryRoutes.PACK_DOWNLOADS) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Auxiliary Screens & Upcoming Stage Roadmap Notice
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = if (isAr) "الأدوات المساندة وخارطة المراحل القادمة" else "Auxiliary Tools & Upcoming Stages Roadmap",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onNavigateAuxiliary(AuxiliaryRoutes.ANATOMY_SCENES) }) {
                            Text(if (isAr) "المشاهد التشريحية 3D" else "Anatomical Scenes")
                        }
                        OutlinedButton(onClick = { onNavigateAuxiliary(AuxiliaryRoutes.SOURCES_AND_LICENSES) }) {
                            Text(if (isAr) "المصادر والتراخيص" else "Sources & Licenses")
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (isAr) {
                            "ميزات قادمة في المراحل (2–5): عارض المجسمات التشريحية GLB التفاعلي (المرحلة 2)، دليل الأصناف اليمنية والعربية الموثقة (المرحلة 3)، الحاسبات الصيدلانية والحالات السريرية والاختبارات الأكاديمية (المرحلة 4)."
                        } else {
                            "Upcoming in Stages 2–5: Interactive 3D GLB Atlas (Stage 2), Verified Yemeni & Arab Drug Registry (Stage 3), Clinical Cases & Calculators (Stage 4)."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionShortcutButton(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
