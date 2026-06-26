package com.documentplatform.documentapi.service;

import com.documentplatform.documentapi.config.AwsProperties;
import com.documentplatform.documentapi.config.DocumentProperties;
import com.documentplatform.documentapi.dto.CreateUploadRequest;
import com.documentplatform.documentapi.dto.CreateUploadResponse;
import com.documentplatform.documentapi.dto.DocumentResponse;
import com.documentplatform.documentapi.dto.PagedDocumentResponse;
import com.documentplatform.documentapi.dto.ViewUrlResponse;
import com.documentplatform.documentapi.enums.DocumentStatus;
import com.documentplatform.documentapi.enums.DocumentType;
import com.documentplatform.documentapi.exception.BadRequestException;
import com.documentplatform.documentapi.exception.DocumentNotFoundException;
import com.documentplatform.documentapi.model.DocumentItem;
import com.documentplatform.documentapi.repository.DynamoDbDocumentRepository;
import com.documentplatform.documentapi.security.AuthenticatedUser;
import com.documentplatform.documentapi.util.FileNameSanitizer;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class DocumentService {

    private final DynamoDbDocumentRepository documentRepository;
    private final DocumentIdGenerator documentIdGenerator;
    private final S3KeyBuilder s3KeyBuilder;
    private final S3PresignedUrlService s3PresignedUrlService;
    private final FileNameSanitizer fileNameSanitizer;
    private final DocumentProperties documentProperties;
    private final AwsProperties awsProperties;
    private final DocumentMetricsService documentMetricsService;

    public DocumentService(
            DynamoDbDocumentRepository documentRepository,
            DocumentIdGenerator documentIdGenerator,
            S3KeyBuilder s3KeyBuilder,
            S3PresignedUrlService s3PresignedUrlService,
            FileNameSanitizer fileNameSanitizer,
            DocumentProperties documentProperties,
            AwsProperties awsProperties,
            DocumentMetricsService documentMetricsService
    ) {
        this.documentRepository = documentRepository;
        this.documentIdGenerator = documentIdGenerator;
        this.s3KeyBuilder = s3KeyBuilder;
        this.s3PresignedUrlService = s3PresignedUrlService;
        this.fileNameSanitizer = fileNameSanitizer;
        this.documentProperties = documentProperties;
        this.awsProperties = awsProperties;
        this.documentMetricsService = documentMetricsService;
    }

    @Transactional
    public CreateUploadResponse createUploadRequest(CreateUploadRequest request, AuthenticatedUser user) {
        documentMetricsService.uploadRequestsTotal().increment();
        Timer.Sample sample = Timer.start();
        try {
            validateBusinessRules(request);
            String documentId = documentIdGenerator.generate();
            String safeFileName = fileNameSanitizer.sanitize(request.getFileName());
            String s3Key = s3KeyBuilder.build(request.getDocumentType(), request.getCustomerId(), documentId, safeFileName);
            Instant now = Instant.now();

            DocumentItem entity = new DocumentItem();
            entity.setPk("DOCUMENT#" + documentId);
            entity.setSk("METADATA");
            entity.setEntityType("DOCUMENT");
            entity.setDocumentId(documentId);
            entity.setCustomerId(request.getCustomerId().trim());
            entity.setDocumentType(request.getDocumentType());
            entity.setFileName(safeFileName);
            entity.setContentType(request.getContentType().trim().toLowerCase());
            entity.setFileSize(request.getFileSize());
            entity.setBucketName(awsProperties.getS3().getBucketName());
            entity.setS3Key(s3Key);
            entity.setStatus(DocumentStatus.UPLOAD_REQUESTED);
            entity.setUploadedBy(user.userId());
            entity.setProcessingAttempts(0);
            entity.setDocumentRevision(1L);
            entity.setCreatedAt(now.toString());
            entity.setUpdatedAt(now.toString());
            entity.setGsi1Pk("S3KEY#" + entity.getS3Key());
            entity.setGsi1Sk("DOCUMENT#" + documentId);
            entity.setGsi2Pk("CUSTOMER#" + entity.getCustomerId() + "#STATUS#" + entity.getStatus().name());
            entity.setGsi2Sk("UPDATED_AT#" + now + "#DOCUMENT#" + documentId);

            documentRepository.save(entity);
            log.info("document metadata created documentId={} customerId={} uploadedBy={}",
                    entity.getDocumentId(), entity.getCustomerId(), entity.getUploadedBy());

            Duration expiry = Duration.ofMinutes(awsProperties.getS3().getUploadUrlExpiryMinutes());
            String uploadUrl = s3PresignedUrlService.generateUploadUrl(
                    entity.getBucketName(), entity.getS3Key(), entity.getContentType(), expiry);

            log.info("presigned upload URL generated documentId={} expirySeconds={}",
                    entity.getDocumentId(), expiry.toSeconds());

            return CreateUploadResponse.builder()
                    .documentId(entity.getDocumentId())
                    .bucketName(entity.getBucketName())
                    .s3Key(entity.getS3Key())
                    .uploadUrl(uploadUrl)
                    .status(entity.getStatus())
                    .expiresInSeconds(expiry.toSeconds())
                    .build();
        } catch (RuntimeException ex) {
            documentMetricsService.uploadRequestsFailedTotal().increment();
            throw ex;
        } finally {
            sample.stop(documentMetricsService.uploadDuration());
        }
    }

    @Transactional(readOnly = true)
    public PagedDocumentResponse listDocuments(String customerId, DocumentStatus status, DocumentType documentType, int page, int size) {
        if (customerId == null || customerId.isBlank()) {
            throw new BadRequestException("customerId is required for DynamoDB queries");
        }
        if (status == null) {
            throw new BadRequestException("status is required for DynamoDB queries");
        }

        DynamoDbDocumentRepository.Holder<String> next = new DynamoDbDocumentRepository.Holder<>();
        java.util.List<DocumentItem> items = documentRepository.listByCustomerAndStatus(customerId, status, documentType, size, null, next);
        log.info("document list fetched page={} size={} returned={}", page, size, items.size());

        return PagedDocumentResponse.builder()
            .content(items.stream().map(this::toResponse).toList())
            .page(page)
            .size(size)
            .totalElements(items.size())
            .totalPages(1)
                .build();
    }

    @Transactional(readOnly = true)
    public DocumentResponse getDocument(String documentId) {
        DocumentItem entity = documentRepository.findByDocumentId(documentId)
                .orElseThrow(() -> new DocumentNotFoundException("Document not found: " + documentId));
        log.info("document details fetched documentId={}", documentId);
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public ViewUrlResponse generateViewUrl(String documentId) {
        DocumentItem entity = documentRepository.findByDocumentId(documentId)
                .orElseThrow(() -> new DocumentNotFoundException("Document not found: " + documentId));

        Duration expiry = Duration.ofMinutes(awsProperties.getS3().getViewUrlExpiryMinutes());
        String viewUrl = s3PresignedUrlService.generateViewUrl(entity.getBucketName(), entity.getS3Key(), expiry);
        documentMetricsService.viewUrlGeneratedTotal().increment();

        log.info("view URL generated documentId={} expirySeconds={}", documentId, expiry.toSeconds());
        return ViewUrlResponse.builder()
                .documentId(entity.getDocumentId())
                .bucketName(entity.getBucketName())
                .s3Key(entity.getS3Key())
                .viewUrl(viewUrl)
                .expiresInSeconds(expiry.toSeconds())
                .build();
    }

    private void validateBusinessRules(CreateUploadRequest request) {
        if (request.getFileSize() > documentProperties.getMaxFileSizeBytes()) {
            throw new BadRequestException("fileSize exceeds max allowed limit");
        }

        String contentType = request.getContentType().trim().toLowerCase();
        boolean allowed = documentProperties.getAllowedContentTypes().stream()
                .map(String::toLowerCase)
                .anyMatch(contentType::equals);

        if (!allowed) {
            throw new BadRequestException("Unsupported content type: " + request.getContentType());
        }

        if (request.getDocumentType() != DocumentType.INVOICE && request.getDocumentType() != DocumentType.RECEIPT) {
            throw new BadRequestException("Unsupported document type: " + request.getDocumentType());
        }
    }

    private DocumentResponse toResponse(DocumentItem entity) {
        return DocumentResponse.builder()
                .documentId(entity.getDocumentId())
                .customerId(entity.getCustomerId())
                .documentType(entity.getDocumentType())
                .fileName(entity.getFileName())
                .contentType(entity.getContentType())
                .fileSize(entity.getFileSize())
                .bucketName(entity.getBucketName())
                .s3Key(entity.getS3Key())
                .status(entity.getStatus())
                .uploadedBy(entity.getUploadedBy())
                .createdAt(Instant.parse(entity.getCreatedAt()))
                .updatedAt(Instant.parse(entity.getUpdatedAt()))
                .build();
    }
}
