# Zizu Foundation

`zizu` is the local and GitHub repository for the RBPO server platform. It contains the implemented server modules plus the infrastructure needed to run the full backend stack locally and in CI.

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
- full `compose.yaml` stack for the application, PostgreSQL, MinIO and keystore bootstrap
- PowerShell script for signature keystore generation
- PowerShell script for HTTPS keystore generation
- Unified API error format
- Typed JWT principal for future domain modules
- Ready `Ticket` and `TicketResponse` contracts

## Repository layout

- repository root: Spring Boot server, infrastructure, CI and Docker stack
- `windows-client/`: future Windows tray client subproject

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

## Docker run

Copy `.env.example` to `.env` and adjust values if needed.

Then run the full server stack:

```powershell
docker compose up -d --build
```

What happens in this mode:

- PostgreSQL starts in a container
- MinIO starts in a container
- `minio-init` creates the private bucket and application user
- `keystore-init` generates development signature and HTTPS keystores in a persistent Docker volume
- the Spring Boot application builds and starts in its own container

Useful endpoints after startup:

- `http://localhost:8080/actuator/health`
- `http://localhost:8080/v3/api-docs`
- `http://localhost:9001` for the MinIO console

To stop the stack:

```powershell
docker compose down
```

To remove the data and generated keystores as well:

```powershell
docker compose down -v
```

## Local Java run

Copy `.env.example` to `.env` and adjust values if needed.

For local infrastructure:

```powershell
docker compose up -d postgres minio minio-init
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

Supporting docs:

- [Foundation checklist](docs/foundation-checklist.md)
- [GitHub secrets](docs/github-secrets.md)
- [UML and ER primer](docs/uml-er-primer.md)
