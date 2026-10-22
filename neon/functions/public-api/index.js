import postgres from "postgres";

// Dawa & Hayah: public, read-only API. Neon injects DATABASE_URL into Functions.
// This function never exposes PostgreSQL credentials or internal clinical drafts.
const sql = postgres(process.env.DAWA_DATABASE_URL || process.env.DATABASE_URL, {
  max: 3, idle_timeout: 15, connect_timeout: 8, prepare: false
});

const json = (payload, status = 200) => new Response(JSON.stringify(payload), {
  status,
  headers: {
    "Content-Type": "application/json; charset=utf-8",
    "Cache-Control": status === 200 ? "public, max-age=90" : "no-store",
    "Access-Control-Allow-Origin": "*",
    "X-Content-Type-Options": "nosniff"
  }
});
const validId = value => /^[a-zA-Z0-9._:-]{1,128}$/.test(value ?? "");
const integerLimit = (input, defaultValue = 100) => Math.min(100, Math.max(1, Number.parseInt(input ?? String(defaultValue), 10) || defaultValue));
const get = async (statement, args = []) => await sql.unsafe(statement, args);
const published = alias => `JOIN publication_status ps ON ps.code=${alias}.status_code AND ps.is_publicly_visible=TRUE`;

async function packManifest(packId) {
  const rows = await get(`SELECT p.* FROM content_packs p ${published("p")} WHERE p.pack_id=$1 LIMIT 1`, [packId]);
  if (!rows.length) return null;
  const p = rows[0];
  const files = await get(`
    SELECT f.relative_path, f.asset_id, f.content_type, f.size_bytes, f.sha256_checksum
    FROM content_pack_files f
    JOIN media_assets a ON a.id=f.asset_id
    ${published("a")}
    WHERE f.pack_id=$1 ORDER BY f.relative_path LIMIT 300`, [packId]);
  const deps = await get("SELECT required_pack_id FROM pack_dependencies WHERE pack_id=$1", [packId]);
  return {
    packId: p.pack_id, version: p.version, scopeType: p.scope_type, scopeId: p.scope_id,
    titleAr: p.title_ar, titleEn: p.title_en, language: p.language,
    contentKind: p.content_kind, quality: p.quality, schemaVersion: p.schema_version,
    minAppVersion: p.min_app_version, sizeBytes: Number(p.total_size_bytes),
    files: files.map(f => ({
      path: f.relative_path, assetId: f.asset_id, contentType: f.content_type,
      sizeBytes: Number(f.size_bytes), sha256: f.sha256_checksum
    })),
    dependencies: deps.map(d => d.required_pack_id), sources: [],
    license: p.license, attribution: p.attribution,
    publishedAt: p.published_at instanceof Date ? p.published_at.toISOString() : String(p.published_at)
  };
}
async function catalogue(locale) {
  const [countRow] = await get(`
    SELECT
      (SELECT count(*) FROM content_items c ${published("c")}) +
      (SELECT count(*) FROM body_systems b ${published("b")}) +
      (SELECT count(*) FROM organs o ${published("o")}) +
      (SELECT count(*) FROM diseases d ${published("d")}) +
      (SELECT count(*) FROM active_ingredients i ${published("i")}) AS total
  `);
  const packRows = await get(`SELECT p.pack_id FROM content_packs p ${published("p")} ORDER BY p.pack_id LIMIT 100`);
  const packs = await Promise.all(packRows.map(p => packManifest(p.pack_id)));
  return { revision: 1, locale, totalPublishedItems: Number(countRow?.total ?? 0), availablePacks: packs.filter(Boolean) };
}

