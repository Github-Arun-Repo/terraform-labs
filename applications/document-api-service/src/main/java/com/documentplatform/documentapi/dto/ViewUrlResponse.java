package com.documentplatform.documentapi.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ViewUrlResponse {
    String documentId;
    String bucketName;
    String s3Key;
    String viewUrl;
    Long expiresInSeconds;
}
