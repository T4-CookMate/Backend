package com.cookmate.orchestrator.Auth.DTO;

public record AuthResponse(
        Long userId,
        String email,
        String name,
        String profileImageUrl,
        String accessToken,
        String refreshToken
) {}