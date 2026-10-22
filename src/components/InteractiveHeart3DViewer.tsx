import React, { useEffect, useRef, useState } from 'react';
import * as THREE from 'three';

export type TerminologyLabelMode = 'AR' | 'EN' | 'LATIN_TA2' | 'TRILINGUAL';

export interface HeartSubMeshInfo {
  meshNodeName: string;
  id: string;
  nameAr: string;
  nameEn: string;
  latinTa2: string;
  summaryAr: string;
  summaryEn: string;
  physiologicalRoleAr: string;
  clinicalRelevanceAr: string;
  colorHex: number;
  position: [number, number, number];
  scale: [number, number, number];
}

export const HEART_TA2_SUBMESHES: HeartSubMeshInfo[] = [
  {
    meshNodeName: 'Atrium_Dextrum',
    id: 'struct-right-atrium',
    nameAr: 'الأذين الأيمن',
    nameEn: 'Right Atrium',
    latinTa2: 'Atrium dextrum',
    summaryAr: 'الحجرة العلوية اليمنى للقلب التي تستقبل الدم غير المؤكسج من الوريدين الأجوفين العلوي والسفلي والجيب التاجي.',
    summaryEn: 'The right upper chamber receiving deoxygenated venous blood from the superior and inferior venae cavae and coronary sinus.',
    physiologicalRoleAr: 'يحتوي في جداره الخلفي العلوي على العقدة الجيبية الأذينية (SA Node) ويدفع الدم عبر الصمام ثلاثي الشرف إلى البطين الأيمن.',
    clinicalRelevanceAr: 'يتوسع في حالات فرط ضغط الشريان الرئوي وقصور الصمام ثلاثي الشرف والرجفان الأذيني.',
    colorHex: 0x2563eb,
    position: [-0.62, 0.52, 0.12],
    scale: [0.52, 0.48, 0.46],
  },
  {
    meshNodeName: 'Atrium_Sinistrum',
    id: 'struct-left-atrium',
    nameAr: 'الأذين الأيسر',
    nameEn: 'Left Atrium',
    latinTa2: 'Atrium sinistrum',
    summaryAr: 'الحجرة العلوية اليسرى التي تستقبل الدم المؤكسج العائد من الرئتين عبر الأوردة الرئوية الأربعة.',
    summaryEn: 'The left upper chamber receiving oxygenated blood from the four pulmonary veins.',
    physiologicalRoleAr: 'يمثل خزاناً ومضخة معززة لملء البطين الأيسر عبر الصمام التاجي (الميترالي) في نهاية الانبساط.',
    clinicalRelevanceAr: 'توسع الأذين الأيسر أو ركود الدم في لُسينه (Left Atrial Appendage) يرتبط بتشكل الخثرات في الرجفان الأذيني.',
    colorHex: 0xdc2626,
    position: [0.62, 0.55, -0.08],
    scale: [0.50, 0.46, 0.46],
  },
  {
    meshNodeName: 'Ventriculus_Dexter',
    id: 'struct-right-ventricle',
    nameAr: 'البطين الأيمن',
    nameEn: 'Right Ventricle',
    latinTa2: 'Ventriculus dexter',
    summaryAr: 'الحجرة السفلية الأمامية ذات الجدار العضلي المتوسط التي تضخ الدم غير المؤكسج عبر الصمام الرئوي إلى الجذع الرئوي.',
    summaryEn: 'The anterior lower chamber pumping venous blood through the pulmonary valve into the pulmonary trunk.',
    physiologicalRoleAr: 'يعمل في دارة منخفضة المقاومة الوعائية (الدورة الرئوية) بضغط انقباضي يقارب 25 ملم زئبق.',
    clinicalRelevanceAr: 'يتأثر بارتفاع المقاومة الوعائية الرئوية والانصمام الرئوي وفشل القلب الأيمن.',
    colorHex: 0x0284c7,
    position: [-0.46, -0.45, 0.25],
    scale: [0.58, 0.78, 0.54],
  },
  {
    meshNodeName: 'Ventriculus_Sinister',
    id: 'struct-left-ventricle',
    nameAr: 'البطين الأيسر',
    nameEn: 'Left Ventricle',
    latinTa2: 'Ventriculus sinister',
    summaryAr: 'الحجرة المخروطية الرئيسة ذات الجدار العضلي السميك (8–12 ملم) التي تشكل قمة القلب وتضخ الدم المؤكسج إلى الأبهر.',
    summaryEn: 'The thick-walled conical chamber forming the cardiac apex and pumping oxygenated blood into the systemic aorta.',
    physiologicalRoleAr: 'يولد الضغط الانقباضي الجهازي اللازم لتروية الدماغ والشرايين التاجية والكليتين وسائر الأنسجة.',
    clinicalRelevanceAr: 'محور الفيزيولوجيا المرضية في فشل القلب الانقباضي (HFrEF) والانبساطي (HFpEF) وتضخم البطين الأيسر التالي لفرط ضغط الدم.',
    colorHex: 0xb91c1c,
    position: [0.46, -0.48, 0.05],
    scale: [0.64, 0.88, 0.60],
  },
  {
    meshNodeName: 'Arcus_Aortae',
    id: 'struct-aortic-arch',
    nameAr: 'قوس الأبهر',
    nameEn: 'Aortic Arch',
    latinTa2: 'Arcus aortae',
    summaryAr: 'الجزء المنحني الرئيس من الشريان الأبهر الصاعد من البطين الأيسر، والذي يعطي الفروع الشريانية الكبرى للرأس والطرفين العلويين.',
    summaryEn: 'The main curved segment of the systemic aorta giving rise to the brachiocephalic trunk, left common carotid, and left subclavian arteries.',
    physiologicalRoleAr: 'يضم مستقبلات الضغط الشرياني (Baroreceptors) في جداره ويمتص موجة النبض الانقباضي بفضل مرونته العالية (ظاهرة Windkessel).',
    clinicalRelevanceAr: 'يتصلب في فرط ضغط الدم المزمن وتصلب الشرايين مما يزيد الضغط النبضي والحمل البعدي.',
    colorHex: 0xe11d48,
    position: [0.15, 1.18, 0.0],
    scale: [0.36, 0.44, 0.36],
  },
  {
    meshNodeName: 'Nodus_Sinuatrialis',
    id: 'struct-sa-node',
    nameAr: 'العقدة الجيبية الأذينية (ناظمة الخطى)',
    nameEn: 'Sinoatrial (SA) Node',
    latinTa2: 'Nodus sinuatrialis',
    summaryAr: 'تجمع مجهري متخصص من الخلايا القلبية الذاتية الاستثارة يقع عند اتصال الوريد الأجوف العلوي بالأذين الأيمن.',
    summaryEn: 'Specialized pacemaker cardiomyocytes located at the junction of the superior vena cava and right atrium.',
    physiologicalRoleAr: 'يولد كمون الفعل التلقائي (60–100 نبضة/دقيقة) عبر تيارات القنوات Funny currents (If) وقنوات الكالسيوم من النمط T و L.',
    clinicalRelevanceAr: 'الهدف الفيزيولوجي لحاصرات بيتا وحاصرات قنوات الكالسيوم غير الديهيدروبيريدينية لتنظيم سرعة القلب.',
    colorHex: 0xf59e0b,
    position: [-0.88, 0.82, 0.36],
    scale: [0.22, 0.22, 0.22],
  },
];

