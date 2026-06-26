package com.documentplatform.documentapi.dto;

import com.documentplatform.documentapi.enums.DocumentStatus;
import com.documentplatform.documentapi.enums.DocumentType;
import java.time.Instant;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DocumentResponse {
    String documentId;
    String customerId;
    DocumentType documentType;
    String fileName;
    String contentType;
    Long fileSize;
    String bucketName;
    String s3Key;
    DocumentStatus status;
    String uploadedBy;
    Instant createdAt;
    Instant updatedAt;
}
