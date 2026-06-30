package com.terraformlabs.documentprocessor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.terraformlabs.documentprocessor.config.DocumentProcessorProperties;
import com.terraformlabs.documentprocessor.dto.UploadInvoiceResponse;
import com.terraformlabs.documentprocessor.exception.BadRequestException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.services.s3.S3Client;

class InvoiceUploadServiceTest {

    private S3Client s3Client;
    private InvoiceUploadService invoiceUploadService;

    @BeforeEach
    void setUp() {
        s3Client = org.mockito.Mockito.mock(S3Client.class);

        DocumentProcessorProperties properties = new DocumentProcessorProperties();
        properties.getS3().setBucketName("documents-inventory-s3");
        properties.getUpload().setMaxFileSizeBytes(10);
        properties.getUpload().setAllowedExtensions(List.of("PDF"));
        properties.getUpload().setAllowedContentTypes(List.of("application/pdf"));

        invoiceUploadService = new InvoiceUploadService(s3Client, properties);
    }

    @Test
    void uploadInvoiceShouldStoreInConfiguredS3Path() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "invoice.pdf",
                "application/pdf",
                "12345".getBytes()
        );

        UploadInvoiceResponse response = invoiceUploadService.uploadInvoice("cust-001", file);

        assertEquals("documents-inventory-s3", response.bucketName());
        assertEquals("cust-001", response.customerId());
        assertEquals("pdf", response.fileType());
        assertEquals("application/pdf", response.contentType());
        org.junit.jupiter.api.Assertions.assertTrue(response.objectKey().startsWith("legacy/raw/pdf/cust-001/"));
        verify(s3Client).putObject(any(software.amazon.awssdk.services.s3.model.PutObjectRequest.class),
                any(software.amazon.awssdk.core.sync.RequestBody.class));
    }

    @Test
    void uploadInvoiceShouldRejectUnsupportedExtension() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "invoice.png",
                "image/png",
                "1234".getBytes()
        );

        assertThrows(BadRequestException.class, () -> invoiceUploadService.uploadInvoice("cust-001", file));
        verifyNoInteractions(s3Client);
    }

    @Test
    void uploadInvoiceShouldRejectOversizedFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "invoice.pdf",
                "application/pdf",
                "12345678901".getBytes()
        );

        assertThrows(BadRequestException.class, () -> invoiceUploadService.uploadInvoice("cust-001", file));
        verifyNoInteractions(s3Client);
    }
}
