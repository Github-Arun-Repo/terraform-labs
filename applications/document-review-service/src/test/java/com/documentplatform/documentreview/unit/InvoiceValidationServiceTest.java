package com.documentplatform.documentreview.unit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.documentplatform.documentreview.exception.ValidationException;
import com.documentplatform.documentreview.model.ExtractionItem;
import com.documentplatform.documentreview.service.InvoiceValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InvoiceValidationServiceTest {

    private InvoiceValidationService service;

    @BeforeEach
    void setUp() {
        service = new InvoiceValidationService();
    }

    @Test
    void shouldValidateCompleteInvoice() {
        ExtractionItem extraction = new ExtractionItem();
        extraction.setInvoiceNumber("INV-1");
        extraction.setSupplierName("Acme");
        extraction.setTotalAmount(100.0);
        extraction.setCurrency("EUR");

        assertDoesNotThrow(() -> service.validateForApproval(extraction));
    }

    @Test
    void shouldFailWhenTotalMissing() {
        ExtractionItem extraction = new ExtractionItem();
        extraction.setInvoiceNumber("INV-1");
        extraction.setSupplierName("Acme");
        extraction.setCurrency("EUR");

        assertThrows(ValidationException.class, () -> service.validateForApproval(extraction));
    }
}
