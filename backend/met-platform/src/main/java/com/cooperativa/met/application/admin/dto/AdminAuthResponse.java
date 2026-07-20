package com.cooperativa.met.application.admin.dto;

import java.util.UUID;

public record AdminAuthResponse(
        UUID adminId,
        String username,
        String fullName,
        String role,
        String accessToken,
        Long expiresInMs
) {
    public static AdminAuthResponse of(UUID adminId, String username, String fullName,
                                       String role, String accessToken, Long expiresInMs) {
        return new AdminAuthResponse(adminId, username, fullName, role, accessToken, expiresInMs);
    }
}
