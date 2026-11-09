package com.ourvision.serviceImpl;

import com.ourvision.service.FileStorageService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    private final FileCompressionServiceImpl compressionService;

    public FileStorageServiceImpl(FileCompressionServiceImpl compressionService) {
        this.compressionService = compressionService;
    }

    @Value("${app.file.upload-dir:${file.upload-dir:uploads}}")
    private String uploadDir;

    @Override
    public String store(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty.");
        }

        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String originalName = StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "file" : file.getOriginalFilename()
        );

        // Compress/optimize supported formats before writing them to disk.
        // Unsupported formats safely fall back to the original bytes.
        byte[] originalBytes = file.getBytes();
        byte[] storedBytes = compressionService.compress(originalBytes, originalName,
                file.getContentType());

        String extension = "";
        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = originalName.substring(dotIndex);
        }

        String uniqueName = UUID.randomUUID().toString() + extension;
        Path target = uploadPath.resolve(uniqueName);

        Files.write(target, storedBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        return "/uploads/" + uniqueName;
    }

    @Override
    public String storeBytes(byte[] bytes, String friendlyName) throws IOException {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("Bytes cannot be empty.");
        }

        Path uploadPath = Paths.get(uploadDir);
        Files.createDirectories(uploadPath);

        String extension = ".bin";
        if (friendlyName != null) {
            int dot = friendlyName.lastIndexOf('.');
            if (dot > 0) extension = friendlyName.substring(dot);
        }

        String uniqueName = UUID.randomUUID().toString() + extension;
        Path target = uploadPath.resolve(uniqueName);

        Files.write(target, bytes);

        return "/uploads/" + uniqueName;
    }

    @Override
    public long getFileSize(String fileUrl) throws IOException {
        if (fileUrl == null || !fileUrl.startsWith("/uploads/")) {
            return 0L;
        }
        String fileName = fileUrl.substring("/uploads/".length());
        Path path = Paths.get(uploadDir).resolve(fileName).normalize();
        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path absolute = path.toAbsolutePath().normalize();
        if (!absolute.startsWith(root) || !Files.exists(absolute)) {
            return 0L;
        }
        return Files.size(absolute);
    }

    @Override
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || !fileUrl.startsWith("/uploads/")) return;
        try {
            String fileName = fileUrl.replace("/uploads/", "");
            Path path = Paths.get(uploadDir).resolve(fileName);
            Files.deleteIfExists(path);
        } catch (IOException e) {
            System.err.println("File delete failed: " + e.getMessage());
        }
    }

    @Override
    public String detectFileType(String originalName) {
        if (originalName == null) return "UNKNOWN";
        String lower = originalName.toLowerCase(java.util.Locale.ROOT);
        if (lower.matches(".*\\\\.(jpg|jpeg|png|gif|webp|avif|svg|bmp)$")) return "IMAGE";
        if (lower.matches(".*\\\\.(mp4|webm|mov|avi|mkv)$")) return "VIDEO";
        if (lower.matches(".*\\\\.(mp3|wav|aac|m4a|ogg|opus|flac)$")) return "AUDIO";
        if (lower.matches(".*\\\\.(zip|7z|rar|gz)$")) return "ARCHIVE";
        if (lower.matches(".*\\\\.pdf$")) return "DOCUMENT";
        return "DOCUMENT";
    }
}
