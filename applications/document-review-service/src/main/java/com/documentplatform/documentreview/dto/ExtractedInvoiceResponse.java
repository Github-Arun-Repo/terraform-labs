package com.documentplatform.documentreview.dto;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ExtractedInvoiceResponse {
    String invoiceNumber;
    String supplierName;
    String supplierAddress;
    String customerName;
    String invoiceDate;
    String dueDate;
    String currency;
    Double subtotalAmount;
    Double taxAmount;
    Double totalAmount;
    String iban;
    Double confidenceScore;
    List<InvoiceLineItemResponse> lineItems;
    List<String> validationErrors;
    List<Map<String, Object>> manualCorrections;
}
