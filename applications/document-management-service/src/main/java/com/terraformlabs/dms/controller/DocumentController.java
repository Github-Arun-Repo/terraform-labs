package com.terraformlabs.dms.controller;

import com.terraformlabs.dms.dto.DocumentUploadResponse;
import com.terraformlabs.dms.service.DocumentService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping
    public DocumentUploadResponse uploadDocument(Authentication authentication,
                                                 @RequestParam("file") MultipartFile file,
                                                 @RequestParam("docType") String docType) {
        return documentService.upload(authentication.getName(), file, docType);
    }

    @GetMapping("/{documentId}")
    public ResponseEntity<ByteArrayResource> downloadDocument(Authentication authentication,
                                                              @PathVariable Long documentId) {
        return documentService.download(authentication.getName(), documentId);
    }
}
