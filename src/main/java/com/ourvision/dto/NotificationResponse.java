package com.ourvision.dto;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        String title,
        String message,
        String category,
        Long programId,
        boolean read,
        LocalDateTime createdAt) {
}
