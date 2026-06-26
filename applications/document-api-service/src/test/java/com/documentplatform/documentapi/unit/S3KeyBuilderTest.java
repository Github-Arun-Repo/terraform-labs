package com.documentplatform.documentapi.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.documentplatform.documentapi.enums.DocumentType;
import com.documentplatform.documentapi.exception.BadRequestException;
import com.documentplatform.documentapi.service.S3KeyBuilder;
import com.documentplatform.documentapi.util.FileNameSanitizer;
import org.junit.jupiter.api.Test;

class S3KeyBuilderTest {

    private final S3KeyBuilder builder = new S3KeyBuilder(new FileNameSanitizer());

    @Test
    void shouldBuildExpectedKey() {
        String key = builder.build(DocumentType.INVOICE, "customer-1001", "doc-abc", "invoice 1001.pdf");
        assertEquals("invoice/raw/customer-1001/doc-abc/invoice-1001.pdf", key);
    }

    @Test
    void shouldRejectEmptyCustomer() {
        assertThrows(BadRequestException.class,
                () -> builder.build(DocumentType.INVOICE, " ", "doc", "a.pdf"));
    }
}
