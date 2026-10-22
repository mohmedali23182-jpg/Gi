# توثيق مهاجرات Neon السحابية و Room المحلية للمرحلة الثانية (Neon & Room Stage 02 Migrations)
## مشروع «دواء وحياة — Dawa & Hayah» — المرحلة الثانية (Stage 02)

---

## 1. ترقية قاعدة البيانات المحلية Room (`version 2 -> version 3`)
في الملف `app/src/main/java/com/dawahayah/app/data/local/DawaHayahDatabase.kt`:
- تم رفع إصدار قاعدة البيانات من `version = 2` (المرحلة الأولى) إلى `version = 3` (المرحلة الثانية).
- أُضيفت المهاجرة الصريحة `MIGRATION_2_3 = object : Migration(2, 3)` التي:
  1. تحافظ بالكامل على جداول المستخدم (`bookmarks`, `reading_progress`, `pack_records`, `local_assets`, `download_tasks`) دون أي مسح للبيانات.
  2. توسع جداول `body_systems`، `organs`، و `diseases` بالأعمدة الجديدة (`hasDetailedPathway`، `interSystemRelationsAr/En`، `locationAr/En`، `has3dAsset`، `icd11Code`، `importedAt`، `humanReviewedAt`).
  3. تنشئ الجداول التسعة الجديدة للمرحلة الثانية:
     - `anatomical_structures`
     - `anatomy_labels`
     - `organ_sections`
     - `science_figures`
     - `anatomy_3d_assets`
     - `disease_sections`
     - `disease_system_links`
     - `disease_organ_links`
     - `source_attributions`

---

## 2. مهاجرة قاعدة بيانات Neon PostgreSQL (`002_stage02_anatomy_diseases_schema.sql`)
في المسار `neon/migrations/002_stage02_anatomy_diseases_schema.sql`:
- تُطبّق المهاجرة فوق مخطط المرحلة الأولى (`001_stage01_foundational_schema.sql`) باستخدام معاملة ذرية (`BEGIN ... COMMIT`).
- تضيف الجداول والفهارس المرجعية (`anatomical_structures`, `anatomy_labels`, `organ_sections`, `science_figures`, `anatomy_3d_assets`, `disease_sections`, `disease_system_links`, `disease_organ_links`, `source_attributions`).

---

## 3. نقاط النهاية الجديدة في عقد OpenAPI v1 (`dawa-hayah-neon-v1.openapi.yaml`)
تمت إضافة نقاط النهاية التالية في `neon/openapi/dawa-hayah-neon-v1.openapi.yaml` وفي `NeonApiService.kt`:
- `GET /v1/body-systems`
- `GET /v1/organs/{id}`
- `GET /v1/anatomy/models`
- `GET /v1/diseases`
- `GET /v1/diseases/{id}`
- `GET /v1/content-packs`
- `GET /v1/sources/{id}`

**ملاحظة أمنية:** لا يحتوي تطبيق Android على أي مفاتيح `DATABASE_URL` أو `NEON_API_KEY` أو مفاتيح S3؛ وفي غياب الخادم السحابي المنشور يعمل `DisabledNeonDataSource` بشفافية تامة مع استمرار عمل القاعدة المحلية وحزم المحتوى المدمجة بنسبة 100%.
