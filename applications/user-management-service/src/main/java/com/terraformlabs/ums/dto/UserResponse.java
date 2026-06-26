package com.terraformlabs.ums.dto;

import com.terraformlabs.ums.entity.Role;
import com.terraformlabs.ums.entity.UserStatus;
import java.time.Instant;
import java.util.Set;

public record UserResponse(
        Long id,
        String email,
        String fullName,
        UserStatus status,
        Set<Role> roles,
        Instant createdAt,
        Instant updatedAt
) {
}
