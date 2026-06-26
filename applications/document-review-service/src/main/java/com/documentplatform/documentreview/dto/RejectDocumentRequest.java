package com.documentplatform.documentreview.dto;

import com.documentplatform.documentreview.enums.RejectReasonCode;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RejectDocumentRequest {
    @NotNull
    private Long expectedDocumentRevision;

    @NotNull
    private RejectReasonCode reasonCode;

    private String comment;
}
