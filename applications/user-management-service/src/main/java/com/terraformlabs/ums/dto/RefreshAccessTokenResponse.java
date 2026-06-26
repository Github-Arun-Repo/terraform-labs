package com.terraformlabs.ums.dto;

public record RefreshAccessTokenResponse(
        String accessToken,
        long expiresIn
) {
}
