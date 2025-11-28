package io.famly.mapper.core.model;

import java.util.List;
import java.util.Map;

/**
 * Represents a single field mapping between JSON and FHIR
 */
public class FieldMapping {
    private String id;
    private String sourcePath;        // Optional - can be null if only defaultValue used
    private String targetPath;        // FHIR path (e.g., "Patient.name[0].given[0]")
    private String dataType;          // string, integer, date, boolean, code
    private String transformExpression; // JEXL expression (e.g., "uppercase()")
    private String condition;         // JEXL condition (e.g., "patient.type == 'adult'")
    private String validator;         // Validation rule (e.g., "notEmpty()", "regex('^\\d{9}$')")
    private boolean required;
    private String defaultValue;      // Static default or constant value
    private String lookupTable;       // Reference to code mapping table
    private String lookupSourceField; // Field to use for lookup (if different from sourcePath)
    private String description;

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSourcePath() { return sourcePath; }
    public void setSourcePath(String sourcePath) { this.sourcePath = sourcePath; }

    public String getTargetPath() { return targetPath; }
    public void setTargetPath(String targetPath) { this.targetPath = targetPath; }

    public String getDataType() { return dataType; }
    public void setDataType(String dataType) { this.dataType = dataType; }

    public String getTransformExpression() { return transformExpression; }
    public void setTransformExpression(String transformExpression) { 
        this.transformExpression = transformExpression; 
    }

    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }

    public String getValidator() { return validator; }
    public void setValidator(String validator) { this.validator = validator; }

    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }

    public String getDefaultValue() { return defaultValue; }
    public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }

    public String getLookupTable() { return lookupTable; }
    public void setLookupTable(String lookupTable) { this.lookupTable = lookupTable; }

    public String getLookupSourceField() { return lookupSourceField; }
    public void setLookupSourceField(String lookupSourceField) { 
        this.lookupSourceField = lookupSourceField; 
    }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    @Override
    public String toString() {
        return "FieldMapping{" +
            "id='" + id + '\'' +
            ", sourcePath='" + sourcePath + '\'' +
            ", targetPath='" + targetPath + '\'' +
            ", dataType='" + dataType + '\'' +
            ", transformExpression='" + transformExpression + '\'' +
            ", condition='" + condition + '\'' +
            ", validator='" + validator + '\'' +
            ", required=" + required +
            ", defaultValue='" + defaultValue + '\'' +
            ", lookupTable='" + lookupTable + '\'' +
            ", lookupSourceField='" + lookupSourceField + '\'' +
            ", description='" + description + '\'' +
            '}';
    }
}