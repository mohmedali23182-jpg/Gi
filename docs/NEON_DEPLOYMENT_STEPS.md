# خطوات نشر بيئة Neon المستقلة خارج AI Studio (Neon Deployment Guide)

---

## 1. متى يجب إنشاء مشروع Neon مستقل؟
- في المرحلة الأولى (Stage 01)، يعمل تطبيق Android بكامل وظائفه التأسيسية وحزمة الاختبار المدمجة محلياً دون الحاجة إلى نشر فوري لخادم Neon، ويظهر التطبيق حالة `DisabledBackend` الصريحة («البيانات السحابية غير متصلة بعد»).
- عند الرغبة في تفعيل التزامن السحابي وتنزيل الحزم عن بُعد في المرحلة الثانية أو الثالثة، يتم إنشاء مشروع Neon مستقل تماماً باسم **`dawa-hayah-aistudio-cloud`** دون المساس بأي مشاريع Neon سابقة.

---

## 2. اختيار المنطقة الجغرافية (Colocated Region Selection)
وفق توثيق Neon الرسمي (`neon.com/docs/introduction/regions` و`neon.com/docs/storage/overview`):
- اختر منطقة تدعم كلاً من **Neon PostgreSQL** و**Neon Object Storage** و**Neon Compute Functions** في نفس الموقع الجغرافي (مثل `aws-eu-central-1` فرانكفورت لقربها من الشرق الأوسط واليمن مع زمن استجابة منخفض).
- وجود قاعدة البيانات والمخزن والدوال في نفس المنطقة يلغي زمن التأخير العابر للمناطق (`Zero Cross-Region Latency`).

---

## 3. تطبيق ترحيلات قاعدة البيانات (Database Migration)
بعد إنشاء مشروع Neon وفرع التطوير `stage-01-dev`:
```bash
psql "$NEON_ADMIN_CONNECTION_URI" -f neon/migrations/001_stage01_dawa_hayah_schema.sql
```
تُنشئ هذه الخطوة الجداول الـ 15 المعتمدة (`body_systems`, `organs`, `diseases`, `active_ingredients`, `content_items`, `content_translations`, `sources`, `citations`, `media_assets`, `content_packs`, `content_pack_files`, `pack_dependencies`, `content_revisions`, `editorial_reviews`, `publication_status`).

---

## 4. تهيئة مخزن الحزم (Neon Object Storage Bucket)
1. أنشئ مخزناً فرعياً (`Branch-Aware Bucket`) باسم `dawa-hayah-public-packs`.
2. ارفع ملفات الحزم التعليمية المرخصة وتأكد من تسجيل بصمة `SHA-256` والحجم الدقيق بالبايت لكل ملف في جدولي `media_assets` و`content_pack_files`.

---

## 5. نشر دوال الـ API (Neon Compute Functions) وربط تطبيق Android
1. انشر المعالج المرجعي `neon/functions/v1-api-handler.ts` على فرع Neon Compute Functions.
2. اختبر نقطة النهاية العامة عبر HTTPS:
   ```bash
   curl -i https://<your-neon-function-domain>/health
   ```
3. ضع رابط الـ HTTPS العام فقط (بدون أي مفاتيح سرية) في حقل `NEON_PUBLIC_API_BASE_URL` داخل `app/build.gradle.kts`، وسيتحول `AppContainer` تلقائياً من `DisabledNeonDataSource` إلى `RetrofitNeonDataSource`.
