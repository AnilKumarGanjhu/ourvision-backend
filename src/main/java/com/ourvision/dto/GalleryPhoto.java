package com.ourvision.dto;

import java.time.LocalDateTime;

/** A programme photo or video shown on the Gallery page. mediaType is PHOTO or VIDEO. */
public record GalleryPhoto(Long id, String fileUrl, String mediaType, Long programId, String programTitle,
        LocalDateTime createdAt) {
}
