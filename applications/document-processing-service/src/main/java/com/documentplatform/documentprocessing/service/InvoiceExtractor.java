package com.documentplatform.documentprocessing.service;

import com.documentplatform.documentprocessing.model.DocumentItem;

public interface InvoiceExtractor {
    ExtractionResult extract(DocumentItem documentItem);
}
