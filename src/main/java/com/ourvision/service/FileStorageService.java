package com.ourvision.service;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

public interface FileStorageService {

    /** Store a MultipartFile — returns URL like "/uploads/uuid.ext" */
    String store(MultipartFile file) throws IOException;

    /** Store raw bytes — returns URL like "/uploads/uuid.ext" */
    String storeBytes(byte[] bytes, String friendlyName) throws IOException;

    /** Return the stored file size in bytes. */
    long getFileSize(String fileUrl) throws IOException;

    /** Delete file from disk */
    void deleteFile(String fileUrl);

    /** Detect file type from extension */
    String detectFileType(String originalName);
}