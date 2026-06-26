package com.documentplatform.documentreview.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ErrorResponse {
    String timestamp;
    int status;
    String error;
    String message;
    String path;
    String correlationId;
    Long currentRevision;
    Long expectedRevision;
}
