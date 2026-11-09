package com.ourvision.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
public record MeetingRequest(@NotBlank String title, @NotNull LocalDate meetingDate, LocalTime meetingTime, String venue, String description) {}
