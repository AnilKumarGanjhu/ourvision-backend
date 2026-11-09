package com.ourvision.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record AttendanceBulkRequest(@NotNull Long meetingId, @Valid List<Entry> entries) {
	public record Entry(@NotNull Long userId, @NotNull String status) {
	}
}