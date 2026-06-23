package com.terraformlabs.dms.dto;

import java.time.Instant;

public record ApiError(
        String message,
        Instant timestamp
) {
}
