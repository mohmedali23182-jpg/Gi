-- Dawa & Hayah — Stage 03 medication schema (Neon appdb).
-- Additive, idempotent schema; clinical material stays unpublished until human review.
CREATE TABLE IF NOT EXISTS drug_classes (
 id TEXT PRIMARY KEY, name_ar TEXT NOT NULL, name_en TEXT NOT NULL,
 atc_code TEXT, description_ar TEXT NOT NULL DEFAULT '', description_en TEXT NOT NULL DEFAULT '',
 source_id VARCHAR(128) NOT NULL REFERENCES sources(id),
 status_code VARCHAR(64) NOT NULL DEFAULT 'DRAFT' REFERENCES publication_status(code),
 last_human_reviewed_at TIMESTAMPTZ
);
CREATE TABLE IF NOT EXISTS drug_class_ingredients (
 class_id TEXT NOT NULL REFERENCES drug_classes(id),
 ingredient_id VARCHAR(128) NOT NULL REFERENCES active_ingredients(id),
 PRIMARY KEY(class_id,ingredient_id)
);
CREATE TABLE IF NOT EXISTS drug_indications (
 id TEXT PRIMARY KEY, ingredient_id VARCHAR(128) NOT NULL REFERENCES active_ingredients(id),
 disease_id VARCHAR(128) REFERENCES diseases(id),
 indication_ar TEXT NOT NULL, indication_en TEXT NOT NULL,
 evidence_level TEXT, source_id VARCHAR(128) NOT NULL REFERENCES sources(id),
 status_code VARCHAR(64) NOT NULL DEFAULT 'DRAFT' REFERENCES publication_status(code),
 reviewed_by TEXT, reviewed_at TIMESTAMPTZ,
 CONSTRAINT drug_indication_review_gate CHECK(status_code <> 'CLINICALLY_REVIEWED_PUBLISHED' OR (reviewed_by IS NOT NULL AND reviewed_at IS NOT NULL))
);
CREATE INDEX IF NOT EXISTS idx_drug_indications_ingredient ON drug_indications(ingredient_id);
CREATE TABLE IF NOT EXISTS drug_dosing_references (
 id TEXT PRIMARY KEY, ingredient_id VARCHAR(128) NOT NULL REFERENCES active_ingredients(id),
 indication_id TEXT REFERENCES drug_indications(id),
 patient_group TEXT NOT NULL, administration_route TEXT NOT NULL,
 formulation TEXT, indication_context TEXT,
 dosage_text_ar TEXT NOT NULL, dosage_text_en TEXT NOT NULL,
 maximum_dose_text TEXT, renal_adjustment_text TEXT, hepatic_adjustment_text TEXT,
 source_id VARCHAR(128) NOT NULL REFERENCES sources(id), source_revision TEXT,
 applicable_region TEXT NOT NULL DEFAULT 'global',
 status_code VARCHAR(64) NOT NULL DEFAULT 'DRAFT' REFERENCES publication_status(code),
 reviewed_by TEXT, reviewed_at TIMESTAMPTZ,
 CONSTRAINT no_unreviewed_published_doses CHECK(status_code <> 'CLINICALLY_REVIEWED_PUBLISHED' OR (reviewed_by IS NOT NULL AND reviewed_at IS NOT NULL))
);
CREATE INDEX IF NOT EXISTS idx_drug_dosing_ingredient ON drug_dosing_references(ingredient_id);
CREATE TABLE IF NOT EXISTS drug_safety_facts (
 id TEXT PRIMARY KEY, ingredient_id VARCHAR(128) NOT NULL REFERENCES active_ingredients(id),
 fact_type TEXT NOT NULL CHECK(fact_type IN ('CONTRAINDICATION','ADVERSE_REACTION','INTERACTION','WARNING','PREGNANCY','LACTATION','MONITORING')),
 related_ingredient_id VARCHAR(128) REFERENCES active_ingredients(id),
 severity TEXT NOT NULL DEFAULT 'UNCLASSIFIED',
 details_ar TEXT NOT NULL, details_en TEXT NOT NULL,
 source_id VARCHAR(128) NOT NULL REFERENCES sources(id),
 status_code VARCHAR(64) NOT NULL DEFAULT 'DRAFT' REFERENCES publication_status(code),
 reviewed_by TEXT, reviewed_at TIMESTAMPTZ,
 CONSTRAINT no_unreviewed_published_safety CHECK(status_code <> 'CLINICALLY_REVIEWED_PUBLISHED' OR (reviewed_by IS NOT NULL AND reviewed_at IS NOT NULL))
);
CREATE INDEX IF NOT EXISTS idx_drug_safety_ingredient ON drug_safety_facts(ingredient_id);
CREATE TABLE IF NOT EXISTS regional_medicine_products (
 id TEXT PRIMARY KEY, ingredient_id VARCHAR(128) NOT NULL REFERENCES active_ingredients(id),
 brand_name TEXT NOT NULL, market_region TEXT NOT NULL,
 manufacturer TEXT, dosage_form TEXT, strength_label TEXT,
 regulator_name TEXT, registration_number TEXT, evidence_url TEXT,
 media_asset_id VARCHAR(128) REFERENCES media_assets(id),
 verification_state TEXT NOT NULL DEFAULT 'UNVERIFIED'
  CHECK(verification_state IN ('UNVERIFIED','OBSERVED_IN_MARKET','REGISTERED_VERIFIED','NOT_CURRENTLY_AVAILABLE')),
 verified_at TIMESTAMPTZ,
 source_id VARCHAR(128) NOT NULL REFERENCES sources(id),
 status_code VARCHAR(64) NOT NULL DEFAULT 'DRAFT' REFERENCES publication_status(code),
 CONSTRAINT registered_requires_evidence CHECK(verification_state <> 'REGISTERED_VERIFIED' OR (registration_number IS NOT NULL AND evidence_url IS NOT NULL AND verified_at IS NOT NULL))
);
CREATE INDEX IF NOT EXISTS idx_regional_brand_market ON regional_medicine_products(market_region, brand_name);
CREATE TABLE IF NOT EXISTS ingredient_identifiers (
 ingredient_id VARCHAR(128) NOT NULL REFERENCES active_ingredients(id),
 vocabulary TEXT NOT NULL CHECK(vocabulary IN ('RXNORM','ATC','UNII','PUBCHEM','CHEMBL','LOCAL')),
 identifier TEXT NOT NULL, source_id VARCHAR(128) NOT NULL REFERENCES sources(id),
 PRIMARY KEY(ingredient_id,vocabulary,identifier)
);
-- Public Data API uses explicit RPCs, NEVER grants raw tables to anonymous.
REVOKE ALL ON ALL TABLES IN SCHEMA public FROM anonymous;
GRANT USAGE ON SCHEMA public TO anonymous;
