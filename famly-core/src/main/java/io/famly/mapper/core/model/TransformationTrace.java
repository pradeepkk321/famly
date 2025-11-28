package io.famly.mapper.core.model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Captures complete transformation execution details for a single mapping operation.
 * Provides field-level tracing, error tracking, and performance metrics.
 * 
 * Usage:
 * <pre>
 * TransformationContext context = new TransformationContext();
 * context.enableTracing("trace-123");
 * 
 * Patient patient = engine.jsonToFhirResource(json, mapping, context, Patient.class);
 * TransformationTrace trace = context.getTrace();
 * 
 * System.out.println("Success: " + trace.isSuccess());
 * System.out.println("Duration: " + trace.getDuration() + "ms");
 * trace.printTraceReport();
 * </pre>
 * 
 * @see FieldTransformationTrace
 * @see TransformationContext#enableTracing()
 */
public class TransformationTrace {

    /** Unique identifier for this trace (UUID or custom) */
    private String traceId;
    
    /** Source type name (e.g., "PatientDTO", "Patient") */
    private String source;
    
    /** Target type name (e.g., "Patient", "PatientDTO") */
    private String target;
    
    /** Mapping ID used for transformation */
    private String mappingId;
    
    /** Whether transformation completed successfully */
    private boolean success;
    
    /** Error message if transformation failed, null otherwise */
    private String errorMessage;
    
    /** Timestamp when transformation started (milliseconds since epoch) */
    private long startTime;
    
    /** Timestamp when transformation completed (milliseconds since epoch) */
    private long endTime;
    
    /** Field-level transformation traces, one per field mapping */
	private List<FieldTransformationTrace> fieldTransformationTraces = new ArrayList<>();

	public TransformationTrace(String traceId) {
		this.traceId = traceId;
	}

	public String getTraceId() {
		return traceId;
	}

	public void setTraceId(String traceId) {
		this.traceId = traceId;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getTarget() {
		return target;
	}

	public void setTarget(String target) {
		this.target = target;
	}

	public String getMappingId() {
		return mappingId;
	}

	public void setMappingId(String mappingId) {
		this.mappingId = mappingId;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public long getStartTime() {
		return startTime;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	public long getEndTime() {
		return endTime;
	}

	public void setEndTime(long endTime) {
		this.endTime = endTime;
	}

	public List<FieldTransformationTrace> getFieldTransformationTraces() {
		return fieldTransformationTraces;
	}

	public void setFieldTransformationTraces(List<FieldTransformationTrace> traces) {
		this.fieldTransformationTraces = traces;
	}

	public void addFieldTransformationTrace(FieldTransformationTrace trace) {
		fieldTransformationTraces.add(trace);
	}

    /**
     * Get list of failed field transformations.
     * @return List of fields with error messages
     */
	public List<FieldTransformationTrace> failedFieldTransformationTraces() {
		return fieldTransformationTraces.stream().filter(t -> !t.isSuccess()).collect(Collectors.toList());
	}
    
    /**
     * Get transformation duration in milliseconds.
     * @return Duration or 0 if not completed
     */
    public long getDuration() {
        return endTime - startTime;
    }

    /**
     * Print human-readable trace report to console.
     * Includes summary statistics and detailed failure information.
     */
	public void printTraceReport() {
		System.out.println("\n=== Transformation Trace Report ===");
		System.out.println("Trace ID: " + traceId);
		System.out.println("Mapping ID: " + mappingId);
		System.out.println("Is success: " + success);
		if(!success)
			System.out.println("Error: " + errorMessage);
		System.out.println("Duration: " + getDuration() + "ms");
		System.out.println("Total fields: " + fieldTransformationTraces.size());
		System.out.println("Successful: " + fieldTransformationTraces.stream().filter(FieldTransformationTrace::isSuccess).count());
		System.out.println("Failed: " + failedFieldTransformationTraces().size());

		if (!failedFieldTransformationTraces().isEmpty()) {
			System.out.println("\nFailures:");
			for (FieldTransformationTrace trace : failedFieldTransformationTraces()) {
				System.out.println("  [" + trace.getFieldId() + "] " + trace.getSourcePath());
				System.out.println("    Error: " + trace.getErrorMessage());
				if (trace.getExpression() != null) {
					System.out.println("    Expression: " + trace.getExpression());
				}
				System.out.println("    Source value: " + trace.getSourceValue());
			}
		}
	}
	
    /**
     * Returns JSON representation of complete trace for export/analysis.
     * @return JSON string with trace data
     */
	@Override
	public String toString() {
	    return "{"
	            + "\"traceId\":\"" + escape(traceId) + "\","
	            + "\"source\":\"" + escape(source) + "\","
	            + "\"target\":\"" + escape(target) + "\","
	            + "\"mappingId\":\"" + escape(mappingId) + "\","
	            + "\"success\":" + success + ","
	            + "\"errorMessage\":\"" + escape(errorMessage) + "\","
	            + "\"startTime\":" + startTime + ","
	            + "\"endTime\":" + endTime + ","
	            + "\"duration\":\"" + getDuration() + " millis\","
	            + "\"fieldTransformationTraces\":" + listToJson(fieldTransformationTraces)
	            + "}";
	}

	private String escape(String v) {
	    if (v == null) return null;
	    return v.replace("\"", "\\\"");
	}

	private String listToJson(List<?> list) {
	    if (list == null) return "[]";
	    StringBuilder sb = new StringBuilder("[");
	    for (int i = 0; i < list.size(); i++) {
	        sb.append(list.get(i).toString()); // each element already JSON
	        if (i < list.size() - 1) sb.append(",");
	    }
	    sb.append("]");
	    return sb.toString();
	}


}
