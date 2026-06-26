package com.documentplatform.documentprocessing.service;

import com.documentplatform.documentprocessing.model.DocumentItem;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class TextractInvoiceExtractor implements InvoiceExtractor {

    @Override
    public ExtractionResult extract(DocumentItem documentItem) {
        // Placeholder deterministic result for AWS_TEXTRACT mode.
        return new ExtractionResult(
                "INV-" + documentItem.getDocumentId(),
                "Textract Supplier",
                java.time.LocalDate.now().toString(),
                "EUR",
                120.0,
                0.88,
                List.of(Map.of("description", "Parsed Line", "amount", 120.0, "quantity", 1, "unitPrice", 120.0)),
                List.of(),
                "{\"source\":\"textract\"}",
                "{\"normalized\":true}"
        );
    }
}
