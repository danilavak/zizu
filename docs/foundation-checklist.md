# Чеклист основы проекта

Репозиторий можно считать подготовленным к дальнейшей разработке, если на месте такие базовые вещи:

- Безопасность:
  - JWT access и refresh
  - ротация refresh-токенов с хранением сессий
  - роли `ADMIN` / `USER`
  - типизированный principal из JWT
- Инфраструктура:
  - конфигурация PostgreSQL
  - стартовые миграции Flyway
  - Dockerfile
  - `compose.yaml` с PostgreSQL и MinIO
  - `.env.example`
- Сборка и проверки:
  - GitHub Actions с `build`, `test`, `secret_scan`, `container_scan`, `sast`, `sca`, `dast`, `fuzzing`
- API:
  - единый JSON-формат ошибок
  - endpoint здоровья
  - OpenAPI endpoint
- Подготовка доменной части:
  - корневые пакеты `license`, `signature`, `malware`, `binaryapi`
  - контракты `Ticket` и `TicketResponse`
  - скрипт генерации keystore подписи

## В каком порядке лучше развивать дальше

1. Схема БД и сервисы лицензий.
2. Загрузка keystore и сервис подписи.
3. Подпись `TicketResponse`.
4. Схема сигнатур, история и аудит.
5. Binary API, манифест и `multipart`.
6. Загрузка файлов и интеграция с MinIO.
