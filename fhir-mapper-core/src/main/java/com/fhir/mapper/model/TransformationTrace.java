package com.fhir.mapper.model;

public class TransformationTrace {
    private String mappingId;
    private String fieldId;
    private String sourcePath;
    private String targetPath;
    private Object sourceValue;
    private Object resultValue;
    private String expression;
    private String condition;
    private boolean conditionPassed;
    private String errorMessage;
    private long startTime;
    private long endTime;
    
    public String getMappingId() {
		return mappingId;
	}

	public void setMappingId(String mappingId) {
		this.mappingId = mappingId;
	}

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

	public boolean isSuccess() {
        return errorMessage == null;
    }
    
    public long getDuration() {
        return endTime - startTime;
    }
    
    @Override
    public String toString() {
        return "{"
                + "\"mappingId\":\"" + escape(mappingId) + "\","
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
                + "\"duration\":" + getDuration()
                + "}";
    }

    private String escape(String v) {
        if (v == null) return null;
        return v.replace("\"", "\\\"");
    }

}
