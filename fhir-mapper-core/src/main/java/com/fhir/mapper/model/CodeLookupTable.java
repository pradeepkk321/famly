package com.fhir.mapper.model;

import java.util.List;

/**
 * Code mapping lookup table for value translations
 * Supports bidirectional lookups automatically
 */
public class CodeLookupTable {
    private String id;
    private String name;
    private String sourceSystem;      // Source coding system
    private String targetSystem;      // Target FHIR coding system
    private boolean bidirectional;    // If true, allows reverse lookups
    private String defaultSourceCode; // Fallback for reverse lookup
    private String defaultTargetCode; // Fallback for forward lookup
    private List<CodeMapping> mappings;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSourceSystem() { return sourceSystem; }
    public void setSourceSystem(String sourceSystem) { this.sourceSystem = sourceSystem; }

    public String getTargetSystem() { return targetSystem; }
    public void setTargetSystem(String targetSystem) { this.targetSystem = targetSystem; }

    public boolean isBidirectional() { return bidirectional; }
    public void setBidirectional(boolean bidirectional) { this.bidirectional = bidirectional; }

    public String getDefaultSourceCode() { return defaultSourceCode; }
    public void setDefaultSourceCode(String defaultSourceCode) { 
        this.defaultSourceCode = defaultSourceCode; 
    }

    public String getDefaultTargetCode() { return defaultTargetCode; }
    public void setDefaultTargetCode(String defaultTargetCode) { 
        this.defaultTargetCode = defaultTargetCode; 
    }

    public List<CodeMapping> getMappings() { return mappings; }
    public void setMappings(List<CodeMapping> mappings) { this.mappings = mappings; }

    /**
     * Lookup target code from source
     */
    public String lookupTarget(String sourceCode) {
        for (CodeMapping mapping : mappings) {
            if (mapping.getSourceCode().equals(sourceCode)) {
                return mapping.getTargetCode();
            }
        }
        return defaultTargetCode;
    }

    /**
     * Reverse lookup source code from target (if bidirectional)
     */
    public String lookupSource(String targetCode) {
        if (!bidirectional) {
            throw new IllegalStateException("Lookup table " + id + " is not bidirectional");
        }
        for (CodeMapping mapping : mappings) {
            if (mapping.getTargetCode().equals(targetCode)) {
                return mapping.getSourceCode();
            }
        }
        return defaultSourceCode;
    }
    
    @Override
    public String toString() {
        return "CodeLookupTable{" +
            "id='" + id + '\'' +
            ", name='" + name + '\'' +
            ", sourceSystem='" + sourceSystem + '\'' +
            ", targetSystem='" + targetSystem + '\'' +
            ", bidirectional=" + bidirectional +
            ", defaultSourceCode='" + defaultSourceCode + '\'' +
            ", defaultTargetCode='" + defaultTargetCode + '\'' +
            ", mappingsCount=" + (mappings != null ? mappings.size() : 0) +
            '}';
    }
}