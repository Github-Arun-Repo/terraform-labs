package com.documentplatform.documentreview.dto;

import com.documentplatform.documentreview.enums.DocumentStatus;
import com.documentplatform.documentreview.enums.RejectReasonCode;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RejectDocumentResponse {
    String documentId;
    DocumentStatus oldStatus;
    DocumentStatus newStatus;
    String rejectedBy;
    String rejectedAt;
    RejectReasonCode reasonCode;
    String message;
}
