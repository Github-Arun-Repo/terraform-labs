package com.documentplatform.documentreview.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class RevisionConflictException extends ApiException {

    private final Long currentRevision;
    private final Long expectedRevision;

    public RevisionConflictException(Long currentRevision, Long expectedRevision) {
        super(HttpStatus.CONFLICT, "Document revision conflict");
        this.currentRevision = currentRevision;
        this.expectedRevision = expectedRevision;
    }
}
