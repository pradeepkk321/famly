package com.fhir.mapper.validation;

import java.util.ArrayList;
import java.util.List;

/**
 * Validation result container
 */
public class ValidationResult {
    private List<ValidationError> errors = new ArrayList<>();
    private List<ValidationError> warnings = new ArrayList<>();
    
    public void addError(String context, String message) {
        errors.add(new ValidationError(context, message, ValidationSeverity.ERROR));
    }
    
    public void addWarning(String context, String message) {
        warnings.add(new ValidationError(context, message, ValidationSeverity.WARNING));
    }
    
    public boolean isValid() {
        return errors.isEmpty();
    }
    
    public List<ValidationError> getErrors() {
        return errors;
    }
    
    public List<ValidationError> getWarnings() {
        return warnings;
    }
    
    public void throwIfInvalid() {
        if (!isValid()) {
            StringBuilder sb = new StringBuilder("Validation failed:\n");
            for (ValidationError error : errors) {
                sb.append("  [ERROR] ").append(error.getContext())
                  .append(": ").append(error.getMessage()).append("\n");
            }
            throw new MappingValidationException(sb.toString());
        }
    }
    
    public void printWarnings() {
        if (!warnings.isEmpty()) {
            System.out.println("Validation warnings:");
            for (ValidationError warning : warnings) {
                System.out.println("  [WARN] " + warning.getContext() + 
                    ": " + warning.getMessage());
            }
        }
    }
}
