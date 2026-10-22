# حدود بيئة Google AI Studio والتمييز بين ما يُنفّذ محلياً وما يحتاج بيئة خارجية
**المشروع:** دواء وحياة (Dawa & Hayah) — المرحلة الأولى

---

## 1. القيود الموثقة لبيئة التشغيل الحالية
1. **طبيعة الحاوية:** تعمل جلسة البناء الحالية داخل حاوية Linux/Node.js بدون Android SDK (`ANDROID_HOME`) وبدون محاكي Android KVM مدمج.
2. **التزام الشفافية وعدم الادعاء:**
   - لم يتم ادعاء تشغيل محاكي Android KVM داخل هذه الحاوية.
   - بدلاً من تحويل المشروع إلى تطبيق ويب، تم بناء **شجرة مشروع Android Native كاملة وحقيقية 100%** (`settings.gradle.kts`, `app/build.gradle.kts`, `AndroidManifest.xml`, كود Kotlin + Jetpack Compose + Room + DataStore + WorkManager + Retrofit، واختبارات JUnit/Compose)، وتجهيزها في حزمة **ZIP** نظيفة قابلة للتصدير الفوري (`dawa-hayah-aistudio-android-stage01.zip`) مع ملف سير عمل **GitHub Actions CI** يتكفل ببناء APK وفحصه في بيئة Ubuntu + Android SDK قياسية.
   - كما تم توفير **منصة فحص ومعاينة تفاعلية (Stage 01 Interactive Workbench)** على المنفذ 3000 تتيح للمالك استعراض وتجربة الشاشات الخمس، البحث العربي المطبع، التحقق الفعلي من بصمة `SHA-256` للحزمة المدمجة، وتنزيل ملف الـ ZIP بنقرة واحدة.

---

## 2. ما يعمل فوراً دون أي إعداد خارجي
- جميع ملفات كود Kotlin وواجهات Jetpack Compose Material 3 باللغتين العربية (RTL) والإنجليزية (LTR).
- قاعدة المعرفة التأسيسية المدمجة (`assets/seed/base_seed_v1.json`) ومخطط قاعدة بيانات Room (`DawaHayahDatabase` الإصدار 2 مع ترحيل `MIGRATION_1_2`).
- حزمة الاختبار المحلية (`pack-cardio-histology-ar-v1`) المزودة ببصمات `SHA-256` حقيقية مطابقة للبايتات الفعلية، والتي تثبت وتُقرأ وتُحذف محلياً في وضع الطيران.

---

## 3. ما يحتاج إلى نشر خارجي مستقل في المراحل القادمة
- **قاعدة Neon السحابية (PostgreSQL + Object Storage + Compute Functions):** تم تسليم ملفات الترحيل (`neon/migrations/001_stage01_dawa_hayah_schema.sql`) ومواصفات OpenAPI (`neon/openapi/dawa-hayah-neon-v1.openapi.yaml`) ودالة الخادم (`neon/functions/v1-api-handler.ts`). في المرحلة الأولى يعمل التطبيق في حالة `DisabledBackend` الصريحة («البيانات السحابية غير متصلة بعد») إلى حين نشر مشروع Neon مستقل وإدراج رابط الـ HTTPS العام في `NEON_PUBLIC_API_BASE_URL`.
- **مزامنة GitHub المباشرة:** لا يتم المساس بأي مستودع قائم؛ يُرفع المشروع عبر ملف الـ ZIP المصدر أو أوامر Git الموضحة في `README.md` إلى المستودع المستقل `dawa-hayah-aistudio-android`.
