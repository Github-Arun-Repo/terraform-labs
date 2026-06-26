package com.terraformlabs.ums.dto;

import java.time.Instant;

public record ApiError(String message, Instant timestamp) {
}
