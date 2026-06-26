package com.documentplatform.documentreview.service;

import com.documentplatform.documentreview.config.AuditProperties;
import com.documentplatform.documentreview.config.AwsProperties;
import com.documentplatform.documentreview.dto.ReviewDetailsResponse;
import com.documentplatform.documentreview.dto.ViewUrlResponse;
import com.documentplatform.documentreview.exception.ResourceNotFoundException;
import com.documentplatform.documentreview.model.DocumentItem;
import com.documentplatform.documentreview.model.ExtractionItem;
import com.documentplatform.documentreview.repository.DynamoDbDocumentRepository;
import com.documentplatform.documentreview.repository.DynamoDbExtractionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReviewDetailsService {

    private final DynamoDbDocumentRepository documentRepository;
    private final DynamoDbExtractionRepository extractionRepository;
    private final S3ViewUrlService viewUrlService;
    private final ReviewMapper mapper;
    private final CurrentUserService currentUserService;
    private final AuditService auditService;
    private final AuditProperties auditProperties;
    private final AwsProperties awsProperties;

    public ReviewDetailsResponse details(String documentId) {
        DocumentItem document = documentRepository.findDocumentById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentId));
        ExtractionItem extraction = extractionRepository.findByDocumentId(documentId).orElse(null);

        if (auditProperties.isReadEventsEnabled()) {
            auditService.recordRead(documentId, currentUserService.username(), "Document review details requested");
        }

        String viewUrl = viewUrlService.generateViewUrl(document.getBucketName(), document.getS3Key());

        return ReviewDetailsResponse.builder()
                .document(mapper.toDocumentSummary(document))
                .extraction(mapper.toExtraction(extraction))
                .viewUrl(ViewUrlResponse.builder()
                        .url(viewUrl)
                        .expiresInSeconds(awsProperties.getS3().getViewUrlExpiryMinutes() * 60)
                        .build())
                .build();
    }
}
