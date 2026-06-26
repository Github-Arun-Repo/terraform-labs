package com.terraformlabs.documentprocessor.dto;

public record UploadInvoiceResponse(
        String bucketName,
        String objectKey,
        String customerId,
        String fileType,
        String contentType,
        long sizeBytes
) {
}
