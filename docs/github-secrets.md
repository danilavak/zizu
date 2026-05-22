# GitHub Secrets

Recommended repository secrets and variables for `zizu`:

## Application secrets

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `APP_ADMIN_USERNAME`
- `APP_ADMIN_EMAIL`
- `APP_ADMIN_PASSWORD`

## HTTPS secrets

- `SERVER_SSL_ENABLED`
- `SERVER_SSL_KEY_STORE`
- `SERVER_SSL_KEY_STORE_PASSWORD`
- `SERVER_SSL_KEY_STORE_TYPE`
- `SERVER_SSL_KEY_ALIAS`

## Signature module secrets

- `SIGNATURE_KEYSTORE_LOCATION`
- `SIGNATURE_KEYSTORE_PASSWORD`
- `SIGNATURE_KEY_ALIAS`
- `SIGNATURE_KEY_PASSWORD`

If the keystore is stored as a file in CI, keep it outside the repository and inject either:

- a path on the runner, or
- a Base64-encoded file restored during workflow execution.

## Container registry

The workflow already uses the default `GITHUB_TOKEN` for `ghcr.io` publication.
