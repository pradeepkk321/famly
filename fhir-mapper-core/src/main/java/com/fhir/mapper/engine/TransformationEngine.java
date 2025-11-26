package com.fhir.mapper.engine;

import java.util.LinkedHashMap;
import java.util.Map;

import org.hl7.fhir.instance.model.api.IBaseResource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fhir.mapper.expression.ExpressionEvaluationException;
import com.fhir.mapper.expression.MappingExpressionEvaluator;
import com.fhir.mapper.model.CodeLookupTable;
import com.fhir.mapper.model.CodeMappingResult;
import com.fhir.mapper.model.FieldMapping;
import com.fhir.mapper.model.MappingDirection;
import com.fhir.mapper.model.MappingRegistry;
import com.fhir.mapper.model.ResourceMapping;
import com.fhir.mapper.model.TransformationContext;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

/**
 * Core transformation engine for JSON <-> FHIR conversion
 */
public class TransformationEngine {
    private final MappingExpressionEvaluator expressionEvaluator;
    private final ObjectMapper objectMapper;
    private final PathNavigator pathNavigator;
    private final ValidationEngine validationEngine;
    private final MappingRegistry mappingRegistry;
    private final FhirContext fhirContext;

    public TransformationEngine(MappingRegistry mappingRegistry) {
        this(mappingRegistry, FhirContext.forR4());
    }

    public TransformationEngine(MappingRegistry mappingRegistry, FhirContext fhirContext) {
        this.expressionEvaluator = new MappingExpressionEvaluator();
        this.objectMapper = new ObjectMapper();
        this.pathNavigator = new PathNavigator();
        this.validationEngine = new ValidationEngine();
        this.mappingRegistry = mappingRegistry;
        this.fhirContext = fhirContext;
    }

    // ============================================================================
    // JSON to FHIR Transformations
    // ============================================================================

    /**
     * Transform JSON string to FHIR Map
     */
    public Map<String, Object> jsonToFhirMap(String jsonString, ResourceMapping mapping,
                                             TransformationContext context) throws Exception {
        validateDirection(mapping, MappingDirection.JSON_TO_FHIR);
        Map<String, Object> source = objectMapper.readValue(jsonString, Map.class);
        return performTransformation(source, mapping, context);
    }

    /**
     * Transform JSON Map to FHIR Map
     */
    public Map<String, Object> jsonToFhirMap(Map<String, Object> jsonMap, ResourceMapping mapping,
                                             TransformationContext context) throws Exception {
        validateDirection(mapping, MappingDirection.JSON_TO_FHIR);
        return performTransformation(jsonMap, mapping, context);
    }

    /**
     * Transform POJO to FHIR Map
     */
    public Map<String, Object> jsonToFhirMap(Object pojo, ResourceMapping mapping,
                                             TransformationContext context) throws Exception {
        validateDirection(mapping, MappingDirection.JSON_TO_FHIR);
        Map<String, Object> source = objectMapper.convertValue(pojo, Map.class);
        return performTransformation(source, mapping, context);
    }

    /**
     * Transform JSON string to FHIR JSON string
     */
    public String jsonToFhirJson(String jsonString, ResourceMapping mapping,
                                 TransformationContext context) throws Exception {
        Map<String, Object> fhirMap = jsonToFhirMap(jsonString, mapping, context);
        return objectMapper.writeValueAsString(fhirMap);
    }

    /**
     * Transform JSON Map to FHIR JSON string
     */
    public String jsonToFhirJson(Map<String, Object> jsonMap, ResourceMapping mapping,
                                 TransformationContext context) throws Exception {
        Map<String, Object> fhirMap = jsonToFhirMap(jsonMap, mapping, context);
        return objectMapper.writeValueAsString(fhirMap);
    }

    /**
     * Transform POJO to FHIR JSON string
     */
    public String jsonToFhirJson(Object pojo, ResourceMapping mapping,
                                 TransformationContext context) throws Exception {
        Map<String, Object> fhirMap = jsonToFhirMap(pojo, mapping, context);
        return objectMapper.writeValueAsString(fhirMap);
    }

