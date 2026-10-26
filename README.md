# Health Records Management System

A complete, end-to-end health records management system for patients, doctors
and admins. This is a working application, not a placeholder: it has a real
Spring Boot API with JWT authentication and role-based access, a MySQL
database, file uploads for lab reports, email notifications, and a React
frontend that talks to that API.

> **Demo / portfolio project.** This is not a certified medical system and
> must not be used for real patient care without a proper security, privacy
> and compliance review.

## Screenshots

Screenshots captured from the running frontend (React + Vite, `http://localhost:5173`).
The login and register screens are the public pages. The dashboard, records,
lab reports and prescriptions screens show the app UI with navigation, forms
and empty states; they were captured without a live backend, so their data
fetches show the app's error state and no real patient data is shown.
Run the backend + MySQL (see Quick start) for live data.

### Login

![Login](screenshots/login.png)

### Register

![Register](screenshots/register.png)

### Dashboard

![Dashboard](screenshots/dashboard.png)

### Medical Records

![Medical Records](screenshots/medical-records.png)

![New medical record](screenshots/medical-records-new.png)

### Lab Reports

![Lab Reports](screenshots/lab-reports.png)

![Upload lab report](screenshots/lab-reports-upload.png)

### Prescriptions

![Prescriptions](screenshots/prescriptions.png)

![New prescription](screenshots/prescriptions-new.png)

## Features

- **Authentication** — register and log in with email/password; JWT tokens
  issued on login and sent on every API request.

  ![Login](screenshots/login.png)

  ![Register](screenshots/register.png)

- **Roles** — `ADMIN`, `DOCTOR`, `PATIENT`.

  Dashboard (role-aware landing page):

  ![Dashboard](screenshots/dashboard.png)
  - Patients can only see, create and manage their **own** records.
  - Doctors and admins can see all patients' records and pick a patient
    when creating records, reports or prescriptions.
- **Medical records** — full CRUD: title, description, diagnosis, treatment,
  record date, patient and doctor.

  ![Medical Records](screenshots/medical-records.png)

  ![New medical record](screenshots/medical-records-new.png)

- **Lab reports** — upload a file (stored under `uploads/` on the server),
  with title, description, patient and optional linked medical record;
  list and secure download. Only the owning patient, doctors and admins can
  download a report.

  ![Lab Reports](screenshots/lab-reports.png)

  ![Upload lab report](screenshots/lab-reports-upload.png)

- **Prescriptions** — full CRUD: medication, dosage, instructions, start/end
  dates, patient, doctor and optional linked medical record.

  ![Prescriptions](screenshots/prescriptions.png)

  ![New prescription](screenshots/prescriptions-new.png)
- **Email notifications** — Spring Mail sends a notification to the patient
  when a prescription is created or a lab report is uploaded. Mail is
  best-effort: if mail is not configured, the operation still succeeds and
  the failure is logged.
- **Validation and errors** — Bean Validation on requests and a global
  exception handler returning consistent JSON errors.

## Tech stack

| Layer | Technologies |
| --- | --- |
| Backend | Java 17, Spring Boot 3.2, Spring Web, Spring Data JPA, Spring Security, Spring Mail, Spring Boot Actuator, springdoc OpenAPI, MySQL, jjwt (JWT), Bean Validation, Maven |
| Frontend | React 18, TypeScript, Vite, Redux Toolkit, React Router, Material-UI, axios |
| Database | MySQL 8 (via docker-compose, or any local MySQL) |
| Files | Local `uploads/` directory, with a UUID prefix per stored file |
| CI | GitHub Actions: backend `mvn test` (with MySQL service) + frontend `npm run build` |

## Architecture

