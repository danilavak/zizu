# Foundation Checklist

The repository is ready for the implementation phase of the new project when the following baseline is in place:

- Security foundation:
  - JWT access and refresh tokens
  - refresh token rotation with persisted sessions
  - `ADMIN` / `USER` role model
  - typed authenticated principal from JWT
- Infrastructure foundation:
  - PostgreSQL runtime config
  - Flyway baseline migration
  - Dockerfile
  - `compose.yaml` with PostgreSQL and MinIO
  - `.env.example`
- Delivery foundation:
  - GitHub Actions `build`, `test`, `secret_scan`, `container_scan`, `sast`, `sca`, `dast`, `fuzzing`
- API foundation:
  - unified JSON error format
  - health endpoint
  - OpenAPI endpoint
- Domain preparation:
  - package roots for `license`, `signature`, `malware`, `binaryapi`
  - `Ticket` and `TicketResponse` contracts
  - signature keystore generation script

## Recommended next implementation order

1. License database schema and services.
2. Signature keystore loading and signing service.
3. Ticket signing for license responses.
4. Malware signature schema, history and audit.
5. Binary API manifest and multipart serialization.
6. Optional file upload and MinIO integration.
