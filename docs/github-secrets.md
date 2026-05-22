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

- `SIGNATURE_KEYSTORE_BASE64`
- `SIGNATURE_KEYSTORE_LOCATION`
- `SIGNATURE_KEYSTORE_TYPE`
- `SIGNATURE_KEYSTORE_PASSWORD`
- `SIGNATURE_KEY_ALIAS`
- `SIGNATURE_KEY_PASSWORD`

Recommended approach:

- store the PKCS12 keystore in `SIGNATURE_KEYSTORE_BASE64`;
- set `SIGNATURE_KEYSTORE_TYPE=PKCS12`;
- keep alias and passwords in separate secrets.
- for PKCS12, use the same value for `SIGNATURE_KEYSTORE_PASSWORD` and `SIGNATURE_KEY_PASSWORD`.

Fallback approach:

- keep the keystore outside the repository and inject `SIGNATURE_KEYSTORE_LOCATION` with a runner-local path.

## Container registry

The workflow already uses the default `GITHUB_TOKEN` for `ghcr.io` publication.
