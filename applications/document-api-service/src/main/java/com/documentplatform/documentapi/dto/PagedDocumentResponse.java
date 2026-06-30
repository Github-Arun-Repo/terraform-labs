package com.documentplatform.documentapi.dto;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PagedDocumentResponse {
    List<DocumentResponse> content;
    int page;
    int size;
    long totalElements;
    // DynamoDB cursor for the next page (opaque). Null when there are no more results.
    String nextToken;
    boolean hasMore;
}
