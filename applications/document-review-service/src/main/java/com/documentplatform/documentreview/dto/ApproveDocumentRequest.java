package com.documentplatform.documentreview.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApproveDocumentRequest {
    @NotNull
    private Long expectedDocumentRevision;
    private String comment;
    private boolean overrideDuplicate;
}