    /**
     * Transform JSON string to HAPI FHIR Resource
     */
    public <T extends IBaseResource> T jsonToFhirResource(String jsonString, ResourceMapping mapping,
                                                          TransformationContext context,
                                                          Class<T> resourceClass) throws Exception {
        String fhirJson = jsonToFhirJson(jsonString, mapping, context);
        IParser parser = fhirContext.newJsonParser();
        return parser.parseResource(resourceClass, fhirJson);
    }

    /**
     * Transform JSON Map to HAPI FHIR Resource
     */
    public <T extends IBaseResource> T jsonToFhirResource(Map<String, Object> jsonMap, 
                                                          ResourceMapping mapping,
                                                          TransformationContext context,
                                                          Class<T> resourceClass) throws Exception {
        String fhirJson = jsonToFhirJson(jsonMap, mapping, context);
        IParser parser = fhirContext.newJsonParser();
        return parser.parseResource(resourceClass, fhirJson);
    }

    /**
     * Transform POJO to HAPI FHIR Resource
     */
    public <T extends IBaseResource> T jsonToFhirResource(Object pojo, ResourceMapping mapping,
                                                          TransformationContext context,
                                                          Class<T> resourceClass) throws Exception {
        String fhirJson = jsonToFhirJson(pojo, mapping, context);
        IParser parser = fhirContext.newJsonParser();
        return parser.parseResource(resourceClass, fhirJson);
    }

    // ============================================================================
    // FHIR to JSON Transformations
    // ============================================================================

    /**
     * Transform FHIR JSON string to JSON Map
     */
    public Map<String, Object> fhirToJsonMap(String fhirJson, ResourceMapping mapping,
                                             TransformationContext context) throws Exception {
        validateDirection(mapping, MappingDirection.FHIR_TO_JSON);
        Map<String, Object> source = objectMapper.readValue(fhirJson, Map.class);
        return performTransformation(source, mapping, context);
    }

    /**
     * Transform FHIR Map to JSON Map
     */
    public Map<String, Object> fhirToJsonMap(Map<String, Object> fhirMap, ResourceMapping mapping,
                                             TransformationContext context) throws Exception {
        validateDirection(mapping, MappingDirection.FHIR_TO_JSON);
        return performTransformation(fhirMap, mapping, context);
    }

    /**
     * Transform HAPI FHIR Resource to JSON Map
     */
    public Map<String, Object> fhirToJsonMap(IBaseResource resource, ResourceMapping mapping,
                                             TransformationContext context) throws Exception {
        IParser parser = fhirContext.newJsonParser();
        String fhirJson = parser.encodeResourceToString(resource);
        return fhirToJsonMap(fhirJson, mapping, context);
    }

    /**
     * Transform FHIR JSON string to JSON string
     */
    public String fhirToJsonString(String fhirJson, ResourceMapping mapping,
                                   TransformationContext context) throws Exception {
        Map<String, Object> jsonMap = fhirToJsonMap(fhirJson, mapping, context);
        return objectMapper.writeValueAsString(jsonMap);
    }

    /**
     * Transform FHIR Map to JSON string
     */
    public String fhirToJsonString(Map<String, Object> fhirMap, ResourceMapping mapping,
                                   TransformationContext context) throws Exception {
        Map<String, Object> jsonMap = fhirToJsonMap(fhirMap, mapping, context);
        return objectMapper.writeValueAsString(jsonMap);
    }

    /**
     * Transform HAPI FHIR Resource to JSON string
     */
    public String fhirToJsonString(IBaseResource resource, ResourceMapping mapping,
                                   TransformationContext context) throws Exception {
        Map<String, Object> jsonMap = fhirToJsonMap(resource, mapping, context);
        return objectMapper.writeValueAsString(jsonMap);
    }

