# مواصفة حزم المحتوى غير المتصلة وسلامة الملفات (Content Pack Specification)

---

## 1. الغرض التصميمي
صُمم محرك `ContentPackManager` لتمكين طلاب الطب والصيدلة من تنزيل حزم دقيقة ومحددة النطاق (`system`, `organ`, `disease`, `medicine`, `lesson`, `image`, `model3d`) والعمل بها دون إنترنت، بدلاً من إجبار جميع المستخدمين على تنزيل مئات الميجابايتات دفعة واحدة.

---

## 2. هيكل عقد الحزمة (`ContentPackManifest`)
```json
{
  "packId": "pack-cardio-histology-ar-v1",
  "version": "1.0.0",
  "scopeType": "organ",
  "scopeId": "organ-heart",
  "titleAr": "حزمة فسيولوجيا وتشريح نظام التوصيل القلبي (Cor)",
  "titleEn": "Cardiac Conduction System Histology & Physiology Pack (Cor)",
  "language": "ar",
  "contentKind": "lesson_and_vector",
  "quality": "standard",
  "schemaVersion": 1,
  "minAppVersion": "0.1.0",
  "sizeBytes": 3917,
  "files": [
    {
      "path": "content/cardiac_conduction_ar.json",
      "contentType": "application/json",
      "sizeBytes": 3219,
      "sha256": "3cc74637daa3d6836166c8e89d9caead5ac2db8c0bf55ed9ee8605431575af64",
      "assetId": "asset-cardiac-conduction-json-v1"
    },
    {
      "path": "graphics/cor_conduction_vector.svg",
      "contentType": "image/svg+xml",
      "sizeBytes": 698,
      "sha256": "0fcd952a44a2ebb991661b33146c4d8efda2e0434f825b2adcd9aba23ccc862e",
      "assetId": "asset-cor-conduction-svg-v1"
    }
  ],
  "dependencies": [],
  "sources": ["src-ta2-fipat", "src-ncbi-bookshelf-statpearls"],
  "license": "CC-BY-4.0 (Dawa & Hayah Original Educational Pack)",
  "attribution": "دواء وحياة — الدكتور الصيدلاني معتز العلقمي — 2026 م.",
  "publishedAt": "2026-10-22T00:00:00Z"
}
```

---

## 3. دورة حياة التنزيل والتحقق على مرحلتين (Two-Phase Staging & Atomic Promotion)
1. **فحص القيود المسبقة:**
   - التحقق من مساحة التخزين المتاحة (`filesDir.usableSpace >= sizeBytes * 2`).
   - احترام خيار المستخدم «تنزيل عبر Wi-Fi فقط» عند التنزيل من الشبكة عبر `WorkManager` (`NetworkType.UNMETERED`).
2. **التنزيل المؤقت (`Staging`):**
   - تُكتب الملفات أولاً في `context.filesDir/staging/{packId}_{version}/`.
   - يُفحص كل مسار نسبي عبر `IntegrityVerifier.isSafeRelativePath` لمنع هجمات اختراق المسارات (`../` أو المسارات المطلقة).
3. **التحقق من البصمة الرقمية (`SHA-256` & Size Verification):**
   - يُحسب `SHA-256` الفعلي لكل ملف في `staging` ويُقارن بالبصمة المعلنة في `manifest.json` مع مطابقة عدد البايتات.
   - في حال فشل أي ملف، يُحذف مجلد `staging` بالكامل فوراً وتتحول حالة الحزمة إلى `FAILED` دون المساس بأي نسخة سابقة مثبتة.
4. **الترقية الذرية والتسجيل في Room (`Atomic Promotion`):**
   - يُنقل المجلد من `staging` إلى `context.filesDir/content_packs/{packId}/{version}/` (تخزين دائم لا يمسحه النظام عشوائياً مثل `cacheDir`).
   - تُنفذ معاملة Room `@Transaction` (`commitPackInstallationTransaction`) لتحديث `pack_records` و`local_assets` و`local_contents`.
5. **الحذف النظيف (`Clean Uninstall`):**
   - عند طلب حذف الحزمة، تُحذف ملفاتها من `filesDir/content_packs/{packId}` وتُزال سجلاتها من `local_assets` و`local_contents` مع بقاء بيانات `BaseSeed` والمفضلة سليمة تماماً.
