-- ============================================================================
-- دواء وحياة (DAWA & HAYAH) — NEON POSTGRESQL SCHEMA MIGRATION 001 (STAGE 01)
-- Target: Neon Serverless PostgreSQL (Branch-Aware Storage & Compute)
-- Security Policy: Never expose draft or unapproved clinical content via public v1 endpoints.
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 1. Publication Status Enum / Lookup Table
CREATE TABLE IF NOT EXISTS publication_status (
    code VARCHAR(64) PRIMARY KEY,
    description_ar TEXT NOT NULL,
    description_en TEXT NOT NULL,
    is_publicly_visible BOOLEAN NOT NULL DEFAULT FALSE
);

INSERT INTO publication_status (code, description_ar, description_en, is_publicly_visible) VALUES
    ('DRAFT', 'مسودة قيد الإعداد (غير متاحة للجمهور)', 'Internal Draft (Not publicly visible)', FALSE),
    ('IN_EDITORIAL_REVIEW', 'قيد المراجعة العلمية والتحريرية', 'Under Scientific Editorial Review', FALSE),
    ('EDUCATIONAL_FOUNDATION_PUBLISHED', 'محتوى تأسيسي تعليمي منشور (غير علاجي)', 'Published Educational Foundation (Non-clinical)', TRUE),
    ('CLINICALLY_REVIEWED_PUBLISHED', 'محتوى مراجع ومعتمد سريرياً وصيدلانياً', 'Clinically & Pharmaceutically Reviewed and Published', TRUE),
    ('ARCHIVED', 'مؤرشف', 'Archived', FALSE)
ON CONFLICT (code) DO NOTHING;

