package com.terraformlabs.ums.dto;

import com.terraformlabs.ums.entity.Role;
import java.util.Set;

public record TokenValidationResponse(
        boolean valid,
        Long userId,
        String email,
        Set<Role> roles,
        long expiresAtEpochSeconds
) {
}
