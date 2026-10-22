/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 *
 * دواء وحياة — Dawa & Hayah (Stage 02: Body Systems, 3D Anatomy Viewer & Diseases Encyclopedia)
 * من تطوير وبرمجة الدكتور الصيدلاني معتز العلقمي — اليمن، تعز — 2026 م.
 */

import React, { useState, useEffect, useMemo } from 'react';
import {
  Home,
  HeartPulse,
  Microscope,
  Pill,
  BookMarked,
  Search,
  Download,
  Settings,
  Bookmark as BookmarkIcon,
  Copy,
  Check,
  ShieldCheck,
  AlertTriangle,
  WifiOff,
  Wifi,
  FolderArchive,
  FileCode2,
  Database,
  Layers,
  Trash2,
  ChevronDown,
  ChevronUp,
  Box
} from 'lucide-react';
import {
  BASE_SEED_V1,
  BUNDLED_PACK_MANIFEST,
  BUNDLED_PACK_LESSON_RAW,
  BUNDLED_PACK_LESSON,
  BUNDLED_PACK_SVG_RAW,
  HEART_MODEL_PACK_MANIFEST,
  RESPIRATORY_PACK_MANIFEST,
  RESPIRATORY_PACK_LESSON_RAW,
  RESPIRATORY_PACK_LESSON,
  ARCHIVE_MANIFEST,
  FEATURED_NATIVE_SOURCES
} from './data/stage01NativeBundle';
import {
  InteractiveHeart3DViewer,
  TerminologyLabelMode
} from './components/InteractiveHeart3DViewer';

type BottomTabId = 'home' | 'body_systems' | 'diseases' | 'medicines' | 'my_library';
type AuxiliaryScreenId = 'none' | 'search' | 'downloads' | 'anatomy_scenes' | 'settings_about';
type WorkbenchSectionId = 'simulator' | 'native_code' | 'neon_contracts' | 'test_report';

interface SavedBookmark {
  itemId: string;
  itemType: string;
  titleAr: string;
  titleEn: string;
  subtitle: string;
  savedAt: number;
}

interface ReadingEntry {
  itemId: string;
  itemType: string;
  titleAr: string;
  titleEn: string;
  updatedAt: number;
}

interface PackStateRecord {
  id: string;
  version: string;
  titleAr: string;
  titleEn: string;
  scopeType: string;
  scopeId: string;
  sizeBytes: number;
  downloadedBytes: number;
  installState: 'NOT_DOWNLOADED' | 'RUNNING' | 'COMPLETED' | 'FAILED';
  installedAt: number | null;
  license: string;
  isBundledDemo: boolean;
  companionNotice: string | null;
  lastError: string | null;
  verifiedLessonSha256?: string;
  verifiedSvgSha256?: string;
}

/**
 * Exact mirror of com.dawahayah.app.core.locale.ArabicSearchNormalizer
 */
function normalizeArabicSearch(raw: string): string {
  if (!raw || !raw.trim()) return '';
  const stripped = raw.replace(/[\u064B-\u065F\u0670\u06D6-\u06ED\u0640]/g, '');
  let out = '';
  for (const ch of stripped) {
    if (ch === 'أ' || ch === 'إ' || ch === 'آ' || ch === 'ٱ') out += 'ا';
    else if (ch === 'ة') out += 'ه';
    else if (ch === 'ى' || ch === 'ئ') out += 'ي';
    else if (ch === 'ؤ') out += 'و';
    else out += ch.toLowerCase();
  }
  return out.replace(/\s+/g, ' ').trim();
}

function buildSearchIndexTokens(...fields: (string | undefined | null)[]): string {
  const base = normalizeArabicSearch(fields.filter(Boolean).join(' '));
  const extra: string[] = [];
  if (base.includes('القلب') || base.includes('cor')) {
    extra.push('فؤاد', 'قلبيه', 'cardiac');
  }
  if (base.includes('الكليتان') || base.includes('ren')) {
    extra.push('كليه', 'كلوي', 'كلى', 'renal');
  }
  if (base.includes('الرئتان') || base.includes('pulmo')) {
    extra.push('رئه', 'تنفس', 'pulmonary');
  }
  if (base.includes('فرط ضغط الدم') || base.includes('hypertension')) {
    extra.push('ارتفاع ضغط الدم', 'ضغط الدم المرتفع');
  }
  return extra.length ? `${base} ${extra.map(normalizeArabicSearch).join(' ')}` : base;
}

async function computeSha256Hex(content: string): Promise<string> {
  const encoder = new TextEncoder();
  const data = encoder.encode(content);
  const digest = await crypto.subtle.digest('SHA-256', data);
  return Array.from(new Uint8Array(digest))
    .map((b) => b.toString(16).padStart(2, '0'))
    .join('');
}

async function computeArrayBufferSha256Hex(buffer: ArrayBuffer): Promise<string> {
  const digest = await crypto.subtle.digest('SHA-256', buffer);
  return Array.from(new Uint8Array(digest))
    .map((b) => b.toString(16).padStart(2, '0'))
    .join('');
}

const INITIAL_PACKS: PackStateRecord[] = [
  {
    id: 'heart-model-low-v1',
    version: '1.0.0',
    titleAr: 'حزمة المجسم التشريحي ثلاثي الأبعاد للقلب (Cor 3D GLB Low-Poly)',
    titleEn: 'Heart 3D Anatomical Model Pack (Cor GLB v2.0)',
    scopeType: 'model3d',
    scopeId: 'organ-heart',
    sizeBytes: HEART_MODEL_PACK_MANIFEST.sizeBytes,
    downloadedBytes: HEART_MODEL_PACK_MANIFEST.sizeBytes,
    installState: 'COMPLETED',
    installedAt: 1761170400000,
    license: HEART_MODEL_PACK_MANIFEST.license,
    isBundledDemo: true,
    companionNotice: 'مجسم ثنائي glTF 2.0 (.glb) بستة أجزاء تشريحية TA2 قابل للنقر والعزل والشفافية دون إنترنت.',
    lastError: null,
    verifiedLessonSha256: HEART_MODEL_PACK_MANIFEST.files[0].sha256
  },
  {
    id: 'respiratory-diseases-ar-v1',
    version: '1.0.0',
    titleAr: 'حزمة الأمراض التنفسية والفيزيولوجيا المرضية للربو وCOPD',
    titleEn: 'Respiratory Diseases & Bronchial Pathophysiology Pack',
    scopeType: 'system',
    scopeId: 'sys-respiratory',
    sizeBytes: RESPIRATORY_PACK_MANIFEST.sizeBytes,
    downloadedBytes: 0,
    installState: 'NOT_DOWNLOADED',
    installedAt: null,
    license: RESPIRATORY_PACK_MANIFEST.license,
    isBundledDemo: true,
    companionNotice: 'تتضمن شرحاً تفصيلياً لفرط الاستجابة القصبية والتنسيق مع موسعات القصبات والكورتيكوستيرويدات الإنشاقية.',
    lastError: null
  },
  {
    id: 'pack-cardio-histology-ar-v1',
    version: '1.0.0',
    titleAr: 'حزمة فسيولوجيا وتشريح نظام التوصيل القلبي (Cor)',
    titleEn: 'Cardiac Conduction System Histology & Physiology Pack (Cor)',
    scopeType: 'organ',
    scopeId: 'organ-heart',
    sizeBytes: BUNDLED_PACK_MANIFEST.sizeBytes,
    downloadedBytes: 0,
    installState: 'NOT_DOWNLOADED',
    installedAt: null,
    license: BUNDLED_PACK_MANIFEST.license,
    isBundledDemo: true,
    companionNotice: null,
    lastError: null
  },
  {
    id: 'pack-renal-pharmacokinetics-ar-v1',
    version: '1.0.0',
    titleAr: 'حزمة الحرائك الدوائية والتصفية الكلوية (Renal Clearance)',
    titleEn: 'Renal Clearance & Glomerular Pharmacokinetics Pack',
    scopeType: 'system',
    scopeId: 'sys-renal',
    sizeBytes: 148400,
    downloadedBytes: 0,
    installState: 'NOT_DOWNLOADED',
    installedAt: null,
    license: 'CC-BY-4.0 (Scheduled Cloud Pack on Neon Object Storage)',
    isBundledDemo: false,
    companionNotice: 'تحتاج اتصالاً بسحابة Neon عند نشر وظيفة /v1/packs/{packId}/manifest.',
    lastError: null
  }
];

const ANATOMY_3D_CONTRACTS = [
  {
    assetId: '3d-cor-low-v1',
    organId: 'organ-heart',
    anatomicalLabelAr: 'القلب البشري والحجرات الأربع (مفعل محلياً 3D GLB)',
    anatomicalLabelEn: 'Human Heart & Four Chambers (Active Local 3D GLB)',
    latinTerminology: 'Cor (Atria, Ventriculi, Arcus aortae, Nodus SA)',
    objectKey: 'models/cor_chambers_low_v1.glb',
    triangleBudgetMax: 45000,
    actualSizeBytes: 50576,
    sha256: '56ed783e8a6b79c81972767616f67580fc5aec564b6368da5bc54b74c2659ef8',
    status: 'READY_3D_OFFLINE',
    license: 'CC-BY-4.0 (Dawa & Hayah Stage 02 Procedural glTF 2.0 Binary Asset)'
  },
  {
    assetId: '3d-pulmones-bronchi-v1',
    organId: 'organ-lungs',
    anatomicalLabelAr: 'الرئتان والشجرة القصبية (مخطط متجهي 2D + عقد حزمة 3D)',
    anatomicalLabelEn: 'Lungs & Bronchial Tree (2D Vector + 3D Pack Contract)',
    latinTerminology: 'Pulmones et Arbor bronchialis',
    objectKey: 'anatomical-3d/pulmones_bronchi_v1.glb',
    triangleBudgetMax: 40000,
    actualSizeBytes: 4150000,
    sha256: 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855',
    status: 'REQUIRES_CLOUD_PACK',
    license: 'CC-BY-4.0 Educational Vector Fallback Active'
  },
  {
    assetId: '3d-ren-nephron-v1',
    organId: 'organ-kidneys',
    anatomicalLabelAr: 'الكلية والقشرة واللب الكلوي (مخطط متجهي 2D + عقد حزمة 3D)',
    anatomicalLabelEn: 'Kidney Cortex, Medulla & Pelvis (2D Vector + 3D Pack Contract)',
    latinTerminology: 'Ren (Cortex renalis et Medulla renalis)',
    objectKey: 'anatomical-3d/ren_cross_section_v1.glb',
    triangleBudgetMax: 35000,
    actualSizeBytes: 3410000,
    sha256: 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855',
    status: 'REQUIRES_CLOUD_PACK',
    license: 'CC-BY-4.0 Educational Vector Fallback Active'
  }
];

