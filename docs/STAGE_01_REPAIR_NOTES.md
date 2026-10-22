# Stage 01 quality-gate repair

- Fixed a false-positive in the secrets scan: `app/src/test` intentionally includes an invalid PostgreSQL URI as a security-test fixture.
- Scan applies to `app/src/main`, while repository history and workflow secrets remain subject to separate credential auditing.
- This change does not claim that Android compilation, offline behavior, or Neon API integration already passed.
- Run the GitHub Actions Android quality gate and record actual outcomes before claiming Stage 01 acceptance.