```
health-records-management-system/
├── backend/        Spring Boot API (port 8080)
│   └── src/main/java/com/healthrecords/
│       ├── config/       SecurityConfig, JwtAuthenticationFilter
│       ├── security/     JwtService
│       ├── model/        User, MedicalRecord, LabReport, Prescription, Role
│       ├── repository/   Spring Data JPA repositories
│       ├── dto/          Request/response records
│       ├── service/      Auth, records, lab reports, prescriptions, email, access rules
│       ├── controller/   REST controllers under /api/...
│       └── exception/    GlobalExceptionHandler
├── frontend/       React + TypeScript + Vite app (port 5173 in dev, port 80 in Docker via nginx)
│   └── src/
│       ├── api/          axios client with JWT interceptor
│       ├── store/        Redux Toolkit auth slice
│       ├── components/   ProtectedRoute, Layout
│       └── pages/        Login, Register, Dashboard, Records, Lab Reports, Prescriptions
├── backend/Dockerfile    Multi-stage Maven build → slim JRE image
├── frontend/Dockerfile   Node build → nginx (SPA fallback + /api proxy to backend)
├── .github/workflows/ci.yml  Backend tests + frontend build on push/PR to main
└── docker-compose.yml  MySQL + backend + frontend together
```

## Quick start

### Option A — Run everything with Docker (recommended)

```bash
cp .env.example .env
# Edit .env and set JWT_SECRET (openssl rand -base64 32) and DB_PASSWORD
docker compose up --build
```

- Frontend: http://localhost (nginx serves the built app and proxies `/api` to the backend)
- Backend API: http://localhost:8080/api
- Swagger UI: http://localhost:8080/swagger-ui.html
- Health: http://localhost:8080/actuator/health

Stop with `docker compose down`. Data in the MySQL and uploads volumes is
kept unless you add `-v`.

### Option B — Run each part locally

#### 1. Start MySQL

```bash
docker compose up -d mysql
```

This starts MySQL 8 on port 3306 with a database named `health_records`.
Set `DB_PASSWORD` in your environment if your MySQL root password is not empty.

### 2. Configure the backend

Generate a JWT secret (Base64, at least 256 bits) and export it — do not
commit it anywhere:

```bash
export JWT_SECRET=$(openssl rand -base64 32)
export DB_URL="jdbc:mysql://localhost:3306/health_records?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
export DB_USERNAME=root
export DB_PASSWORD=""
# Optional, for email notifications:
export MAIL_HOST=smtp.example.com
export MAIL_PORT=587
export MAIL_USERNAME=you@example.com
export MAIL_PASSWORD=""
```

Then run the backend:

```bash
cd backend
mvn spring-boot:run
```

(Maven is required; no Maven wrapper is bundled in this project.)

The API is available at http://localhost:8080/api.

### 3. Run the frontend

```bash
cd frontend
npm install
npm run dev
```

Open http://localhost:5173, register an account (choose PATIENT, DOCTOR or
ADMIN), and log in.

## Seed / demo data

There is no hardcoded demo user and no hardcoded production credential.
Create demo data through the app itself:

1. Register a DOCTOR account and a PATIENT account (two different emails).
2. As the doctor, create a medical record for the patient from the
   Medical Records page (the patient picker appears for doctors/admins).
3. Upload a lab report for that patient from the Lab Reports page.
4. Create a prescription for that patient from the Prescriptions page.
5. Log in as the patient to confirm only that patient's data is visible.

## Environment variables

