package com.documentplatform.documentreview.dto;

import com.documentplatform.documentreview.enums.DocumentStatus;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ApproveDocumentResponse {
    String documentId;
    DocumentStatus oldStatus;
    DocumentStatus newStatus;
    String approvedBy;
    String approvedAt;
    String message;
}
