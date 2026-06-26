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
    int totalPages;
}
