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
import com.fhir.mapper.model.TransformationTrace;
import com.fhir.mapper.model.FieldTransformationTrace;

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
	public Map<String, Object> jsonToFhirMap(String jsonString, ResourceMapping mapping, TransformationContext context)
			throws Exception {
		initTrace(mapping, context);
		try {
			validateDirection(mapping, MappingDirection.JSON_TO_FHIR);
			Map<String, Object> source = objectMapper.readValue(jsonString, Map.class);
			Map<String, Object> fhirMap = performTransformation(source, mapping, context);

			updateSuccessTrace(context);

			return fhirMap;

		} catch (Exception e) {
			updateFailureTrace(context);
			throw e;
		}
	}

	/**
	 * Transform JSON Map to FHIR Map
	 */
	public Map<String, Object> jsonToFhirMap(Map<String, Object> jsonMap, ResourceMapping mapping,
			TransformationContext context) throws Exception {
		initTrace(mapping, context);
		try {
			validateDirection(mapping, MappingDirection.JSON_TO_FHIR);
			Map<String, Object> fhirMap = performTransformation(jsonMap, mapping, context);

			updateSuccessTrace(context);

			return fhirMap;
		} catch (Exception e) {
			updateFailureTrace(context);
			throw e;
		}
	}

	/**
	 * Transform POJO to FHIR Map
	 */
	public Map<String, Object> jsonToFhirMap(Object pojo, ResourceMapping mapping, TransformationContext context)
			throws Exception {
		initTrace(mapping, context);
		try {
			validateDirection(mapping, MappingDirection.JSON_TO_FHIR);
			Map<String, Object> source = objectMapper.convertValue(pojo, Map.class);
			Map<String, Object> fhirMap = performTransformation(source, mapping, context);

			updateSuccessTrace(context);

			return fhirMap;
		} catch (Exception e) {
			updateFailureTrace(context);
			throw e;
		}
	}

	/**
	 * Transform JSON string to FHIR JSON string
	 */
	public String jsonToFhirJson(String jsonString, ResourceMapping mapping, TransformationContext context)
			throws Exception {
		initTrace(mapping, context);
		try {
			Map<String, Object> fhirMap = jsonToFhirMap(jsonString, mapping, context);
			String fhirJson = objectMapper.writeValueAsString(fhirMap);

			updateSuccessTrace(context);
			return fhirJson;

		} catch (Exception e) {
			updateFailureTrace(context);
			throw e;
		}
	}

	/**
	 * Transform JSON Map to FHIR JSON string
	 */
	public String jsonToFhirJson(Map<String, Object> jsonMap, ResourceMapping mapping, TransformationContext context)
			throws Exception {
		initTrace(mapping, context);
		try {
			Map<String, Object> fhirMap = jsonToFhirMap(jsonMap, mapping, context);
			String jsonString = objectMapper.writeValueAsString(fhirMap);

			updateSuccessTrace(context);

			return jsonString;
		} catch (Exception e) {
			updateFailureTrace(context);
			throw e;
		}
	}

	/**
	 * Transform POJO to FHIR JSON string
	 */
	public String jsonToFhirJson(Object pojo, ResourceMapping mapping, TransformationContext context) throws Exception {
		initTrace(mapping, context);
		try {
			Map<String, Object> fhirMap = jsonToFhirMap(pojo, mapping, context);
			String jsonString = objectMapper.writeValueAsString(fhirMap);

			updateSuccessTrace(context);

			return jsonString;
		} catch (Exception e) {
			updateFailureTrace(context);
			throw e;
		}
	}

	/**
	 * Transform JSON string to HAPI FHIR Resource
	 */
	public <T extends IBaseResource> T jsonToFhirResource(String jsonString, ResourceMapping mapping,
			TransformationContext context, Class<T> resourceClass) throws Exception {
		initTrace(mapping, context);
		try {
			String fhirJson = jsonToFhirJson(jsonString, mapping, context);
			IParser parser = fhirContext.newJsonParser();
			T resource = parser.parseResource(resourceClass, fhirJson);

			updateSuccessTrace(context);

			return resource;
		} catch (Exception e) {
			updateFailureTrace(context);
			throw e;
		}
	}

	/**
	 * Transform JSON Map to HAPI FHIR Resource
	 */
	public <T extends IBaseResource> T jsonToFhirResource(Map<String, Object> jsonMap, ResourceMapping mapping,
			TransformationContext context, Class<T> resourceClass) throws Exception {
		initTrace(mapping, context);
		try {
			String fhirJson = jsonToFhirJson(jsonMap, mapping, context);
			IParser parser = fhirContext.newJsonParser();
			T resource = parser.parseResource(resourceClass, fhirJson);

			updateSuccessTrace(context);

			return resource;
		} catch (Exception e) {
			updateFailureTrace(context);
			throw e;
		}
	}

	/**
	 * Transform POJO to HAPI FHIR Resource
	 */
	public <T extends IBaseResource> T jsonToFhirResource(Object pojo, ResourceMapping mapping,
			TransformationContext context, Class<T> resourceClass) throws Exception {
		initTrace(mapping, context);
		try {
			String fhirJson = jsonToFhirJson(pojo, mapping, context);
			IParser parser = fhirContext.newJsonParser();
			T resource = parser.parseResource(resourceClass, fhirJson);

			updateSuccessTrace(context);

			return resource;
		} catch (Exception e) {
			updateFailureTrace(context);
			throw e;
		}
	}

	// ============================================================================
	// FHIR to JSON Transformations
	// ============================================================================

	/**
	 * Transform FHIR JSON string to JSON Map
	 */
	public Map<String, Object> fhirToJsonMap(String fhirJson, ResourceMapping mapping, TransformationContext context)
			throws Exception {
		initTrace(mapping, context);
		try {
			validateDirection(mapping, MappingDirection.FHIR_TO_JSON);
			Map<String, Object> source = objectMapper.readValue(fhirJson, Map.class);

			Map<String, Object> jsonMap = performTransformation(source, mapping, context);

			updateSuccessTrace(context);

			return jsonMap;
		} catch (Exception e) {
			updateFailureTrace(context);
			throw e;
		}
	}

	/**
	 * Transform FHIR Map to JSON Map
	 */
	public Map<String, Object> fhirToJsonMap(Map<String, Object> fhirMap, ResourceMapping mapping,
			TransformationContext context) throws Exception {
		initTrace(mapping, context);
		try {
			validateDirection(mapping, MappingDirection.FHIR_TO_JSON);
			Map<String, Object> jsonMap = performTransformation(fhirMap, mapping, context);
			updateSuccessTrace(context);

			return jsonMap;
		} catch (Exception e) {
			updateFailureTrace(context);
			throw e;
		}
	}

	/**
	 * Transform HAPI FHIR Resource to JSON Map
	 */
	public Map<String, Object> fhirToJsonMap(IBaseResource resource, ResourceMapping mapping,
			TransformationContext context) throws Exception {
		initTrace(mapping, context);
		try {
			IParser parser = fhirContext.newJsonParser();
			String fhirJson = parser.encodeResourceToString(resource);
			return fhirToJsonMap(fhirJson, mapping, context);
		} catch (Exception e) {
			updateFailureTrace(context);
			throw e;
		}
	}

	/**
	 * Transform FHIR JSON string to JSON string
	 */
	public String fhirToJsonString(String fhirJson, ResourceMapping mapping, TransformationContext context)
			throws Exception {
		initTrace(mapping, context);
		try {
			Map<String, Object> jsonMap = fhirToJsonMap(fhirJson, mapping, context);
			String jsonString = objectMapper.writeValueAsString(jsonMap);

			updateSuccessTrace(context);

			return jsonString;
		} catch (Exception e) {
			updateFailureTrace(context);
			throw e;
		}
	}

	/**
	 * Transform FHIR Map to JSON string
	 */
	public String fhirToJsonString(Map<String, Object> fhirMap, ResourceMapping mapping, TransformationContext context)
			throws Exception {
		initTrace(mapping, context);
		try {
			Map<String, Object> jsonMap = fhirToJsonMap(fhirMap, mapping, context);
			String jsonString = objectMapper.writeValueAsString(jsonMap);

			updateSuccessTrace(context);

			return jsonString;
		} catch (Exception e) {
			updateFailureTrace(context);
			throw e;
		}
	}

	/**
	 * Transform HAPI FHIR Resource to JSON string
	 */
	public String fhirToJsonString(IBaseResource resource, ResourceMapping mapping, TransformationContext context)
			throws Exception {
		initTrace(mapping, context);
		try {
			Map<String, Object> jsonMap = fhirToJsonMap(resource, mapping, context);
			String jsonString = objectMapper.writeValueAsString(jsonMap);

			updateSuccessTrace(context);

			return jsonString;
		} catch (Exception e) {
			updateFailureTrace(context);
			throw e;
		}
	}

	/**
	 * Transform FHIR JSON string to POJO
	 */
	public <T> T fhirToJsonObject(String fhirJson, ResourceMapping mapping, TransformationContext context,
			Class<T> targetClass) throws Exception {
		initTrace(mapping, context);
		try {
			Map<String, Object> jsonMap = fhirToJsonMap(fhirJson, mapping, context);
			T obj = objectMapper.convertValue(jsonMap, targetClass);

			updateSuccessTrace(context);

			return obj;
		} catch (Exception e) {
			updateFailureTrace(context);
			throw e;
		}
	}

	/**
	 * Transform FHIR Map to POJO
	 */
	public <T> T fhirToJsonObject(Map<String, Object> fhirMap, ResourceMapping mapping, TransformationContext context,
			Class<T> targetClass) throws Exception {
		initTrace(mapping, context);
		try {
			Map<String, Object> jsonMap = fhirToJsonMap(fhirMap, mapping, context);
			T obj = objectMapper.convertValue(jsonMap, targetClass);

			updateSuccessTrace(context);

			return obj;
		} catch (Exception e) {
			updateFailureTrace(context);
			throw e;
		}
	}

	/**
	 * Transform HAPI FHIR Resource to POJO
	 */
	public <T> T fhirToJsonObject(IBaseResource resource, ResourceMapping mapping, TransformationContext context,
			Class<T> targetClass) throws Exception {
		initTrace(mapping, context);
		try {
			Map<String, Object> jsonMap = fhirToJsonMap(resource, mapping, context);
			T obj = objectMapper.convertValue(jsonMap, targetClass);

			updateSuccessTrace(context);

			return obj;
		} catch (Exception e) {
			updateFailureTrace(context);
			throw e;
		}
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
					"Invalid mapping direction. Expected " + expectedDirection + " but got " + mapping.getDirection());
		}
	}

	/**
	 * Perform the actual transformation
	 */
	private Map<String, Object> performTransformation(Map<String, Object> source, ResourceMapping mapping,
			TransformationContext context) throws Exception {

		Map<String, Object> target = new LinkedHashMap<>();

//        context.setMappingId(mapping.getId());

		// Set resourceType for FHIR output
		if (mapping.getDirection() == MappingDirection.JSON_TO_FHIR) {
			target.put("resourceType", mapping.getTargetType());
		}

		for (FieldMapping fieldMapping : mapping.getFieldMappings()) {
			try {
				processMapping(source, target, fieldMapping, context);
			} catch (Exception e) {
				if (fieldMapping.isRequired()) {
					throw new TransformationException("Failed to map required field: " + fieldMapping.getId(), e);
				}
			}
		}

		return target;
	}

	/**
	 * Process individual field mapping
	 */
	private void processMapping(Map<String, Object> source, Map<String, Object> target, FieldMapping mapping,
			TransformationContext context) {

		FieldTransformationTrace fieldTransformationTrace = initFieldTransformationTrace(mapping, context);
		boolean enableTracing = context.isEnableTracing();

		try {
			// Check condition with context
			if (mapping.getCondition() != null) {
				boolean conditionResult = evaluateCondition(mapping.getCondition(), source, context);
				if (enableTracing)
					fieldTransformationTrace.setConditionPassed(conditionResult);

				if (!conditionResult && enableTracing) {
					fieldTransformationTrace.setEndTime(System.currentTimeMillis());
//                    context.addTrace(trace);
					context.getTrace().addFieldTransformationTrace(fieldTransformationTrace);
					return;
				}
			}

			String sourcePath = mapping.getSourcePath();
			String targetPath = mapping.getTargetPath();

			Object value = null;

			// Extract value from source if path exists
			if (sourcePath != null) {
				value = pathNavigator.getValue(source, sourcePath);
				if (enableTracing)
					fieldTransformationTrace.setSourceValue(value);
			}

			// Apply default value (supports context variables)
			if (value == null && mapping.getDefaultValue() != null) {
				value = resolveValue(mapping.getDefaultValue(), context);
			}

			// Transform with context
			if (mapping.getTransformExpression() != null) {
				try {
					value = applyTransform(value, mapping.getTransformExpression(), source, context);
				} catch (Exception e) {
					if (enableTracing) {
						fieldTransformationTrace.setErrorMessage("Transform failed: " + e.getMessage());
						fieldTransformationTrace.setEndTime(System.currentTimeMillis());
//                        context.addTrace(fieldTransformationTrace);
						context.getTrace().addFieldTransformationTrace(fieldTransformationTrace);
					}
					if (mapping.isRequired()) {
						throw new TransformationException("Required field transformation failed: " + mapping.getId(),
								e);
					}
					return;
				}
			}

			// Skip if still null and not required
			if (value == null) {
				if (mapping.isRequired()) {
					if (enableTracing) {
						fieldTransformationTrace.setErrorMessage("Required field is null after all transformations");
						fieldTransformationTrace.setEndTime(System.currentTimeMillis());
//                        context.addTrace(fieldTransformationTrace);
						context.getTrace().addFieldTransformationTrace(fieldTransformationTrace);
					}
					throw new TransformationException(
							"Required field missing: " + (sourcePath != null ? sourcePath : mapping.getId()));
				}

				if (enableTracing) {
					fieldTransformationTrace.setErrorMessage("Value is null (non-required)");
					fieldTransformationTrace.setEndTime(System.currentTimeMillis());
//                    context.addTrace(fieldTransformationTrace);
					context.getTrace().addFieldTransformationTrace(fieldTransformationTrace);
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

			if (enableTracing) {
				fieldTransformationTrace.setResultValue(value);
				fieldTransformationTrace.setEndTime(System.currentTimeMillis());
//                context.addTrace(fieldTransformationTrace);
				context.getTrace().addFieldTransformationTrace(fieldTransformationTrace);
			}

		} catch (Exception e) {
			if (enableTracing) {
				fieldTransformationTrace.setErrorMessage(e.getMessage());
				fieldTransformationTrace.setEndTime(System.currentTimeMillis());
//                context.addTrace(fieldTransformationTrace);
				context.getTrace().addFieldTransformationTrace(fieldTransformationTrace);
			}
			throw e;
		}
	}

	/**
	 * Convert value to specified FHIR data type
	 */
	private Object convertToType(Object value, String dataType) {
		if (value == null)
			return null;

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
			if (endIdx == -1)
				endIdx = path.lastIndexOf("\"]");
			String key = path.substring(10, endIdx);
			return context.getSettings().get(key);
		}

		// Handle direct properties
		switch (path) {
		case "organizationId":
			return context.getOrganizationId();
		case "facilityId":
			return context.getFacilityId();
		case "tenantId":
			return context.getTenantId();
		default:
			return context.getVariable(path);
		}
	}

	/**
	 * Apply code lookup (supports bidirectional)
	 */
	private Object applyLookupOld(Object value, String lookupTableId) {
		if (value == null)
			return null;

		CodeLookupTable lookupTable = mappingRegistry.getLookupTable(lookupTableId);
		if (lookupTable == null) {
			throw new TransformationException("Lookup table not found: " + lookupTableId);
		}

		String code = value.toString();
		String result = lookupTable.lookupTarget(code);

		if (result == null) {
			throw new TransformationException("No mapping found for code '" + code + "' in lookup: " + lookupTableId);
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
		if (value == null)
			return null;

		CodeLookupTable lookupTable = mappingRegistry.getLookupTable(lookupTableId);
		if (lookupTable == null) {
			throw new TransformationException("Lookup table not found: " + lookupTableId);
		}

		String code = value.toString();

		// Use new method that returns system info
		CodeMappingResult result = lookupTable.lookupTargetWithSystem(code);

		if (result == null) {
			throw new TransformationException("No mapping found for code '" + code + "' in lookup: " + lookupTableId);
		}
		return result;
	}

	/**
	 * Evaluate JEXL condition with context
	 */
	private boolean evaluateCondition(String condition, Map<String, Object> source, TransformationContext context) {
		try {
			return expressionEvaluator.evaluateCondition(condition, source, context);
		} catch (ExpressionEvaluationException e) {
			throw new TransformationException("Condition evaluation failed: " + condition, e);
		}
	}

	/**
	 * Apply JEXL transformation with context
	 */
	private Object applyTransform(Object value, String expression, Map<String, Object> source,
			TransformationContext context) {
		try {
			return expressionEvaluator.evaluate(expression, value, source, context);
		} catch (ExpressionEvaluationException e) {
			throw new TransformationException("Transform failed: " + expression, e);
		}
	}

	/**
	 * Replace $ctx variables in expressions - NO LONGER NEEDED Context is now
	 * available in JEXL as 'ctx' object
	 */
	@Deprecated
	private String resolveContextInExpression(String expression, TransformationContext context) {
		// No longer needed - context available directly in expressions
		return expression;
	}

	private void initTrace(ResourceMapping mapping, TransformationContext context) {
		if (context.isEnableTracing()) {
			TransformationTrace trace = context.getTrace();
			trace.setMappingId(mapping.getId());
			trace.setSource(mapping.getSourceType());
			trace.setTarget(mapping.getTargetType());
			trace.setStartTime(System.currentTimeMillis());
		}
	}

	private void updateFailureTrace(TransformationContext context) {
		if (context.isEnableTracing()) {
			TransformationTrace trace = context.getTrace();
			trace.setEndTime(System.currentTimeMillis());
			trace.setSuccess(false);
		}
	}

	private void updateSuccessTrace(TransformationContext context) {
		if (context.isEnableTracing()) {
			TransformationTrace trace = context.getTrace();
			trace.setEndTime(System.currentTimeMillis());
			trace.setSuccess(true);
		}
	}

	private FieldTransformationTrace initFieldTransformationTrace(FieldMapping mapping, TransformationContext context) {
		if (context.isEnableTracing()) {
			FieldTransformationTrace trace = new FieldTransformationTrace();
			trace.setFieldId(mapping.getId());
			trace.setSourcePath(mapping.getSourcePath());
			trace.setTargetPath(mapping.getTargetPath());
			trace.setExpression(mapping.getTransformExpression());
			trace.setCondition(mapping.getCondition());
			trace.setStartTime(System.currentTimeMillis());
			return trace;
		}
		return null;
	}
}