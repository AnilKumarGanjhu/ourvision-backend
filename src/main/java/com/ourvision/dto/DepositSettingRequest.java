package com.ourvision.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record DepositSettingRequest(@NotNull @DecimalMin("0.01") BigDecimal monthlyAmount,
		@NotNull LocalDate effectiveFrom) {
}