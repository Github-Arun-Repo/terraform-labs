package com.documentplatform.documentprocessing.unit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.documentplatform.documentprocessing.config.AppProperties;
import com.documentplatform.documentprocessing.enums.DocumentStatus;
import com.documentplatform.documentprocessing.model.DocumentItem;
import com.documentplatform.documentprocessing.repository.DynamoAuditRepository;
import com.documentplatform.documentprocessing.repository.DynamoDocumentRepository;
import com.documentplatform.documentprocessing.repository.DynamoExtractionRepository;
import com.documentplatform.documentprocessing.service.DocumentProcessingService;
import com.documentplatform.documentprocessing.service.DocumentValidationException;
import com.documentplatform.documentprocessing.service.ExtractionResult;
import com.documentplatform.documentprocessing.service.MockInvoiceExtractor;
import com.documentplatform.documentprocessing.service.S3ObjectValidator;
import com.documentplatform.documentprocessing.service.TextractInvoiceExtractor;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3Client;

@ExtendWith(MockitoExtension.class)
class DocumentProcessingServiceTest {

    @Mock
    private DynamoDocumentRepository documentRepository;

    @Mock
    private DynamoExtractionRepository extractionRepository;

    @Mock
    private DynamoAuditRepository auditRepository;

    @Mock
    private MockInvoiceExtractor mockInvoiceExtractor;

    @Mock
    private TextractInvoiceExtractor textractInvoiceExtractor;

    @Mock
    private S3ObjectValidator s3ObjectValidator;

    @Mock
    private S3Client s3Client;

    private DocumentProcessingService service;

    @BeforeEach
    void setUp() {
        AppProperties appProperties = new AppProperties();
        appProperties.setMaxProcessingAttempts(3);
        appProperties.setProcessingStaleTimeoutMinutes(10);
        service = new DocumentProcessingService(
                documentRepository,
                extractionRepository,
                auditRepository,
                mockInvoiceExtractor,
                textractInvoiceExtractor,
                s3ObjectValidator,
                s3Client,
                appProperties,
                new SimpleMeterRegistry()
        );
    }

    @Test
    void duplicateEventSkipsAlreadyPendingApproval() {
        DocumentItem document = document(DocumentStatus.PENDING_APPROVAL, 1);
        when(documentRepository.findByS3Key("invoice/raw/customer-1/doc-1/invoice.pdf")).thenReturn(Optional.of(document));
        when(documentRepository.tryStartProcessing(any(), any(), anyInt(), anyInt()))
                .thenReturn(DynamoDocumentRepository.ProcessingStartOutcome.SKIPPED_FINAL);

        boolean result = service.process("documents-inventory-s3", "invoice/raw/customer-1/doc-1/invoice.pdf");

        assertTrue(result);
        verify(mockInvoiceExtractor, never()).extract(any());
    }

    @Test
    void uploadRequestedTransitionsToProcessingAndProcesses() {
        DocumentItem original = document(DocumentStatus.UPLOAD_REQUESTED, 0);
        DocumentItem transitioned = document(DocumentStatus.PROCESSING, 1);

        when(documentRepository.findByS3Key(anyString())).thenReturn(Optional.of(original));
        when(documentRepository.tryStartProcessing(any(), any(), anyInt(), anyInt()))
                .thenReturn(DynamoDocumentRepository.ProcessingStartOutcome.STARTED);
        when(documentRepository.findByDocumentId(original.getDocumentId())).thenReturn(Optional.of(transitioned));
        when(mockInvoiceExtractor.extract(any())).thenReturn(validExtraction());
        when(documentRepository.createDuplicateIndexItem(anyString(), anyString(), anyString(), anyString(), anyString(), any()))
                .thenReturn(true);

        boolean result = service.process(original.getBucketName(), original.getS3Key());

        assertTrue(result);
        verify(documentRepository).tryStartProcessing(any(), any(), anyInt(), anyInt());
        verify(extractionRepository).save(any());
    }

    @Test
    void secondConcurrentProcessingTransitionFailsSafely() {
        DocumentItem document = document(DocumentStatus.UPLOAD_REQUESTED, 0);
        when(documentRepository.findByS3Key(anyString())).thenReturn(Optional.of(document));
        when(documentRepository.tryStartProcessing(any(), any(), anyInt(), anyInt()))
                .thenReturn(DynamoDocumentRepository.ProcessingStartOutcome.TRANSITION_CONFLICT);

        boolean result = service.process(document.getBucketName(), document.getS3Key());

        assertTrue(result);
        verify(mockInvoiceExtractor, never()).extract(any());
    }

    @Test
    void duplicateInvoiceSetsDuplicateDetectedStatus() {
        DocumentItem original = document(DocumentStatus.UPLOADED, 0);
        DocumentItem transitioned = document(DocumentStatus.PROCESSING, 1);

        when(documentRepository.findByS3Key(anyString())).thenReturn(Optional.of(original));
        when(documentRepository.tryStartProcessing(any(), any(), anyInt(), anyInt()))
                .thenReturn(DynamoDocumentRepository.ProcessingStartOutcome.STARTED);
        when(documentRepository.findByDocumentId(original.getDocumentId())).thenReturn(Optional.of(transitioned));
        when(mockInvoiceExtractor.extract(any())).thenReturn(validExtraction());
        when(documentRepository.createDuplicateIndexItem(anyString(), anyString(), anyString(), anyString(), anyString(), any()))
                .thenReturn(false);

        boolean result = service.process(original.getBucketName(), original.getS3Key());

        assertTrue(result);
        ArgumentCaptor<DocumentItem> captor = ArgumentCaptor.forClass(DocumentItem.class);
        verify(documentRepository).save(captor.capture());
        assertTrue(captor.getValue().getStatus() == DocumentStatus.DUPLICATE_DETECTED);
    }

