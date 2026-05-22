# Task 1 Status

This document maps task 1 requirements to the `zizu` repository state.

## 1. Git repository for the server part

Done:

- Local Git repository created in `C:\Users\danil\Documents\zizu`
- GitHub repository created: `danilavak/zizu`

## 2. Transfer authentication, authorization, HTTPS, PostgreSQL

Done:

- JWT access/refresh authentication with refresh rotation
- Role-based authorization with `ADMIN` and `USER`
- PostgreSQL runtime configuration
- Flyway baseline migration
- HTTPS configuration through `server.ssl.*`
- HTTPS keystore generation script: `scripts/create-https-keystore.ps1`

## 3. Transfer variables and secrets or create new ones

Done:

- `.env.example` created for local runtime
- `docs/github-secrets.md` created
- Initial GitHub repository secrets created for:
  - `DB_URL`
  - `DB_USERNAME`
  - `DB_PASSWORD`
  - `JWT_SECRET`
  - `JWT_ACCESS_TOKEN_MINUTES`
  - `JWT_REFRESH_TOKEN_DAYS`
  - `APP_ADMIN_USERNAME`
  - `APP_ADMIN_EMAIL`
  - `APP_ADMIN_PASSWORD`
  - `SERVER_SSL_ENABLED`
  - `SERVER_SSL_KEY_STORE_TYPE`

## 4. Transfer pipeline with test and build

Done:

- GitHub Actions workflow present in `.github/workflows/build.yml`
- Separate `test` and `build` jobs
- Additional security and verification jobs kept from the study project

## 5. Study UML diagrams theory

Done:

- Summary note added: `docs/uml-er-primer.md`

## 6. Study ER diagrams theory

Done:

- Summary note added: `docs/uml-er-primer.md`
