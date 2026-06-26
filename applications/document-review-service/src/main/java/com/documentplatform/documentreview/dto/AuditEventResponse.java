package com.documentplatform.documentreview.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AuditEventResponse {
    String eventType;
    String oldStatus;
    String newStatus;
    String performedBy;
    String message;
    String createdAt;
}
