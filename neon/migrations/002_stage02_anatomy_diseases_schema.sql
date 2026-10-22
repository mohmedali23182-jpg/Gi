-- FIX: Stage 01 already defines diseases.icd11_code and diseases.imported_at TIMESTAMPTZ.
-- FIX: Stage 01 uses sources(id), not medical_sources(id).
-- =============================================================================
-- Dawa & Hayah (دواء وحياة) — Stage 02 Neon PostgreSQL Schema Migration — corrected Stage 01 compatibility
-- File: neon/migrations/002_stage02_anatomy_diseases_schema.sql
-- Purpose: Expands the Stage 01 foundational schema with Stage 02 tables for:
--   - 13 Human Body Systems (inter-system relations, detailed pathway flags)
--   - Anatomical Structures & TA2 Trilingual Labels
--   - Organ Sections & Science Figures with Numbered Annotations
--   - 3D Anatomical Assets (GLB/GLTF metadata, LOD levels, SHA-256, licenses)
--   - Diseases Encyclopedia (ICD-11 codes, A-J structured sections, links)
--   - Source Attributions (imported_at vs human_reviewed_at)
-- =============================================================================


-- 1. Expand body_systems
ALTER TABLE body_systems
    ADD COLUMN IF NOT EXISTS has_detailed_pathway BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS inter_system_relations_ar TEXT NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS inter_system_relations_en TEXT NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS related_drug_classes JSONB NOT NULL DEFAULT '[]'::jsonb;

-- 2. Expand organs
ALTER TABLE organs
    ADD COLUMN IF NOT EXISTS location_ar TEXT NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS location_en TEXT NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS has_3d_asset BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS asset_3d_id TEXT,
    ADD COLUMN IF NOT EXISTS related_disease_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    ADD COLUMN IF NOT EXISTS related_drug_classes JSONB NOT NULL DEFAULT '[]'::jsonb;

