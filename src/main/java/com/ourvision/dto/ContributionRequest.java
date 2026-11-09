package com.ourvision.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
public record ContributionRequest(@NotNull Long userId, @NotNull @Positive BigDecimal amount,
 @NotBlank String month, @NotBlank String paymentMode, String receiptNo) {}