-- 2. Scientific Sources Registry (Provenance)
CREATE TABLE IF NOT EXISTS sources (
    id VARCHAR(128) PRIMARY KEY,
    title TEXT NOT NULL,
    url TEXT NOT NULL,
    doi VARCHAR(255),
    publisher TEXT NOT NULL,
    publication_date VARCHAR(64),
    retrieved_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    evidence_version_date VARCHAR(64),
    reuse_license TEXT NOT NULL,
    attribution TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 3. Body Systems
CREATE TABLE IF NOT EXISTS body_systems (
    id VARCHAR(128) PRIMARY KEY,
    canonical_uuid UUID NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    name_ar TEXT NOT NULL,
    name_en TEXT NOT NULL,
    latin_name VARCHAR(255),
    description_ar TEXT NOT NULL,
    description_en TEXT NOT NULL,
    status_code VARCHAR(64) NOT NULL REFERENCES publication_status(code) DEFAULT 'EDUCATIONAL_FOUNDATION_PUBLISHED',
    imported_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_human_reviewed_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 4. Anatomical Organs (Terminologia Anatomica TA2)
CREATE TABLE IF NOT EXISTS organs (
    id VARCHAR(128) PRIMARY KEY,
    canonical_uuid UUID NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    system_id VARCHAR(128) NOT NULL REFERENCES body_systems(id) ON DELETE RESTRICT,
    name_ar TEXT NOT NULL,
    name_en TEXT NOT NULL,
    latin_name VARCHAR(255) NOT NULL,
    summary_ar TEXT NOT NULL,
    summary_en TEXT NOT NULL,
    status_code VARCHAR(64) NOT NULL REFERENCES publication_status(code) DEFAULT 'EDUCATIONAL_FOUNDATION_PUBLISHED',
    imported_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_human_reviewed_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_organs_system_id ON organs(system_id);

-- 5. Diseases (Pathophysiological & Educational)
CREATE TABLE IF NOT EXISTS diseases (
    id VARCHAR(128) PRIMARY KEY,
    canonical_uuid UUID NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    primary_system_id VARCHAR(128) NOT NULL REFERENCES body_systems(id) ON DELETE RESTRICT,
    icd11_code VARCHAR(64),
    name_ar TEXT NOT NULL,
    name_en TEXT NOT NULL,
    introduction_ar TEXT NOT NULL,
    introduction_en TEXT NOT NULL,
    status_code VARCHAR(64) NOT NULL REFERENCES publication_status(code) DEFAULT 'EDUCATIONAL_FOUNDATION_PUBLISHED',
    imported_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_human_reviewed_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_diseases_system_id ON diseases(primary_system_id);

-- 6. Active Ingredients (Educational Pharmacology & Mechanism of Action)
CREATE TABLE IF NOT EXISTS active_ingredients (
    id VARCHAR(128) PRIMARY KEY,
    canonical_uuid UUID NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    rxnorm_cui VARCHAR(64),
    atc_code VARCHAR(32),
    name_ar TEXT NOT NULL,
    name_en TEXT NOT NULL,
    class_name TEXT NOT NULL,
    general_description_ar TEXT NOT NULL,
    general_description_en TEXT NOT NULL,
    safety_note TEXT NOT NULL,
    status_code VARCHAR(64) NOT NULL REFERENCES publication_status(code) DEFAULT 'EDUCATIONAL_FOUNDATION_PUBLISHED',
    imported_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_human_reviewed_at TIMESTAMPTZ
);

-- 7. Canonical Content Items & Articles
CREATE TABLE IF NOT EXISTS content_items (
    id VARCHAR(128) PRIMARY KEY,
    canonical_uuid UUID NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    content_type VARCHAR(64) NOT NULL, -- LESSON, ARTICLE, CLINICAL_CASE, ANATOMY_NOTE
    scope_type VARCHAR(64) NOT NULL,   -- system, organ, disease, medicine
    scope_id VARCHAR(128) NOT NULL,
    current_revision INT NOT NULL DEFAULT 1,
    status_code VARCHAR(64) NOT NULL REFERENCES publication_status(code) DEFAULT 'DRAFT',
    imported_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    published_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_content_items_scope ON content_items(scope_type, scope_id);
CREATE INDEX IF NOT EXISTS idx_content_items_status ON content_items(status_code);

-- 8. Content Translations (AR / EN / Latin)
CREATE TABLE IF NOT EXISTS content_translations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content_id VARCHAR(128) NOT NULL REFERENCES content_items(id) ON DELETE CASCADE,
    locale VARCHAR(16) NOT NULL, -- 'ar', 'en'
    revision INT NOT NULL DEFAULT 1,
    title TEXT NOT NULL,
    summary TEXT,
    body_markdown TEXT NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (content_id, locale, revision)
);

-- 9. Content Revisions Log
CREATE TABLE IF NOT EXISTS content_revisions (
    revision_id BIGSERIAL PRIMARY KEY,
    content_id VARCHAR(128) NOT NULL REFERENCES content_items(id) ON DELETE CASCADE,
    revision_number INT NOT NULL,
    change_summary TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 10. Editorial Reviews (Strictly Server-Side Audit Trail — Never Exposed to Public Clients)
CREATE TABLE IF NOT EXISTS editorial_reviews (
    review_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content_id VARCHAR(128) NOT NULL REFERENCES content_items(id) ON DELETE CASCADE,
    revision_number INT NOT NULL,
    reviewer_role VARCHAR(128) NOT NULL, -- e.g. Clinical Pharmacist, Anatomist
    review_decision VARCHAR(64) NOT NULL, -- APPROVED, CHANGES_REQUESTED, REJECTED
    clinical_dosing_verified BOOLEAN NOT NULL DEFAULT FALSE,
    reviewed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 11. Citations Mapping (Linking Entities & Content to Verified Sources)
CREATE TABLE IF NOT EXISTS citations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type VARCHAR(64) NOT NULL, -- SYSTEM, ORGAN, DISEASE, INGREDIENT, CONTENT_ITEM
    entity_id VARCHAR(128) NOT NULL,
    source_id VARCHAR(128) NOT NULL REFERENCES sources(id) ON DELETE RESTRICT,
    relationship_type VARCHAR(64) NOT NULL DEFAULT 'PRIMARY_EVIDENCE',
    citation_note TEXT,
    UNIQUE (entity_type, entity_id, source_id)
);

-- 12. Media Assets (Stored in Neon Object Storage Buckets: SVG, WebP, GLB/GLTF)
CREATE TABLE IF NOT EXISTS media_assets (
    id VARCHAR(128) PRIMARY KEY,
    bucket_name VARCHAR(128) NOT NULL,
    object_key TEXT NOT NULL UNIQUE,
    content_type VARCHAR(128) NOT NULL,
    size_bytes BIGINT NOT NULL CHECK (size_bytes > 0),
    sha256_checksum CHAR(64) NOT NULL,
    access_policy VARCHAR(32) NOT NULL DEFAULT 'public_read', -- 'public_read' or 'presigned_url'
    quality_level VARCHAR(32) NOT NULL DEFAULT 'standard',
    triangle_count INT,
    license TEXT NOT NULL,
    attribution TEXT NOT NULL,
    source_id VARCHAR(128) REFERENCES sources(id),
    status_code VARCHAR(64) NOT NULL REFERENCES publication_status(code) DEFAULT 'EDUCATIONAL_FOUNDATION_PUBLISHED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 13. Offline Content Packs (Granular Downloadable Bundles)
CREATE TABLE IF NOT EXISTS content_packs (
    pack_id VARCHAR(128) PRIMARY KEY,
    version VARCHAR(32) NOT NULL,
    scope_type VARCHAR(64) NOT NULL, -- system, organ, disease, medicine, lesson, image, model3d
    scope_id VARCHAR(128) NOT NULL,
    title_ar TEXT NOT NULL,
    title_en TEXT NOT NULL,
    language VARCHAR(16) NOT NULL DEFAULT 'ar',
    content_kind VARCHAR(64) NOT NULL,
    quality VARCHAR(32) NOT NULL DEFAULT 'standard',
    schema_version INT NOT NULL DEFAULT 1,
    min_app_version VARCHAR(32) NOT NULL DEFAULT '0.1.0',
    total_size_bytes BIGINT NOT NULL CHECK (total_size_bytes > 0),
    license TEXT NOT NULL,
    attribution TEXT NOT NULL,
    status_code VARCHAR(64) NOT NULL REFERENCES publication_status(code) DEFAULT 'EDUCATIONAL_FOUNDATION_PUBLISHED',
    published_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (pack_id, version)
);

-- 14. Content Pack Files
CREATE TABLE IF NOT EXISTS content_pack_files (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pack_id VARCHAR(128) NOT NULL REFERENCES content_packs(pack_id) ON DELETE CASCADE,
    relative_path TEXT NOT NULL,
    asset_id VARCHAR(128) NOT NULL REFERENCES media_assets(id) ON DELETE RESTRICT,
    content_type VARCHAR(128) NOT NULL,
    size_bytes BIGINT NOT NULL CHECK (size_bytes > 0),
    sha256_checksum CHAR(64) NOT NULL,
    UNIQUE (pack_id, relative_path)
);

-- 15. Pack Dependencies (Companion Packs Notice)
CREATE TABLE IF NOT EXISTS pack_dependencies (
    pack_id VARCHAR(128) NOT NULL REFERENCES content_packs(pack_id) ON DELETE CASCADE,
    required_pack_id VARCHAR(128) NOT NULL REFERENCES content_packs(pack_id) ON DELETE RESTRICT,
    dependency_reason_ar TEXT NOT NULL,
    dependency_reason_en TEXT NOT NULL,
    PRIMARY KEY (pack_id, required_pack_id)
);
