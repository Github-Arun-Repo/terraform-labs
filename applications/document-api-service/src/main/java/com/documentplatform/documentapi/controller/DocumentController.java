package com.documentplatform.documentapi.controller;

import com.documentplatform.documentapi.dto.CreateUploadRequest;
import com.documentplatform.documentapi.dto.CreateUploadResponse;
import com.documentplatform.documentapi.dto.DocumentResponse;
import com.documentplatform.documentapi.dto.PagedDocumentResponse;
import com.documentplatform.documentapi.dto.ViewUrlResponse;
import com.documentplatform.documentapi.enums.DocumentStatus;
import com.documentplatform.documentapi.enums.DocumentType;
import com.documentplatform.documentapi.security.AuthenticatedUser;
import com.documentplatform.documentapi.service.DocumentService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping("/upload-request")
    @PreAuthorize("hasAnyRole('SUPPLIER','ADMIN')")
    public CreateUploadResponse createUploadRequest(@Valid @RequestBody CreateUploadRequest request, Authentication authentication) {
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        log.info("upload request received customerId={} documentType={} uploadedBy={}",
                request.getCustomerId(), request.getDocumentType(), user.userId());
        return documentService.createUploadRequest(request, user);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE_REVIEWER','FINANCE_APPROVER','SUPPLIER','AUDITOR')")
    public PagedDocumentResponse listDocuments(
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) DocumentStatus status,
            @RequestParam(required = false) DocumentType documentType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String nextToken
    ) {
        return documentService.listDocuments(customerId, status, documentType, page, size, nextToken);
    }

    @GetMapping("/{documentId}")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE_REVIEWER','FINANCE_APPROVER','SUPPLIER','AUDITOR')")
    public DocumentResponse getDocument(@PathVariable String documentId) {
        return documentService.getDocument(documentId);
    }

    @GetMapping("/{documentId}/view-url")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE_REVIEWER','FINANCE_APPROVER','SUPPLIER','AUDITOR')")
    public ViewUrlResponse getViewUrl(@PathVariable String documentId) {
        return documentService.generateViewUrl(documentId);
    }
}
