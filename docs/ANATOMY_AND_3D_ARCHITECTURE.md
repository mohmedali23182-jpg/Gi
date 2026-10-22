# معمارية التشريح وعارض المجسمات ثلاثية الأبعاد (Anatomy & 3D Viewer Architecture)
## مشروع «دواء وحياة — Dawa & Hayah» — المرحلة الثانية (Stage 02)

---

## 1. القرار الهندسي لعارض 3D على Android 8.0 (`minSdk = 26`)
لتحقيق التوازن بين:
1. دعم أجهزة Android بدءاً من **Android 8.0 (API 26)** والأجهزة الاقتصادية والمتوسطة (2GB–3GB RAM).
2. العمل الكامل دون إنترنت (**100% Offline-First**) دون أي اعتماد على شبكات CDN خارجية.
3. تجنب أعطال بناء NDK/C++ أو تضخم حجم APK الناتج عن محركات الألعاب الثقيلة.

تم اعتماد **معمارية هجينة معزولة (Isolated Local WebGL + Binary glTF 2.0 `.glb` Engine)**:
- تظل كافة شاشات التطبيق وتنقلاته وشريط الأدوات وبطاقات التراكيب التشريحية مبنية بنسبة **100% بـ Jetpack Compose + Material 3**.
- داخل شاشة المشهد ثلاثي الأبعاد (`AnatomyScenesScreen.kt`) فقط، يُستخدم مكون `WebView` محلي معزول تماماً يحمّل الملف المحلي `file:///android_asset/viewer3d/anatomy_viewer.html` ويحقن بايتات ملف `.glb` المحلي الموثق (`models/cor_chambers_low_v1.glb` أو الملف المثبت في `filesDir/content_packs/`) بعد التحقق من بصمة `SHA-256`.

### الضوابط الأمنية الصارمة لـ WebView المحلي:
- `allowFileAccessFromFileURLs = false`
- `allowUniversalAccessFromFileURLs = false`
- `allowContentAccess = false`
- `mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW`
- ترويسة `Content-Security-Policy` تمنع أي اتصال شبكي خارجي (`default-src 'self' 'unsafe-inline' data: blob:`).

---

## 2. هيكل ملف Binary glTF 2.0 (`.glb`) والعقد التشريحية (TA2 Sub-Meshes)
ملف المجسم المحلي `cor_chambers_low_v1.glb` (`50,576 bytes`) يلتزم بمعيار **glTF 2.0 Binary Specification** (ترويسة `glTF` السحرية `0x46546C67` + مقطع `JSON` + مقطع `BIN` ثنائي يضم مصفوفات `POSITION` و `NORMAL` و `INDICES` لكل جزء تشريحي على حدة):

| اسم العقدة في ملف GLB (`meshNodeName`) | الاسم العربي | الاسم الإنجليزي | المصطلح اللاتيني (TA2) | رمز TA2 |
|---|---|---|---|---|
| `Atrium_Dextrum` | الأذين الأيمن | Right Atrium | *Atrium cordis dextrum* | `TA2-3955` |
| `Atrium_Sinistrum` | الأذين الأيسر | Left Atrium | *Atrium cordis sinistrum* | `TA2-3996` |
| `Ventriculus_Dexter` | البطين الأيمن | Right Ventricle | *Ventriculus cordis dexter* | `TA2-3973` |
| `Ventriculus_Sinister` | البطين الأيسر | Left Ventricle | *Ventriculus cordis sinister* | `TA2-4005` |
| `Arcus_Aortae` | قوس الأبهر | Aortic Arch | *Arcus aortae* | `TA2-4116` |
| `Nodus_Sinuatrialis` | العقدة الجيبية الأذينية | Sinoatrial (SA) Node | *Nodus sinuatrialis* | `TA2-4041` |

---

## 3. ميزانيات الأداء ومستويات الجودة (LOD Budgets)

| مستوى الجودة (`QualityLevel`) | الحد الأقصى للمثلثات | الحد الأقصى لحجم الملف | الاستخدام |
|---|---|---|---|
| `LOW_MOBILE` | `≤ 15,000 Triangles` | `≤ 500 KB` | مضمن محلياً للأجهزة الاقتصادية والمتوسطة ويعمل فوراً دون تنزيل سحابي. |
| `STANDARD_EDUCATIONAL` | `≤ 45,000 Triangles` | `≤ 6.5 MB` | حزمة اختيارية قياسية للتفاصيل الصمامية والوعائية. |
| `HIGH_ATLAS` | `≤ 65,000 Triangles` | `≤ 8.5 MB` | حزمة أطلس سحابية عالية الدقة (`heart-model-high-v1` من Z-Anatomy/BodyParts3D). |

---

## 4. خطة التراجع التلقائي (2D Diagram Fallback Strategy)
في حال فشل تهيئة سياق `WebGL` على معالج الرسوميات في جهاز قديم أو محاكي بدون تسريع عتادي، أو عند اختيار المستخدم لوضع التوفير، يتحول `Stage01AnatomyAssetPlayer` و `AnatomyScenesScreen` تلقائياً إلى **المخطط التشريحي المتجهي ثنائي الأبعاد (2D Vector Diagram Fallback)** مع الحفاظ الكامل على اللافتات التفاعلية والشرح التشريحي دون انهيار التطبيق.
