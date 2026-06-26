package com.documentplatform.documentreview.dto;

import com.documentplatform.documentreview.enums.DocumentStatus;
import com.documentplatform.documentreview.enums.DocumentType;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ReviewQueueItemResponse {
    String documentId;
    String customerId;
    DocumentType documentType;
    String fileName;
    String supplierName;
    String invoiceNumber;
    String invoiceDate;
    Double totalAmount;
    String currency;
    Double confidenceScore;
    DocumentStatus status;
    String updatedAt;
}
