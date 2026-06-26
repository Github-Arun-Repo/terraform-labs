package com.documentplatform.documentreview.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class InvoiceLineItemResponse {
    String description;
    Double quantity;
    Double unitPrice;
    Double amount;
}
