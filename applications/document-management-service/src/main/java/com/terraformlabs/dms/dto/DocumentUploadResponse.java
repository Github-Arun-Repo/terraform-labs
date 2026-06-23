package com.terraformlabs.dms.dto;

import com.terraformlabs.dms.entity.DocumentType;
import java.time.Instant;

public record DocumentUploadResponse(
        Long documentId,
        String originalFilename,
        DocumentType docType,
        Long size,
        Instant createdAt
) {
}
