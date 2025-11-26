package com.fhir.mapper.model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TransformationTrace {

	private String traceId;
	private String source;
	private String target;
	private String mappingId;
	private boolean success;
	private String errorMessage;
	private long startTime;
	private long endTime;
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

	public List<FieldTransformationTrace> failedFieldTransformationTraces() {
		return fieldTransformationTraces.stream().filter(t -> !t.isSuccess()).collect(Collectors.toList());
	}
    
    public long getDuration() {
        return endTime - startTime;
    }

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
