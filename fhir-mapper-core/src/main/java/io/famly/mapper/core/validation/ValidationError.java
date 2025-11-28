package com.fhir.mapper.validation;

/**
 * Validation error/warning
 */
public class ValidationError {
    private String context;
    private String message;
    private ValidationSeverity severity;
    
    public ValidationError(String context, String message, ValidationSeverity severity) {
        this.context = context;
        this.message = message;
        this.severity = severity;
    }
    
    public String getContext() { return context; }
    public String getMessage() { return message; }
    public ValidationSeverity getSeverity() { return severity; }
}

enum ValidationSeverity {
    ERROR, WARNING
}