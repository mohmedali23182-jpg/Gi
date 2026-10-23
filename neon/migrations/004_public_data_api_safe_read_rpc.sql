-- Public-read PostgREST RPC adapter for the user-selected Neon Data API.
-- Security: grant ONLY explicit read RPCs, no public base-table reads, no editor routes.
ALTER TABLE media_assets ADD COLUMN IF NOT EXISTS public_url TEXT;

CREATE OR REPLACE FUNCTION public.dawa_public_pack_manifest(p_pack_id TEXT)
RETURNS JSONB LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public, pg_temp AS $fn$
 SELECT jsonb_build_object(
   'packId',p.pack_id,'version',p.version,'scopeType',p.scope_type,'scopeId',p.scope_id,
   'titleAr',p.title_ar,'titleEn',p.title_en,'language',p.language,
   'contentKind',p.content_kind,'quality',p.quality,'schemaVersion',p.schema_version,
   'minAppVersion',p.min_app_version,'sizeBytes',p.total_size_bytes,'license',p.license,
   'attribution',p.attribution,'publishedAt',to_char(p.published_at,'YYYY-MM-DD"T"HH24:MI:SS"Z"'),
   'dependencies',COALESCE((SELECT jsonb_agg(d.required_pack_id) FROM pack_dependencies d WHERE d.pack_id=p.pack_id),'[]'::jsonb),
   'sources','[]'::jsonb,
   'files',COALESCE((SELECT jsonb_agg(jsonb_build_object(
      'path',f.relative_path,'contentType',f.content_type,'sizeBytes',f.size_bytes,
      'sha256',f.sha256_checksum,'assetId',f.asset_id
     ) ORDER BY f.relative_path)
      FROM content_pack_files f JOIN media_assets m ON m.id=f.asset_id
      JOIN publication_status ms ON ms.code=m.status_code
      WHERE f.pack_id=p.pack_id AND ms.is_publicly_visible),'[]'::jsonb)
 )
 FROM content_packs p JOIN publication_status ps ON ps.code=p.status_code
 WHERE p.pack_id=p_pack_id AND ps.is_publicly_visible
$fn$;
REVOKE ALL ON FUNCTION public.dawa_public_pack_manifest(TEXT) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.dawa_public_pack_manifest(TEXT) TO anonymous, authenticated;

CREATE OR REPLACE FUNCTION public.dawa_public_catalog(p_locale TEXT DEFAULT 'ar')
RETURNS JSONB LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public, pg_temp AS $fn$
 SELECT jsonb_build_object(
 'revision',1,
 'locale',CASE WHEN p_locale='en' THEN 'en' ELSE 'ar' END,
 'totalPublishedItems',(SELECT count(*) FROM content_items c JOIN publication_status s ON s.code=c.status_code WHERE s.is_publicly_visible),
 'availablePacks',COALESCE((SELECT jsonb_agg(public.dawa_public_pack_manifest(p.pack_id))
   FROM content_packs p JOIN publication_status s ON s.code=p.status_code WHERE s.is_publicly_visible),'[]'::jsonb))
$fn$;
REVOKE ALL ON FUNCTION public.dawa_public_catalog(TEXT) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.dawa_public_catalog(TEXT) TO anonymous, authenticated;

CREATE OR REPLACE FUNCTION public.dawa_public_asset_access(p_asset_id TEXT)
RETURNS JSONB LANGUAGE sql STABLE SECURITY DEFINER SET search_path=public,pg_temp AS $fn$
 SELECT jsonb_build_object('assetId',m.id,'accessMode','public_read',
   'downloadUrl',m.public_url,'expiresAt',NULL,
   'sha256',m.sha256_checksum,'sizeBytes',m.size_bytes,'license',m.license)
 FROM media_assets m JOIN publication_status s ON s.code=m.status_code
 WHERE m.id=p_asset_id AND s.is_publicly_visible
   AND m.access_policy='public_read' AND m.public_url LIKE 'https://%'
$fn$;
REVOKE ALL ON FUNCTION public.dawa_public_asset_access(TEXT) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.dawa_public_asset_access(TEXT) TO anonymous, authenticated;

CREATE OR REPLACE FUNCTION public.dawa_public_systems(p_locale TEXT DEFAULT 'ar')
RETURNS JSONB LANGUAGE sql STABLE SECURITY DEFINER SET search_path=public,pg_temp AS $fn$
 SELECT COALESCE(jsonb_agg(jsonb_build_object(
  'id',b.id,'nameAr',b.name_ar,'nameEn',b.name_en,'latinName',b.latin_name,
  'descriptionAr',b.description_ar,'descriptionEn',b.description_en,
  'sourceIds','[]'::jsonb,'lastReviewedAt',NULL,
  'hasDetailedPathway',b.has_detailed_pathway,
  'interSystemRelationsAr',b.inter_system_relations_ar,
  'interSystemRelationsEn',b.inter_system_relations_en,'relatedDrugClasses',b.related_drug_classes)
  ORDER BY b.id),'[]'::jsonb)
 FROM body_systems b JOIN publication_status s ON s.code=b.status_code WHERE s.is_publicly_visible
$fn$;
REVOKE ALL ON FUNCTION public.dawa_public_systems(TEXT) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.dawa_public_systems(TEXT) TO anonymous, authenticated;

CREATE OR REPLACE FUNCTION public.dawa_public_medications(p_locale TEXT DEFAULT 'ar')
RETURNS JSONB LANGUAGE sql STABLE SECURITY DEFINER SET search_path=public,pg_temp AS $fn$
 SELECT COALESCE(jsonb_agg(jsonb_build_object(
 'id',i.id,'nameAr',i.name_ar,'nameEn',i.name_en,
 'className',i.class_name,'generalDescriptionAr',i.general_description_ar,
 'generalDescriptionEn',i.general_description_en,'safetyNote',i.safety_note,
 'citationIds','[]'::jsonb,'reviewStatus','EDUCATIONAL_FOUNDATION')
 ORDER BY i.name_en),'[]'::jsonb)
 FROM active_ingredients i JOIN publication_status s ON s.code=i.status_code
 WHERE s.is_publicly_visible AND s.code='EDUCATIONAL_FOUNDATION_PUBLISHED'
$fn$;
REVOKE ALL ON FUNCTION public.dawa_public_medications(TEXT) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.dawa_public_medications(TEXT) TO anonymous, authenticated;
