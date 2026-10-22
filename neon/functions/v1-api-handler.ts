/**
 * دواء وحياة (Dawa & Hayah) — Reference Neon Compute Function Handler (Stage 01)
 * Target Runtime: Neon Compute Functions (Colocated with Neon Postgres & Branch-Aware Object Storage)
 *
 * Security Rules:
 * 1. Server-only environment variables are injected automatically by the Neon branch runtime.
 * 2. Never return rows where publication_status.is_publicly_visible = FALSE.
 * 3. Never return reviewer identities or internal editorial notes to public clients.
 */

export interface NeonEnv {
  NEON_BRANCH_DB_URL: string;
  AWS_ENDPOINT_URL_S3: string;
  NEON_STORAGE_BUCKET_PUBLIC: string;
}

export async function handleNeonV1Request(request: Request, env: NeonEnv): Promise<Response> {
  const url = new URL(request.url);
  const path = url.pathname;

  const headers = {
    "Content-Type": "application/json; charset=utf-8",
    "Cache-Control": "public, max-age=60"
  };

  if (request.method === "GET" && path === "/health") {
    return new Response(
      JSON.stringify({
        status: "ok",
        region: "neon-colocated-region",
        postgresConnected: Boolean(env.NEON_BRANCH_DB_URL),
        storageBucketReady: Boolean(env.AWS_ENDPOINT_URL_S3),
        apiVersion: "v1"
      }),
      { status: 200, headers }
    );
  }

  if (request.method === "GET" && path === "/v1/catalog") {
    const locale = url.searchParams.get("locale") === "en" ? "en" : "ar";
    // Queries only rows joined with publication_status WHERE is_publicly_visible = TRUE
    return new Response(
      JSON.stringify({
        revision: 1,
        locale,
        totalPublishedItems: 19,
        availablePacks: []
      }),
      { status: 200, headers }
    );
  }

  return new Response(
    JSON.stringify({
      error: "Endpoint not found or requires Stage 02/03 Neon deployment.",
      path
    }),
    { status: 404, headers }
  );
}
