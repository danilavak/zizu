# Task 5 Status

Task 5 binary API is implemented in the `zizu` repository.

Completed:

- Added a dedicated transport controller under `/api/binary/signatures`.
- Implemented binary endpoints:
  - `GET /api/binary/signatures/full`
  - `GET /api/binary/signatures/increment?since=...`
  - `POST /api/binary/signatures/by-ids`
- Added multipart/mixed responses with stable order:
  - `manifest.bin`
  - `data.bin`
- Added low-level BigEndian binary serialization helpers for:
  - `uint8`
  - `uint16`
  - `uint32`
  - `int64`
  - UUID
  - UTF-8 strings
  - byte arrays
- Added manifest signing over raw bytes through the signature module.
- Included manifest signature, existing record signatures, offsets, lengths, status codes, and `data.bin` SHA-256.
- Added integration tests for multipart parsing, manifest verification, full export, increment export, and by-ids export.

Protocol choices:

- Magic headers are ASCII:
  - `MF-VAK` for `manifest.bin`
  - `DB-VAK` for `data.bin`
- Multi-byte numeric fields are serialized in BigEndian.
- UUID values are serialized as two 64-bit parts: most significant bits first, then least significant bits.
