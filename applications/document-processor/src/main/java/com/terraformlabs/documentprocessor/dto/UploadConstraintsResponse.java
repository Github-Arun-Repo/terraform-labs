package com.terraformlabs.documentprocessor.dto;

import java.util.List;

public record UploadConstraintsResponse(
        String bucketName,
        long maxFileSizeBytes,
        List<String> allowedExtensions,
        List<String> allowedContentTypes
) {
}
