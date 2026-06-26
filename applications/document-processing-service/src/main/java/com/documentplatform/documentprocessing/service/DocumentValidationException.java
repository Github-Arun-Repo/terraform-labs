package com.documentplatform.documentprocessing.service;

public class DocumentValidationException extends RuntimeException {

    public DocumentValidationException(String message) {
        super(message);
    }

    public DocumentValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}