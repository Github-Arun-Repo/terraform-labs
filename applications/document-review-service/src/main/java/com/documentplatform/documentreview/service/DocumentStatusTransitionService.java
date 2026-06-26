package com.documentplatform.documentreview.service;

import com.documentplatform.documentreview.enums.DocumentStatus;
import com.documentplatform.documentreview.exception.ValidationException;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class DocumentStatusTransitionService {

    private static final Map<DocumentStatus, Set<DocumentStatus>> ALLOWED = Map.of(
            DocumentStatus.PENDING_APPROVAL, Set.of(DocumentStatus.APPROVED, DocumentStatus.REJECTED, DocumentStatus.MANUAL_REVIEW_REQUIRED),
            DocumentStatus.MANUAL_REVIEW_REQUIRED, Set.of(DocumentStatus.APPROVED, DocumentStatus.REJECTED),
            DocumentStatus.DUPLICATE_DETECTED, Set.of(DocumentStatus.APPROVED, DocumentStatus.REJECTED),
            DocumentStatus.EXTRACTION_COMPLETED, Set.of(DocumentStatus.PENDING_APPROVAL, DocumentStatus.MANUAL_REVIEW_REQUIRED, DocumentStatus.DUPLICATE_DETECTED)
    );

    public void validateTransition(DocumentStatus from, DocumentStatus to) {
        if (from == null || to == null) {
            throw new ValidationException("Status transition requires source and target status");
        }
        if (!ALLOWED.getOrDefault(from, Set.of()).contains(to)) {
            throw new ValidationException("Invalid status transition from " + from + " to " + to);
        }
    }
}
