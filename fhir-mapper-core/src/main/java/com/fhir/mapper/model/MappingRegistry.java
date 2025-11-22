package com.fhir.mapper.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry holding all loaded mappings and lookup tables
 */
public class MappingRegistry {
    private List<ResourceMapping> resourceMappings;
    private Map<String, CodeLookupTable> lookupTables; // Global lookup tables
    private String fhirVersion;       // R4, R5, etc.
    private long loadedTimestamp;

    public MappingRegistry() {
        this.loadedTimestamp = System.currentTimeMillis();
        this.lookupTables = new HashMap<>();
    }

    public List<ResourceMapping> getResourceMappings() { return resourceMappings; }
    public void setResourceMappings(List<ResourceMapping> resourceMappings) { 
        this.resourceMappings = resourceMappings; 
    }

    public Map<String, CodeLookupTable> getLookupTables() { return lookupTables; }
    public void setLookupTables(Map<String, CodeLookupTable> lookupTables) { 
        this.lookupTables = lookupTables; 
    }

    public String getFhirVersion() { return fhirVersion; }
    public void setFhirVersion(String fhirVersion) { this.fhirVersion = fhirVersion; }

    public long getLoadedTimestamp() { return loadedTimestamp; }

    /**
     * Find mapping by source type and direction
     */
    public ResourceMapping findBySourceAndDirection(String sourceType, MappingDirection direction) {
        return resourceMappings.stream()
            .filter(m -> m.getSourceType().equals(sourceType) && m.getDirection() == direction)
            .findFirst()
            .orElse(null);
    }

    /**
     * Find mapping by ID
     */
    public ResourceMapping findById(String id) {
        return resourceMappings.stream()
            .filter(m -> m.getId().equals(id))
            .findFirst()
            .orElse(null);
    }

    public CodeLookupTable getLookupTable(String id) {
        return lookupTables.get(id);
    }

    public void addLookupTable(CodeLookupTable table) {
        lookupTables.put(table.getId(), table);
    }
    
    @Override
    public String toString() {
        return "MappingRegistry{" +
            "fhirVersion='" + fhirVersion + '\'' +
            ", resourceMappingsCount=" + (resourceMappings != null ? resourceMappings.size() : 0) +
            ", lookupTablesCount=" + (lookupTables != null ? lookupTables.size() : 0) +
            ", loadedTimestamp=" + loadedTimestamp +
            '}';
    }
}
