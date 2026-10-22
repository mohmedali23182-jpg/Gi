import pg from "pg";

const pool = new pg.Pool({
  connectionString: process.env.DATABASE_URL,
  max: 3,
  idleTimeoutMillis: 20000,
  connectionTimeoutMillis: 8000
});
pool.on("error", (error) => console.error("Neon pool idle disconnect:", error?.code || "unknown"));

function json(value, status = 200) {
  return new Response(JSON.stringify(value), {
    status,
    headers: {
      "content-type": "application/json; charset=utf-8",
      "cache-control": status === 200 ? "public, max-age=60" : "no-store",
      "x-content-type-options": "nosniff"
    }
  });
}

async function one(sql, values = []) {
  const result = await pool.query(sql, values);
  return result.rows?.[0]?.payload ?? null;
}

export default {
  async fetch(request) {
    const url = new URL(request.url);
    if (request.method !== "GET") return json({error:"Method not allowed"},405);

    try {
      if (url.pathname === "/health") {
        await one("SELECT 1 AS payload");
        return json({status:"ok",apiVersion:"v1",postgresConnected:true});
      }
      if (url.pathname === "/v1/catalog") {
        const lang = url.searchParams.get("locale") === "en" ? "en" : "ar";
        return json(await one("SELECT public.dawa_public_catalog($1)::jsonb AS payload",[lang]));
      }
      if (url.pathname === "/v1/systems" || url.pathname === "/v1/body-systems") {
        const lang = url.searchParams.get("locale") === "en" ? "en" : "ar";
        return json(await one("SELECT public.dawa_public_systems($1)::jsonb AS payload",[lang]));
      }
      if (url.pathname === "/v1/medicines") {
        const lang = url.searchParams.get("locale") === "en" ? "en" : "ar";
        return json(await one("SELECT public.dawa_public_medications($1)::jsonb AS payload",[lang]));
      }
      const pack = url.pathname.match(/^\/v1\/packs\/([a-zA-Z0-9_-]{1,128})\/manifest$/);
      if (pack) {
        const record = await one("SELECT public.dawa_public_pack_manifest($1)::jsonb AS payload",[pack[1]]);
        return record ? json(record) : json({error:"Pack unavailable"},404);
      }
      const asset = url.pathname.match(/^\/v1\/assets\/([a-zA-Z0-9_-]{1,128})\/access$/);
      if (asset) {
        const record = await one("SELECT public.dawa_public_asset_access($1)::jsonb AS payload",[asset[1]]);
        return record ? json(record) : json({error:"Asset unavailable"},404);
      }
      return json({error:"Not found"},404);
    } catch (error) {
      const safeMessage = String(error?.message || "unknown").replace(/postgres(?:ql)?:\/\/[^\s]+/gi, "[redacted-db-url]").replace(/npg_[A-Za-z0-9_-]+/g, "[redacted]");
      console.error("API failure:", String(error?.code || error?.name || "unknown"), safeMessage.slice(0, 240));
      return json({error:"Service temporarily unavailable"},503);
    }
  }
};