    /**
     * Transform FHIR JSON string to POJO
     */
    public <T> T fhirToJsonObject(String fhirJson, ResourceMapping mapping,
                                  TransformationContext context, Class<T> targetClass) throws Exception {
        Map<String, Object> jsonMap = fhirToJsonMap(fhirJson, mapping, context);
        return objectMapper.convertValue(jsonMap, targetClass);
    }

    /**
     * Transform FHIR Map to POJO
     */
    public <T> T fhirToJsonObject(Map<String, Object> fhirMap, ResourceMapping mapping,
                                  TransformationContext context, Class<T> targetClass) throws Exception {
        Map<String, Object> jsonMap = fhirToJsonMap(fhirMap, mapping, context);
        return objectMapper.convertValue(jsonMap, targetClass);
    }

    /**
     * Transform HAPI FHIR Resource to POJO
     */
    public <T> T fhirToJsonObject(IBaseResource resource, ResourceMapping mapping,
                                  TransformationContext context, Class<T> targetClass) throws Exception {
        Map<String, Object> jsonMap = fhirToJsonMap(resource, mapping, context);
        return objectMapper.convertValue(jsonMap, targetClass);
    }

    // ============================================================================
    // Internal Methods
    // ============================================================================

    /**
     * Validate mapping direction
     */
    private void validateDirection(ResourceMapping mapping, MappingDirection expectedDirection) {
        if (mapping.getDirection() != expectedDirection) {
            throw new TransformationException(
                "Invalid mapping direction. Expected " + expectedDirection + 
                " but got " + mapping.getDirection());
        }
    }

    /**
     * Perform the actual transformation
     */
    private Map<String, Object> performTransformation(Map<String, Object> source,
                                                      ResourceMapping mapping,
                                                      TransformationContext context) throws Exception {
        Map<String, Object> target = new LinkedHashMap<>();
        
        // Set resourceType for FHIR output
        if (mapping.getDirection() == MappingDirection.JSON_TO_FHIR) {
            target.put("resourceType", mapping.getTargetType());
        }

        for (FieldMapping fieldMapping : mapping.getFieldMappings()) {
            try {
                processMapping(source, target, fieldMapping, context);
            } catch (Exception e) {
                if (fieldMapping.isRequired()) {
                    throw new TransformationException(
                        "Failed to map required field: " + fieldMapping.getId(), e);
                }
            }
        }

        return target;
    }

    /**
     * Process individual field mapping
     */
    private void processMapping(Map<String, Object> source, Map<String, Object> target, 
                                FieldMapping mapping, TransformationContext context) {
        // Check condition with context
        if (mapping.getCondition() != null && 
            !evaluateCondition(mapping.getCondition(), source, context)) {
            return;
        }

        String sourcePath = mapping.getSourcePath();
        String targetPath = mapping.getTargetPath();

        Object value = null;

        // Extract value from source if path exists
        if (sourcePath != null) {
            value = pathNavigator.getValue(source, sourcePath);
        }

        // Apply default value (supports context variables)
        if (value == null && mapping.getDefaultValue() != null) {
            value = resolveValue(mapping.getDefaultValue(), context);
        }
        
        // Transform with context
        if (mapping.getTransformExpression() != null) {
            value = applyTransform(value, mapping.getTransformExpression(), source, context);
        }

        // Skip if still null and not required
        if (value == null) {
            if (mapping.isRequired()) {
                throw new TransformationException("Required field missing: " + 
                    (sourcePath != null ? sourcePath : mapping.getId()));
            }
            return;
        }

        // Apply lookup if specified
        if (mapping.getLookupTable() != null) {
            value = applyLookupAndGetCode(value, mapping.getLookupTable());
        }

        // Convert to proper type based on dataType
        if (mapping.getDataType() != null) {
            value = convertToType(value, mapping.getDataType());
        }

        // Validate
        if (mapping.getValidator() != null) {
            validationEngine.validate(value, mapping.getValidator(), mapping.getId());
        }

        // Set value in target
        pathNavigator.setValue(target, targetPath, value);
    }

