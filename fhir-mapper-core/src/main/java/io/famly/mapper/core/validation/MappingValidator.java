package com.fhir.mapper.validation;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.JexlExpression;

import com.fhir.mapper.model.*;

import ca.uhn.fhir.context.FhirContext;

/**
 * Enhanced validator with uniqueness checks for IDs and names
 */
public class MappingValidator {
    private final JexlEngine jexlEngine;
    private final FhirPathValidator fhirPathValidator;
    private final Set<String> validDataTypes;
    
    public MappingValidator() {
        this(FhirContext.forR4());
    }

    public MappingValidator(FhirContext fhirContext) {
        this.jexlEngine = new JexlBuilder().create();
        this.fhirPathValidator = new FhirPathValidator(fhirContext);
        this.validDataTypes = initValidDataTypes();
    }

    /**
     * Validate entire mapping registry with uniqueness checks
     */
    public ValidationResult validateRegistry(MappingRegistry registry) {
        ValidationResult result = new ValidationResult();
        
        // Validate uniqueness across all mappings
        validateUniqueIds(registry, result);
        validateUniqueNames(registry, result);
        
        // Validate lookup tables
        validateLookupTableUniqueness(registry, result);
        for (CodeLookupTable lookup : registry.getLookupTables().values()) {
            validateLookupTable(lookup, result);
        }
        
        // Validate resource mappings
        for (ResourceMapping mapping : registry.getResourceMappings()) {
            validateResourceMapping(mapping, registry, result);
        }
        
        return result;
    }
    
    /**
     * Validate that all mapping IDs are unique
     */
    private void validateUniqueIds(MappingRegistry registry, ValidationResult result) {
        Map<String, List<ResourceMapping>> idGroups = registry.getResourceMappings().stream()
            .filter(m -> m.getId() != null)
            .collect(Collectors.groupingBy(ResourceMapping::getId));
        
        idGroups.forEach((id, mappings) -> {
            if (mappings.size() > 1) {
                String context = "Registry Validation";
                result.addError(context, 
                    String.format("Duplicate mapping ID '%s' found in %d mappings: %s",
                        id,
                        mappings.size(),
                        mappings.stream()
                            .map(m -> m.getName() != null ? m.getName() : "unnamed")
                            .collect(Collectors.joining(", "))
                    )
                );
            }
        });
    }
    
    /**
     * Validate that all mapping names are unique (WARNING only)
     */
    private void validateUniqueNames(MappingRegistry registry, ValidationResult result) {
        Map<String, List<ResourceMapping>> nameGroups = registry.getResourceMappings().stream()
            .filter(m -> m.getName() != null && !m.getName().isEmpty())
            .collect(Collectors.groupingBy(ResourceMapping::getName));
        
        nameGroups.forEach((name, mappings) -> {
            if (mappings.size() > 1) {
                String context = "Registry Validation";
                result.addWarning(context, 
                    String.format("Duplicate mapping name '%s' found in %d mappings: %s. " +
                        "Consider using unique names for better clarity.",
                        name,
                        mappings.size(),
                        mappings.stream()
                            .map(ResourceMapping::getId)
                            .collect(Collectors.joining(", "))
                    )
                );
            }
        });
    }
    
    /**
     * Validate that all lookup table IDs are unique
     */
    private void validateLookupTableUniqueness(MappingRegistry registry, ValidationResult result) {
        Map<String, CodeLookupTable> lookups = registry.getLookupTables();
        Set<String> names = new HashSet<>();
        
        for (CodeLookupTable lookup : lookups.values()) {
            if (lookup.getName() != null && !lookup.getName().isEmpty()) {
                if (!names.add(lookup.getName())) {
                    result.addError("Lookup Table Validation",
                        "Duplicate lookup table name: " + lookup.getName());
                }
            }
        }
    }

