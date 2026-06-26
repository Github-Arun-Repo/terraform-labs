package com.documentplatform.documentreview.controller;

import com.documentplatform.documentreview.dto.AuditHistoryResponse;
import com.documentplatform.documentreview.dto.ReviewDecisionResponse;
import com.documentplatform.documentreview.service.AuditQueryService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit")
@Validated
public class AuditController {

    private final AuditQueryService auditQueryService;

    public AuditController(AuditQueryService auditQueryService) {
        this.auditQueryService = auditQueryService;
    }

    @GetMapping("/{documentId}")
    public AuditHistoryResponse history(
            @PathVariable String documentId,
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int limit
    ) {
        return auditQueryService.auditHistory(documentId, limit);
    }

    @GetMapping("/{documentId}/decision")
    public ReviewDecisionResponse decision(@PathVariable String documentId) {
        return auditQueryService.latestDecision(documentId);
    }
}
