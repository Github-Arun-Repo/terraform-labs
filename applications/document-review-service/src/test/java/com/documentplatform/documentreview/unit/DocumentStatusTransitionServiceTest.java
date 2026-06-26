package com.documentplatform.documentreview.unit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.documentplatform.documentreview.enums.DocumentStatus;
import com.documentplatform.documentreview.exception.ValidationException;
import com.documentplatform.documentreview.service.DocumentStatusTransitionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DocumentStatusTransitionServiceTest {

    private DocumentStatusTransitionService service;

    @BeforeEach
    void setUp() {
        service = new DocumentStatusTransitionService();
    }

    @Test
    void shouldAllowPendingApprovalToApproved() {
        assertDoesNotThrow(() -> service.validateTransition(DocumentStatus.PENDING_APPROVAL, DocumentStatus.APPROVED));
    }

    @Test
    void shouldRejectInvalidTransition() {
        assertThrows(ValidationException.class,
                () -> service.validateTransition(DocumentStatus.UPLOADED, DocumentStatus.APPROVED));
    }
}
