package com.ourvision.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Used for create and update. The form always sends the full object: null description, dates or coordinator clear that field. */
public record ProgramRequest(
        @NotBlank(message = "Title is required") @Size(max = 200) String title,
        @NotBlank(message = "Category is required") String category,
        String description,
        String status,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal budget,
        Integer progress,
        Long coordinatorId) {
}
