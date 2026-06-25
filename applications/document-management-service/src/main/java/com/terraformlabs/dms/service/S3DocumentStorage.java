package com.terraformlabs.dms.service;

import com.terraformlabs.dms.exception.BadRequestException;
import java.io.IOException;
import java.util.Objects;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Component
public class S3DocumentStorage {

    private final S3Client s3Client;

    public S3DocumentStorage(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    public void upload(String bucket, String objectKey, String contentType, byte[] content) {
        Objects.requireNonNull(bucket, "bucket must not be null");
        Objects.requireNonNull(objectKey, "objectKey must not be null");
        Objects.requireNonNull(content, "content must not be null");

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .contentType(contentType)
                    .build();
            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(content));
        } catch (S3Exception ex) {
            throw new BadRequestException("Unable to upload file to S3");
        }
    }

    public byte[] download(String bucket, String objectKey) {
        Objects.requireNonNull(bucket, "bucket must not be null");
        Objects.requireNonNull(objectKey, "objectKey must not be null");

        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .build();
            return s3Client.getObjectAsBytes(getObjectRequest).asByteArray();
        } catch (NoSuchKeyException ex) {
            throw new BadRequestException("Document content not found in S3");
        } catch (S3Exception | IOException ex) {
            throw new BadRequestException("Unable to download file from S3");
        }
    }
}
