package com.ourvision.dto;
public record LoginResponse(String accessToken, Long userId, String name, String role) {}