| Variable | Purpose | Default |
| --- | --- | --- |
| `DB_URL` | JDBC URL for MySQL | `jdbc:mysql://localhost:3306/health_records?...` |
| `DB_USERNAME` | Database username | `root` |
| `DB_PASSWORD` | Database password | (empty) |
| `JWT_SECRET` | **Required.** Base64-encoded 256-bit JWT signing key | (none — app fails clearly if unset when issuing a token) |
| `JWT_EXPIRATION_MS` | Access-token lifetime in milliseconds | `900000` (15 min) |
| `REFRESH_TOKEN_EXPIRATION_MS` | Refresh-token lifetime in milliseconds | `604800000` (7 days) |
| `ENCRYPTION_KEY` | **Required for real data.** Base64, 32 bytes (AES-256) for field-level encryption — see [Security](#security) | (none — fields stored as plaintext with a startup warning) |
| `LOCKOUT_MAX_ATTEMPTS` / `LOCKOUT_DURATION_MINUTES` | Failed logins before a temporary account lock, and lock duration | `5` / `15` |
| `SENSITIVE_RATE_LIMIT_PER_MINUTE` | Max sensitive-write / audit-log requests per IP per minute (in-memory, single instance) | `120` |
| `UPLOAD_DIR` | Directory for uploaded lab reports | `uploads` |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` | SMTP settings for notifications | (empty — mail is skipped if unset) |
| `CORS_ORIGINS` | Allowed frontend origins | `http://localhost:5173` |
| `UPLOAD_MAX_BYTES` | Max lab-report file size in bytes (also enforced in code) | `10485760` (10 MB) |
| `UPLOAD_ALLOWED_TYPES` / `UPLOAD_ALLOWED_EXTENSIONS` | Allowed upload content types / extensions | `application/pdf,image/png,image/jpeg,text/plain` / `pdf,png,jpg,jpeg,txt` |
| `AUTH_RATE_LIMIT_PER_MINUTE` | Max `/api/auth/**` requests per IP per minute (in-memory, single instance) | `30` |
| `VITE_API_URL` | Frontend API base URL | `http://localhost:8080/api` (Docker build uses `/api`) |

## API endpoints

All endpoints except login, register, refresh and logout require an
`Authorization: Bearer <JWT>` header. MFA endpoints require authentication.

| Method | Endpoint | Description | Access |
| --- | --- | --- | --- |
| POST | `/api/auth/register` | Register (name, email, password, role) | Public |
| POST | `/api/auth/login` | Log in, returns access + refresh tokens, or `mfaRequired: true` | Public |
| POST | `/api/auth/refresh` | Rotate a refresh token (old token revoked, new pair issued) | Public (with a valid refresh token) |
| POST | `/api/auth/logout` | Revoke a refresh token (or all of the current user's, if none given) | Public / authenticated |
| POST | `/api/auth/mfa/setup` | Start TOTP setup; returns the secret + otpauth URL **once** | Authenticated |
| POST | `/api/auth/mfa/verify` | Verify a TOTP code to enable MFA | Authenticated |
| POST | `/api/auth/mfa/disable` | Verify a TOTP code to disable MFA | Authenticated |
| GET | `/api/audit-logs` | Paged audit logs, optional `?action=` filter | ADMIN |
| PUT | `/api/users/{id}/role` | Change a user's role (admins cannot change their own) | ADMIN |
| GET | `/api/users/me` | Current user | Any authenticated user |
| GET | `/api/users` | List users, optional `?role=` | ADMIN, DOCTOR |
| GET | `/api/users/patients` | List patients | ADMIN, DOCTOR |
| GET | `/api/medical-records` | List medical records, optional `?search=` (title) and `?patientId=` | Own (patient) / all (staff) |
| GET | `/api/medical-records/paged` | Paged records: `?page=0&size=20&sort=createdAt,desc`, plus `search` / `patientId` | Own / staff |
| GET | `/api/users/paged` | Paged users, optional `?role=` | ADMIN, DOCTOR |
| GET | `/api/medical-records/{id}` | Get one record | Own / staff |
| POST | `/api/medical-records` | Create a record | Own / staff |
| PUT | `/api/medical-records/{id}` | Update a record | Own / staff |
| DELETE | `/api/medical-records/{id}` | Delete a record | Own / staff |
| GET | `/api/lab-reports` | List lab reports, optional `?search=` (title) and `?patientId=` | Own / staff |
| GET | `/api/lab-reports/paged` | Paged lab reports: `?page=0&size=20&sort=uploadedAt,desc` | Own / staff |
| GET | `/api/lab-reports/{id}` | Get one report | Own / staff |
| POST | `/api/lab-reports` | Upload a report (multipart: `title`, `patientId`, `file`, optional `description`, `medicalRecordId`) | Own / staff |
| GET | `/api/lab-reports/{id}/download` | Download the report file | Own / staff |
| DELETE | `/api/lab-reports/{id}` | Delete a report and its file | Own / staff |
| GET | `/api/prescriptions` | List prescriptions, optional `?search=` (medication) and `?patientId=` | Own / staff |
| GET | `/api/prescriptions/paged` | Paged prescriptions: `?page=0&size=20&sort=createdAt,desc` | Own / staff |
| GET | `/api/prescriptions/{id}` | Get one prescription | Own / staff |
| POST | `/api/prescriptions` | Create a prescription | Own / staff |
| PUT | `/api/prescriptions/{id}` | Update a prescription | Own / staff |
| DELETE | `/api/prescriptions/{id}` | Delete a prescription | Own / staff |

Example — register, then create a record (replace `<TOKEN>`):

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"name":"Dr Demo","email":"doctor@example.com","password":"Password123!","role":"DOCTOR"}'

curl -X POST http://localhost:8080/api/medical-records \
  -H "Authorization: Bearer <TOKEN>" -H 'Content-Type: application/json' \
  -d '{"title":"Annual check-up","diagnosis":"Healthy","recordDate":"2026-10-26","patientId":2}'
```

## Security

See [SECURITY.md](SECURITY.md) for the vulnerability-reporting process and
the full security model. Summary of what is in place:

- Passwords are stored with BCrypt, never in plain text. Passwords must be
  at least 12 characters with upper-case, lower-case, a digit and a symbol
  (enforced server-side; the register form shows a live checklist).
- Access tokens are short-lived JWTs (15 min by default) signed with a
  secret supplied only via `JWT_SECRET`. Sessions continue through
  refresh tokens: only a SHA-256 hash is stored, rotation revokes the old
  token, presenting a revoked token revokes **all** of that user's refresh
  tokens, and logout revokes them. Access tokens themselves are stateless
  and cannot be individually revoked before expiry — the short lifetime
  plus refresh revocation is the mitigation.
- Failed logins lock an account after 5 attempts for 15 minutes
  (configurable). Locked logins return HTTP 423 with a clear message;
  wrong passwords/MFA codes still return a generic 401 so account
  existence is not leaked.
- Optional TOTP MFA (RFC 6238, pure JDK, no extra dependency): setup
  returns the secret and an `otpauth://` URL once; login for an
  MFA-enabled account returns `mfaRequired: true` with **no tokens**
  until a valid code is supplied. The secret is stored encrypted with
  the field-encryption key when one is configured.
- Field-level encryption at rest (AES-256-GCM, pure JDK) for diagnosis,
  treatment, prescription instructions and TOTP secrets, via a JPA
  `AttributeConverter` (`EncryptedStringConverter`). The key comes only
  from `ENCRYPTION_KEY` (Base64, 32 bytes) — there is no hardcoded key.
  **Losing the key makes encrypted fields permanently unreadable**, so
  back it up in a secret manager before writing real data. If the key is
  unset, fields are stored as plaintext with a startup warning (dev
  only). Rows written before encryption was enabled are returned as-is
  on read and must be re-saved after a key is configured. Search on
  encrypted fields is not possible: repository searches cover only
  unencrypted fields (title, medication).
- Audit logging: an append-only `audit_log` table records logins,
  logouts, login failures, record/prescription create-update-delete,
  lab-report upload/download/delete and role changes, with actor, action,
  entity, IP and user-agent (truncated to 500 chars) and timestamp. It
  never stores health data, passwords, tokens or TOTP codes, runs in its
  own transaction, and never fails a business operation. Admins can list
  it paged at `GET /api/audit-logs` (UI: Audit Logs page).
- Patients are restricted to their own data in the service layer
  (`AccessService`), not just in the UI — the API enforces it too.
- Uploaded filenames are cleaned and stored with a UUID prefix to avoid
  collisions and path traversal.
- Security headers: backend sends CSP (with `frame-ancestors 'none'`),
  `X-Frame-Options: DENY`, `X-Content-Type-Options`, `Referrer-Policy:
  no-referrer` and HSTS; the frontend nginx config sends its own strict
  CSP, frame/content-type/referrer and `Permissions-Policy` headers.
  HSTS is not set on plain-HTTP nginx — terminate TLS at a reverse
  proxy in production and set HSTS there.
- Rate limiting: a strict bucket for `/api/auth/**` (default 30/min/IP)
  and a looser bucket for sensitive writes and audit-log reads (default
  120/min/IP). Both are in-memory and single-instance only — enforce
  limits at the gateway/load balancer (or with Redis) for multi-instance
  deployments.
- CI runs a non-blocking dependency scan job (`npm audit` at high level
  for the frontend, OWASP Dependency-Check for the backend) with
  `continue-on-error: true`, so it reports findings without failing the
  build merely because a scanner is unavailable. Treat its output as a
  triage list until a fail threshold is agreed.
- Input/output safety: all request DTOs are validated (bean validation
  plus a server-side password-policy check that a client cannot bypass);
  error responses never leak internal details; the frontend renders
  server data as text (no `dangerouslySetInnerHTML`), so stored values
  cannot inject markup. See the Threat Model below for what this does
  and does not cover.

### Threat model

**Assets:** patient health records (diagnosis/treatment notes,
prescriptions, lab-report files), user accounts (especially doctor/admin
accounts), credentials and tokens, the audit trail.

**Threats and mitigations:**

| Threat | Mitigation |
| --- | --- |
| Credential stuffing / brute force | BCrypt passwords, strong password policy, lockout after 5 failures, strict auth rate limit, generic 401s |
| Stolen access token | 15-minute expiry; refresh-token rotation + revocation on logout; presenting a revoked token revokes all tokens |
| Stolen refresh token | Only hashes stored; rotation detects reuse and revokes all tokens for the user |
| Stolen password alone | Optional TOTP MFA; login with MFA pending issues no tokens |
| Database dump / disk read | AES-GCM field encryption for the most sensitive free-text fields (key from env only); BCrypt passwords; TOTP secrets encrypted |
| Privilege escalation / IDOR | Role checks (`@PreAuthorize`) + `AccessService` ownership checks on every read/write/download; role changes are admin-only and audited |
| XSS | Strict CSP on the frontend, `X-Content-Type-Options: nosniff`, React's escaped rendering, no inline scripts in the SPA |
| Clickjacking | `X-Frame-Options: DENY` + CSP `frame-ancestors 'none'` |
| CSRF | No session cookies (bearer tokens only), so there is no cookie to forge; re-enable CSRF if cookie auth is ever added |
| Path traversal / malicious upload | UUID-prefixed basename filenames resolved only inside the upload dir; extension/content-type/size allow-list |
| Abuse / enumeration | Rate limits on auth and sensitive writes; audit logging of sensitive actions for later review |

**What is still NOT covered — do not claim otherwise:**

- **No HIPAA, GDPR-medical or any compliance claim.** This is a
  demo/portfolio project. No penetration test, privacy impact
  assessment or clinical-safety review has been done.
- **No virus/malware scanning** of uploaded lab reports.
- **No backup/disaster-recovery procedure** for the database, uploads or
  the encryption key. Key loss = encrypted data loss.
- **No WAF, no distributed rate limiting, no secrets manager** beyond
  environment variables.
- **Uploaded files are not encrypted at rest** (only the free-text
  fields listed above are); they live on local disk / a Docker volume.
- **Access tokens cannot be individually revoked** before their short
  expiry; revocation applies to refresh tokens.
- **No Flyway migrations** — schema is still JPA `ddl-auto=update`
  with `db/schema.sql` as reference only.
- **Search on encrypted fields is not possible** (see above).
- **Do not use this with real patient data** without a proper security,
  privacy and compliance review.

## Industry-level / Production notes

This section describes what was hardened beyond a basic demo, and — just as
importantly — what is still missing before real patient data is involved.

### Architecture

- Layered backend: controllers (HTTP + validation) → services (business
  rules + access control) → repositories (JPA) → MySQL. The frontend never
  decides access: `AccessService` enforces the patient-owns-own-data rule in
  the service layer on every read, write and download.
- Standard error responses from `GlobalExceptionHandler`: every error body
  has `status`, `code`, `error`, `timestamp` and `path`; validation errors
  add a `fields` map. The React frontend already reads `error`/`fields`,
  so this shape is kept stable. Internal exception details are logged
  server-side, never returned to clients.
- Audit fields: `createdAt`/`updatedAt` on users, records and prescriptions,
  `uploadedAt`/`updatedAt` on lab reports, and `createdBy` (creator email)
  on medical records and prescriptions; lab reports record `uploadedBy`.
  Indexes exist on foreign keys and frequently queried fields
  (`patient_id`, `doctor_id`, `medical_record_id`, `record_date`, `role`).
- Schema reference: `backend/src/main/resources/db/schema.sql` documents
  the tables and indexes. It is **not** auto-executed
  (`spring.sql.init.mode=never`); the current mechanism is JPA
  `ddl-auto=update`. For production, replace ddl-auto with reviewed
  Flyway migrations based on that file, and never let ddl-auto run
  against a production database.

### Security model

- Passwords: BCrypt only (verified in `SecurityConfig` /
  `AuthService`). No plain-text passwords are stored or logged.
- JWT: signed with a secret supplied only via `JWT_SECRET` (Base64, must
  decode to at least 32 bytes); access-token lifetime via
  `JWT_EXPIRATION_MS` (default 15 min), refresh-token lifetime via
  `REFRESH_TOKEN_EXPIRATION_MS` (default 7 days). Refresh tokens rotate
  and are revoked on logout; only their SHA-256 hashes are stored.
  Tokens are bearer tokens sent in the `Authorization` header; there is
  no session cookie, so CSRF is disabled deliberately — re-enable it
  if cookie auth is ever added.
- MFA: optional TOTP (RFC 6238, pure JDK implementation in
  `TotpService`, no extra dependency). Secrets are stored encrypted
  when a field-encryption key is configured and are shown once at setup.
- Field encryption: AES-256-GCM via `EncryptedStringConverter` for
  diagnosis, treatment, prescription instructions and TOTP secrets.
  Key only from `ENCRYPTION_KEY`; key loss = data loss for those fields.
- Audit log: append-only `audit_log` table, admin-only paged endpoint,
  never stores health data / passwords / tokens / TOTP codes, and never
  fails the business operation it records.
- Account lockout: 5 failed logins → 15-minute lock (both configurable),
  HTTP 423 with a clear message.
- CORS: allowed origins come only from `CORS_ORIGINS` (env); `*` is
  rejected when credentials are allowed, and only
  `Authorization`/`Content-Type`/`Accept` headers are allowed.
- Security headers: backend CSP (with `frame-ancestors 'none'`),
  `X-Frame-Options: DENY`, `X-Content-Type-Options`,
  `Referrer-Policy: no-referrer`, and HSTS (31536000s, subdomains);
  frontend nginx sends its own strict CSP plus
  `Permissions-Policy`. Public auth surface is now only
  login/register/refresh/logout — MFA endpoints require authentication.
- Uploads: `FileUploadValidator` enforces a size limit, an allow-list of
  extensions and content types, and basename-only filenames; stored files
  get a UUID prefix and are resolved only inside the upload directory,
  so path traversal is blocked. The multipart size limit in
  `application.properties` is a second, independent limit.
- Rate limiting: `RateLimitFilter` caps `/api/auth/**` at
  `AUTH_RATE_LIMIT_PER_MINUTE` requests per IP per minute (default 30)
  and sensitive writes plus audit-log reads at
  `SENSITIVE_RATE_LIMIT_PER_MINUTE` (default 120), returning a standard
  429 body. It is in-memory and single-instance only — in a
  multi-instance deployment, enforce limits at the gateway or with
  Redis instead.
- OpenAPI: Swagger UI at `/swagger-ui.html` and docs at `/v3/api-docs`.
  Restrict those endpoints at the reverse proxy in production.
- Observability: Actuator exposes only `health` and `info` over HTTP
  (`management.endpoints.web.exposure.include=health,info`), with health
  details hidden. Logs are structured single-line via
  `logback-spring.xml`; application code must never log tokens,
  passwords or health-data payloads.

### Testing and CI

- Unit tests (Mockito, no database needed): `AuthServiceTest`
  (hashing, default role, duplicate email, weak-password rejection,
  login, 5-failure lockout, MFA-required login),
  `MedicalRecordServiceTest` (patient scoping, create + `createdBy`),
  `AccessServiceTest` (staff vs patient ownership rules),
  `FileUploadValidatorTest` (allowed type, blocked extension, size,
  path-traversal filename), `TotpServiceTest`, `AesGcmEncryptionTest`
  (round-trip, fresh IV per value, wrong key, short key, converter
  pass-through), `RefreshTokenServiceTest` (rotation, revoked reuse
  revokes all, expiry, revocation) and `AuditServiceTest` (audit write
  shape, audit failure never throws).
- The pre-existing `HealthRecordsApplicationTests` context test needs a
  running MySQL database, which is why the CI backend job runs with a
  MySQL service container.
- CI (`.github/workflows/ci.yml`) runs on push/PR to `main`:
  backend `mvn -B test` with JDK 17 + MySQL, and frontend
  `npm ci` + `npm run build` with Node 20, plus a non-blocking
  `security-scan` job (frontend `npm audit --audit-level=high`,
  backend OWASP Dependency-Check), both `continue-on-error: true` so
  scanner unavailability does not fail the build.

### What is still NOT production-ready for real patient data

Be honest about this in any portfolio write-up — do **not** claim
HIPAA, GDPR-medical or any other compliance for this project:

- Uploads live on local disk (or a Docker volume), not in encrypted,
  backed-up object storage, and there is no virus/malware scan.
  Uploaded files are not field-encrypted (only the free-text fields
  listed in [Security](#security) are).
- No data retention/deletion workflow, and no backup/restore procedure
  — including for the encryption key, whose loss loses encrypted data.
- Access tokens cannot be individually revoked before their short
  expiry; revocation applies to refresh tokens.
- No distributed rate limiting, WAF, or secrets manager — secrets are
  environment variables, which is correct for a demo but not a
  complete production secrets story.
- No Flyway migrations yet (still JPA `ddl-auto=update`); no
  penetration test, privacy impact assessment, or clinical-safety
  review. **Do not use this with real patient data.**

## Upgrade notes — cybersecurity round (2026-10-26)

Security: refresh tokens with rotation and hash-only storage
(`RefreshToken` + `/api/auth/refresh`, `/api/auth/logout`), access
tokens shortened from 24h to 15 min, failed-login lockout (5 attempts /
15 min, HTTP 423), 12-character complexity password policy, optional
TOTP MFA with a pure-JDK implementation (no hardcoded codes, secret
shown once), append-only audit logging with an admin-only paged
endpoint, AES-256-GCM field encryption for diagnosis/treatment/
prescription instructions and TOTP secrets via a JPA converter (key
from `ENCRYPTION_KEY` only), backend CSP + tightened public auth
surface, frontend nginx security headers, extended rate limiting for
sensitive writes, `SECURITY.md`, a README Threat Model, and a
non-blocking CI dependency-scan job.

Quality: DTO mapping extracted into `RecordMapper` (medical records,
prescriptions, lab reports, users), admin role-change endpoint with
auditing, detail pages for a medical record and a prescription, list
pages switched to the existing `/paged` endpoints with search and
Prev/Next pagination, dashboard now shows recent items as well as
counts, loading/error states gained retry buttons, and new unit tests
for the security features listed under Testing and CI above.
