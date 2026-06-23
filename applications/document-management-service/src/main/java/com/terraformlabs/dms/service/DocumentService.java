package com.terraformlabs.dms.service;

import com.terraformlabs.dms.config.AppProperties;
import com.terraformlabs.dms.dto.DocumentUploadResponse;
import com.terraformlabs.dms.entity.AppUser;
import com.terraformlabs.dms.entity.Document;
import com.terraformlabs.dms.entity.DocumentType;
import com.terraformlabs.dms.exception.BadRequestException;
import com.terraformlabs.dms.exception.ResourceNotFoundException;
import com.terraformlabs.dms.repository.DocumentRepository;
import java.io.IOException;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final UserService userService;
    private final long maxSizeBytes;
    private final Set<DocumentType> allowedTypes;

    public DocumentService(DocumentRepository documentRepository,
                           UserService userService,
                           AppProperties appProperties) {
        this.documentRepository = documentRepository;
        this.userService = userService;
        this.maxSizeBytes = appProperties.getDocuments().getMaxSizeBytes();
        this.allowedTypes = appProperties.getDocuments().getAllowedTypes().stream()
                .map(t -> DocumentType.valueOf(t.toUpperCase(Locale.ROOT)))
                .collect(Collectors.toSet());
    }

    public DocumentUploadResponse upload(String username, MultipartFile file, String docTypeRaw) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File must be provided");
        }
        if (file.getSize() > maxSizeBytes) {
            throw new BadRequestException("File exceeds max allowed size");
        }

        DocumentType documentType;
        try {
            documentType = DocumentType.valueOf(docTypeRaw.toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new BadRequestException("Unsupported document type");
        }

        if (!allowedTypes.contains(documentType)) {
            throw new BadRequestException("Document type is not allowed");
        }

        AppUser user = userService.getByUsername(username);
        Document document = new Document();
        document.setUser(user);
        document.setDocType(documentType);
        document.setOriginalFilename(file.getOriginalFilename() == null ? "unnamed" : file.getOriginalFilename());
        document.setContentType(file.getContentType() == null ? "application/octet-stream" : file.getContentType());
        document.setFileSize(file.getSize());
        try {
            document.setContent(file.getBytes());
        } catch (IOException ex) {
            throw new BadRequestException("Unable to read uploaded file");
        }

        Document saved = documentRepository.save(document);
        return new DocumentUploadResponse(
                saved.getId(),
                saved.getOriginalFilename(),
                saved.getDocType(),
                saved.getFileSize(),
                saved.getCreatedAt());
    }

    public ResponseEntity<ByteArrayResource> download(String username, Long documentId) {
        AppUser user = userService.getByUsername(username);
        Document document = documentRepository.findByIdAndUserId(documentId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));

        ByteArrayResource resource = new ByteArrayResource(
                Objects.requireNonNull(document.getContent(), "document content must not be null"));
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(document.getOriginalFilename())
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        Objects.requireNonNull(document.getContentType(), "document contentType must not be null")))
                .contentLength(document.getFileSize())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(resource);
    }
}
