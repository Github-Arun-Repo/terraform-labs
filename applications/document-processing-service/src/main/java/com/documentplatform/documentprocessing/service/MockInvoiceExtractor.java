package com.documentplatform.documentprocessing.service;

import com.documentplatform.documentprocessing.model.DocumentItem;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class MockInvoiceExtractor implements InvoiceExtractor {

    @Override
    public ExtractionResult extract(DocumentItem documentItem) {
        String docId = documentItem.getDocumentId();
        String raw = "{\"mock\":true,\"documentId\":\"" + docId + "\"}";
        String normalized = "{\"invoiceNumber\":\"INV-" + docId + "\",\"supplierName\":\"Mock Supplier\"}";

        return new ExtractionResult(
                "INV-" + docId,
                "Mock Supplier",
                java.time.LocalDate.now().toString(),
                "EUR",
                100.0,
                0.95,
                List.of(Map.of("description", "Service Fee", "amount", 100.0, "quantity", 1, "unitPrice", 100.0)),
                List.of(),
                raw,
                normalized
        );
    }
}