    /**
     * Validate resource mapping with enhanced checks
     */
    public void validateResourceMapping(ResourceMapping mapping, MappingRegistry registry, 
                                        ValidationResult result) {
        String context = "Mapping: " + (mapping.getId() != null ? mapping.getId() : "unknown");
        
        // Basic validations
        if (mapping.getId() == null || mapping.getId().isEmpty()) {
            result.addError(context, "Mapping ID is required");
        } else {
            // Validate ID format (alphanumeric, hyphens, underscores only)
            if (!mapping.getId().matches("^[a-zA-Z0-9_-]+$")) {
                result.addError(context, 
                    "Mapping ID must contain only alphanumeric characters, hyphens, and underscores: " + 
                    mapping.getId());
            }
        }
        
        if (mapping.getName() == null || mapping.getName().isEmpty()) {
            result.addWarning(context, "Mapping name is recommended");
        }
        
        if (mapping.getDirection() == null) {
            result.addError(context, "Mapping direction is required");
        }
        
        if (mapping.getSourceType() == null || mapping.getSourceType().isEmpty()) {
            result.addError(context, "Source type is required");
        }
        
        if (mapping.getTargetType() == null || mapping.getTargetType().isEmpty()) {
            result.addError(context, "Target type is required");
        }
        
        if (mapping.getFieldMappings() == null || mapping.getFieldMappings().isEmpty()) {
            result.addError(context, "At least one field mapping is required");
            return; // Can't validate field mappings if none exist
        }
        
        // Validate FHIR resource type
        if (mapping.getDirection() == MappingDirection.JSON_TO_FHIR) {
            if (!fhirPathValidator.isValidResourceType(mapping.getTargetType())) {
                result.addError(context, "Invalid FHIR resource type: " + mapping.getTargetType());
            }
        } else if (mapping.getDirection() == MappingDirection.FHIR_TO_JSON) {
            if (!fhirPathValidator.isValidResourceType(mapping.getSourceType())) {
                result.addError(context, "Invalid FHIR resource type: " + mapping.getSourceType());
            }
        }
        
        // Validate field mapping uniqueness and integrity
        validateFieldMappingUniqueness(mapping, result);
        
        // Validate each field mapping
        for (FieldMapping field : mapping.getFieldMappings()) {
            validateFieldMapping(field, mapping, registry, result);
        }
    }
    
    /**
     * Validate field mapping ID uniqueness within a resource mapping
     */
    private void validateFieldMappingUniqueness(ResourceMapping mapping, ValidationResult result) {
        String context = "Mapping: " + mapping.getId();
        
        Map<String, List<FieldMapping>> idGroups = mapping.getFieldMappings().stream()
            .filter(f -> f.getId() != null)
            .collect(Collectors.groupingBy(FieldMapping::getId));
        
        idGroups.forEach((id, fields) -> {
            if (fields.size() > 1) {
                result.addError(context, 
                    String.format("Duplicate field mapping ID '%s' found %d times", id, fields.size())
                );
            }
        });
        
        // Check for duplicate target paths (potential conflict)
        Map<String, List<FieldMapping>> targetPathGroups = mapping.getFieldMappings().stream()
            .filter(f -> f.getTargetPath() != null)
            .collect(Collectors.groupingBy(FieldMapping::getTargetPath));
        
        targetPathGroups.forEach((path, fields) -> {
            if (fields.size() > 1) {
                // Only warn if they don't have mutually exclusive conditions
                boolean hasConditions = fields.stream().allMatch(f -> f.getCondition() != null);
                if (!hasConditions) {
                    result.addWarning(context,
                        String.format("Multiple field mappings target the same path '%s' without conditions. " +
                            "This may cause conflicts. Field IDs: %s",
                            path,
                            fields.stream().map(FieldMapping::getId).collect(Collectors.joining(", "))
                        )
                    );
                }
            }
        });
    }

