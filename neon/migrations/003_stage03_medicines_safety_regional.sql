-- Dawa & Hayah — Stage 03 pharmaceutical knowledge schema (Neon PostgreSQL)
-- Clinical safety: new records are private until reviewed and explicitly published.
-- Apply after 001 + corrected 002 on the selected appdb database.
BEGIN;
ALTER TABLE active_ingredients
  ADD COLUMN IF NOT EXISTS mechanism_ar TEXT,
  ADD COLUMN IF NOT EXISTS mechanism_en TEXT,
  ADD COLUMN IF NOT EXISTS pharmacokinetics_ar TEXT,
  ADD COLUMN IF NOT EXISTS pharmacokinetics_en TEXT,
  ADD COLUMN IF NOT EXISTS source_verified_at TIMESTAMPTZ;
CREATE TABLE IF NOT EXISTS drug_classes (
  id TEXT PRIMARY KEY,
  parent_id TEXT REFERENCES drug_classes(id),
  name_ar TEXT NOT NULL,
  name_en TEXT NOT NULL,
  atc_code TEXT,
  overview_ar TEXT NOT NULL DEFAULT '',
  overview_en TEXT NOT NULL DEFAULT '',
  status_code VARCHAR(64) NOT NULL DEFAULT 'DRAFT' REFERENCES publication_status(code)
);
CREATE TABLE IF NOT EXISTS ingredient_classes (
  ingredient_id VARCHAR(128) NOT NULL REFERENCES active_ingredients(id) ON DELETE CASCADE,
  class_id TEXT NOT NULL REFERENCES drug_classes(id) ON DELETE RESTRICT,
  PRIMARY KEY (ingredient_id, class_id)
);
CREATE TABLE IF NOT EXISTS ingredient_identifiers (
  ingredient_id VARCHAR(128) NOT NULL REFERENCES active_ingredients(id) ON DELETE CASCADE,
  system TEXT NOT NULL CHECK (system IN ('RXNORM','ATC','PUBCHEM','DRUGBANK','CHEMBL','UNII','OTHER')),
  external_id TEXT NOT NULL,
  source_id VARCHAR(128) REFERENCES sources(id),
  verified_at TIMESTAMPTZ,
  PRIMARY KEY (ingredient_id, system, external_id)
);
CREATE INDEX IF NOT EXISTS idx_ingredient_identifiers_external ON ingredient_identifiers(system, external_id);
CREATE TABLE IF NOT EXISTS ingredient_aliases (
  id BIGSERIAL PRIMARY KEY,
  ingredient_id VARCHAR(128) NOT NULL REFERENCES active_ingredients(id) ON DELETE CASCADE,
  alias TEXT NOT NULL,
  language_code TEXT NOT NULL DEFAULT 'ar',
  normalized_alias TEXT NOT NULL,
  source_id VARCHAR(128) REFERENCES sources(id),
  UNIQUE (ingredient_id, normalized_alias, language_code)
);
CREATE INDEX IF NOT EXISTS idx_ingredient_aliases_search ON ingredient_aliases(normalized_alias);
CREATE TABLE IF NOT EXISTS drug_indications (
  id TEXT PRIMARY KEY,
  ingredient_id VARCHAR(128) NOT NULL REFERENCES active_ingredients(id) ON DELETE CASCADE,
  disease_id VARCHAR(128) REFERENCES diseases(id) ON DELETE SET NULL,
  indication_ar TEXT NOT NULL,
  indication_en TEXT NOT NULL,
  region_code TEXT NOT NULL DEFAULT 'GLOBAL',
  source_id VARCHAR(128) NOT NULL REFERENCES sources(id),
  status_code VARCHAR(64) NOT NULL DEFAULT 'DRAFT' REFERENCES publication_status(code),
  verified_by TEXT,
  verified_at TIMESTAMPTZ,
  CHECK (status_code <> 'CLINICALLY_REVIEWED_PUBLISHED' OR (verified_by IS NOT NULL AND verified_at IS NOT NULL))
);
CREATE INDEX IF NOT EXISTS idx_drug_indications_ingredient ON drug_indications(ingredient_id);
CREATE TABLE IF NOT EXISTS drug_dose_references (
  id TEXT PRIMARY KEY,
  ingredient_id VARCHAR(128) NOT NULL REFERENCES active_ingredients(id) ON DELETE CASCADE,
  indication_id TEXT REFERENCES drug_indications(id),
  administration_route TEXT NOT NULL,
  population_scope TEXT NOT NULL,
  dose_text_ar TEXT NOT NULL,
  dose_text_en TEXT NOT NULL,
  renal_adjustment_ar TEXT,
  hepatic_adjustment_ar TEXT,
  contraindication_warning_ar TEXT,
  country_code TEXT NOT NULL DEFAULT 'US',
  product_label_version TEXT,
  source_id VARCHAR(128) NOT NULL REFERENCES sources(id),
  status_code VARCHAR(64) NOT NULL DEFAULT 'DRAFT' REFERENCES publication_status(code),
  reviewer_id TEXT,
  reviewed_at TIMESTAMPTZ,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CHECK (status_code <> 'CLINICALLY_REVIEWED_PUBLISHED' OR (reviewer_id IS NOT NULL AND reviewed_at IS NOT NULL))
);
CREATE INDEX IF NOT EXISTS idx_drug_dose_ingredient_status ON drug_dose_references(ingredient_id, status_code);
CREATE TABLE IF NOT EXISTS drug_safety_notices (
  id TEXT PRIMARY KEY,
  ingredient_id VARCHAR(128) NOT NULL REFERENCES active_ingredients(id) ON DELETE CASCADE,
  notice_type TEXT NOT NULL CHECK(notice_type IN ('WARNING','CONTRAINDICATION','ADVERSE_REACTION','PREGNANCY','LACTATION','PEDIATRIC','GERIATRIC','MONITORING','RECALL')),
  text_ar TEXT NOT NULL,
  text_en TEXT NOT NULL,
  severity TEXT NOT NULL DEFAULT 'UNCLASSIFIED',
  source_id VARCHAR(128) NOT NULL REFERENCES sources(id),
  status_code VARCHAR(64) NOT NULL DEFAULT 'DRAFT' REFERENCES publication_status(code),
  reviewed_at TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_drug_safety_type ON drug_safety_notices(ingredient_id, notice_type);
CREATE TABLE IF NOT EXISTS drug_interactions (
  id TEXT PRIMARY KEY,
  ingredient_a_id VARCHAR(128) NOT NULL REFERENCES active_ingredients(id) ON DELETE CASCADE,
  ingredient_b_id VARCHAR(128) NOT NULL REFERENCES active_ingredients(id) ON DELETE CASCADE,
  description_ar TEXT NOT NULL,
  description_en TEXT NOT NULL,
  evidence_strength TEXT NOT NULL DEFAULT 'UNASSESSED',
  clinical_action_ar TEXT,
  source_id VARCHAR(128) NOT NULL REFERENCES sources(id),
  status_code VARCHAR(64) NOT NULL DEFAULT 'DRAFT' REFERENCES publication_status(code),
  reviewer_id TEXT,
  reviewed_at TIMESTAMPTZ,
  CHECK (ingredient_a_id <> ingredient_b_id),
  CHECK (status_code <> 'CLINICALLY_REVIEWED_PUBLISHED' OR (reviewer_id IS NOT NULL AND reviewed_at IS NOT NULL))
);
CREATE TABLE IF NOT EXISTS pharmaceutical_manufacturers (
  id TEXT PRIMARY KEY,
  legal_name TEXT NOT NULL,
  country_code TEXT NOT NULL,
  website_url TEXT,
  evidence_source_id VARCHAR(128) REFERENCES sources(id)
);
CREATE TABLE IF NOT EXISTS regional_medicine_products (
  id TEXT PRIMARY KEY,
  ingredient_id VARCHAR(128) NOT NULL REFERENCES active_ingredients(id) ON DELETE RESTRICT,
  brand_name TEXT NOT NULL,
  country_code TEXT NOT NULL,
  manufacturer_id TEXT REFERENCES pharmaceutical_manufacturers(id),
  strength_label TEXT NOT NULL,
  dosage_form TEXT NOT NULL,
  registration_identifier TEXT,
  availability_state TEXT NOT NULL DEFAULT 'UNVERIFIED' CHECK(availability_state IN ('UNVERIFIED','LISTED','REGISTERED','OBSERVED','DISCONTINUED')),
  source_id VARCHAR(128) REFERENCES sources(id),
  verified_at TIMESTAMPTZ,
  image_asset_id VARCHAR(128) REFERENCES media_assets(id),
  status_code VARCHAR(64) NOT NULL DEFAULT 'DRAFT' REFERENCES publication_status(code),
  UNIQUE (country_code, brand_name, strength_label, dosage_form)
);
CREATE INDEX IF NOT EXISTS idx_regional_products_country ON regional_medicine_products(country_code, ingredient_id);
CREATE TABLE IF NOT EXISTS medical_review_audit (
  id BIGSERIAL PRIMARY KEY,
  entity_type TEXT NOT NULL,
  entity_id TEXT NOT NULL,
  decision TEXT NOT NULL CHECK(decision IN ('APPROVED','REJECTED','NEEDS_CHANGES','WITHDRAWN')),
  reviewer_id TEXT NOT NULL,
  source_id VARCHAR(128) REFERENCES sources(id),
  notes TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
-- Clinically sensitive material remains private even when informational sections are published.
CREATE OR REPLACE VIEW public_drug_ingredients AS
  SELECT id, name_ar, name_en, class_name, general_description_ar, general_description_en,
         safety_note, rxnorm_cui, atc_code, mechanism_ar, mechanism_en
  FROM active_ingredients a
  JOIN publication_status p ON p.code=a.status_code
  WHERE p.is_publicly_visible;
CREATE OR REPLACE VIEW public_drug_safety AS
  SELECT s.id, s.ingredient_id, s.notice_type, s.text_ar, s.text_en, s.severity, s.source_id
  FROM drug_safety_notices s
  JOIN publication_status p ON p.code=s.status_code
  WHERE p.is_publicly_visible;
CREATE OR REPLACE VIEW public_reviewed_drug_doses AS
  SELECT d.id, d.ingredient_id, d.indication_id, d.administration_route, d.population_scope,
         d.dose_text_ar, d.dose_text_en, d.country_code, d.source_id, d.reviewed_at
  FROM drug_dose_references d
  WHERE d.status_code='CLINICALLY_REVIEWED_PUBLISHED' AND d.reviewer_id IS NOT NULL
    AND d.reviewed_at IS NOT NULL;
-- DO NOT give public API access to base tables; expose narrowly filtered views via controlled Functions only.
COMMIT;