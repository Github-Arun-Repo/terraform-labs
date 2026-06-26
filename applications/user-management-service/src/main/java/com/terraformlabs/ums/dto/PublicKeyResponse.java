package com.terraformlabs.ums.dto;

public record PublicKeyResponse(
        boolean rsaModeEnabled,
        String algorithm,
        String publicKeyPem
) {
}
