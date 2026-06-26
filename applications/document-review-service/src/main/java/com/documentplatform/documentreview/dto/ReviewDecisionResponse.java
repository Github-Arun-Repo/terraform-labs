package com.documentplatform.documentreview.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ReviewDecisionResponse {
    String documentId;
    String decision;
    String decidedBy;
    String reasonCode;
    String comment;
    String createdAt;
}
