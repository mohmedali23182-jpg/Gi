# عقد واجهة برمجة التطبيقات السحابية لـ Neon (Neon API Contract v1)

---

## 1. نظرة عامة على البنية السحابية المعتمدة
تعتمد سحابة «دواء وحياة» حصرياً على منظومة **Neon** المكونة من ثلاث طبقات متكاملة في نفس المنطقة الجغرافية (`Colocated Region`):
1. **Neon Serverless PostgreSQL:** لتخزين الفهارس، المقالات العلمية، التراجم، سجل المصادر، manifests الحزم، وحالات المراجعة التحريرية.
2. **Neon Object Storage (Branch-Aware Buckets):** لتخزين ملفات الحزم القابلة للتنزيل، الصور التشريحية، ومجسمات `GLB/GLTF` المستقبلية مع دعم التفرع (`Copy-on-Write Branching`).
3. **Neon Compute Functions:** طبقة الـ API الآمنة عبر HTTPS التي تفصل تطبيق Android عن بيانات اعتماد قاعدة البيانات ومخازن الملفات.

---

## 2. المسارات المعتمدة (Endpoints v1)

| المسار | الطريقة | الوظيفة | المصادقة والوصول |
| :--- | :--- | :--- | :--- |
| `/health` | `GET` | فحص جاهزية دالة Neon واتصال PostgreSQL وObject Storage | عام (HTTPS) |
| `/v1/catalog?locale=ar` | `GET` | جلب رقم المراجعة الحالي وإجمالي العناصر المنشورة وقائمة الحزم المتاحة | عام (HTTPS) |
| `/v1/systems?locale=ar` | `GET` | جلب قائمة أجهزة جسم الإنسان المنشورة | عام (HTTPS) |
| `/v1/content/{id}?locale=ar` | `GET` | جلب محتوى مقال أو درس منشور حسب معرفه القانوني (`canonicalId`) | عام (HTTPS) |
| `/v1/packs?scope=organ&scopeId=...` | `GET` | جلب الحزم المتاحة لنطاق محدد (`system`, `organ`, `disease`, `medicine`, `model3d`) | عام (HTTPS) |
| `/v1/packs/{id}/manifest` | `GET` | جلب ملف الـ Manifest الموقع رقمياً ببصمات `SHA-256` وأحجام الملفات والتبعيات | عام (HTTPS) |
| `/v1/assets/{id}/access` | `GET` | الحصول على رابط تنزيل آمن (`public_read` للأصول التعليمية العامة أو `presigned_url` محدد المدة) | عام / مقيد بالجلسة |
| `/v1/updates?fromRevision=...` | `GET` | جلب قائمة المعرفات والحزم التي تغيرت منذ مراجعة محددة | عام (HTTPS) |

---

## 3. قواعد الأمان وحجب المسودات
- تلتزم جميع استعلامات SQL داخل **Neon Compute Functions** بالربط مع جدول `publication_status` وشرط `WHERE is_publicly_visible = TRUE`.
- لا يُسمح بإرجاع أي سجل يحمل حالة `DRAFT` أو `IN_EDITORIAL_REVIEW` إلى تطبيق Android العام.
- جدول `editorial_reviews` مخصص للتدقيق الداخلي على الخادم ولا يُكشف عبر نقاط النهاية العامة.
- المواصفة الكاملة بصيغة OpenAPI 3.1 متوفرة في الملف: `neon/openapi/dawa-hayah-neon-v1.openapi.yaml`.
