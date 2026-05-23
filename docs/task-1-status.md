# Статус задания 1

Здесь зафиксировано, как требования первого задания закрыты в репозитории `zizu`.

## 1. Git-репозиторий для серверной части

Сделано:

- локальный репозиторий создан в `C:\Users\danil\Documents\zizu`
- GitHub-репозиторий создан: `danilavak/zizu`

## 2. Перенос аутентификации, авторизации, HTTPS и PostgreSQL

Сделано:

- авторизация по JWT access/refresh с ротацией refresh-сессий
- ролевая модель `ADMIN` и `USER`
- конфигурация PostgreSQL
- стартовая миграция Flyway
- конфигурация HTTPS через `server.ssl.*`
- скрипт генерации HTTPS keystore: `scripts/create-https-keystore.ps1`

## 3. Переменные и секреты

Сделано:

- создан `.env.example` для локального запуска
- создан файл `docs/github-secrets.md`
- подготовлен стартовый набор секретов репозитория для:
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

## 4. Pipeline с шагами test и build

Сделано:

- workflow GitHub Actions лежит в `.github/workflows/build.yml`
- `test` и `build` вынесены в отдельные job
- дополнительно оставлены security-проверки из учебного проекта

## 5. Теория по UML

Сделано:

- добавлена краткая памятка `docs/uml-er-primer.md`

## 6. Теория по ER

Сделано:

- добавлена краткая памятка `docs/uml-er-primer.md`
