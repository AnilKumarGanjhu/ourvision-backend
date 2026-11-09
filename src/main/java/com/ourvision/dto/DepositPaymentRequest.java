package com.ourvision.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record DepositPaymentRequest(@NotNull Long userId, @NotNull LocalDate month,
		@NotNull @DecimalMin("0.01") BigDecimal amount, LocalDate paymentDate) {
}