async function handler(request) {
  if (request.method === "OPTIONS") return new Response(null, {
    status: 204, headers: {"Access-Control-Allow-Origin":"*", "Access-Control-Allow-Methods":"GET,OPTIONS"}
  });
  if (request.method !== "GET") return json({ error:"Method not allowed" }, 405);
  const url = new URL(request.url);
  const path = url.pathname.replace(/\/+$/, "") || "/";
  const locale = url.searchParams.get("locale") === "en" ? "en" : "ar";
  if (path === "/health") {
    await get("SELECT 1 AS healthy");
    return json({status:"ok", region:"aws-eu-central-1", postgresConnected:true, storageBucketReady:false, apiVersion:"v1"});
  }
  if (path === "/v1/catalog") return json(await catalogue(locale));
  if (path === "/v1/systems" || path === "/v1/body-systems") {
    const rows = await get(`SELECT b.* FROM body_systems b ${published("b")} ORDER BY b.id LIMIT $1`,[integerLimit(url.searchParams.get("limit"))]);
    return json(rows.map(b => ({
      id:b.id,nameAr:b.name_ar,nameEn:b.name_en,latinName:b.latin_name,
      descriptionAr:b.description_ar,descriptionEn:b.description_en,sourceIds:[],
      lastReviewedAt:b.last_human_reviewed_at?.toISOString?.() ?? null,
      hasDetailedPathway:b.has_detailed_pathway,interSystemRelationsAr:b.inter_system_relations_ar,
      interSystemRelationsEn:b.inter_system_relations_en,relatedDrugClasses:b.related_drug_classes ?? []
    })));
  }
  if (path === "/v1/medications") {
    const rows = await get(`SELECT i.* FROM active_ingredients i ${published("i")} ORDER BY i.name_en LIMIT $1`,[integerLimit(url.searchParams.get("limit"))]);
    return json(rows.map(i => ({
      id:i.id,nameAr:i.name_ar,nameEn:i.name_en,className:i.class_name,
      generalDescriptionAr:i.general_description_ar,generalDescriptionEn:i.general_description_en,
      safetyNote:i.safety_note,citationIds:[],reviewStatus:"PUBLISHED"
    })));
  }
  if (path === "/v1/diseases") {
    const system = url.searchParams.get("systemId");
    if (system && !validId(system)) return json({error:"Invalid system ID"},400);
    const rows = await get(`SELECT d.* FROM diseases d ${published("d")} WHERE ($1::text IS NULL OR d.primary_system_id=$1) ORDER BY d.id LIMIT $2`,
      [system,integerLimit(url.searchParams.get("limit"))]);
    return json(rows.map(d => ({
      id:d.id, primarySystemId:d.primary_system_id, nameAr:d.name_ar, nameEn:d.name_en,
      introductionAr:d.introduction_ar,introductionEn:d.introduction_en,
      citationIds:[],reviewStatus:"PUBLISHED",icd11Code:d.icd11_code ?? "",
      secondarySystemIds:d.secondary_system_ids ?? [],affectedOrganIds:d.affected_organ_ids ?? [],
      relatedDrugClasses:d.related_drug_classes ?? [],importedAt:d.imported_at?.toISOString?.().slice(0,10) ?? "",
      humanReviewedAt:d.human_reviewed_at?.toISOString?.().slice(0,10) ?? null,sections:[]
    })));
  }
  if (path === "/v1/packs" || path === "/v1/content-packs") {
    const scope = url.searchParams.get("scope"), scopeId=url.searchParams.get("scopeId");
    if ((scope&&!validId(scope))||(scopeId&&!validId(scopeId))) return json({error:"Invalid scope"},400);
    const rows = await get(`SELECT p.pack_id FROM content_packs p ${published("p")}
      WHERE ($1::text IS NULL OR p.scope_type=$1) AND ($2::text IS NULL OR p.scope_id=$2)
      ORDER BY p.pack_id LIMIT $3`, [scope,scopeId,integerLimit(url.searchParams.get("limit"))]);
    return json((await Promise.all(rows.map(p => packManifest(p.pack_id)))).filter(Boolean));
  }
  const packMatch = path.match(/^\/v1\/packs\/([^/]+)\/manifest$/);
  if (packMatch) {
    const id=decodeURIComponent(packMatch[1]);
    if(!validId(id)) return json({error:"Invalid pack ID"},400);
    const result=await packManifest(id);
    return result ? json(result) : json({error:"Pack not found"},404);
  }
  const assetMatch = path.match(/^\/v1\/assets\/([^/]+)\/access$/);
  if(assetMatch) {
    const id=decodeURIComponent(assetMatch[1]);
    if(!validId(id)) return json({error:"Invalid asset ID"},400);
    const rows=await get(`SELECT a.* FROM media_assets a ${published("a")}
      WHERE a.id=$1 AND a.access_policy='public_read' AND a.public_url LIKE 'https://%' LIMIT 1`,[id]);
    if(!rows.length) return json({error:"Public asset unavailable"},404);
    const a=rows[0];
    return json({assetId:a.id, accessMode:"public_read",downloadUrl:a.public_url,expiresAt:null,sha256:a.sha256_checksum,sizeBytes:Number(a.size_bytes),license:a.license});
  }
  const contentMatch=path.match(/^\/v1\/content\/([^/]+)$/);
  if(contentMatch) {
    const id=decodeURIComponent(contentMatch[1]); if(!validId(id)) return json({error:"Invalid content ID"},400);
    const rows=await get(`SELECT c.id,c.current_revision,c.content_type,c.published_at,t.title,t.body_markdown,t.locale
      FROM content_items c ${published("c")}
      JOIN content_translations t ON t.content_id=c.id AND t.revision=c.current_revision
      WHERE c.id=$1 AND t.locale=$2 ORDER BY t.revision DESC LIMIT 1`,[id,locale]);
    if(!rows.length) return json({error:"Content not available"},404);
    const x=rows[0];
    return json({id:x.id,canonicalId:x.id,revision:x.current_revision,contentType:x.content_type,
      locale:x.locale,sourceId:"",origin:"NEON",publishedAt:x.published_at?.toISOString?.() ?? "",
      reviewStatus:"PUBLISHED",titleAr:x.title,titleEn:x.title,bodyAr:x.body_markdown,bodyEn:x.body_markdown});
  }
  if(path === "/v1/updates") {
    const revision=Math.max(0,Math.min(1000000,Number.parseInt(url.searchParams.get("fromRevision")??"0",10)||0));
    return json({fromRevision:revision, latestRevision:1,updatedContentIds:[],updatedPackIds:[]});
  }
  return json({error:"Not found"},404);
}
export default {
  async fetch(request) {
    try { return await handler(request); }
    catch (error) {
      console.error("Public API error:",error?.code ?? "unknown");
      return json({error:"Service unavailable"},503);
    }
  }
};