    /**
     * Validate individual field mapping
     */
    private void validateFieldMapping(FieldMapping field, ResourceMapping parent, 
                                      MappingRegistry registry, ValidationResult result) {
        String context = "Mapping: " + parent.getId() + ", Field: " + 
            (field.getId() != null ? field.getId() : "unknown");
        
        // ID validation
        if (field.getId() == null || field.getId().isEmpty()) {
            result.addError(context, "Field ID is required");
        } else {
            // Validate ID format
            if (!field.getId().matches("^[a-zA-Z0-9_-]+$")) {
                result.addError(context, 
                    "Field ID must contain only alphanumeric characters, hyphens, and underscores: " + 
                    field.getId());
            }
        }
        
        // Target path is always required
        if (field.getTargetPath() == null || field.getTargetPath().isEmpty()) {
            result.addError(context, "Target path is required");
        }
        
        // Source path validation (can be null if default value is provided)
        if (field.getSourcePath() == null && field.getDefaultValue() == null) {
            if (field.isRequired()) {
                result.addError(context, "Required field must have either sourcePath or defaultValue");
            } else {
                result.addWarning(context, "Field has neither sourcePath nor defaultValue");
            }
        }
        
        // Data type validation
        if (field.getDataType() != null && !validDataTypes.contains(field.getDataType())) {
            result.addError(context, "Invalid data type: " + field.getDataType() + 
                ". Valid types: " + validDataTypes);
        }
        
        // FHIR path validation
        String resourceType = parent.getDirection() == MappingDirection.JSON_TO_FHIR ? 
            parent.getTargetType() : parent.getSourceType();
        
        String fhirPath = parent.getDirection() == MappingDirection.JSON_TO_FHIR ?
            field.getTargetPath() : field.getSourcePath();
            
        if (fhirPath != null) {
            ValidationResult pathResult = fhirPathValidator.validatePathExists(resourceType, fhirPath);
            if (!pathResult.isValid()) {
                result.addError(context, "Invalid FHIR path '" + fhirPath + "': " + 
                    pathResult.getErrors().stream()
                        .map(ValidationError::getMessage)
                        .collect(Collectors.joining(", "))
                );
            }
            
            // Validate dataType matches FHIR path expected type
            if (field.getDataType() != null) {
                String expectedType = fhirPathValidator.getExpectedType(resourceType, fhirPath);
                if (expectedType != null && !isCompatibleType(field.getDataType(), expectedType)) {
                    result.addWarning(context, "DataType mismatch: field specifies '" + 
                        field.getDataType() + "' but FHIR path expects '" + expectedType + "'");
                }
            }
        }
        
        // Condition validation
        if (field.getCondition() != null) {
            validateExpression(field.getCondition(), context + " (condition)", result, true);
        }
        
        // Transform expression validation
        if (field.getTransformExpression() != null) {
            validateExpression(field.getTransformExpression(), context + " (transform)", result, false);
        }
        
        // Validator validation
        if (field.getValidator() != null) {
            validateValidatorExpression(field.getValidator(), context, result);
        }
        
        // Lookup table validation
        if (field.getLookupTable() != null) {
            if (registry.getLookupTable(field.getLookupTable()) == null) {
                result.addError(context, "Lookup table not found: " + field.getLookupTable());
            }
        }
    }

    /**
     * Validate JEXL expression
     */
    private void validateExpression(String expression, String context, 
                                    ValidationResult result, boolean mustBeBoolean) {
        try {
            // Replace context variables with dummy values for validation
            String testExpression = prepareExpressionForValidation(expression);
            
            JexlExpression expr = jexlEngine.createExpression(testExpression);
            
            // Additional check: if it must be boolean, verify it's a comparison/logical expression
            if (mustBeBoolean) {
                // Basic heuristic: boolean expressions usually contain operators
                if (!expression.matches(".*[=!<>].*|.*\\b(and|or|not)\\b.*")) {
                    result.addWarning(context, "Condition may not evaluate to boolean: " + expression);
                }
            }
        } catch (JexlException e) {
            result.addError(context, "Invalid JEXL expression '" + expression + "': " + e.getMessage());
        }
    }
    
    /**
     * Prepare expression for validation by replacing context variables
     */
    private String prepareExpressionForValidation(String expression) {
        if (expression == null) {
            return expression;
        }
        
        String prepared = expression;
        
        // Replace old-style context variable references
        if (prepared.contains("$ctx.")) {
            prepared = prepared.replaceAll("\\$ctx\\.organizationId", "'test-org-id'");
            prepared = prepared.replaceAll("\\$ctx\\.facilityId", "'test-facility-id'");
            prepared = prepared.replaceAll("\\$ctx\\.tenantId", "'test-tenant-id'");
            prepared = prepared.replaceAll("\\$ctx\\.identifierSystem", "'test-system'");
            prepared = prepared.replaceAll("\\$ctx\\.settings\\['([^']+)'\\]", "'test-value'");
            prepared = prepared.replaceAll("\\$ctx\\.settings\\[\"([^\"]+)\"\\]", "'test-value'");
            prepared = prepared.replaceAll("\\$ctx\\.\\w+", "'test-value'");
        }
        
        // Replace new-style context access (ctx.property)
        // This is trickier because we need to preserve the JEXL structure
        // For validation purposes, we just ensure the expression parses
        
        return prepared;
    }

