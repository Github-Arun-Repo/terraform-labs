package com.documentplatform.documentreview.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ViewUrlResponse {
    String url;
    Long expiresInSeconds;
}
