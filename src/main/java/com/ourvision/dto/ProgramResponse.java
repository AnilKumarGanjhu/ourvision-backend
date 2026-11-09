package com.ourvision.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ProgramResponse(
        Long id,
        String title,
        String category,
        String description,
        String status,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal budget,
        BigDecimal actualExpense,
        Integer progress,
        Long coordinatorId,
        String coordinatorName,
        String bannerUrl,
        int participantCount,
        List<Participant> participants,
        List<Expense> expenses,
        List<Media> media,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public record Participant(Long userId, String name, String email, String role, LocalDateTime joinedAt) {
    }

    public record Expense(Long id, String title, BigDecimal amount, LocalDate expenseDate) {
    }

    public record Media(Long id, String fileUrl, String originalName, String mediaType) {
    }
}
