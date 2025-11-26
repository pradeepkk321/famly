package com.fhir.mapper.model;

/**
 * Captures transformation details for a single field mapping.
 * Tracks source values, transformations, conditions, results, and errors.
 * 
 * Collected automatically when tracing is enabled via TransformationContext.
 * 
 * @see TransformationTrace
 */
public class FieldTransformationTrace {
	
    /** Field mapping ID */
    private String fieldId;
    
    /** Source path expression (e.g., "patient.firstName", "addresses[0].city") */
    private String sourcePath;
    
    /** Target path expression (e.g., "name[0].given[0]", "address[0].city") */
    private String targetPath;
    
    /** Value extracted from source, null if not found or sourcePath is null */
    private Object sourceValue;
    
    /** Final value set in target after all transformations, null if skipped */
    private Object resultValue;
    
    /** JEXL transformation expression applied, null if none */
    private String expression;
    
    /** JEXL condition expression evaluated, null if none */
    private String condition;
    
    /** Whether condition evaluated to true (only meaningful if condition != null) */
    private boolean conditionPassed;
    
    /** Error message if field transformation failed, null on success */
    private String errorMessage;
    
    /** Timestamp when field processing started (milliseconds since epoch) */
    private long startTime;
    
    /** Timestamp when field processing completed (milliseconds since epoch) */
    private long endTime;;

	public String getFieldId() {
		return fieldId;
	}

	public void setFieldId(String fieldId) {
		this.fieldId = fieldId;
	}

	public String getSourcePath() {
		return sourcePath;
	}

	public void setSourcePath(String sourcePath) {
		this.sourcePath = sourcePath;
	}

	public String getTargetPath() {
		return targetPath;
	}

	public void setTargetPath(String targetPath) {
		this.targetPath = targetPath;
	}

	public Object getSourceValue() {
		return sourceValue;
	}

	public void setSourceValue(Object sourceValue) {
		this.sourceValue = sourceValue;
	}

	public Object getResultValue() {
		return resultValue;
	}

	public void setResultValue(Object resultValue) {
		this.resultValue = resultValue;
	}

	public String getExpression() {
		return expression;
	}

	public void setExpression(String expression) {
		this.expression = expression;
	}

	public String getCondition() {
		return condition;
	}

	public void setCondition(String condition) {
		this.condition = condition;
	}

	public boolean isConditionPassed() {
		return conditionPassed;
	}

	public void setConditionPassed(boolean conditionPassed) {
		this.conditionPassed = conditionPassed;
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

    /**
     * Check if field transformation was successful.
     * @return true if no error occurred
     */
	public boolean isSuccess() {
        return errorMessage == null;
    }
    
    /**
     * Get field transformation duration in milliseconds.
     * @return Duration or 0 if not completed
     */
    public long getDuration() {
        return endTime - startTime;
    }
    
    @Override
    public String toString() {
        return "{"
                + "\"fieldId\":\"" + escape(fieldId) + "\","
                + "\"sourcePath\":\"" + escape(sourcePath) + "\","
                + "\"targetPath\":\"" + escape(targetPath) + "\","
                + "\"sourceValue\":\"" + escape(String.valueOf(sourceValue)) + "\","
                + "\"resultValue\":\"" + escape(String.valueOf(resultValue)) + "\","
                + "\"expression\":\"" + escape(expression) + "\","
                + "\"condition\":\"" + escape(condition) + "\","
                + "\"conditionPassed\":" + conditionPassed + ","
                + "\"errorMessage\":\"" + escape(errorMessage) + "\","
                + "\"startTime\":" + startTime + ","
                + "\"endTime\":" + endTime + ","
                + "\"duration\":\"" + getDuration() + " millis\""
                + "}";
    }

    private String escape(String v) {
        if (v == null) return null;
        return v.replace("\"", "\\\"");
    }

}
