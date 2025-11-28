package io.famly.mapper.core.model;

import java.util.List;

/**
 * Container for all mappings for a specific resource type
 * Each direction should have its own mapping file
 */
public class ResourceMapping {
    private String id;
    private String name;
    private String sourceType;        // e.g., "PatientDTO" or "Patient"
    private String targetType;        // e.g., "Patient" or "PatientDTO"
    private String version;           // Mapping version
    private MappingDirection direction; // Direction of transformation
    private List<FieldMapping> fieldMappings;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { 
        this.targetType = targetType; 
    }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public MappingDirection getDirection() { return direction; }
    public void setDirection(MappingDirection direction) { this.direction = direction; }

    public List<FieldMapping> getFieldMappings() { return fieldMappings; }
    public void setFieldMappings(List<FieldMapping> fieldMappings) { 
        this.fieldMappings = fieldMappings; 
    }
    
    @Override
    public String toString() {
        return "ResourceMapping{" +
            "id='" + id + '\'' +
            ", name='" + name + '\'' +
            ", sourceType='" + sourceType + '\'' +
            ", targetType='" + targetType + '\'' +
            ", version='" + version + '\'' +
            ", direction=" + direction +
            ", fieldMappingsCount=" + (fieldMappings != null ? fieldMappings.size() : 0) +
            '}';
    }
}

