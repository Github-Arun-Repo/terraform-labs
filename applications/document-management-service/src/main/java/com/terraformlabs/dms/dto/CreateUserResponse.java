package com.terraformlabs.dms.dto;

public record CreateUserResponse(
        Long userId,
        String username,
        String password
) {
}