    /**
     * Convert value to specified FHIR data type
     */
    private Object convertToType(Object value, String dataType) {
        if (value == null) return null;
        
        String strValue = value.toString();
        
        try {
            switch (dataType.toLowerCase()) {
                case "boolean":
                    return Boolean.parseBoolean(strValue);
                case "integer":
                case "unsignedint":
                case "positiveint":
                    return Integer.parseInt(strValue);
                case "decimal":
                    return Double.parseDouble(strValue);
                default:
                    // For string, code, uri, date, etc. keep as string
                    return value;
            }
        } catch (Exception e) {
            // If conversion fails, return original value
            return value;
        }
    }

    /**
     * Resolve value that may contain context variables
     */
    private Object resolveValue(String value, TransformationContext context) {
        if (value == null || !value.startsWith("$ctx.")) {
            return value;
        }

        String path = value.substring(5); // Remove "$ctx."
        
        // Handle settings map
        if (path.startsWith("settings['") || path.startsWith("settings[\"")) {
            int endIdx = path.lastIndexOf("']");
            if (endIdx == -1) endIdx = path.lastIndexOf("\"]");
            String key = path.substring(10, endIdx);
            return context.getSettings().get(key);
        }

        // Handle direct properties
        switch (path) {
            case "organizationId": return context.getOrganizationId();
            case "facilityId": return context.getFacilityId();
            case "tenantId": return context.getTenantId();
            default: return context.getVariable(path);
        }
    }

    /**
     * Apply code lookup (supports bidirectional)
     */
    private Object applyLookupOld(Object value, String lookupTableId) {
        if (value == null) return null;

        CodeLookupTable lookupTable = mappingRegistry.getLookupTable(lookupTableId);
        if (lookupTable == null) {
            throw new TransformationException("Lookup table not found: " + lookupTableId);
        }

        String code = value.toString();
        String result = lookupTable.lookupTarget(code);
        
        if (result == null) {
            throw new TransformationException(
                "No mapping found for code '" + code + "' in lookup: " + lookupTableId);
        }
        
        return result;
    }
    
    private Object applyLookupAndGetCode(Object value, String lookupTableId) {
        CodeMappingResult result = applyLookup(value, lookupTableId);
        return result.getCode();
    }
    
    private Object applyLookupAndGetSystem(Object value, String lookupTableId) {
        CodeMappingResult result = applyLookup(value, lookupTableId);
        return result.getSystem();
    }
    
    private CodeMappingResult applyLookup(Object value, String lookupTableId) {
        if (value == null) return null;
        
        CodeLookupTable lookupTable = mappingRegistry.getLookupTable(lookupTableId);
        if (lookupTable == null) {
            throw new TransformationException("Lookup table not found: " + lookupTableId);
        }
        
        String code = value.toString();
        
        // Use new method that returns system info
        CodeMappingResult result = lookupTable.lookupTargetWithSystem(code);
        
        if (result == null) {
            throw new TransformationException(
                "No mapping found for code '" + code + "' in lookup: " + lookupTableId);
        }
        return result;
    }

    /**
     * Evaluate JEXL condition with context
     */
    private boolean evaluateCondition(String condition, Map<String, Object> source,
                                      TransformationContext context) {
        try {
            return expressionEvaluator.evaluateCondition(condition, source, context);
        } catch (ExpressionEvaluationException e) {
            throw new TransformationException("Condition evaluation failed: " + condition, e);
        }
    }

    /**
     * Apply JEXL transformation with context
     */
    private Object applyTransform(Object value, String expression, 
                                  Map<String, Object> source, TransformationContext context) {
        try {
            return expressionEvaluator.evaluate(expression, value, source, context);
        } catch (ExpressionEvaluationException e) {
            throw new TransformationException("Transform failed: " + expression, e);
        }
    }

    /**
     * Replace $ctx variables in expressions - NO LONGER NEEDED
     * Context is now available in JEXL as 'ctx' object
     */
    @Deprecated
    private String resolveContextInExpression(String expression, TransformationContext context) {
        // No longer needed - context available directly in expressions
        return expression;
    }
}