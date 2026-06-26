package com.documentplatform.documentreview.service;

import com.documentplatform.documentreview.exception.ValidationException;
import com.documentplatform.documentreview.model.ExtractionItem;
import org.springframework.stereotype.Service;

@Service
public class InvoiceValidationService {

    public void validateForApproval(ExtractionItem extractionItem) {
        if (extractionItem == null) {
            throw new ValidationException("Extraction is required for approval");
        }
        if (isBlank(extractionItem.getInvoiceNumber())) {
            throw new ValidationException("Invoice number is required");
        }
        if (isBlank(extractionItem.getSupplierName())) {
            throw new ValidationException("Supplier name is required");
        }
        if (extractionItem.getTotalAmount() == null || extractionItem.getTotalAmount() <= 0) {
            throw new ValidationException("Total amount must be greater than zero");
        }
        if (isBlank(extractionItem.getCurrency())) {
            throw new ValidationException("Currency is required");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
