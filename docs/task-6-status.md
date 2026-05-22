# Task 6 Status

The optional signature-files subsystem is implemented.

## Implemented

- Added private object storage integration via MinIO.
- Added a dedicated `signature_files` table for uploaded file metadata.
- Added `POST /malware-signature-files/upload` for admin-only file upload and signature derivation.
- Added `POST /malware-signature-files/presigned-urls` for admin-only presigned download URLs by signature IDs.
- Added MinIO application-user bootstrap to `compose.yaml`.

## Current derivation strategy

- `firstBytesHex`: first `N` bytes of the uploaded file, where `N` is `SIGNATURE_FILE_FIRST_BYTES_COUNT` and defaults to `16`.
- `remainderHashHex`: `SHA-256` of the remaining bytes.
- `remainderLength`: number of bytes after the first window.
- `fileType`: lower-case file extension, or `bin` if missing.
- `offsetStart`: `0`
- `offsetEnd`: `firstBytesLength - 1`

## Notes

- Uploaded source files are stored in a private MinIO bucket with a non-root application user.
- Presigned URLs are generated only for signatures that have an uploaded source file.
- Empty files are rejected because the current signature derivation requires a non-empty first-byte window.
