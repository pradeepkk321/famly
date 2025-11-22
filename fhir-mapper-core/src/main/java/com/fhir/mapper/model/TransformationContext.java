package com.fhir.mapper.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Transformation context with global variables and settings
 */
public class TransformationContext {
    private Map<String, Object> variables;
    private String organizationId;
    private String facilityId;
    private String tenantId;
    private Map<String, String> settings;

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

    public void setVariable(String key, Object value) {
        variables.put(key, value);
    }

    public Object getVariable(String key) {
        return variables.get(key);
    }
    
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
