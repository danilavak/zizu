# Task 3 Status

Task 3 foundation is implemented in the `zizu` repository.

Completed:

- Added a keystore-backed signature module with cached key loading from either `SIGNATURE_KEYSTORE_BASE64` or `SIGNATURE_KEYSTORE_LOCATION`.
- Added deterministic JSON canonicalization for signed DTO payloads.
- Implemented `SHA256withRSA` signing and verification with Base64 output.
- Integrated ticket signing into the license module so `TicketResponse.signature` is now populated.
- Added a public certificate endpoint: `GET /signature/certificate`.
- Generated a local development keystore under the ignored `secrets/` directory.
- Synced signature keystore secrets to GitHub repository `danilavak/zizu`.
- Added integration tests for sign/verify behavior and certificate exposure.

Runtime notes:

- For PKCS12 keystores, keep key password equal to store password.
- Local development secrets are stored only in the ignored `secrets/` directory.
- Clients must verify signatures over the same canonical JSON representation as the server.
