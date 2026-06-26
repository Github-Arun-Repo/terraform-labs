package com.documentplatform.documentprocessing.unit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.documentplatform.documentprocessing.config.AppProperties;
import com.documentplatform.documentprocessing.model.DocumentItem;
import com.documentplatform.documentprocessing.service.DocumentValidationException;
import com.documentplatform.documentprocessing.service.S3ObjectValidator;
import java.io.ByteArrayInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

@ExtendWith(MockitoExtension.class)
class S3ObjectValidatorTest {

    @Mock
    private S3Client s3Client;

    private S3ObjectValidator validator;

    @BeforeEach
    void setUp() {
        AppProperties properties = new AppProperties();
        properties.setAllowedContentTypes(java.util.List.of("application/pdf", "image/png", "image/jpeg", "image/tiff"));
        properties.setMaxFileSizeBytes(1024 * 1024);
        validator = new S3ObjectValidator(s3Client, properties);
    }

    @Test
    void validPdfMagicBytesPassesValidation() {
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(HeadObjectResponse.builder().contentLength(100L).contentType("application/pdf").build());
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(stream(new byte[]{0x25, 0x50, 0x44, 0x46, 0x2D, 0x31}));

        assertDoesNotThrow(() -> validator.validate(document("application/pdf")));
    }

    @Test
    void invalidMagicBytesFailsValidation() {
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(HeadObjectResponse.builder().contentLength(100L).contentType("application/pdf").build());
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(stream(new byte[]{0x00, 0x11, 0x22, 0x33}));

        assertThrows(DocumentValidationException.class, () -> validator.validate(document("application/pdf")));
    }

    private DocumentItem document(String contentType) {
        DocumentItem item = new DocumentItem();
        item.setBucketName("documents-inventory-s3");
        item.setS3Key("invoice/raw/customer-1/doc-1/invoice.pdf");
        item.setContentType(contentType);
        return item;
    }

    private ResponseInputStream<GetObjectResponse> stream(byte[] bytes) {
        return new ResponseInputStream<>(
                GetObjectResponse.builder().build(),
                AbortableInputStream.create(new ByteArrayInputStream(bytes))
        );
    }
}
