package com.cookmate.orchestrator.Auth.DTO;

public record GoogleUserInfo(
        String sub,        // Google 고유 사용자 ID
        String email,
        String name,
        String picture
) {}