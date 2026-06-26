package com.documentplatform.documentreview.service;

import com.documentplatform.documentreview.dto.FieldCorrectionRequest;
import com.documentplatform.documentreview.dto.FieldCorrectionResponse;
import com.documentplatform.documentreview.enums.DocumentStatus;
import com.documentplatform.documentreview.exception.ResourceNotFoundException;
import com.documentplatform.documentreview.exception.RevisionConflictException;
import com.documentplatform.documentreview.model.DocumentItem;
import com.documentplatform.documentreview.model.ExtractionItem;
import com.documentplatform.documentreview.repository.DynamoDbDocumentRepository;
import com.documentplatform.documentreview.repository.DynamoDbExtractionRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CorrectionService {

    private final DynamoDbDocumentRepository documentRepository;
    private final DynamoDbExtractionRepository extractionRepository;
    private final CurrentUserService currentUserService;
    private final DocumentStatusTransitionService statusTransitionService;
    private final AuditService auditService;
    private final MeterRegistry meterRegistry;

    public FieldCorrectionResponse applyCorrections(String documentId, FieldCorrectionRequest request) {
        DocumentItem document = documentRepository.findDocumentById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentId));

        Long currentRevision = document.getDocumentRevision();
        if (currentRevision == null) {
            currentRevision = 0L;
        }
        if (!currentRevision.equals(request.getExpectedDocumentRevision())) {
            throw new RevisionConflictException(currentRevision, request.getExpectedDocumentRevision());
        }

        statusTransitionService.validateTransition(document.getStatus(), DocumentStatus.MANUAL_REVIEW_REQUIRED);

        ExtractionItem extraction = extractionRepository.findByDocumentId(documentId)
                .orElseGet(() -> {
                    ExtractionItem item = new ExtractionItem();
                    item.setPk("DOCUMENT#" + documentId);
                    item.setSk("EXTRACTION#LATEST");
                    item.setEntityType("EXTRACTION");
                    item.setDocumentId(documentId);
                    item.setCreatedAt(Instant.now().toString());
                    return item;
                });

        Map<String, Object> correctionEvent = new HashMap<>();
        correctionEvent.put("timestamp", Instant.now().toString());
        correctionEvent.put("user", currentUserService.username());
        correctionEvent.put("comment", request.getComment());
        correctionEvent.put("changes", request.getCorrections());

        java.util.List<Map<String, Object>> corrections = extraction.getManualCorrections() == null
                ? new java.util.ArrayList<>()
                : new java.util.ArrayList<>(extraction.getManualCorrections());
        corrections.add(correctionEvent);
        extraction.setManualCorrections(corrections);
        extraction.setUpdatedAt(Instant.now().toString());

        applyExtractionCorrections(extraction, request.getCorrections());
        extractionRepository.save(extraction);

        DocumentStatus oldStatus = document.getStatus();
        documentRepository.updateStatus(document, DocumentStatus.MANUAL_REVIEW_REQUIRED);
        auditService.recordStatusTransition(
                documentId,
                oldStatus.name(),
                DocumentStatus.MANUAL_REVIEW_REQUIRED.name(),
                currentUserService.username(),
                "Manual corrections applied"
        );

        meterRegistry.counter("document_review_corrections_total").increment();

        return FieldCorrectionResponse.builder()
                .documentId(documentId)
                .status(DocumentStatus.MANUAL_REVIEW_REQUIRED)
                .documentRevision(document.getDocumentRevision())
                .message("Corrections applied")
                .build();
    }

    private void applyExtractionCorrections(ExtractionItem extraction, Map<String, Object> corrections) {
        if (corrections == null) {
            return;
        }
        corrections.forEach((key, value) -> {
            switch (key) {
                case "invoiceNumber" -> extraction.setInvoiceNumber(stringValue(value));
                case "supplierName" -> extraction.setSupplierName(stringValue(value));
                case "supplierAddress" -> extraction.setSupplierAddress(stringValue(value));
                case "customerName" -> extraction.setCustomerName(stringValue(value));
                case "invoiceDate" -> extraction.setInvoiceDate(stringValue(value));
                case "dueDate" -> extraction.setDueDate(stringValue(value));
                case "currency" -> extraction.setCurrency(stringValue(value));
                case "subtotalAmount" -> extraction.setSubtotalAmount(numberValue(value));
                case "taxAmount" -> extraction.setTaxAmount(numberValue(value));
                case "totalAmount" -> extraction.setTotalAmount(numberValue(value));
                case "iban" -> extraction.setIban(stringValue(value));
                default -> {
                    // Unknown fields are preserved in manual correction history only.
                }
            }
        });
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Double numberValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        return Double.parseDouble(String.valueOf(value));
    }
}
