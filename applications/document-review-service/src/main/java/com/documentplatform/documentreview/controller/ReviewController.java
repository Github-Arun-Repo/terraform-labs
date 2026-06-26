package com.documentplatform.documentreview.controller;

import com.documentplatform.documentreview.dto.ApproveDocumentRequest;
import com.documentplatform.documentreview.dto.ApproveDocumentResponse;
import com.documentplatform.documentreview.dto.FieldCorrectionRequest;
import com.documentplatform.documentreview.dto.FieldCorrectionResponse;
import com.documentplatform.documentreview.dto.RejectDocumentRequest;
import com.documentplatform.documentreview.dto.RejectDocumentResponse;
import com.documentplatform.documentreview.dto.ReviewDetailsResponse;
import com.documentplatform.documentreview.dto.ReviewQueueResponse;
import com.documentplatform.documentreview.service.CorrectionService;
import com.documentplatform.documentreview.service.DecisionService;
import com.documentplatform.documentreview.service.ReviewDetailsService;
import com.documentplatform.documentreview.service.ReviewQueueService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/review")
@Validated
public class ReviewController {

    private final ReviewQueueService reviewQueueService;
    private final ReviewDetailsService reviewDetailsService;
    private final CorrectionService correctionService;
    private final DecisionService decisionService;

    public ReviewController(
            ReviewQueueService reviewQueueService,
            ReviewDetailsService reviewDetailsService,
            CorrectionService correctionService,
            DecisionService decisionService
    ) {
        this.reviewQueueService = reviewQueueService;
        this.reviewDetailsService = reviewDetailsService;
        this.correctionService = correctionService;
        this.decisionService = decisionService;
    }

    @GetMapping("/queue")
    public ReviewQueueResponse queue(
            @RequestParam String customerId,
            @RequestParam(defaultValue = "PENDING_APPROVAL") String status,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
            @RequestParam(required = false) String nextToken
    ) {
        return reviewQueueService.queue(customerId, status, limit, nextToken);
    }

    @GetMapping("/{documentId}")
    public ReviewDetailsResponse details(@PathVariable String documentId) {
        return reviewDetailsService.details(documentId);
    }

    @PatchMapping("/{documentId}/fields")
    public FieldCorrectionResponse applyCorrections(
            @PathVariable String documentId,
            @Valid @RequestBody FieldCorrectionRequest request
    ) {
        return correctionService.applyCorrections(documentId, request);
    }

    @PostMapping("/{documentId}/approve")
    public ApproveDocumentResponse approve(
            @PathVariable String documentId,
            @Valid @RequestBody ApproveDocumentRequest request
    ) {
        return decisionService.approve(documentId, request);
    }

    @PostMapping("/{documentId}/reject")
    public RejectDocumentResponse reject(
            @PathVariable String documentId,
            @Valid @RequestBody RejectDocumentRequest request
    ) {
        return decisionService.reject(documentId, request);
    }
}
