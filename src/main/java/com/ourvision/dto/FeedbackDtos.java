package com.ourvision.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public final class FeedbackDtos {

    private FeedbackDtos() {
    }

    public record FeedbackRequest(@Min(1) @Max(5) int rating, @Size(max = 1000) String comment) {
    }

    public record FeedbackResponse(Long id, Long userId, String name, int rating, String comment,
            LocalDateTime createdAt) {
    }

    /** average is 0 when there are no reviews yet. */
    public record FeedbackList(double average, int count, List<FeedbackResponse> items) {
    }
}
