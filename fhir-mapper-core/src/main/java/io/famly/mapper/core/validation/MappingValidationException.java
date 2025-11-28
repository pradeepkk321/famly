package com.fhir.mapper.validation;

public class MappingValidationException extends RuntimeException {
    public MappingValidationException(String message) {
        super(message);
    }
}
