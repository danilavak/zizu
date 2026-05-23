# Секреты GitHub

Ниже список секретов и переменных, которые нужны репозиторию `zizu`.

## Секреты приложения

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `APP_ADMIN_USERNAME`
- `APP_ADMIN_EMAIL`
- `APP_ADMIN_PASSWORD`

## Секреты для HTTPS

- `SERVER_SSL_ENABLED`
- `SERVER_SSL_KEY_STORE`
- `SERVER_SSL_KEY_STORE_PASSWORD`
- `SERVER_SSL_KEY_STORE_TYPE`
- `SERVER_SSL_KEY_ALIAS`

## Секреты для модуля подписи

- `SIGNATURE_KEYSTORE_BASE64`
- `SIGNATURE_KEYSTORE_LOCATION`
- `SIGNATURE_KEYSTORE_TYPE`
- `SIGNATURE_KEYSTORE_PASSWORD`
- `SIGNATURE_KEY_ALIAS`
- `SIGNATURE_KEY_PASSWORD`

Рекомендуемый вариант:

- хранить PKCS12 keystore в `SIGNATURE_KEYSTORE_BASE64`;
- `SIGNATURE_KEYSTORE_TYPE` ставить в `PKCS12`;
- alias и пароли хранить отдельными секретами;
- для PKCS12 использовать одинаковое значение в `SIGNATURE_KEYSTORE_PASSWORD` и `SIGNATURE_KEY_PASSWORD`.

Запасной вариант:

- хранить keystore вне репозитория и передавать путь через `SIGNATURE_KEYSTORE_LOCATION` на runner.

## Публикация контейнера

Workflow уже использует стандартный `GITHUB_TOKEN` для публикации в `ghcr.io`.

## Секреты для MinIO

- `MINIO_ENDPOINT`
- `MINIO_ACCESS_KEY`
- `MINIO_SECRET_KEY`
- `MINIO_BUCKET`
- `MINIO_REGION`
- `MINIO_PRESIGNED_EXPIRY_MINUTES`

## Дополнительно для ускорения SCA

- `NVD_API_KEY`

Если добавить `NVD_API_KEY` в секреты GitHub, шаг `sca` будет работать заметно быстрее, потому что `dependency-check` сможет обновлять базу NVD без жёстких ограничений по скорости.
