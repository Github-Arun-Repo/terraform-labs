package com.documentplatform.documentprocessing.service;

import com.documentplatform.documentprocessing.config.AppProperties;
import com.documentplatform.documentprocessing.enums.DocumentStatus;
import com.documentplatform.documentprocessing.enums.ExtractorMode;
import com.documentplatform.documentprocessing.model.AuditEventItem;
import com.documentplatform.documentprocessing.model.DocumentItem;
import com.documentplatform.documentprocessing.model.ExtractionItem;
import com.documentplatform.documentprocessing.repository.DynamoAuditRepository;
import com.documentplatform.documentprocessing.repository.DynamoDocumentRepository;
import com.documentplatform.documentprocessing.repository.DynamoExtractionRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentProcessingService {

    private static final Set<DocumentStatus> FINAL_STATUSES = Set.of(
            DocumentStatus.PENDING_APPROVAL,
            DocumentStatus.MANUAL_REVIEW_REQUIRED,
            DocumentStatus.DUPLICATE_DETECTED,
            DocumentStatus.EXTRACTION_FAILED,
            DocumentStatus.FAILED,
            DocumentStatus.APPROVED,
            DocumentStatus.REJECTED
    );

    private final DynamoDocumentRepository documentRepository;
    private final DynamoExtractionRepository extractionRepository;
    private final DynamoAuditRepository auditRepository;
    private final MockInvoiceExtractor mockInvoiceExtractor;
    private final TextractInvoiceExtractor textractInvoiceExtractor;
    private final S3ObjectValidator s3ObjectValidator;
    private final S3Client s3Client;
    private final AppProperties appProperties;
    private final MeterRegistry meterRegistry;

    public boolean process(String bucket, String key) {
        DocumentItem document = documentRepository.findByS3Key(key)
                .orElseThrow(() -> new IllegalArgumentException("Document not found for s3 key: " + key));

        Instant now = Instant.now();
        DynamoDocumentRepository.ProcessingStartOutcome startOutcome = documentRepository.tryStartProcessing(
                document,
                now,
                appProperties.getMaxProcessingAttempts(),
                appProperties.getProcessingStaleTimeoutMinutes()
        );

        if (startOutcome == DynamoDocumentRepository.ProcessingStartOutcome.SKIPPED_FINAL) {
            log.info("Skipping duplicate event for final-status document documentId={} status={}", document.getDocumentId(), document.getStatus());
            return true;
        }

        if (startOutcome == DynamoDocumentRepository.ProcessingStartOutcome.SKIPPED_RECENT_PROCESSING
                || startOutcome == DynamoDocumentRepository.ProcessingStartOutcome.TRANSITION_CONFLICT) {
            log.info("Skipping concurrent/recent processing for documentId={} status={}", document.getDocumentId(), document.getStatus());
            return true;
        }

        if (startOutcome == DynamoDocumentRepository.ProcessingStartOutcome.MAX_ATTEMPTS_EXCEEDED) {
            if (document.getStatus() != DocumentStatus.FAILED && !FINAL_STATUSES.contains(document.getStatus())) {
                updateStatus(document, DocumentStatus.FAILED, "Max attempts exceeded", "DOCUMENT_PROCESSING_MAX_ATTEMPTS_EXCEEDED");
            }
            return true;
        }

        documentRepository.findByDocumentId(document.getDocumentId()).ifPresent(latest -> {
            document.setStatus(latest.getStatus());
            document.setProcessingAttempts(latest.getProcessingAttempts());
            document.setDocumentRevision(latest.getDocumentRevision());
            document.setUpdatedAt(latest.getUpdatedAt());
            document.setProcessingStartedAt(latest.getProcessingStartedAt());
            document.setLastProcessedAt(latest.getLastProcessedAt());
            document.setGsi2Pk(latest.getGsi2Pk());
            document.setGsi2Sk(latest.getGsi2Sk());
        });

        if (document.getStatus() != DocumentStatus.PROCESSING) {
            log.warn("Document was not moved to PROCESSING after transition attempt, skipping documentId={} status={}",
                    document.getDocumentId(), document.getStatus());
            return true;
        }

        meterRegistry.counter("documents.processing.started").increment();

        writeAudit(document, "DOCUMENT_PROCESSING_STARTED", DocumentStatus.PROCESSING, "Processing started");

        try {
            s3ObjectValidator.validate(document);
            InvoiceExtractor extractor = appProperties.getExtractorMode() == ExtractorMode.AWS_TEXTRACT
                    ? textractInvoiceExtractor
                    : mockInvoiceExtractor;

            ExtractionResult result = extractor.extract(document);

            String processedBase = document.getDocumentType().toLowerCase() + "/processed/" + document.getCustomerId() + "/" + document.getDocumentId();
            String rawKey = processedBase + "/textract-raw-output.json";
            String normalizedKey = processedBase + "/normalized-output.json";

            putJson(bucket, rawKey, result.rawJson());
            putJson(bucket, normalizedKey, result.normalizedJson());

            ExtractionItem extractionItem = new ExtractionItem();
            extractionItem.setPk(document.getPk());
            extractionItem.setSk("EXTRACTION#LATEST");
            extractionItem.setEntityType("EXTRACTION");
            extractionItem.setDocumentId(document.getDocumentId());
            extractionItem.setInvoiceNumber(result.invoiceNumber());
            extractionItem.setSupplierName(result.supplierName());
            extractionItem.setInvoiceDate(result.invoiceDate());
            extractionItem.setCurrency(result.currency());
            extractionItem.setTotalAmount(result.totalAmount());
            extractionItem.setConfidenceScore(result.confidenceScore());
            extractionItem.setLineItems(result.lineItems());
            extractionItem.setValidationErrors(result.validationErrors());
            extractionItem.setRawTextractS3Key(rawKey);
            extractionItem.setNormalizedJsonS3Key(normalizedKey);
            extractionItem.setCreatedAt(Instant.now().toString());
            extractionItem.setUpdatedAt(Instant.now().toString());
            extractionRepository.save(extractionItem);

            String supplierNameNormalized = normalizeToken(result.supplierName());
            String invoiceNumberNormalized = normalizeToken(result.invoiceNumber());
            if (!supplierNameNormalized.isBlank() && !invoiceNumberNormalized.isBlank()) {
                String duplicateKey = "DUPLICATE#" + document.getCustomerId() + "#" + supplierNameNormalized + "#" + invoiceNumberNormalized;
                boolean created = documentRepository.createDuplicateIndexItem(
                        duplicateKey,
                        document.getDocumentId(),
                        document.getCustomerId(),
                        supplierNameNormalized,
                        invoiceNumberNormalized,
                        Instant.now()
                );

                if (!created) {
                    updateStatus(document, DocumentStatus.DUPLICATE_DETECTED, "Duplicate invoice detected", "DOCUMENT_DUPLICATE_DETECTED");
                    meterRegistry.counter("document_processing_duplicate_detected_total").increment();
                    return true;
                }
            }

            DocumentStatus finalStatus = (result.validationErrors() == null || result.validationErrors().isEmpty())
                    ? DocumentStatus.PENDING_APPROVAL
                    : DocumentStatus.MANUAL_REVIEW_REQUIRED;
            updateStatus(document, finalStatus, "Processing completed", "STATUS_TRANSITION");
            meterRegistry.counter("document_processing_success_total").increment();
            return true;
        } catch (DocumentValidationException ex) {
            String failedPath = document.getDocumentType().toLowerCase() + "/failed/" + document.getCustomerId() + "/" + document.getDocumentId() + "/error.json";
            putJson(bucket, failedPath, "{\"error\":\"" + ex.getMessage().replace('"', '\'') + "\"}");
            updateStatus(document, DocumentStatus.FAILED, "Validation failed: " + ex.getMessage(), "DOCUMENT_VALIDATION_FAILED");
            meterRegistry.counter("documents.processing.failed").increment();
            meterRegistry.counter("document_processing_failed_total", "reason", "validation").increment();
            return true;
        } catch (Exception ex) {
            String failedPath = document.getDocumentType().toLowerCase() + "/failed/" + document.getCustomerId() + "/" + document.getDocumentId() + "/error.json";
            putJson(bucket, failedPath, "{\"error\":\"" + ex.getMessage().replace('"', '\'') + "\"}");
            updateStatus(document, DocumentStatus.EXTRACTION_FAILED, "Processing failed: " + ex.getMessage(), "STATUS_TRANSITION");
            meterRegistry.counter("documents.processing.failed").increment();
            meterRegistry.counter("document_processing_failed_total").increment();
            return false;
        }
    }

    private void putJson(String bucket, String key, String payload) {
        s3Client.putObject(
                PutObjectRequest.builder().bucket(bucket).key(key).contentType("application/json").build(),
                RequestBody.fromBytes(payload.getBytes(StandardCharsets.UTF_8))
        );
    }

    private void updateStatus(DocumentItem document, DocumentStatus newStatus, String message, String eventType) {
        DocumentStatus oldStatus = document.getStatus();
        document.setStatus(newStatus);
        document.setDocumentRevision(document.getDocumentRevision() == null ? 1L : document.getDocumentRevision() + 1);
        document.setUpdatedAt(Instant.now().toString());
        document.setLastProcessedAt(document.getUpdatedAt());
        document.setGsi2Pk("CUSTOMER#" + document.getCustomerId() + "#STATUS#" + newStatus.name());
        document.setGsi2Sk("UPDATED_AT#" + document.getUpdatedAt() + "#DOCUMENT#" + document.getDocumentId());
        documentRepository.save(document);

        writeAudit(document, eventType, oldStatus, newStatus, message);
    }

    private void writeAudit(DocumentItem document, String eventType, DocumentStatus newStatus, String message) {
        writeAudit(document, eventType, document.getStatus(), newStatus, message);
    }

    private void writeAudit(DocumentItem document, String eventType, DocumentStatus oldStatus, DocumentStatus newStatus, String message) {
        AuditEventItem audit = new AuditEventItem();
        audit.setPk(document.getPk());
        audit.setSk("AUDIT#" + Instant.now().toEpochMilli() + "#" + UUID.randomUUID());
        audit.setEntityType("AUDIT");
        audit.setDocumentId(document.getDocumentId());
        audit.setEventType(eventType);
        audit.setOldStatus(oldStatus == null ? null : oldStatus.name());
        audit.setNewStatus(newStatus == null ? null : newStatus.name());
        audit.setPerformedBy("document-processing-service");
        audit.setMessage(message);
        audit.setCreatedAt(Instant.now().toString());
        auditRepository.save(audit);
    }

    private String normalizeToken(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
    }
}
