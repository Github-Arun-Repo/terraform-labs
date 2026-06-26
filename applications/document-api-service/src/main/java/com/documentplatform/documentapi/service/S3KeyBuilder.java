package com.documentplatform.documentapi.service;

import com.documentplatform.documentapi.enums.DocumentType;
import com.documentplatform.documentapi.exception.BadRequestException;
import com.documentplatform.documentapi.util.FileNameSanitizer;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class S3KeyBuilder {

    private final FileNameSanitizer fileNameSanitizer;

    public S3KeyBuilder(FileNameSanitizer fileNameSanitizer) {
        this.fileNameSanitizer = fileNameSanitizer;
    }

    public String build(DocumentType documentType, String customerId, String documentId, String fileName) {
        if (!StringUtils.hasText(customerId)) {
            throw new BadRequestException("customerId must not be blank");
        }

        String normalizedCustomerId = customerId.trim().replaceAll("[^a-zA-Z0-9_-]", "-");
        String safeName = fileNameSanitizer.sanitize(fileName);

        return documentType.name().toLowerCase() + "/raw/" + normalizedCustomerId + "/" + documentId + "/" + safeName;
    }
}