-- 3. Anatomical Structures (TA2 sub-structures & 3D mesh node mapping)
CREATE TABLE IF NOT EXISTS anatomical_structures (
    id TEXT PRIMARY KEY,
    organ_id TEXT NOT NULL REFERENCES organs(id) ON DELETE CASCADE,
    mesh_node_name TEXT,
    name_ar TEXT NOT NULL,
    name_en TEXT NOT NULL,
    latin_name TEXT NOT NULL,
    ta2_code TEXT NOT NULL,
    structure_type TEXT NOT NULL,
    function_ar TEXT NOT NULL,
    function_en TEXT NOT NULL,
    clinical_relevance_ar TEXT NOT NULL,
    clinical_relevance_en TEXT NOT NULL,
    source_id TEXT NOT NULL REFERENCES sources(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_anatomical_structures_organ_id ON anatomical_structures(organ_id);
CREATE INDEX IF NOT EXISTS idx_anatomical_structures_ta2_code ON anatomical_structures(ta2_code);

-- 4. Anatomy Labels (Independent AR / EN / Latin TA2 terminology switch support)
CREATE TABLE IF NOT EXISTS anatomy_labels (
    id TEXT PRIMARY KEY,
    structure_id TEXT NOT NULL REFERENCES anatomical_structures(id) ON DELETE CASCADE,
    organ_id TEXT NOT NULL REFERENCES organs(id) ON DELETE CASCADE,
    label_ar TEXT NOT NULL,
    label_en TEXT NOT NULL,
    label_latin TEXT NOT NULL,
    ta2_code TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_anatomy_labels_organ_id ON anatomy_labels(organ_id);

-- 5. Organ Sections (Macro Anatomy, Histology Layers, Physiological Homeostasis)
CREATE TABLE IF NOT EXISTS organ_sections (
    id TEXT PRIMARY KEY,
    organ_id TEXT NOT NULL REFERENCES organs(id) ON DELETE CASCADE,
    section_key TEXT NOT NULL,
    title_ar TEXT NOT NULL,
    title_en TEXT NOT NULL,
    body_ar TEXT NOT NULL,
    body_en TEXT NOT NULL,
    order_index INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_organ_sections_organ_id ON organ_sections(organ_id, order_index);

-- 6. Science Figures & Numbered Annotations
CREATE TABLE IF NOT EXISTS science_figures (
    id TEXT PRIMARY KEY,
    organ_id TEXT NOT NULL REFERENCES organs(id) ON DELETE CASCADE,
    title_ar TEXT NOT NULL,
    title_en TEXT NOT NULL,
    provenance_type TEXT NOT NULL CHECK (provenance_type IN ('VERIFIED_SCIENTIFIC_DIAGRAM', 'SCHEMATIC_EDUCATIONAL')),
    license TEXT NOT NULL,
    attribution TEXT NOT NULL,
    annotations_json JSONB NOT NULL DEFAULT '[]'::jsonb
);

-- 7. Anatomy 3D Assets (GLB/GLTF Object Storage Registry)
CREATE TABLE IF NOT EXISTS anatomy_3d_assets (
    asset_id TEXT PRIMARY KEY,
    organ_id TEXT NOT NULL REFERENCES organs(id) ON DELETE CASCADE,
    system_id TEXT NOT NULL REFERENCES body_systems(id) ON DELETE CASCADE,
    anatomical_label_ar TEXT NOT NULL,
    anatomical_label_en TEXT NOT NULL,
    latin_terminology TEXT NOT NULL,
    object_key TEXT NOT NULL UNIQUE,
    local_relative_path TEXT,
    quality_level TEXT NOT NULL CHECK (quality_level IN ('LOW_MOBILE', 'STANDARD_EDUCATIONAL', 'HIGH_ATLAS')),
    triangle_budget_max INTEGER NOT NULL,
    size_bytes_budget_max BIGINT NOT NULL,
    actual_size_bytes BIGINT NOT NULL,
    sha256_checksum CHAR(64) NOT NULL,
    license TEXT NOT NULL,
    attribution TEXT NOT NULL,
    source_url TEXT NOT NULL,
    is_offline_bundled BOOLEAN NOT NULL DEFAULT FALSE,
    provenance_category TEXT NOT NULL CHECK (provenance_category IN ('VERIFIED_SCIENTIFIC_MESH', 'SCHEMATIC_EDUCATIONAL_MESH', 'DEMO_PLACEHOLDER')),
    selectable_sub_meshes JSONB NOT NULL DEFAULT '[]'::jsonb,
    required_pack_id TEXT
);

CREATE INDEX IF NOT EXISTS idx_anatomy_3d_assets_organ_id ON anatomy_3d_assets(organ_id);

-- 8. Expand diseases & add structured A-J sections
ALTER TABLE diseases
    ADD COLUMN IF NOT EXISTS secondary_system_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    ADD COLUMN IF NOT EXISTS affected_organ_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    ADD COLUMN IF NOT EXISTS related_drug_classes JSONB NOT NULL DEFAULT '[]'::jsonb,
    ADD COLUMN IF NOT EXISTS human_reviewed_at DATE;

CREATE TABLE IF NOT EXISTS disease_sections (
    id TEXT PRIMARY KEY,
    disease_id TEXT NOT NULL REFERENCES diseases(id) ON DELETE CASCADE,
    section_code TEXT NOT NULL,
    title_ar TEXT NOT NULL,
    title_en TEXT NOT NULL,
    content_ar TEXT NOT NULL,
    content_en TEXT NOT NULL,
    information_tier TEXT NOT NULL CHECK (information_tier IN ('GENERAL_EDUCATIONAL', 'CLINICAL_GUIDELINE_REF', 'UNDER_REVIEW_SAFEGUARD')),
    order_index INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_disease_sections_disease_id ON disease_sections(disease_id, order_index);

CREATE TABLE IF NOT EXISTS disease_system_links (
    disease_id TEXT NOT NULL REFERENCES diseases(id) ON DELETE CASCADE,
    system_id TEXT NOT NULL REFERENCES body_systems(id) ON DELETE CASCADE,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (disease_id, system_id)
);

CREATE TABLE IF NOT EXISTS disease_organ_links (
    disease_id TEXT NOT NULL REFERENCES diseases(id) ON DELETE CASCADE,
    organ_id TEXT NOT NULL REFERENCES organs(id) ON DELETE CASCADE,
    PRIMARY KEY (disease_id, organ_id)
);

CREATE TABLE IF NOT EXISTS source_attributions (
    id TEXT PRIMARY KEY,
    entity_id TEXT NOT NULL,
    entity_type TEXT NOT NULL,
    source_id TEXT NOT NULL REFERENCES sources(id),
    imported_at DATE NOT NULL,
    human_reviewed_at DATE,
    review_status TEXT NOT NULL CHECK (review_status IN ('draft', 'under_review', 'verified', 'published', 'archived'))
);

