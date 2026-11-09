package com.ourvision.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record EventRequest(@NotBlank String title, @NotNull LocalDate eventDate, String venue, String description,
		String bannerUrl) {
}
