# OurVision Google Drive OAuth – backend integration notes

## Important status
This archive has been prepared for a safe Google Drive integration, but the existing photo APIs still use `ImageStorageServiceImpl` (local filesystem). `GOOGLE_DRIVE_ENABLED` deliberately defaults to `false`. Do **not** set it to true yet: the OAuth callback, token exchange/refresh, Drive upload/download adapter, and API wiring still need implementation and verification against this project. This avoids silently breaking profile/member photo uploads.

## Current photo flow
- Profile photo: `PUT/GET /api/users/{id}/image`, column `users.image_url`.
- Member-information photo: `PUT/GET /api/users/{id}/information/photo`, column `users.member_photo_url`.
- Current service: `serviceImpl/ImageStorageServiceImpl.java`.
- Existing Base64/local-file migration: `serviceImpl/ImageMigrationRunner.java` controlled by `MIGRATE_IMAGES`.

## Google Cloud setup
1. Create a Google Cloud project and enable Google Drive API.
2. Configure OAuth consent screen. Add the personal Gmail account as a test user while testing.
3. Create OAuth Client ID of type Web application.
4. Add redirect URI `http://localhost:8080/api/google-drive/oauth/callback` and the production URI `https://YOUR-SERVICE.onrender.com/api/google-drive/oauth/callback`.
5. Request offline access and the narrow `drive.file` scope (`https://www.googleapis.com/auth/drive.file`). Use the same OAuth client/account for folder creation and all uploads.
6. Before production, publish/configure the consent screen and complete any Google verification requirements applicable to the chosen scopes. External apps left in Testing can have refresh-token expiry limitations.

## Environment variables
Set these in Render (never commit values):
- `GOOGLE_DRIVE_ENABLED=false` until the full adapter is implemented and tested.
- `GOOGLE_CLIENT_ID`
- `GOOGLE_CLIENT_SECRET`
- `GOOGLE_REDIRECT_URI`
- `GOOGLE_DRIVE_ROOT_FOLDER_ID`
- `GOOGLE_TOKEN_ENCRYPTION_KEY` (32-byte key encoded as Base64; backend must use AES-256-GCM with a fresh random 12-byte nonce per encryption).
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `GEMINI_API_KEY`.

## Database migration
Flyway migration `V7__google_drive_storage_support.sql` adds encrypted OAuth credential storage and an idempotent photo-migration ledger. The refresh token must only ever be persisted as authenticated ciphertext. Never log OAuth codes, access tokens, refresh tokens, client secrets, or image bytes.

## Required implementation behavior before enabling Drive
1. Admin-only OAuth start/disconnect/status endpoints. Callback must validate a cryptographically random, one-time `state` value and bind it to the initiating admin session.
2. Exchange authorization code server-side; request offline access. Preserve an existing refresh token if Google omits a new refresh token on a subsequent authorization. Encrypt refresh tokens with AES-256-GCM.
3. Refresh access tokens server-side; handle revocation by marking the connection disconnected and asking an admin to reconnect.
4. Create private folders (`OurVision/MemberPhotos`, `OurVision/ProfilePhotos`, optionally `Documents`). Do not make member photos public.
5. Upload with Drive API and store the returned Drive file ID in `users.image_url` / `users.member_photo_url` or a dedicated file table. Download through authenticated backend endpoints; do not expose public Drive links.
6. On replacement, upload and verify the new file first, update the DB reference transactionally, then delete the old Drive file. On delete, verify ownership and authorization.
7. Migration must be resumable and idempotent. Process bounded batches, hash each source value, upload, download/verify checksum and content type, update reference, then and only then clear Base64. Keep originals until verification and backup are complete.
8. Enforce admin authorization for migration and settings endpoints. Existing photo GET endpoints currently have public access in `SecurityConfig`; review whether profile photos are intentionally public, and keep sensitive member-information photos restricted.

## Critical credential action
The source archive contained database and Gemini credentials in YAML defaults. Those defaults have been removed from this prepared copy. Rotate the previously exposed Neon database password and Gemini API key, then set new values as Render environment variables.

## Build verification
The editing environment used for this archive did not have Maven installed, so a Maven compile/test could not be run here. Run `mvn clean verify` in your Java 21 development environment before deployment.
