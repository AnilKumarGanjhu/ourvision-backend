package com.ourvision.controller;

import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/events")
@CrossOrigin
public class EventThumbnailController {

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    /**
     * Generate a compressed thumbnail in memory.
     * Nothing is saved to disk or database.
     *
     * Example:
     * GET /api/events/thumbnail/abc123.jpg
     */
    @GetMapping(
        value = "/thumbnail/{fileName:.+}",
        produces = MediaType.IMAGE_JPEG_VALUE
    )
    public ResponseEntity<byte[]> getThumbnail(
            @PathVariable String fileName) {

        // Allow only a simple filename, never a directory path.
        if (fileName == null
                || !fileName.matches("[A-Za-z0-9._-]+")
                || fileName.contains("..")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid image filename");
        }

        String lower = fileName.toLowerCase();

        // SVG is not a raster image; do not process it as JPEG.
        if (!(lower.endsWith(".jpg")
                || lower.endsWith(".jpeg")
                || lower.endsWith(".png")
                || lower.endsWith(".gif")
                || lower.endsWith(".webp")
                || lower.endsWith(".bmp"))) {
            throw new ResponseStatusException(
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Unsupported thumbnail image format");
        }

        try {
            Path uploadRoot = Paths.get(uploadDir)
                    .toAbsolutePath()
                    .normalize();

            Path imagePath = uploadRoot.resolve(fileName)
                    .normalize();

            // Prevent path traversal and access outside uploads.
            if (!imagePath.startsWith(uploadRoot)
                    || !Files.isRegularFile(imagePath)
                    || !Files.isReadable(imagePath)) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Image not found");
            }

            // Verify that the file is actually a readable image.
            if (ImageIO.read(imagePath.toFile()) == null) {
                throw new ResponseStatusException(
                        HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                        "File is not a supported raster image");
            }

            ByteArrayOutputStream output = new ByteArrayOutputStream();

            // Resize/compress in memory only.
            Thumbnails.of(imagePath.toFile())
                    .size(320, 220)
                    .outputFormat("jpg")
                    .outputQuality(0.72)
                    .toOutputStream(output);

            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .cacheControl(
                        CacheControl.maxAge(1, TimeUnit.HOURS)
                            .cachePublic())
                    .body(output.toByteArray());

        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (IOException | IllegalArgumentException ex) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to generate image thumbnail", ex);
        }
    }
}
