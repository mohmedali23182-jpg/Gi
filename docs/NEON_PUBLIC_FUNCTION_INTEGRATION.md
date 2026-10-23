# Dawa & Hayah — public Neon backend on selected appdb

## Selected environment
- Neon project: `icy-mode-02911943`
- Database: `appdb`, branch: `br-steep-moon-b9kxm3uf`
- Public read-only API: `https://br-steep-moon-b9kxm3uf-api.compute.c-7.eu-central-1.aws.neon.tech/`
- Existing Neon Data API: `https://ep-broad-violet-b96xtyh6.apirest.c-7.eu-central-1.aws.neon.tech/appdb/rest/v1`
- Use the *Function* URL for public app reading, not the Data API: Data API currently requires a JWT even for reads.

## Security model
Android embeds **only the public Function URL**. Never embed DATABASE_URL, unpooled URL, Neon API key, S3 credentials, or auth admin secrets in code or Android BuildConfig. Private credentials are only stored in Neon Function environment. Create a dedicated least-privileged database role before beta release and rotate any database password exposed outside secure channels.

Neon Function exposes only GET `/health`, `/v1/catalog`, `/v1/systems`, `/v1/medications`, `/v1/diseases`, `/v1/packs`, `/v1/packs/:id/manifest`, `/v1/assets/:id/access` (public-read assets only), `/v1/content/:id`, and `/v1/updates`. It filters by `publication_status.is_publicly_visible`. A 404 or empty result is expected for unapproved medical content; do not substitute mock clinical data.

## Independent verification
Validation function and main branch passed GitHub HTTPS smoke checks for PostgreSQL connectivity, JSON schemas, and rejection of mutation/clinical dosing endpoints. Actual image/model content from Object Storage is **not yet end-to-end verified**. Follow with a licensed test asset upload, accessible public read URL and SHA-256 verification, then Android offline reinstall/airplane mode test.

## Release gates
- Stages 1–2 are scaffolding until real Android device offline and Neon optional pack tests succeed.
- Stage 3 beta is strictly **blocked** until Kotlin unit tests, lint, ARM64 assembly, actual public API and content-pack downloads pass on an Android 8+ device or emulator; licensed content and human-reviewed clinical content required for publication.
- Main stage: `1.0.0` only in stage 5. 
