package com.documentplatform.documentreview.service;

import com.documentplatform.documentreview.dto.AuditEventResponse;
import com.documentplatform.documentreview.dto.DocumentSummaryResponse;
import com.documentplatform.documentreview.dto.ExtractedInvoiceResponse;
import com.documentplatform.documentreview.dto.InvoiceLineItemResponse;
import com.documentplatform.documentreview.dto.ReviewDecisionResponse;
import com.documentplatform.documentreview.dto.ReviewQueueItemResponse;
import com.documentplatform.documentreview.model.AuditEventItem;
import com.documentplatform.documentreview.model.DocumentItem;
import com.documentplatform.documentreview.model.ExtractionItem;
import com.documentplatform.documentreview.model.ReviewDecisionItem;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ReviewMapper {

    public ReviewQueueItemResponse toQueueItem(DocumentItem doc, ExtractionItem extraction) {
        return ReviewQueueItemResponse.builder()
                .documentId(doc.getDocumentId())
                .customerId(doc.getCustomerId())
                .documentType(doc.getDocumentType())
                .fileName(doc.getFileName())
                .supplierName(extraction == null ? null : extraction.getSupplierName())
                .invoiceNumber(extraction == null ? null : extraction.getInvoiceNumber())
                .invoiceDate(extraction == null ? null : extraction.getInvoiceDate())
                .totalAmount(extraction == null ? null : extraction.getTotalAmount())
                .currency(extraction == null ? null : extraction.getCurrency())
                .confidenceScore(extraction == null ? null : extraction.getConfidenceScore())
                .status(doc.getStatus())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }

    public DocumentSummaryResponse toDocumentSummary(DocumentItem doc) {
        return DocumentSummaryResponse.builder()
                .documentId(doc.getDocumentId())
                .customerId(doc.getCustomerId())
                .documentType(doc.getDocumentType())
                .fileName(doc.getFileName())
                .contentType(doc.getContentType())
                .fileSize(doc.getFileSize())
                .bucketName(doc.getBucketName())
                .s3Key(doc.getS3Key())
                .status(doc.getStatus())
                .documentRevision(doc.getDocumentRevision())
                .uploadedBy(doc.getUploadedBy())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }

    public ExtractedInvoiceResponse toExtraction(ExtractionItem extraction) {
        if (extraction == null) {
            return null;
        }
        List<InvoiceLineItemResponse> lineItems = extraction.getLineItems() == null ? List.of() : extraction.getLineItems().stream()
                .map(this::toLineItem)
                .toList();

        return ExtractedInvoiceResponse.builder()
                .invoiceNumber(extraction.getInvoiceNumber())
                .supplierName(extraction.getSupplierName())
                .supplierAddress(extraction.getSupplierAddress())
                .customerName(extraction.getCustomerName())
                .invoiceDate(extraction.getInvoiceDate())
                .dueDate(extraction.getDueDate())
                .currency(extraction.getCurrency())
                .subtotalAmount(extraction.getSubtotalAmount())
                .taxAmount(extraction.getTaxAmount())
                .totalAmount(extraction.getTotalAmount())
                .iban(extraction.getIban())
                .confidenceScore(extraction.getConfidenceScore())
                .lineItems(lineItems)
                .validationErrors(extraction.getValidationErrors() == null ? List.of() : extraction.getValidationErrors())
                .manualCorrections(extraction.getManualCorrections() == null ? List.of() : extraction.getManualCorrections())
                .build();
    }

    private InvoiceLineItemResponse toLineItem(Map<String, Object> item) {
        return InvoiceLineItemResponse.builder()
                .description(readString(item, "description"))
                .quantity(readDouble(item, "quantity"))
                .unitPrice(readDouble(item, "unitPrice"))
                .amount(readDouble(item, "amount"))
                .build();
    }

    private String readString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private Double readDouble(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return Double.parseDouble(String.valueOf(value));
    }

    public AuditEventResponse toAuditEvent(AuditEventItem item) {
        return AuditEventResponse.builder()
                .eventType(item.getEventType())
                .oldStatus(item.getOldStatus())
                .newStatus(item.getNewStatus())
                .performedBy(item.getPerformedBy())
                .message(item.getMessage())
                .createdAt(item.getCreatedAt())
                .build();
    }

    public ReviewDecisionResponse toReviewDecision(ReviewDecisionItem item) {
        if (item == null) {
            return null;
        }
        return ReviewDecisionResponse.builder()
                .documentId(item.getDocumentId())
                .decision(item.getDecision())
                .decidedBy(item.getDecidedBy())
                .reasonCode(item.getReasonCode())
                .comment(item.getComment())
                .createdAt(item.getCreatedAt())
                .build();
    }
}
