package com.fhir.mapper.engine;

public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}
