package com.documentplatform.documentreview.service;

import com.documentplatform.documentreview.model.AuditEventItem;
import com.documentplatform.documentreview.repository.DynamoDbAuditRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final DynamoDbAuditRepository auditRepository;

    public void recordStatusTransition(String documentId, String oldStatus, String newStatus, String performedBy, String message) {
        AuditEventItem item = new AuditEventItem();
        item.setPk("DOCUMENT#" + documentId);
        item.setSk("AUDIT#" + Instant.now().toEpochMilli() + "#" + java.util.UUID.randomUUID());
        item.setEntityType("AUDIT_EVENT");
        item.setDocumentId(documentId);
        item.setEventType("STATUS_TRANSITION");
        item.setOldStatus(oldStatus);
        item.setNewStatus(newStatus);
        item.setPerformedBy(performedBy);
        item.setMessage(message);
        item.setCreatedAt(Instant.now().toString());
        auditRepository.save(item);
    }

    public void recordRead(String documentId, String performedBy, String message) {
        AuditEventItem item = new AuditEventItem();
        item.setPk("DOCUMENT#" + documentId);
        item.setSk("AUDIT#" + Instant.now().toEpochMilli() + "#" + java.util.UUID.randomUUID());
        item.setEntityType("AUDIT_EVENT");
        item.setDocumentId(documentId);
        item.setEventType("READ");
        item.setPerformedBy(performedBy);
        item.setMessage(message);
        item.setCreatedAt(Instant.now().toString());
        auditRepository.save(item);
    }
}
