package com.documentplatform.documentreview.dto;

import jakarta.validation.constraints.NotNull;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FieldCorrectionRequest {
    @NotNull
    private Long expectedDocumentRevision;

    @NotNull
    private Map<String, Object> corrections;

    private String comment;
}
