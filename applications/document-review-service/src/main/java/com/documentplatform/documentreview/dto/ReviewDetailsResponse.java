package com.documentplatform.documentreview.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ReviewDetailsResponse {
    DocumentSummaryResponse document;
    ExtractedInvoiceResponse extraction;
    ViewUrlResponse viewUrl;
}
