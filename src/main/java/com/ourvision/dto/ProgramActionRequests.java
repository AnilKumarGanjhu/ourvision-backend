package com.ourvision.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public final class ProgramActionRequests {

    private ProgramActionRequests() {
    }

    public record StatusRequest(@NotBlank String status) {
    }

    public record ParticipantRequest(@NotNull Long userId, String role) {
    }

    public record ExpenseRequest(@NotBlank String title, @NotNull BigDecimal amount, LocalDate expenseDate) {
    }
}
