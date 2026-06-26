package com.documentplatform.documentprocessing.service;

import com.documentplatform.documentprocessing.config.AppProperties;
import com.documentplatform.documentprocessing.model.DocumentItem;
import java.io.IOException;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

@Component
@RequiredArgsConstructor
public class S3ObjectValidator {

    private final S3Client s3Client;
    private final AppProperties appProperties;

    public void validate(DocumentItem document) {
        HeadObjectResponse head = s3Client.headObject(HeadObjectRequest.builder()
                .bucket(document.getBucketName())
                .key(document.getS3Key())
                .build());

        Long size = head.contentLength();
        String contentType = head.contentType() == null ? "" : head.contentType().toLowerCase(Locale.ROOT);

        if (size == null || size <= 0 || size > appProperties.getMaxFileSizeBytes()) {
            throw new DocumentValidationException("Invalid object size");
        }

        boolean allowed = appProperties.getAllowedContentTypes().stream()
                .map(v -> v.toLowerCase(Locale.ROOT))
                .anyMatch(contentType::equals);

        if (!allowed) {
            throw new DocumentValidationException("Invalid object content type");
        }

        byte[] signature = readSignature(document);
        if (!matchesMagicBytes(signature, contentType)) {
            throw new DocumentValidationException("Invalid object signature");
        }
    }

    private byte[] readSignature(DocumentItem document) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(document.getBucketName())
                .key(document.getS3Key())
                .range("bytes=0-7")
                .build();

        try (var stream = s3Client.getObject(request)) {
            return stream.readNBytes(8);
        } catch (IOException ex) {
            throw new DocumentValidationException("Unable to read object signature", ex);
        }
    }

    private boolean matchesMagicBytes(byte[] data, String contentType) {
        if (data.length < 4) {
            return false;
        }

        return switch (contentType) {
            case "application/pdf" -> hasPrefix(data, new int[]{0x25, 0x50, 0x44, 0x46});
            case "image/png" -> hasPrefix(data, new int[]{0x89, 0x50, 0x4E, 0x47});
            case "image/jpeg" -> hasPrefix(data, new int[]{0xFF, 0xD8, 0xFF});
                case "image/tiff" -> hasPrefix(data, new int[]{0x49, 0x49, 0x2A})
                    || hasPrefix(data, new int[]{0x4D, 0x4D, 0x00, 0x2A});
            default -> false;
        };
    }

    private boolean hasPrefix(byte[] data, int[] prefix) {
        if (data.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if ((data[i] & 0xFF) != prefix[i]) {
                return false;
            }
        }
        return true;
    }
}
