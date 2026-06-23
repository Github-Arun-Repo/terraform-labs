package com.terraformlabs.dms.service;

import com.terraformlabs.dms.config.AppProperties;
import com.terraformlabs.dms.dto.DocumentUploadResponse;
import com.terraformlabs.dms.entity.AppUser;
import com.terraformlabs.dms.entity.Document;
import com.terraformlabs.dms.entity.DocumentType;
import com.terraformlabs.dms.exception.BadRequestException;
import com.terraformlabs.dms.exception.ResourceNotFoundException;
import com.terraformlabs.dms.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("null") // Mockito any() matchers are untyped; save() is @NonNull in Spring Data
@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock private DocumentRepository documentRepository;
    @Mock private UserService userService;

    private DocumentService documentService;

    // Allowed types from application.yml: PDF,PNG,JPEG,JPG,DOCX,TXT → 10 MB max
    private static final long MAX_SIZE = 10_485_760L;
    private static final List<String> ALLOWED_TYPES = List.of("PDF", "PNG", "JPEG", "JPG", "DOCX", "TXT");

    @BeforeEach
    void setUp() {
        AppProperties props = new AppProperties();
        props.getDocuments().setMaxSizeBytes(MAX_SIZE);
        props.getDocuments().setAllowedTypes(ALLOWED_TYPES);
        documentService = new DocumentService(documentRepository, userService, props);
    }

    // =========== upload ===========

    @Test
    void upload_successfullyPersistsAndReturnsResponse() {
        AppUser user = mock(AppUser.class);
        when(userService.getByUsername("alice")).thenReturn(user);

        MockMultipartFile file = new MockMultipartFile(
                "file", "report.pdf", "application/pdf", "PDF content".getBytes());

        Document saved = mock(Document.class);
        when(saved.getId()).thenReturn(42L);
        when(saved.getOriginalFilename()).thenReturn("report.pdf");
        when(saved.getDocType()).thenReturn(DocumentType.PDF);
        when(saved.getFileSize()).thenReturn(11L);
        when(saved.getCreatedAt()).thenReturn(Instant.now());
        when(documentRepository.save(any(Document.class))).thenReturn(saved);

        DocumentUploadResponse response = documentService.upload("alice", file, "PDF");

        assertThat(response.documentId()).isEqualTo(42L);
        assertThat(response.originalFilename()).isEqualTo("report.pdf");
        assertThat(response.docType()).isEqualTo(DocumentType.PDF);
    }

    @Test
    void upload_throwsBadRequestWhenFileIsEmpty() {
        MockMultipartFile empty = new MockMultipartFile("file", new byte[0]);

        assertThatThrownBy(() -> documentService.upload("alice", empty, "PDF"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("File must be provided");
    }

    @Test
    void upload_throwsBadRequestWhenFileSizeExceedsMax() {
        byte[] oversized = new byte[(int) MAX_SIZE + 1];
        MockMultipartFile file = new MockMultipartFile(
                "file", "big.pdf", "application/pdf", oversized);

        assertThatThrownBy(() -> documentService.upload("alice", file, "PDF"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("File exceeds max allowed size");
    }

    @Test
    void upload_throwsBadRequestForUnrecognisedDocType() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "virus.exe", "application/octet-stream", "data".getBytes());

        assertThatThrownBy(() -> documentService.upload("alice", file, "EXE"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Unsupported document type");
    }

    @Test
    void upload_throwsBadRequestForDisallowedDocType() {
        // Temporarily construct service with empty allowed list
        AppProperties restrictedProps = new AppProperties();
        restrictedProps.getDocuments().setMaxSizeBytes(MAX_SIZE);
        restrictedProps.getDocuments().setAllowedTypes(List.of());
        DocumentService restrictedService =
                new DocumentService(documentRepository, userService, restrictedProps);

        MockMultipartFile file = new MockMultipartFile(
                "file", "report.pdf", "application/pdf", "data".getBytes());

        assertThatThrownBy(() -> restrictedService.upload("alice", file, "PDF"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Document type is not allowed");
    }

    @Test
    void upload_usesUnnamedWhenOriginalFilenameIsNull() {
        AppUser user = mock(AppUser.class);
        when(userService.getByUsername("alice")).thenReturn(user);

        // MockMultipartFile with null original name
        MockMultipartFile file = new MockMultipartFile("file", null, "application/pdf", "data".getBytes());

        Document saved = mock(Document.class);
        when(saved.getId()).thenReturn(1L);
        when(saved.getOriginalFilename()).thenReturn("unnamed");
        when(saved.getDocType()).thenReturn(DocumentType.PDF);
        when(saved.getFileSize()).thenReturn(4L);
        when(saved.getCreatedAt()).thenReturn(Instant.now());
        when(documentRepository.save(any(Document.class))).thenReturn(saved);

        DocumentUploadResponse response = documentService.upload("alice", file, "PDF");
        assertThat(response.originalFilename()).isEqualTo("unnamed");
    }

    // =========== download ===========

    @Test
    void download_returnsOkWithBodyAndHeaders() {
        AppUser user = mock(AppUser.class);
        when(user.getId()).thenReturn(1L);
        when(userService.getByUsername("alice")).thenReturn(user);

        Document doc = mock(Document.class);
        when(doc.getContent()).thenReturn(new byte[]{1, 2, 3, 4});
        when(doc.getContentType()).thenReturn("image/png");
        when(doc.getFileSize()).thenReturn(4L);
        when(doc.getOriginalFilename()).thenReturn("photo.png");
        when(documentRepository.findByIdAndUserId(7L, 1L)).thenReturn(Optional.of(doc));

        ResponseEntity<ByteArrayResource> response = documentService.download("alice", 7L);

        ByteArrayResource body = response.getBody();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(body).isNotNull();
        assertThat(body.getByteArray()).isEqualTo(new byte[]{1, 2, 3, 4});
        assertThat(response.getHeaders().getContentLength()).isEqualTo(4L);
    }

    @Test
    void download_throwsResourceNotFoundWhenDocumentMissing() {
        AppUser user = mock(AppUser.class);
        when(user.getId()).thenReturn(1L);
        when(userService.getByUsername("alice")).thenReturn(user);
        when(documentRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.download("alice", 99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Document not found");
    }

}

