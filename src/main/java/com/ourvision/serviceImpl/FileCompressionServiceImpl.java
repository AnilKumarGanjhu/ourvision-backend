package com.ourvision.serviceImpl;

import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.*;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Central file optimization pipeline used by FileStorageService.
 *
 * Image optimization runs in-process. PDF and audio/video optimization use
 * optional qpdf/ffmpeg executables. If an engine is unavailable or cannot
 * process a file, original bytes are returned so uploads continue safely.
 * ZIP/RAR/7z and unsupported types are preserved unchanged.
 */
@Service
public class FileCompressionServiceImpl {

    @Value("${file.compression.image-quality:0.85}")
    private double imageQuality;

    @Value("${file.compression.process-timeout-seconds:120}")
    private long processTimeoutSeconds;

    @Value("${file.compression.qpdf-command:qpdf}")
    private String qpdfCommand;

    @Value("${file.compression.ffmpeg-command:ffmpeg}")
    private String ffmpegCommand;

    public byte[] compress(byte[] original, String originalName, String contentType) {
        if (original == null || original.length == 0) {
            return original;
        }

        String extension = extensionOf(originalName);
        try {
            return switch (extension) {
                case "jpg", "jpeg", "png", "webp" ->
                        optimizeImage(original, extension);
                case "pdf" ->
                        runOptionalEngine(original, extension, Engine.PDF);
                case "mp4", "mov", "mkv", "webm", "avi" ->
                        runOptionalEngine(original, extension, Engine.VIDEO);
                case "mp3", "aac", "m4a", "ogg", "opus" ->
                        runOptionalEngine(original, extension, Engine.AUDIO);
                // Already compressed archives and unknown formats are kept as-is.
                case "zip", "7z", "rar", "gz", "flac", "wav", "gif", "bmp", "svg" ->
                        original;
                default -> original;
            };
        } catch (Exception ex) {
            // Compression must not turn a valid upload into a failed upload.
            return original;
        }
    }

    private byte[] optimizeImage(byte[] original, String extension) throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(original));
        if (image == null) {
            return original;
        }

        String outputFormat = extension.equals("jpg") ? "jpeg" : extension;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Thumbnails.of(image)
                .scale(1.0) // preserve original dimensions
                .outputFormat(outputFormat)
                .outputQuality(Math.max(0.50, Math.min(1.0, imageQuality)))
                .toOutputStream(output);

        byte[] candidate = output.toByteArray();
        // Keep original if re-encoding does not actually reduce the size.
        return candidate.length > 0 && candidate.length < original.length
                ? candidate : original;
    }

    private byte[] runOptionalEngine(byte[] original, String extension, Engine engine) {
        Path input = null;
        Path output = null;
        try {
            input = Files.createTempFile("ourvision-compress-input-", "." + extension);
            output = Files.createTempFile("ourvision-compress-output-", "." + extension);
            Files.write(input, original);
            Files.deleteIfExists(output);

            ProcessBuilder builder;
            if (engine == Engine.PDF) {
                builder = new ProcessBuilder(
                        qpdfCommand,
                        "--object-streams=generate",
                        "--stream-data=compress",
                        input.toAbsolutePath().toString(),
                        output.toAbsolutePath().toString()
                );
            } else if (engine == Engine.VIDEO) {
                if (extension.equals("webm")) {
                    builder = new ProcessBuilder(
                            ffmpegCommand, "-y", "-v", "error",
                            "-i", input.toAbsolutePath().toString(),
                            "-map_metadata", "0",
                            "-c:v", "libvpx-vp9", "-crf", "32", "-b:v", "0",
                            "-c:a", "libopus", "-b:a", "128k",
                            output.toAbsolutePath().toString()
                    );
                } else {
                    builder = new ProcessBuilder(
                            ffmpegCommand, "-y", "-v", "error",
                            "-i", input.toAbsolutePath().toString(),
                            "-map_metadata", "0",
                            "-c:v", "libx264", "-crf", "23", "-preset", "medium",
                            "-c:a", "aac", "-b:a", "192k",
                            output.toAbsolutePath().toString()
                    );
                }
            } else {
                // Audio is re-encoded only for lossy formats supported by this preset.
                if (!extension.equals("mp3")) {
                    return original;
                }
                builder = new ProcessBuilder(
                        ffmpegCommand, "-y", "-v", "error",
                        "-i", input.toAbsolutePath().toString(),
                        "-vn", "-c:a", "libmp3lame", "-b:a", "192k",
                        output.toAbsolutePath().toString()
                );
            }

            builder.redirectErrorStream(true);
            Process process = builder.start();
            boolean finished = process.waitFor(Math.max(1, processTimeoutSeconds), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return original;
            }
            if (process.exitValue() != 0 || !Files.isRegularFile(output)) {
                return original;
            }

            byte[] candidate = Files.readAllBytes(output);
            return candidate.length > 0 && candidate.length < original.length
                    ? candidate : original;
        } catch (Exception ex) {
            return original;
        } finally {
            deleteQuietly(input);
            deleteQuietly(output);
        }
    }

    private String extensionOf(String originalName) {
        if (originalName == null) return "";
        String cleanName = originalName.toLowerCase(Locale.ROOT);
        int dot = cleanName.lastIndexOf('.');
        return dot >= 0 && dot < cleanName.length() - 1
                ? cleanName.substring(dot + 1) : "";
    }

    private void deleteQuietly(Path path) {
        if (path != null) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException ignored) {
                // Best-effort cleanup of temporary files.
            }
        }
    }

    private enum Engine { PDF, VIDEO, AUDIO }
}
