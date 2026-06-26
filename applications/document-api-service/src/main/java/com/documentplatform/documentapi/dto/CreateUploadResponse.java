package com.documentplatform.documentapi.dto;

import com.documentplatform.documentapi.enums.DocumentStatus;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CreateUploadResponse {
    String documentId;
    String bucketName;
    String s3Key;
    String uploadUrl;
    DocumentStatus status;
    Long expiresInSeconds;
}