    @Test
    void validInvoiceCreatesDuplicateIndexItem() {
        DocumentItem original = document(DocumentStatus.UPLOADED, 0);
        DocumentItem transitioned = document(DocumentStatus.PROCESSING, 1);

        when(documentRepository.findByS3Key(anyString())).thenReturn(Optional.of(original));
        when(documentRepository.tryStartProcessing(any(), any(), anyInt(), anyInt()))
                .thenReturn(DynamoDocumentRepository.ProcessingStartOutcome.STARTED);
        when(documentRepository.findByDocumentId(original.getDocumentId())).thenReturn(Optional.of(transitioned));
        when(mockInvoiceExtractor.extract(any())).thenReturn(validExtraction());
        when(documentRepository.createDuplicateIndexItem(anyString(), anyString(), anyString(), anyString(), anyString(), any()))
                .thenReturn(true);

        boolean result = service.process(original.getBucketName(), original.getS3Key());

        assertTrue(result);
        verify(documentRepository).createDuplicateIndexItem(
                eq("DUPLICATE#customer-1#acme-gmbh#inv-1001"),
                eq("doc-1"),
                eq("customer-1"),
                eq("acme-gmbh"),
                eq("inv-1001"),
                any()
        );
    }

    @Test
    void invalidMagicBytesFailValidationAndSetFailed() {
        DocumentItem original = document(DocumentStatus.UPLOADED, 0);
        DocumentItem transitioned = document(DocumentStatus.PROCESSING, 1);

        when(documentRepository.findByS3Key(anyString())).thenReturn(Optional.of(original));
        when(documentRepository.tryStartProcessing(any(), any(), anyInt(), anyInt()))
                .thenReturn(DynamoDocumentRepository.ProcessingStartOutcome.STARTED);
        when(documentRepository.findByDocumentId(original.getDocumentId())).thenReturn(Optional.of(transitioned));
        org.mockito.Mockito.doThrow(new DocumentValidationException("Invalid object signature"))
                .when(s3ObjectValidator).validate(any());

        boolean result = service.process(original.getBucketName(), original.getS3Key());

        assertTrue(result);
        ArgumentCaptor<DocumentItem> captor = ArgumentCaptor.forClass(DocumentItem.class);
        verify(documentRepository).save(captor.capture());
        assertTrue(captor.getValue().getStatus() == DocumentStatus.FAILED);
    }

    @Test
    void maxProcessingAttemptsSetsFailed() {
        DocumentItem document = document(DocumentStatus.UPLOADED, 3);
        when(documentRepository.findByS3Key(anyString())).thenReturn(Optional.of(document));
        when(documentRepository.tryStartProcessing(any(), any(), anyInt(), anyInt()))
                .thenReturn(DynamoDocumentRepository.ProcessingStartOutcome.MAX_ATTEMPTS_EXCEEDED);

        boolean result = service.process(document.getBucketName(), document.getS3Key());

        assertTrue(result);
        ArgumentCaptor<DocumentItem> captor = ArgumentCaptor.forClass(DocumentItem.class);
        verify(documentRepository).save(captor.capture());
        assertTrue(captor.getValue().getStatus() == DocumentStatus.FAILED);
    }

    @Test
    void staleProcessingCanRetry() {
        DocumentItem original = document(DocumentStatus.PROCESSING, 1);
        original.setProcessingStartedAt("2026-06-20T00:00:00Z");
        DocumentItem transitioned = document(DocumentStatus.PROCESSING, 2);

        when(documentRepository.findByS3Key(anyString())).thenReturn(Optional.of(original));
        when(documentRepository.tryStartProcessing(any(), any(), anyInt(), anyInt()))
                .thenReturn(DynamoDocumentRepository.ProcessingStartOutcome.STARTED);
        when(documentRepository.findByDocumentId(original.getDocumentId())).thenReturn(Optional.of(transitioned));
        when(mockInvoiceExtractor.extract(any())).thenReturn(validExtraction());
        when(documentRepository.createDuplicateIndexItem(anyString(), anyString(), anyString(), anyString(), anyString(), any()))
                .thenReturn(true);

        boolean result = service.process(original.getBucketName(), original.getS3Key());

        assertTrue(result);
        verify(documentRepository).tryStartProcessing(any(), any(), anyInt(), anyInt());
    }

    private DocumentItem document(DocumentStatus status, int attempts) {
        DocumentItem item = new DocumentItem();
        item.setPk("DOCUMENT#doc-1");
        item.setSk("METADATA");
        item.setDocumentId("doc-1");
        item.setCustomerId("customer-1");
        item.setDocumentType("invoice");
        item.setBucketName("documents-inventory-s3");
        item.setS3Key("invoice/raw/customer-1/doc-1/invoice.pdf");
        item.setStatus(status);
        item.setProcessingAttempts(attempts);
        item.setDocumentRevision(1L);
        item.setCreatedAt("2026-06-20T00:00:00Z");
        item.setUpdatedAt("2026-06-20T00:00:00Z");
        return item;
    }

    private ExtractionResult validExtraction() {
        return new ExtractionResult(
                "INV-1001",
                "Acme GmbH",
                "2026-06-26",
                "EUR",
                100.0,
                0.98,
                List.of(Map.of("description", "item", "amount", 100.0)),
                List.of(),
                "{\"raw\":true}",
                "{\"normalized\":true}"
        );
    }
}