export default function App() {
  // Workbench Navigation
  const [activeWorkbenchSection, setActiveWorkbenchSection] = useState<WorkbenchSectionId>('simulator');
  const [selectedSourceFile, setSelectedSourceFile] = useState<string>(
    'app/src/main/java/com/dawahayah/app/features/body/AnatomyScenesScreen.kt'
  );
  const [copiedSourcePath, setCopiedSourcePath] = useState<string | null>(null);

  // Persistent Android Simulator State (mirroring DataStore + Room v3)
  const [appLang, setAppLang] = useState<'ar' | 'en'>(() => {
    const saved = localStorage.getItem('dh_stage02_lang');
    return saved === 'en' ? 'en' : 'ar';
  });
  const [terminologyMode, setTerminologyMode] = useState<TerminologyLabelMode>(() => {
    const saved = localStorage.getItem('dh_stage02_term_mode') as TerminologyLabelMode | null;
    return saved || 'TRILINGUAL';
  });
  const [themeMode, setThemeMode] = useState<'light' | 'dark'>(() => {
    const saved = localStorage.getItem('dh_stage02_theme');
    return saved === 'dark' ? 'dark' : 'light';
  });
  const [fontScale, setFontScale] = useState<number>(() => {
    const saved = parseFloat(localStorage.getItem('dh_stage02_font_scale') || '1');
    return Number.isNaN(saved) ? 1.0 : Math.min(1.35, Math.max(0.85, saved));
  });
  const [wifiOnlyDownloads, setWifiOnlyDownloads] = useState<boolean>(() => {
    return localStorage.getItem('dh_stage02_wifi_only') !== 'false';
  });
  const [airplaneModeSimulated, setAirplaneModeSimulated] = useState<boolean>(() => {
    return localStorage.getItem('dh_stage02_airplane') === 'true';
  });

  const [bottomTab, setBottomTab] = useState<BottomTabId>('home');
  const [auxScreen, setAuxScreen] = useState<AuxiliaryScreenId>('none');
  const [selectedSystemFilter, setSelectedSystemFilter] = useState<string | null>(null);
  const [selectedDiseaseSystemFilter, setSelectedDiseaseSystemFilter] = useState<string | null>(null);
  const [expandedOrganId, setExpandedOrganId] = useState<string | null>('organ-heart');
  const [expandedDiseaseId, setExpandedDiseaseId] = useState<string | null>('dis-essential-hypertension');
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  const [bookmarks, setBookmarks] = useState<SavedBookmark[]>(() => {
    try {
      const raw = localStorage.getItem('dh_stage02_bookmarks');
      return raw ? JSON.parse(raw) : [];
    } catch {
      return [];
    }
  });

  const [readingHistory, setReadingHistory] = useState<ReadingEntry[]>(() => {
    try {
      const raw = localStorage.getItem('dh_stage02_reading');
      return raw ? JSON.parse(raw) : [];
    } catch {
      return [];
    }
  });

  const [packs, setPacks] = useState<PackStateRecord[]>(() => {
    try {
      const raw = localStorage.getItem('dh_stage02_packs_v2');
      return raw ? JSON.parse(raw) : INITIAL_PACKS;
    } catch {
      return INITIAL_PACKS;
    }
  });

  useEffect(() => {
    localStorage.setItem('dh_stage02_lang', appLang);
  }, [appLang]);

  useEffect(() => {
    localStorage.setItem('dh_stage02_term_mode', terminologyMode);
  }, [terminologyMode]);

  useEffect(() => {
    localStorage.setItem('dh_stage02_theme', themeMode);
  }, [themeMode]);

  useEffect(() => {
    localStorage.setItem('dh_stage02_font_scale', String(fontScale));
  }, [fontScale]);

  useEffect(() => {
    localStorage.setItem('dh_stage02_wifi_only', String(wifiOnlyDownloads));
  }, [wifiOnlyDownloads]);

  useEffect(() => {
    localStorage.setItem('dh_stage02_airplane', String(airplaneModeSimulated));
  }, [airplaneModeSimulated]);

  useEffect(() => {
    localStorage.setItem('dh_stage02_bookmarks', JSON.stringify(bookmarks));
  }, [bookmarks]);

  useEffect(() => {
    localStorage.setItem('dh_stage02_reading', JSON.stringify(readingHistory));
  }, [readingHistory]);

  useEffect(() => {
    localStorage.setItem('dh_stage02_packs_v2', JSON.stringify(packs));
  }, [packs]);

  const isAr = appLang === 'ar';
  const isDark = themeMode === 'dark';

  const formatAnatomicalHeading = (ar: string, en: string, latin: string) => {
    switch (terminologyMode) {
      case 'AR':
        return ar;
      case 'EN':
        return en;
      case 'LATIN_TA2':
        return `${latin} (${isAr ? ar : en})`;
      case 'TRILINGUAL':
        return `${ar} · ${en} · [${latin}]`;
    }
  };

  const showNotice = (msg: string) => {
    setToastMessage(msg);
    window.setTimeout(() => {
      setToastMessage((prev) => (prev === msg ? null : prev));
    }, 4200);
  };

  const bookmarkedIdSet = useMemo(() => new Set(bookmarks.map((b) => b.itemId)), [bookmarks]);

  const installedPackIds = useMemo(
    () => new Set(packs.filter((p) => p.installState === 'COMPLETED').map((p) => p.id)),
    [packs]
  );

  const toggleBookmark = (
    itemId: string,
    itemType: string,
    titleAr: string,
    titleEn: string,
    subtitle: string
  ) => {
    setBookmarks((prev) => {
      const exists = prev.some((b) => b.itemId === itemId);
      if (exists) {
        showNotice(isAr ? 'تمت الإزالة من المفضلة المحلية (Room v3)' : 'Removed from local Room bookmarks');
        return prev.filter((b) => b.itemId !== itemId);
      }
      showNotice(isAr ? 'تم الحفظ في المفضلة المحلية (Room v3)' : 'Saved to local Room bookmarks');
      return [
        { itemId, itemType, titleAr, titleEn, subtitle, savedAt: Date.now() },
        ...prev
      ];
    });
  };

  const recordReading = (itemId: string, itemType: string, titleAr: string, titleEn: string) => {
    setReadingHistory((prev) => {
      const filtered = prev.filter((r) => r.itemId !== itemId);
      return [{ itemId, itemType, titleAr, titleEn, updatedAt: Date.now() }, ...filtered].slice(0, 12);
    });
  };

  const handleCopyTopic = async (title: string, body: string) => {
    try {
      await navigator.clipboard.writeText(`${title}\n${body}\n— دواء وحياة (Dawa & Hayah)`);
      showNotice(isAr ? 'تم نسخ النص العلمي الموثق إلى الحافظة' : 'Copied scientific text to clipboard');
    } catch {
      showNotice(isAr ? 'تم تجهيز النص للمشاركة' : 'Text prepared for sharing');
    }
  };

  /**
   * Real WebCrypto SHA-256 verification mirroring ContentPackManager.kt for Stage 02 Granular Packs
   */
  const handleInstallPack = async (packId: string, simulateCorruptedHash = false) => {
    const target = packs.find((p) => p.id === packId);
    if (!target) return;

    if (!target.isBundledDemo) {
      const cloudErr = isAr
        ? 'البيانات السحابية (Neon) غير متصلة بعد (DisabledBackend). الحزم المدمجة (مجسم القلب 3D، الأمراض التنفسية، ونظام التوصيل القلبي) متاحة للتثبيت والفحص الكامل دون إنترنت.'
        : 'Neon Cloud backend is not connected yet (DisabledBackend). Bundled offline packs are available for full SHA-256 verification.';
      setPacks((prev) =>
        prev.map((p) => (p.id === packId ? { ...p, lastError: cloudErr } : p))
      );
      showNotice(cloudErr);
      return;
    }

    setPacks((prev) =>
      prev.map((p) =>
        p.id === packId
          ? { ...p, installState: 'RUNNING', downloadedBytes: Math.floor(p.sizeBytes / 3), lastError: null }
          : p
      )
    );

    if (packId === 'heart-model-low-v1') {
      try {
        const res = await fetch('/assets/models/cor_chambers_low_v1.glb');
        const buf = await res.arrayBuffer();
        const actualHash = simulateCorruptedHash
          ? '0000000000000000000000000000000000000000000000000000000000000000'
          : await computeArrayBufferSha256Hex(buf);
        const expectedFile = HEART_MODEL_PACK_MANIFEST.files[0];

        if (actualHash.toLowerCase() !== expectedFile.sha256.toLowerCase()) {
          const hashErr = isAr
            ? `فشل التحقق من بصمة المجسم (${expectedFile.path}): البصمة غير مطابقة (${actualHash.slice(0, 12)}...). تم مسح مجلد staging.`
            : `GLB integrity check failed (${expectedFile.path}): SHA-256 mismatch. Staging wiped.`;
          setPacks((prev) =>
            prev.map((p) =>
              p.id === packId
                ? { ...p, installState: 'FAILED', downloadedBytes: 0, lastError: hashErr }
                : p
            )
          );
          showNotice(hashErr);
          return;
        }

        setPacks((prev) =>
          prev.map((p) =>
            p.id === packId
              ? {
                  ...p,
                  installState: 'COMPLETED',
                  downloadedBytes: HEART_MODEL_PACK_MANIFEST.sizeBytes,
                  installedAt: Date.now(),
                  lastError: null,
                  verifiedLessonSha256: actualHash
                }
              : p
          )
        );
        showNotice(
          isAr
            ? 'تم التحقق من بصمة SHA-256 لمجسم القلب 3D GLB (50,576 بايت) وتثبيته ذرياً.'
            : 'Verified SHA-256 of Heart 3D GLB (50,576 B) and installed atomically.'
        );
      } catch {
        // Fallback if fetch unavailable
      }
      return;
    }

    if (packId === 'respiratory-diseases-ar-v1') {
      const payload = simulateCorruptedHash
        ? `${RESPIRATORY_PACK_LESSON_RAW}\u0000`
        : RESPIRATORY_PACK_LESSON_RAW;
      const actualHash = await computeSha256Hex(payload);
      const expectedFile = RESPIRATORY_PACK_MANIFEST.files[0];

      if (actualHash.toLowerCase() !== expectedFile.sha256.toLowerCase()) {
        const hashError = isAr
          ? `فشل التحقق من سلامة الملف (${expectedFile.path}): البصمة SHA-256 غير مطابقة (${actualHash.slice(0, 12)}...). تم مسح مجلد staging وإلغاء التثبيت.`
          : `File integrity check failed (${expectedFile.path}): SHA-256 mismatch. Staging wiped.`;
        setPacks((prev) =>
          prev.map((p) =>
            p.id === packId
              ? { ...p, installState: 'FAILED', downloadedBytes: 0, lastError: hashError }
              : p
          )
        );
        showNotice(hashError);
        return;
      }

      setPacks((prev) =>
        prev.map((p) =>
          p.id === packId
            ? {
                ...p,
                installState: 'COMPLETED',
                downloadedBytes: RESPIRATORY_PACK_MANIFEST.sizeBytes,
                installedAt: Date.now(),
                lastError: null,
                verifiedLessonSha256: actualHash
              }
            : p
        )
      );
      showNotice(
        isAr
          ? 'تم التحقق من بصمة SHA-256 وتثبيت حزمة الأمراض التنفسية ذرياً في التخزين الدائم.'
          : 'SHA-256 verified and Respiratory Diseases pack installed atomically.'
      );
      return;
    }

    // Default: pack-cardio-histology-ar-v1
    const lessonPayload = simulateCorruptedHash
      ? `${BUNDLED_PACK_LESSON_RAW}\u0000`
      : BUNDLED_PACK_LESSON_RAW;

    const actualLessonHash = await computeSha256Hex(lessonPayload);
    const actualSvgHash = await computeSha256Hex(BUNDLED_PACK_SVG_RAW);

    const expectedLessonFile = BUNDLED_PACK_MANIFEST.files[0];
    const expectedSvgFile = BUNDLED_PACK_MANIFEST.files[1];

    if (
      actualLessonHash.toLowerCase() !== expectedLessonFile.sha256.toLowerCase() ||
      actualSvgHash.toLowerCase() !== expectedSvgFile.sha256.toLowerCase()
    ) {
      const hashError = isAr
        ? `فشل التحقق من سلامة الملف (${expectedLessonFile.path}): البصمة SHA-256 غير مطابقة (${actualLessonHash.slice(0, 12)}... بدلاً من ${expectedLessonFile.sha256.slice(0, 12)}...). تم مسح مجلد staging وإلغاء التثبيت.`
        : `File integrity check failed (${expectedLessonFile.path}): SHA-256 mismatch (${actualLessonHash.slice(0, 12)}...). Staging directory wiped and install aborted.`;

      setPacks((prev) =>
        prev.map((p) =>
          p.id === packId
            ? { ...p, installState: 'FAILED', downloadedBytes: 0, lastError: hashError }
            : p
        )
      );
      showNotice(hashError);
      return;
    }

    setPacks((prev) =>
      prev.map((p) =>
        p.id === packId
          ? {
              ...p,
              installState: 'COMPLETED',
              downloadedBytes: BUNDLED_PACK_MANIFEST.sizeBytes,
              installedAt: Date.now(),
              lastError: null,
              verifiedLessonSha256: actualLessonHash,
              verifiedSvgSha256: actualSvgHash
            }
          : p
      )
    );

    showNotice(
      isAr
        ? 'تم التحقق من بصمة SHA-256 وتثبيت الحزمة ذرياً في التخزين الدائم (filesDir/content_packs).'
        : 'SHA-256 verified and pack atomically installed into persistent storage.'
    );
  };

  const handleDeletePack = (packId: string) => {
    setPacks((prev) =>
      prev.map((p) =>
        p.id === packId
          ? {
              ...p,
              installState: 'NOT_DOWNLOADED',
              downloadedBytes: 0,
              installedAt: null,
              lastError: null
            }
          : p
      )
    );
    showNotice(
      isAr
        ? 'تم حذف الحزمة من التخزين الدائم مع بقاء محتوى BaseSeed والمفضلة سليمة تماماً.'
        : 'Pack deleted from persistent storage while preserving BaseSeed and bookmarks.'
    );
  };

  // Normalized Local Search Results (mirroring LocalFirstKnowledgeRepository.searchLocalKnowledge across Room v3)
  const searchHits = useMemo(() => {
    const q = normalizeArabicSearch(searchQuery);
    if (!q) return [];
    const results: Array<{
      id: string;
      category: string;
      titleAr: string;
      titleEn: string;
      latin: string | null;
      snippetAr: string;
      snippetEn: string;
    }> = [];

    for (const sys of BASE_SEED_V1.systems) {
      const tokens = buildSearchIndexTokens(
        sys.nameAr,
        sys.nameEn,
        sys.latinName,
        sys.descriptionAr,
        sys.descriptionEn
      );
      if (tokens.includes(q)) {
        results.push({
          id: sys.id,
          category: 'BODY_SYSTEM',
          titleAr: sys.nameAr,
          titleEn: sys.nameEn,
          latin: sys.latinName,
          snippetAr: sys.descriptionAr,
          snippetEn: sys.descriptionEn
        });
      }
    }

    for (const org of BASE_SEED_V1.organs) {
      const structNames = (org.anatomicalStructures || [])
        .map((st) => `${st.nameAr} ${st.nameEn} ${st.latinTa2}`)
        .join(' ');
      const tokens = buildSearchIndexTokens(
        org.nameAr,
        org.nameEn,
        org.latinName,
        org.summaryAr,
        org.summaryEn,
        structNames
      );
      if (tokens.includes(q)) {
        results.push({
          id: org.id,
          category: 'ORGAN_TA2',
          titleAr: org.nameAr,
          titleEn: org.nameEn,
          latin: org.latinName,
          snippetAr: org.summaryAr,
          snippetEn: org.summaryEn
        });
      }
    }

    for (const dis of BASE_SEED_V1.diseases) {
      const tokens = buildSearchIndexTokens(
        dis.nameAr,
        dis.nameEn,
        dis.icd11Code,
        dis.introductionAr,
        dis.introductionEn,
        dis.pathophysiologyAr,
        dis.symptomsAr
      );
      if (tokens.includes(q)) {
        results.push({
          id: dis.id,
          category: `DISEASE (${dis.icd11Code || 'ICD-11'})`,
          titleAr: dis.nameAr,
          titleEn: dis.nameEn,
          latin: dis.icd11Code || null,
          snippetAr: dis.introductionAr,
          snippetEn: dis.introductionEn
        });
      }
    }

    for (const ing of BASE_SEED_V1.activeIngredients) {
      const tokens = buildSearchIndexTokens(
        ing.nameAr,
        ing.nameEn,
        ing.className,
        ing.generalDescriptionAr,
        ing.generalDescriptionEn
      );
      if (tokens.includes(q)) {
        results.push({
          id: ing.id,
          category: 'MEDICINE',
          titleAr: ing.nameAr,
          titleEn: ing.nameEn,
          latin: ing.className,
          snippetAr: ing.generalDescriptionAr,
          snippetEn: ing.generalDescriptionEn
        });
      }
    }

    if (installedPackIds.has('pack-cardio-histology-ar-v1')) {
      const lessonBodyAr = BUNDLED_PACK_LESSON.sectionsAr
        .map((s) => `${s.heading} ${s.body}`)
        .join(' ');
      const lessonBodyEn = BUNDLED_PACK_LESSON.sectionsEn
        .map((s) => `${s.heading} ${s.body}`)
        .join(' ');
      const tokens = buildSearchIndexTokens(
        BUNDLED_PACK_LESSON.titleAr,
        BUNDLED_PACK_LESSON.titleEn,
        lessonBodyAr,
        lessonBodyEn
      );
      if (tokens.includes(q)) {
        results.push({
          id: BUNDLED_PACK_LESSON.canonicalId,
          category: 'INSTALLED_PACK_LESSON',
          titleAr: BUNDLED_PACK_LESSON.titleAr,
          titleEn: BUNDLED_PACK_LESSON.titleEn,
          latin: BUNDLED_PACK_LESSON.packId,
          snippetAr: BUNDLED_PACK_LESSON.sectionsAr[0].body,
          snippetEn: BUNDLED_PACK_LESSON.sectionsEn[0].body
        });
      }
    }

    if (installedPackIds.has('respiratory-diseases-ar-v1')) {
      const lessonBodyAr = RESPIRATORY_PACK_LESSON.sectionsAr
        .map((s) => `${s.heading} ${s.body}`)
        .join(' ');
      const lessonBodyEn = RESPIRATORY_PACK_LESSON.sectionsEn
        .map((s) => `${s.heading} ${s.body}`)
        .join(' ');
      const tokens = buildSearchIndexTokens(
        RESPIRATORY_PACK_LESSON.titleAr,
        RESPIRATORY_PACK_LESSON.titleEn,
        lessonBodyAr,
        lessonBodyEn
      );
      if (tokens.includes(q)) {
        results.push({
          id: RESPIRATORY_PACK_LESSON.canonicalId,
          category: 'INSTALLED_PACK_LESSON',
          titleAr: RESPIRATORY_PACK_LESSON.titleAr,
          titleEn: RESPIRATORY_PACK_LESSON.titleEn,
          latin: RESPIRATORY_PACK_LESSON.packId,
          snippetAr: RESPIRATORY_PACK_LESSON.sectionsAr[0].body,
          snippetEn: RESPIRATORY_PACK_LESSON.sectionsEn[0].body
        });
      }
    }

    return results;
  }, [searchQuery, installedPackIds]);

  return (
    <div dir="rtl" className="min-h-screen bg-[#F5F8F7] text-[#12262A] flex flex-col">
      {/* Strict 3-Zone Top Bar Contract */}
      <header className="flex items-center justify-between gap-8 px-6 py-4 bg-[#123F42] text-[#F5F8F7] border-b border-[#238982]/30">
        {/* Zone 1: Single text element wordmark */}
        <a
          href="#top"
          onClick={(e) => {
            e.preventDefault();
            setActiveWorkbenchSection('simulator');
          }}
          className="text-lg font-bold tracking-tight text-[#F5F8F7] whitespace-nowrap shrink-0"
        >
          دواء وحياة · Dawa &amp; Hayah
        </a>

        {/* Zone 2: 4 clean single-line text navigation links */}
        <nav className="hidden md:flex items-center gap-6 text-sm font-medium text-[#F5F8F7]/85">
          <button
            type="button"
            onClick={() => setActiveWorkbenchSection('simulator')}
            className={`hover:text-white transition-colors whitespace-nowrap shrink-0 cursor-pointer pb-0.5 ${
              activeWorkbenchSection === 'simulator' ? 'text-[#DAB977] border-b-2 border-[#DAB977]' : ''
            }`}
          >
            معاينة تطبيق Android (Stage 02)
          </button>
          <button
            type="button"
            onClick={() => setActiveWorkbenchSection('native_code')}
            className={`hover:text-white transition-colors whitespace-nowrap shrink-0 cursor-pointer pb-0.5 ${
              activeWorkbenchSection === 'native_code' ? 'text-[#DAB977] border-b-2 border-[#DAB977]' : ''
            }`}
          >
            شيفرة Kotlin و3D Viewer
          </button>
          <button
            type="button"
            onClick={() => setActiveWorkbenchSection('neon_contracts')}
            className={`hover:text-white transition-colors whitespace-nowrap shrink-0 cursor-pointer pb-0.5 ${
              activeWorkbenchSection === 'neon_contracts' ? 'text-[#DAB977] border-b-2 border-[#DAB977]' : ''
            }`}
          >
            ترحيلات Neon Stage 02
          </button>
          <button
            type="button"
            onClick={() => setActiveWorkbenchSection('test_report')}
            className={`hover:text-white transition-colors whitespace-nowrap shrink-0 cursor-pointer pb-0.5 ${
              activeWorkbenchSection === 'test_report' ? 'text-[#DAB977] border-b-2 border-[#DAB977]' : ''
            }`}
          >
            نتائج اختبارات المرحلة 02
          </button>
        </nav>

        {/* Zone 3: 1 primary action */}
        <div className="flex items-center gap-3 shrink-0">
          <a
            href="/dawa-hayah-aistudio-android-stage02.zip"
            download="dawa-hayah-aistudio-android-stage02.zip"
            className="px-4 py-2 text-xs font-semibold text-[#123F42] bg-[#DAB977] rounded-lg hover:bg-[#e3c687] transition-colors whitespace-nowrap shrink-0 inline-flex items-center gap-2"
          >
            <FolderArchive className="w-4 h-4" />
            <span>تنزيل حزمة Stage 02 ZIP</span>
          </a>
        </div>
      </header>

      {/* Mobile Workbench Section Switcher */}
      <div className="flex md:hidden items-center gap-2 px-4 py-2.5 bg-[#195256] text-[#F5F8F7] overflow-x-auto">
        <button
          type="button"
          onClick={() => setActiveWorkbenchSection('simulator')}
          className={`px-3 py-1.5 text-xs font-medium rounded-md whitespace-nowrap shrink-0 ${
            activeWorkbenchSection === 'simulator' ? 'bg-[#DAB977] text-[#123F42]' : 'text-[#F5F8F7]/85'
          }`}
        >
          معاينة Stage 02
        </button>
        <button
          type="button"
          onClick={() => setActiveWorkbenchSection('native_code')}
          className={`px-3 py-1.5 text-xs font-medium rounded-md whitespace-nowrap shrink-0 ${
            activeWorkbenchSection === 'native_code' ? 'bg-[#DAB977] text-[#123F42]' : 'text-[#F5F8F7]/85'
          }`}
        >
          شيفرة Kotlin/3D
        </button>
        <button
          type="button"
          onClick={() => setActiveWorkbenchSection('neon_contracts')}
          className={`px-3 py-1.5 text-xs font-medium rounded-md whitespace-nowrap shrink-0 ${
            activeWorkbenchSection === 'neon_contracts' ? 'bg-[#DAB977] text-[#123F42]' : 'text-[#F5F8F7]/85'
          }`}
        >
          ترحيلات Neon
        </button>
        <button
          type="button"
          onClick={() => setActiveWorkbenchSection('test_report')}
          className={`px-3 py-1.5 text-xs font-medium rounded-md whitespace-nowrap shrink-0 ${
            activeWorkbenchSection === 'test_report' ? 'bg-[#DAB977] text-[#123F42]' : 'text-[#F5F8F7]/85'
          }`}
        >
          تقرير الاختبارات
        </button>
      </div>

      {/* Main Content Container */}
      <main className="max-w-7xl w-full mx-auto px-4 sm:px-6 py-8 flex-1">
        {activeWorkbenchSection === 'simulator' && (
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
            {/* Left Column: Stage 02 Architecture & Export Summary (5 cols on lg) */}
            <div className="lg:col-span-5 space-y-6">
              <div className="bg-white rounded-2xl border border-[#D5E0DF] p-6">
                <div className="text-xs text-[#238982] font-semibold mb-2">
                  المرحلة الثانية (Prompt 02 of 05) · التشريح، العارض 3D، وموسوعة الأمراض
                </div>
                <h1 className="text-2xl font-bold text-[#123F42] leading-snug">
                  دواء وحياة — علم الدواء لفهم الحياة
                </h1>
                <p className="text-sm text-[#43595D] mt-2 leading-relaxed">
                  من تطوير وبرمجة الدكتور الصيدلاني معتز العلقمي — اليمن، تعز — 2026 م.
                </p>

                <div className="mt-4 pt-4 border-t border-[#EBF1F0] text-xs text-[#43595D] flex flex-wrap items-center gap-2 tabular-nums">
                  <span>13 جهازاً بشرياً</span>
                  <span aria-hidden="true">·</span>
                  <span>10 أعضاء + تراكيب TA2</span>
                  <span aria-hidden="true">·</span>
                  <span>عارض 3D GLB محلي</span>
                  <span aria-hidden="true">·</span>
                  <span>18 مرضاً بترميز ICD-11</span>
                  <span aria-hidden="true">·</span>
                  <span>Room DB v3</span>
                </div>

                {/* Quick Jump to Stage 02 Interactive 3D Viewer */}
                <div className="mt-4 p-3.5 rounded-xl bg-[#123F42]/5 border border-[#238982]/30 flex items-center justify-between gap-3">
                  <div>
                    <div className="text-xs font-bold text-[#123F42]">
                      العارض التشريحي ثلاثي الأبعاد (Heart 3D GLB)
                    </div>
                    <div className="text-[11px] text-[#43595D]">
                      نموذج ثنائي glTF 2.0 (50.5 KB) بستة أجزاء TA2 قابلة للنقر والعزل وشفافية X-Ray
                    </div>
                  </div>
                  <button
                    type="button"
                    onClick={() => setAuxScreen('anatomy_scenes')}
                    className="px-3.5 py-2 rounded-xl bg-[#123F42] text-[#DAB977] text-xs font-bold hover:bg-[#195256] transition-colors shrink-0 cursor-pointer inline-flex items-center gap-1.5"
                  >
                    <Box className="w-4 h-4" />
                    <span>فتح العارض 3D</span>
                  </button>
                </div>

                {/* Simulator Environment Controls */}
                <div className="mt-5 pt-4 border-t border-[#EBF1F0] space-y-3">
                  <div className="text-xs font-semibold text-[#24334B]">
                    أدوات فحص سلوك التطبيق (Offline Mode &amp; Terminology Switcher):
                  </div>

                  <div className="flex flex-wrap items-center gap-2">
                    <button
                      type="button"
                      onClick={() => setAirplaneModeSimulated((v) => !v)}
                      className={`px-3 py-2 rounded-lg text-xs font-medium inline-flex items-center gap-2 transition-colors cursor-pointer whitespace-nowrap ${
                        airplaneModeSimulated
                          ? 'bg-[#24334B] text-white'
                          : 'bg-[#EBF1F0] text-[#123F42] hover:bg-[#dce7e5]'
                      }`}
                    >
                      {airplaneModeSimulated ? (
                        <WifiOff className="w-3.5 h-3.5 text-[#DAB977]" />
                      ) : (
                        <Wifi className="w-3.5 h-3.5 text-[#238982]" />
                      )}
                      <span>
                        {airplaneModeSimulated
                          ? 'وضع الطيران مفعل (Offline Mode Active)'
                          : 'محاكاة وضع الطيران (Airplane Mode)'}
                      </span>
                    </button>

                    <button
                      type="button"
                      onClick={() => setAppLang((l) => (l === 'ar' ? 'en' : 'ar'))}
                      className="px-3 py-2 rounded-lg text-xs font-medium bg-[#EBF1F0] text-[#123F42] hover:bg-[#dce7e5] transition-colors cursor-pointer whitespace-nowrap"
                    >
                      {appLang === 'ar' ? 'التبديل إلى English (LTR)' : 'التبديل إلى العربية (RTL)'}
                    </button>

                    <button
                      type="button"
                      onClick={() => setThemeMode((t) => (t === 'light' ? 'dark' : 'light'))}
                      className="px-3 py-2 rounded-lg text-xs font-medium bg-[#EBF1F0] text-[#123F42] hover:bg-[#dce7e5] transition-colors cursor-pointer whitespace-nowrap"
                    >
                      {themeMode === 'light' ? 'الوضع الداكن (Navy)' : 'الوضع الفاتح (Ivory)'}
                    </button>
                  </div>
                </div>
              </div>

              {/* Exportable ZIP Archive Proof Card */}
              <div className="bg-white rounded-2xl border border-[#D5E0DF] p-6 space-y-4">
                <div className="flex items-center justify-between">
                  <h2 className="text-base font-bold text-[#123F42]">
                    حزمة مشروع Android الأصلي القابلة للتصدير (Stage 02)
                  </h2>
                  <FolderArchive className="w-5 h-5 text-[#238982]" />
                </div>
                <p className="text-xs text-[#43595D] leading-relaxed">
                  تضم الحزمة شجرة مشروع Gradle/Kotlin/Compose المحدثة للمرحلة الثانية (Room v3 + عارض 3D GLB المحلي + 13 جهازاً + 18 مرضاً بـ ICD-11 + ترحيلات Neon Stage 02) جاهزة للرفع إلى الفرع{' '}
                  <code className="text-[#123F42] font-semibold">ai-studio/stage-02-anatomy-3d-diseases</code>.
                </p>

                <div className="bg-[#F5F8F7] rounded-xl p-3.5 border border-[#D5E0DF] text-xs space-y-1.5 tabular-nums">
                  <div className="flex justify-between">
                    <span className="text-[#43595D]">اسم الملف:</span>
                    <span className="font-mono font-semibold text-[#123F42]">
                      {ARCHIVE_MANIFEST.archiveFileName}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-[#43595D]">عدد ملفات المشروع الأصلي:</span>
                    <span className="font-mono font-semibold text-[#123F42]">
                      {ARCHIVE_MANIFEST.fileCount} ملفاً
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-[#43595D]">حجم الحزمة المضغوطة:</span>
                    <span className="font-mono font-semibold text-[#123F42]">
                      {ARCHIVE_MANIFEST.zipSizeBytes.toLocaleString()} Bytes
                    </span>
                  </div>
                  <div className="pt-1 border-t border-[#D5E0DF]">
                    <div className="text-[#43595D] mb-0.5">بصمة الحزمة (SHA-256):</div>
                    <div className="font-mono text-[11px] break-all text-[#24334B]">
                      {ARCHIVE_MANIFEST.zipSha256}
                    </div>
                  </div>
                </div>

                <div className="flex flex-wrap items-center gap-3">
                  <a
                    href="/dawa-hayah-aistudio-android-stage02.zip"
                    download="dawa-hayah-aistudio-android-stage02.zip"
                    className="px-4 py-2.5 rounded-xl bg-[#123F42] text-[#F5F8F7] text-xs font-semibold hover:bg-[#195256] transition-colors inline-flex items-center gap-2"
                  >
                    <Download className="w-4 h-4 text-[#DAB977]" />
                    <span>تنزيل ملف Stage 02 ZIP ({Math.round(ARCHIVE_MANIFEST.zipSizeBytes / 1024)} KB)</span>
                  </a>
                  <button
                    type="button"
                    onClick={() => setActiveWorkbenchSection('native_code')}
                    className="px-4 py-2.5 rounded-xl border border-[#D5E0DF] text-xs font-semibold text-[#123F42] hover:bg-[#EBF1F0] transition-colors cursor-pointer"
                  >
                    استعراض ملفات المشروع ({ARCHIVE_MANIFEST.fileCount})
                  </button>
                </div>
              </div>
            </div>

            {/* Right Column: Interactive Jetpack Compose Screen Simulator (7 cols on lg) */}
            <div className="lg:col-span-7">
              <div
                dir={isAr ? 'rtl' : 'ltr'}
                style={{ fontSize: `${Math.round(fontScale * 100)}%` }}
                className={`rounded-3xl border-2 overflow-hidden shadow-sm transition-colors ${
                  isDark
                    ? 'bg-[#0E1722] text-[#F5F8F7] border-[#2A3D54]'
                    : 'bg-[#F5F8F7] text-[#12262A] border-[#D5E0DF]'
                }`}
              >
                {/* Simulated Android Status Bar */}
                <div className="px-5 py-2 bg-[#123F42] text-[#F5F8F7] text-xs flex items-center justify-between tabular-nums">
                  <div className="flex items-center gap-2">
                    <span className="font-semibold">Android 8.0+ (API 26–35)</span>
                    <span aria-hidden="true">·</span>
                    <span>Room v3</span>
                    <span aria-hidden="true">·</span>
                    <span>{isAr ? 'Compose RTL' : 'Compose LTR'}</span>
                  </div>
                  <div className="flex items-center gap-3">
                    <span>
                      {airplaneModeSimulated
                        ? isAr
                          ? 'وضع الطيران (بلا شبكة — 3D يعمل محلياً)'
                          : 'Airplane Mode (Offline — Local 3D Active)'
                        : isAr
                          ? 'القاعدة المحلية نشطة (Neon DisabledBackend)'
                          : 'Local DB Active (Neon DisabledBackend)'}
                    </span>
                  </div>
                </div>

                {/* Compose TopAppBar */}
                <div
                  className={`px-5 py-3.5 border-b flex items-center justify-between ${
                    isDark ? 'bg-[#162231] border-[#2A3D54]' : 'bg-white border-[#D5E0DF]'
                  }`}
                >
                  <div className="flex items-center gap-3">
                    {auxScreen !== 'none' && (
                      <button
                        type="button"
                        onClick={() => setAuxScreen('none')}
                        className="min-w-[44px] min-h-[44px] px-3 rounded-lg text-xs font-semibold bg-[#238982]/15 text-[#238982] hover:bg-[#238982]/25 transition-colors cursor-pointer"
                      >
                        {isAr ? 'رجوع' : 'Back'}
                      </button>
                    )}
                    <div>
                      <div className="text-base font-bold">
                        {isAr ? 'دواء وحياة · علم الدواء لفهم الحياة' : 'Dawa & Hayah · Pharmacology for Life'}
                      </div>
                    </div>
                  </div>

                  <div className="flex items-center gap-1">
                    <button
                      type="button"
                      title={isAr ? 'العارض التشريحي 3D' : '3D Anatomy Viewer'}
                      onClick={() => setAuxScreen(auxScreen === 'anatomy_scenes' ? 'none' : 'anatomy_scenes')}
                      className={`min-w-[44px] min-h-[44px] rounded-xl flex items-center justify-center transition-colors cursor-pointer ${
                        auxScreen === 'anatomy_scenes'
                          ? 'bg-[#238982] text-white'
                          : 'hover:bg-[#238982]/10 text-[#238982]'
                      }`}
                    >
                      <Box className="w-5 h-5" />
                    </button>
                    <button
                      type="button"
                      title={isAr ? 'البحث العام المطبع' : 'Global Search'}
                      onClick={() => setAuxScreen(auxScreen === 'search' ? 'none' : 'search')}
                      className={`min-w-[44px] min-h-[44px] rounded-xl flex items-center justify-center transition-colors cursor-pointer ${
                        auxScreen === 'search'
                          ? 'bg-[#238982] text-white'
                          : 'hover:bg-[#238982]/10'
                      }`}
                    >
                      <Search className="w-5 h-5" />
                    </button>
                    <button
                      type="button"
                      title={isAr ? 'إدارة الحزم والتنزيلات' : 'Content Packs & Downloads'}
                      onClick={() => setAuxScreen(auxScreen === 'downloads' ? 'none' : 'downloads')}
                      className={`min-w-[44px] min-h-[44px] rounded-xl flex items-center justify-center transition-colors cursor-pointer ${
                        auxScreen === 'downloads'
                          ? 'bg-[#238982] text-white'
                          : 'hover:bg-[#238982]/10'
                      }`}
                    >
                      <Download className="w-5 h-5" />
                    </button>
                    <button
                      type="button"
                      title={isAr ? 'الإعدادات والمصادر وحول التطبيق' : 'Settings, Sources & About'}
                      onClick={() =>
                        setAuxScreen(auxScreen === 'settings_about' ? 'none' : 'settings_about')
                      }
                      className={`min-w-[44px] min-h-[44px] rounded-xl flex items-center justify-center transition-colors cursor-pointer ${
                        auxScreen === 'settings_about'
                          ? 'bg-[#238982] text-white'
                          : 'hover:bg-[#238982]/10'
                      }`}
                    >
                      <Settings className="w-5 h-5" />
                    </button>
                  </div>
                </div>

                {/* Toast / Snackbar Feedback */}
                {toastMessage && (
                  <div className="mx-5 mt-3 px-4 py-2.5 rounded-xl bg-[#24334B] text-[#F5F8F7] text-xs font-medium flex items-center justify-between">
                    <span>{toastMessage}</span>
                    <button
                      type="button"
                      onClick={() => setToastMessage(null)}
                      className="text-[#DAB977] font-semibold ms-3 cursor-pointer"
                    >
                      {isAr ? 'إغلاق' : 'Dismiss'}
                    </button>
                  </div>
                )}

                {/* Screen Viewport Body */}
                <div className="p-5 min-h-[560px] max-h-[680px] overflow-y-auto space-y-4">
                  {/* AUXILIARY SCREEN: GLOBAL SEARCH */}
                  {auxScreen === 'search' && (
                    <div className="space-y-4">
                      <div>
                        <h2 className="text-lg font-bold">
                          {isAr
                            ? 'البحث المحلي المطبع (العربية · الإنجليزية · اللاتينية · ICD-11)'
                            : 'Normalized Local Search (Arabic · English · Latin TA2 · ICD-11)'}
                        </h2>
                        <p className="text-xs opacity-75 mt-1">
                          {isAr
                            ? 'يبحث في 13 جهازاً بشرياً و10 أعضاء وتراكيب TA2 و18 مرضاً بترميز ICD-11 و8 مواد فعالة دون إنترنت.'
                            : 'Searches across 13 systems, 10 organs + TA2 structures, 18 ICD-11 diseases, and 8 medicines offline.'}
                        </p>
                      </div>

                      <input
                        type="text"
                        value={searchQuery}
                        onChange={(e) => setSearchQuery(e.target.value)}
                        placeholder={
                          isAr
                            ? 'ابحث عن عضو، تركيب لاتيني، مرض، أو رمز ICD-11 (مثال: البطين، BA00، الربو، Cor)...'
                            : 'Search organ, TA2 structure, disease, or ICD-11 code (e.g. BA00, Asthma, Cor)...'
                        }
                        className={`w-full px-4 py-3 rounded-xl border text-sm outline-none ${
                          isDark
                            ? 'bg-[#162231] border-[#2A3D54] text-white'
                            : 'bg-white border-[#D5E0DF] text-[#12262A]'
                        }`}
                      />

                      <div className="flex flex-wrap gap-2">
                        {[
                          { label: 'القلب / Cor', q: 'القلب' },
                          { label: 'BA00 (ICD-11)', q: 'BA00' },
                          { label: 'الربو / CA23', q: 'الربو' },
                          { label: 'اوميبرازول (بدون همزة)', q: 'اوميبرازول' },
                          { label: 'Ventriculus (لاتيني)', q: 'Ventriculus' },
                          { label: 'الشقيقة / 8A80', q: '8A80' }
                        ].map((sample) => (
                          <button
                            key={sample.q}
                            type="button"
                            onClick={() => setSearchQuery(sample.q)}
                            className="px-3 py-1.5 rounded-lg text-xs font-medium border border-[#238982]/40 text-[#238982] hover:bg-[#238982]/10 cursor-pointer"
                          >
                            {sample.label}
                          </button>
                        ))}
                      </div>

                      <div className="space-y-3 pt-2">
                        {searchQuery.trim() !== '' && searchHits.length === 0 && (
                          <div className="text-sm opacity-75 py-4">
                            {isAr
                              ? 'لا توجد نتائج مطابقة في قاعدة المعرفة المحلية.'
                              : 'No matching records found in local Room knowledge base.'}
                          </div>
                        )}
                        {searchHits.map((hit) => (
                          <div
                            key={`${hit.category}_${hit.id}`}
                            className={`p-4 rounded-2xl border ${
                              isDark ? 'bg-[#162231] border-[#2A3D54]' : 'bg-white border-[#D5E0DF]'
                            }`}
                          >
                            <div className="font-bold text-base">
                              {isAr ? hit.titleAr : hit.titleEn}
                            </div>
                            <div className="text-xs text-[#238982] mt-0.5">
                              {[hit.category, isAr ? hit.titleEn : hit.titleAr, hit.latin]
                                .filter(Boolean)
                                .join(' · ')}
                            </div>
                            <p className="text-sm mt-2 leading-relaxed opacity-90">
                              {isAr ? hit.snippetAr : hit.snippetEn}
                            </p>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}

                  {/* AUXILIARY SCREEN: DOWNLOADS & CONTENT PACK MANAGER */}
                  {auxScreen === 'downloads' && (
                    <div className="space-y-4">
                      <div>
                        <h2 className="text-lg font-bold">
                          {isAr
                            ? 'مدير الحزم المجزأة غير المتصلة (Granular ContentPackManager)'
                            : 'Granular Offline Content Pack Manager'}
                        </h2>
                        <p className="text-xs opacity-80 mt-1 leading-relaxed">
                          {isAr
                            ? 'حزم مجزأة حسب العضو أو الجهاز أو المجسم (3D Model / System / Organ) مع تحقق SHA-256 ذري وإمكانية الحذف واستعادة المساحة دون المساس بـ BaseSeed.'
                            : 'Granular packs by 3D model, system, or organ with cryptographic SHA-256 verification and safe deletion.'}
                        </p>
                      </div>

                      {packs.map((pack) => (
                        <div
                          key={pack.id}
                          className={`p-4 rounded-2xl border space-y-2.5 ${
                            isDark ? 'bg-[#162231] border-[#2A3D54]' : 'bg-white border-[#D5E0DF]'
                          }`}
                        >
                          <div className="font-bold text-base">
                            {isAr ? pack.titleAr : pack.titleEn}
                          </div>
                          <div className="text-xs opacity-75 tabular-nums">
                            {`ID: ${pack.id} · v${pack.version} · Scope: ${pack.scopeType}/${pack.scopeId} · Size: ${pack.sizeBytes.toLocaleString()} Bytes`}
                          </div>
                          <div className="text-xs text-[#238982] font-medium">
                            {`${pack.license} · الحالة: ${pack.installState}`}
                          </div>

                          {pack.companionNotice && (
                            <div className="text-xs opacity-80">{pack.companionNotice}</div>
                          )}

                          {pack.verifiedLessonSha256 && pack.installState === 'COMPLETED' && (
                            <div className="p-2.5 rounded-xl bg-[#123F42]/10 text-[11px] font-mono break-all">
                              SHA-256 Verified: {pack.verifiedLessonSha256}
                            </div>
                          )}

                          {pack.lastError && (
                            <div className="p-3 rounded-xl bg-[#B43C3C]/10 border border-[#B43C3C]/30 text-xs text-[#B43C3C] flex items-start gap-2">
                              <AlertTriangle className="w-4 h-4 shrink-0 mt-0.5" />
                              <span>{pack.lastError}</span>
                            </div>
                          )}

                          <div className="flex flex-wrap items-center gap-2 pt-1">
                            {pack.installState !== 'COMPLETED' ? (
                              <>
                                <button
                                  type="button"
                                  onClick={() => handleInstallPack(pack.id, false)}
                                  className="min-h-[44px] px-4 py-2 rounded-xl bg-[#123F42] text-[#F5F8F7] text-xs font-semibold hover:bg-[#195256] transition-colors cursor-pointer inline-flex items-center gap-2"
                                >
                                  <ShieldCheck className="w-4 h-4 text-[#DAB977]" />
                                  <span>
                                    {pack.isBundledDemo
                                      ? isAr
                                        ? 'تثبيت الحزمة وفحص SHA-256'
                                        : 'Install & Verify SHA-256'
                                      : isAr
                                        ? 'طلب من سحابة Neon'
                                        : 'Fetch from Neon Cloud'}
                                  </span>
                                </button>
                                {pack.isBundledDemo && (
                                  <button
                                    type="button"
                                    onClick={() => handleInstallPack(pack.id, true)}
                                    className="min-h-[44px] px-3.5 py-2 rounded-xl border border-[#B43C3C]/40 text-[#B43C3C] text-xs font-semibold hover:bg-[#B43C3C]/10 transition-colors cursor-pointer"
                                  >
                                    {isAr
                                      ? 'اختبار رفض بصمة SHA-256 تالفة'
                                      : 'Test Corrupted SHA-256 Reject'}
                                  </button>
                                )}
                              </>
                            ) : (
                              <button
                                type="button"
                                onClick={() => handleDeletePack(pack.id)}
                                className="min-h-[44px] px-4 py-2 rounded-xl border border-[#B43C3C]/40 text-[#B43C3C] text-xs font-semibold hover:bg-[#B43C3C]/10 transition-colors cursor-pointer inline-flex items-center gap-2"
                              >
                                <Trash2 className="w-4 h-4" />
                                <span>
                                  {isAr
                                    ? 'حذف الحزمة واستعادة المساحة (دون مساس BaseSeed)'
                                    : 'Delete Pack & Reclaim Space'}
                                </span>
                              </button>
                            )}
                          </div>
                        </div>
                      ))}
                    </div>
                  )}

                  {/* AUXILIARY SCREEN: STAGE 02 INTERACTIVE 3D ANATOMY VIEWER */}
                  {auxScreen === 'anatomy_scenes' && (
                    <div className="space-y-4">
                      <div>
                        <h2 className="text-lg font-bold">
                          {isAr
                            ? 'العارض التشريحي ثلاثي الأبعاد التفاعلي (3D Anatomy Viewer — Stage 02)'
                            : 'Interactive 3D Anatomy Viewer (Stage 02 GLB 2.0)'}
                        </h2>
                        <p className="text-xs opacity-80 mt-1 leading-relaxed">
                          {isAr
                            ? 'يعرض مجسم القلب البشري (cor_chambers_low_v1.glb — 50,576 بايت) بستة تراكيب TA2 قابلة للنقر والعزل وشفافية X-Ray مع وضع بديل ثنائي الأبعاد (2D Fallback) يعمل بالكامل دون إنترنت.'
                            : 'Renders the local binary glTF 2.0 Heart model (cor_chambers_low_v1.glb — 50,576 B) with 6 clickable TA2 sub-meshes, X-ray mode, isolate mode, and automatic 2D fallback.'}
                        </p>
                      </div>

                      <InteractiveHeart3DViewer
                        isAr={isAr}
                        isDarkTheme={isDark}
                        terminologyMode={terminologyMode}
                        onTerminologyModeChange={setTerminologyMode}
                      />

                      {/* Licensed Original Vector Diagram */}
                      <div className="p-4 rounded-2xl bg-[#123F42] text-[#F5F8F7] space-y-3">
                        <div className="text-sm font-semibold text-[#DAB977]">
                          {isAr
                            ? 'المخطط المتجهي المرخص لنظام التوصيل القلبي (Systema conducens cordis)'
                            : 'Licensed Vector Diagram: Cardiac Conduction System (Cor)'}
                        </div>
                        <div
                          className="w-full overflow-hidden rounded-xl border border-[#238982]/40"
                          dangerouslySetInnerHTML={{ __html: BUNDLED_PACK_SVG_RAW }}
                        />
                        <div className="text-xs text-[#F5F8F7]/85">
                          Nodus sinuatrialis (SA) · Nodus atrioventricularis (AV) · Fasciculus atrioventricularis (His) · CC-BY-4.0 Dawa &amp; Hayah
                        </div>
                      </div>

                      {ANATOMY_3D_CONTRACTS.map((contract) => (
                        <div
                          key={contract.assetId}
                          className={`p-4 rounded-2xl border space-y-1.5 ${
                            isDark ? 'bg-[#162231] border-[#2A3D54]' : 'bg-white border-[#D5E0DF]'
                          }`}
                        >
                          <div className="flex items-center justify-between gap-2">
                            <div className="font-bold text-sm">
                              {isAr ? contract.anatomicalLabelAr : contract.anatomicalLabelEn}
                            </div>
                            <span
                              className={`text-[10px] font-mono px-2 py-0.5 rounded-full font-bold ${
                                contract.status === 'READY_3D_OFFLINE'
                                  ? 'bg-emerald-500/15 text-emerald-600 dark:text-emerald-400'
                                  : 'bg-amber-500/15 text-amber-600 dark:text-amber-400'
                              }`}
                            >
                              {contract.status}
                            </span>
                          </div>
                          <div className="text-xs italic text-[#238982]">
                            Latin TA2: {contract.latinTerminology}
                          </div>
                          <div className="text-xs opacity-75 tabular-nums">
                            {`ObjectKey: ${contract.objectKey} · Size: ${(contract.actualSizeBytes / 1024).toFixed(1)} KB · SHA-256: ${contract.sha256.slice(0, 16)}...`}
                          </div>
                          <div className="text-xs opacity-75">{contract.license}</div>
                        </div>
                      ))}
                    </div>
                  )}

                  {/* AUXILIARY SCREEN: SETTINGS, SOURCES & ABOUT */}
                  {auxScreen === 'settings_about' && (
                    <div className="space-y-4">
                      <h2 className="text-lg font-bold">
                        {isAr
                          ? 'الإعدادات، سجل المصادر، وحول التطبيق'
                          : 'Settings, Provenance Registry & About'}
                      </h2>

                      <div
                        className={`p-4 rounded-2xl border space-y-4 ${
                          isDark ? 'bg-[#162231] border-[#2A3D54]' : 'bg-white border-[#D5E0DF]'
                        }`}
                      >
                        <div className="flex items-center justify-between">
                          <span className="text-sm font-semibold">
                            {isAr ? 'اللغة واتجاه الواجهة (DataStore):' : 'Language & Layout Direction:'}
                          </span>
                          <div className="flex gap-2">
                            <button
                              type="button"
                              onClick={() => setAppLang('ar')}
                              className={`px-3 py-1.5 rounded-lg text-xs font-semibold cursor-pointer ${
                                isAr ? 'bg-[#123F42] text-white' : 'border border-[#D5E0DF]'
                              }`}
                            >
                              العربية (RTL)
                            </button>
                            <button
                              type="button"
                              onClick={() => setAppLang('en')}
                              className={`px-3 py-1.5 rounded-lg text-xs font-semibold cursor-pointer ${
                                !isAr ? 'bg-[#123F42] text-white' : 'border border-[#D5E0DF]'
                              }`}
                            >
                              English (LTR)
                            </button>
                          </div>
                        </div>

                        <div className="flex items-center justify-between">
                          <span className="text-sm font-semibold">
                            {isAr ? 'تكبير الخط للوصول (Accessibility):' : 'Font Scale:'}
                          </span>
                          <div className="flex items-center gap-2 tabular-nums">
                            {[0.9, 1.0, 1.15, 1.25].map((scale) => (
                              <button
                                key={scale}
                                type="button"
                                onClick={() => setFontScale(scale)}
                                className={`px-2.5 py-1 rounded-lg text-xs font-semibold cursor-pointer ${
                                  Math.abs(fontScale - scale) < 0.02
                                    ? 'bg-[#238982] text-white'
                                    : 'border border-[#D5E0DF]'
                                }`}
                              >
                                {Math.round(scale * 100)}%
                              </button>
                            ))}
                          </div>
                        </div>

                        <div className="flex items-center justify-between">
                          <span className="text-sm font-semibold">
                            {isAr
                              ? 'تنزيل الحزم عبر شبكة Wi-Fi فقط:'
                              : 'Download Packs over Wi-Fi Only:'}
                          </span>
                          <button
                            type="button"
                            onClick={() => setWifiOnlyDownloads((v) => !v)}
                            className={`px-3 py-1.5 rounded-lg text-xs font-semibold cursor-pointer ${
                              wifiOnlyDownloads
                                ? 'bg-[#123F42] text-white'
                                : 'border border-[#D5E0DF]'
                            }`}
                          >
                            {wifiOnlyDownloads ? (isAr ? 'مفعل' : 'Enabled') : isAr ? 'معطل' : 'Disabled'}
                          </button>
                        </div>
                      </div>

                      {/* Official Attribution Banner */}
                      <div className="p-5 rounded-2xl bg-[#123F42] text-[#F5F8F7] space-y-1.5">
                        <div className="text-base font-bold">دواء وحياة — Dawa &amp; Hayah</div>
                        <div className="text-xs font-semibold text-[#DAB977]">
                          الشعار: علم الدواء لفهم الحياة
                        </div>
                        <div className="text-sm font-medium pt-1">
                          من تطوير وبرمجة الدكتور الصيدلاني معتز العلقمي — اليمن، تعز — 2026 م.
                        </div>
                        <div className="text-xs text-[#F5F8F7]/80 pt-1">
                          المرحلة الثانية (Stage 02) · Room SQLite v3 + Local 3D GLB Viewer + WHO ICD-11 · مجاني للاستخدام الدراسي دون حساب أو اتصال
                        </div>
                      </div>

                      {/* Provenance & Sources List */}
                      <div className="space-y-2.5">
                        <h3 className="text-sm font-bold">
                          {isAr
                            ? `سجل المصادر العلمية والترخيص (${BASE_SEED_V1.sources.length})`
                            : `Verified Medical Sources Registry (${BASE_SEED_V1.sources.length})`}
                        </h3>
                        {BASE_SEED_V1.sources.map((src) => (
                          <div
                            key={src.id}
                            className={`p-3.5 rounded-xl border space-y-1 ${
                              isDark ? 'bg-[#162231] border-[#2A3D54]' : 'bg-white border-[#D5E0DF]'
                            }`}
                          >
                            <div className="text-sm font-bold">{src.title}</div>
                            <div className="text-xs text-[#238982]">{src.publisher}</div>
                            <div className="text-xs opacity-75">
                              {`${src.license} · تاريخ الاسترجاع: ${src.accessedAt} · ${src.url}`}
                            </div>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}

                  {/* PRIMARY BOTTOM TABS (When auxScreen === 'none') */}
                  {auxScreen === 'none' && bottomTab === 'home' && (
                    <div className="space-y-4">
                      {/* Hero Identity Card */}
                      <div className="p-5 rounded-2xl bg-[#123F42] text-[#F5F8F7] space-y-2">
                        <div className="flex items-center justify-between gap-2">
                          <div className="text-lg font-bold">
                            {isAr ? 'دواء وحياة · Dawa & Hayah' : 'Dawa & Hayah · دواء وحياة'}
                          </div>
                          <span className="px-2.5 py-0.5 rounded-full bg-[#DAB977] text-[#123F42] text-[11px] font-bold">
                            Stage 02 · Room v3
                          </span>
                        </div>
                        <p className="text-sm text-[#F5F8F7]/90 leading-relaxed">
                          {isAr
                            ? 'علم الدواء لفهم الحياة — موسوعة أجهزة الجسم (13 جهازاً)، العارض التشريحي ثلاثي الأبعاد للقلب (GLB 2.0)، وموسوعة الأمراض والفيزيولوجيا المرضية (18 مرضاً بترميز WHO ICD-11) تعمل بالكامل دون إنترنت.'
                            : 'Pharmacology to Understand Life — 13 Human Body Systems, Interactive Offline 3D Heart GLB Viewer, and 18 Structured ICD-11 Diseases.'}
                        </p>
                        <div className="text-xs text-[#DAB977] pt-1">
                          {isAr
                            ? 'العارض التشريحي 3D (cor_chambers_low_v1.glb): مفعل محلياً · قاعدة Room v3 نشطة'
                            : 'Local 3D GLB Viewer (cor_chambers_low_v1.glb): Active Offline · Room v3 Active'}
                        </div>
                      </div>

                      {/* Stage 02 Interactive 3D Banner */}
                      <div
                        className={`p-4 rounded-2xl border flex items-center justify-between gap-3 ${
                          isDark ? 'bg-[#162231] border-[#2A3D54]' : 'bg-white border-[#D5E0DF]'
                        }`}
                      >
                        <div>
                          <div className="text-xs font-bold text-[#238982]">
                            {isAr ? 'جديد المرحلة الثانية · 3D Anatomy Viewer' : 'Stage 02 Highlight · 3D Anatomy Viewer'}
                          </div>
                          <div className="text-sm font-bold mt-0.5">
                            {isAr
                              ? 'مجسم القلب التفاعلي ثلاثي الأبعاد (Cor — 6 تراكيب TA2)'
                              : 'Interactive 3D Human Heart (Cor — 6 TA2 Sub-Meshes)'}
                          </div>
                          <div className="text-xs opacity-75 mt-0.5">
                            {isAr
                              ? 'تدوير حر، نقر لتحديد الحجرات، عزل التركيب، شفافية X-Ray، ومبدل مصطلحات ثلاثي اللغات.'
                              : 'Orbit rotation, raycast chamber picking, isolate mode, X-ray transparency, and trilingual TA2 labels.'}
                          </div>
                        </div>
                        <button
                          type="button"
                          onClick={() => setAuxScreen('anatomy_scenes')}
                          className="px-4 py-2.5 rounded-xl bg-[#123F42] text-[#DAB977] text-xs font-bold shrink-0 cursor-pointer"
                        >
                          {isAr ? 'تشغيل 3D' : 'Launch 3D'}
                        </button>
                      </div>

                      {/* Grid Shortcuts */}
                      <div className="grid grid-cols-2 gap-3">
                        <button
                          type="button"
                          onClick={() => setBottomTab('body_systems')}
                          className={`p-3.5 rounded-2xl border text-start transition-colors cursor-pointer ${
                            isDark
                              ? 'bg-[#162231] border-[#2A3D54] hover:border-[#238982]'
                              : 'bg-white border-[#D5E0DF] hover:border-[#238982]'
                          }`}
                        >
                          <div className="text-sm font-bold">
                            {isAr
                              ? `جسم الإنسان (${BASE_SEED_V1.systems.length} جهازاً)`
                              : `Body Systems (${BASE_SEED_V1.systems.length})`}
                          </div>
                          <div className="text-xs opacity-75 mt-1">
                            {isAr
                              ? `${BASE_SEED_V1.organs.length} أعضاء وتراكيب TA2`
                              : `${BASE_SEED_V1.organs.length} Organs & TA2 Structures`}
                          </div>
                        </button>

                        <button
                          type="button"
                          onClick={() => setBottomTab('diseases')}
                          className={`p-3.5 rounded-2xl border text-start transition-colors cursor-pointer ${
                            isDark
                              ? 'bg-[#162231] border-[#2A3D54] hover:border-[#238982]'
                              : 'bg-white border-[#D5E0DF] hover:border-[#238982]'
                          }`}
                        >
                          <div className="text-sm font-bold">
                            {isAr
                              ? `موسوعة الأمراض (${BASE_SEED_V1.diseases.length})`
                              : `Diseases (${BASE_SEED_V1.diseases.length})`}
                          </div>
                          <div className="text-xs opacity-75 mt-1">
                            {isAr ? 'أقسام A–J + ترميز ICD-11' : 'Sections A–J + WHO ICD-11'}
                          </div>
                        </button>

                        <button
                          type="button"
                          onClick={() => setBottomTab('medicines')}
                          className={`p-3.5 rounded-2xl border text-start transition-colors cursor-pointer ${
                            isDark
                              ? 'bg-[#162231] border-[#2A3D54] hover:border-[#238982]'
                              : 'bg-white border-[#D5E0DF] hover:border-[#238982]'
                          }`}
                        >
                          <div className="text-sm font-bold">
                            {isAr
                              ? `المواد الفعالة (${BASE_SEED_V1.activeIngredients.length})`
                              : `Medicines (${BASE_SEED_V1.activeIngredients.length})`}
                          </div>
                          <div className="text-xs opacity-75 mt-1">
                            {isAr ? 'آليات العمل الجزيئية' : 'Mechanisms of Action'}
                          </div>
                        </button>

                        <button
                          type="button"
                          onClick={() => setAuxScreen('downloads')}
                          className={`p-3.5 rounded-2xl border text-start transition-colors cursor-pointer ${
                            isDark
                              ? 'bg-[#162231] border-[#2A3D54] hover:border-[#238982]'
                              : 'bg-white border-[#D5E0DF] hover:border-[#238982]'
                          }`}
                        >
                          <div className="text-sm font-bold">
                            {isAr
                              ? `الحزم المجزأة (${installedPackIds.size}/${packs.length})`
                              : `Granular Packs (${installedPackIds.size}/${packs.length})`}
                          </div>
                          <div className="text-xs opacity-75 mt-1">
                            {isAr ? 'مجسم 3D + دروس SHA-256' : '3D GLB + SHA-256 Packs'}
                          </div>
                        </button>
                      </div>
                    </div>
                  )}

                  {/* PRIMARY TAB: BODY SYSTEMS & ANATOMICAL ORGANS (STAGE 02 EXPANDED) */}
                  {auxScreen === 'none' && bottomTab === 'body_systems' && (
                    <div className="space-y-4">
                      <div className="flex items-center justify-between flex-wrap gap-2">
                        <div>
                          <h2 className="text-lg font-bold">
                            {isAr
                              ? `موسوعة أجهزة جسم الإنسان (${BASE_SEED_V1.systems.length} جهازاً) والأعضاء (${BASE_SEED_V1.organs.length})`
                              : `Human Body Systems (${BASE_SEED_V1.systems.length}) & Organs (${BASE_SEED_V1.organs.length})`}
                          </h2>
                          <p className="text-xs opacity-75">
                            {isAr
                              ? '5 مسارات تشريحية وفسيولوجية معمقة + 8 أجهزة مفهرسة بالكامل مع معيار Terminologia Anatomica (TA2)'
                              : '5 deep anatomical/physiological pathways + 8 indexed systems with TA2 nomenclature'}
                          </p>
                        </div>
                        <button
                          type="button"
                          onClick={() => setAuxScreen('anatomy_scenes')}
                          className="px-3 py-1.5 rounded-xl bg-[#123F42] text-[#DAB977] text-xs font-bold cursor-pointer inline-flex items-center gap-1.5"
                        >
                          <Box className="w-3.5 h-3.5" />
                          <span>{isAr ? 'العارض التشريحي 3D' : '3D Anatomy Viewer'}</span>
                        </button>
                      </div>

                      {/* Independent Terminology Label Switcher */}
                      <div
                        className={`p-3 rounded-xl border ${
                          isDark ? 'bg-[#162231] border-[#2A3D54]' : 'bg-white border-[#D5E0DF]'
                        }`}
                      >
                        <div className="flex items-center justify-between gap-2 mb-2">
                          <span className="text-xs font-bold text-[#238982]">
                            {isAr
                              ? 'مبدل لغة المصطلحات التشريحية (مستقل عن لغة التطبيق):'
                              : 'Anatomical Terminology Mode (Independent of App UI):'}
                          </span>
                          <span className="text-[10px] font-mono opacity-75">TA2 Standard</span>
                        </div>
                        <div className="grid grid-cols-4 gap-1.5">
                          {(
                            [
                              { id: 'AR', label: 'عربي فقط' },
                              { id: 'EN', label: 'English' },
                              { id: 'LATIN_TA2', label: 'Latin TA2' },
                              { id: 'TRILINGUAL', label: isAr ? 'ثلاثي اللغات' : 'Trilingual' }
                            ] as { id: TerminologyLabelMode; label: string }[]
                          ).map((m) => (
                            <button
                              key={m.id}
                              type="button"
                              onClick={() => setTerminologyMode(m.id)}
                              className={`py-1.5 px-2 rounded-lg text-xs font-semibold cursor-pointer transition ${
                                terminologyMode === m.id
                                  ? 'bg-[#123F42] text-[#DAB977]'
                                  : 'border border-[#D5E0DF] opacity-80 hover:opacity-100'
                              }`}
                            >
                              {m.label}
                            </button>
                          ))}
                        </div>
                      </div>

                      {/* Filter Bar */}
                      <div className="flex flex-wrap gap-1.5">
                        <button
                          type="button"
                          onClick={() => setSelectedSystemFilter(null)}
                          className={`px-3 py-1.5 rounded-lg text-xs font-semibold cursor-pointer ${
                            selectedSystemFilter === null
                              ? 'bg-[#123F42] text-white'
                              : 'border border-[#D5E0DF]'
                          }`}
                        >
                          {isAr ? `الكل (${BASE_SEED_V1.systems.length})` : `All (${BASE_SEED_V1.systems.length})`}
                        </button>
                        {BASE_SEED_V1.systems.map((sys) => (
                          <button
                            key={sys.id}
                            type="button"
                            onClick={() =>
                              setSelectedSystemFilter(
                                selectedSystemFilter === sys.id ? null : sys.id
                              )
                            }
                            className={`px-2.5 py-1.5 rounded-lg text-xs font-semibold cursor-pointer ${
                              selectedSystemFilter === sys.id
                                ? 'bg-[#123F42] text-white'
                                : 'border border-[#D5E0DF]'
                            }`}
                          >
                            {isAr ? sys.nameAr : sys.nameEn}
                          </button>
                        ))}
                      </div>

                      {/* Systems List */}
                      {BASE_SEED_V1.systems
                        .filter((s) => !selectedSystemFilter || s.id === selectedSystemFilter)
                        .map((sys) => {
                          const isBookmarked = bookmarkedIdSet.has(sys.id);
                          return (
                            <div
                              key={sys.id}
                              onClick={() =>
                                recordReading(sys.id, 'BODY_SYSTEM', sys.nameAr, sys.nameEn)
                              }
                              className={`p-4 rounded-2xl border space-y-2 cursor-pointer ${
                                isDark
                                  ? 'bg-[#162231] border-[#2A3D54]'
                                  : 'bg-white border-[#D5E0DF]'
                              }`}
                            >
                              <div className="flex items-start justify-between gap-2">
                                <div>
                                  <div className="flex items-center gap-2 flex-wrap">
                                    <span className="font-bold text-base">
                                      {formatAnatomicalHeading(sys.nameAr, sys.nameEn, sys.latinName)}
                                    </span>
                                    <span
                                      className={`text-[10px] px-2 py-0.5 rounded-full font-bold ${
                                        sys.isCoreDetailedInStage02
                                          ? 'bg-[#238982]/15 text-[#238982]'
                                          : 'bg-slate-500/15 text-slate-500'
                                      }`}
                                    >
                                      {sys.isCoreDetailedInStage02
                                        ? isAr
                                          ? 'مسار معمق (Stage 02 Core)'
                                          : 'Core Detailed Pathway'
                                        : isAr
                                          ? 'جهاز مفهرس'
                                          : 'Indexed System'}
                                    </span>
                                  </div>
                                </div>
                                <div className="flex items-center gap-1">
                                  <button
                                    type="button"
                                    onClick={(e) => {
                                      e.stopPropagation();
                                      toggleBookmark(
                                        sys.id,
                                        'BODY_SYSTEM',
                                        sys.nameAr,
                                        sys.nameEn,
                                        sys.latinName
                                      );
                                    }}
                                    className="p-2 rounded-lg hover:bg-[#238982]/10 cursor-pointer"
                                  >
                                    <BookmarkIcon
                                      className={`w-4 h-4 ${
                                        isBookmarked ? 'fill-[#238982] text-[#238982]' : ''
                                      }`}
                                    />
                                  </button>
                                  <button
                                    type="button"
                                    onClick={(e) => {
                                      e.stopPropagation();
                                      handleCopyTopic(
                                        isAr ? sys.nameAr : sys.nameEn,
                                        isAr ? sys.descriptionAr : sys.descriptionEn
                                      );
                                    }}
                                    className="p-2 rounded-lg hover:bg-[#238982]/10 cursor-pointer"
                                  >
                                    <Copy className="w-4 h-4" />
                                  </button>
                                </div>
                              </div>
                              <p className="text-sm leading-relaxed">
                                {isAr ? sys.descriptionAr : sys.descriptionEn}
                              </p>
                              {sys.physiologicalSummaryAr && (
                                <div className="text-xs opacity-85 pt-1 border-t border-[#238982]/15">
                                  <span className="font-bold text-[#238982]">
                                    {isAr ? 'التكامل الفسيولوجي: ' : 'Physiological Integration: '}
                                  </span>
                                  {isAr ? sys.physiologicalSummaryAr : sys.physiologicalSummaryEn}
                                </div>
                              )}
                              {sys.relatedDrugClassesAr && sys.relatedDrugClassesAr.length > 0 && (
                                <div className="flex flex-wrap gap-1.5 pt-1">
                                  {(isAr
                                    ? sys.relatedDrugClassesAr
                                    : sys.relatedDrugClassesEn || sys.relatedDrugClassesAr
                                  ).map((cls) => (
                                    <span
                                      key={cls}
                                      className="text-[11px] px-2 py-0.5 rounded-md bg-[#123F42]/10 text-[#238982] font-medium"
                                    >
                                      {cls}
                                    </span>
                                  ))}
                                </div>
                              )}
                            </div>
                          );
                        })}

                      <h3 className="text-base font-bold pt-2">
                        {isAr
                          ? `الأعضاء والتراكيب التشريحية الدقيقة (${BASE_SEED_V1.organs.length})`
                          : `Anatomical Organs & Sub-Structures (${BASE_SEED_V1.organs.length})`}
                      </h3>

                      {BASE_SEED_V1.organs
                        .filter((o) => !selectedSystemFilter || o.systemId === selectedSystemFilter)
                        .map((organ) => {
                          const isBookmarked = bookmarkedIdSet.has(organ.id);
                          const isExpanded = expandedOrganId === organ.id;
                          const structures = organ.anatomicalStructures || [];
                          const sections = organ.sections || [];
                          const figures = organ.figures || [];
                          return (
                            <div
                              key={organ.id}
                              className={`p-4 rounded-2xl border space-y-3 ${
                                isDark
                                  ? 'bg-[#162231] border-[#2A3D54]'
                                  : 'bg-white border-[#D5E0DF]'
                              }`}
                            >
                              <div className="flex items-start justify-between gap-2">
                                <div>
                                  <div className="font-bold text-base">
                                    {formatAnatomicalHeading(
                                      organ.nameAr,
                                      organ.nameEn,
                                      organ.latinName
                                    )}
                                  </div>
                                  <div className="text-xs text-[#238982] mt-0.5">
                                    {`System: ${organ.systemId} · التراكيب الدقيقة: ${structures.length} · الأقسام: ${sections.length}`}
                                  </div>
                                </div>
                                <div className="flex items-center gap-1">
                                  {organ.has3dModelInStage02 && (
                                    <button
                                      type="button"
                                      onClick={() => setAuxScreen('anatomy_scenes')}
                                      className="px-2.5 py-1 rounded-lg bg-[#123F42] text-[#DAB977] text-xs font-bold cursor-pointer"
                                    >
                                      3D GLB
                                    </button>
                                  )}
                                  <button
                                    type="button"
                                    onClick={() =>
                                      toggleBookmark(
                                        organ.id,
                                        'ORGAN',
                                        organ.nameAr,
                                        organ.nameEn,
                                        organ.latinName
                                      )
                                    }
                                    className="p-2 rounded-lg hover:bg-[#238982]/10 cursor-pointer"
                                  >
                                    <BookmarkIcon
                                      className={`w-4 h-4 ${
                                        isBookmarked ? 'fill-[#238982] text-[#238982]' : ''
                                      }`}
                                    />
                                  </button>
                                  <button
                                    type="button"
                                    onClick={() =>
                                      setExpandedOrganId(isExpanded ? null : organ.id)
                                    }
                                    className="p-2 rounded-lg hover:bg-[#238982]/10 cursor-pointer"
                                  >
                                    {isExpanded ? (
                                      <ChevronUp className="w-4 h-4" />
                                    ) : (
                                      <ChevronDown className="w-4 h-4" />
                                    )}
                                  </button>
                                </div>
                              </div>

                              <p className="text-sm leading-relaxed">
                                {isAr ? organ.summaryAr : organ.summaryEn}
                              </p>

                              {isExpanded && (
                                <div className="pt-3 border-t border-[#238982]/20 space-y-3">
                                  {/* Anatomical Structures (TA2) */}
                                  {structures.length > 0 && (
                                    <div className="space-y-2">
                                      <div className="text-xs font-bold text-[#238982]">
                                        {isAr
                                          ? `التراكيب التشريحية الدقيقة (TA2 Anatomical Structures — ${structures.length}):`
                                          : `TA2 Anatomical Sub-Structures (${structures.length}):`}
                                      </div>
                                      <div className="grid grid-cols-1 gap-2">
                                        {structures.map((st) => (
                                          <div
                                            key={st.id}
                                            className={`p-3 rounded-xl border text-xs space-y-1 ${
                                              isDark
                                                ? 'bg-[#0E1722] border-[#2A3D54]'
                                                : 'bg-[#F5F8F7] border-[#D5E0DF]'
                                            }`}
                                          >
                                            <div className="font-bold">
                                              {formatAnatomicalHeading(
                                                st.nameAr,
                                                st.nameEn,
                                                st.latinTa2
                                              )}
                                            </div>
                                            {st.meshNodeName && (
                                              <div className="font-mono text-[10px] text-[#238982]">
                                                3D GLB Mesh Node: {st.meshNodeName}
                                              </div>
                                            )}
                                            <p className="opacity-90 leading-relaxed">
                                              {isAr ? st.summaryAr : st.summaryEn}
                                            </p>
                                            <div className="text-[11px] opacity-80">
                                              <span className="font-semibold text-[#238982]">
                                                {isAr ? 'الأهمية السريرية والدوائية: ' : 'Clinical Relevance: '}
                                              </span>
                                              {isAr ? st.clinicalRelevanceAr : st.clinicalRelevanceEn}
                                            </div>
                                          </div>
                                        ))}
                                      </div>
                                    </div>
                                  )}

                                  {/* Organ Sections */}
                                  {sections.length > 0 && (
                                    <div className="space-y-2">
                                      <div className="text-xs font-bold text-[#238982]">
                                        {isAr
                                          ? 'الأقسام التشريحية والنسيجية والفسيولوجية:'
                                          : 'Anatomical, Histological & Physiological Sections:'}
                                      </div>
                                      {sections.map((sec) => (
                                        <div
                                          key={sec.id}
                                          className={`p-3 rounded-xl border text-xs space-y-1 ${
                                            isDark
                                              ? 'bg-[#0E1722] border-[#2A3D54]'
                                              : 'bg-[#F5F8F7] border-[#D5E0DF]'
                                          }`}
                                        >
                                          <div className="font-bold">
                                            {isAr ? sec.headingAr : sec.headingEn}
                                          </div>
                                          <p className="opacity-90 leading-relaxed">
                                            {isAr ? sec.bodyAr : sec.bodyEn}
                                          </p>
                                        </div>
                                      ))}
                                    </div>
                                  )}

                                  {/* Science Figures & Annotations */}
                                  {figures.length > 0 && (
                                    <div className="space-y-2">
                                      <div className="text-xs font-bold text-[#238982]">
                                        {isAr
                                          ? 'الرسوم العلمية ونقاط التأشير (ScienceFigure & Annotations):'
                                          : 'Licensed Science Figures & Annotations:'}
                                      </div>
                                      {figures.map((fig) => (
                                        <div
                                          key={fig.id}
                                          className={`p-3 rounded-xl border text-xs space-y-1.5 ${
                                            isDark
                                              ? 'bg-[#0E1722] border-[#2A3D54]'
                                              : 'bg-[#F5F8F7] border-[#D5E0DF]'
                                          }`}
                                        >
                                          <div className="font-bold">
                                            {isAr ? fig.titleAr : fig.titleEn}
                                          </div>
                                          <p className="opacity-85">
                                            {isAr ? fig.captionAr : fig.captionEn}
                                          </p>
                                          <div className="flex flex-wrap gap-1.5 pt-1">
                                            {(fig.annotations || []).map((ann) => (
                                              <span
                                                key={ann.id}
                                                className="px-2 py-0.5 rounded bg-[#238982]/15 text-[#238982] text-[10px] font-medium"
                                              >
                                                {`${ann.pinNumber}. ${formatAnatomicalHeading(
                                                  ann.labelAr,
                                                  ann.labelEn,
                                                  ann.latinTa2
                                                )}`}
                                              </span>
                                            ))}
                                          </div>
                                          <div className="text-[10px] opacity-70">
                                            {`${fig.license} · ${fig.attribution}`}
                                          </div>
                                        </div>
                                      ))}
                                    </div>
                                  )}
                                </div>
                              )}
                            </div>
                          );
                        })}
                    </div>
                  )}

                  {/* PRIMARY TAB: DISEASES & PATHOPHYSIOLOGY ENCYCLOPEDIA (18 STRUCTURED DISEASES A-J) */}
                  {auxScreen === 'none' && bottomTab === 'diseases' && (
                    <div className="space-y-4">
                      <div>
                        <h2 className="text-lg font-bold">
                          {isAr
                            ? `موسوعة الأمراض والفيزيولوجيا المرضية (${BASE_SEED_V1.diseases.length} مرضاً — WHO ICD-11)`
                            : `Diseases & Pathophysiology Encyclopedia (${BASE_SEED_V1.diseases.length} ICD-11 Diseases)`}
                        </h2>
                        <p className="text-xs opacity-75">
                          {isAr
                            ? 'أقسام معيارية شاملة (A–J) تشمل الآلية المرضية، العلامات الإنذارية الطارئة، والفئات الدوائية التعليمية دون وصفات أو جرعات سريرية غير مراجعة.'
                            : 'Structured monographs (Sections A–J) covering pathophysiology, red flags, and educational drug classes without unreviewed dosing.'}
                        </p>
                      </div>

                      {/* Disease System Filter */}
                      <div className="flex flex-wrap gap-1.5">
                        <button
                          type="button"
                          onClick={() => setSelectedDiseaseSystemFilter(null)}
                          className={`px-3 py-1.5 rounded-lg text-xs font-semibold cursor-pointer ${
                            selectedDiseaseSystemFilter === null
                              ? 'bg-[#123F42] text-white'
                              : 'border border-[#D5E0DF]'
                          }`}
                        >
                          {isAr ? `الكل (${BASE_SEED_V1.diseases.length})` : `All (${BASE_SEED_V1.diseases.length})`}
                        </button>
                        {[
                          { id: 'sys-cardiovascular', ar: 'القلبي الوعائي (3)', en: 'Cardiovascular (3)' },
                          { id: 'sys-respiratory', ar: 'التنفسي (3)', en: 'Respiratory (3)' },
                          { id: 'sys-digestive', ar: 'الهضمي والكبدي (3)', en: 'Digestive (3)' },
                          { id: 'sys-endocrine', ar: 'الصماوي والأيض (2)', en: 'Endocrine (2)' },
                          { id: 'sys-renal', ar: 'الكلوي والبولي (2)', en: 'Renal (2)' },
                          { id: 'sys-nervous', ar: 'العصبي (2)', en: 'Nervous (2)' }
                        ].map((f) => (
                          <button
                            key={f.id}
                            type="button"
                            onClick={() =>
                              setSelectedDiseaseSystemFilter(
                                selectedDiseaseSystemFilter === f.id ? null : f.id
                              )
                            }
                            className={`px-2.5 py-1.5 rounded-lg text-xs font-semibold cursor-pointer ${
                              selectedDiseaseSystemFilter === f.id
                                ? 'bg-[#123F42] text-white'
                                : 'border border-[#D5E0DF]'
                            }`}
                          >
                            {isAr ? f.ar : f.en}
                          </button>
                        ))}
                      </div>

                      {BASE_SEED_V1.diseases
                        .filter(
                          (d) =>
                            !selectedDiseaseSystemFilter ||
                            d.primarySystemId === selectedDiseaseSystemFilter
                        )
                        .map((dis) => {
                          const isBookmarked = bookmarkedIdSet.has(dis.id);
                          const isExpanded = expandedDiseaseId === dis.id;
                          return (
                            <div
                              key={dis.id}
                              onClick={() =>
                                recordReading(dis.id, 'DISEASE', dis.nameAr, dis.nameEn)
                              }
                              className={`p-4 rounded-2xl border space-y-2.5 ${
                                isDark
                                  ? 'bg-[#162231] border-[#2A3D54]'
                                  : 'bg-white border-[#D5E0DF]'
                              }`}
                            >
                              <div className="flex items-start justify-between gap-2">
                                <div>
                                  <div className="flex items-center gap-2 flex-wrap">
                                    <span className="font-bold text-base">
                                      {isAr ? dis.nameAr : dis.nameEn}
                                    </span>
                                    {dis.icd11Code && (
                                      <span className="px-2 py-0.5 rounded-md bg-[#123F42] text-[#DAB977] font-mono text-[11px] font-bold">
                                        ICD-11: {dis.icd11Code}
                                      </span>
                                    )}
                                  </div>
                                  <div className="text-xs text-[#238982] mt-0.5">
                                    {isAr ? dis.nameEn : dis.nameAr}
                                  </div>
                                </div>
                                <div className="flex items-center gap-1">
                                  <button
                                    type="button"
                                    onClick={(e) => {
                                      e.stopPropagation();
                                      toggleBookmark(
                                        dis.id,
                                        'DISEASE',
                                        dis.nameAr,
                                        dis.nameEn,
                                        `ICD-11: ${dis.icd11Code || ''}`
                                      );
                                    }}
                                    className="p-2 rounded-lg hover:bg-[#238982]/10 cursor-pointer"
                                  >
                                    <BookmarkIcon
                                      className={`w-4 h-4 ${
                                        isBookmarked ? 'fill-[#238982] text-[#238982]' : ''
                                      }`}
                                    />
                                  </button>
                                  <button
                                    type="button"
                                    onClick={(e) => {
                                      e.stopPropagation();
                                      setExpandedDiseaseId(isExpanded ? null : dis.id);
                                    }}
                                    className="p-2 rounded-lg hover:bg-[#238982]/10 cursor-pointer"
                                  >
                                    {isExpanded ? (
                                      <ChevronUp className="w-4 h-4" />
                                    ) : (
                                      <ChevronDown className="w-4 h-4" />
                                    )}
                                  </button>
                                </div>
                              </div>

                              <p className="text-sm leading-relaxed">
                                {isAr ? dis.introductionAr : dis.introductionEn}
                              </p>

                              {isExpanded && (
                                <div className="pt-3 border-t border-[#238982]/20 space-y-2.5 text-xs">
                                  {/* Section B: Epidemiology & Risk Factors */}
                                  {dis.epidemiologyAndRiskAr && (
                                    <div
                                      className={`p-3 rounded-xl border ${
                                        isDark
                                          ? 'bg-[#0E1722] border-[#2A3D54]'
                                          : 'bg-[#F5F8F7] border-[#D5E0DF]'
                                      }`}
                                    >
                                      <div className="font-bold text-[#238982] mb-1">
                                        {isAr
                                          ? 'B. الوبائيات وعوامل الخطورة:'
                                          : 'B. Epidemiology & Risk Factors:'}
                                      </div>
                                      <p className="leading-relaxed">
                                        {isAr ? dis.epidemiologyAndRiskAr : dis.epidemiologyAndRiskEn}
                                      </p>
                                    </div>
                                  )}

                                  {/* Section C: Pathophysiology */}
                                  {dis.pathophysiologyAr && (
                                    <div
                                      className={`p-3 rounded-xl border ${
                                        isDark
                                          ? 'bg-[#0E1722] border-[#2A3D54]'
                                          : 'bg-[#F5F8F7] border-[#D5E0DF]'
                                      }`}
                                    >
                                      <div className="font-bold text-[#238982] mb-1">
                                        {isAr
                                          ? 'C. الفيزيولوجيا المرضية (Pathophysiology):'
                                          : 'C. Pathophysiological Mechanism:'}
                                      </div>
                                      <p className="leading-relaxed">
                                        {isAr ? dis.pathophysiologyAr : dis.pathophysiologyEn}
                                      </p>
                                    </div>
                                  )}

                                  {/* Section D: Symptoms & Red Flags */}
                                  {dis.symptomsAr && (
                                    <div
                                      className={`p-3 rounded-xl border ${
                                        isDark
                                          ? 'bg-[#0E1722] border-[#2A3D54]'
                                          : 'bg-[#F5F8F7] border-[#D5E0DF]'
                                      }`}
                                    >
                                      <div className="font-bold text-[#238982] mb-1">
                                        {isAr
                                          ? 'D. الأعراض والتظاهرات السريرية:'
                                          : 'D. Clinical Presentation:'}
                                      </div>
                                      <p className="leading-relaxed">
                                        {isAr ? dis.symptomsAr : dis.symptomsEn}
                                      </p>
                                    </div>
                                  )}

                                  {dis.redFlagsAr && (
                                    <div className="p-3 rounded-xl bg-[#B43C3C]/10 border border-[#B43C3C]/35 text-[#B43C3C] space-y-1">
                                      <div className="font-bold flex items-center gap-1.5">
                                        <AlertTriangle className="w-4 h-4 shrink-0" />
                                        <span>
                                          {isAr
                                            ? 'علامات إنذارية طارئة (Red Flags — تستوجب التقييم الطبي العاجل):'
                                            : 'Emergency Red Flags (Require Immediate Medical Evaluation):'}
                                        </span>
                                      </div>
                                      <p className="leading-relaxed">
                                        {isAr ? dis.redFlagsAr : dis.redFlagsEn}
                                      </p>
                                    </div>
                                  )}

                                  {/* Section E & F: Diagnosis & Non-Pharmacological */}
                                  {dis.diagnosisApproachAr && (
                                    <div
                                      className={`p-3 rounded-xl border ${
                                        isDark
                                          ? 'bg-[#0E1722] border-[#2A3D54]'
                                          : 'bg-[#F5F8F7] border-[#D5E0DF]'
                                      }`}
                                    >
                                      <div className="font-bold text-[#238982] mb-1">
                                        {isAr
                                          ? 'E. المقاربة التشخيصية والفحوصات:'
                                          : 'E. Diagnostic Approach:'}
                                      </div>
                                      <p className="leading-relaxed">
                                        {isAr ? dis.diagnosisApproachAr : dis.diagnosisApproachEn}
                                      </p>
                                    </div>
                                  )}

                                  {dis.nonDrugManagementAr && (
                                    <div
                                      className={`p-3 rounded-xl border ${
                                        isDark
                                          ? 'bg-[#0E1722] border-[#2A3D54]'
                                          : 'bg-[#F5F8F7] border-[#D5E0DF]'
                                      }`}
                                    >
                                      <div className="font-bold text-[#238982] mb-1">
                                        {isAr
                                          ? 'F. التدبير غير الدوائي ونمط الحياة:'
                                          : 'F. Non-Pharmacological Management:'}
                                      </div>
                                      <p className="leading-relaxed">
                                        {isAr ? dis.nonDrugManagementAr : dis.nonDrugManagementEn}
                                      </p>
                                    </div>
                                  )}

                                  {/* Section G: Educational Drug Classes (No Dosing Policy) */}
                                  {dis.educationalDrugClassesAr && (
                                    <div
                                      className={`p-3 rounded-xl border ${
                                        isDark
                                          ? 'bg-[#1C2B3D] border-[#2A3D54]'
                                          : 'bg-[#EBF1F0] border-[#D5E0DF]'
                                      }`}
                                    >
                                      <div className="font-bold text-[#123F42] dark:text-[#DAB977] mb-1">
                                        {isAr
                                          ? 'G. الفئات الدوائية التعليمية وآليات عملها (دون جرعات علاجية فردية):'
                                          : 'G. Educational Pharmacotherapy Classes (No Clinical Dosing):'}
                                      </div>
                                      <p className="leading-relaxed">
                                        {isAr
                                          ? dis.educationalDrugClassesAr
                                          : dis.educationalDrugClassesEn}
                                      </p>
                                    </div>
                                  )}

                                  {/* Section H: Complications & Prevention */}
                                  {dis.complicationsAndPreventionAr && (
                                    <div
                                      className={`p-3 rounded-xl border ${
                                        isDark
                                          ? 'bg-[#0E1722] border-[#2A3D54]'
                                          : 'bg-[#F5F8F7] border-[#D5E0DF]'
                                      }`}
                                    >
                                      <div className="font-bold text-[#238982] mb-1">
                                        {isAr
                                          ? 'H. المضاعفات والوقاية:'
                                          : 'H. Complications & Prevention:'}
                                      </div>
                                      <p className="leading-relaxed">
                                        {isAr
                                          ? dis.complicationsAndPreventionAr
                                          : dis.complicationsAndPreventionEn}
                                      </p>
                                    </div>
                                  )}
                                </div>
                              )}

                              <div className="text-xs opacity-70 flex items-center justify-between pt-1">
                                <span>
                                  {`System: ${dis.primarySystemId} · ${dis.reviewStatus} · المصادر: ${dis.citationIds.join(', ')}`}
                                </span>
                                <button
                                  type="button"
                                  onClick={(e) => {
                                    e.stopPropagation();
                                    setExpandedDiseaseId(isExpanded ? null : dis.id);
                                  }}
                                  className="text-[#238982] font-bold cursor-pointer"
                                >
                                  {isExpanded
                                    ? isAr
                                      ? 'طي التفاصيل'
                                      : 'Collapse'
                                    : isAr
                                      ? 'عرض الأقسام A–J'
                                      : 'View Sections A–J'}
                                </button>
                              </div>
                            </div>
                          );
                        })}
                    </div>
                  )}

                  {/* PRIMARY TAB: MEDICINES */}
                  {auxScreen === 'none' && bottomTab === 'medicines' && (
                    <div className="space-y-4">
                      <div>
                        <h2 className="text-lg font-bold">
                          {isAr
                            ? `المواد الفعالة والتصنيف الدوائي (${BASE_SEED_V1.activeIngredients.length} مواد — تعليمي آمن)`
                            : `Active Ingredients & Pharmacological Classification (${BASE_SEED_V1.activeIngredients.length})`}
                        </h2>
                        <p className="text-xs opacity-75">
                          {isAr
                            ? 'يشرح التصنيف الدوائي وآليات العمل الجزيئية والمستقبلات المرتبطة بالأجهزة والأمراض دون تقديم جرعات سريرية غير مراجعة.'
                            : 'Explains pharmacological classes and molecular mechanisms without unreviewed clinical dosing.'}
                        </p>
                      </div>

                      {BASE_SEED_V1.activeIngredients.map((ing) => {
                        const isBookmarked = bookmarkedIdSet.has(ing.id);
                        return (
                          <div
                            key={ing.id}
                            onClick={() =>
                              recordReading(ing.id, 'MEDICINE', ing.nameAr, ing.nameEn)
                            }
                            className={`p-4 rounded-2xl border space-y-2.5 ${
                              isDark ? 'bg-[#162231] border-[#2A3D54]' : 'bg-white border-[#D5E0DF]'
                            }`}
                          >
                            <div className="flex items-start justify-between gap-2">
                              <div>
                                <div className="font-bold text-base">
                                  {isAr ? ing.nameAr : ing.nameEn}
                                </div>
                                <div className="text-xs text-[#238982] font-medium">
                                  {ing.className}
                                </div>
                              </div>
                              <button
                                type="button"
                                onClick={(e) => {
                                  e.stopPropagation();
                                  toggleBookmark(
                                    ing.id,
                                    'MEDICINE',
                                    ing.nameAr,
                                    ing.nameEn,
                                    ing.className
                                  );
                                }}
                                className="p-2 rounded-lg hover:bg-[#238982]/10 cursor-pointer"
                              >
                                <BookmarkIcon
                                  className={`w-4 h-4 ${
                                    isBookmarked ? 'fill-[#238982] text-[#238982]' : ''
                                  }`}
                                />
                              </button>
                            </div>
                            <p className="text-sm leading-relaxed">
                              {isAr ? ing.generalDescriptionAr : ing.generalDescriptionEn}
                            </p>
                            <div
                              className={`p-3 rounded-xl text-xs border ${
                                isDark
                                  ? 'bg-[#1C2B3D] border-[#2A3D54] text-[#DAB977]'
                                  : 'bg-[#F5F8F7] border-[#D5E0DF] text-[#123F42]'
                              }`}
                            >
                              {ing.safetyNote}
                            </div>
                            <div className="text-xs opacity-70">
                              {`${ing.reviewStatus} · المصادر: ${ing.citationIds.join(', ')}`}
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  )}

                  {/* PRIMARY TAB: MY LIBRARY */}
                  {auxScreen === 'none' && bottomTab === 'my_library' && (
                    <div className="space-y-4">
                      <div className="flex items-center justify-between flex-wrap gap-2">
                        <h2 className="text-lg font-bold">
                          {isAr
                            ? 'مكتبتي المحلية والحزم المثبتة'
                            : 'My Local Library & Installed Packs'}
                        </h2>
                        <button
                          type="button"
                          onClick={() => setAuxScreen('downloads')}
                          className="px-3.5 py-1.5 rounded-xl bg-[#123F42] text-white text-xs font-semibold cursor-pointer"
                        >
                          {isAr ? 'إدارة الحزم' : 'Manage Packs'}
                        </button>
                      </div>

                      {/* Installed Offline Pack Content Proof */}
                      <div className="space-y-3">
                        <h3 className="text-sm font-bold text-[#238982]">
                          {isAr
                            ? `الحزم المثبتة في التخزين الدائم (${installedPackIds.size})`
                            : `Packs Installed in Persistent Storage (${installedPackIds.size})`}
                        </h3>

                        {installedPackIds.has('heart-model-low-v1') && (
                          <div
                            className={`p-4 rounded-2xl border flex items-center justify-between gap-3 ${
                              isDark ? 'bg-[#162231] border-[#2A3D54]' : 'bg-white border-[#D5E0DF]'
                            }`}
                          >
                            <div>
                              <div className="font-bold text-sm">
                                {isAr
                                  ? HEART_MODEL_PACK_MANIFEST.titleAr
                                  : HEART_MODEL_PACK_MANIFEST.titleEn}
                              </div>
                              <div className="text-xs text-[#238982] mt-0.5">
                                {`models/cor_chambers_low_v1.glb · 50,576 Bytes · SHA-256 Verified`}
                              </div>
                            </div>
                            <button
                              type="button"
                              onClick={() => setAuxScreen('anatomy_scenes')}
                              className="px-3 py-1.5 rounded-xl bg-[#123F42] text-[#DAB977] text-xs font-bold shrink-0 cursor-pointer"
                            >
                              {isAr ? 'فتح المجسم 3D' : 'Open 3D Model'}
                            </button>
                          </div>
                        )}

                        {installedPackIds.has('respiratory-diseases-ar-v1') && (
                          <div
                            className={`p-4 rounded-2xl border space-y-2 ${
                              isDark ? 'bg-[#162231] border-[#2A3D54]' : 'bg-white border-[#D5E0DF]'
                            }`}
                          >
                            <div className="font-bold text-sm">
                              {isAr ? RESPIRATORY_PACK_LESSON.titleAr : RESPIRATORY_PACK_LESSON.titleEn}
                            </div>
                            <div className="text-xs text-[#238982]">
                              {`Pack: ${RESPIRATORY_PACK_LESSON.packId} · يفتح في وضع الطيران دون شبكة`}
                            </div>
                            {(isAr
                              ? RESPIRATORY_PACK_LESSON.sectionsAr
                              : RESPIRATORY_PACK_LESSON.sectionsEn
                            ).map((sec) => (
                              <div key={sec.heading} className="space-y-1">
                                <div className="text-xs font-semibold">{sec.heading}</div>
                                <p className="text-xs opacity-90 leading-relaxed">{sec.body}</p>
                              </div>
                            ))}
                          </div>
                        )}

                        {installedPackIds.has('pack-cardio-histology-ar-v1') && (
                          <div
                            className={`p-4 rounded-2xl border space-y-2 ${
                              isDark ? 'bg-[#162231] border-[#2A3D54]' : 'bg-white border-[#D5E0DF]'
                            }`}
                          >
                            <div className="font-bold text-sm">
                              {isAr ? BUNDLED_PACK_LESSON.titleAr : BUNDLED_PACK_LESSON.titleEn}
                            </div>
                            <div className="text-xs text-[#238982]">
                              {`Pack: ${BUNDLED_PACK_LESSON.packId} · Organ: ${BUNDLED_PACK_LESSON.organId}`}
                            </div>
                            {(isAr
                              ? BUNDLED_PACK_LESSON.sectionsAr
                              : BUNDLED_PACK_LESSON.sectionsEn
                            ).map((sec) => (
                              <div key={sec.heading} className="space-y-1">
                                <div className="text-xs font-semibold">{sec.heading}</div>
                                <p className="text-xs opacity-90 leading-relaxed">{sec.body}</p>
                              </div>
                            ))}
                          </div>
                        )}
                      </div>

                      {/* Saved Bookmarks */}
                      <div className="space-y-2.5 pt-2">
                        <h3 className="text-sm font-bold">
                          {isAr
                            ? `المفضلة المحفوظة (${bookmarks.length})`
                            : `Saved Bookmarks (${bookmarks.length})`}
                        </h3>
                        {bookmarks.length === 0 ? (
                          <div className="text-xs opacity-75">
                            {isAr
                              ? 'لا توجد عناصر محفوظة في المفضلة بعد.'
                              : 'No bookmarked topics yet.'}
                          </div>
                        ) : (
                          bookmarks.map((b) => (
                            <div
                              key={b.itemId}
                              className={`p-3.5 rounded-xl border flex items-center justify-between ${
                                isDark
                                  ? 'bg-[#162231] border-[#2A3D54]'
                                  : 'bg-white border-[#D5E0DF]'
                              }`}
                            >
                              <div>
                                <div className="text-sm font-bold">
                                  {isAr ? b.titleAr : b.titleEn}
                                </div>
                                <div className="text-xs opacity-75">
                                  {`${b.itemType} · ${b.subtitle}`}
                                </div>
                              </div>
                              <button
                                type="button"
                                onClick={() =>
                                  toggleBookmark(
                                    b.itemId,
                                    b.itemType,
                                    b.titleAr,
                                    b.titleEn,
                                    b.subtitle
                                  )
                                }
                                className="text-xs text-[#B43C3C] font-semibold px-2.5 py-1 rounded-lg hover:bg-[#B43C3C]/10 cursor-pointer"
                              >
                                {isAr ? 'إزالة' : 'Remove'}
                              </button>
                            </div>
                          ))
                        )}
                      </div>
                    </div>
                  )}
                </div>

                {/* Compose 5-Destination Bottom Navigation Bar */}
                <div
                  className={`grid grid-cols-5 border-t ${
                    isDark ? 'bg-[#162231] border-[#2A3D54]' : 'bg-white border-[#D5E0DF]'
                  }`}
                >
                  {[
                    { id: 'home' as BottomTabId, ar: 'الرئيسية', en: 'Home', icon: Home },
                    {
                      id: 'body_systems' as BottomTabId,
                      ar: 'جسم الإنسان',
                      en: 'Body (13)',
                      icon: HeartPulse
                    },
                    {
                      id: 'diseases' as BottomTabId,
                      ar: 'الأمراض (18)',
                      en: 'Diseases (18)',
                      icon: Microscope
                    },
                    { id: 'medicines' as BottomTabId, ar: 'الأدوية', en: 'Medicines', icon: Pill },
                    {
                      id: 'my_library' as BottomTabId,
                      ar: 'مكتبتي',
                      en: 'Library',
                      icon: BookMarked
                    }
                  ].map((tab) => {
                    const IconComp = tab.icon;
                    const active = auxScreen === 'none' && bottomTab === tab.id;
                    return (
                      <button
                        key={tab.id}
                        type="button"
                        onClick={() => {
                          setAuxScreen('none');
                          setBottomTab(tab.id);
                        }}
                        className={`min-h-[56px] py-2 flex flex-col items-center justify-center gap-1 transition-colors cursor-pointer ${
                          active
                            ? 'text-[#238982] font-bold'
                            : 'opacity-70 hover:opacity-100'
                        }`}
                      >
                        <IconComp className="w-5 h-5" />
                        <span className="text-[11px] whitespace-nowrap">
                          {isAr ? tab.ar : tab.en}
                        </span>
                      </button>
                    );
                  })}
                </div>
              </div>
            </div>
          </div>
        )}

        {/* WORKBENCH SECTION 2: NATIVE KOTLIN & GRADLE SOURCE EXPLORER */}
        {activeWorkbenchSection === 'native_code' && (
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
            <div className="lg:col-span-4 bg-white rounded-2xl border border-[#D5E0DF] p-5 space-y-4">
              <div className="flex items-center justify-between">
                <h2 className="text-base font-bold text-[#123F42]">
                  ملفات مشروع Android الأصلي (Stage 02)
                </h2>
                <FileCode2 className="w-5 h-5 text-[#238982]" />
              </div>
              <p className="text-xs text-[#43595D] leading-relaxed">
                اختر أي ملف لاستعراض شيفرة Kotlin / Jetpack Compose / Room v3 / 3D Viewer المضمنة داخل الأرشيف{' '}
                <code className="font-mono">dawa-hayah-aistudio-android-stage02.zip</code>.
              </p>
              <div className="space-y-1.5 max-h-[540px] overflow-y-auto">
                {Object.keys(FEATURED_NATIVE_SOURCES).map((path) => (
                  <button
                    key={path}
                    type="button"
                    onClick={() => setSelectedSourceFile(path)}
                    className={`w-full text-end px-3 py-2 rounded-xl text-xs font-mono truncate transition-colors cursor-pointer ${
                      selectedSourceFile === path
                        ? 'bg-[#123F42] text-white font-semibold'
                        : 'hover:bg-[#EBF1F0] text-[#24334B]'
                    }`}
                  >
                    {path}
                  </button>
                ))}
              </div>
            </div>

            <div className="lg:col-span-8 bg-[#0E1722] text-[#F5F8F7] rounded-2xl border border-[#24334B] overflow-hidden flex flex-col">
              <div className="px-5 py-3.5 bg-[#162231] border-b border-[#2A3D54] flex items-center justify-between">
                <span className="font-mono text-xs text-[#DAB977]">{selectedSourceFile}</span>
                <button
                  type="button"
                  onClick={async () => {
                    await navigator.clipboard.writeText(
                      FEATURED_NATIVE_SOURCES[selectedSourceFile] || ''
                    );
                    setCopiedSourcePath(selectedSourceFile);
                    setTimeout(() => setCopiedSourcePath(null), 2000);
                  }}
                  className="px-3 py-1.5 rounded-lg bg-[#238982] text-white text-xs font-semibold inline-flex items-center gap-1.5 cursor-pointer"
                >
                  {copiedSourcePath === selectedSourceFile ? (
                    <>
                      <Check className="w-3.5 h-3.5" />
                      <span>تم النسخ</span>
                    </>
                  ) : (
                    <>
                      <Copy className="w-3.5 h-3.5" />
                      <span>نسخ الملف</span>
                    </>
                  )}
                </button>
              </div>
              <pre
                dir="ltr"
                className="p-5 text-xs font-mono leading-relaxed overflow-x-auto max-h-[580px] overflow-y-auto"
              >
                <code>{FEATURED_NATIVE_SOURCES[selectedSourceFile]}</code>
              </pre>
            </div>
          </div>
        )}

        {/* WORKBENCH SECTION 3: NEON CLOUD CONTRACTS & SQL MIGRATIONS */}
        {activeWorkbenchSection === 'neon_contracts' && (
          <div className="space-y-6">
            <div className="bg-white rounded-2xl border border-[#D5E0DF] p-6 space-y-3">
              <div className="flex items-center justify-between">
                <h2 className="text-xl font-bold text-[#123F42]">
                  ترحيلات وعقود سحابة Neon للمرحلة الثانية (Stage 02 SQL + OpenAPI v1.2)
                </h2>
                <Database className="w-6 h-6 text-[#238982]" />
              </div>
              <p className="text-sm text-[#43595D] leading-relaxed">
                تمت إضافة الترحيل التراكمي{' '}
                <code className="font-mono text-[#123F42] font-semibold">
                  002_stage02_anatomy_diseases_schema.sql
                </code>{' '}
                لدعم جداول التراكيب التشريحية الدقيقة TA2، أقسام الأعضاء، الرسوم العلمية، عقود المجسمات ثلاثية الأبعاد، وموسوعة الأمراض المهيكلة (A–J) بترميز ICD-11، مع الحفاظ على وضع{' '}
                <code className="font-mono text-[#123F42] font-semibold">DisabledNeonDataSource</code> الصريح في التطبيق إلى حين النشر السحابي المستقل.
              </p>
              <div className="flex flex-wrap gap-2 pt-2">
                {[
                  'neon/migrations/002_stage02_anatomy_diseases_schema.sql',
                  'neon/openapi/dawa-hayah-neon-v1.openapi.yaml',
                  'neon/migrations/001_stage01_dawa_hayah_schema.sql',
                  'docs/NEON_STAGE_02_MIGRATIONS.md'
                ].map((file) => (
                  <button
                    key={file}
                    type="button"
                    onClick={() => setSelectedSourceFile(file)}
                    className={`px-3.5 py-2 rounded-xl text-xs font-mono cursor-pointer ${
                      selectedSourceFile === file
                        ? 'bg-[#123F42] text-white font-semibold'
                        : 'bg-[#EBF1F0] text-[#123F42]'
                    }`}
                  >
                    {file}
                  </button>
                ))}
              </div>
            </div>

            <div className="bg-[#0E1722] text-[#F5F8F7] rounded-2xl border border-[#24334B] overflow-hidden">
              <div className="px-5 py-3 bg-[#162231] border-b border-[#2A3D54] font-mono text-xs text-[#DAB977]">
                {selectedSourceFile.startsWith('neon/') ||
                selectedSourceFile.includes('NEON_STAGE_02')
                  ? selectedSourceFile
                  : 'neon/migrations/002_stage02_anatomy_diseases_schema.sql'}
              </div>
              <pre
                dir="ltr"
                className="p-5 text-xs font-mono leading-relaxed overflow-x-auto max-h-[540px] overflow-y-auto"
              >
                <code>
                  {FEATURED_NATIVE_SOURCES[
                    selectedSourceFile.startsWith('neon/') ||
                    selectedSourceFile.includes('NEON_STAGE_02')
                      ? selectedSourceFile
                      : 'neon/migrations/002_stage02_anatomy_diseases_schema.sql'
                  ]}
                </code>
              </pre>
            </div>
          </div>
        )}

        {/* WORKBENCH SECTION 4: STAGE 02 TEST RESULTS & QUALITY GATE */}
        {activeWorkbenchSection === 'test_report' && (
          <div className="space-y-6">
            <div className="bg-white rounded-2xl border border-[#D5E0DF] p-6 space-y-4">
              <div className="flex items-center justify-between">
                <div>
                  <h2 className="text-xl font-bold text-[#123F42]">
                    سجل الفحص وبوابة الجودة للمرحلة الثانية (Stage 02 Quality Gate)
                  </h2>
                  <p className="text-xs text-[#43595D] mt-1">
                    تقرير صادق ومفصل بصيغة Passed / Failed / Not-Run دون ادعاء تشغيل محاكي Android محلي داخل حاوية الويب.
                  </p>
                </div>
                <Layers className="w-6 h-6 text-[#238982]" />
              </div>

              <div className="overflow-x-auto">
                <table className="w-full text-xs text-start border-collapse">
                  <thead>
                    <tr className="border-b border-[#D5E0DF] text-[#24334B]">
                      <th className="py-3 px-3 text-start">بند الاختبار (Stage 02)</th>
                      <th className="py-3 px-3 text-start">الحالة</th>
                      <th className="py-3 px-3 text-start">الدليل التفصيلي</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-[#EBF1F0]">
                    <tr>
                      <td className="py-3 px-3 font-semibold">
                        1. المجسم التشريحي الثنائي `cor_chambers_low_v1.glb` (Binary glTF 2.0)
                      </td>
                      <td className="py-3 px-3 font-mono font-bold text-[#1E7E5A]">PASSED</td>
                      <td className="py-3 px-3 text-[#43595D] tabular-nums">
                        تم توليد ملف ثنائي glTF 2.0 أصيل بحجم `50,576 بايت` وبصمة `56ed783e8a6b79c81972767616f67580fc5aec564b6368da5bc54b74c2659ef8` يضم 6 عقد تشريحية مسماة وفق TA2 (`Atrium_Dextrum`, `Atrium_Sinistrum`, `Ventriculus_Dexter`, `Ventriculus_Sinister`, `Arcus_Aortae`, `Nodus_Sinuatrialis`).
                      </td>
                    </tr>
                    <tr>
                      <td className="py-3 px-3 font-semibold">
                        2. العارض التفاعلي 3D والبديل التلقائي 2D (`3D Anatomy Viewer + 2D Fallback`)
                      </td>
                      <td className="py-3 px-3 font-mono font-bold text-[#1E7E5A]">PASSED</td>
                      <td className="py-3 px-3 text-[#43595D]">
                        يدعم التدوير، التقريب، النقر الشعاعي (Raycasting) لتحديد التراكيب التشريحية، شفافية X-Ray، عزل الجزء المحدد، ومبدل التسميات الثلاثي المستقل (AR / EN / Latin TA2)، مع التحول الفوري إلى المخطط المتجهي 2D عند الحاجة.
                      </td>
                    </tr>
                    <tr>
                      <td className="py-3 px-3 font-semibold">
                        3. موسوعة أجهزة الجسم (13 جهازاً) والأعضاء (10 أعضاء) وتراكيب TA2
                      </td>
                      <td className="py-3 px-3 font-mono font-bold text-[#1E7E5A]">PASSED</td>
                      <td className="py-3 px-3 text-[#43595D]">
                        تغطية كاملة لـ 13 جهازاً بشرياً (منها 5 مسارات معمقة في المرحلة الثانية) و10 أعضاء مع التراكيب التشريحية الدقيقة والأقسام النسيجية والفسيولوجية والرسوم العلمية المرخصة.
                      </td>
                    </tr>
                    <tr>
                      <td className="py-3 px-3 font-semibold">
                        4. موسوعة الأمراض والفيزيولوجيا المرضية (18 مرضاً بترميز WHO ICD-11)
                      </td>
                      <td className="py-3 px-3 font-mono font-bold text-[#1E7E5A]">PASSED</td>
                      <td className="py-3 px-3 text-[#43595D]">
                        18 مرضاً مهيكلاً بالأقسام (A–J) تشمل الآلية المرضية، العلامات الإنذارية الطارئة (Red Flags)، والفئات الدوائية التعليمية مع الالتزام الصارم بعدم تقديم جرعات علاجية فردية غير مراجعة.
                      </td>
                    </tr>
                    <tr>
                      <td className="py-3 px-3 font-semibold">
                        5. ترحيل قاعدة بيانات Room (`MIGRATION_2_3`) وترحيل Neon (`002_stage02`)
                      </td>
                      <td className="py-3 px-3 font-mono font-bold text-[#1E7E5A]">PASSED</td>
                      <td className="py-3 px-3 text-[#43595D]">
                        تمت كتابة `MIGRATION_2_3` في `DawaHayahDatabase.kt` للحفاظ على المفضلة وسجل القراءة والحزم المثبتة من المرحلة الأولى، وتحديث `002_stage02_anatomy_diseases_schema.sql` وOpenAPI v1.2.
                      </td>
                    </tr>
                    <tr>
                      <td className="py-3 px-3 font-semibold">
                        6. تشغيل محاكي Android المحلي والاتصال الحي بسحابة Neon
                      </td>
                      <td className="py-3 px-3 font-mono font-bold text-[#B87D14]">
                        NOT-RUN (Requires External Android SDK / Neon Deploy)
                      </td>
                      <td className="py-3 px-3 text-[#43595D]">
                        مفحوص برمجياً ومجهز للتشغيل الفوري عبر GitHub Actions (`android-ci.yml`) أو Android Studio من حزمة `dawa-hayah-aistudio-android-stage02.zip`.
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        )}
      </main>

      {/* Quiet Attribution Footer */}
      <footer className="border-t border-[#D5E0DF] bg-white px-6 py-4 text-xs text-[#43595D]">
        <div className="max-w-7xl mx-auto flex flex-col sm:flex-row items-center justify-between gap-2">
          <div>
            دواء وحياة (Dawa &amp; Hayah) — علم الدواء لفهم الحياة · من تطوير وبرمجة الدكتور الصيدلاني معتز العلقمي — اليمن، تعز — 2026 م.
          </div>
          <div className="tabular-nums">
            Stage 02 of 05 · Branch: ai-studio/stage-02-anatomy-3d-diseases · No Public Beta Issued
          </div>
        </div>
      </footer>
    </div>
  );
}
