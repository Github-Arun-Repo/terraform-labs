package com.documentplatform.documentreview.service;

import com.documentplatform.documentreview.dto.ApproveDocumentRequest;
import com.documentplatform.documentreview.dto.ApproveDocumentResponse;
import com.documentplatform.documentreview.dto.RejectDocumentRequest;
import com.documentplatform.documentreview.dto.RejectDocumentResponse;
import com.documentplatform.documentreview.enums.DocumentStatus;
import com.documentplatform.documentreview.enums.ReviewDecisionType;
import com.documentplatform.documentreview.exception.ResourceNotFoundException;
import com.documentplatform.documentreview.exception.RevisionConflictException;
import com.documentplatform.documentreview.model.DocumentItem;
import com.documentplatform.documentreview.model.ExtractionItem;
import com.documentplatform.documentreview.model.ReviewDecisionItem;
import com.documentplatform.documentreview.repository.DynamoDbDocumentRepository;
import com.documentplatform.documentreview.repository.DynamoDbExtractionRepository;
import com.documentplatform.documentreview.repository.DynamoDbReviewDecisionRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DecisionService {

    private final DynamoDbDocumentRepository documentRepository;
    private final DynamoDbExtractionRepository extractionRepository;
    private final DynamoDbReviewDecisionRepository decisionRepository;
    private final DocumentStatusTransitionService transitionService;
    private final InvoiceValidationService invoiceValidationService;
    private final CurrentUserService currentUserService;
    private final AuditService auditService;
    private final MeterRegistry meterRegistry;

    public ApproveDocumentResponse approve(String documentId, ApproveDocumentRequest request) {
        DocumentItem document = documentRepository.findDocumentById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentId));
        enforceRevision(document, request.getExpectedDocumentRevision());

        if (document.getStatus() == DocumentStatus.DUPLICATE_DETECTED && !request.isOverrideDuplicate()) {
            throw new com.documentplatform.documentreview.exception.ForbiddenOperationException(
                    "Duplicate detected documents require overrideDuplicate=true to approve"
            );
        }

        transitionService.validateTransition(document.getStatus(), DocumentStatus.APPROVED);

        ExtractionItem extraction = extractionRepository.findByDocumentId(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Extraction not found for document: " + documentId));
        invoiceValidationService.validateForApproval(extraction);

        DocumentStatus oldStatus = document.getStatus();
        documentRepository.updateStatus(document, DocumentStatus.APPROVED);

        String actor = currentUserService.username();
        saveDecision(documentId, ReviewDecisionType.APPROVED.name(), actor, null, request.getComment());
        auditService.recordStatusTransition(documentId, oldStatus.name(), DocumentStatus.APPROVED.name(), actor, "Document approved");
        meterRegistry.counter("documents.review.approved").increment();
        meterRegistry.counter("document_review_approvals_total").increment();

        return ApproveDocumentResponse.builder()
                .documentId(documentId)
                .oldStatus(oldStatus)
                .newStatus(DocumentStatus.APPROVED)
                .approvedBy(actor)
                .approvedAt(Instant.now().toString())
                .message("Document approved")
                .build();
    }

    public RejectDocumentResponse reject(String documentId, RejectDocumentRequest request) {
        DocumentItem document = documentRepository.findDocumentById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentId));
        enforceRevision(document, request.getExpectedDocumentRevision());

        transitionService.validateTransition(document.getStatus(), DocumentStatus.REJECTED);

        DocumentStatus oldStatus = document.getStatus();
        documentRepository.updateStatus(document, DocumentStatus.REJECTED);

        String actor = currentUserService.username();
        saveDecision(documentId, ReviewDecisionType.REJECTED.name(), actor, request.getReasonCode().name(), request.getComment());
        auditService.recordStatusTransition(documentId, oldStatus.name(), DocumentStatus.REJECTED.name(), actor, "Document rejected");
        meterRegistry.counter("documents.review.rejected").increment();
        meterRegistry.counter("document_review_rejections_total", "reason", request.getReasonCode().name()).increment();

        return RejectDocumentResponse.builder()
                .documentId(documentId)
                .oldStatus(oldStatus)
                .newStatus(DocumentStatus.REJECTED)
                .rejectedBy(actor)
                .rejectedAt(Instant.now().toString())
                .reasonCode(request.getReasonCode())
                .message("Document rejected")
                .build();
    }

    private void enforceRevision(DocumentItem document, Long expectedRevision) {
        Long currentRevision = document.getDocumentRevision() == null ? 0L : document.getDocumentRevision();
        if (!currentRevision.equals(expectedRevision)) {
            throw new RevisionConflictException(currentRevision, expectedRevision);
        }
    }

    private void saveDecision(String documentId, String decision, String actor, String reasonCode, String comment) {
        ReviewDecisionItem item = new ReviewDecisionItem();
        item.setPk("DOCUMENT#" + documentId);
        item.setSk("DECISION#LATEST");
        item.setEntityType("REVIEW_DECISION");
        item.setDocumentId(documentId);
        item.setDecision(decision);
        item.setDecidedBy(actor);
        item.setReasonCode(reasonCode);
        item.setComment(comment);
        item.setCreatedAt(Instant.now().toString());
        decisionRepository.save(item);
    }
}
