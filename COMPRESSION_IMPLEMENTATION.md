# OurVision Backend - Automatic File Compression Integration

## What changed
- Added `FileCompressionService` as a common compression pipeline.
- `FileStorageServiceImpl.store(MultipartFile)` now passes file bytes through the compression pipeline before saving to the existing `uploads/` directory.
- `EventServiceImpl` records the actual stored file size after compression rather than the original multipart size.
- `FileStorageService` now exposes `getFileSize(...)` to retrieve the stored file size.
- The existing upload API routes and database entity structure were retained.
- Member profile image uploads also pass through the same optimizer before the existing thumbnail/data-URL processing.

## Current behavior
- JPG/JPEG/PNG/WebP: attempts same-format image re-encoding without changing dimensions; uses the smaller output only.
- PDF: attempts qpdf structural optimization when qpdf is installed and callable.
- MP4/MOV/MKV/WebM/AVI: attempts FFmpeg H.264/AAC re-encoding when FFmpeg is installed and the container/codecs are supported; keeps the original if output is not smaller.
- MP3: attempts FFmpeg MP3 re-encoding when FFmpeg is installed; keeps the original if output is not smaller.
- ZIP/7z/RAR/GZ, FLAC/WAV, GIF/BMP/SVG, and unsupported formats are preserved unchanged.
- If an optional engine is unavailable, fails, times out, or produces a larger file, the original upload is saved instead.
- Temporary engine files are cleaned up on a best-effort basis.

## Important storage clarification
The current project uses two storage patterns:
- Event attachments are written to the `uploads/` directory; their URL/path and metadata are stored in PostgreSQL.
- Member profile images are stored in the existing user image column as a thumbnail data URL.

This change preserves those existing patterns. It does not migrate event attachments into PostgreSQL binary columns.

## Optional engine installation
- For PDF optimization, install qpdf and make `qpdf` available on the backend process PATH.
- For video/MP3 optimization, install FFmpeg and make `ffmpeg` available on the backend process PATH.
- Override commands/timeouts/quality in `application.yml` or environment-specific configuration:
  - `file.compression.image-quality` (default 0.85)
  - `file.compression.process-timeout-seconds` (default 120)
  - `file.compression.qpdf-command` (default `qpdf`)
  - `file.compression.ffmpeg-command` (default `ffmpeg`)

## Workflow
1. Existing frontend sends a file to its current upload API.
2. Controller/service calls `FileStorageService.store(file)`.
3. Storage service invokes `FileCompressionService.compress(...)`.
4. The service detects the file extension and chooses an engine with a `switch`.
5. If compression succeeds and the output is smaller, compressed bytes are saved. Otherwise original bytes are saved.
6. The upload URL and metadata are stored as before.

## Validation
Run `mvn test` from the backend project directory. Test image, PDF, video and audio uploads in a development environment before production use. The optional command-line engines must be installed separately; they are not bundled in this source ZIP.
