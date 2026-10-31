package com.email.writer.exception;

import org.springframework.http.HttpStatus;

public class ModelServiceException extends RuntimeException {
    private final HttpStatus status;

    public ModelServiceException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}