    /**
     * Validate validator expression
     */
    private void validateValidatorExpression(String validator, String context, ValidationResult result) {
        if (validator.equals("notEmpty()")) {
            return; // Valid
        }
        
        if (validator.startsWith("regex(")) {
            // Extract and validate regex pattern
            String pattern = validator.substring(6, validator.length() - 1);
            // Remove quotes
            pattern = pattern.replaceAll("^['\"]|['\"]$", "");
            try {
                Pattern.compile(pattern);
            } catch (Exception e) {
                result.addError(context, "Invalid regex pattern in validator: " + e.getMessage());
            }
            return;
        }
        
        if (validator.startsWith("range(")) {
            // Validate range format: range(min, max)
            return;
        }
        
        result.addWarning(context, "Unknown validator function: " + validator);
    }

    /**
     * Validate lookup table
     */
    private void validateLookupTable(CodeLookupTable lookup, ValidationResult result) {
        String context = "Lookup: " + (lookup.getId() != null ? lookup.getId() : "unknown");
        
        if (lookup.getId() == null || lookup.getId().isEmpty()) {
            result.addError(context, "Lookup ID is required");
        } else {
            // Validate ID format
            if (!lookup.getId().matches("^[a-zA-Z0-9_-]+$")) {
                result.addError(context, 
                    "Lookup ID must contain only alphanumeric characters, hyphens, and underscores: " + 
                    lookup.getId());
            }
        }
        
        if (lookup.getName() == null || lookup.getName().isEmpty()) {
            result.addWarning(context, "Lookup name is recommended");
        }
        
        if (lookup.getMappings() == null || lookup.getMappings().isEmpty()) {
            result.addError(context, "Lookup must have at least one mapping");
            return;
        }
        
        // Check for duplicate source codes
        Set<String> sourceCodes = new HashSet<>();
        for (CodeMapping mapping : lookup.getMappings()) {
            if (mapping.getSourceCode() == null || mapping.getSourceCode().isEmpty()) {
                result.addError(context, "Source code cannot be null or empty");
            } else {
                if (!sourceCodes.add(mapping.getSourceCode())) {
                    result.addError(context, "Duplicate source code: " + mapping.getSourceCode());
                }
            }
            
            if (mapping.getTargetCode() == null || mapping.getTargetCode().isEmpty()) {
                result.addError(context, "Target code cannot be null or empty for source: " + 
                    mapping.getSourceCode());
            }
        }
        
        // If bidirectional, check for duplicate target codes
        if (lookup.isBidirectional()) {
            Set<String> targetCodes = new HashSet<>();
            for (CodeMapping mapping : lookup.getMappings()) {
                if (mapping.getTargetCode() != null && !targetCodes.add(mapping.getTargetCode())) {
                    result.addError(context, "Bidirectional lookup has duplicate target code: " + 
                        mapping.getTargetCode());
                }
            }
        }
    }

    /**
     * Check if mapping dataType is compatible with FHIR expected type
     */
    private boolean isCompatibleType(String mappingType, String fhirType) {
        // Exact match
        if (mappingType.equals(fhirType)) {
            return true;
        }
        
        // Compatible type mappings
        Map<String, Set<String>> compatibleTypes = new HashMap<>();
        compatibleTypes.put("string", new HashSet<>(Arrays.asList(
            "string", "markdown", "id", "code", "uri", "url", "canonical", "oid", "uuid"
        )));
        compatibleTypes.put("integer", new HashSet<>(Arrays.asList(
            "integer", "unsignedInt", "positiveInt"
        )));
        compatibleTypes.put("decimal", new HashSet<>(Arrays.asList(
            "decimal"
        )));
        compatibleTypes.put("date", new HashSet<>(Arrays.asList(
            "date", "dateTime", "instant"
        )));
        compatibleTypes.put("dateTime", new HashSet<>(Arrays.asList(
            "dateTime", "instant"
        )));
        compatibleTypes.put("boolean", new HashSet<>(Arrays.asList(
            "boolean"
        )));
        compatibleTypes.put("code", new HashSet<>(Arrays.asList(
            "code", "string"
        )));
        
        Set<String> compatible = compatibleTypes.get(mappingType.toLowerCase());
        return compatible != null && compatible.contains(fhirType.toLowerCase());
    }

    /**
     * Initialize valid FHIR data types
     */
    private Set<String> initValidDataTypes() {
        return new HashSet<>(Arrays.asList(
            "string", "integer", "decimal", "boolean", "date", "dateTime", "time",
            "instant", "code", "uri", "url", "canonical", "oid", "uuid", "id",
            "markdown", "base64Binary", "unsignedInt", "positiveInt", "array"
        ));
    }
}