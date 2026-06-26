package com.terraformlabs.documentprocessor.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app")
@Validated
public class DocumentProcessorProperties {

    @Valid
    private final S3 s3 = new S3();

    @Valid
    private final Upload upload = new Upload();

    public S3 getS3() {
        return s3;
    }

    public Upload getUpload() {
        return upload;
    }

    public static class S3 {

        @NotBlank
        private String bucketName = "documents-inventory-s3";

        public String getBucketName() {
            return bucketName;
        }

        public void setBucketName(String bucketName) {
            this.bucketName = bucketName;
        }
    }

    public static class Upload {

        @Positive
        private long maxFileSizeBytes = 10_485_760;

        @NotEmpty
        private List<String> allowedExtensions = new ArrayList<>(List.of("PDF", "PNG", "JPEG", "JPG"));

        @NotEmpty
        private List<String> allowedContentTypes =
                new ArrayList<>(List.of("application/pdf", "image/png", "image/jpeg"));

        public long getMaxFileSizeBytes() {
            return maxFileSizeBytes;
        }

        public void setMaxFileSizeBytes(long maxFileSizeBytes) {
            this.maxFileSizeBytes = maxFileSizeBytes;
        }

        public List<String> getAllowedExtensions() {
            return allowedExtensions;
        }

        public void setAllowedExtensions(List<String> allowedExtensions) {
            this.allowedExtensions = allowedExtensions;
        }

        public List<String> getAllowedContentTypes() {
            return allowedContentTypes;
        }

        public void setAllowedContentTypes(List<String> allowedContentTypes) {
            this.allowedContentTypes = allowedContentTypes;
        }
    }
}
