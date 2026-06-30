package com.documentplatform.documentapi.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.documentplatform.documentapi.dto.CreateUploadRequest;
import com.documentplatform.documentapi.dto.CreateUploadResponse;
import com.documentplatform.documentapi.dto.DocumentResponse;
import com.documentplatform.documentapi.dto.PagedDocumentResponse;
import com.documentplatform.documentapi.enums.DocumentStatus;
import com.documentplatform.documentapi.enums.DocumentType;
import com.documentplatform.documentapi.repository.DynamoDbDocumentRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
class DocumentApiIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("document_platform")
            .withUsername("doc_user")
            .withPassword("doc_password");

    @DynamicPropertySource
    static void dbProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("jwt.secret", () -> "very-strong-secret-key-please-change");
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private DynamoDbDocumentRepository documentRepository;

    @MockBean
    private com.documentplatform.documentapi.service.S3PresignedUrlService s3PresignedUrlService;

    private HttpHeaders headers;

    @BeforeEach
    void setUp() throws Exception {
        when(s3PresignedUrlService.generateUploadUrl(any(), any(), any(), any())).thenReturn("https://upload.url");
        when(s3PresignedUrlService.generateViewUrl(any(), any(), any())).thenReturn("https://view.url");

        headers = new HttpHeaders();
        headers.setBearerAuth(tokenWithRole("SUPPLIER"));
    }

    @Test
    void createUploadRequestStoresMetadata() {
        CreateUploadRequest req = baseRequest();
        ResponseEntity<CreateUploadResponse> response = restTemplate.exchange(
                "/api/v1/documents/upload-request",
                HttpMethod.POST,
                new HttpEntity<>(req, headers),
                CreateUploadResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(documentRepository.findByDocumentId(response.getBody().getDocumentId())).isPresent();
    }

    @Test
    void unsupportedContentTypeReturns400() {
        CreateUploadRequest req = baseRequest();
        req.setContentType("application/x-msdownload");

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/documents/upload-request",
                HttpMethod.POST,
                new HttpEntity<>(req, headers),
                String.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void fileSizeAboveLimitReturns400() {
        CreateUploadRequest req = baseRequest();
        req.setFileSize(30_000_000L);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/documents/upload-request",
                HttpMethod.POST,
                new HttpEntity<>(req, headers),
                String.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void getDocumentReturns200() {
        CreateUploadResponse created = restTemplate.exchange(
                "/api/v1/documents/upload-request",
                HttpMethod.POST,
                new HttpEntity<>(baseRequest(), headers),
                CreateUploadResponse.class
        ).getBody();

        ResponseEntity<DocumentResponse> response = restTemplate.exchange(
                "/api/v1/documents/" + created.getDocumentId(),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                DocumentResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void unknownDocumentReturns404() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/documents/doc-unknown",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void listDocumentsReturnsPagedResponse() {
        restTemplate.exchange(
                "/api/v1/documents/upload-request",
                HttpMethod.POST,
                new HttpEntity<>(baseRequest(), headers),
                CreateUploadResponse.class
        );

        ResponseEntity<PagedDocumentResponse> response = restTemplate.exchange(
                "/api/v1/documents?customerId=customer-1001&status=" + DocumentStatus.UPLOAD_REQUESTED + "&documentType=" + DocumentType.INVOICE + "&page=0&size=20",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                PagedDocumentResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTotalElements()).isEqualTo(1);
    }

    private CreateUploadRequest baseRequest() {
        CreateUploadRequest req = new CreateUploadRequest();
        req.setCustomerId("customer-1001");
        req.setDocumentType(DocumentType.INVOICE);
        req.setFileName("invoice-1001.pdf");
        req.setContentType("application/pdf");
        req.setFileSize(1_048_576L);
        return req;
    }

    private String tokenWithRole(String role) throws Exception {
        byte[] keyBytes = "very-strong-secret-key-please-change".getBytes(StandardCharsets.UTF_8);
        return Jwts.builder()
                .subject("user-123")
                .issuer("document-platform")
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .claim("email", "user@example.com")
                .claim("roles", List.of(role))
                .signWith(Keys.hmacShaKeyFor(keyBytes), Jwts.SIG.HS256)
                .compact();
    }
}
