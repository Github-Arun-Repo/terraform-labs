package com.documentplatform.documentapi.util;

import com.documentplatform.documentapi.exception.BadRequestException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class FileNameSanitizer {

    public String sanitize(String originalFileName) {
        if (!StringUtils.hasText(originalFileName)) {
            throw new BadRequestException("fileName must not be blank");
        }

        String cleaned = StringUtils.cleanPath(originalFileName.trim());
        if (cleaned.contains("..") || cleaned.contains("/") || cleaned.contains("\\")) {
            throw new BadRequestException("Invalid fileName");
        }

        String sanitized = cleaned.replaceAll("[^a-zA-Z0-9._ -]", "-")
                .replace(" ", "-")
                .replaceAll("-+", "-");

        if (!StringUtils.hasText(sanitized) || sanitized.startsWith(".")) {
            throw new BadRequestException("Invalid fileName");
        }

        return sanitized;
    }
}
