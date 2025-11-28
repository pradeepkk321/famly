package io.famly.mapper.core.engine;

/**
 * Validation engine
 */
public class ValidationEngine {
    public void validate(Object value, String validator, String fieldId) {
        if (validator.equals("notEmpty()")) {
            if (value == null || value.toString().isEmpty()) {
                throw new ValidationException("Field " + fieldId + " cannot be empty");
            }
        } else if (validator.startsWith("regex(")) {
            String pattern = validator.substring(7, validator.length() - 2);
            if (value != null && !value.toString().matches(pattern)) {
                throw new ValidationException("Field " + fieldId + " does not match pattern: " + pattern);
            }
        }
    }
}
