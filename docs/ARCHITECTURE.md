# التوثيق المعماري لتطبيق «دواء وحياة» (Architecture Specification)

---

## 1. المبادئ المعمارية الحاكمة
1. **Local-First, Optional Cloud Content:**
   - القاعدة المحلية **Room/SQLite** هي المصدر الوحيد للحقيقة (`Single Source of Truth`) أثناء تشغيل واجهات Jetpack Compose.
   - يعمل التطبيق بكامل وظائفه الأساسية من أول تشغيل في وضع الطيران (`Airplane Mode`) بالاعتماد على `base_seed_v1.json`.
   - المحتوى السحابي الإضافي يُجلب اختيارياً عبر **Neon Compute Functions HTTPS API** ويُحفظ في التخزين الدائم للتطبيق (`context.filesDir/content_packs`).
2. **Single-Activity, Single-Module Clean Packaging:**
   - نشاط واحد (`MainActivity`) مع `Navigation Compose` و5 وجهات سفلية رئيسية + شاشات مساندة.
   - تنظيم الكود في حزم معزولة داخل وحدة `:app` لسهولة الصيانة والتوسع في المراحل الخمس:
     - `ui/theme`, `ui/components`, `ui/navigation`
     - `features/home`, `features/body`, `features/diseases`, `features/medicines`, `features/library`, `features/search`, `features/downloads`, `features/settings`
     - `domain/model`
     - `data/local`, `data/remote`, `data/repository`
     - `core/locale`, `core/security`, `core/errors`, `core/network`, `core/storage`, `core/contentpacks`

---

## 2. تدفق البيانات وسياسة المستودع (`LocalFirstKnowledgeRepository`)
- جميع استعلامات القراءة تُرجع `Flow<List<T>>` من `KnowledgeDao` على خيوط خلفية، ولا تُجري أي عمليات إدخال/إخراج (`I/O`) على الخيط الرئيسي (`Main Thread`).
- عند البحث عبر `searchLocalKnowledge(query)`، يمر النص المدخل عبر `ArabicSearchNormalizer.normalize(query)` ليطابق العمود المفهرس `normalizedSearchText` في جداول Room الخمسة (`body_systems`, `organs`, `diseases`, `active_ingredients`, `local_contents`).
- المفضلة (`bookmarks`) وسجل تقدم القراءة (`reading_progress`) تُحفظ في جداول Room مستقلة وتبقى محفوظة عند تحديث الحزم أو إعادة تشغيل التطبيق.
- إعدادات المستخدم (اللغة والاتجاه RTL/LTR، الوضع الفاتح/الداكن، تكبير الخط، وسياسة Wi-Fi فقط) تُحفظ في `Jetpack DataStore` (`UserPreferencesDataStore`).

---

## 3. العزل الأمني لطبقة السحابة (Neon Security Boundary)
- لا يحتوي تطبيق Android على أي اتصال مباشر بـ PostgreSQL (`jdbc`/`postgresql://`) ولا يحتوي على مفاتيح S3 (`AWS_SECRET_ACCESS_KEY`) أو `NEON_API_KEY`.
- يتصل التطبيق فقط بـ `NeonApiService` عبر HTTPS (`cleartextTrafficPermitted="false"` في `network_security_config.xml`).
- في حال عدم ضبط عنوان خادم Neon في المرحلة الأولى، يعمل `DisabledNeonDataSource` افتراضياً ويعرض حالة صريحة للمستخدم دون تزييف نتائج الشبكة.
