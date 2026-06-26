package com.terraformlabs.documentprocessor.dto;

import java.time.Instant;

public record ApiError(String message, Instant timestamp) {
}
