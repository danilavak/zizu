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

Start PostgreSQL and create a database, for example `zizu`. Then run:

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

The repository is intentionally trimmed to infrastructure and security foundation. Business modules for licenses, signatures, binary export, and file storage are not implemented yet, but the package structure and configuration are ready for that work.
