-- Dawa & Hayah | Stage 03 | Educational draft drug taxonomy
-- Tested on child Neon branch, then populated main appdb with the same class IDs.
-- These are original short class definitions; NO clinical approval, dosing or brand claims.
-- Keep these rows DRAFT until a qualified reviewer approves the associated sources.
BEGIN;
INSERT INTO public.drug_classes(id,name_ar,name_en,description_ar,description_en,source_id,status_code)
VALUES
('class-ccb-dhp', 'حاصرات قنوات الكالسيوم ثنائية الهيدروبيريدين', 'Dihydropyridine calcium channel blockers', 'تثبط دخول الكالسيوم عبر قنوات L-type في العضلات الملساء الوعائية.', 'Inhibit L-type calcium influx into vascular smooth muscle.', 'src-medlineplus-nlm', 'DRAFT'),
('class-beta1-blocker', 'حاصرات مستقبلات بيتا-1 الانتقائية', 'Selective beta-1 adrenergic blockers', 'تقلل تنشيط مستقبلات بيتا-1 الأدرينالية في القلب.', 'Reduce beta-1 adrenergic receptor stimulation in the heart.', 'src-medlineplus-nlm', 'DRAFT'),
('class-sglt2', 'مثبطات ناقل الغلوكوز والصوديوم SGLT2', 'Sodium-glucose cotransporter 2 inhibitors', 'تقلل إعادة امتصاص الغلوكوز في النبيبات القريبة للكلى.', 'Reduce proximal renal tubular glucose reabsorption.', 'src-medlineplus-nlm', 'DRAFT'),
('class-loop', 'مدرات البول العروية', 'Loop diuretics', 'تثبط ناقل الصوديوم والبوتاسيوم والكلوريد في الجزء الصاعد الغليظ من عروة هنلي.', 'Inhibit sodium-potassium-chloride cotransport in the thick ascending limb.', 'src-medlineplus-nlm', 'DRAFT'),
('class-acei', 'مثبطات الإنزيم المحول للأنجيوتنسين', 'ACE inhibitors', 'تثبط نشاط الإنزيم المحول للأنجيوتنسين.', 'Inhibit angiotensin-converting enzyme activity.', 'src-medlineplus-nlm', 'DRAFT'),
('class-biguanide', 'البيغوانيدات', 'Biguanides', 'فئة من خافضات سكر الدم تشمل الميتفورمين.', 'Antihyperglycemic drug class including metformin.', 'src-medlineplus-nlm', 'DRAFT'),
('class-ppi', 'مثبطات مضخة البروتون', 'Proton pump inhibitors', 'تثبط مضخة الهيدروجين والبوتاسيوم في الخلايا الجدارية بالمعدة.', 'Inhibit the gastric parietal cell hydrogen-potassium ATPase.', 'src-medlineplus-nlm', 'DRAFT'),
('class-saba', 'ناهضات مستقبلات بيتا-2 قصيرة المفعول', 'Short-acting beta-2 agonists', 'تنشط مستقبلات بيتا-2 وتساعد على إرخاء العضلات الملساء للشعب الهوائية.', 'Activate beta-2 receptors to relax airway smooth muscle.', 'src-medlineplus-nlm', 'DRAFT')
ON CONFLICT (id) DO NOTHING;
INSERT INTO public.drug_class_ingredients(class_id,ingredient_id)
VALUES
('class-ccb-dhp', 'ing-amlodipine'),
('class-beta1-blocker', 'ing-bisoprolol'),
('class-sglt2', 'ing-dapagliflozin'),
('class-loop', 'ing-furosemide'),
('class-acei', 'ing-lisinopril'),
('class-biguanide', 'ing-metformin'),
('class-ppi', 'ing-omeprazole'),
('class-saba', 'ing-salbutamol')
ON CONFLICT (class_id,ingredient_id) DO NOTHING;
COMMIT;
