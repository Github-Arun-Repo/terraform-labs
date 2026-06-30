package com.terraformlabs.documentprocessor.service;

import com.terraformlabs.documentprocessor.config.DocumentProcessorProperties;
import com.terraformlabs.documentprocessor.dto.UploadConstraintsResponse;
import com.terraformlabs.documentprocessor.dto.UploadInvoiceResponse;
import com.terraformlabs.documentprocessor.exception.BadRequestException;
import com.terraformlabs.documentprocessor.exception.StorageException;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Service
public class InvoiceUploadService {

    private final S3Client s3Client;
    private final DocumentProcessorProperties properties;

    public InvoiceUploadService(S3Client s3Client, DocumentProcessorProperties properties) {
        this.s3Client = s3Client;
        this.properties = properties;
    }

    public UploadInvoiceResponse uploadInvoice(String customerId, MultipartFile file) {
        validateFile(file);

        String originalFileName = cleanFileName(file.getOriginalFilename());
        String extension = extractExtension(originalFileName);
        validateFileType(extension, file.getContentType());

        String objectKey = buildObjectKey(extension, customerId, originalFileName);
        String contentType = file.getContentType() == null ? "application/octet-stream" : file.getContentType();

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(properties.getS3().getBucketName())
                    .key(objectKey)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));
        } catch (S3Exception | IOException ex) {
            throw new StorageException("Failed to upload invoice to S3", ex);
        }

        return new UploadInvoiceResponse(
                properties.getS3().getBucketName(),
                objectKey,
                customerId,
                extension,
                contentType,
                file.getSize()
        );
    }

    public UploadConstraintsResponse getConstraints() {
        return new UploadConstraintsResponse(
                properties.getS3().getBucketName(),
                properties.getUpload().getMaxFileSizeBytes(),
                properties.getUpload().getAllowedExtensions(),
                properties.getUpload().getAllowedContentTypes()
        );
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is required and cannot be empty");
        }

        if (file.getSize() > properties.getUpload().getMaxFileSizeBytes()) {
            throw new BadRequestException("File exceeds max allowed size");
        }

        if (!StringUtils.hasText(file.getOriginalFilename())) {
            throw new BadRequestException("File name is missing");
        }
    }

    private String cleanFileName(String originalFileName) {
        String cleaned = StringUtils.cleanPath(originalFileName);

        if (cleaned.contains("..")) {
            throw new BadRequestException("Invalid file name");
        }

        return cleaned.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String extractExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');

        if (lastDot <= 0 || lastDot == fileName.length() - 1) {
            throw new BadRequestException("File extension is required");
        }

        return fileName.substring(lastDot + 1).toLowerCase(Locale.ROOT);
    }

    private void validateFileType(String extension, String contentType) {
        List<String> allowedExtensions = properties.getUpload().getAllowedExtensions().stream()
                .map(value -> value.toLowerCase(Locale.ROOT))
                .toList();

        if (!allowedExtensions.contains(extension)) {
            throw new BadRequestException("File type is not allowed");
        }

        if (contentType == null) {
            throw new BadRequestException("Content type is missing");
        }

        List<String> allowedContentTypes = properties.getUpload().getAllowedContentTypes().stream()
                .map(value -> value.toLowerCase(Locale.ROOT))
                .toList();

        if (!allowedContentTypes.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new BadRequestException("Content type is not allowed");
        }
    }

    private String buildObjectKey(String extension, String customerId, String fileName) {
        // This is the standalone/legacy upload path. It deliberately writes under a dedicated
        // "legacy/raw/" namespace that is OUTSIDE the S3 -> SQS event-processing prefixes
        // (invoice/raw/, receipt/raw/). The async pipeline keys documents off DynamoDB metadata
        // that this service does not create, so routing here into those prefixes would only
        // generate unresolvable poison messages on the ingestion DLQ.
        String uniqueName = Instant.now().toEpochMilli() + "-" + UUID.randomUUID() + "-" + fileName;
        return "legacy/raw/" + extension + "/" + customerId + "/" + uniqueName;
    }
}
