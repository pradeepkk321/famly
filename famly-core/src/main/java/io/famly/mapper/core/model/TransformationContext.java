package io.famly.mapper.core.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Transformation context with global variables and settings
 */
public class TransformationContext {
    private Map<String, Object> variables;
    private String organizationId;
    private String facilityId;
    private String tenantId;
    private Map<String, String> settings;
//    private String mappingId;
//    private String traceId;
//    private List<TransformationTrace> traces = new ArrayList<>();
    private TransformationTrace trace;
    private boolean enableTracing = false;

    public TransformationContext() {
        this.variables = new HashMap<>();
        this.settings = new HashMap<>();
    }

    public Map<String, Object> getVariables() { return variables; }
    public void setVariables(Map<String, Object> variables) { this.variables = variables; }

    public String getOrganizationId() { return organizationId; }
    public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }

    public String getFacilityId() { return facilityId; }
    public void setFacilityId(String facilityId) { this.facilityId = facilityId; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public Map<String, String> getSettings() { return settings; }
    public void setSettings(Map<String, String> settings) { this.settings = settings; }

//    public String getMappingId() { return mappingId; }
//    public void setMappingId(String mappingId) { this.mappingId = mappingId; }
//
//    public String getTraceId() { return traceId; }
//    public void setTraceId(String traceId) { this.traceId = traceId; }

    public boolean isEnableTracing() {
		return enableTracing;
	}

    /**
     * Enable tracing
     * Random traceId will be generated
     */
	public void enableTracing() {
		this.enableTracing = true;
		this.trace = new TransformationTrace(UUID.randomUUID().toString());
	}

	/**
	 * Enable tracing with a traceId
	 * @param traceId
	 */
	public void enableTracing(String traceId) {
		this.enableTracing = true;
		this.trace = new TransformationTrace(traceId);
	}
	
	public TransformationTrace getTrace() {
		return enableTracing ? this.trace : null;
	}

	public void setVariable(String key, Object value) {
        variables.put(key, value);
    }

    public Object getVariable(String key) {
        return variables.get(key);
    }
    
//    public void addTrace(FieldTransformationTrace trace) {
//        if (enableTracing) {
//            traces.add(trace);
//        }
//    }
    
//    public List<TransformationTrace> getTraces() {
//        return traces;
//    }
//    
//    public List<TransformationTrace> getFailedTraces() {
//        return traces.stream()
//            .filter(t -> !t.isSuccess())
//            .collect(Collectors.toList());
//    }
//    
//    public void printTraceReport() {
//        System.out.println("\n=== Transformation Trace Report ===");
//        System.out.println("Total fields: " + traces.size());
//        System.out.println("Successful: " + traces.stream().filter(TransformationTrace::isSuccess).count());
//        System.out.println("Failed: " + getFailedTraces().size());
//        
//        if (!getFailedTraces().isEmpty()) {
//            System.out.println("\nFailures:");
//            for (TransformationTrace trace : getFailedTraces()) {
//                System.out.println("  [" + trace.getFieldId() + "] " + trace.getSourcePath());
//                System.out.println("    Error: " + trace.getErrorMessage());
//                if (trace.getExpression() != null) {
//                    System.out.println("    Expression: " + trace.getExpression());
//                }
//                System.out.println("    Source value: " + trace.getSourceValue());
//            }
//        }
//    }
    
    @Override
    public String toString() {
        return "TransformationContext{" +
            "organizationId='" + organizationId + '\'' +
            ", facilityId='" + facilityId + '\'' +
            ", tenantId='" + tenantId + '\'' +
            ", variablesCount=" + (variables != null ? variables.size() : 0) +
            ", settingsCount=" + (settings != null ? settings.size() : 0) +
            '}';
    }
}
