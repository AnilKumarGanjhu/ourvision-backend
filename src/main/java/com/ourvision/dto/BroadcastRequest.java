package com.ourvision.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** programId null = all active members; otherwise only that programme's participants. */
public record BroadcastRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank String message,
        Long programId) {
}
