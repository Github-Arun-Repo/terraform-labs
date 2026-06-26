package com.documentplatform.documentreview.exception;

import com.documentplatform.documentreview.dto.ErrorResponse;
import com.documentplatform.documentreview.security.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException exception, HttpServletRequest request) {
        Long currentRevision = null;
        Long expectedRevision = null;
        if (exception instanceof RevisionConflictException revisionConflictException) {
            currentRevision = revisionConflictException.getCurrentRevision();
            expectedRevision = revisionConflictException.getExpectedRevision();
        }

        return ResponseEntity.status(exception.getStatus())
                .body(errorBody(exception.getStatus(), exception.getMessage(), request, currentRevision, expectedRevision));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
    public ResponseEntity<ErrorResponse> handleValidationException(Exception exception, HttpServletRequest request) {
        return ResponseEntity.badRequest()
                .body(errorBody(HttpStatus.BAD_REQUEST, "Validation failed", request, null, null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnhandled(Exception exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorBody(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error", request, null, null));
    }

    private ErrorResponse errorBody(
            HttpStatus status,
            String message,
            HttpServletRequest request,
            Long currentRevision,
            Long expectedRevision
    ) {
        return ErrorResponse.builder()
                .timestamp(Instant.now().toString())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .correlationId(request.getHeader(CorrelationIdFilter.HEADER))
                .currentRevision(currentRevision)
                .expectedRevision(expectedRevision)
                .build();
    }
}
