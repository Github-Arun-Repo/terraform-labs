package com.documentplatform.documentreview.dto;

import com.documentplatform.documentreview.enums.DocumentStatus;
import com.documentplatform.documentreview.enums.DocumentType;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DocumentSummaryResponse {
    String documentId;
    String customerId;
    DocumentType documentType;
    String fileName;
    String contentType;
    Long fileSize;
    String bucketName;
    String s3Key;
    DocumentStatus status;
    Long documentRevision;
    String uploadedBy;
    String createdAt;
    String updatedAt;
}
