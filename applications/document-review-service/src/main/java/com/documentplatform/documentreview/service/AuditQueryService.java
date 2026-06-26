package com.documentplatform.documentreview.service;

import com.documentplatform.documentreview.dto.AuditHistoryResponse;
import com.documentplatform.documentreview.dto.ReviewDecisionResponse;
import com.documentplatform.documentreview.repository.DynamoDbAuditRepository;
import com.documentplatform.documentreview.repository.DynamoDbReviewDecisionRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditQueryService {

    private final DynamoDbAuditRepository auditRepository;
    private final DynamoDbReviewDecisionRepository reviewDecisionRepository;
    private final ReviewMapper mapper;

    public AuditHistoryResponse auditHistory(String documentId, int limit) {
        List<com.documentplatform.documentreview.dto.AuditEventResponse> events =
                auditRepository.findByDocumentId(documentId, limit).stream().map(mapper::toAuditEvent).toList();

        return AuditHistoryResponse.builder()
                .documentId(documentId)
                .events(events)
                .build();
    }

    public ReviewDecisionResponse latestDecision(String documentId) {
        return mapper.toReviewDecision(reviewDecisionRepository.findLatestByDocumentId(documentId).orElse(null));
    }
}
