# Security Policy

## Supported versions

This is a demo/portfolio project, not a maintained product release.
Security fixes are applied to the `main` branch only; there are no
long-term-support versions.

| Version | Supported |
| --- | --- |
| `main` branch | ✅ |
| Anything else / forks | ❌ |

## Reporting a vulnerability

If you find a security issue, **do not open a public issue** with the
details. Instead, report it privately to the repository owner
(`sid11raj` on GitHub) with:

- a description of the issue and its impact,
- steps to reproduce (a minimal request or test case),
- the version/branch you tested against.

You should receive an acknowledgement within a reasonable time. If the
issue is confirmed, it will be fixed on `main` and the fix described in
the commit message. There is no bug-bounty programme and no SLA.

## Security model

### Authentication

- Passwords: BCrypt, minimum 12 characters with upper-case, lower-case,
  a digit and a symbol, enforced server-side (`PasswordPolicy` /
  `AuthService`) — a client cannot bypass it.
- Access tokens: JWTs signed with `JWT_SECRET` (Base64, at least 32
  bytes), 15-minute lifetime by default (`JWT_EXPIRATION_MS`).
- Refresh tokens: 256-bit random values, of which **only a SHA-256 hash
  is stored**. Rotation revokes the presented token and issues a fresh
  one; presenting an already-revoked token revokes **all** of that
  user's refresh tokens (possible theft). Logout revokes the presented
  token, or all tokens for the current user if none is presented.
- Access tokens are stateless JWTs and **cannot be individually revoked**
  before expiry. The mitigation is the short lifetime plus refresh-token
  revocation.
- Failed logins: 5 failures lock the account for 15 minutes (both
  configurable via `LOCKOUT_MAX_ATTEMPTS` / `LOCKOUT_DURATION_MINUTES`).
  Locked logins return HTTP 423; wrong passwords and wrong MFA codes
  return a generic 401 so account existence is not leaked.
- MFA: optional TOTP (RFC 6238, SHA-1, 30-second steps, 6 digits, ±1
  step tolerance), implemented with the JDK only (`TotpService`, no
  extra dependency). The secret is shown once at setup (secret +
  `otpauth://` URL for any standard authenticator app) and is stored
  encrypted when a field-encryption key is configured. Login for an
  MFA-enabled account returns `mfaRequired: true` with **no tokens**
  until a valid code is supplied. MFA failures count toward lockout.

### Authorization

- Roles: `ADMIN`, `DOCTOR`, `PATIENT`. Endpoint access is enforced with
  `@PreAuthorize` and, for patient data, with `AccessService` ownership
  checks in the service layer on every read, write and download — the
  frontend never decides access.
- Role changes are admin-only, an admin cannot change their own role,
  and every role change is written to the audit log.
- Only `/api/auth/login`, `/api/auth/register`, `/api/auth/refresh`
  and `/api/auth/logout` are public. MFA endpoints require
  authentication. Swagger UI / OpenAPI docs should be restricted at the
  reverse proxy in production.

### Data protection

- Field-level encryption at rest: AES-256-GCM (JDK only) via the JPA
  `AttributeConverter` `EncryptedStringConverter`, applied to medical
  record diagnosis/treatment, prescription instructions and TOTP
  secrets. Format: Base64(12-byte random IV || ciphertext + 128-bit
  GCM tag), so identical plaintext encrypts differently each time.
- The key comes **only** from the `ENCRYPTION_KEY` environment variable
  (Base64, 32 bytes). There is no hardcoded key anywhere in the code,
  README, CI or `.env.example`.
- **Key loss = data loss for encrypted fields.** Back the key up in a
  secret manager before writing real data. If the key is unset, fields
  are stored as plaintext with a startup warning (development only).
  Rows written before encryption was enabled are returned as-is on
  read and must be re-saved after a key is configured. Search on
  encrypted fields is not possible; searches cover only unencrypted
  fields (title, medication).
- Uploaded lab-report files are **not** field-encrypted; they live on
  local disk / a Docker volume with UUID-prefixed basenames, resolved
  only inside the upload directory.

### Audit logging

- An append-only `audit_log` table records logins, logouts, login
  failures, record/prescription create-update-delete, lab-report
  upload/download/delete and role changes, with actor, action, entity,
  IP (first `X-Forwarded-For` value, else remote address) and
  user-agent (truncated to 500 chars) and timestamp.
- Audit writes never store health data, passwords, tokens or TOTP
  codes, run in their own transaction, and never fail the business
  operation they record. Listing is admin-only and paged
  (`GET /api/audit-logs`).

### Transport and headers

- Backend: CSP (with `frame-ancestors 'none'`), `X-Frame-Options:
  DENY`, `X-Content-Type-Options`, `Referrer-Policy: no-referrer`,
  HSTS (31536000s, subdomains). Frontend nginx: strict CSP,
  `X-Frame-Options: DENY`, `X-Content-Type-Options`,
  `Referrer-Policy: no-referrer`, `Permissions-Policy`. HSTS is not set
  on plain-HTTP nginx — terminate TLS at a reverse proxy in production
  and set HSTS there.
- CORS: origins only from `CORS_ORIGINS`; `*` is rejected when
  credentials are allowed; only `Authorization`/`Content-Type`/`Accept`
  headers are allowed.
- Rate limiting: strict bucket for `/api/auth/**` (default 30/min/IP)
  and a looser bucket for sensitive writes and audit-log reads
  (default 120/min/IP). Both are **in-memory and single-instance only**
  — enforce limits at the gateway/load balancer (or with Redis) for
  multi-instance deployments.

### CI security

- The `security-scan` job in `.github/workflows/ci.yml` runs frontend
  `npm audit --audit-level=high` and backend OWASP Dependency-Check,
  both with `continue-on-error: true`. It is advisory and non-blocking
  by design: it must not fail the build merely because a scanner is
  unavailable. Treat its output as a triage list until a fail threshold
  is agreed.

## What this project does NOT claim

- **No HIPAA, GDPR-medical or any compliance claim.** No penetration
  test, privacy impact assessment or clinical-safety review has been
  done. **Do not use this with real patient data** without a proper
  security, privacy and compliance review.
- No virus/malware scanning of uploads.
- No backup/disaster-recovery procedure (including for the encryption
  key).
- No WAF, no distributed rate limiting, no secrets manager beyond
  environment variables.
- No Flyway migrations yet (schema is JPA `ddl-auto=update` with
  `db/schema.sql` as reference only).
