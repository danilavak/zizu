# Zizu Foundation

`zizu` is the local and GitHub foundation for the new RBPO server project. The repository is prepared for the next implementation phase: licensing, digital signatures, malware signatures, binary API, and optional MinIO integration.

## Included foundation

- Spring Boot 3.5 / Java 21 / Maven wrapper
- PostgreSQL-ready configuration
- Flyway migrations
- JWT access/refresh authentication with refresh session rotation
- Role-based authorization with `ADMIN` and `USER`
- Actuator health endpoint
- OpenAPI endpoint
- Docker build
- GitHub Actions pipeline with build, tests, scans, DAST and fuzzing
- `compose.yaml` for PostgreSQL and MinIO
- PowerShell script for signature keystore generation
- PowerShell script for HTTPS keystore generation
- Unified API error format
- Typed JWT principal for future domain modules
- Ready `Ticket` and `TicketResponse` contracts

## Environment variables

Core runtime:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `JWT_ACCESS_TOKEN_MINUTES`
- `JWT_REFRESH_TOKEN_DAYS`

HTTPS:

- `SERVER_SSL_ENABLED`
- `SERVER_SSL_KEY_STORE`
- `SERVER_SSL_KEY_STORE_PASSWORD`
- `SERVER_SSL_KEY_STORE_TYPE`
- `SERVER_SSL_KEY_ALIAS`

Signature module preparation:

- `SIGNATURE_KEYSTORE_LOCATION`
- `SIGNATURE_KEYSTORE_PASSWORD`
- `SIGNATURE_KEY_ALIAS`
- `SIGNATURE_KEY_PASSWORD`

Bootstrap admin account:

- `APP_ADMIN_USERNAME`
- `APP_ADMIN_EMAIL`
- `APP_ADMIN_PASSWORD`

## Local run

Copy `.env.example` to `.env` and adjust values if needed.

For local infrastructure:

```powershell
docker compose up -d
```

For signature keystore bootstrap:

```powershell
.\scripts\create-signature-keystore.ps1
```

For HTTPS bootstrap:

```powershell
.\scripts\create-https-keystore.ps1
```

Then run:

```powershell
./mvnw.cmd spring-boot:run
```

Useful endpoints:

- `POST /auth/register`
- `POST /auth/login`
- `POST /auth/refresh`
- `GET /auth/me`
- `GET /actuator/health`
- `GET /v3/api-docs`

## What comes next

The repository is intentionally trimmed to infrastructure and implementation contracts. Full business modules for licenses, signatures, binary export, and file storage are still to be implemented, but the repository already contains the runtime, CI, local infra, API conventions, and domain entry points needed to start them directly.

Supporting docs:

- [Foundation checklist](docs/foundation-checklist.md)
- [GitHub secrets](docs/github-secrets.md)
- [UML and ER primer](docs/uml-er-primer.md)
