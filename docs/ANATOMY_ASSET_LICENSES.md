# سجل تراخيص ونسب الأصول التشريحية والمجسمات ثلاثية الأبعاد (Anatomy & 3D Asset Licenses)
## مشروع «دواء وحياة — Dawa & Hayah» — المرحلة الثانية (Stage 02)

---

## 1. السياسة الصارمة لتراخيص الأصول البصرية والتشريحية
يمنع في مشروع «دواء وحياة» استخدام أي صورة أو مخطط أو مجسم ثلاثي الأبعاد مجهول المصدر أو خاضع لحقوق ملكية مغلقة. كما يلتزم التطبيق **بالتمييز الصريح في واجهة المستخدم وفي قاعدة البيانات (`provenanceCategory`)** بين:
1. `VERIFIED_SCIENTIFIC_MESH`: مجسمات تشريحية علمية مرجعية مدققة من أطالس مفتوحة المصدر (مثل Z-Anatomy / BodyParts3D).
2. `SCHEMATIC_EDUCATIONAL_MESH`: مجسمات تعليمية تخطيطية متعددة الأجزاء مبنية وموسومة وفق معيار TA2 لشرح العلاقات التشريحية والوظيفية على الجوال.
3. `VERIFIED_SCIENTIFIC_DIAGRAM`: مخططات تشريحية متجهية ثنائية الأبعاد موثقة.

---

## 2. جدول الأصول المسجلة في المرحلة الثانية

| معرف الأصل (`assetId`) | الملف / المسار | الفئة (`provenanceCategory`) | الرخصة | النسب العلمي (`attribution`) | بصمة SHA-256 |
|---|---|---|---|---|---|
| `3d-cor-chambers-low-v1` | `assets/models/cor_chambers_low_v1.glb` (`50,576 bytes`) | `SCHEMATIC_EDUCATIONAL_MESH` | `CC-BY-4.0` | دواء وحياة — د. معتز العلقمي — 2026 م (مسميات העقد الست وفق FIPAT TA2) | `56ed783e8a6b79c81972767616f67580fc5aec564b6368da5bc54b74c2659ef8` |
| `fig-cor-conduction-v1` | `assets/graphics/cor_anatomy_diagram.svg` | `VERIFIED_SCIENTIFIC_DIAGRAM` | `CC-BY-4.0` | دواء وحياة — مبني على مراجع OpenStax Anatomy & Physiology 2e و FIPAT TA2 | `0fcd952a44a2ebb991661b33146c4d8efda2e0434f825b2adcd9aba23ccc862e` |
| `3d-cor-chambers-high-v1` | `anatomical-3d/cor_chambers_high_v1.glb` (حزمة سحابية اختيارية) | `VERIFIED_SCIENTIFIC_MESH` | `CC-BY-SA-4.0` | Z-Anatomy / BodyParts3D (Database Center for Life Science, Japan) | مسجل في عقد الحزمة السحابية |
| `3d-pulmones-bronchi-v1` | `anatomical-3d/pulmones_bronchi_v1.glb` (حزمة سحابية اختيارية) | `VERIFIED_SCIENTIFIC_MESH` | `CC-BY-SA-4.0` | Z-Anatomy / BodyParts3D / FIPAT TA2 | مسجل في عقد الحزمة السحابية |
| `3d-ren-nephron-v1` | `anatomical-3d/ren_cross_section_v1.glb` (حزمة سحابية اختيارية) | `VERIFIED_SCIENTIFIC_MESH` | `CC-BY-SA-4.0` | Z-Anatomy / BodyParts3D / FIPAT TA2 | مسجل في عقد الحزمة السحابية |
