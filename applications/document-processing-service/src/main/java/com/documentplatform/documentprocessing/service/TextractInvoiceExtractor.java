package com.documentplatform.documentprocessing.service;

import com.documentplatform.documentprocessing.model.DocumentItem;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * NOTE: This is a STUB. Despite the name and the {@code AWS_TEXTRACT} extractor mode,
 * it does NOT call Amazon Textract; it returns deterministic placeholder data so the
 * end-to-end pipeline can be exercised without incurring Textract costs. Implementing
 * real extraction means calling Textract AnalyzeExpense and mapping the response here.
 */
@Component
public class TextractInvoiceExtractor implements InvoiceExtractor {

    private static final Logger LOGGER = LoggerFactory.getLogger(TextractInvoiceExtractor.class);

    @Override
    public ExtractionResult extract(DocumentItem documentItem) {
        LOGGER.warn("AWS_TEXTRACT extractor is a stub and does not call Amazon Textract; "
                + "returning placeholder extraction for documentId={}", documentItem.getDocumentId());
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