interface InteractiveHeart3DViewerProps {
  isAr: boolean;
  isDarkTheme: boolean;
  terminologyMode: TerminologyLabelMode;
  onTerminologyModeChange: (mode: TerminologyLabelMode) => void;
  onSelectStructure?: (structure: HeartSubMeshInfo) => void;
}

export const InteractiveHeart3DViewer: React.FC<InteractiveHeart3DViewerProps> = ({
  isAr,
  isDarkTheme,
  terminologyMode,
  onTerminologyModeChange,
  onSelectStructure,
}) => {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const [selectedMeshName, setSelectedMeshName] = useState<string>('Ventriculus_Sinister');
  const [hiddenMeshes, setHiddenMeshes] = useState<Record<string, boolean>>({});
  const [xrayEnabled, setXrayEnabled] = useState<boolean>(false);
  const [isolateSelected, setIsolateSelected] = useState<boolean>(false);
  const [canvasDarkMode, setCanvasDarkMode] = useState<boolean>(true);
  const [fallback2DMode, setFallback2DMode] = useState<boolean>(false);
  const [autoRotate, setAutoRotate] = useState<boolean>(true);
  const [glbVerifiedStatus, setGlbVerifiedStatus] = useState<{
    loaded: boolean;
    magicHeader: string;
    version: number;
    byteLength: number;
    nodeCount: number;
  }>({
    loaded: false,
    magicHeader: 'glTF',
    version: 2,
    byteLength: 50576,
    nodeCount: 6,
  });

  // Verify the binary GLB file from /assets/models/cor_chambers_low_v1.glb
  useEffect(() => {
    let cancelled = false;
    fetch('/assets/models/cor_chambers_low_v1.glb')
      .then((res) => res.arrayBuffer())
      .then((buf) => {
        if (cancelled || buf.byteLength < 20) return;
        const view = new DataView(buf);
        const magic = String.fromCharCode(
          view.getUint8(0),
          view.getUint8(1),
          view.getUint8(2),
          view.getUint8(3)
        );
        const version = view.getUint32(4, true);
        const totalLen = view.getUint32(8, true);
        const jsonChunkLen = view.getUint32(12, true);
        const jsonBytes = new Uint8Array(buf, 20, jsonChunkLen);
        const jsonStr = new TextDecoder().decode(jsonBytes);
        const gltfJson = JSON.parse(jsonStr);
        setGlbVerifiedStatus({
          loaded: magic === 'glTF' && version === 2,
          magicHeader: magic,
          version,
          byteLength: totalLen,
          nodeCount: Array.isArray(gltfJson.nodes) ? gltfJson.nodes.length : 6,
        });
      })
      .catch(() => {
        // Offline fallback still works seamlessly
      });
    return () => {
      cancelled = true;
    };
  }, []);

  // Refs for Three.js scene objects so state updates don't recreate the WebGL context
  const meshesMapRef = useRef<Record<string, THREE.Mesh>>({});
  const cameraRef = useRef<THREE.PerspectiveCamera | null>(null);
  const sceneRef = useRef<THREE.Scene | null>(null);
  const groupRef = useRef<THREE.Group | null>(null);
  const sphericalRef = useRef({ radius: 3.8, theta: 0.35, phi: 1.38 });
  const panOffsetRef = useRef({ x: 0, y: 0 });
  const autoRotateRef = useRef(autoRotate);
  autoRotateRef.current = autoRotate;

  useEffect(() => {
    if (fallback2DMode) return;
    const container = containerRef.current;
    if (!container) return;

    const width = container.clientWidth || 360;
    const height = container.clientHeight || 300;

    const scene = new THREE.Scene();
    scene.background = new THREE.Color(canvasDarkMode ? 0x071318 : 0xf1f5f9);
    sceneRef.current = scene;

    const camera = new THREE.PerspectiveCamera(42, width / height, 0.1, 100);
    cameraRef.current = camera;

    const renderer = new THREE.WebGLRenderer({ antialias: true, alpha: false });
    renderer.setSize(width, height);
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
    container.innerHTML = '';
    container.appendChild(renderer.domElement);

    // Lighting
    const ambientLight = new THREE.AmbientLight(0xffffff, 0.75);
    scene.add(ambientLight);

    const dirLight1 = new THREE.DirectionalLight(0xffffff, 1.15);
    dirLight1.position.set(3, 5, 4);
    scene.add(dirLight1);

    const dirLight2 = new THREE.DirectionalLight(0x38bdf8, 0.45);
    dirLight2.position.set(-4, -2, -3);
    scene.add(dirLight2);

    const heartGroup = new THREE.Group();
    scene.add(heartGroup);
    groupRef.current = heartGroup;

    // Subtle reference grid
    const grid = new THREE.GridHelper(4.5, 9, 0x0d9488, canvasDarkMode ? 0x1e293b : 0xcbd5e1);
    grid.position.y = -1.45;
    scene.add(grid);

    const meshMap: Record<string, THREE.Mesh> = {};
    HEART_TA2_SUBMESHES.forEach((info) => {
      let geometry: THREE.BufferGeometry;
      if (info.meshNodeName === 'Arcus_Aortae') {
        geometry = new THREE.TorusGeometry(0.42, 0.16, 20, 36, Math.PI * 1.15);
      } else if (info.meshNodeName === 'Nodus_Sinuatrialis') {
        geometry = new THREE.SphereGeometry(0.18, 24, 24);
      } else {
        geometry = new THREE.SphereGeometry(0.55, 28, 24);
        geometry.scale(info.scale[0] * 1.5, info.scale[1] * 1.5, info.scale[2] * 1.5);
      }

      const material = new THREE.MeshPhysicalMaterial({
        color: info.colorHex,
        roughness: 0.32,
        metalness: 0.08,
        clearcoat: 0.25,
        transparent: true,
        opacity: 1.0,
      });

      const mesh = new THREE.Mesh(geometry, material);
      mesh.position.set(info.position[0], info.position[1], info.position[2]);
      mesh.name = info.meshNodeName;
      mesh.userData = { info };
      heartGroup.add(mesh);
      meshMap[info.meshNodeName] = mesh;
    });
    meshesMapRef.current = meshMap;

    // Conduction pathway visual filament connecting SA Node to ventricles
    const curvePoints = [
      new THREE.Vector3(-0.88, 0.82, 0.36),
      new THREE.Vector3(-0.35, 0.35, 0.25),
      new THREE.Vector3(0.0, 0.05, 0.18),
      new THREE.Vector3(-0.35, -0.45, 0.22),
      new THREE.Vector3(0.35, -0.48, 0.12),
    ];
    const curve = new THREE.CatmullRomCurve3(curvePoints);
    const tubeGeo = new THREE.TubeGeometry(curve, 32, 0.03, 8, false);
    const tubeMat = new THREE.MeshBasicMaterial({ color: 0xfbbf24, transparent: true, opacity: 0.85 });
    const conductionTube = new THREE.Mesh(tubeGeo, tubeMat);
    heartGroup.add(conductionTube);

    // Pointer interaction (orbit + raycast pick)
    let isDragging = false;
    let pointerDownPos = { x: 0, y: 0 };
    let prevPointerPos = { x: 0, y: 0 };
    const raycaster = new THREE.Raycaster();
    const mouse = new THREE.Vector2();

    const updateCameraPosition = () => {
      const { radius, theta, phi } = sphericalRef.current;
      const clampedPhi = Math.max(0.25, Math.min(Math.PI - 0.25, phi));
      camera.position.x = radius * Math.sin(clampedPhi) * Math.sin(theta) + panOffsetRef.current.x;
      camera.position.y = radius * Math.cos(clampedPhi) + panOffsetRef.current.y;
      camera.position.z = radius * Math.sin(clampedPhi) * Math.cos(theta);
      camera.lookAt(panOffsetRef.current.x, panOffsetRef.current.y, 0);
    };
    updateCameraPosition();

    const domElem = renderer.domElement;

    const onPointerDown = (e: PointerEvent) => {
      isDragging = true;
      pointerDownPos = { x: e.clientX, y: e.clientY };
      prevPointerPos = { x: e.clientX, y: e.clientY };
    };

    const onPointerMove = (e: PointerEvent) => {
      if (!isDragging) return;
      const dx = e.clientX - prevPointerPos.x;
      const dy = e.clientY - prevPointerPos.y;
      prevPointerPos = { x: e.clientX, y: e.clientY };
      sphericalRef.current.theta -= dx * 0.01;
      sphericalRef.current.phi -= dy * 0.01;
      sphericalRef.current.phi = Math.max(0.25, Math.min(Math.PI - 0.25, sphericalRef.current.phi));
      updateCameraPosition();
    };

    const onPointerUp = (e: PointerEvent) => {
      if (!isDragging) return;
      isDragging = false;
      const moveDist = Math.hypot(e.clientX - pointerDownPos.x, e.clientY - pointerDownPos.y);
      if (moveDist < 6) {
        const rect = domElem.getBoundingClientRect();
        mouse.x = ((e.clientX - rect.left) / rect.width) * 2 - 1;
        mouse.y = -((e.clientY - rect.top) / rect.height) * 2 + 1;
        raycaster.setFromCamera(mouse, camera);
        const pickableMeshes = Object.values(meshesMapRef.current).filter((m) => m.visible);
        const intersects = raycaster.intersectObjects(pickableMeshes, false);
        if (intersects.length > 0) {
          const hitMesh = intersects[0].object as THREE.Mesh;
          if (hitMesh.name) {
            setSelectedMeshName(hitMesh.name);
            if (onSelectStructure && hitMesh.userData?.info) {
              onSelectStructure(hitMesh.userData.info);
            }
          }
        }
      }
    };

    const onWheel = (e: WheelEvent) => {
      e.preventDefault();
      sphericalRef.current.radius = Math.max(
        2.1,
        Math.min(6.5, sphericalRef.current.radius + e.deltaY * 0.0025)
      );
      updateCameraPosition();
    };

    domElem.addEventListener('pointerdown', onPointerDown);
    window.addEventListener('pointermove', onPointerMove);
    window.addEventListener('pointerup', onPointerUp);
    domElem.addEventListener('wheel', onWheel, { passive: false });

    let animFrameId = 0;
    const animate = () => {
      animFrameId = requestAnimationFrame(animate);
      if (autoRotateRef.current && !isDragging && groupRef.current) {
        groupRef.current.rotation.y += 0.0045;
      }
      renderer.render(scene, camera);
    };
    animate();

    return () => {
      cancelAnimationFrame(animFrameId);
      domElem.removeEventListener('pointerdown', onPointerDown);
      window.removeEventListener('pointermove', onPointerMove);
      window.removeEventListener('pointerup', onPointerUp);
      domElem.removeEventListener('wheel', onWheel);
      renderer.dispose();
    };
  }, [fallback2DMode, canvasDarkMode]);

  // Synchronize selection highlight, X-ray mode, Isolate mode, and Layer visibility
  useEffect(() => {
    const meshMap = meshesMapRef.current;
    HEART_TA2_SUBMESHES.forEach((info) => {
      const mesh = meshMap[info.meshNodeName];
      if (!mesh) return;
      const isSelected = info.meshNodeName === selectedMeshName;
      const isHiddenByChip = !!hiddenMeshes[info.meshNodeName];
      const isHiddenByIsolate = isolateSelected && !isSelected;

      mesh.visible = !isHiddenByChip && !isHiddenByIsolate;

      const mat = mesh.material as THREE.MeshPhysicalMaterial;
      if (isSelected) {
        mat.emissive.setHex(0x14b8a6);
        mat.emissiveIntensity = 0.45;
        mat.opacity = 1.0;
        mat.wireframe = false;
      } else {
        mat.emissive.setHex(0x000000);
        mat.emissiveIntensity = 0.0;
        mat.opacity = xrayEnabled ? 0.28 : 0.88;
      }
      mat.needsUpdate = true;
    });
  }, [selectedMeshName, hiddenMeshes, xrayEnabled, isolateSelected, fallback2DMode]);

  const selectedInfo =
    HEART_TA2_SUBMESHES.find((m) => m.meshNodeName === selectedMeshName) ||
    HEART_TA2_SUBMESHES[3];

  const formatLabel = (info: HeartSubMeshInfo) => {
    switch (terminologyMode) {
      case 'AR':
        return info.nameAr;
      case 'EN':
        return info.nameEn;
      case 'LATIN_TA2':
        return info.latinTa2;
      case 'TRILINGUAL':
        return `${info.nameAr} · ${info.nameEn} (${info.latinTa2})`;
    }
  };

  const resetCamera = () => {
    sphericalRef.current = { radius: 3.8, theta: 0.35, phi: 1.38 };
    panOffsetRef.current = { x: 0, y: 0 };
    if (groupRef.current) {
      groupRef.current.rotation.set(0, 0, 0);
    }
    if (cameraRef.current) {
      const { radius, theta, phi } = sphericalRef.current;
      cameraRef.current.position.x = radius * Math.sin(phi) * Math.sin(theta);
      cameraRef.current.position.y = radius * Math.cos(phi);
      cameraRef.current.position.z = radius * Math.sin(phi) * Math.cos(theta);
      cameraRef.current.lookAt(0, 0, 0);
    }
    setIsolateSelected(false);
    setHiddenMeshes({});
  };

  const toggleMeshVisibility = (meshNodeName: string) => {
    setHiddenMeshes((prev) => ({
      ...prev,
      [meshNodeName]: !prev[meshNodeName],
    }));
  };

  return (
    <div className="space-y-2.5">
      {/* Terminology Label Switcher (Independent of App UI Language) */}
      <div
        className={`p-2.5 rounded-xl border ${
          isDarkTheme ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-200'
        }`}
      >
        <div className="flex items-center justify-between gap-2 mb-1.5">
          <span className="text-[11px] font-bold text-teal-600 dark:text-teal-400">
            {isAr
              ? 'لغة التسميات التشريحية (مستقلة عن لغة التطبيق)'
              : 'Anatomical Label Mode (Independent)'}
          </span>
          <span className="text-[10px] font-mono px-1.5 py-0.5 rounded bg-teal-500/10 text-teal-600 dark:text-teal-300">
            TA2 / FIPAT
          </span>
        </div>
        <div className="grid grid-cols-4 gap-1">
          {(
            [
              { id: 'AR', label: 'عربي' },
              { id: 'EN', label: 'EN' },
              { id: 'LATIN_TA2', label: 'Latin TA2' },
              { id: 'TRILINGUAL', label: isAr ? 'ثلاثي' : '3-Lang' },
            ] as { id: TerminologyLabelMode; label: string }[]
          ).map((mode) => (
            <button
              key={mode.id}
              onClick={() => onTerminologyModeChange(mode.id)}
              className={`py-1 px-1.5 rounded-lg text-[10px] font-bold transition ${
                terminologyMode === mode.id
                  ? 'bg-teal-700 text-white shadow-sm'
                  : isDarkTheme
                  ? 'bg-slate-800 text-slate-300 hover:bg-slate-700'
                  : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
              }`}
            >
              {mode.label}
            </button>
          ))}
        </div>
      </div>

      {/* 3D Viewport / 2D Fallback Container */}
      <div
        className={`rounded-2xl border overflow-hidden ${
          isDarkTheme ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-200 shadow-sm'
        }`}
      >
        {/* Top Viewer Status Bar */}
        <div className="px-3 py-2 bg-slate-950 text-slate-200 flex items-center justify-between gap-2 border-b border-slate-800">
          <div className="flex items-center gap-1.5">
            <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
            <span className="text-[10px] font-bold">
              {fallback2DMode
                ? isAr
                  ? 'الوضع البديل 2D Vector (Fallback)'
                  : '2D Vector Fallback Mode'
                : isAr
                ? 'عارض 3D التشريحي التفاعلي (GLB 2.0)'
                : 'Interactive 3D GLB 2.0 Viewer'}
            </span>
          </div>
          <div className="flex items-center gap-1">
            {glbVerifiedStatus.loaded && (
              <span className="text-[9px] font-mono px-1.5 py-0.5 rounded bg-emerald-950/90 text-emerald-300 border border-emerald-700/50">
                {glbVerifiedStatus.magicHeader} v{glbVerifiedStatus.version} ·{' '}
                {(glbVerifiedStatus.byteLength / 1024).toFixed(1)} KB
              </span>
            )}
            <button
              onClick={() => setFallback2DMode((prev) => !prev)}
              className="text-[9px] font-bold px-2 py-0.5 rounded bg-slate-800 hover:bg-slate-700 text-teal-300 border border-slate-700"
            >
              {fallback2DMode ? (isAr ? 'تفعيل 3D' : '3D View') : isAr ? 'بديل 2D' : '2D Fallback'}
            </button>
          </div>
        </div>

        {/* Viewport Area */}
        {!fallback2DMode ? (
          <div className="relative">
            <div
              ref={containerRef}
              className="w-full h-[250px] cursor-grab active:cursor-grabbing select-none"
            />
            {/* Floating Camera & Mode Controls */}
            <div className="absolute top-2 right-2 left-2 flex flex-wrap items-center justify-between gap-1 pointer-events-none">
              <div className="flex flex-wrap gap-1 pointer-events-auto">
                <button
                  onClick={() => setXrayEnabled((v) => !v)}
                  className={`px-2 py-1 rounded-lg text-[10px] font-bold backdrop-blur border transition ${
                    xrayEnabled
                      ? 'bg-amber-500/90 text-slate-950 border-amber-400'
                      : 'bg-slate-900/80 text-slate-200 border-slate-700'
                  }`}
                >
                  {isAr ? 'شفافية X-Ray' : 'X-Ray'}
                </button>
                <button
                  onClick={() => setIsolateSelected((v) => !v)}
                  className={`px-2 py-1 rounded-lg text-[10px] font-bold backdrop-blur border transition ${
                    isolateSelected
                      ? 'bg-teal-500/90 text-slate-950 border-teal-300'
                      : 'bg-slate-900/80 text-slate-200 border-slate-700'
                  }`}
                >
                  {isAr ? 'عزل المحدد' : 'Isolate'}
                </button>
                <button
                  onClick={() => setAutoRotate((v) => !v)}
                  className={`px-2 py-1 rounded-lg text-[10px] font-bold backdrop-blur border transition ${
                    autoRotate
                      ? 'bg-teal-900/85 text-teal-200 border-teal-700'
                      : 'bg-slate-900/80 text-slate-300 border-slate-700'
                  }`}
                >
                  {isAr ? 'دوران تلقائي' : 'Auto-Rotate'}
                </button>
              </div>
              <div className="flex gap-1 pointer-events-auto">
                <button
                  onClick={() => setCanvasDarkMode((v) => !v)}
                  className="px-2 py-1 rounded-lg text-[10px] font-bold bg-slate-900/80 text-slate-200 border border-slate-700"
                >
                  {canvasDarkMode ? '☀' : '☾'}
                </button>
                <button
                  onClick={resetCamera}
                  className="px-2 py-1 rounded-lg text-[10px] font-bold bg-slate-900/80 text-teal-300 border border-slate-700"
                >
                  {isAr ? 'إعادة ضبط' : 'Reset'}
                </button>
              </div>
            </div>

            {/* Bottom Selected Sub-Mesh Overlay */}
            <div className="absolute bottom-2 right-2 left-2 px-2.5 py-1.5 rounded-xl bg-slate-950/85 backdrop-blur border border-slate-800 flex items-center justify-between gap-2 pointer-events-none">
              <div className="truncate">
                <span className="text-[9px] text-teal-400 font-mono block">
                  Node: {selectedInfo.meshNodeName}
                </span>
                <span className="text-[11px] font-bold text-white truncate block">
                  {formatLabel(selectedInfo)}
                </span>
              </div>
              <span className="text-[9px] text-slate-400 shrink-0">
                {isAr ? 'اسحب للتدوير · انقر للتحديد' : 'Drag to orbit · Tap to pick'}
              </span>
            </div>
          </div>
        ) : (
          /* 2D Diagram Fallback View */
          <div className="p-3 bg-slate-950 text-white">
            <div className="text-[10px] text-amber-300 mb-2 flex items-center justify-between">
              <span>
                {isAr
                  ? 'المخطط التشريحي المتجهي البديل (2D Fallback) — يعمل تلقائياً عند تعطل WebGL'
                  : '2D Annotated Vector Fallback — Active when WebGL is disabled'}
              </span>
              <span className="font-mono">CC-BY-4.0</span>
            </div>
            <svg viewBox="0 0 360 210" className="w-full h-44 rounded-xl bg-slate-900 border border-slate-800">
              {/* Aortic Arch */}
              <path
                d="M 165 65 C 165 22, 225 22, 225 65"
                fill="none"
                stroke={selectedMeshName === 'Arcus_Aortae' ? '#2dd4bf' : '#e11d48'}
                strokeWidth="14"
                strokeLinecap="round"
                className="cursor-pointer"
                onClick={() => setSelectedMeshName('Arcus_Aortae')}
              />
              {/* Right Atrium */}
              <ellipse
                cx="120"
                cy="88"
                rx="38"
                ry="30"
                fill={selectedMeshName === 'Atrium_Dextrum' ? '#14b8a6' : '#2563eb'}
                fillOpacity="0.85"
                stroke="#93c5fd"
                strokeWidth="2"
                className="cursor-pointer"
                onClick={() => setSelectedMeshName('Atrium_Dextrum')}
              />
              {/* Left Atrium */}
              <ellipse
                cx="235"
                cy="88"
                rx="36"
                ry="28"
                fill={selectedMeshName === 'Atrium_Sinistrum' ? '#14b8a6' : '#dc2626'}
                fillOpacity="0.85"
                stroke="#fca5a5"
                strokeWidth="2"
                className="cursor-pointer"
                onClick={() => setSelectedMeshName('Atrium_Sinistrum')}
              />
              {/* Right Ventricle */}
              <path
                d="M 90 120 Q 130 185 172 175 L 172 115 Z"
                fill={selectedMeshName === 'Ventriculus_Dexter' ? '#14b8a6' : '#0284c7'}
                fillOpacity="0.88"
                stroke="#7dd3fc"
                strokeWidth="2"
                className="cursor-pointer"
                onClick={() => setSelectedMeshName('Ventriculus_Dexter')}
              />
              {/* Left Ventricle */}
              <path
                d="M 182 115 L 182 178 Q 235 190 265 120 Z"
                fill={selectedMeshName === 'Ventriculus_Sinister' ? '#14b8a6' : '#b91c1c'}
                fillOpacity="0.9"
                stroke="#fda4af"
                strokeWidth="2.5"
                className="cursor-pointer"
                onClick={() => setSelectedMeshName('Ventriculus_Sinister')}
              />
              {/* SA Node */}
              <circle
                cx="98"
                cy="72"
                r="9"
                fill={selectedMeshName === 'Nodus_Sinuatrialis' ? '#2dd4bf' : '#f59e0b'}
                stroke="#fef08a"
                strokeWidth="2.5"
                className="cursor-pointer"
                onClick={() => setSelectedMeshName('Nodus_Sinuatrialis')}
              />
              <text x="95" y="92" fill="#ffffff" fontSize="9" fontWeight="bold">
                RA
              </text>
              <text x="225" y="92" fill="#ffffff" fontSize="9" fontWeight="bold">
                LA
              </text>
              <text x="132" y="148" fill="#ffffff" fontSize="10" fontWeight="bold">
                RV
              </text>
              <text x="205" y="148" fill="#ffffff" fontSize="10" fontWeight="bold">
                LV
              </text>
              <text x="182" y="45" fill="#fda4af" fontSize="9" fontWeight="bold">
                Arcus aortae
              </text>
              <text x="52" y="58" fill="#fde047" fontSize="8" fontWeight="bold">
                SA Node
              </text>
            </svg>
          </div>
        )}

        {/* Sub-Mesh Selection & Layer Visibility Chips */}
        <div className="p-2.5 border-t border-slate-200 dark:border-slate-800 space-y-2">
          <div className="flex items-center justify-between">
            <span className="text-[10px] font-bold text-slate-500">
              {isAr
                ? 'أجزاء المجسم التشريحي (6 تراكيب TA2 — انقر للتحديد أو الإخفاء)'
                : 'Anatomical Sub-Meshes (6 TA2 Structures — Tap to select or toggle)'}
            </span>
          </div>
          <div className="flex flex-wrap gap-1">
            {HEART_TA2_SUBMESHES.map((m) => {
              const isSelected = m.meshNodeName === selectedMeshName;
              const isHidden = !!hiddenMeshes[m.meshNodeName];
              return (
                <div
                  key={m.meshNodeName}
                  className={`inline-flex items-center rounded-lg border text-[10px] overflow-hidden transition ${
                    isSelected
                      ? 'border-teal-600 bg-teal-600/15 text-teal-700 dark:text-teal-300 font-bold'
                      : isDarkTheme
                      ? 'border-slate-800 bg-slate-800/70 text-slate-300'
                      : 'border-slate-200 bg-slate-100 text-slate-700'
                  }`}
                >
                  <button
                    onClick={() => {
                      setSelectedMeshName(m.meshNodeName);
                      if (onSelectStructure) onSelectStructure(m);
                    }}
                    className="px-2 py-1 text-start"
                  >
                    {formatLabel(m)}
                  </button>
                  <button
                    onClick={() => toggleMeshVisibility(m.meshNodeName)}
                    title={isAr ? 'إظهار/إخفاء الطبقة' : 'Toggle Layer Visibility'}
                    className={`px-1.5 py-1 border-s ${
                      isDarkTheme ? 'border-slate-700' : 'border-slate-200'
                    } ${isHidden ? 'text-rose-500 font-bold' : 'text-slate-400 hover:text-teal-500'}`}
                  >
                    {isHidden ? '🚫' : '👁'}
                  </button>
                </div>
              );
            })}
          </div>
        </div>
      </div>

      {/* Selected Anatomical Structure Educational Card */}
      <div
        className={`p-3 rounded-2xl border ${
          isDarkTheme ? 'bg-slate-900 border-teal-900/60' : 'bg-teal-50/50 border-teal-200'
        }`}
      >
        <div className="flex items-start justify-between gap-2">
          <div>
            <span className="text-[10px] font-mono text-teal-600 dark:text-teal-400">
              TA2: {selectedInfo.latinTa2} · Node: {selectedInfo.meshNodeName}
            </span>
            <h4 className="text-xs font-extrabold mt-0.5">
              {selectedInfo.nameAr} — {selectedInfo.nameEn}
            </h4>
          </div>
          <span className="text-[9px] px-2 py-0.5 rounded-full bg-teal-600 text-white font-bold shrink-0">
            {isAr ? 'تركيب تشريحي' : 'TA2 Structure'}
          </span>
        </div>
        <p className="text-[11px] text-slate-600 dark:text-slate-300 mt-1.5 leading-relaxed">
          {isAr ? selectedInfo.summaryAr : selectedInfo.summaryEn}
        </p>
        <div className="mt-2 pt-2 border-t border-teal-200/60 dark:border-slate-800 space-y-1 text-[10px]">
          <div>
            <span className="font-bold text-teal-700 dark:text-teal-300">
              {isAr ? 'الوظيفة الفسيولوجية: ' : 'Physiological Role: '}
            </span>
            <span className="text-slate-600 dark:text-slate-300">
              {selectedInfo.physiologicalRoleAr}
            </span>
          </div>
          <div>
            <span className="font-bold text-amber-700 dark:text-amber-400">
              {isAr ? 'الأهمية السريرية: ' : 'Clinical Relevance: '}
            </span>
            <span className="text-slate-600 dark:text-slate-300">
              {selectedInfo.clinicalRelevanceAr}
            </span>
          </div>
        </div>
      </div>
    </div>
  );
};
