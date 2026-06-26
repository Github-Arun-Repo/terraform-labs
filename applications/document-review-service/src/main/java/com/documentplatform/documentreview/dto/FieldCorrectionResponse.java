package com.documentplatform.documentreview.dto;

import com.documentplatform.documentreview.enums.DocumentStatus;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class FieldCorrectionResponse {
    String documentId;
    DocumentStatus status;
    Long documentRevision;
    String message;
}
