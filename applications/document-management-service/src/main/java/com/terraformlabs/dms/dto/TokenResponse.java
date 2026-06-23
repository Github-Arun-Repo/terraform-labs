package com.terraformlabs.dms.dto;

public record TokenResponse(
        String token,
        String tokenType,
        long expiresInSeconds
) {
}
