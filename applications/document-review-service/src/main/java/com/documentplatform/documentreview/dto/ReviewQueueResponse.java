package com.documentplatform.documentreview.dto;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ReviewQueueResponse {
    List<ReviewQueueItemResponse> items;
    String nextToken;
}
