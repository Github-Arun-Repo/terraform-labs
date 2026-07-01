package com.documentplatform.documentapi.unit;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.documentplatform.documentapi.config.AwsProperties;
import com.documentplatform.documentapi.config.DocumentProperties;
import com.documentplatform.documentapi.dto.CreateUploadRequest;
import com.documentplatform.documentapi.enums.DocumentType;
import com.documentplatform.documentapi.exception.BadRequestException;
import com.documentplatform.documentapi.repository.DynamoDbDocumentRepository;
import com.documentplatform.documentapi.security.AuthenticatedUser;
import com.documentplatform.documentapi.service.DocumentIdGenerator;
import com.documentplatform.documentapi.service.DocumentMetricsService;
import com.documentplatform.documentapi.service.DocumentService;
import com.documentplatform.documentapi.service.S3KeyBuilder;
import com.documentplatform.documentapi.service.S3PresignedUrlService;
import com.documentplatform.documentapi.util.FileNameSanitizer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DocumentServiceValidationTest {

    private DocumentService service;

    @BeforeEach
    void setUp() {
        DynamoDbDocumentRepository repository = Mockito.mock(DynamoDbDocumentRepository.class);
        S3PresignedUrlService presignedUrlService = Mockito.mock(S3PresignedUrlService.class);
        when(presignedUrlService.generateUploadUrl(any(), any(), any(), any())).thenReturn("https://upload");

        DocumentProperties documentProperties = new DocumentProperties();
        documentProperties.setMaxFileSizeBytes(20_971_520);
        documentProperties.setAllowedContentTypes(List.of("application/pdf"));

        AwsProperties awsProperties = new AwsProperties();
        awsProperties.getS3().setBucketName("documents-inventory-s3");
        awsProperties.getS3().setUploadUrlExpiryMinutes(10);
        awsProperties.getS3().setViewUrlExpiryMinutes(5);
        awsProperties.setRegion("eu-central-1");

        service = new DocumentService(
                repository,
                new DocumentIdGenerator(),
                new S3KeyBuilder(new FileNameSanitizer()),
                presignedUrlService,
                new FileNameSanitizer(),
                documentProperties,
                awsProperties,
                new DocumentMetricsService(new SimpleMeterRegistry())
        );
    }

    @Test
    void shouldRejectUnsupportedContentType() {
        CreateUploadRequest request = new CreateUploadRequest();
        request.setCustomerId("customer-1");
        request.setDocumentType(DocumentType.INVOICE);
        request.setFileName("a.pdf");
        request.setContentType("application/x-msdownload");
        request.setFileSize(100L);

        assertThrows(BadRequestException.class,
                () -> service.createUploadRequest(request, new AuthenticatedUser("user-1", "a@b.com", Set.of("SUPPLIER"))));
    }
}
