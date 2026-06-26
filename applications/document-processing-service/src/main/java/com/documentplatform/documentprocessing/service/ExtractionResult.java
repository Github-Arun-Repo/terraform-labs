package com.documentplatform.documentprocessing.service;

import java.util.List;
import java.util.Map;

public record ExtractionResult(
        String invoiceNumber,
        String supplierName,
        String invoiceDate,
        String currency,
        Double totalAmount,
        Double confidenceScore,
        List<Map<String, Object>> lineItems,
        List<String> validationErrors,
        String rawJson,
        String normalizedJson
) {
}
