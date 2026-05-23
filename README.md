# Zizu

`zizu` - репозиторий серверной части проекта по РБПО. Здесь лежит сервер на Spring Boot, локальная инфраструктура и задел под будущую клиентскую часть.

## Что уже есть

- Spring Boot 3.5 / Java 21 / Maven Wrapper
- конфигурация для PostgreSQL
- миграции Flyway
- авторизация по JWT access/refresh с ротацией refresh-сессий
- роли `ADMIN` и `USER`
- эндпоинт здоровья через Actuator
- OpenAPI
- Dockerfile
- GitHub Actions с `build`, `test`, проверками безопасности, DAST и fuzzing
- полный `compose.yaml` для приложения, PostgreSQL, MinIO и генерации keystore
- PowerShell-скрипт для keystore подписи
- PowerShell-скрипт для HTTPS keystore
- единый формат ошибок API
- типизированный JWT principal
- контракты `Ticket` и `TicketResponse`

## Структура репозитория

- корень репозитория: сервер, инфраструктура, CI и Docker-стек
- `windows-client/`: будущий Windows-клиент в рамках этого же репозитория

## Переменные окружения

Основные:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `JWT_ACCESS_TOKEN_MINUTES`
- `JWT_REFRESH_TOKEN_DAYS`

Для HTTPS:

- `SERVER_SSL_ENABLED`
- `SERVER_SSL_KEY_STORE`
- `SERVER_SSL_KEY_STORE_PASSWORD`
- `SERVER_SSL_KEY_STORE_TYPE`
- `SERVER_SSL_KEY_ALIAS`

Для модуля подписи:

- `SIGNATURE_KEYSTORE_LOCATION`
- `SIGNATURE_KEYSTORE_PASSWORD`
- `SIGNATURE_KEY_ALIAS`
- `SIGNATURE_KEY_PASSWORD`

Для стартового администратора:

- `APP_ADMIN_USERNAME`
- `APP_ADMIN_EMAIL`
- `APP_ADMIN_PASSWORD`

## Запуск через Docker

1. Скопировать `.env.example` в `.env`.
2. При необходимости поменять значения в `.env`.
3. Выполнить:

```powershell
docker compose up -d --build
```

Что поднимется:

- PostgreSQL в контейнере
- MinIO в контейнере
- `minio-init`, который создаёт приватный bucket и пользователя приложения
- `keystore-init`, который создаёт dev-keystore в Docker volume
- само приложение `zizu`

Полезные адреса после запуска:

- `http://localhost:8080/actuator/health`
- `http://localhost:8080/v3/api-docs`
- `http://localhost:9001` - консоль MinIO

Остановить стек:

```powershell
docker compose down
```

Удалить стек вместе с данными и keystore:

```powershell
docker compose down -v
```

## Локальный запуск без Docker для приложения

Если нужно запускать само приложение не в контейнере, а напрямую через Java:

1. Скопировать `.env.example` в `.env`.
2. Поднять только инфраструктуру:

```powershell
docker compose up -d postgres minio minio-init
```

3. Создать keystore подписи:

```powershell
.\scripts\create-signature-keystore.ps1
```

4. Создать HTTPS keystore:

```powershell
.\scripts\create-https-keystore.ps1
```

5. Запустить приложение:

```powershell
./mvnw.cmd spring-boot:run
```

Полезные эндпоинты:

- `POST /auth/register`
- `POST /auth/login`
- `POST /auth/refresh`
- `GET /auth/me`
- `GET /actuator/health`
- `GET /v3/api-docs`

Дополнительные документы:

- [Чеклист основы проекта](docs/foundation-checklist.md)
- [Секреты GitHub](docs/github-secrets.md)
- [Кратко по UML и ER](docs/uml-er-primer.md)
