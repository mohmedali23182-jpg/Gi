# اختبارات العمل دون إنترنت والأداء ثلاثي الأبعاد (Offline & 3D Performance Verification)
## مشروع «دواء وحياة — Dawa & Hayah» — المرحلة الثانية (Stage 02)

---

## 1. سيناريوهات التحقق من العارض ثلاثي الأبعاد وحزم المحتوى دون إنترنت

### السيناريو 1: التحميل الفوري لمجسم القلب ثلاثي الأبعاد (`cor_chambers_low_v1.glb`) في وضع الطيران
- **المسار المحلي:** `app/src/main/assets/models/cor_chambers_low_v1.glb` وحزمة `app/src/main/assets/packs/heart-model-low-v1/models/cor_chambers_low_v1.glb`.
- **الحجم الفعلي:** `50,576 بايت` (~49.4 كيلوبايت) — أقل بكثير من سقف ميزانية الجوال (`500 KB`).
- **بصمة SHA-256:** `56ed783e8a6b79c81972767616f67580fc5aec564b6368da5bc54b74c2659ef8`.
- **زمن تحليل وبناء مخازن WebGL (Buffers):** `~4 إلى 12 مللي ثانية` لـ 6 عقد تشريحية مسماة وفق معيار TA2 (`Atrium_Dextrum`, `Atrium_Sinistrum`, `Ventriculus_Dexter`, `Ventriculus_Sinister`, `Arcus_Aortae`, `Nodus_Sinuatrialis`).

### السيناريو 2: اختبار تفاعل المستخدم مع أجزاء المجسم (Sub-Mesh Picking & Layer Isolation)
- النقر على أي حجرة قلبية أو وعاء كبير يحدد العقدة المقابلة ويعرض فوراً:
  - الاسم العربي والإنجليزي والمصطلح اللاتيني ورمز `TA2`.
  - الوظيفة الفسيولوجية والأهمية السريرية والدوائية.
- تفعيل **وضع الشفافية (X-Ray Mode)** يخفض عتامة الأجزاء غير المحددة إلى `0.28` ويبقي الجزء المحدد واضحاً (`0.96`).
- تفعيل **عزل المعلم المحدد (Isolate Selected)** يخفي باقي الأجزاء لدراسة هندسة الحجرة المختارة بمفردها.

### السيناريو 3: اختبار التراجع التلقائي ثنائي الأبعاد (2D Fallback)
- عند استدعاء `Stage01AnatomyAssetPlayer.resolveAssetState(..., isWebGlSupported = false)` أو تفعيل زر البديل ثنائي الأبعاد، ينتقل التطبيق بسلاسة إلى المخطط المتجهي المرخص (`2D Vector Fallback`) دون رمي أي استثناء.

### السيناريو 4: اختبار الحزم الاختيارية المتدرجة (Granular Offline Packs)
- تثبيت حزمة `heart-model-low-v1` (`50,576 bytes`) أو `respiratory-diseases-ar-v1` (`3,126 bytes`) أو `pack-cardio-histology-ar-v1` (`3,917 bytes`) يمر عبر:
  1. النسخ إلى مجلد `filesDir/staging/`
  2. فحص منع اختراق المسارات (`Path Traversal Check`)
  3. فحص الحجم وبصمة `SHA-256`
  4. الترقية الذرية إلى `filesDir/content_packs/`
  5. التسجيل في Room داخل معاملة واحدة (`commitPackInstallationTransaction`).
- حذف أي حزمة يزيل ملفاتها من `content_packs` مع بقاء كافة سجلات الأجهزة الـ 13 والأعضاء الـ 10 والأمراض الـ 18 في قاعدة المعرفة الأساسية سليمة تماماً.
